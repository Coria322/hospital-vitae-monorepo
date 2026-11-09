package com.example.vitae_mobile.ui.components

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CargaViewModelTest {
    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    @Test
    fun cargaAlCrearseYMuestraCargandoMientrasEspera() = runTest {
        val respuesta = CompletableDeferred<Resultado<List<String>>>()
        val vm = CargaViewModel { respuesta.await() }
        assertEquals(Carga.Cargando, vm.estado.value)

        respuesta.complete(Resultado.Exito(listOf("a")))

        assertEquals(Carga.Listo(listOf("a")), vm.estado.value)
    }

    @Test
    fun unaListaVaciaEsUnEstadoListo() = runTest {
        val vm = CargaViewModel { Resultado.Exito(emptyList<String>()) }
        assertEquals(Carga.Listo(emptyList<String>()), vm.estado.value)
    }

    // UI_UX_SPEC: error con reintento.
    @Test
    fun unErrorSeReintentaConCargar() = runTest {
        var intentos = 0
        val vm = CargaViewModel<String> {
            if (++intentos == 1) Resultado.Fallo(ApiError.Red) else Resultado.Exito("listo")
        }
        assertEquals(Carga.Error(ApiError.Red), vm.estado.value)

        vm.cargar()

        assertEquals(Carga.Listo("listo"), vm.estado.value)
        assertEquals(2, intentos)
    }

    // RF-003 / RF-010 escenario 2: un 403 llega como error y no trae datos.
    @Test
    fun unPermisoDenegadoNoTraeDatos() = runTest {
        val vm = CargaViewModel<String> { Resultado.Fallo(ApiError.SinPermisos) }
        assertEquals(Carga.Error(ApiError.SinPermisos), vm.estado.value)
    }
}
