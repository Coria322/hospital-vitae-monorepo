package com.example.vitae_mobile.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.privacidad.AvisoPrevioLocal
import com.example.vitae_mobile.data.seguridad.SimulacionRoot
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-017 (PROV-UX-003, WCAG 2.1 AA): revisa el árbol semántico (lo que lee TalkBack) de cada pantalla.
 *  - Todo control tiene texto o descripción que TalkBack pueda leer (WCAG 4.1.2).
 *  - Todo objetivo táctil mide al menos 48 x 48 dp (WCAG 2.5.8 y guía de Material).
 *  - Toda imagen informativa tiene descripción (WCAG 1.1.1).
 *  - Toda pantalla tiene un encabezado, para navegar por encabezados (WCAG 2.4.6).
 * El contraste de colores lo comprueba `ContrasteTest` sobre los colores del tema. Accessibility Scanner y TalkBack con
 * voz real son auditorías manuales: no se pueden automatizar aquí (ver `evidence/AI-05-mobile`).
 */
@RunWith(AndroidJUnit4::class)
class AccesibilidadTest : AppTest() {
    @After
    fun restaurar() {
        SimulacionRoot.activo = false
        AvisoPrevioLocal(contexto).marcarVisto()
    }

    private fun revisar(pantalla: String) {
        rule.waitForIdle()
        val minimo = DP_MINIMO * contexto.resources.displayMetrics.density
        val nodos = arbol().descendientes().toList()
        val problemas = mutableListOf<String>()

        val controles = nodos.filter { it.esInteractivo || it.esEditable }
        controles.forEach { nodo ->
            val nombre = nodo.textoHablable
            if (nombre.isBlank()) problemas += "control sin texto hablable (${nodo.boundsInRoot})"
            val toque = nodo.touchBoundsInRoot
            if (toque.width < minimo - 1 || toque.height < minimo - 1) {
                problemas += "objetivo táctil pequeño '$nombre' (${toque.width / minimo * DP_MINIMO} x ${toque.height / minimo * DP_MINIMO} dp)"
            }
        }
        nodos.filter { it.esImagen && it.textoHablable.isBlank() }.forEach { problemas += "imagen sin descripción (${it.boundsInRoot})" }
        if (nodos.none { it.esEncabezado }) problemas += "sin encabezado"

        assertTrue("$pantalla: ${problemas.joinToString("; ")}", problemas.isEmpty())
        pantallasRevisadas += pantalla to controles.size
    }

    private val pantallasRevisadas = mutableListOf<Pair<String, Int>>()

    @Test
    fun lasPantallasClinicasCumplen() {
        recorrerPantallasClinicas(::revisar)
        // Que el recorrido de verdad llegó a las 7 pantallas y encontró controles que revisar.
        assertTrue(pantallasRevisadas.toString(), pantallasRevisadas.size == 7 && pantallasRevisadas.sumOf { it.second } >= 8)
    }

    @Test
    fun avisoPrevioYLoginCumplen() {
        enHiloPrincipal { sesion.expirar() }
        contexto.getSharedPreferences("aviso_previo", Context.MODE_PRIVATE).edit().clear().commit()
        abrirApp()
        esperar { visible(continuarAvisoPrevio) }
        revisar("Aviso previo")
        pulsar(continuarAvisoPrevio)
        esperar { visible(texto(R.string.login_entrar)) }
        revisar("Login")
    }

    @Test
    fun avisoDePrivacidadPreferenciasYArcoCumplen() {
        privacidad.reiniciar()
        privacidad.publicarNuevaVersion()
        abrirApp()
        entrarHastaElAviso()
        esperar { visible(aceptarAviso) }
        revisar("Aviso de privacidad")
        pulsar(aceptarAviso)
        esperar { visible(saludo) }

        abrirSeccion(R.string.pantalla_privacidad)
        esperar { visible(texto(R.string.aviso_finalidades)) }
        revisar("Preferencias de privacidad")
        volverAInicio()

        arco.reiniciar()
        abrirSeccion(R.string.pantalla_arco)
        esperar { visible(texto(R.string.arco_enviar)) }
        revisar("Solicitud ARCO")
        volverAInicio()
    }

    @Test
    fun lasPantallasDeSistemaCumplen() {
        entrar()
        enHiloPrincipal { sesion.expirar() }
        esperar { visible(texto(R.string.sesion_expirada_texto)) }
        revisar("Sesión expirada")

        SimulacionRoot.activo = true
        abrirApp()
        esperar { visible(texto(R.string.pantalla_root)) }
        revisar("Dispositivo no seguro")
    }

    private companion object {
        const val DP_MINIMO = 48f
    }
}
