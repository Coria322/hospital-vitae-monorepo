package com.example.vitae_mobile.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.privacidad.AvisoPrevioLocal
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** PROV-RN-008, escenario 1: un paciente nuevo ve el aviso antes de cualquier formulario (el de login incluido). */
@RunWith(AndroidJUnit4::class)
class AvisoPrevioTest : AppTest() {
    private val preferencias get() = contexto.getSharedPreferences("aviso_previo", Context.MODE_PRIVATE)
    private val entrar get() = texto(R.string.login_entrar)

    // Las demás pruebas dan por hecho que el aviso previo ya se vio.
    @After
    fun dejarVisto() = AvisoPrevioLocal(contexto).marcarVisto()

    /** Reinicia la app como si fuera la primera vez (o con otra versión del aviso vista) y sin sesión. */
    private fun abrirDesdeCero(versionVista: String? = null) {
        enHiloPrincipal { sesion.expirar() }
        preferencias.edit().apply { if (versionVista == null) clear() else putString("version_vista", versionVista) }.commit()
        abrirApp()
        esperar { visible(continuarAvisoPrevio) }
    }

    @Test
    fun laPrimeraAperturaMuestraElAvisoAntesDeCualquierFormulario() {
        abrirDesdeCero()

        assertTrue(visible(texto(R.string.pantalla_aviso)))
        assertTrue(visible(texto(R.string.aviso_previo_texto)))
        assertTrue(visible(texto(R.string.aviso_enlace_integral)))
        assertFalse("No debe haber formulario antes del aviso", visible(entrar))
        assertFalse(visible(texto(R.string.login_correo)))
    }

    @Test
    fun continuarLlevaAlLoginYElAvisoNoVuelveAMostrarse() {
        abrirDesdeCero()

        pulsar(continuarAvisoPrevio)
        esperar { visible(entrar) }

        abrirApp()
        esperar { visible(entrar) }
        assertFalse(visible(continuarAvisoPrevio))
    }

    // Caso borde de PROV-RN-008: un cambio sustancial del aviso pide verlo de nuevo.
    @Test
    fun unaVersionNuevaDelAvisoSeVuelveAMostrar() {
        abrirDesdeCero(versionVista = "0.9")

        assertTrue(visible(continuarAvisoPrevio))
        assertFalse(visible(entrar))
    }
}
