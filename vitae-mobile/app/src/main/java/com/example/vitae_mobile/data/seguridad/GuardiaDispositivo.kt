package com.example.vitae_mobile.data.seguridad

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado del dispositivo para toda la app. La comprobación es local, síncrona y barata, así que no hay pantalla de
 * carga: se hace al construirlo y en cada [verificar] (la actividad lo llama al volver a primer plano).
 *
 * [alBloquear] se invoca cada vez que el estado resulta bloqueado; el contenedor lo usa para borrar la sesión local.
 */
class GuardiaDispositivo(
    private val entorno: EntornoDispositivo,
    private val detector: DetectorRoot,
    private val alBloquear: () -> Unit = {},
) {
    private val _estado = MutableStateFlow(evaluar())
    val estado: StateFlow<EstadoDispositivo> = _estado

    // El 426 del servidor no desaparece al volver a comprobar: la versión de la app no cambia hasta reinstalar.
    private var servidorIncompatible = false

    init {
        notificarSiBloqueado()
    }

    fun verificar() {
        _estado.value = evaluar()
        notificarSiBloqueado()
    }

    /** HTTP 426 (RNF-006): el servidor ya no acepta esta versión de la app. */
    fun marcarIncompatible() {
        servidorIncompatible = true
        verificar()
    }

    /** Solo para pruebas instrumentadas: el 426 es permanente en producción, pero las pruebas comparten proceso. */
    internal fun olvidarIncompatible() {
        servidorIncompatible = false
        verificar()
    }

    /** Con el dispositivo bloqueado no sale ninguna petición de red (ver `DispositivoInterceptor`). */
    fun bloqueado(): Boolean = _estado.value != EstadoDispositivo.Apto

    private fun evaluar(): EstadoDispositivo {
        val base = verificarDispositivo(entorno, detector)
        return if (base == EstadoDispositivo.Apto && servidorIncompatible) EstadoDispositivo.Incompatible else base
    }

    private fun notificarSiBloqueado() {
        if (bloqueado()) alBloquear()
    }
}
