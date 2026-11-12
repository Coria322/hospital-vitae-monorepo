package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.luego
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Perfil: `GET /mobile/perfil`. El portal móvil lo entrega como JSON propio, no como FHIR. */
class PerfilRemoto(private val api: ApiBackend, private val backend: SesionBackend? = null) : PerfilRepository {
    override suspend fun perfil(): Resultado<Perfil> =
        llamarDatos { api.perfil() }.mapear { Perfil(it.nombre, it.curp, it.fechaNacimiento, it.seguroActivo) }
}

/** `GET /mobile/citas`: citas del paciente, de la más próxima a la más lejana; `POST /mobile/citas` pide una nueva. */
class CitasRemoto(private val api: ApiBackend) : CitasRepository {
    override suspend fun citas(): Resultado<List<Cita>> =
        llamarDatos { api.citas() }.mapear { lista -> lista.mapNotNull { it.aCita() } }

    override suspend fun solicitar(fechaHora: Instant, especialidad: String?, motivo: String?): Resultado<Cita> {
        val cuerpo = CitaCuerpo(
            DateTimeFormatter.ISO_INSTANT.format(fechaHora.truncatedTo(ChronoUnit.SECONDS)),
            especialidad,
            motivo,
        )
        return llamarDatos { api.solicitarCita(cuerpo) }.luego { dto ->
            dto.aCita()?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.Desconocido)
        }
    }

    override suspend fun solicitarCita(fechaHora: String, especialidad: String?, motivo: String?): Resultado<Cita> {
        val instant = runCatching { Instant.parse(fechaHora) }.getOrElse { Instant.now() }
        return solicitar(instant, especialidad, motivo)
    }
}

// Una cita con la fecha ilegible se omite: no se inventa una hora.
private fun CitaDto.aCita(): Cita? = runCatching { Cita(id, Instant.parse(fechaHora), estado) }.getOrNull()

/** `GET /mobile/recetas`: la lista ya trae todo lo que la app muestra, así que el detalle sale de ella. */
class RecetasRemoto(private val api: ApiBackend, private val backend: SesionBackend? = null) : RecetasRepository {
    override suspend fun recetas(): Resultado<List<Receta>> =
        llamarDatos { api.recetas() }.mapear { lista -> lista.map { it.aReceta() } }

    override suspend fun receta(id: String): Resultado<Receta> = recetas().luego { lista ->
        lista.firstOrNull { it.id == id }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.Desconocido)
    }
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
 * La lista sale de `GET /mobile/resultados`. El contenido llega descifrado dentro del JSON (`contenido_descifrado`,
 * texto); un archivo binario no cabe en ese formato. El id de cada resultado es el de su solicitud de estudio.
 */
class ResultadosRemoto(
    private val api: ApiBackend,
    private val directorio: File,
    private val backend: SesionBackend? = null,
) : ResultadosRepository {
    override suspend fun resultados(): Resultado<List<ResultadoEstudio>> =
        llamarDatos { api.resultados() }.mapear { lista ->
            lista.mapNotNull { r ->
                val fecha = runCatching { Instant.parse(r.fechaHora) }.getOrNull() ?: return@mapNotNull null
                ResultadoEstudio(r.idSolicitud, r.solicitud?.tipo ?: "Estudio", fecha, r.mime)
            }
        }

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
