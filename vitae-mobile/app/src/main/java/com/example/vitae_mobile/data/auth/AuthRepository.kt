package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.Resultado

/**
 * Contrato de autenticación del paciente. El intercambio OAuth + PKCE queda dentro de la implementación:
 * la especificación no define cómo se entrega el código (ver PLAN.md, decisiones abiertas).
 */
interface AuthRepository {
    /** Credenciales correctas en un dispositivo nuevo devuelven [ResultadoLogin.RequiereOtp], sin tokens. */
    suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<ResultadoLogin>

    /** Completa el login que quedó pendiente de OTP. */
    suspend fun verificarOtp(codigo: String): Resultado<Sesion>

    /** Refresh rotativo: el token anterior deja de valer; reutilizarlo invalida la sesión. */
    suspend fun renovar(refreshToken: String): Resultado<Sesion>

    /** Revocación inmediata en el servidor. */
    suspend fun cerrarSesion(accessToken: String): Resultado<Unit>
}
