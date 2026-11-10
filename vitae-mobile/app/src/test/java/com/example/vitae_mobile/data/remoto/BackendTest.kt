package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.network.ApiClient
import java.io.File
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before

/**
 * Base de las pruebas de los repositorios remotos: un MockWebServer sirve respuestas reales del backend (capturadas con
 * datos sintéticos en `src/test/resources/backend`) y se comprueba qué manda la app y cómo lo interpreta.
 */
abstract class BackendTest {
    protected lateinit var servidor: MockWebServer
    protected lateinit var api: ApiBackend
    protected lateinit var backend: SesionBackend

    /** El token que la app tendría en ese momento; null antes de iniciar sesión. */
    protected var token: String? = "TOKEN"

    @Before
    fun iniciarServidor() {
        servidor = MockWebServer().apply { start() }
        api = ApiClient.crear(servidor.url("/api/v1/").toString(), { token }, {}, nivelApi = NIVEL_API).create(ApiBackend::class.java)
        backend = SesionBackend().apply { usuarioId = "PAC-001" }
    }

    @After
    fun cerrarServidor() = servidor.close()

    protected fun fixture(nombre: String): String = File("src/test/resources/backend/$nombre.json").readText()

    protected fun responder(cuerpo: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse.Builder().code(codigo).body(cuerpo).build())
    }

    protected fun peticion(): RecordedRequest = servidor.takeRequest()

    protected fun <T> exito(resultado: Resultado<T>): T = (resultado as? Resultado.Exito)?.valor ?: error("Se esperaba éxito: $resultado")

    protected fun error(resultado: Resultado<*>): ApiError = (resultado as? Resultado.Fallo)?.error ?: error("Se esperaba un fallo: $resultado")

    companion object {
        const val NIVEL_API = 31
    }
}
