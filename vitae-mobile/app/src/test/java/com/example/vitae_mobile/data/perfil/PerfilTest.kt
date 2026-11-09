package com.example.vitae_mobile.data.perfil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerfilTest {
    @Test
    fun laFechaCompletaSeMuestraDiaMesAnio() {
        assertEquals("17/05/1990", formatearFechaFhir("1990-05-17"))
    }

    // FHIR permite fechas parciales: se muestran como llegan, sin inventar el día.
    @Test
    fun laFechaParcialSeMuestraTalCual() {
        assertEquals("1990-05", formatearFechaFhir("1990-05"))
        assertEquals("1990", formatearFechaFhir("1990"))
    }

    @Test
    fun sinFechaNoHayTexto() {
        assertNull(formatearFechaFhir(null))
    }
}
