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
 * TASK-M-015 (RF-011) y TASK-M-019 (RF-002.1): la app no puede escribir recursos clínicos y `id_paciente` no aparece en
 * ningún archivo: la identidad sale del token del paciente. Son guardas sobre el código: si alguien añade una operación
 * de escritura, una lectura fuera del portal móvil o el identificador, la prueba obliga a revisarlo. El lado servidor
 * (403 a POST, PUT y DELETE sobre recursos clínicos) lo cubre el backend (`bloquearMutacionClinica`).
 *
 * Única lectura fuera de `/mobile`: el contenido de un resultado (PLAN.md, decisión 19).
 */
class SoloLecturaTest {
    private fun metodos(tipo: Class<*>) = tipo.declaredMethods
        .filter { !it.isSynthetic && !it.name.contains('$') }
        .map { it.name }
        .toSet()

    // Los repositorios clínicos solo leen (salvo solicitud de cita móvil PROV-RF-002):
    @Test
    fun losRepositoriosClinicosSoloExponenLecturas() {
        // Pedir una cita (PROV-RF-002) es la única escritura de este grupo: no toca ningún registro clínico.
        assertEquals(setOf("citas", "solicitar", "solicitarCita"), metodos(CitasRepository::class.java))
        assertEquals(setOf("perfil"), metodos(PerfilRepository::class.java))
        assertEquals(setOf("recetas", "receta"), metodos(RecetasRepository::class.java))
        assertEquals(setOf("resultados", "archivo"), metodos(ResultadosRepository::class.java))
    }

    // Lista blanca: lo único que la app escribe es auth, la solicitud de una cita, consentimientos y solicitudes ARCO.
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
        val permitidas = listOf(
            "auth/login",
            "auth/refresh",
            "auth/logout",
            "consentimientos",
            "solicitudes-arco",
            "mobile/citas",
            "mobile/consentimientos",
            "mobile/solicitudes-arco",
        )
        // También la anotación calificada (@retrofit2.http.DELETE).
        val escritura = Regex("""@(?:[\w.]+\.)?(?:POST|PUT|DELETE|PATCH)\(\s*"([^"]*)"""")
        val fuera = fuentes.filter { it.extension == "kt" }.flatMap { archivo ->
            escritura.findAll(archivo.readText()).map { it.groupValues[1] }.filter { ruta -> permitidas.none { ruta.trimStart('/').startsWith(it) } }
        }.toList()
        assertTrue("Escrituras fuera de la lista blanca: $fuera", fuera.isEmpty())
        val http = Regex("""@(?:[\w.]+\.)?HTTP\(\s*method\s*=\s*"(?:POST|PUT|DELETE|PATCH)"""")
        assertTrue("@HTTP de escritura sin revisar", fuentes.none { http.containsMatchIn(it.readText()) })
    }

    // Las lecturas salen del portal móvil; la única excepción es el contenido de un resultado.
    @Test
    fun lasLecturasSalenDelPortalMovilSalvoElContenidoDeUnResultado() {
        val lectura = Regex("""@(?:[\w.]+\.)?GET\(\s*"([^"]*)"""")
        val fuera = fuentes.filter { it.extension == "kt" }.flatMap { archivo ->
            lectura.findAll(archivo.readText()).map { it.groupValues[1] }.filter { !it.trimStart('/').startsWith("mobile/") }
        }.toList()
        assertEquals(listOf("solicitudes-estudio/{id}/resultados"), fuera)
    }

    // RF-002.1: ninguna petición nombra `id_paciente`; la identidad sale del token.
    @Test
    fun idPacienteNoApareceEnNingunArchivo() {
        assertTrue("Las fuentes no se encontraron: ${File("src/main").absolutePath}", fuentes.isNotEmpty())
        val con = fuentes.filter { "id_paciente" in it.readText() }.map { it.path.replace('\\', '/').substringAfter("src/") }
        assertEquals(emptyList<String>(), con)
    }
}
