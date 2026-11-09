package com.example.vitae_mobile.data.fhir

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.Receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-009 / RNF-007 / TASK-M-010: MedicationRequest tipado, con error controlado ante recursos inválidos.
class FhirRecetasTest {
    private fun extensiones(qrCodigo: String?, qrEstado: String?): String = listOfNotNull(
        qrCodigo?.let { """{ "url": "$EXTENSION_QR_CODIGO", "valueString": "$it" }""" },
        qrEstado?.let { """{ "url": "$EXTENSION_QR_ESTADO", "valueString": "$it" }""" },
    ).joinToString(",")

    private fun receta(
        id: String = "rec-1",
        status: String = "active",
        qrEstado: String? = "VIGENTE",
        qrCodigo: String? = "CODIGO-1",
    ) = """
        {
          "resourceType": "MedicationRequest",
          "id": "$id",
          "status": "$status",
          "medicationCodeableConcept": { "text": "Paracetamol 500 mg" },
          "dosageInstruction": [{
            "text": "1 cada 8 horas",
            "route": { "text": "Oral" },
            "doseAndRate": [{ "doseQuantity": { "value": 500.0, "unit": "mg" } }]
          }],
          "dispenseRequest": { "expectedSupplyDuration": { "value": 7, "unit": "días" } },
          "extension": [${extensiones(qrCodigo, qrEstado)}]
        }
    """.trimIndent()

    private fun leer(texto: String): Receta = (FhirParser.recetaDe(texto) as Resultado.Exito).valor

    private fun errorReceta(texto: String): ApiError = (FhirParser.recetaDe(texto) as Resultado.Fallo).error

    private fun errorLista(texto: String): ApiError = (FhirParser.recetasDe(texto) as Resultado.Fallo).error

    private fun bundle(vararg recursos: String) =
        """{ "resourceType": "Bundle", "type": "searchset", "entry": [${recursos.joinToString(",") { """{ "resource": $it }""" }}] }"""

    // Escenario 1 de RF-009: el detalle muestra medicamento, dosis, vía, duración y estado.
    @Test
    fun leeMedicamentoDosisViaDuracionYEstado() {
        val r = leer(receta())
        assertEquals("rec-1", r.id)
        assertEquals("Paracetamol 500 mg", r.medicamento)
        assertEquals("500 mg", r.dosis)
        assertEquals("Oral", r.via)
        assertEquals("7 días", r.duracion)
        assertEquals(EstadoReceta.Activa, r.estado)
        assertEquals("CODIGO-1", r.codigoQr)
        assertTrue(r.qrVigente)
    }

    // Escenario 2: una receta dispensada muestra estado "dispensada" y su QR no es válido.
    @Test
    fun unaRecetaConQrConsumidoEstaDispensadaYElQrNoEsValido() {
        val r = leer(receta(status = "active", qrEstado = "CONSUMIDO"))
        assertEquals(EstadoReceta.Dispensada, r.estado)
        assertFalse(r.qrVigente)
    }

    @Test
    fun elEstadoSalDelStatusFhirOQrConsumidoOCancelado() {
        assertEquals(EstadoReceta.Dispensada, leer(receta(status = "completed", qrEstado = "VIGENTE")).estado)
        assertEquals(EstadoReceta.Cancelada, leer(receta(status = "cancelled", qrEstado = "CANCELADO")).estado)
        assertEquals(EstadoReceta.Cancelada, leer(receta(status = "stopped", qrEstado = null)).estado)
        assertEquals(EstadoReceta.Cancelada, leer(receta(status = "active", qrEstado = "CANCELADO")).estado)
        assertEquals(EstadoReceta.Desconocido, leer(receta(status = "draft", qrEstado = null)).estado)
    }

    @Test
    fun soloUnaRecetaActivaConQrVigenteTieneQrValido() {
        assertFalse(leer(receta(status = "cancelled", qrEstado = "VIGENTE")).qrVigente)
        assertFalse(leer(receta(status = "active", qrEstado = null)).qrVigente)
        assertFalse(leer(receta(status = "active", qrEstado = "VIGENTE", qrCodigo = null)).qrVigente)
    }

    @Test
    fun sinCodigoQrNoSeDibujaNinguno() {
        assertNull(leer(receta(qrCodigo = null, qrEstado = null)).codigoQr)
    }

    @Test
    fun usaLosTextosDeRespaldoYNoInventaLoQueFalta() {
        val minima = """{"resourceType":"MedicationRequest","id":"r","status":"active",
            "medicationCodeableConcept":{"coding":[{"display":"Ibuprofeno"}]},
            "dosageInstruction":[{"text":"2 tabletas"}]}"""
        val r = leer(minima)
        assertEquals("Ibuprofeno", r.medicamento)
        assertEquals("2 tabletas", r.dosis)
        assertNull(r.via)
        assertNull(r.duracion)
    }

    @Test
    fun laDosisConDecimalesSeMuestraTalCual() {
        val r = leer("""{"resourceType":"MedicationRequest","id":"r","dosageInstruction":[{"doseAndRate":[{"doseQuantity":{"value":2.5,"unit":"ml"}}]}]}""")
        assertEquals("2.5 ml", r.dosis)
    }

    // RNF-007, escenario 2: recursos que no validan → error controlado.
    @Test
    fun unRecursoInvalidoEsErrorControlado() {
        assertEquals(ApiError.Desconocido, errorReceta("""{"id":"r","status":"active"}""")) // sin resourceType
        assertEquals(ApiError.Desconocido, errorReceta("""{"resourceType":"Patient","id":"r"}"""))
        assertEquals(ApiError.Desconocido, errorReceta("""{"resourceType":"MedicationRequest","status":"active"}""")) // sin id
        assertEquals(ApiError.Desconocido, errorReceta("""{"resourceType":"MedicationRequest","id":"  "}"""))
        assertEquals(ApiError.Desconocido, errorReceta("no es json"))
        assertEquals(ApiError.Desconocido, errorReceta("""{"resourceType":"MedicationRequest","id":"r","dosageInstruction":"mal"}"""))
    }

    @Test
    fun unBundleValidoDevuelveTodasLasRecetas() {
        val lista = (FhirParser.recetasDe(bundle(receta("a"), receta("b", status = "completed"))) as Resultado.Exito).valor
        assertEquals(listOf("a", "b"), lista.map { it.id })
    }

    @Test
    fun unBundleVacioEsUnaListaVacia() {
        assertEquals(emptyList<Receta>(), (FhirParser.recetasDe(bundle()) as Resultado.Exito).valor)
    }

    @Test
    fun unBundleConUnaEntradaInvalidaSeRechazaCompleto() {
        assertEquals(ApiError.Desconocido, errorLista(bundle(receta("a"), """{"resourceType":"Observation","id":"o"}""")))
        assertEquals(ApiError.Desconocido, errorLista("""{"resourceType":"Bundle","entry":[{}]}"""))
        assertEquals(ApiError.Desconocido, errorLista(bundle(receta("a"), """{"resourceType":"MedicationRequest"}""")))
    }

    @Test
    fun unaListaQueNoEsBundleSeRechaza() {
        assertEquals(ApiError.Desconocido, errorLista("""{"entry":[]}"""))
        assertEquals(ApiError.Desconocido, errorLista(receta()))
        assertEquals(ApiError.Desconocido, errorLista("[]"))
    }
}
