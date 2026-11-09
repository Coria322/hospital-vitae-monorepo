package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado

val SESION_PRUEBA = Sesion(accessToken = "acc-1", refreshToken = "ref-1", expiraEnSegundos = 900)

class MemoriaTokenStore : TokenStore {
    var sesion: Sesion? = null
    override fun guardar(sesion: Sesion) { this.sesion = sesion }
    override fun leer(): Sesion? = sesion
    override fun borrar() { sesion = null }
    override fun idInstalacion(): String = "instalacion-de-prueba"
}

/** Repositorio programable: cada operación devuelve lo que el test le asigne y deja constancia de sus llamadas. */
class FakeAuthRepository : AuthRepository {
    var login: Resultado<ResultadoLogin> = Resultado.Fallo(ApiError.SesionInvalida)
    var otp: Resultado<Sesion> = Resultado.Fallo(ApiError.SesionInvalida)
    var renovacion: Resultado<Sesion> = Resultado.Fallo(ApiError.SesionInvalida)

    val intentosLogin = mutableListOf<Pair<String, String>>()
    val codigosOtp = mutableListOf<String>()
    val refreshUsados = mutableListOf<String>()
    val revocados = mutableListOf<String>()

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<ResultadoLogin> {
        intentosLogin += correo to contrasena
        return login
    }

    override suspend fun verificarOtp(codigo: String): Resultado<Sesion> {
        codigosOtp += codigo
        return otp
    }

    override suspend fun renovar(refreshToken: String): Resultado<Sesion> {
        refreshUsados += refreshToken
        return renovacion
    }

    override suspend fun cerrarSesion(accessToken: String): Resultado<Unit> {
        revocados += accessToken
        return Resultado.Exito(Unit)
    }
}
