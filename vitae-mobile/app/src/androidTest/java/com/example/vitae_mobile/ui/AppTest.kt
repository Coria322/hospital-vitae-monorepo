package com.example.vitae_mobile.ui

import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.vitae_mobile.MainActivity
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.auth.CODIGO_OTP_DEMO
import com.example.vitae_mobile.data.auth.CONTRASENA_DEMO
import com.example.vitae_mobile.data.auth.CORREO_PACIENTE_DEMO
import org.junit.Before
import org.junit.Rule

/**
 * Base de las pruebas que recorren la app real (con los datos simulados de debug) como lo haría un paciente.
 * Se ejecutan en dispositivo o emulador.
 *
 * La actividad se abre con `am start` a través de UiAutomation, no con `ActivityScenario`: MIUI bloquea que una app
 * abra su propia pantalla desde segundo plano (y `am instrument` reinicia el proceso, así que siempre lo está), pero
 * deja abrirla a la identidad de shell. Se abre en [iniciar], que corre después de arrancar la regla de Compose.
 * El estado de la sesión vive en el proceso: [entrar] se adapta a que ya haya sesión o no.
 */
abstract class AppTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    protected val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    protected fun texto(id: Int, vararg args: Any): String = contexto.getString(id, *args)

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

    /** Abre MainActivity como actividad nueva (descarta la anterior); la sesión del proceso se conserva. */
    protected fun abrirApp() {
        val comando = "am start -W --activity-clear-task -n ${contexto.packageName}/${MainActivity::class.java.name}"
        val salida = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        ParcelFileDescriptor.AutoCloseInputStream(salida).use { it.readBytes() } // espera a que termine el comando
    }

    @Before
    fun iniciar() = abrirApp()

    /** Deja la app en Inicio: inicia sesión si hace falta (con OTP si el mock aún no conoce el dispositivo). */
    protected fun entrar() {
        esperar { visible(texto(R.string.login_entrar)) || visible(saludo) }
        if (visible(texto(R.string.login_entrar))) {
            rule.onNodeWithText(texto(R.string.login_correo)).performTextInput(CORREO_PACIENTE_DEMO)
            rule.onNodeWithText(texto(R.string.login_contrasena)).performTextInput(CONTRASENA_DEMO)
            rule.onNodeWithText(texto(R.string.login_entrar)).performClick()
            esperar { visible(texto(R.string.pantalla_otp)) || visible(saludo) }
            if (visible(texto(R.string.pantalla_otp))) {
                rule.onNodeWithText(texto(R.string.otp_codigo)).performTextInput(CODIGO_OTP_DEMO)
                rule.onNodeWithText(texto(R.string.otp_verificar)).performClick()
            }
        }
        esperar { visible(saludo) }
    }

    /** Pulsa el botón de una sección de Inicio. */
    protected fun abrirSeccion(titulo: Int) = rule.onNodeWithText(texto(titulo)).performClick()

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
