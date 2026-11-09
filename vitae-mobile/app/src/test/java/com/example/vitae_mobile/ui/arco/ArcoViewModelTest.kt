package com.example.vitae_mobile.ui.arco

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.ui.components.Carga
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// PROV-RN-009 / TASK-M-013: solicitud ARCO desde la app.
@OptIn(ExperimentalCoroutinesApi::class)
class ArcoViewModelTest {
    private val hoy = LocalDate.parse("2026-11-04")

    private fun solicitud(folio: String, derecho: DerechoArco = DerechoArco.Acceso, estado: String = "Recibida") =
        SolicitudArco(folio, derecho, hoy, hoy.plusDays(20), hoy.plusDays(35), estado)

    private class Repo(var existentes: List<SolicitudArco> = emptyList()) : ArcoRepository {
        val creadas = mutableListOf<DerechoArco>()
        var errorAlCrear: ApiError? = null
        var respuestaConsulta: (String) -> Resultado<SolicitudArco> = { folio -> existentes.firstOrNull { it.folio == folio }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.SinPermisos) }
        var nueva: (DerechoArco) -> SolicitudArco = { error("sin programar") }

        override suspend fun solicitudes(): Resultado<List<SolicitudArco>> = Resultado.Exito(existentes)

        override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> {
            creadas += derecho
            errorAlCrear?.let { return Resultado.Fallo(it) }
            return nueva(derecho).also { existentes = listOf(it) + existentes }.let { Resultado.Exito(it) }
        }

        override suspend fun consultar(folio: String) = respuestaConsulta(folio)
    }

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    @Test
    fun cargaLasSolicitudesAlCrearse() = runTest {
        val vm = ArcoViewModel(Repo(listOf(solicitud("ARCO-0001"))))
        assertEquals(Carga.Listo(listOf(solicitud("ARCO-0001"))), vm.estado.value.solicitudes)
    }

    @Test
    fun sinSolicitudesLaListaEstaVaciaSinError() = runTest {
        assertEquals(Carga.Listo(emptyList<SolicitudArco>()), ArcoViewModel(Repo()).estado.value.solicitudes)
    }

    @Test
    fun noSeEnviaNadaSinElegirUnDerecho() = runTest {
        val repo = Repo()
        ArcoViewModel(repo).enviar()
        assertTrue(repo.creadas.isEmpty())
    }

    // Escenario 1: la solicitud se envía y la respuesta trae folio y fechas límite.
    @Test
    fun enviarMuestraElFolioYLosPlazosYRecargaLaLista() = runTest {
        val repo = Repo().apply { nueva = { solicitud("ARCO-0001", it) } }
        val vm = ArcoViewModel(repo)
        vm.seleccionar(DerechoArco.Rectificacion)

        vm.enviar()

        assertEquals(listOf(DerechoArco.Rectificacion), repo.creadas)
        assertEquals(solicitud("ARCO-0001", DerechoArco.Rectificacion), vm.estado.value.creada)
        assertNull(vm.estado.value.derecho) // el formulario queda listo para otra solicitud
        assertEquals(Carga.Listo(listOf(solicitud("ARCO-0001", DerechoArco.Rectificacion))), vm.estado.value.solicitudes)
    }

    @Test
    fun unErrorAlEnviarSeMuestraYConservaElDerechoElegido() = runTest {
        val repo = Repo().apply { errorAlCrear = ApiError.Red }
        val vm = ArcoViewModel(repo)
        vm.seleccionar(DerechoArco.Oposicion)

        vm.enviar()

        assertEquals(ApiError.Red, vm.estado.value.errorEnvio)
        assertEquals(DerechoArco.Oposicion, vm.estado.value.derecho)
        assertNull(vm.estado.value.creada)
    }

    @Test
    fun unSegundoToqueMientrasSeEnviaNoDuplicaLaSolicitud() = runTest {
        val respuesta = CompletableDeferred<Resultado<SolicitudArco>>()
        val creadas = mutableListOf<DerechoArco>()
        val repo = object : ArcoRepository {
            override suspend fun solicitudes() = Resultado.Exito(emptyList<SolicitudArco>())
            override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> {
                creadas += derecho
                return respuesta.await()
            }
            override suspend fun consultar(folio: String): Resultado<SolicitudArco> = Resultado.Fallo(ApiError.SinPermisos)
        }
        val vm = ArcoViewModel(repo)
        vm.seleccionar(DerechoArco.Acceso)

        vm.enviar()
        vm.enviar()
        respuesta.complete(Resultado.Exito(solicitud("ARCO-0001")))

        assertEquals(1, creadas.size)
    }

    // Escenario 2: consultar una solicitud existente devuelve su estado.
    @Test
    fun actualizarReflejaElNuevoEstadoEnLaLista() = runTest {
        val repo = Repo(listOf(solicitud("ARCO-0002"), solicitud("ARCO-0001")))
        val vm = ArcoViewModel(repo)
        repo.respuestaConsulta = { Resultado.Exito(solicitud(it, estado = "En proceso")) }

        vm.actualizar("ARCO-0001")

        val lista = (vm.estado.value.solicitudes as Carga.Listo).valor
        assertEquals(listOf("Recibida", "En proceso"), lista.map { it.estado })
        assertNull(vm.estado.value.actualizando)
    }

    @Test
    fun unFolioAjenoNoCambiaLaListaNiDejaElBotonOcupado() = runTest {
        val repo = Repo(listOf(solicitud("ARCO-0001")))
        val vm = ArcoViewModel(repo)
        repo.respuestaConsulta = { Resultado.Fallo(ApiError.SinPermisos) }

        vm.actualizar("ARCO-0001")

        assertEquals(Carga.Listo(listOf(solicitud("ARCO-0001"))), vm.estado.value.solicitudes)
        assertNull(vm.estado.value.actualizando)
    }

    @Test
    fun siFallaLaListaSeMuestraElErrorYSePuedeReintentar() = runTest {
        val repo = object : ArcoRepository {
            var fallar = true
            override suspend fun solicitudes(): Resultado<List<SolicitudArco>> = if (fallar) Resultado.Fallo(ApiError.Red) else Resultado.Exito(emptyList())
            override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> = Resultado.Fallo(ApiError.Red)
            override suspend fun consultar(folio: String): Resultado<SolicitudArco> = Resultado.Fallo(ApiError.Red)
        }
        val vm = ArcoViewModel(repo)
        assertEquals(Carga.Error(ApiError.Red), vm.estado.value.solicitudes)

        repo.fallar = false
        vm.cargar()

        assertEquals(Carga.Listo(emptyList<SolicitudArco>()), vm.estado.value.solicitudes)
    }
}
