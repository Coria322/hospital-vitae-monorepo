package com.example.vitae_mobile.data.clinico

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.recetas.EstadoReceta
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Fija los datos simulados de debug de los que dependen las pruebas instrumentadas.
class MockClinicoTest {
    private val ahora = Instant.parse("2026-11-04T12:00:00Z")
    private val reloj = Clock.fixed(ahora, ZoneOffset.UTC)

    @Test
    fun hayDosCitasFuturasYUnaPasada() = runTest {
        val citas = (MockCitasRepository(reloj).citas() as Resultado.Exito).valor
        assertEquals(listOf("Programada", "Programada", "Atendida"), citas.map { it.estado })
        assertEquals(listOf(true, true, false), citas.map { it.fechaHora > ahora })
    }

    @Test
    fun lasTresRecetasCubrenLosTresEstados() = runTest {
        val recetas = (MockRecetasRepository().recetas() as Resultado.Exito).valor
        assertEquals(listOf(EstadoReceta.Activa, EstadoReceta.Dispensada, EstadoReceta.Cancelada), recetas.map { it.estado })
        assertEquals(listOf(true, false, false), recetas.map { it.qrVigente })
        assertTrue(recetas.all { it.codigoQr != null && it.dosis == "500 mg" && it.via == "Oral" })
    }

    @Test
    fun elDetalleDeUnaRecetaPropiaSeLee() = runTest {
        val receta = (MockRecetasRepository().receta("rec-002") as Resultado.Exito).valor
        assertEquals("Amoxicilina 500 mg cápsulas", receta.medicamento)
        assertEquals("10 días", receta.duracion)
        assertEquals(EstadoReceta.Dispensada, receta.estado)
        assertFalse(receta.qrVigente)
    }

    // Una receta ajena responde 403, como el servidor real.
    @Test
    fun unaRecetaAjenaResponde403() = runTest {
        assertEquals(Resultado.Fallo(ApiError.SinPermisos), MockRecetasRepository().receta("de-otro-paciente"))
    }

    @Test
    fun hayUnPdfYUnaImagen() = runTest {
        val resultados = (MockResultadosRepository(File("no-se-usa"), reloj).resultados() as Resultado.Exito).valor
        assertEquals(listOf("application/pdf", "image/png"), resultados.map { it.mime })
        assertEquals(listOf(true, false), resultados.map { it.esPdf })
        assertEquals(listOf(false, true), resultados.map { it.esImagen })
    }

    // RF-010, escenario 2: un resultado ajeno responde 403 y no se crea ningún archivo.
    @Test
    fun unResultadoAjenoResponde403SinCrearArchivos() = runTest {
        val directorio = File.createTempFile("res", "dir").apply { delete() }
        val respuesta = MockResultadosRepository(directorio, reloj).archivo("de-otro-paciente") { }
        assertEquals(Resultado.Fallo(ApiError.SinPermisos), respuesta)
        assertFalse(directorio.exists())
    }
}
