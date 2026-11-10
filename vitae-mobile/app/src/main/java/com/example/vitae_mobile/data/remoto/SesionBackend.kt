package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.luego
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.auth.ResultadoLogin
import com.example.vitae_mobile.data.auth.Sesion
import com.example.vitae_mobile.data.network.llamar
import com.example.vitae_mobile.data.network.llamarDatos
import java.util.concurrent.CopyOnWriteArrayList

private const val ROL_PACIENTE = "PACIENTE"

/** Lo que la app recuerda de la sesión del paciente, solo en memoria; se borra al cerrar la sesión. */
class SesionBackend {
    @Volatile
    var usuarioId: String? = null

    /** El portal móvil no tiene consulta de solicitudes ARCO: se muestran las enviadas en esta sesión. */
    val arcoEnviadas = CopyOnWriteArrayList<SolicitudArco>()

    fun olvidar() {
        usuarioId = null
        arcoEnviadas.clear()
    }
}

/**
 * `POST /auth/login`, `/auth/refresh` y `/auth/logout`. Sin OTP ni PKCE: el backend actual no los tiene. El portal móvil
 * es solo para pacientes (RF-002): una cuenta de personal inicia sesión en el servidor, pero la app la rechaza y revoca
 * su token en el acto.
 */
class AuthRemoto(private val api: ApiBackend, private val backend: SesionBackend) : AuthRepository {
    // El campo "correo" de la app lleva el id de usuario del backend (`id_usuario`).
    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<ResultadoLogin> =
        llamarDatos { api.login(LoginCuerpo(correo, contrasena)) }.luego<LoginDatos, ResultadoLogin> { datos ->
            if (datos.usuario.rol != ROL_PACIENTE) {
                llamar { api.cerrarSesion("Bearer ${datos.token}") }
                Resultado.Fallo(ApiError.SinPermisos)
            } else {
                backend.usuarioId = datos.usuario.idUsuario
                Resultado.Exito(ResultadoLogin.Autenticado(Sesion(datos.token, datos.token, datos.expiraEn)))
            }
        }

    override suspend fun verificarOtp(codigo: String): Resultado<Sesion> = Resultado.Fallo(ApiError.Desconocido)

    // No hay refresh token: se renueva con el access token vigente, que el backend revoca y reemplaza.
    override suspend fun renovar(refreshToken: String): Resultado<Sesion> =
        llamarDatos { api.refrescar("Bearer $refreshToken") }.mapear { Sesion(it.token, it.token, it.expiraEn) }

    override suspend fun cerrarSesion(accessToken: String): Resultado<Unit> =
        llamar { api.cerrarSesion("Bearer $accessToken") }.mapear { }
}
