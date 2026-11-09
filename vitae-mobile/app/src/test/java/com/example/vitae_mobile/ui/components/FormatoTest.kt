package com.example.vitae_mobile.ui.components

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

// CON-015: el servidor manda UTC; la app muestra la hora de la zona del dispositivo.
class FormatoTest {
    private val instante = Instant.parse("2026-11-04T05:30:00Z")

    @Test
    fun enUtcSeMuestraTalCual() {
        assertEquals("04/11/2026", formatearFecha(instante, ZoneId.of("UTC")))
        assertEquals("05:30", formatearHora(instante, ZoneId.of("UTC")))
    }

    // Cruza la medianoche: la fecha local es la del día anterior.
    @Test
    fun enLaZonaDeMexicoCambiaDeDia() {
        val mexico = ZoneId.of("America/Mexico_City")
        assertEquals("03/11/2026", formatearFecha(instante, mexico))
        assertEquals("23:30", formatearHora(instante, mexico))
    }
}
