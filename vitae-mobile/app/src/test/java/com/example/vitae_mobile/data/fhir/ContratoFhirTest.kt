package com.example.vitae_mobile.data.fhir

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK-M-018 / RNF-007: pruebas de contrato FHIR sobre archivos. Todo `.json` de `contrato/fhir/valido` debe leerse y
 * todo el de `contrato/fhir/invalido` debe dar un error controlado, nunca una excepción. El parser se elige por el
 * prefijo del nombre: `patient-`, `receta-` o `bundle-`.
 *
 * Cuando exista la colección compartida (TASK-W-091), sus ejemplos se copian a esas carpetas sin tocar el código.
 */
class ContratoFhirTest {
    private val raiz = File("src/test/resources/contrato/fhir")

    private fun fixtures(carpeta: String): List<File> =
        File(raiz, carpeta).listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    private fun leer(archivo: File): Resultado<*> {
        val texto = archivo.readText()
        return when {
            archivo.name.startsWith("patient-") -> FhirParser.perfilDe(texto)
            archivo.name.startsWith("receta-") -> FhirParser.recetaDe(texto)
            archivo.name.startsWith("bundle-") -> FhirParser.recetasDe(texto)
            else -> error("Prefijo no reconocido: ${archivo.name}")
        }
    }

    // Que una carpeta vacía no haga pasar la prueba por no probar nada.
    @Test
    fun hayEjemplosDeLosTresRecursos() {
        listOf("valido", "invalido").forEach { carpeta ->
            val nombres = fixtures(carpeta).map { it.name.substringBefore('-') }.toSet()
            assertEquals("$carpeta: faltan recursos", setOf("patient", "receta", "bundle"), nombres)
        }
    }

    @Test
    fun cadaEjemploValidoSeLee() {
        fixtures("valido").forEach { archivo ->
            assertTrue("${archivo.name} debía leerse: ${leer(archivo)}", leer(archivo) is Resultado.Exito)
        }
    }

    @Test
    fun cadaEjemploInvalidoDaErrorControlado() {
        fixtures("invalido").forEach { archivo ->
            val resultado = leer(archivo) // si lanzara una excepción, la prueba fallaría aquí
            assertTrue("${archivo.name} debía fallar: $resultado", resultado is Resultado.Fallo)
            assertEquals(archivo.name, ApiError.Desconocido, (resultado as Resultado.Fallo).error)
        }
    }

    // El ejemplo completo conserva los datos que la app muestra y ninguno más.
    @Test
    fun elPacienteCompletoDaLosDatosDelPerfil() {
        val perfil = (FhirParser.perfilDe(File(raiz, "valido/patient-completo.json").readText()) as Resultado.Exito).valor
        assertEquals("Ana María Pérez Demo", perfil.nombre)
        assertEquals("DEMO900517MDFRRN09", perfil.curp)
        assertEquals("1990-05-17", perfil.fechaNacimiento)
        assertEquals(true, perfil.seguroActivo)
    }

    @Test
    fun elBundleTrae2RecetasYElVacioNinguna() {
        val dos = (FhirParser.recetasDe(File(raiz, "valido/bundle-dos-recetas.json").readText()) as Resultado.Exito).valor
        val vacio = (FhirParser.recetasDe(File(raiz, "valido/bundle-vacio.json").readText()) as Resultado.Exito).valor
        assertEquals(listOf("rec-demo-1", "rec-demo-2"), dos.map { it.id })
        assertTrue(vacio.isEmpty())
    }
}
