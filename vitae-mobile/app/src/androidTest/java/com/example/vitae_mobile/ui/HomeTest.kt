package com.example.vitae_mobile.ui

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** TASK-M-008: Inicio y Perfil. Ver [AppTest] para cómo se abre la app. */
@RunWith(AndroidJUnit4::class)
class HomeTest : AppTest() {
    private fun abrirPerfil() {
        abrirSeccion(R.string.pantalla_perfil)
        esperar { visible("DEMO900517MDFRRN09") }
    }

    @Test
    fun inicioSaludaAlPacienteConSusPropiosDatos() {
        entrar()
        rule.onNodeWithText(saludo).assertExists()
    }

    // TASK-M-008: Inicio resume próxima cita, recetas activas y último resultado.
    @Test
    fun inicioMuestraElResumenDelPaciente() {
        entrar()

        esperar { visible(texto(R.string.resumen_proxima_cita)) && !visible(texto(R.string.dato_no_disponible)) }
        esperar { visible("Biometría hemática", subcadena = true) }
        assertFalse(visible(texto(R.string.resumen_sin_cita)))
        assertFalse(visible(texto(R.string.resumen_sin_resultado)))
        assertTrue(visible(texto(R.string.resumen_recetas_activas)))
        assertTrue(visible("1")) // el mock tiene una sola receta activa
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
        esperar { visible(saludo) }
        rule.waitForIdle()
        Thread.sleep(1_000)

        assertFalse("Se mostró Login con la sesión viva", visible(texto(R.string.login_entrar)))
        assertFalse(visible(texto(R.string.pantalla_otp)))
    }

    // Este test deja la app en Login.
    @Test
    fun cerrarSesionVuelveALogin() {
        entrar()
        rule.onNodeWithText(texto(R.string.inicio_cerrar_sesion)).performClick()

        esperar { visible(texto(R.string.login_entrar)) }
        assertFalse(visible(saludo))
    }
}
