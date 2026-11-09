package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Fija las reglas del servidor simulado para que los demás tests puedan apoyarse en él.
class MockAuthRepositoryTest {
    private var instalacion = "instalacion-1"
    private val repo = MockAuthRepository { instalacion }

    private suspend fun login() = repo.iniciarSesion(CORREO_PACIENTE_DEMO, CONTRASENA_DEMO)

    private suspend fun sesionConOtp(): Sesion {
        login()
        return (repo.verificarOtp(CODIGO_OTP_DEMO) as Resultado.Exito).valor
    }

    @Test
    fun unDispositivoNuevoPideOtpSinEntregarTokens() = runTest {
        assertEquals(Resultado.Exito(ResultadoLogin.RequiereOtp), login())
    }

    @Test
    fun conOtpCorrectoEntregaSesionYElSiguienteLoginYaNoLoPide() = runTest {
        val sesion = sesionConOtp()
        assertTrue(repo.esValido(sesion.accessToken))
        assertTrue(sesion.expiraEnSegundos <= 900)

        val segundoLogin = (login() as Resultado.Exito).valor
        assertTrue(segundoLogin is ResultadoLogin.Autenticado)
    }

    @Test
    fun otroDispositivoVuelveAPedirOtp() = runTest {
        sesionConOtp()
        instalacion = "instalacion-2"
        assertEquals(Resultado.Exito(ResultadoLogin.RequiereOtp), login())
    }

    @Test
    fun otpIncorrectoOSinLoginPendienteFalla() = runTest {
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), repo.verificarOtp(CODIGO_OTP_DEMO)) // sin login previo
        login()
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), repo.verificarOtp("000000"))
    }

    // PROV-SEC-001: bloqueo al tercer intento fallido, incluso si luego llega la contraseña correcta.
    @Test
    fun laTerceraCredencialIncorrectaBloqueaLaCuenta() = runTest {
        val mal: suspend () -> Resultado<ResultadoLogin> = { repo.iniciarSesion(CORREO_PACIENTE_DEMO, "incorrecta") }
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), mal())
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), mal())
        assertEquals(Resultado.Fallo(ApiError.CuentaBloqueada), mal())
        assertEquals(Resultado.Fallo(ApiError.CuentaBloqueada), login())
    }

    @Test
    fun correoInexistenteYContrasenaErroneaSeVenIgual() = runTest {
        val inexistente = repo.iniciarSesion("nadie@vitae.test", CONTRASENA_DEMO)
        val erronea = repo.iniciarSesion(CORREO_PACIENTE_DEMO, "incorrecta")
        assertEquals(inexistente, erronea)
    }

    // RF-002, escenario 2.
    @Test
    fun elPersonalRecibe403() = runTest {
        assertEquals(Resultado.Fallo(ApiError.SinPermisos), repo.iniciarSesion(CORREO_PERSONAL_DEMO, CONTRASENA_DEMO))
    }

    @Test
    fun elRefreshRotaYReutilizarloInvalidaLaSesionEntera() = runTest {
        val primera = sesionConOtp()
        val segunda = (repo.renovar(primera.refreshToken) as Resultado.Exito).valor
        assertNotEquals(primera.refreshToken, segunda.refreshToken)
        assertTrue(repo.esValido(segunda.accessToken))

        // El refresh viejo ya se usó: reutilizarlo fuerza nuevo login y revoca también la sesión vigente.
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), repo.renovar(primera.refreshToken))
        assertFalse(repo.esValido(segunda.accessToken))
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), repo.renovar(segunda.refreshToken))
    }

    // PROV-SEC-002, escenario 2: tras el logout el access token anterior deja de valer.
    @Test
    fun cerrarSesionRevocaElToken() = runTest {
        val sesion = sesionConOtp()
        repo.cerrarSesion(sesion.accessToken)
        assertFalse(repo.esValido(sesion.accessToken))
        assertEquals(Resultado.Fallo(ApiError.SesionInvalida), repo.renovar(sesion.refreshToken))
    }

    @Test
    fun pkceSoloEntregaTokensConElVerifierCorrecto() {
        val verifier = Pkce.generarVerifier()
        val challenge = Pkce.challengeS256(verifier)

        val codigoMalo = repo.autorizar(CORREO_PACIENTE_DEMO, challenge)
        assertNull(repo.canjear(codigoMalo, Pkce.generarVerifier()))
        assertNull(repo.canjear(codigoMalo, verifier)) // el código es de un solo uso

        val codigoBueno = repo.autorizar(CORREO_PACIENTE_DEMO, challenge)
        assertNotNull(repo.canjear(codigoBueno, verifier))
    }
}
