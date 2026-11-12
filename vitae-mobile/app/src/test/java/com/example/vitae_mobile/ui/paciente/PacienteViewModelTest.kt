package com.example.vitae_mobile.ui.paciente

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.remoto.BackendTest
import com.example.vitae_mobile.ui.components.Carga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Elección del paciente (con los endpoints actuales entra el personal y consulta a un paciente). */
@OptIn(ExperimentalCoroutinesApi::class)
class PacienteViewModelTest : BackendTest() {
    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    @Test
    fun alAbrirCargaLaListaDePacientes() = runTest {
        responder(fixture("pacientes-lista"))

        val vm = PacienteViewModel(backend)
        val estado = vm.estado.first { it.lista !is Carga.Cargando }

        val pacientes = (estado.lista as Carga.Listo).valor
        assertEquals(listOf("Paciente Sintético Alfa", "Paciente Sintética Beta", "Paciente Desconocido Temporal"), pacientes.map { it.nombre })
        assertFalse(estado.elegido)
    }

    @Test
    fun siLaListaFallaSeMuestraElErrorYSePuedeReintentar() = runTest {
        responder("""{"status":"error"}""", 500)
        val vm = PacienteViewModel(backend)
        assertEquals(Carga.Error(ApiError.Servidor), vm.estado.first { it.lista !is Carga.Cargando }.lista)

        responder(fixture("pacientes-lista"))
        vm.cargar()
        assertTrue(vm.estado.first { it.lista is Carga.Listo }.lista is Carga.Listo)
    }

    @Test
    fun elegirDejaAlPacienteActivoYAvisa() = runTest {
        responder(fixture("pacientes-lista"))
        val vm = PacienteViewModel(backend)
        vm.estado.first { it.lista !is Carga.Cargando }
        responder(fixture("paciente-detalle"))

        vm.elegir(ID_ALFA)
        val estado = vm.estado.first { it.elegido }

        assertFalse(estado.eligiendo)
        assertNull(estado.error)
        assertEquals(ID_ALFA, backend.activo?.id)
        assertEquals("EXP-SINT01", backend.activo?.expediente)
    }

    @Test
    fun siElegirFallaSeMuestraElErrorYNoSeAvanza() = runTest {
        responder(fixture("pacientes-lista"))
        val vm = PacienteViewModel(backend)
        vm.estado.first { it.lista !is Carga.Cargando }
        responder("""{"status":"error"}""", 403)

        vm.elegir(ID_ALFA)
        val estado = vm.estado.first { it.error != null }

        assertEquals(ApiError.SinPermisos, estado.error)
        assertFalse(estado.elegido)
        assertFalse(estado.eligiendo)
        assertNull(backend.activo)
    }
}
