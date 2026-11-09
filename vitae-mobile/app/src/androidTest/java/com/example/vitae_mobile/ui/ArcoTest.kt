package com.example.vitae_mobile.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** TASK-M-013: solicitud ARCO (PROV-RN-009): crear y consultar estado y plazos. */
@RunWith(AndroidJUnit4::class)
class ArcoTest : AppTest() {
    @Before
    fun sinSolicitudes() = arco.reiniciar()

    private fun abrirArco() {
        entrar()
        abrirSeccion(R.string.pantalla_arco)
        esperar { visible(texto(R.string.arco_enviar)) }
    }

    private fun elegir(derecho: Int) {
        rule.onNodeWithText(texto(derecho)).performScrollTo().performClick()
    }

    @Test
    fun sinElegirUnDerechoNoSePuedeEnviar() {
        abrirArco()

        listOf(R.string.derecho_acceso, R.string.derecho_rectificacion, R.string.derecho_cancelacion, R.string.derecho_oposicion)
            .forEach { assertTrue(visible(texto(it))) }
        rule.onNodeWithText(texto(R.string.arco_enviar)).performScrollTo().assertIsNotEnabled()

        elegir(R.string.derecho_rectificacion)
        rule.onNodeWithText(texto(R.string.arco_enviar)).assertIsEnabled()
    }

    @Test
    fun sinSolicitudesSeMuestraElMensajeVacio() {
        abrirArco()
        esperar { visible(texto(R.string.arco_vacio)) }
    }

    // Escenario 1: la solicitud responde con folio y fechas límite.
    @Test
    fun enviarUnaSolicitudMuestraElFolioYLosPlazos() {
        abrirArco()

        elegir(R.string.derecho_acceso)
        pulsar(texto(R.string.arco_enviar))

        esperar { visible(texto(R.string.arco_enviada)) }
        assertTrue(visible("ARCO-0001"))
        assertTrue(visible(texto(R.string.arco_plazo_respuesta)))
        assertTrue(visible(texto(R.string.arco_plazo_ejecucion)))
        esperar { visible(texto(R.string.arco_actualizar)) } // ya aparece en "Mis solicitudes"
        assertTrue(visible("Recibida"))
        assertEquals(1, arco.run { kotlinx.coroutines.runBlocking { (solicitudes() as com.example.vitae_mobile.core.Resultado.Exito).valor.size } })
        rule.onNodeWithText(texto(R.string.arco_enviar)).performScrollTo().assertIsNotEnabled() // el formulario queda limpio
    }

    // Escenario 2: consultar el estado de una solicitud existente.
    @Test
    fun consultarElEstadoDeUnaSolicitudExistente() {
        abrirArco()
        elegir(R.string.derecho_cancelacion)
        pulsar(texto(R.string.arco_enviar))
        esperar { visible(texto(R.string.arco_actualizar)) }

        pulsar(texto(R.string.arco_actualizar))

        // Termina la consulta: el botón vuelve a estar disponible y la solicitud sigue en la lista.
        esperar { visible(texto(R.string.arco_actualizar)) }
        rule.onNodeWithText(texto(R.string.arco_actualizar)).performScrollTo().assertIsEnabled()
        assertTrue(visible("Recibida"))
        assertFalse(visible(texto(R.string.error_permisos)))
    }
}
