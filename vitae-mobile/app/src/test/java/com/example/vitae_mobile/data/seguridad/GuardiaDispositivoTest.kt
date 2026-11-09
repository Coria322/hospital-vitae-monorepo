package com.example.vitae_mobile.data.seguridad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardiaDispositivoTest {
    private val entorno = EntornoFalso()
    private var bloqueos = 0
    private fun guardia() = GuardiaDispositivo(entorno, DetectorRoot(entorno)) { bloqueos++ }

    @Test
    fun dispositivoLimpioEsAptoYNoBloquea() {
        val g = guardia()
        assertEquals(EstadoDispositivo.Apto, g.estado.value)
        assertFalse(g.bloqueado())
        assertEquals(0, bloqueos)
    }

    @Test
    fun rootAlArrancarBloqueaYAvisaDeInmediato() {
        entorno.archivos = setOf("/sbin/su")
        val g = guardia()
        assertEquals(EstadoDispositivo.RootDetectado, g.estado.value)
        assertTrue(g.bloqueado())
        assertEquals(1, bloqueos)
    }

    // Magisk pudo activarse con la app en segundo plano: al volver se comprueba otra vez.
    @Test
    fun rootQueApareceDespuesSeDetectaAlVerificar() {
        val g = guardia()
        entorno.paquetes = setOf("com.topjohnwu.magisk")
        g.verificar()
        assertEquals(EstadoDispositivo.RootDetectado, g.estado.value)
        assertEquals(1, bloqueos)
    }

    @Test
    fun androidNoSoportadoEsIncompatible() {
        entorno.sdk = SDK_MINIMO - 1
        val g = guardia()
        assertEquals(EstadoDispositivo.Incompatible, g.estado.value)
        assertEquals(1, bloqueos)
    }

    // RNF-006: un 426 del servidor bloquea y no se levanta al volver a comprobar.
    @Test
    fun un426BloqueaYSeMantiene() {
        val g = guardia()
        g.marcarIncompatible()
        assertEquals(EstadoDispositivo.Incompatible, g.estado.value)
        assertTrue(g.bloqueado())
        assertEquals(1, bloqueos)
        g.verificar()
        assertEquals(EstadoDispositivo.Incompatible, g.estado.value)
    }

    @Test
    fun rootGanaAlIncompatibleDelServidor() {
        val g = guardia()
        g.marcarIncompatible()
        entorno.archivos = setOf("/sbin/su")
        g.verificar()
        assertEquals(EstadoDispositivo.RootDetectado, g.estado.value)
    }

    @Test
    fun siElRootDesapareceVuelveAApto() {
        entorno.archivos = setOf("/sbin/su")
        val g = guardia()
        entorno.archivos = emptySet()
        g.verificar()
        assertEquals(EstadoDispositivo.Apto, g.estado.value)
        assertFalse(g.bloqueado())
    }
}
