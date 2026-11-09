package com.example.vitae_mobile.data

import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.perfil.PerfilRepository
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK-M-015 (RF-011) y TASK-M-019 (RF-002.1): la app no puede escribir recursos clínicos, y `id_paciente` solo aparece
 * donde se declara el contrato del backend. Son guardas sobre el código: si alguien añade una operación de escritura o
 * usa el identificador en otro sitio, la prueba obliga a revisarlo. El lado servidor (403 o 405 a POST, PUT y DELETE)
 * es de TASK-W-080.
 *
 * Excepción decidida el 8 de noviembre de 2026: los endpoints actuales del backend son de personal y piden el id del
 * paciente en la ruta o el cuerpo (PLAN.md, decisión 16), así que `ApiBackend.kt` lo declara. En ningún otro archivo.
 */
class SoloLecturaTest {
    private fun metodos(tipo: Class<*>) = tipo.declaredMethods.map { it.name }.toSet()

    // Los repositorios clínicos solo leen: una función nueva (guardar, actualizar, eliminar...) rompe esta prueba.
    @Test
    fun losRepositoriosClinicosSoloExponenLecturas() {
        assertEquals(setOf("citas"), metodos(CitasRepository::class.java))
        assertEquals(setOf("perfil"), metodos(PerfilRepository::class.java))
        assertEquals(setOf("recetas", "receta"), metodos(RecetasRepository::class.java))
        assertEquals(setOf("resultados", "archivo"), metodos(ResultadosRepository::class.java))
    }

    // Lista blanca: lo único que la app escribe es auth, consentimientos y solicitudes ARCO.
    @Test
    fun lasEscriturasSonSoloLasDeLaListaBlanca() {
        assertEquals(setOf("iniciarSesion", "verificarOtp", "renovar", "cerrarSesion"), metodos(AuthRepository::class.java))
        assertEquals(setOf("aviso", "registrar"), metodos(PrivacidadRepository::class.java))
        assertEquals(setOf("solicitudes", "crear", "consultar"), metodos(ArcoRepository::class.java))
    }

    private val fuentes = listOf("src/main", "src/debug", "src/release").flatMap { carpeta ->
        File(carpeta).walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }.toList()
    }

    // Cualquier interfaz Retrofit con POST, PUT, DELETE o PATCH debe apuntar a la lista blanca.
    @Test
    fun ningunaEscrituraDeRetrofitSaleDeLaListaBlanca() {
        // Prefijos de los endpoints normales del backend; `consentimientos/{id}/revocaciones` cuelga de `consentimientos`.
        val permitidas = listOf("auth/login", "auth/refresh", "auth/logout", "consentimientos", "solicitudes-arco")
        // También la anotación calificada (@retrofit2.http.DELETE).
        val escritura = Regex("""@(?:[\w.]+\.)?(?:POST|PUT|DELETE|PATCH)\(\s*"([^"]*)"""")
        val fuera = fuentes.filter { it.extension == "kt" }.flatMap { archivo ->
            escritura.findAll(archivo.readText()).map { it.groupValues[1] }.filter { ruta -> permitidas.none { ruta.trimStart('/').startsWith(it) } }
        }.toList()
        assertTrue("Escrituras fuera de la lista blanca: $fuera", fuera.isEmpty())
        val http = Regex("""@(?:[\w.]+\.)?HTTP\(\s*method\s*=\s*"(?:POST|PUT|DELETE|PATCH)"""")
        assertTrue("@HTTP de escritura sin revisar", fuentes.none { http.containsMatchIn(it.readText()) })
    }

    // RF-002.1 con la excepción de arriba: solo el contrato del backend nombra `id_paciente`.
    @Test
    fun idPacienteSoloApareceEnElContratoDelBackend() {
        assertTrue("Las fuentes no se encontraron: ${File("src/main").absolutePath}", fuentes.isNotEmpty())
        val con = fuentes.filter { "id_paciente" in it.readText() }.map { it.path.replace('\\', '/').substringAfter("src/") }
        assertEquals(listOf("main/java/com/example/vitae_mobile/data/remoto/ApiBackend.kt"), con)
    }
}
