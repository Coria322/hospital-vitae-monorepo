package com.example.vitae_mobile.data.fhir

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.perfil.Perfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// RNF-007 / TASK-M-010: la app valida los recursos FHIR y falla de forma controlada.
class FhirParserTest {
    private val pacienteValido = """
        {
          "resourceType": "Patient",
          "identifier": [{ "system": "$SISTEMA_CURP", "value": "DEMO900517MDFRRN09" }],
          "name": [{ "family": "Pérez Demo", "given": ["Ana", "María"] }],
          "birthDate": "1990-05-17",
          "extension": [{ "url": "$EXTENSION_SEGURO_ACTIVO", "valueBoolean": true }]
        }
    """.trimIndent()

    private fun perfil(texto: String): Perfil = (FhirParser.perfilDe(texto) as Resultado.Exito).valor

    private fun error(texto: String): ApiError = (FhirParser.perfilDe(texto) as Resultado.Fallo).error

    // Escenario 1: un Patient válido muestra CURP, nombre y fecha de nacimiento.
    @Test
    fun leeCurpNombreFechaYSeguro() {
        assertEquals(
            Perfil(nombre = "Ana María Pérez Demo", curp = "DEMO900517MDFRRN09", fechaNacimiento = "1990-05-17", seguroActivo = true),
            perfil(pacienteValido),
        )
    }

    @Test
    fun ignoraLosCamposFhirQueNoConoce() {
        val conExtras = """{"resourceType":"Patient","id":"x","meta":{"versionId":"1"},"active":true,"gender":"female","name":[{"text":"Ana"}]}"""
        assertEquals("Ana", perfil(conExtras).nombre)
    }

    @Test
    fun elNombrePrefiereTextoYSinNombreQuedaVacio() {
        assertEquals("Ana P.", perfil("""{"resourceType":"Patient","name":[{"text":"Ana P.","family":"Pérez","given":["Ana"]}]}""").nombre)
        assertEquals("Pérez", perfil("""{"resourceType":"Patient","name":[{"family":"Pérez"}]}""").nombre)
        assertNull(perfil("""{"resourceType":"Patient"}""").nombre)
    }

    @Test
    fun loQueFaltaQuedaVacioSinInventarValores() {
        val minimo = perfil("""{"resourceType":"Patient","name":[{"text":"Ana"}]}""")
        assertNull(minimo.curp)
        assertNull(minimo.fechaNacimiento)
        assertNull(minimo.seguroActivo)
    }

    @Test
    fun unIdentificadorDeOtroSistemaNoSeTomaComoCurp() {
        val otro = """{"resourceType":"Patient","identifier":[{"system":"urn:otro","value":"123"}]}"""
        assertNull(perfil(otro).curp)
    }

    // Escenario 2: respuesta sin resourceType → error controlado, sin excepción.
    @Test
    fun sinResourceTypeEsErrorControlado() {
        assertEquals(ApiError.Desconocido, error("""{"name":[{"text":"Ana"}]}"""))
    }

    @Test
    fun unRecursoDeOtroTipoEsErrorControlado() {
        assertEquals(ApiError.Desconocido, error("""{"resourceType":"Observation"}"""))
    }

    @Test
    fun jsonIlegibleEsErrorControlado() {
        listOf("esto no es json", "", "[]", "null", """{"resourceType":""").forEach {
            assertEquals("entrada: '$it'", ApiError.Desconocido, error(it))
        }
    }

    @Test
    fun unTipoIncorrectoEnUnCampoEsErrorControlado() {
        assertEquals(ApiError.Desconocido, error("""{"resourceType":"Patient","name":"Ana"}"""))
    }
}
