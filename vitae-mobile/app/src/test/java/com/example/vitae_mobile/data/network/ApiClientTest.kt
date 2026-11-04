package com.example.vitae_mobile.data.network

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

private interface PruebaApi {
    @GET("ping")
    suspend fun ping(): ApiEnvelope<String>
}

class ApiClientTest {
    private lateinit var servidor: MockWebServer
    private var token: String? = "abc"
    private var avisos = 0

    private fun api(): PruebaApi =
        ApiClient.crear(servidor.url("/api/v1/").toString(), { token }, { avisos++ }).create(PruebaApi::class.java)

    @Before
    fun iniciar() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun cerrar() = servidor.close()

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse.Builder().code(codigo).body(cuerpo).build())
    }

    private fun errorDe(resultado: Resultado<*>): ApiError = (resultado as Resultado.Fallo).error

    @Test
    fun respuestaExitosaLlevaBearerYNoEnviaIdPaciente() = runTest {
        responder(200, """{"status":"success","data":"pong","extra":1}""")

        val resultado = llamar { api().ping() }

        assertEquals("pong", (resultado as Resultado.Exito).valor.data)
        val peticion = servidor.takeRequest()
        assertEquals("Bearer abc", peticion.headers["Authorization"])
        assertSinIdPaciente(peticion.url.toString(), peticion.headers, peticion.body?.utf8().orEmpty())
    }

    @Test
    fun codigosHttpSeTraducenAErroresDeLaApp() = runTest {
        val esperados = mapOf(
            401 to ApiError.SesionInvalida,
            403 to ApiError.SinPermisos,
            423 to ApiError.CuentaBloqueada,
            426 to ApiError.AppIncompatible,
            500 to ApiError.Servidor,
            503 to ApiError.Servidor,
            404 to ApiError.Desconocido,
        )
        esperados.forEach { (codigo, error) ->
            responder(codigo, """{"status":"error"}""")
            assertEquals("HTTP $codigo", error, errorDe(llamar { api().ping() }))
        }
    }

    // RF-003, escenario 1: cualquier 401 de una petición con sesión avisa.
    @Test
    fun un401ConSesionAvisaUnaVez() = runTest {
        responder(401)
        llamar { api().ping() }
        assertEquals(1, avisos)
    }

    // El 401 del login (sin token) es "credenciales incorrectas", no una sesión caducada.
    @Test
    fun un401SinSesionNoAvisa() = runTest {
        token = null
        responder(401)
        assertEquals(ApiError.SesionInvalida, errorDe(llamar { api().ping() }))
        assertEquals(0, avisos)
    }

    // RF-003, escenario 2: el 403 no cierra la sesión.
    @Test
    fun un403NoAvisa() = runTest {
        responder(403)
        llamar { api().ping() }
        assertEquals(0, avisos)
    }

    @Test
    fun servidorCaidoEsErrorDeRed() = runTest {
        val cliente = api()
        servidor.close()
        assertEquals(ApiError.Red, errorDe(llamar { cliente.ping() }))
    }

    @Test
    fun cuerpoIlegibleEsErrorDesconocido() = runTest {
        responder(200, "esto no es json")
        assertEquals(ApiError.Desconocido, errorDe(llamar { api().ping() }))
    }

    @Test
    fun sinTokenNoSeEnviaAuthorization() = runTest {
        token = null
        responder(200, """{"status":"success"}""")
        llamar { api().ping() }
        assertNull(servidor.takeRequest().headers["Authorization"])
    }

    @Test
    fun elAsertorDetectaIdPaciente() {
        assertTrue(contieneIdPaciente("https://x/api?id_paciente=1", Headers.headersOf(), ""))
        assertTrue(contieneIdPaciente("https://x/api/id_paciente/1", Headers.headersOf(), ""))
        assertTrue(contieneIdPaciente("https://x/api", Headers.headersOf(), """{"id_paciente":"1"}"""))
        assertFalse(contieneIdPaciente("https://x/api/mobile/citas", Headers.headersOf(), "{}"))
    }
}

/** RF-002.1: ninguna petición puede llevar id_paciente en ruta, query, cabeceras o cuerpo. */
fun contieneIdPaciente(url: String, cabeceras: Headers, cuerpo: String): Boolean =
    "id_paciente" in url || "id_paciente" in cuerpo || cabeceras.names().any { "id_paciente" in it.lowercase() }

fun assertSinIdPaciente(url: String, cabeceras: Headers, cuerpo: String) =
    assertFalse("La petición lleva id_paciente: $url", contieneIdPaciente(url, cabeceras, cuerpo))
