package com.example.vitae_mobile.ui.perfil

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.perfil.PerfilRepository
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
class PerfilViewModelTest {
    private val perfil = Perfil("Ana Pérez", "DEMO900517MDFRRN09", "1990-05-17", true)

    private class RepoProgramable(var resultado: Resultado<Perfil>) : PerfilRepository {
        var llamadas = 0
        override suspend fun perfil(): Resultado<Perfil> {
            llamadas++
            return resultado
        }
    }

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    @Test
    fun cargaElPerfilAlCrearse() = runTest {
        val vm = PerfilViewModel(RepoProgramable(Resultado.Exito(perfil)))
        assertEquals(PerfilUiState.Listo(perfil), vm.estado.value)
    }

    @Test
    fun muestraCargandoMientrasEsperaLaRespuesta() = runTest {
        val respuesta = CompletableDeferred<Resultado<Perfil>>()
        val vm = PerfilViewModel(object : PerfilRepository {
            override suspend fun perfil() = respuesta.await()
        })
        assertEquals(PerfilUiState.Cargando, vm.estado.value)

        respuesta.complete(Resultado.Exito(perfil))

        assertEquals(PerfilUiState.Listo(perfil), vm.estado.value)
    }

    // UI_UX_SPEC: error con reintento.
    @Test
    fun unErrorSePuedeReintentar() = runTest {
        val repo = RepoProgramable(Resultado.Fallo(ApiError.Red))
        val vm = PerfilViewModel(repo)
        assertEquals(PerfilUiState.Error(ApiError.Red), vm.estado.value)

        repo.resultado = Resultado.Exito(perfil)
        vm.cargar()

        assertEquals(PerfilUiState.Listo(perfil), vm.estado.value)
        assertEquals(2, repo.llamadas)
    }

    // RNF-007, escenario 2: un recurso inválido llega como error y la pantalla sigue viva.
    @Test
    fun unRecursoInvalidoEsUnEstadoDeError() = runTest {
        val vm = PerfilViewModel(RepoProgramable(Resultado.Fallo(ApiError.Desconocido)))
        assertEquals(PerfilUiState.Error(ApiError.Desconocido), vm.estado.value)
    }

    // RF-003: un 403 se muestra como error sin cerrar sesión.
    @Test
    fun unPermisoDenegadoEsUnEstadoDeError() = runTest {
        val vm = PerfilViewModel(RepoProgramable(Resultado.Fallo(ApiError.SinPermisos)))
        assertEquals(PerfilUiState.Error(ApiError.SinPermisos), vm.estado.value)
    }
}
