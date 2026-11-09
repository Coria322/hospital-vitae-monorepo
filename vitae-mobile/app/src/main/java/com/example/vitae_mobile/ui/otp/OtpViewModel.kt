package com.example.vitae_mobile.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.auth.LONGITUD_OTP
import com.example.vitae_mobile.data.auth.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OtpUiState(
    val codigo: String = "",
    val cargando: Boolean = false,
    val error: Boolean = false,
    val sinConexion: Boolean = false,
)

class OtpViewModel(
    private val repositorio: AuthRepository,
    private val sesion: SessionManager,
) : ViewModel() {
    private val _estado = MutableStateFlow(OtpUiState())
    val estado: StateFlow<OtpUiState> = _estado

    fun cambiarCodigo(valor: String) =
        _estado.update { it.copy(codigo = valor.filter(Char::isDigit).take(LONGITUD_OTP), error = false, sinConexion = false) }

    fun verificar() {
        val actual = _estado.value
        if (actual.cargando || actual.codigo.length != LONGITUD_OTP) return
        _estado.update { it.copy(cargando = true, error = false, sinConexion = false) }
        viewModelScope.launch {
            when (val r = repositorio.verificarOtp(actual.codigo)) {
                // Al iniciar la sesión la navegación pasa sola a Inicio.
                is Resultado.Exito -> sesion.iniciar(r.valor)
                is Resultado.Fallo -> _estado.update {
                    it.copy(
                        cargando = false,
                        codigo = "",
                        error = r.error != ApiError.Red && r.error != ApiError.Servidor,
                        sinConexion = r.error == ApiError.Red || r.error == ApiError.Servidor,
                    )
                }
            }
        }
    }
}
