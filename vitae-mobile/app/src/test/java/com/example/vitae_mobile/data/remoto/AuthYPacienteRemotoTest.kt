package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.auth.ResultadoLogin
import com.example.vitae_mobile.data.auth.Sesion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Login, renovación y cierre contra los endpoints de `auth`, y la elección del paciente (`/pacientes`). */
class AuthYPacienteRemotoTest : BackendTest() {
    private val auth get() = AuthRemoto(api, backend)

    @Test
    fun elLoginManda_idUsuarioYPassword_yDevuelveLaSesion() = runTest {
        token = null // aún no hay sesión
        responder(fixture("auth-login"))

        val login = exito(auth.iniciarSesion("MED-001", "clave-de-prueba")) as ResultadoLogin.Autenticado

        // El backend no tiene refresh token: el access token hace de los dos y dura 900 s.
        assertEquals(Sesion("TOKEN_DE_PRUEBA", "TOKEN_DE_PRUEBA", 900), login.sesion)
        assertEquals("MED-001", backend.usuarioId)
        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/auth/login", p.url.encodedPath)
        assertEquals("""{"id_usuario":"MED-001","password":"clave-de-prueba"}""", p.body?.utf8())
        assertNull("el login no lleva Bearer", p.headers["Authorization"])
    }

    @Test
    fun loginConErroresDelBackend() = runTest {
        mapOf(401 to ApiError.SesionInvalida, 423 to ApiError.CuentaBloqueada, 403 to ApiError.SinPermisos, 500 to ApiError.Servidor).forEach { (codigo, esperado) ->
            responder(fixture("auth-login-error"), codigo)
            assertEquals("HTTP $codigo", esperado, error(auth.iniciarSesion("MED-001", "mala")))
        }
        assertNull(backend.usuarioId)
    }

    @Test
    fun renovarUsaElTokenVigenteComoBearer() = runTest {
        responder(fixture("auth-refresh"))

        val sesion = exito(auth.renovar("TOKEN-VIEJO"))

        assertEquals(Sesion("TOKEN_DE_PRUEBA", "TOKEN_DE_PRUEBA", 900), sesion)
        val p = peticion()
        assertEquals("/api/v1/auth/refresh", p.url.encodedPath)
        assertEquals("Bearer TOKEN-VIEJO", p.headers["Authorization"])
    }

    @Test
    fun cerrarSesionRevocaElTokenIndicado() = runTest {
        responder("""{"status":"success","message":"Sesión cerrada y token revocado correctamente"}""")

        exito(auth.cerrarSesion("TOKEN-A-REVOCAR"))

        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/auth/logout", p.url.encodedPath)
        assertEquals("Bearer TOKEN-A-REVOCAR", p.headers["Authorization"])
    }

    @Test
    fun elBackendNoTieneOtp() = runTest {
        assertEquals(ApiError.Desconocido, error(auth.verificarOtp("123456")))
    }

    @Test
    fun laListaDePacientesSaleDeLaPaginaDeLaravel() = runTest {
        responder(fixture("pacientes-lista"))

        val pacientes = exito(backend.pacientes())

        assertEquals(3, pacientes.size)
        assertEquals("Paciente Sintético Alfa", pacientes[0].nombre)
        assertEquals("SINT900101HDFR01", pacientes[0].curp)
        // El paciente de urgencias sin identificar no tiene CURP: se reconoce por su folio.
        assertNull(pacientes[2].curp)
        assertEquals("URG-NN-SINT01", pacientes[2].folioUrgencias)
        assertEquals("/api/v1/pacientes", peticion().url.encodedPath)
    }

    @Test
    fun elegirGuardaElPacienteConSuExpedienteYSeguro_yOlvidarLoBorra() = runTest {
        responder(fixture("paciente-detalle"))

        val paciente = exito(backend.elegir(ID_ALFA))

        assertEquals(PacienteActivo(ID_ALFA, "EXP-SINT01", "Paciente Sintético Alfa", true), paciente)
        assertEquals(paciente, backend.activo)
        assertEquals("/api/v1/pacientes/$ID_ALFA", peticion().url.encodedPath)

        backend.olvidar()
        assertNull(backend.activo)
        assertNull(backend.usuarioId)
    }

    @Test
    fun siElegirFallaNoQuedaNingunPacienteActivo() = runTest {
        responder("""{"message":"No query results"}""", 404)

        assertEquals(ApiError.Desconocido, error(backend.elegir(ID_ALFA)))
        assertNull(backend.activo)
    }
}
