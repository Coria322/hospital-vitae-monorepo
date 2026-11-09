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

/** Perfil: `GET /fhir/Patient/{id}` (FHIR R4 crudo) más el seguro, que el Patient no trae pero el paciente sí. */
class PerfilRemoto(private val api: ApiBackend, private val backend: SesionBackend) : PerfilRepository {
    override suspend fun perfil(): Resultado<Perfil> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        return llamar { withContext(Dispatchers.IO) { api.fhirPatient(paciente.id).string() } }
            .luego { FhirParser.perfilDe(it) }
            .mapear { it.copy(seguroActivo = paciente.seguroActivo ?: it.seguroActivo) }
    }
}

/** El backend no tiene agenda ni citas: no hay endpoint que consultar. */
class CitasRemoto : CitasRepository {
    override suspend fun citas(): Resultado<List<Cita>> = Resultado.Fallo(ApiError.NoDisponible)
}

/** Expediente del paciente activo; sin expediente no hay recetas ni estudios, no es un error. */
private suspend fun expedienteActivo(api: ApiBackend, backend: SesionBackend): Resultado<ExpedienteDto> {
    val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
    val expediente = paciente.expediente ?: return Resultado.Exito(ExpedienteDto())
    return llamarDatos { api.expediente(expediente) }
}

/**
 * No existe una lista de recetas del paciente: salen del expediente (`GET /expedientes-clinicos/{id}`), que el backend
 * solo entrega al médico dueño del expediente (o con acceso de emergencia); otro usuario recibe 403.
 */
class RecetasRemoto(private val api: ApiBackend, private val backend: SesionBackend) : RecetasRepository {
    override suspend fun recetas(): Resultado<List<Receta>> =
        expedienteActivo(api, backend).mapear { e -> e.recetas.map { it.aReceta() } }

    override suspend fun receta(id: String): Resultado<Receta> = llamarDatos { api.receta(id) }.mapear { it.aReceta() }
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
    // El backend guarda las indicaciones como texto libre (`dosageInstruction.text`): va en "dosis"; vía y duración no existen.
    return Receta(id, medicamentos, indicaciones, null, null, estado, codigo, codigo != null && estado == EstadoReceta.Activa)
}

/**
 * Los resultados salen de las solicitudes de estudio del expediente que ya tienen resultado. El contenido llega
 * descifrado dentro del JSON (`contenido_descifrado`, texto); un archivo binario no cabe en ese formato.
 */
class ResultadosRemoto(
    private val api: ApiBackend,
    private val backend: SesionBackend,
    private val directorio: File,
) : ResultadosRepository {
    override suspend fun resultados(): Resultado<List<ResultadoEstudio>> = expedienteActivo(api, backend).mapear { e ->
        e.solicitudes.mapNotNull { solicitud ->
            val resultado = solicitud.resultado ?: return@mapNotNull null
            val fecha = runCatching { Instant.parse(resultado.fechaHora) }.getOrNull() ?: return@mapNotNull null
            ResultadoEstudio(solicitud.id, solicitud.tipo, fecha, resultado.mime)
        }
    }

    // El id es el de la solicitud de estudio.
    override suspend fun archivo(id: String, progreso: (Float) -> Unit): Resultado<ArchivoDescargado> =
        llamarDatos { api.resultado(id) }.luego { detalle ->
            val contenido = detalle.contenido ?: return@luego Resultado.Fallo(ApiError.Desconocido)
            withContext(Dispatchers.IO) {
                directorio.mkdirs()
                val destino = File(directorio, id)
                destino.writeText(contenido, Charsets.UTF_8)
                progreso(1f)
                Resultado.Exito(ArchivoDescargado(destino, detalle.resultado.mime))
            }
        }
}
