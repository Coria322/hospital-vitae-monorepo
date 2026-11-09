package com.example.vitae_mobile.data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** PROV-SEC-001: 5 minutos sin interacción cierran la sesión. */
const val LIMITE_INACTIVIDAD_MS = 300_000L

class InactivityMonitor(
    private val alcance: CoroutineScope,
    /** Reloj monotónico en ms; en producción `SystemClock.elapsedRealtime`, que sigue contando con el equipo dormido. */
    private val reloj: () -> Long,
    private val limiteMs: Long = LIMITE_INACTIVIDAD_MS,
    private val alVencer: () -> Unit,
) {
    @Volatile
    private var ultimaActividad = reloj()
    private var vigilancia: Job? = null

    fun iniciar() {
        ultimaActividad = reloj()
        vigilancia?.cancel()
        vigilancia = alcance.launch {
            while (true) {
                val restante = limiteMs - (reloj() - ultimaActividad)
                if (restante <= 0) {
                    vigilancia = null
                    alVencer()
                    return@launch
                }
                delay(restante)
            }
        }
    }

    fun detener() {
        vigilancia?.cancel()
        vigilancia = null
    }

    /** Se llama en cada interacción (Activity.onUserInteraction). */
    fun registrarActividad() {
        ultimaActividad = reloj()
    }

    /** Al volver a primer plano el temporizador pudo no correr: se compara con el reloj. */
    fun comprobar() {
        if (vigilancia != null && reloj() - ultimaActividad >= limiteMs) {
            detener()
            alVencer()
        }
    }
}
