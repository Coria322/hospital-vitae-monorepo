package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.luego
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.fhir.FhirParser
import com.example.vitae_mobile.data.network.llamar
import com.example.vitae_mobile.data.network.llamarDatos
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.perfil.PerfilRepository
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.resultados.ArchivoDescargado
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Perfil: consulta endpoints normativos móviles (RF-011) o FHIR R4 si hay expediente clínico explícito. */
class PerfilRemoto(private val api: ApiBackend, private val backend: SesionBackend) : PerfilRepository {
    override suspend fun perfil(): Resultado<Perfil> {
        val paciente = backend.activo
        if (paciente == null && backend.usuarioId == null) {
            return Resultado.Fallo(ApiError.Desconocido)
        }

        if (paciente != null && paciente.expediente != null) {
            return llamar { withContext(Dispatchers.IO) { api.fhirPatient(paciente.id).string() } }
                .luego { FhirParser.perfilDe(it) }
                .mapear { it.copy(seguroActivo = paciente.seguroActivo ?: it.seguroActivo) }
        }

        val respuestaMobile = llamarDatos { api.mobilePerfil() }
        if (respuestaMobile is Resultado.Exito) {
            val dto = respuestaMobile.valor
            backend.actualizarActivo(PacienteActivo(dto.idPaciente, null, dto.nombreCompleto, dto.seguroActivo))
            return Resultado.Exito(
                Perfil(
                    nombre = dto.nombreCompleto,
                    curp = dto.curp,
                    fechaNacimiento = dto.fechaNacimiento,
                    seguroActivo = dto.seguroActivo,
                )
            )
        }

        if (paciente != null) {
            return llamar { withContext(Dispatchers.IO) { api.fhirPatient(paciente.id).string() } }
                .luego { FhirParser.perfilDe(it) }
                .mapear { it.copy(seguroActivo = paciente.seguroActivo ?: it.seguroActivo) }
        }

        return Resultado.Fallo(ApiError.Desconocido)
    }
}

/** Citas del paciente: consulta a `/mobile/citas` (RF-011 / PROV-RF-002). */
class CitasRemoto(private val api: ApiBackend? = null) : CitasRepository {
    override suspend fun citas(): Resultado<List<Cita>> {
        val cliente = api ?: return Resultado.Fallo(ApiError.NoDisponible)
        return llamarDatos { cliente.mobileCitas() }.mapear { lista ->
            lista.map { dto ->
                val fecha = runCatching { Instant.parse(dto.fechaHora) }.getOrDefault(Instant.now())
                Cita(
                    id = dto.idCita,
                    fechaHora = fecha,
                    estado = dto.estado ?: "PROGRAMADA",
                    especialidad = dto.especialidad,
                    consultorio = dto.consultorio,
                )
            }
        }
    }

    override suspend fun solicitarCita(fechaHora: String, especialidad: String?, motivo: String?): Resultado<Cita> {
        val cliente = api ?: return Resultado.Fallo(ApiError.NoDisponible)
        return llamarDatos {
            cliente.solicitarCita(SolicitarCitaCuerpo(fechaHora, especialidad, motivo))
        }.mapear { dto ->
            val fecha = runCatching { Instant.parse(dto.fechaHora) }.getOrDefault(Instant.now())
            Cita(
                id = dto.idCita,
                fechaHora = fecha,
                estado = dto.estado ?: "PENDIENTE",
                especialidad = dto.especialidad,
                consultorio = dto.consultorio,
            )
        }
    }
}

/** Expediente del paciente activo; sin expediente no hay recetas ni estudios, no es un error. */
private suspend fun expedienteActivo(api: ApiBackend, backend: SesionBackend): Resultado<ExpedienteDto> {
    val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
    val expediente = paciente.expediente ?: return Resultado.Exito(ExpedienteDto())
    return llamarDatos { api.expediente(expediente) }
}

/** Recetas médicas: consulta `/mobile/recetas` en app móvil, o expediente si se seleccionó uno manualmente. */
class RecetasRemoto(private val api: ApiBackend, private val backend: SesionBackend) : RecetasRepository {
    override suspend fun recetas(): Resultado<List<Receta>> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        if (paciente.expediente != null) {
            return expedienteActivo(api, backend).mapear { e -> e.recetas.map { it.aReceta() } }
        }
        if (backend.usuarioId == null || backend.usuarioId != paciente.id) {
            return Resultado.Exito(emptyList())
        }

        val respuestaMobile = llamarDatos { api.mobileRecetas() }
        if (respuestaMobile is Resultado.Exito) {
            return Resultado.Exito(respuestaMobile.valor.map { it.aReceta() })
        }
        return expedienteActivo(api, backend).mapear { e -> e.recetas.map { it.aReceta() } }
    }

    override suspend fun receta(id: String): Resultado<Receta> = llamarDatos { api.receta(id) }.mapear { it.aReceta() }
}

internal fun MobileRecetaDto.aReceta(): Receta {
    val estado = when (qrEstado) {
        "VIGENTE" -> EstadoReceta.Activa
        "CONSUMIDO" -> EstadoReceta.Dispensada
        "CANCELADO" -> EstadoReceta.Cancelada
        else -> EstadoReceta.Desconocido
    }
    val codigo = qrCodigo?.takeIf { it.isNotBlank() }
    return Receta(
        id = idReceta,
        medicamento = medicamentos ?: folio,
        dosis = indicaciones,
        via = null,
        duracion = null,
        estado = estado,
        codigoQr = codigo,
        qrVigente = codigo != null && estado == EstadoReceta.Activa,
    )
}

/** El QR llega solo después de firmar la receta; antes, `qr_codigo` es null. El estado sale de `qr_estado`. */
internal fun RecetaDto.aReceta(): Receta {
    val estado = when (qrEstado) {
        "VIGENTE" -> EstadoReceta.Activa
        "CONSUMIDO" -> EstadoReceta.Dispensada
        "CANCELADO" -> EstadoReceta.Cancelada
        else -> EstadoReceta.Desconocido
    }
    val codigo = qrCodigo?.takeIf { it.isNotBlank() }
    return Receta(id, medicamentos, indicaciones, null, null, estado, codigo, codigo != null && estado == EstadoReceta.Activa)
}

/**
 * Resultados de estudio: consulta `/mobile/resultados` en app móvil o expediente si se seleccionó manualmente.
 */
class ResultadosRemoto(
    private val api: ApiBackend,
    private val backend: SesionBackend,
    private val directorio: File,
) : ResultadosRepository {
    override suspend fun resultados(): Resultado<List<ResultadoEstudio>> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        if (paciente.expediente != null) {
            return expedienteActivo(api, backend).mapear { e ->
                e.solicitudes.mapNotNull { solicitud ->
                    val resultado = solicitud.resultado ?: return@mapNotNull null
                    val fecha = runCatching { Instant.parse(resultado.fechaHora) }.getOrNull() ?: return@mapNotNull null
                    ResultadoEstudio(solicitud.id, solicitud.tipo, fecha, resultado.mime)
                }
            }
        }
        if (backend.usuarioId == null || backend.usuarioId != paciente.id) {
            return Resultado.Exito(emptyList())
        }

        val respuestaMobile = llamarDatos { api.mobileResultados() }
        if (respuestaMobile is Resultado.Exito) {
            return Resultado.Exito(
                respuestaMobile.valor.mapNotNull { dto ->
                    val fecha = runCatching { Instant.parse(dto.fechaHora) }.getOrNull() ?: return@mapNotNull null
                    val tipo = dto.solicitud?.tipoEstudio ?: "Estudio de Laboratorio"
                    ResultadoEstudio(
                        id = dto.solicitud?.idSolicitud ?: dto.idResultado,
                        tipoEstudio = tipo,
                        fechaHora = fecha,
                        mime = dto.mime ?: "application/pdf",
                    )
                }
            )
        }
        return expedienteActivo(api, backend).mapear { e ->
            e.solicitudes.mapNotNull { solicitud ->
                val resultado = solicitud.resultado ?: return@mapNotNull null
                val fecha = runCatching { Instant.parse(resultado.fechaHora) }.getOrNull() ?: return@mapNotNull null
                ResultadoEstudio(solicitud.id, solicitud.tipo, fecha, resultado.mime)
            }
        }
    }

    override suspend fun archivo(id: String, progreso: (Float) -> Unit): Resultado<ArchivoDescargado> {
        val detalleRes = llamarDatos { api.resultado(id) }.let { res ->
            if (res is Resultado.Exito) res else {
                val movil = llamarDatos { api.mobileResultadoDetalle(id) }
                if (movil is Resultado.Exito) movil else res
            }
        }

        if (detalleRes is Resultado.Fallo) return detalleRes
        val detalle = (detalleRes as Resultado.Exito).valor
        val contenido = detalle.contenido ?: return Resultado.Fallo(ApiError.Desconocido)

        return withContext(Dispatchers.IO) {
            directorio.mkdirs()
            val destino = File(directorio, id)
            destino.writeText(contenido, Charsets.UTF_8)
            progreso(1f)
            Resultado.Exito(ArchivoDescargado(destino, detalle.resultado.mime))
        }
    }
}
