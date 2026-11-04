package com.example.vitae_mobile

import androidx.compose.ui.graphics.Color
import com.example.vitae_mobile.ui.navigation.Ruta
import com.example.vitae_mobile.ui.theme.EsquemaClaro
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
        )
        pares.forEach { (nombre, par) ->
            assertTrue("$nombre = ${contraste(par.first, par.second)}", contraste(par.first, par.second) >= 4.5)
        }
    }

    // UI_UX_SPEC.md define 12 pantallas con rutas únicas.
    @Test
    fun rutasSonUnicas() {
        assertEquals(12, Ruta.entries.size)
        assertEquals(Ruta.entries.size, Ruta.entries.map { it.ruta }.toSet().size)
    }
}
