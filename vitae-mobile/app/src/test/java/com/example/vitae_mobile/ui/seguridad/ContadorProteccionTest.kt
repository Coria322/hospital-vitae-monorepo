package com.example.vitae_mobile.ui.seguridad

import org.junit.Assert.assertEquals
import org.junit.Test

// PROV-SEC-011: FLAG_SECURE no se quita mientras quede alguna pantalla protegida abierta.
class ContadorProteccionTest {
    private var puesto = 0
    private var quitado = 0
    private val contador = ContadorProteccion(poner = { puesto++ }, quitar = { quitado++ })

    @Test
    fun laPrimeraPantallaPoneElFlagYLaUltimaLoQuita() {
        contador.entrar()
        contador.salir()
        assertEquals(1, puesto)
        assertEquals(1, quitado)
    }

    // Al navegar de una pantalla protegida a otra, la nueva entra antes de que la anterior salga.
    @Test
    fun navegarEntrePantallasProtegidasNoQuitaElFlag() {
        contador.entrar() // lista de recetas
        contador.entrar() // detalle
        contador.salir() // se descarta la lista
        assertEquals(1, puesto)
        assertEquals(0, quitado)

        contador.salir() // se cierra el detalle
        assertEquals(1, quitado)
    }

    @Test
    fun salirDeMasNoDejaElContadorEnNegativo() {
        contador.salir()
        contador.entrar()
        assertEquals(1, puesto)
        assertEquals(0, quitado)
    }
}
