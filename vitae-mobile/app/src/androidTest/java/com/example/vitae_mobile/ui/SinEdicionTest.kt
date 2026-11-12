package com.example.vitae_mobile.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TASK-M-015 (RF-011): las pantallas de consulta clínica son de solo lectura. Se recorren todas y se cuentan los
 * controles de edición: debe haber 0. Un control de edición es un campo donde se pueda escribir, o algo que se pueda
 * tocar cuyo texto sea una acción de escritura. Privacidad y ARCO no entran: son las escrituras de la lista blanca SUP-002.
 * Citas solo tiene el botón "Solicitar cita", que abre otra pantalla (PROV-RF-002, decisión 20 del plan); esa pantalla de
 * solicitud tampoco entra en este recorrido.
 */
@RunWith(AndroidJUnit4::class)
class SinEdicionTest : AppTest() {
    private val accionesDeEscritura = Regex(
        "guardar|editar|eliminar|borrar|modificar|actualizar datos|agendar|reprogramar|cancelar|crear|nuevo|nueva|compartir|imprimir|enviar|confirmar",
        RegexOption.IGNORE_CASE,
    )

    private val controlesRevisados = mutableListOf<String>()
    private val controlesDeEdicion = mutableListOf<String>()

    private fun contar(pantalla: String) {
        rule.waitForIdle()
        arbol().descendientes().forEach { nodo ->
            if (nodo.esEditable) controlesDeEdicion += "$pantalla: campo de texto"
            if (nodo.esInteractivo) {
                controlesRevisados += "$pantalla: ${nodo.textoHablable}"
                if (accionesDeEscritura.containsMatchIn(nodo.textoHablable)) controlesDeEdicion += "$pantalla: '${nodo.textoHablable}'"
            }
        }
    }

    @Test
    fun lasPantallasClinicasNoTienenControlesDeEdicion() {
        recorrerPantallasClinicas(::contar)

        // No vale pasar por no haber mirado nada: se inspeccionaron los botones de Inicio, las filas "Abrir", etc.
        assertTrue("Controles inspeccionados: $controlesRevisados", controlesRevisados.size >= 8)
        assertEquals("Controles de edición: $controlesDeEdicion", 0, controlesDeEdicion.size)
    }
}
