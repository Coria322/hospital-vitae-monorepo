package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.network.ApiClient
import java.io.File
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
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
        api = ApiClient.crear(servidor.url("/api/v1/").toString(), { token }, {}).create(ApiBackend::class.java)
        backend = SesionBackend(api)
    }

    @After
    fun cerrarServidor() = servidor.close()

    protected fun fixture(nombre: String): String = File("src/test/resources/backend/$nombre.json").readText()

    protected fun responder(cuerpo: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse.Builder().code(codigo).body(cuerpo).build())
    }

    /** Elige al paciente sintético Alfa (con expediente EXP-SINT01 y seguro activo) como paciente activo. */
    protected suspend fun elegirPacienteAlfa() {
        responder(fixture("paciente-detalle"))
        assertEquals(true, backend.elegir(ID_ALFA) is Resultado.Exito)
        servidor.takeRequest() // la petición de elegir no interesa en cada prueba
    }

    protected fun peticion(): RecordedRequest = servidor.takeRequest()

    protected fun <T> exito(resultado: Resultado<T>): T = (resultado as? Resultado.Exito)?.valor ?: error("Se esperaba éxito: $resultado")

    protected fun error(resultado: Resultado<*>): ApiError = (resultado as? Resultado.Fallo)?.error ?: error("Se esperaba un fallo: $resultado")

    companion object {
        const val ID_ALFA = "fb7d76f1-3a30-43d2-bea0-8eb350b6dbb7"
    }
}
