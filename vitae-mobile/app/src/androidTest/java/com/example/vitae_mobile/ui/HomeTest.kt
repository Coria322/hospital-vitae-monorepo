package com.example.vitae_mobile.ui

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vitae_mobile.MainActivity
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.auth.CODIGO_OTP_DEMO
import com.example.vitae_mobile.data.auth.CONTRASENA_DEMO
import com.example.vitae_mobile.data.auth.CORREO_PACIENTE_DEMO
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-008: recorre la app real (con el servidor simulado de debug) como lo haría un paciente.
 * Se ejecuta en dispositivo o emulador.
 *
 * La actividad se abre con `am start` a través de UiAutomation, no con `ActivityScenario`: MIUI bloquea que una app
 * abra su propia pantalla desde segundo plano (y `am instrument` reinicia el proceso, así que siempre lo está), pero
 * deja abrirla a la identidad de shell. Se abre en [iniciar], que corre después de arrancar la regla de Compose.
 * El estado de la sesión vive en el proceso: [entrar] se adapta a que ya haya sesión o no.
 */
@RunWith(AndroidJUnit4::class)
class HomeTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    // Mientras la actividad no ha compuesto aún, Compose lanza IllegalStateException: se trata como "no visible".
    private fun visible(texto: String) = try {
        rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
    } catch (e: IllegalStateException) {
        false
    }

    private val saludo get() = texto(R.string.inicio_saludo, "Ana Pérez Demo")

    /** Abre MainActivity como actividad nueva (descarta la anterior); la sesión del proceso se conserva. */
    private fun abrirApp() {
        val comando = "am start -W --activity-clear-task -n ${contexto.packageName}/${MainActivity::class.java.name}"
        val salida = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        ParcelFileDescriptor.AutoCloseInputStream(salida).use { it.readBytes() } // espera a que termine el comando
    }

    @Before
    fun iniciar() = abrirApp()

    /** Deja la app en Inicio: inicia sesión si hace falta (con OTP si el mock aún no conoce el dispositivo). */
    private fun entrar() {
        rule.waitUntil(ESPERA_MS) { visible(texto(R.string.login_entrar)) || visible(saludo) }
        if (visible(texto(R.string.login_entrar))) {
            rule.onNodeWithText(texto(R.string.login_correo)).performTextInput(CORREO_PACIENTE_DEMO)
            rule.onNodeWithText(texto(R.string.login_contrasena)).performTextInput(CONTRASENA_DEMO)
            rule.onNodeWithText(texto(R.string.login_entrar)).performClick()
            rule.waitUntil(ESPERA_MS) { visible(texto(R.string.pantalla_otp)) || visible(saludo) }
            if (visible(texto(R.string.pantalla_otp))) {
                rule.onNodeWithText(texto(R.string.otp_codigo)).performTextInput(CODIGO_OTP_DEMO)
                rule.onNodeWithText(texto(R.string.otp_verificar)).performClick()
            }
        }
        rule.waitUntil(ESPERA_MS) { visible(saludo) }
    }

    private fun abrirPerfil() {
        rule.onNodeWithText(texto(R.string.pantalla_perfil)).performClick()
        rule.waitUntil(ESPERA_MS) { visible("DEMO900517MDFRRN09") }
    }

    private fun volverAInicio() {
        Espresso.pressBack()
        rule.waitUntil(ESPERA_MS) { visible(saludo) }
    }

    @Test
    fun inicioSaludaAlPacienteConSusPropiosDatos() {
        entrar()
        rule.onNodeWithText(saludo).assertExists()
    }

    // RNF-007, escenario 1: el Patient válido muestra CURP, nombre y fecha de nacimiento.
    @Test
    fun perfilMuestraNombreCurpFechaYSeguro() {
        entrar()
        abrirPerfil()

        rule.onNodeWithText("Ana Pérez Demo").assertExists()
        rule.onNodeWithText("DEMO900517MDFRRN09").assertExists()
        rule.onNodeWithText("17/05/1990").assertExists()
        rule.onNodeWithText(texto(R.string.perfil_seguro_activo)).assertExists()
        volverAInicio()
    }

    // RF-011: ni Inicio ni Perfil tienen controles de guardar, editar o eliminar.
    @Test
    fun lasPantallasSonDeSoloLectura() {
        val prohibidos = listOf("Guardar", "Editar", "Eliminar", "Borrar")
        entrar()
        prohibidos.forEach { assertFalse("Inicio: $it", visible(it)) }

        abrirPerfil()
        prohibidos.forEach { assertFalse("Perfil: $it", visible(it)) }
        volverAInicio()
    }

    // Una actividad nueva con la sesión viva (p. ej. tras salir con "atrás" y reabrir) debe mostrar Inicio, no Login.
    @Test
    fun unaActividadNuevaConLaSesionVivaMuestraInicio() {
        entrar()

        abrirApp() // actividad nueva; el proceso y la sesión siguen vivos
        rule.waitUntil(ESPERA_MS) { visible(saludo) }
        rule.waitForIdle()
        Thread.sleep(ESPERA_COMPOSICION_MS)

        assertFalse("Se mostró Login con la sesión viva", visible(texto(R.string.login_entrar)))
        assertFalse(visible(texto(R.string.pantalla_otp)))
    }

    @Test
    fun cerrarSesionVuelveALogin() {
        entrar()
        rule.onNodeWithText(texto(R.string.inicio_cerrar_sesion)).performClick()

        rule.waitUntil(ESPERA_MS) { visible(texto(R.string.login_entrar)) }
        assertFalse(visible(saludo))
    }

    private companion object {
        const val ESPERA_MS = 8_000L
        const val ESPERA_COMPOSICION_MS = 1_000L
    }
}
