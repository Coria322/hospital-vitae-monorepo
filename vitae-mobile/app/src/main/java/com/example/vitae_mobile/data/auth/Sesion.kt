package com.example.vitae_mobile.data.auth

import kotlinx.serialization.Serializable

/** Tokens de la sesión. El access token dura <= 15 min (PROV-SEC-002) y el refresh rota en cada uso. */
@Serializable
data class Sesion(
    val accessToken: String,
    val refreshToken: String,
    val expiraEnSegundos: Long,
)

sealed interface ResultadoLogin {
    data class Autenticado(val sesion: Sesion) : ResultadoLogin

    /** Dispositivo no registrado: no se entregan tokens hasta verificar el OTP (PROV-SEC-001). */
    data object RequiereOtp : ResultadoLogin
}

/** Longitud del código de un solo uso. La especificación no la define: supuesto de la etapa 2. */
const val LONGITUD_OTP = 6
