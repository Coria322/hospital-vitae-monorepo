package com.example.vitae_mobile.ui.resultados

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.resultados.ArchivoDescargado
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultadoVisorViewModelTest {
    private val archivos = mutableListOf<File>()

    private class Repo(
        val bloque: suspend (id: String, progreso: (Float) -> Unit) -> Resultado<ArchivoDescargado>,
    ) : ResultadosRepository {
        override suspend fun resultados(): Resultado<List<ResultadoEstudio>> = Resultado.Exito(emptyList())
        override suspend fun archivo(id: String, progreso: (Float) -> Unit) = bloque(id, progreso)
    }

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() {
        Dispatchers.resetMain()
        archivos.forEach { it.delete() }
    }

    private fun archivoTemporal() = File.createTempFile("resultado", ".pdf").also { archivos += it }

    private fun modelo(almacen: ViewModelStore, repo: ResultadosRepository, id: String = "res-1") =
        ViewModelProvider.create(almacen, viewModelFactory { initializer { ResultadoVisorViewModel(repo, id) } })[ResultadoVisorViewModel::class.java]

    // RF-010, caso borde: un archivo grande muestra progreso.
    @Test
    fun muestraElAvanceDeLaDescargaYLuegoElArchivo() = runTest {
        val fin = CompletableDeferred<Unit>()
        val archivo = archivoTemporal()
        val repo = Repo { _, progreso ->
            progreso(0.4f)
            fin.await()
            Resultado.Exito(ArchivoDescargado(archivo, "application/pdf"))
        }
        val vm = modelo(ViewModelStore(), repo)

        assertEquals(ArchivoUiState.Descargando(0.4f), vm.estado.value)

        fin.complete(Unit)

        assertEquals(ArchivoUiState.Listo(ArchivoDescargado(archivo, "application/pdf")), vm.estado.value)
    }

    // RF-010, escenario 2: un resultado ajeno responde 403 y la app no muestra datos.
    @Test
    fun unResultadoAjenoNoMuestraNada() = runTest {
        val vm = modelo(ViewModelStore(), Repo { _, _ -> Resultado.Fallo(ApiError.SinPermisos) })
        assertEquals(ArchivoUiState.Error(ApiError.SinPermisos), vm.estado.value)
    }

    @Test
    fun unFalloDeRedSePuedeReintentar() = runTest {
        var intentos = 0
        val archivo = archivoTemporal()
        val vm = modelo(ViewModelStore(), Repo { _, _ ->
            if (++intentos == 1) Resultado.Fallo(ApiError.Red) else Resultado.Exito(ArchivoDescargado(archivo, "image/png"))
        })
        assertEquals(ArchivoUiState.Error(ApiError.Red), vm.estado.value)

        vm.cargar()

        assertTrue(vm.estado.value is ArchivoUiState.Listo)
    }

    @Test
    fun pideElArchivoDelIdIndicado() = runTest {
        var pedido = ""
        modelo(ViewModelStore(), Repo { id, _ -> pedido = id; Resultado.Fallo(ApiError.Red) }, id = "res-42")
        assertEquals("res-42", pedido)
    }

    // El resultado es información clínica: no queda en disco al cerrar el visor.
    @Test
    fun alSalirDelVisorElArchivoSeBorra() = runTest {
        val archivo = archivoTemporal()
        val almacen = ViewModelStore()
        modelo(almacen, Repo { _, _ -> Resultado.Exito(ArchivoDescargado(archivo, "application/pdf")) })
        assertTrue(archivo.exists())

        almacen.clear()

        assertFalse(archivo.exists())
    }
}
