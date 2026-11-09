package com.example.vitae_mobile.data.privacidad

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.MockArcoRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Fija las reglas de los servidores simulados de debug de las que dependen las pruebas instrumentadas.
class MockPrivacidadYArcoTest {
    private val reloj = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC)

    private suspend fun MockPrivacidadRepository.avisoVigente() = (aviso() as Resultado.Exito).valor

    private suspend fun MockPrivacidadRepository.aceptar(finalidades: List<String> = emptyList()) =
        registrar(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, versionVigente, finalidades, AccionConsentimiento.ACEPTAR))

    @Test
    fun alPrincipioElAvisoNoEstaAceptadoYLasFinalidadesVanDesmarcadas() = runTest {
        val aviso = MockPrivacidadRepository(reloj).avisoVigente()
        assertFalse(aviso.aceptada)
        assertTrue(aviso.finalidadesSecundarias.none { it.aceptada })
        assertEquals(2, aviso.finalidadesSecundarias.size)
    }

    @Test
    fun aceptarElAvisoLoMarcaAceptadoConLasFinalidadesElegidas() = runTest {
        val repo = MockPrivacidadRepository(reloj)

        repo.aceptar(listOf("ESTADISTICA"))

        val aviso = repo.avisoVigente()
        assertTrue(aviso.aceptada)
        assertEquals(listOf("ESTADISTICA"), aviso.finalidadesSecundarias.filter { it.aceptada }.map { it.codigo })
    }

    // Caso borde: un cambio sustancial del aviso exige aceptarlo otra vez.
    @Test
    fun unaVersionNuevaVuelveAPedirLaAceptacion() = runTest {
        val repo = MockPrivacidadRepository(reloj)
        repo.aceptar()
        assertTrue(repo.avisoVigente().aceptada)

        repo.publicarNuevaVersion()

        assertEquals("2.0", repo.versionVigente)
        assertFalse(repo.avisoVigente().aceptada)
    }

    @Test
    fun noSeAceptaUnaVersionQueYaNoEsLaVigente() = runTest {
        val repo = MockPrivacidadRepository(reloj)
        val respuesta = repo.registrar(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "0.9", emptyList(), AccionConsentimiento.ACEPTAR))
        assertEquals(Resultado.Fallo(ApiError.Desconocido), respuesta)
        assertTrue(repo.registros.isEmpty())
    }

    // CON-016: revocar no modifica el registro anterior, añade uno nuevo.
    @Test
    fun revocarCreaUnRegistroNuevoYNoTocaElAnterior() = runTest {
        val repo = MockPrivacidadRepository(reloj)
        repo.aceptar(listOf("RECORDATORIOS", "ESTADISTICA"))
        val antes = repo.registros

        val revocacion = repo.registrar(
            SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, repo.versionVigente, listOf("RECORDATORIOS"), AccionConsentimiento.REVOCAR)
        )

        assertTrue(revocacion is Resultado.Exito)
        assertEquals(antes, repo.registros.take(antes.size))
        assertEquals(antes.size + 1, repo.registros.size)
        assertEquals(AccionConsentimiento.REVOCAR, repo.registros.last().solicitud.accion)
        assertEquals(listOf("ESTADISTICA"), repo.avisoVigente().finalidadesSecundarias.filter { it.aceptada }.map { it.codigo })
    }

    @Test
    fun laSolicitudArcoTraeFolioYPlazosDe20Y15Dias() = runTest {
        val repo = MockArcoRepository(reloj)

        val creada = (repo.crear(DerechoArco.Acceso) as Resultado.Exito).valor

        assertEquals("ARCO-0001", creada.folio)
        assertEquals(LocalDate.parse("2026-11-04"), creada.fechaRecepcion)
        assertEquals(LocalDate.parse("2026-11-24"), creada.respuestaLimite)
        assertEquals(LocalDate.parse("2026-12-09"), creada.ejecucionLimite)
        assertEquals("Recibida", creada.estado)
    }

    @Test
    fun lasSolicitudesArcoSeListanDeLaMasNuevaALaMasAntigua() = runTest {
        val repo = MockArcoRepository(reloj)
        repo.crear(DerechoArco.Acceso)
        repo.crear(DerechoArco.Cancelacion)

        val lista = (repo.solicitudes() as Resultado.Exito).valor

        assertEquals(listOf("ARCO-0002", "ARCO-0001"), lista.map { it.folio })
        assertEquals(listOf(DerechoArco.Cancelacion, DerechoArco.Acceso), lista.map { it.derecho })
    }

    @Test
    fun consultarUnFolioPropioDevuelveSuEstadoYUnoAjenoResponde403() = runTest {
        val repo = MockArcoRepository(reloj)
        repo.crear(DerechoArco.Oposicion)

        assertEquals("Recibida", (repo.consultar("ARCO-0001") as Resultado.Exito).valor.estado)
        assertEquals(Resultado.Fallo(ApiError.SinPermisos), repo.consultar("ARCO-9999"))
    }
}
