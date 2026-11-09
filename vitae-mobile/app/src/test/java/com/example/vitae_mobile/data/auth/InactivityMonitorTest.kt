package com.example.vitae_mobile.data.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// PROV-SEC-001 / TASK-M-005: 300 s sin interacción cierran la sesión.
@OptIn(ExperimentalCoroutinesApi::class)
class InactivityMonitorTest {

    @Test
    fun venceExactamenteALos300Segundos() = runTest {
        var vencimientos = 0
        val monitor = InactivityMonitor(backgroundScope, { testScheduler.currentTime }) { vencimientos++ }
        monitor.iniciar()

        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(0, vencimientos)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, vencimientos)
    }

    @Test
    fun laActividadReiniciaElPlazo() = runTest {
        var vencimientos = 0
        val monitor = InactivityMonitor(backgroundScope, { testScheduler.currentTime }) { vencimientos++ }
        monitor.iniciar()

        advanceTimeBy(200_000)
        monitor.registrarActividad() // el plazo ahora vence en t = 500 s
        advanceTimeBy(250_000)
        runCurrent()
        assertEquals(0, vencimientos)

        advanceTimeBy(50_000)
        runCurrent()
        assertEquals(1, vencimientos)
    }

    @Test
    fun detenerCancelaElVencimiento() = runTest {
        var vencimientos = 0
        val monitor = InactivityMonitor(backgroundScope, { testScheduler.currentTime }) { vencimientos++ }
        monitor.iniciar()
        monitor.detener()

        advanceTimeBy(400_000)
        runCurrent()
        assertEquals(0, vencimientos)
    }

    // El temporizador puede no correr con la app en segundo plano: al volver, comprobar() consulta el reloj.
    @Test
    fun comprobarVenceSiElRelojPasoDelLimite() = runTest {
        var ahora = 0L
        var vencimientos = 0
        val monitor = InactivityMonitor(backgroundScope, { ahora }) { vencimientos++ }
        monitor.iniciar()

        ahora = 299_999
        monitor.comprobar()
        assertEquals(0, vencimientos)

        ahora = 300_000
        monitor.comprobar()
        assertEquals(1, vencimientos)

        monitor.comprobar() // ya vencido y detenido: no vuelve a avisar
        assertEquals(1, vencimientos)
    }
}
