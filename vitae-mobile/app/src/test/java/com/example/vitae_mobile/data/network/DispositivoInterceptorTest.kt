package com.example.vitae_mobile.data.network

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

private interface DispositivoApi {
    @GET("ping")
    suspend fun ping(): ApiEnvelope<String>
}

class DispositivoInterceptorTest {
    private lateinit var servidor: MockWebServer
    private var bloqueado = false
    private var incompatibles = 0

    private fun api(): DispositivoApi = ApiClient.crear(
        servidor.url("/api/v1/").toString(),
        token = { "abc" },
        alSesionInvalida = {},
        bloqueado = { bloqueado },
        alIncompatible = { incompatibles++ },
    ).create(DispositivoApi::class.java)

    @Before
    fun iniciar() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun cerrar() = servidor.close()

    private fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse.Builder().code(codigo).body(cuerpo).build())
    }

    // PROV-SEC-011: con el dispositivo bloqueado no sale ni una petición, y la UI lo ve como un fallo de red.
    @Test
    fun conElDispositivoBloqueadoNoSaleNingunaPeticion() = runTest {
        bloqueado = true
        responder(200, """{"status":"success","data":"pong"}""")

        val resultado = llamar { api().ping() }

        assertEquals(ApiError.Red, (resultado as Resultado.Fallo).error)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun alDesbloquearSeVuelveAEnviar() = runTest {
        bloqueado = true
        responder(200, """{"status":"success","data":"pong"}""")
        val cliente = api()
        llamar { cliente.ping() }

        bloqueado = false
        val resultado = llamar { cliente.ping() }

        assertEquals("pong", (resultado as Resultado.Exito).valor.data)
        assertEquals(1, servidor.requestCount)
    }

    // RNF-006: el 426 avisa una vez y el error sigue llegando a quien llamó.
    @Test
    fun un426MarcaLaAppComoIncompatible() = runTest {
        responder(426)

        val resultado = llamar { api().ping() }

        assertEquals(ApiError.AppIncompatible, (resultado as Resultado.Fallo).error)
        assertEquals(1, incompatibles)
    }

    @Test
    fun otrosCodigosNoMarcanIncompatible() = runTest {
        listOf(200, 401, 403, 423, 500).forEach { codigo ->
            responder(codigo, """{"status":"success"}""")
            llamar { api().ping() }
        }
        assertEquals(0, incompatibles)
    }
}
