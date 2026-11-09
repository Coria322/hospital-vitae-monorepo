package com.example.vitae_mobile

import androidx.compose.ui.graphics.Color
import com.example.vitae_mobile.ui.navigation.Ruta
import com.example.vitae_mobile.ui.theme.EsquemaClaro
import com.example.vitae_mobile.ui.theme.VitaeBlanco
import com.example.vitae_mobile.ui.theme.VitaeCobalto
import com.example.vitae_mobile.ui.theme.VitaeCobaltoBrillante
import com.example.vitae_mobile.ui.theme.VitaeCobaltoProfundo
import com.example.vitae_mobile.ui.theme.VitaeTinta
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrasteTest {

    private fun luminancia(c: Color): Double {
        fun canal(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(c.red) + 0.7152 * canal(c.green) + 0.0722 * canal(c.blue)
    }

    private fun contraste(a: Color, b: Color): Double {
        val (claro, oscuro) = luminancia(a).let { la -> luminancia(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (claro + 0.05) / (oscuro + 0.05)
    }

    // PROV-UX-003 / TASK-M-017: texto normal >= 4.5:1 en los pares que usa la app.
    @Test
    fun paresDeTextoCumplenWcagAA() {
        val c = EsquemaClaro
        val pares = mapOf(
            "onPrimary/primary" to (c.onPrimary to c.primary),
            "onSecondary/secondary" to (c.onSecondary to c.secondary),
            "onBackground/background" to (c.onBackground to c.background),
            "onSurface/surface" to (c.onSurface to c.surface),
            "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
            // Los componentes de Material usan estos pares sobre el fondo gris (etiquetas, botón de contorno, subtítulos).
            "onSurfaceVariant/background" to (c.onSurfaceVariant to c.background),
            "primary/background" to (c.primary to c.background),
            // Guía de estilo: tarjetas cálidas, tarjeta azul destacada y mensajes de error sobre cada superficie.
            "onSurface/surfaceVariant (tarjeta cálida)" to (c.onSurface to c.surfaceVariant),
            "onSurfaceVariant/surfaceVariant (etiqueta gris en tarjeta cálida)" to (c.onSurfaceVariant to c.surfaceVariant),
            "blanco/cobalto (tarjeta destacada)" to (Color.White to VitaeCobalto),
            "blanco/cobalto profundo (elementos sobre la tarjeta azul)" to (Color.White to VitaeCobaltoProfundo),
            "tinta/blanco" to (VitaeTinta to VitaeBlanco),
            "error/background" to (c.error to c.background),
            "error/surface" to (c.error to c.surface),
            "error/surfaceVariant" to (c.error to c.surfaceVariant),
        )
        pares.forEach { (nombre, par) ->
            assertTrue("$nombre = ${contraste(par.first, par.second)}", contraste(par.first, par.second) >= 4.5)
        }
    }

    // WCAG 1.4.11 (AA): el borde y el pulgar de los controles necesitan 3:1 contra el fondo y la superficie.
    @Test
    fun losBordesDeLosControlesCumplenContrasteNoTextual() {
        val c = EsquemaClaro
        mapOf(
            "outline/background" to (c.outline to c.background),
            "outline/surface" to (c.outline to c.surface),
            "outline/surfaceVariant (tarjeta cálida)" to (c.outline to c.surfaceVariant),
            // El Cobalt Blue de la guía solo se usa como borde de lo seleccionado, sin texto encima.
            "cobalto brillante/background (borde de selección)" to (VitaeCobaltoBrillante to c.background),
            "outline/surfaceContainerHighest (pulgar sobre la pista apagada)" to (c.outline to c.surfaceContainerHighest),
        ).forEach { (nombre, par) ->
            assertTrue("$nombre = ${contraste(par.first, par.second)}", contraste(par.first, par.second) >= 3.0)
        }
    }

    // UI_UX_SPEC.md define 12 pantallas, más la de preferencias de privacidad, el aviso previo al login y la elección de paciente, con rutas únicas.
    @Test
    fun rutasSonUnicas() {
        assertEquals(15, Ruta.entries.size)
        assertEquals(Ruta.entries.size, Ruta.entries.map { it.ruta }.toSet().size)
    }
}
