package com.example.vitae_mobile.ui

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vitae_mobile.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-009, M-010 y M-011: citas, recetas con QR y resultados con visor interno, contra los datos simulados de
 * debug. Cada test deja la app en Inicio. Ver [AppTest] para cómo se abre la app.
 */
@RunWith(AndroidJUnit4::class)
class ClinicoTest : AppTest() {
    private val prohibidos = listOf("Guardar", "Editar", "Eliminar", "Borrar", "Imprimir", "Compartir")

    private fun abrirFila(indice: Int) {
        rule.onAllNodesWithText(texto(R.string.abrir))[indice].performClick()
    }

    private fun abrirRecetas() {
        abrirSeccion(R.string.pantalla_recetas)
        esperar { visible("Paracetamol 500 mg tabletas") }
    }

    private fun abrirResultados() {
        abrirSeccion(R.string.pantalla_resultados)
        esperar { visible("Biometría hemática") }
    }

    // PROV-RF-002: fecha, hora y estado de cada cita propia, sin acciones.
    @Test
    fun citasMuestraLasCitasPropiasEnSoloLectura() {
        entrar()
        abrirSeccion(R.string.pantalla_citas)

        esperar { visible("Atendida") }
        assertEquals(2, rule.onAllNodesWithText("Programada").fetchSemanticsNodes().size)
        assertEquals(3, rule.onAllNodesWithText(texto(R.string.cita_fecha)).fetchSemanticsNodes().size)
        assertEquals(3, rule.onAllNodesWithText(texto(R.string.cita_hora)).fetchSemanticsNodes().size)
        prohibidos.forEach { assertFalse("Citas: $it", visible(it)) }
        assertFalse(visible(texto(R.string.abrir))) // las citas no tienen acción por fila
        volverAInicio()
    }

    @Test
    fun recetasListaLasTresRecetasConSuEstadoEnTexto() {
        entrar()
        abrirRecetas()

        assertTrue(visible("Amoxicilina 500 mg cápsulas"))
        assertTrue(visible("Ibuprofeno 400 mg tabletas"))
        // El estado va siempre como texto, no solo con color.
        listOf(R.string.receta_estado_activa, R.string.receta_estado_dispensada, R.string.receta_estado_cancelada).forEach {
            assertEquals(1, rule.onAllNodesWithText(texto(it)).fetchSemanticsNodes().size)
        }
        assertEquals(3, rule.onAllNodesWithText(texto(R.string.abrir)).fetchSemanticsNodes().size)
        prohibidos.forEach { assertFalse("Recetas: $it", visible(it)) }
        volverAInicio()
    }

    // RF-009, escenario 1: el detalle muestra medicamento, dosis, vía, duración y estado, con su QR.
    @Test
    fun elDetalleDeUnaRecetaActivaMuestraSusDatosYUnQrValido() {
        entrar()
        abrirRecetas()
        abrirFila(0)

        esperar { visible("500 mg") }
        assertTrue(visible("Paracetamol 500 mg tabletas"))
        assertTrue(visible("Oral"))
        assertTrue(visible("7 días"))
        assertTrue(visible(texto(R.string.receta_estado_activa)))
        assertTrue(visibleConDescripcion(texto(R.string.receta_qr_desc)))
        assertFalse(visible(texto(R.string.receta_qr_invalido)))
        prohibidos.forEach { assertFalse("Detalle: $it", visible(it)) }
        volverAInicio(veces = 2)
    }

    // RF-009, escenario 2: una receta dispensada muestra el QR como no válido y el estado "dispensada".
    @Test
    fun unaRecetaDispensadaMuestraElQrNoValido() {
        entrar()
        abrirRecetas()
        abrirFila(1)

        esperar { visible(texto(R.string.receta_qr_invalido)) }
        assertTrue(visible(texto(R.string.receta_estado_dispensada)))
        assertTrue(visibleConDescripcion(texto(R.string.receta_qr_invalido_desc)))
        assertFalse(visibleConDescripcion(texto(R.string.receta_qr_desc)))
        volverAInicio(veces = 2)
    }

    // RF-010, escenario 1: un PDF propio se abre en el visor interno sin errores.
    @Test
    fun elVisorAbreUnPdfDeDosPaginas() {
        entrar()
        abrirResultados()
        abrirFila(0)

        esperar { visibleConDescripcion(texto(R.string.visor_pagina_desc, 1, 2)) }
        assertFalse(visible(texto(R.string.error_recurso)))
        assertFalse(visible("Descargando", subcadena = true))
        prohibidos.forEach { assertFalse("Visor: $it", visible(it)) }
        volverAInicio(veces = 2)
    }

    @Test
    fun elVisorAbreUnaImagen() {
        entrar()
        abrirResultados()
        abrirFila(1)

        esperar { visibleConDescripcion(texto(R.string.visor_imagen_desc)) }
        assertFalse(visible(texto(R.string.error_recurso)))
        volverAInicio(veces = 2)
    }

    // RF-010: el resultado no se guarda: el archivo existe solo en la caché interna y se borra al cerrar el visor.
    @Test
    fun elArchivoDelVisorSeBorraAlSalir() {
        val archivo = File(contexto.cacheDir, "resultados/res-001")
        entrar()
        abrirResultados()
        abrirFila(0)
        esperar { visibleConDescripcion(texto(R.string.visor_pagina_desc, 1, 2)) }
        assertTrue("El archivo debe estar en la caché interna", archivo.exists())

        volverAInicio(veces = 2)

        esperar { !archivo.exists() }
    }

    // PROV-SEC-011: todas las pantallas con información clínica bloquean capturas; navegar entre ellas no quita el flag.
    @Test
    fun lasPantallasClinicasBloqueanLasCapturas() {
        entrar()
        assertTrue("Inicio (resumen clínico)", ventanaProtegida())

        abrirSeccion(R.string.pantalla_citas)
        esperar { visible("Atendida") }
        assertTrue("Citas", ventanaProtegida())
        volverAInicio()
        assertTrue("Inicio tras salir de Citas", ventanaProtegida())

        abrirSeccion(R.string.pantalla_perfil)
        esperar { visible("DEMO900517MDFRRN09") }
        assertTrue("Perfil", ventanaProtegida())
        volverAInicio()
        assertTrue("Inicio tras salir de Perfil", ventanaProtegida())

        abrirRecetas()
        assertTrue("Recetas", ventanaProtegida())
        abrirFila(0)
        esperar { visible("500 mg") }
        assertTrue("Detalle de receta (navegar entre pantallas protegidas no quita el flag)", ventanaProtegida())
        volverAInicio(veces = 2)
        assertTrue("Inicio tras salir de Recetas", ventanaProtegida())

        abrirResultados()
        assertTrue("Resultados", ventanaProtegida())
        abrirFila(0)
        esperar { visibleConDescripcion(texto(R.string.visor_pagina_desc, 1, 2)) }
        assertTrue("Visor", ventanaProtegida())
        volverAInicio(veces = 2)
        assertTrue("Inicio tras salir de Resultados", ventanaProtegida())
    }

    // Al cerrar sesión el flag se libera: Login no tiene información clínica.
    @Test
    fun alCerrarSesionSeLiberaLaProteccion() {
        entrar()
        assertTrue("Inicio", ventanaProtegida())

        rule.onNodeWithText(texto(R.string.inicio_cerrar_sesion)).performClick()

        esperar { visible(texto(R.string.login_entrar)) }
        assertFalse("Login no está protegida", ventanaProtegida())
    }
}
