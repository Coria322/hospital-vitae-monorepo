package com.example.vitae_mobile.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.seguridad.SimulacionRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-014 / TASK-M-007: estados del sistema (RNF-006, PROV-SEC-011). El root se simula con [SimulacionRoot], que solo
 * existe en debug; el 426 con [com.example.vitae_mobile.data.seguridad.GuardiaDispositivo.marcarIncompatible].
 */
@RunWith(AndroidJUnit4::class)
class SeguridadTest : AppTest() {
    private val tituloRoot get() = texto(R.string.pantalla_root)
    private val tituloIncompatible get() = texto(R.string.pantalla_incompatible)
    private val salir get() = texto(R.string.bloqueo_salir)

    // Las pruebas comparten proceso: se deja el dispositivo como llegó.
    @After
    fun restaurarDispositivo() {
        SimulacionRoot.activo = false
        enHiloPrincipal { dispositivo.olvidarIncompatible() }
    }

    private fun abrirConRoot() {
        SimulacionRoot.activo = true
        abrirApp()
        esperar { visible(tituloRoot) }
    }

    /** Los textos visibles de la pantalla, para comprobar que no cuenta nada técnico. */
    private fun textosVisibles(): Set<String> = rule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
        .flatMap { nodo -> nodo.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }
        .toSet()

    @Test
    fun conRootLaAppSeBloqueaAntesDelLogin() {
        abrirConRoot()

        assertTrue(visible(texto(R.string.bloqueo_root_texto)))
        assertFalse("No debe haber formulario de login", visible(texto(R.string.login_entrar)))
        assertTrue(visible(salir))
    }

    // El mensaje es genérico: ni qué señal se detectó ni nada del sistema.
    @Test
    fun laPantallaDeRootNoRevelaDetallesTecnicos() {
        abrirConRoot()

        assertEquals(setOf(tituloRoot, texto(R.string.bloqueo_root_texto), salir), textosVisibles())
    }

    @Test
    fun conRootYSesionVivaLaSesionSeBorraEnLocal() {
        entrar()

        abrirConRoot()

        assertTrue(sesion.estado.value is EstadoSesion.SinSesion)
        assertFalse(visible(saludo))
        assertTrue(dispositivo.bloqueado())
    }

    @Test
    fun salirCierraLaApp() {
        abrirConRoot()

        pulsar(salir)

        esperar { !visible(tituloRoot) }
    }

    // Si el root desaparece, al volver a comprobar la app deja de estar bloqueada.
    @Test
    fun alQuitarseElRootSeVuelveAlLogin() {
        entrarHastaElAviso() // deja visto el aviso previo
        abrirConRoot()

        SimulacionRoot.activo = false
        enHiloPrincipal { dispositivo.verificar() }

        esperar { visible(texto(R.string.login_entrar)) }
        assertFalse(visible(tituloRoot))
    }

    // RNF-006: un 426 con la app abierta lleva a la pantalla de incompatibilidad y cierra la sesión local.
    @Test
    fun un426MuestraLaPantallaIncompatible() {
        entrar()

        enHiloPrincipal { dispositivo.marcarIncompatible() }

        esperar { visible(tituloIncompatible) }
        assertTrue(visible(texto(R.string.bloqueo_incompatible_texto)))
        assertFalse(visible(saludo))
        assertTrue(sesion.estado.value is EstadoSesion.SinSesion)
        assertEquals(setOf(tituloIncompatible, texto(R.string.bloqueo_incompatible_texto), salir), textosVisibles())
    }

    // RF-003 / PROV-SEC-001: sesión expirada → su pantalla, y desde ahí al login.
    @Test
    fun laSesionExpiradaMuestraSuPantallaYLlevaAlLogin() {
        entrar()

        enHiloPrincipal { sesion.expirar() }

        esperar { visible(texto(R.string.sesion_expirada_texto)) }
        assertFalse(visible(saludo))
        pulsar(texto(R.string.pantalla_login))
        esperar { visible(texto(R.string.login_entrar)) }
    }
}
