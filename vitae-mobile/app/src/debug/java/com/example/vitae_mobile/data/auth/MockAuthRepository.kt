package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado

// Credenciales sintéticas del mock. Viven en el source set debug: el release no puede referenciarlas.
const val CORREO_PACIENTE_DEMO = "paciente.demo@vitae.test"
const val CORREO_PERSONAL_DEMO = "medico.demo@vitae.test"
const val CONTRASENA_DEMO = "Demo#2026-vitae"
const val CODIGO_OTP_DEMO = "123456"

/**
 * Servidor simulado hasta que exista los endpoints de `/api/v1/mobile/auth`. Reproduce las reglas de la especificación:
 * OTP en dispositivo nuevo (PROV-SEC-001), bloqueo al 3.er fallo (423), personal rechazado con 403 (RF-002),
 * PKCE S256 y refresh rotativo con detección de reutilización (PROV-SEC-002).
 */
class MockAuthRepository(private val idInstalacion: () -> String) : AuthRepository {
    private val fallos = mutableMapOf<String, Int>()
    private val dispositivosVerificados = mutableSetOf<Pair<String, String>>()
    private var correoPendiente: String? = null

    private val codigos = mutableMapOf<String, Pair<String, String>>() // código → (challenge, correo)
    private val accessVigentes = mutableMapOf<String, String>() // access → correo
    private val refreshVigentes = mutableMapOf<String, String>() // refresh → correo
    private var contador = 0

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<ResultadoLogin> {
        if ((fallos[correo] ?: 0) >= MAX_FALLOS) return Resultado.Fallo(ApiError.CuentaBloqueada)
        if (correo == CORREO_PERSONAL_DEMO && contrasena == CONTRASENA_DEMO) return Resultado.Fallo(ApiError.SinPermisos)
        if (correo != CORREO_PACIENTE_DEMO || contrasena != CONTRASENA_DEMO) {
            // Mismo error para correo inexistente y contraseña errónea: sin enumeración de cuentas.
            val n = (fallos[correo] ?: 0) + 1
            fallos[correo] = n
            return Resultado.Fallo(if (n >= MAX_FALLOS) ApiError.CuentaBloqueada else ApiError.SesionInvalida)
        }
        fallos.remove(correo)
        if (correo to idInstalacion() !in dispositivosVerificados) {
            correoPendiente = correo
            return Resultado.Exito(ResultadoLogin.RequiereOtp)
        }
        return Resultado.Exito(ResultadoLogin.Autenticado(emitir(correo)))
    }

    override suspend fun verificarOtp(codigo: String): Resultado<Sesion> {
        val correo = correoPendiente
        if (correo == null || codigo != CODIGO_OTP_DEMO) return Resultado.Fallo(ApiError.SesionInvalida)
        correoPendiente = null
        dispositivosVerificados += correo to idInstalacion()
        return Resultado.Exito(emitir(correo))
    }

    override suspend fun renovar(refreshToken: String): Resultado<Sesion> {
        val correo = refreshVigentes.remove(refreshToken)
        if (correo == null) {
            // Refresh reutilizado: se revoca toda la familia de tokens.
            accessVigentes.clear()
            refreshVigentes.clear()
            return Resultado.Fallo(ApiError.SesionInvalida)
        }
        return Resultado.Exito(emitir(correo))
    }

    override suspend fun cerrarSesion(accessToken: String): Resultado<Unit> {
        val correo = accessVigentes.remove(accessToken)
        refreshVigentes.values.removeAll { it == correo }
        return Resultado.Exito(Unit)
    }

    /** Lo que el servidor real respondería con 401 si el token estuviera revocado. */
    fun esValido(accessToken: String): Boolean = accessToken in accessVigentes

    // Authorization Code + PKCE de punta a punta, dentro del mismo proceso.
    private fun emitir(correo: String): Sesion {
        val verifier = Pkce.generarVerifier()
        val codigo = autorizar(correo, Pkce.challengeS256(verifier))
        return checkNotNull(canjear(codigo, verifier)) { "El mock rechazó su propio PKCE" }
    }

    internal fun autorizar(correo: String, challenge: String): String {
        val codigo = "cod-${++contador}"
        codigos[codigo] = challenge to correo
        return codigo
    }

    /** Solo entrega tokens si el verifier corresponde al challenge (code_challenge_method=S256). */
    internal fun canjear(codigo: String, verifier: String): Sesion? {
        val (challenge, correo) = codigos.remove(codigo) ?: return null
        if (Pkce.challengeS256(verifier) != challenge) return null
        val n = ++contador
        val sesion = Sesion(accessToken = "acc-$n", refreshToken = "ref-$n", expiraEnSegundos = EXPIRA_EN_S)
        accessVigentes[sesion.accessToken] = correo
        refreshVigentes[sesion.refreshToken] = correo
        return sesion
    }

    private companion object {
        const val MAX_FALLOS = 3
        const val EXPIRA_EN_S = 900L
    }
}
