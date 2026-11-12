package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.auth.ResultadoLogin
import com.example.vitae_mobile.data.auth.Sesion
import com.example.vitae_mobile.data.network.llamar
import com.example.vitae_mobile.data.network.llamarDatos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** El paciente cuyos datos se consultan. [expediente] es null si aún no tiene expediente clínico. */
data class PacienteActivo(val id: String, val expediente: String?, val nombre: String?, val seguroActivo: Boolean?)

/**
 * Gestiona la identidad del paciente autenticado y la sesión activa en el portal móvil (RF-011).
 */
class SesionBackend(private val api: ApiBackend) {
    @Volatile
    var usuarioId: String? = null

    private val _paciente = MutableStateFlow<PacienteActivo?>(null)
    val paciente: StateFlow<PacienteActivo?> = _paciente
    val activo: PacienteActivo? get() = _paciente.value

    fun activarUsuario(id: String, nombre: String? = null) {
        usuarioId = id
        if (_paciente.value == null) {
            _paciente.value = PacienteActivo(id = id, expediente = null, nombre = nombre, seguroActivo = true)
        }
    }

    fun actualizarActivo(paciente: PacienteActivo) {
        _paciente.value = paciente
    }

    suspend fun sincronizarPerfil(): Resultado<PacienteActivo> = llamarDatos { api.mobilePerfil() }.mapear { p ->
        usuarioId = p.idPaciente
        PacienteActivo(p.idPaciente, null, p.nombreCompleto, p.seguroActivo).also { _paciente.value = it }
    }

    suspend fun pacientes(): Resultado<List<PacienteDto>> = llamarDatos { api.pacientes() }.mapear { it.data }

    /** Lee el detalle (trae el expediente y el seguro) y deja al paciente como activo. */
    suspend fun elegir(id: String): Resultado<PacienteActivo> = llamarDatos { api.paciente(id) }.mapear { p ->
        PacienteActivo(p.id, p.expediente?.id, p.nombre, p.seguroActivo).also { _paciente.value = it }
    }

    fun olvidar() {
        usuarioId = null
        _paciente.value = null
    }
}

/** `POST /auth/login`, `/auth/refresh` y `/auth/logout`. */
class AuthRemoto(private val api: ApiBackend, private val backend: SesionBackend) : AuthRepository {
    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<ResultadoLogin> =
        llamarDatos { api.login(LoginCuerpo(correo, contrasena)) }.mapear { datos ->
            backend.usuarioId = datos.usuario.idUsuario
            backend.activarUsuario(datos.usuario.idUsuario)
            ResultadoLogin.Autenticado(Sesion(datos.token, datos.token, datos.expiraEn))
        }

    override suspend fun verificarOtp(codigo: String): Resultado<Sesion> = Resultado.Fallo(ApiError.Desconocido)

    override suspend fun renovar(refreshToken: String): Resultado<Sesion> =
        llamarDatos { api.refrescar("Bearer $refreshToken") }.mapear { Sesion(it.token, it.token, it.expiraEn) }

    override suspend fun cerrarSesion(accessToken: String): Resultado<Unit> =
        llamar { api.cerrarSesion("Bearer $accessToken") }.mapear { backend.olvidar() }
}
