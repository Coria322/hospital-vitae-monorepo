package com.example.vitae_mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.auth.ResultadoLogin
import com.example.vitae_mobile.data.auth.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ErrorLogin { Credenciales, SinAcceso, CuentaBloqueada, Red, Incompatible }

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: ErrorLogin? = null,
    /** Credenciales correctas en un dispositivo nuevo: la pantalla debe abrir el OTP. */
    val requiereOtp: Boolean = false,
)

class LoginViewModel(
    private val repositorio: AuthRepository,
    private val sesion: SessionManager,
) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun cerrarAlerta() = _estado.update { it.copy(error = null) }
    fun otpAbierto() = _estado.update { it.copy(requiereOtp = false) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando || actual.correo.isBlank() || actual.contrasena.isEmpty()) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = repositorio.iniciarSesion(actual.correo.trim(), actual.contrasena)) {
                is Resultado.Exito -> when (val login = r.valor) {
                    is ResultadoLogin.Autenticado -> {
                        sesion.iniciar(login.sesion)
                        _estado.update { LoginUiState(correo = it.correo) }
                    }
                    ResultadoLogin.RequiereOtp -> _estado.update { it.copy(cargando = false, contrasena = "", requiereOtp = true) }
                }
                // La contraseña no se conserva tras un intento fallido.
                is Resultado.Fallo -> _estado.update { it.copy(cargando = false, contrasena = "", error = r.error.aErrorLogin()) }
            }
        }
    }
}

// PROV-SEC-001: los mensajes de autenticación son genéricos; solo 403, 423 y 426 tienen texto propio.
internal fun ApiError.aErrorLogin(): ErrorLogin = when (this) {
    ApiError.SinPermisos -> ErrorLogin.SinAcceso
    ApiError.CuentaBloqueada -> ErrorLogin.CuentaBloqueada
    ApiError.AppIncompatible -> ErrorLogin.Incompatible
    ApiError.Red, ApiError.Servidor -> ErrorLogin.Red
    ApiError.SesionInvalida, ApiError.Desconocido -> ErrorLogin.Credenciales
}
