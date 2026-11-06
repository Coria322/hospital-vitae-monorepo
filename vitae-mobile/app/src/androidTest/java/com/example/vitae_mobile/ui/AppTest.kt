package com.example.vitae_mobile.ui

import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.vitae_mobile.MainActivity
import com.example.vitae_mobile.R
import com.example.vitae_mobile.VitaeApplication
import com.example.vitae_mobile.data.arco.MockArcoRepository
import com.example.vitae_mobile.data.auth.CODIGO_OTP_DEMO
import com.example.vitae_mobile.data.auth.CONTRASENA_DEMO
import com.example.vitae_mobile.data.auth.CORREO_PACIENTE_DEMO
import com.example.vitae_mobile.data.privacidad.MockPrivacidadRepository
import org.junit.Before
import org.junit.Rule

/**
 * Base de las pruebas que recorren la app real (con los datos simulados de debug) como lo haría un paciente.
 * Se ejecutan en dispositivo o emulador.
 *
 * La actividad se abre con `am start` a través de UiAutomation, no con `ActivityScenario`: MIUI bloquea que una app
 * abra su propia pantalla desde segundo plano (y `am instrument` reinicia el proceso, así que siempre lo está), pero
 * deja abrirla a la identidad de shell. Se abre en [iniciar], que corre después de arrancar la regla de Compose.
 * El estado de la sesión y de los servidores simulados vive en el proceso: [entrar] se adapta a que ya haya sesión o
 * no, y a que el aviso de privacidad esté aceptado o no.
 */
abstract class AppTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    protected val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    protected fun texto(id: Int, vararg args: Any): String = contexto.getString(id, *args)

    private val contenedor get() = (contexto.applicationContext as VitaeApplication).contenedor

    /** Servidores simulados de debug: permiten preparar y comprobar el estado desde las pruebas. */
    protected val privacidad get() = contenedor.privacidadRepository as MockPrivacidadRepository
    protected val arco get() = contenedor.arcoRepository as MockArcoRepository
    protected val sesion get() = contenedor.sesion
    protected val dispositivo get() = contenedor.dispositivo

    protected fun enHiloPrincipal(bloque: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(bloque)

    // Mientras la actividad no ha compuesto aún, Compose lanza IllegalStateException: se trata como "no visible".
    protected fun visible(texto: String, subcadena: Boolean = false) = try {
        rule.onAllNodesWithText(texto, substring = subcadena).fetchSemanticsNodes().isNotEmpty()
    } catch (e: IllegalStateException) {
        false
    }

    protected fun visibleConDescripcion(descripcion: String) = try {
        rule.onAllNodesWithContentDescription(descripcion).fetchSemanticsNodes().isNotEmpty()
    } catch (e: IllegalStateException) {
        false
    }

    protected fun esperar(condicion: () -> Boolean) = rule.waitUntil(ESPERA_MS, condicion)

    protected val saludo get() = texto(R.string.inicio_saludo, "Ana Pérez Demo")
    protected val aceptarAviso get() = texto(R.string.aviso_aceptar)
    protected val continuarAvisoPrevio get() = texto(R.string.aviso_previo_continuar)

    /** Abre MainActivity como actividad nueva (descarta la anterior); la sesión del proceso se conserva. */
    protected fun abrirApp() {
        val comando = "am start -W --activity-clear-task -n ${contexto.packageName}/${MainActivity::class.java.name}"
        val salida = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        ParcelFileDescriptor.AutoCloseInputStream(salida).use { it.readBytes() } // espera a que termine el comando
    }

    @Before
    fun iniciar() = abrirApp()

    /**
     * Inicia sesión si hace falta (con OTP si el mock aún no conoce el dispositivo) y espera a que aparezca el aviso
     * de privacidad o Inicio; no acepta nada.
     */
    protected fun entrarHastaElAviso() {
        esperar { visible(texto(R.string.login_entrar)) || visible(saludo) || visible(aceptarAviso) || visible(continuarAvisoPrevio) }
        // Primera apertura (o versión nueva del aviso): el aviso simplificado va antes del login.
        if (visible(continuarAvisoPrevio)) pulsar(continuarAvisoPrevio)
        esperar { visible(texto(R.string.login_entrar)) || visible(saludo) || visible(aceptarAviso) }
        if (visible(texto(R.string.login_entrar))) {
            rule.onNodeWithText(texto(R.string.login_correo)).performTextInput(CORREO_PACIENTE_DEMO)
            rule.onNodeWithText(texto(R.string.login_contrasena)).performTextInput(CONTRASENA_DEMO)
            rule.onNodeWithText(texto(R.string.login_entrar)).performClick()
            esperar { visible(texto(R.string.pantalla_otp)) || visible(saludo) || visible(aceptarAviso) }
            if (visible(texto(R.string.pantalla_otp))) {
                rule.onNodeWithText(texto(R.string.otp_codigo)).performTextInput(CODIGO_OTP_DEMO)
                rule.onNodeWithText(texto(R.string.otp_verificar)).performClick()
            }
        }
        esperar { visible(saludo) || visible(aceptarAviso) }
    }

    /** Deja la app en Inicio: inicia sesión y acepta el aviso de privacidad si hace falta. */
    protected fun entrar() {
        entrarHastaElAviso()
        if (visible(aceptarAviso)) pulsar(aceptarAviso)
        esperar { visible(saludo) }
    }

    /** Raíz del árbol semántico de lo que hay en pantalla. */
    protected fun arbol() = rule.onRoot().fetchSemanticsNode()

    private fun abrirFila(indice: Int) = rule.onAllNodesWithText(texto(R.string.abrir))[indice].performClick()

    /**
     * Recorre las pantallas de consulta clínica (Inicio, Citas, Recetas y su detalle, Resultados y su visor, Perfil) y
     * llama a [revisar] en cada una, con la pantalla ya cargada. Parte de una sesión iniciada y deja la app en Inicio.
     */
    protected fun recorrerPantallasClinicas(revisar: (nombre: String) -> Unit) {
        entrar()
        esperar { visible(texto(R.string.resumen_proxima_cita)) && !visible(texto(R.string.dato_no_disponible)) }
        revisar("Inicio")

        abrirSeccion(R.string.pantalla_citas)
        esperar { visible("Atendida") }
        revisar("Citas")
        volverAInicio()

        abrirSeccion(R.string.pantalla_recetas)
        esperar { visible("Paracetamol 500 mg tabletas") }
        revisar("Recetas")
        abrirFila(0)
        esperar { visible("500 mg") }
        revisar("Detalle de receta")
        volverAInicio(veces = 2)

        abrirSeccion(R.string.pantalla_resultados)
        esperar { visible("Biometría hemática") }
        revisar("Resultados")
        abrirFila(0)
        esperar { visibleConDescripcion(texto(R.string.visor_pagina_desc, 1, 2)) }
        revisar("Visor de resultado")
        volverAInicio(veces = 2)

        abrirSeccion(R.string.pantalla_perfil)
        esperar { visible("DEMO900517MDFRRN09") }
        revisar("Perfil")
        volverAInicio()
    }

    /** Pulsa un botón, desplazándose hasta él si la pantalla se desplaza y no cabe entera. */
    protected fun pulsar(texto: String) {
        val nodo = rule.onNodeWithText(texto)
        try {
            nodo.performScrollTo()
        } catch (e: AssertionError) {
            // La pantalla no se desplaza: el botón ya está a la vista.
        }
        nodo.performClick()
    }

    /** Pulsa el botón de una sección de Inicio. */
    protected fun abrirSeccion(titulo: Int) = pulsar(texto(titulo))

    /** Pulsa "atrás" [veces] veces y espera a estar de nuevo en Inicio. */
    protected fun volverAInicio(veces: Int = 1) {
        repeat(veces) { Espresso.pressBack() }
        esperar { visible(saludo) }
    }

    /** `true` si la ventana visible tiene FLAG_SECURE (capturas y grabación bloqueadas). */
    protected fun ventanaProtegida(): Boolean {
        var protegida = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val actividad = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
            protegida = actividad != null && actividad.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        }
        return protegida
    }

    protected companion object {
        const val ESPERA_MS = 8_000L
    }
}
