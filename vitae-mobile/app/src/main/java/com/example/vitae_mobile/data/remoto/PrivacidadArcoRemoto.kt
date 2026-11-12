package com.example.vitae_mobile.data.remoto

import android.content.Context
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.luego
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.data.network.llamarDatos
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.AvisoPrevio
import com.example.vitae_mobile.data.privacidad.AvisoPrivacidad
import com.example.vitae_mobile.data.privacidad.Consentimiento
import com.example.vitae_mobile.data.privacidad.FinalidadSecundaria
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.privacidad.SolicitudConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate

/** El backend no sirve el texto del aviso ni sus finalidades: van en la app (marcadores a validar con Legal). */
private val FINALIDADES = listOf(
    "RECORDATORIOS" to "Recibir recordatorios y comunicaciones del hospital.",
    "ESTADISTICA" to "Usar mis datos de forma anónima para fines estadísticos.",
)
private val VIGENTE_DESDE: Instant = Instant.parse("2026-10-01T12:00:00Z") // a mediodía UTC: en cualquier zona horaria cae el 1 de octubre

/**
 * Lo que este dispositivo recuerda de lo que el paciente aceptó. El portal móvil solo permite registrar consentimientos
 * (`POST`): no hay consulta de lo registrado ni revocación, así que el estado se guarda aquí, por usuario y sin datos
 * personales (el usuario va como hash).
 */
interface MemoriaConsentimientos {
    fun avisoAceptado(usuario: String): String?
    fun finalidadesAceptadas(usuario: String): Set<String>
    fun guardar(usuario: String, aviso: String?, finalidades: Set<String>)
}

class PreferenciasConsentimientos(contexto: Context) : MemoriaConsentimientos {
    private val preferencias = contexto.getSharedPreferences("consentimientos", Context.MODE_PRIVATE)

    override fun avisoAceptado(usuario: String): String? = preferencias.getString("aviso_${clave(usuario)}", null)

    override fun finalidadesAceptadas(usuario: String): Set<String> =
        preferencias.getStringSet("finalidades_${clave(usuario)}", emptySet()).orEmpty().toSet()

    override fun guardar(usuario: String, aviso: String?, finalidades: Set<String>) {
        preferencias.edit()
            .putString("aviso_${clave(usuario)}", aviso)
            .putStringSet("finalidades_${clave(usuario)}", finalidades)
            .apply()
    }

    private fun clave(usuario: String) =
        MessageDigest.getInstance("SHA-256").digest(usuario.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * Consentimientos con `POST /mobile/consentimientos`. El servidor pide un firmante y un médico responsable existente:
 * el firmante es el nombre del perfil y el responsable, el médico de la cita o receta más reciente del paciente (si no
 * tiene ninguna, no se puede registrar). Los registros son inmutables: aceptar el aviso guarda una fila de aviso y, si
 * hay finalidades opcionales, otra aparte. Los códigos de las finalidades viajan en el texto: `... | SECUNDARIAS: A,B`.
 * Revocar no existe en el portal móvil: devuelve [ApiError.NoDisponible].
 */
class PrivacidadRemoto(
    private val api: ApiBackend,
    private val backend: SesionBackend,
    private val memoria: MemoriaConsentimientos,
    private val textoAviso: String,
) : PrivacidadRepository {
    override suspend fun aviso(): Resultado<AvisoPrivacidad> {
        val usuario = backend.usuarioId ?: return Resultado.Fallo(ApiError.SesionInvalida)
        val aceptadas = memoria.finalidadesAceptadas(usuario)
        return Resultado.Exito(
            AvisoPrivacidad(
                version = AvisoPrevio.VERSION,
                vigenteDesde = VIGENTE_DESDE,
                textoSimplificado = textoAviso,
                urlIntegral = AvisoPrevio.URL_INTEGRAL,
                finalidadesSecundarias = FINALIDADES.map { (codigo, texto) -> FinalidadSecundaria(codigo, texto, codigo in aceptadas) },
                aceptada = memoria.avisoAceptado(usuario) == AvisoPrevio.VERSION,
            ),
        )
    }

    override suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
        val usuario = backend.usuarioId ?: return Resultado.Fallo(ApiError.SesionInvalida)
        if (solicitud.accion == AccionConsentimiento.REVOCAR) return Resultado.Fallo(ApiError.NoDisponible)
        return firma().luego { (firmante, responsable) ->
            val avisoAceptado = if (solicitud.tipo == TipoConsentimiento.AVISO_PRIVACIDAD) {
                val texto = "Aviso de privacidad simplificado v${solicitud.versionDocumento}"
                when (val r = conceder(TipoConsentimiento.AVISO_PRIVACIDAD, texto, firmante, responsable)) {
                    is Resultado.Fallo -> return@luego r
                    is Resultado.Exito -> r.also { memoria.guardar(usuario, solicitud.versionDocumento, memoria.finalidadesAceptadas(usuario)) }
                }
            } else {
                null
            }
            if (solicitud.finalidades.isEmpty()) return@luego avisoAceptado ?: Resultado.Fallo(ApiError.Desconocido)
            val texto = "Finalidades opcionales | SECUNDARIAS: ${solicitud.finalidades.sorted().joinToString(",")}"
            val secundarias = conceder(TipoConsentimiento.FINALIDADES_SECUNDARIAS, texto, firmante, responsable)
            if (secundarias is Resultado.Exito) {
                memoria.guardar(usuario, memoria.avisoAceptado(usuario), memoria.finalidadesAceptadas(usuario) + solicitud.finalidades)
            }
            avisoAceptado ?: secundarias
        }
    }

    // Nombre del titular y médico responsable: el perfil y la cita o receta más reciente.
    private suspend fun firma(): Resultado<Pair<String, String>> {
        val nombre = (llamarDatos { api.perfil() } as? Resultado.Exito)?.valor?.nombre ?: "Titular"
        val deCitas = (llamarDatos { api.citas() } as? Resultado.Exito)?.valor?.firstNotNullOfOrNull { it.idMedico }
        val responsable = deCitas ?: (llamarDatos { api.recetas() } as? Resultado.Exito)?.valor?.firstNotNullOfOrNull { it.idMedico }
        return if (responsable != null) Resultado.Exito(nombre to responsable) else Resultado.Fallo(ApiError.NoDisponible)
    }

    private suspend fun conceder(tipo: TipoConsentimiento, finalidades: String, firmante: String, responsable: String): Resultado<Consentimiento> =
        llamarDatos { api.consentir(ConsentimientoCuerpo(tipo.name, finalidades, firmante, responsable)) }
            .mapear { Consentimiento(it.id, runCatching { Instant.parse(it.fechaHora) }.getOrDefault(Instant.EPOCH)) }
}

/**
 * Solicitudes ARCO con `POST /mobile/solicitudes-arco`. El servidor exige una descripción de al menos 15 caracteres y la
 * app, que solo pide elegir el derecho, la redacta. No hay consulta de solicitudes: se guardan las enviadas en esta
 * sesión y no se puede actualizar su estado.
 */
class ArcoRemoto(private val api: ApiBackend, private val backend: SesionBackend) : ArcoRepository {
    override suspend fun solicitudes(): Resultado<List<SolicitudArco>> = Resultado.Exito(backend.arcoEnviadas.toList())

    override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> {
        val descripcion = "Solicitud del derecho de ${derecho.name.lowercase()} (ARCO) presentada desde la aplicación móvil."
        return llamarDatos { api.crearArco(ArcoCuerpo(derecho.name.uppercase(), descripcion)) }
            .luego { dto -> dto.aSolicitud()?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.Desconocido) }
            .mapear { solicitud -> solicitud.also { backend.arcoEnviadas.add(0, it) } }
    }

    override suspend fun consultar(folio: String): Resultado<SolicitudArco> =
        backend.arcoEnviadas.firstOrNull { it.folio == folio }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.NoDisponible)
}

// Las fechas llegan como `2026-11-08T00:00:00.000000Z`: solo importa el día.
private fun ArcoDto.aSolicitud(): SolicitudArco? = runCatching {
    SolicitudArco(
        folio = folio,
        derecho = DerechoArco.entries.first { it.name.equals(derecho, ignoreCase = true) },
        fechaRecepcion = LocalDate.parse(fechaRecepcion.take(10)),
        respuestaLimite = LocalDate.parse(respuestaLimite.take(10)),
        ejecucionLimite = LocalDate.parse(ejecucionLimite.take(10)),
        estado = estado,
    )
}.getOrNull()
