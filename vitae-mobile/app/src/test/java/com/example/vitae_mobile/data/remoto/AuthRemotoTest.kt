package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.data.auth.ResultadoLogin
import com.example.vitae_mobile.data.auth.Sesion
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Login, renovación y cierre contra los endpoints de `auth`; solo entran pacientes (RF-002). */
class AuthRemotoTest : BackendTest() {
    private val auth get() = AuthRemoto(api, backend)

    @Test
    fun elLoginDeUnPacienteManda_idUsuarioYPassword_yDevuelveLaSesion() = runTest {
        token = null // aún no hay sesión
        backend.usuarioId = null
        responder(fixture("auth-login"))

        val login = exito(auth.iniciarSesion("PAC-001", "clave-de-prueba")) as ResultadoLogin.Autenticado

        // El backend no tiene refresh token: el access token hace de los dos y dura 900 s.
        assertEquals(Sesion("TOKEN_DE_PRUEBA", "TOKEN_DE_PRUEBA", 900), login.sesion)
        assertEquals("PAC-001", backend.usuarioId)
        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/auth/login", p.url.encodedPath)
        assertEquals("""{"id_usuario":"PAC-001","password":"clave-de-prueba"}""", p.body?.utf8())
        assertNull("el login no lleva Bearer", p.headers["Authorization"])
    }

    // El portal móvil es solo para pacientes: la cuenta de personal se rechaza y su token se revoca en el servidor.
    @Test
    fun unaCuentaDePersonalSeRechazaYSuTokenSeRevoca() = runTest {
        token = null
        backend.usuarioId = null
        responder(fixture("auth-login-personal"))
        responder("""{"status":"success","message":"Sesión cerrada y token revocado correctamente"}""")

        assertEquals(ApiError.SinPermisos, error(auth.iniciarSesion("MED-001", "clave-de-prueba")))

        assertNull(backend.usuarioId)
        assertEquals("/api/v1/auth/login", peticion().url.encodedPath)
        val cierre = peticion()
        assertEquals("/api/v1/auth/logout", cierre.url.encodedPath)
        assertEquals("Bearer TOKEN_DE_PRUEBA", cierre.headers["Authorization"])
    }

    @Test
    fun loginConErroresDelBackend() = runTest {
        backend.usuarioId = null
        mapOf(401 to ApiError.SesionInvalida, 423 to ApiError.CuentaBloqueada, 403 to ApiError.SinPermisos, 500 to ApiError.Servidor).forEach { (codigo, esperado) ->
            responder(fixture("auth-login-error"), codigo)
            assertEquals("HTTP $codigo", esperado, error(auth.iniciarSesion("PAC-001", "mala")))
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
    fun olvidarBorraElUsuarioYLasSolicitudesArcoDeLaSesion() {
        backend.arcoEnviadas.add(
            SolicitudArco("ARCO-1", DerechoArco.Acceso, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 29), "RECIBIDA"),
        )

        backend.olvidar()

        assertNull(backend.usuarioId)
        assertEquals(emptyList<Any>(), backend.arcoEnviadas.toList())
    }

    // RNF-006: el portal móvil rechaza (400/426) las peticiones sin el nivel de API del dispositivo.
    @Test
    fun todasLasPeticionesLlevanElNivelDeApi() = runTest {
        token = null
        responder(fixture("auth-login"))
        auth.iniciarSesion("PAC-001", "x")
        responder(fixture("mobile-perfil"))
        PerfilRemoto(api).perfil()

        assertEquals("API-$NIVEL_API", peticion().headers["X-Client-Version"])
        assertEquals("API-$NIVEL_API", peticion().headers["X-Client-Version"])
    }
}
