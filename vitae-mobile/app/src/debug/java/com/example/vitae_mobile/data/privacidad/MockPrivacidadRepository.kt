package com.example.vitae_mobile.data.privacidad

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/** Un POST ya aceptado por el servidor simulado. La lista es de solo añadir, como la tabla real (CON-016). */
data class RegistroConsentimiento(val id: String, val solicitud: SolicitudConsentimiento, val fechaHora: Instant)

private const val TEXTO_DEMO =
    "Texto de ejemplo del aviso de privacidad simplificado (datos sintéticos). El hospital trata tus datos " +
        "personales para brindarte atención médica y administrar tus citas, recetas y resultados. " +
        "Puedes consultar el aviso integral en el enlace de abajo."

/**
 * Servidor simulado hasta que existan `/api/v1/mobile/aviso-privacidad` y `/api/v1/mobile/consentimientos`.
 * Conserva el estado mientras viva el proceso; [publicarNuevaVersion] simula un cambio sustancial del aviso, que
 * exige aceptarlo otra vez.
 */
class MockPrivacidadRepository(private val reloj: Clock = Clock.systemUTC()) : PrivacidadRepository {
    var versionVigente = "1.0"
        private set

    private val finalidades = listOf(
        "RECORDATORIOS" to "Recibir recordatorios y comunicaciones del hospital.",
        "ESTADISTICA" to "Usar mis datos de forma anónima para fines estadísticos.",
    )
    private val aceptadas = mutableSetOf<String>()
    private var versionAceptada: String? = null
    private val _registros = mutableListOf<RegistroConsentimiento>()
    private var contador = 0

    val registros: List<RegistroConsentimiento> get() = _registros.toList()

    /** Vuelve al estado de un paciente nuevo; lo usan las pruebas instrumentadas. */
    fun reiniciar() {
        versionVigente = "1.0"
        aceptadas.clear()
        versionAceptada = null
        _registros.clear()
        contador = 0
    }

    fun publicarNuevaVersion() {
        versionVigente = "${versionVigente.substringBefore('.').toInt() + 1}.0"
    }

    override suspend fun aviso(): Resultado<AvisoPrivacidad> {
        delay(RETRASO_MS)
        return Resultado.Exito(
            AvisoPrivacidad(
                version = versionVigente,
                vigenteDesde = reloj.instant().minus(Duration.ofDays(30)),
                textoSimplificado = TEXTO_DEMO,
                urlIntegral = "https://example.com/aviso-de-privacidad-integral",
                finalidadesSecundarias = finalidades.map { (codigo, texto) -> FinalidadSecundaria(codigo, texto, codigo in aceptadas) },
                aceptada = versionAceptada == versionVigente,
            )
        )
    }

    override suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
        delay(RETRASO_MS)
        // El servidor real rechazaría una versión que ya no es la vigente.
        if (solicitud.versionDocumento != versionVigente) return Resultado.Fallo(ApiError.Desconocido)
        when {
            solicitud.accion == AccionConsentimiento.REVOCAR -> aceptadas -= solicitud.finalidades.toSet()
            solicitud.tipo == TipoConsentimiento.AVISO_PRIVACIDAD -> {
                versionAceptada = solicitud.versionDocumento
                aceptadas.clear()
                aceptadas += solicitud.finalidades
            }
            else -> aceptadas += solicitud.finalidades
        }
        val ahora = reloj.instant()
        val id = "con-${++contador}"
        _registros += RegistroConsentimiento(id, solicitud, ahora)
        return Resultado.Exito(Consentimiento(id, ahora))
    }

    private companion object {
        const val RETRASO_MS = 300L
    }
}
