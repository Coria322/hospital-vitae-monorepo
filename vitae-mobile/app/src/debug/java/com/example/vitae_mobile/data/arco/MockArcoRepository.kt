package com.example.vitae_mobile.data.arco

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Servidor simulado hasta que exista `/api/v1/mobile/solicitudes-arco`. El plazo de respuesta es de 20 días y el de
 * ejecución de 15 más (días naturales: la especificación no define el calendario hábil, AMB-014).
 */
class MockArcoRepository(private val reloj: Clock = Clock.systemUTC()) : ArcoRepository {
    private val registradas = mutableListOf<SolicitudArco>()

    /** Vuelve a no tener solicitudes; lo usan las pruebas instrumentadas. */
    fun reiniciar() = registradas.clear()

    override suspend fun solicitudes(): Resultado<List<SolicitudArco>> {
        delay(RETRASO_MS)
        return Resultado.Exito(registradas.reversed())
    }

    override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> {
        delay(RETRASO_MS)
        val hoy = LocalDate.now(reloj)
        val respuesta = hoy.plusDays(20)
        val solicitud = SolicitudArco(
            folio = String.format(Locale.ROOT, "ARCO-%04d", registradas.size + 1),
            derecho = derecho,
            fechaRecepcion = hoy,
            respuestaLimite = respuesta,
            ejecucionLimite = respuesta.plusDays(15),
            estado = "Recibida",
        )
        registradas += solicitud
        return Resultado.Exito(solicitud)
    }

    override suspend fun consultar(folio: String): Resultado<SolicitudArco> {
        delay(RETRASO_MS)
        // Un folio que no es del paciente responde 403, como el servidor real.
        return registradas.firstOrNull { it.folio == folio }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.SinPermisos)
    }

    private companion object {
        const val RETRASO_MS = 300L
    }
}
