package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MotivoCierre { Voluntario, Inactividad, Expirada }

sealed interface EstadoSesion {
    /** [motivo] decide la pantalla de destino: Login, o "Sesión expirada" si no fue voluntario. */
    data class SinSesion(val motivo: MotivoCierre? = null) : EstadoSesion
    data class Autenticada(val sesion: Sesion) : EstadoSesion
}

class SessionManager(
    private val almacen: TokenStore,
    private val repositorio: AuthRepository,
    private val alcance: CoroutineScope,
) {
    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.SinSesion())
    val estado: StateFlow<EstadoSesion> = _estado

    private var renovacion: Job? = null

    /** Token para el AuthInterceptor. */
    fun token(): String? = (_estado.value as? EstadoSesion.Autenticada)?.sesion?.accessToken

    fun iniciar(sesion: Sesion) {
        almacen.guardar(sesion)
        _estado.value = EstadoSesion.Autenticada(sesion)
        programarRenovacion(sesion)
    }

    /** Cierre iniciado por la app (botón o inactividad): borra lo local y revoca el token en el servidor. */
    suspend fun cerrar(motivo: MotivoCierre) {
        val token = token()
        limpiar(motivo)
        if (token != null) repositorio.cerrarSesion(token)
    }

    /** El servidor ya invalidó la sesión (401): solo se limpia, no hay nada que revocar. */
    fun expirar() = limpiar(MotivoCierre.Expirada)

    /**
     * Política de arranque en frío: la sesión no se restaura (la regla de 5 min de inactividad ya habría vencido).
     * Si quedó un token de un proceso anterior, se borra y se revoca.
     */
    suspend fun descartarSesionPrevia() {
        val previa = almacen.leer() ?: return
        almacen.borrar()
        repositorio.cerrarSesion(previa.accessToken)
    }

    private fun limpiar(motivo: MotivoCierre) {
        renovacion?.cancel()
        almacen.borrar()
        _estado.value = EstadoSesion.SinSesion(motivo)
    }

    // Renueva un minuto antes de que venza el access token.
    private fun programarRenovacion(sesion: Sesion) {
        renovacion?.cancel()
        renovacion = alcance.launch {
            delay((sesion.expiraEnSegundos - MARGEN_RENOVACION_S).coerceAtLeast(1) * 1000)
            when (val r = repositorio.renovar(sesion.refreshToken)) {
                is Resultado.Exito -> iniciar(r.valor)
                // Refresh reutilizado o revocado: se fuerza nuevo login.
                is Resultado.Fallo -> if (r.error == ApiError.SesionInvalida) expirar()
            }
        }
    }

    private companion object {
        const val MARGEN_RENOVACION_S = 60L
    }
}
