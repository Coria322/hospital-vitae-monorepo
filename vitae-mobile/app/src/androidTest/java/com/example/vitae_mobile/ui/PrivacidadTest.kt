package com.example.vitae_mobile.ui

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-012: aviso de privacidad y consentimientos (PROV-RN-008). Cada test parte de un paciente nuevo para el
 * servidor simulado; [AppTest.entrar] acepta el aviso si hace falta.
 */
@RunWith(AndroidJUnit4::class)
class PrivacidadTest : AppTest() {
    private val recordatorios = "Recibir recordatorios y comunicaciones del hospital."

    @Before
    fun pacienteNuevo() = privacidad.reiniciar()

    /** Deja la app en la puerta del aviso con una versión que el paciente aún no aceptó. */
    private fun llegarAlAviso() {
        privacidad.publicarNuevaVersion()
        abrirApp() // actividad nueva: si ya hay sesión, arranca en la puerta del aviso
        entrarHastaElAviso()
        esperar { visible(aceptarAviso) }
    }

    // Escenario 1: el paciente ve el aviso antes de entrar y sin nada marcado de antemano.
    @Test
    fun elAvisoSeMuestraAntesDeInicioYLasFinalidadesOpcionalesVanDesmarcadas() {
        llegarAlAviso()

        assertTrue(visible(texto(R.string.pantalla_aviso)))
        assertTrue(visible("Texto de ejemplo", subcadena = true))
        assertTrue(visible("Versión ${privacidad.versionVigente}", subcadena = true))
        assertTrue(visible(texto(R.string.aviso_enlace_integral)))
        assertFalse("Inicio no debe verse antes de aceptar", visible(saludo))
        assertEquals(2, rule.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
        rule.onAllNodes(isToggleable()).assertAll(isOff())
        assertTrue(visible(texto(R.string.aviso_no_acepto)))
    }

    @Test
    fun aceptarRegistraLaVersionMostradaYLlevaAInicio() {
        llegarAlAviso()

        pulsar(aceptarAviso)

        esperar { visible(saludo) }
        val registro = privacidad.registros.single().solicitud
        assertEquals(TipoConsentimiento.AVISO_PRIVACIDAD, registro.tipo)
        assertEquals(AccionConsentimiento.ACEPTAR, registro.accion)
        assertEquals(privacidad.versionVigente, registro.versionDocumento)
        assertEquals(emptyList<String>(), registro.finalidades)
    }

    @Test
    fun lasFinalidadesMarcadasSeRegistranAlAceptar() {
        llegarAlAviso()

        rule.onNodeWithText(recordatorios).performClick()
        pulsar(aceptarAviso)

        esperar { visible(saludo) }
        assertEquals(listOf("RECORDATORIOS"), privacidad.registros.single().solicitud.finalidades)
    }

    @Test
    fun noAceptarCierraLaSesion() {
        llegarAlAviso()

        pulsar(texto(R.string.aviso_no_acepto))

        esperar { visible(texto(R.string.login_entrar)) }
        assertFalse(visible(saludo))
        assertTrue("No se registró ningún consentimiento", privacidad.registros.isEmpty())
    }

    // Caso borde: un cambio sustancial del aviso pide aceptarlo otra vez antes de volver a Inicio.
    @Test
    fun unaVersionNuevaDelAvisoPideAceptarOtraVez() {
        entrar()

        privacidad.publicarNuevaVersion()
        abrirApp()

        esperar { visible(aceptarAviso) }
        assertFalse(visible(saludo))
    }

    // Escenario 2: aceptar y luego revocar una finalidad opcional desde Inicio; cada cambio es un registro nuevo.
    @Test
    fun desdeInicioSePuedeAceptarYRevocarUnaFinalidadOpcional() {
        entrar()

        abrirSeccion(R.string.pantalla_privacidad)
        esperar { visible(texto(R.string.aviso_actualizar)) }
        rule.onNodeWithText(texto(R.string.aviso_actualizar)).assertIsNotEnabled() // sin cambios no hay nada que enviar
        assertFalse("En gestión no hay 'No acepto'", visible(texto(R.string.aviso_no_acepto)))

        rule.onNodeWithText(recordatorios).performClick()
        rule.onNodeWithText(texto(R.string.aviso_actualizar)).assertIsEnabled()
        pulsar(texto(R.string.aviso_actualizar))
        esperar { visible(saludo) }

        val aceptacion = privacidad.registros.last()
        assertEquals(TipoConsentimiento.FINALIDADES_SECUNDARIAS, aceptacion.solicitud.tipo)
        assertEquals(AccionConsentimiento.ACEPTAR, aceptacion.solicitud.accion)
        assertEquals(listOf("RECORDATORIOS"), aceptacion.solicitud.finalidades)
        val antes = privacidad.registros

        abrirSeccion(R.string.pantalla_privacidad)
        esperar { visible(recordatorios) }
        rule.onAllNodes(isToggleable())[0].assertIsOn() // el servidor recuerda lo aceptado
        rule.onNodeWithText(recordatorios).performClick()
        pulsar(texto(R.string.aviso_actualizar))
        esperar { visible(saludo) }

        val revocacion = privacidad.registros.last()
        assertEquals(AccionConsentimiento.REVOCAR, revocacion.solicitud.accion)
        assertEquals(listOf("RECORDATORIOS"), revocacion.solicitud.finalidades)
        assertEquals("Revocar añade un registro, no modifica el anterior", antes.size + 1, privacidad.registros.size)
        assertEquals(antes, privacidad.registros.take(antes.size))
    }
}
