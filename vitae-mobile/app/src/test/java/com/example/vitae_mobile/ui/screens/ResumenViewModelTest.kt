package com.example.vitae_mobile.ui.screens

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.resultados.ArchivoDescargado
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import com.example.vitae_mobile.ui.components.Carga
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
class ResumenViewModelTest {
    private val ahora = Instant.parse("2026-11-04T12:00:00Z")
    private val reloj = Clock.fixed(ahora, ZoneOffset.UTC)

    private fun cita(id: String, dias: Long) = Cita(id, ahora.plusSeconds(dias * 86_400), "Programada")
    private fun receta(id: String, estado: EstadoReceta) = Receta(id, "Med", null, null, null, estado, null, false)
    private fun resultado(id: String, dias: Long) =
        ResultadoEstudio(id, "Estudio $id", ahora.minusSeconds(dias * 86_400), "application/pdf")

    private class Citas(var respuesta: Resultado<List<Cita>>) : CitasRepository {
        override suspend fun citas() = respuesta
    }

    private class Recetas(var respuesta: Resultado<List<Receta>>) : RecetasRepository {
        override suspend fun recetas() = respuesta
        override suspend fun receta(id: String): Resultado<Receta> = Resultado.Fallo(ApiError.Desconocido)
    }

    private class Resultados(var respuesta: Resultado<List<ResultadoEstudio>>) : ResultadosRepository {
        override suspend fun resultados() = respuesta
        override suspend fun archivo(id: String, progreso: (Float) -> Unit): Resultado<ArchivoDescargado> =
            Resultado.Fallo(ApiError.Desconocido)
    }

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    private fun vm(
        citas: Resultado<List<Cita>> = Resultado.Exito(emptyList()),
        recetas: Resultado<List<Receta>> = Resultado.Exito(emptyList()),
        resultados: Resultado<List<ResultadoEstudio>> = Resultado.Exito(emptyList()),
    ) = ResumenViewModel(Citas(citas), Recetas(recetas), Resultados(resultados), reloj)

    @Test
    fun laProximaCitaEsLaMasCercanaEnElFuturo() = runTest {
        val e = vm(citas = Resultado.Exito(listOf(cita("lejana", 10), cita("pasada", -3), cita("cercana", 2)))).estado.value
        assertEquals(Carga.Listo(cita("cercana", 2)), e.proximaCita)
    }

    @Test
    fun sinCitasFuturasNoHayProximaCita() = runTest {
        val e = vm(citas = Resultado.Exito(listOf(cita("pasada", -3)))).estado.value
        assertEquals(Carga.Listo(null), e.proximaCita)
    }

    @Test
    fun cuentaSoloLasRecetasActivas() = runTest {
        val recetas = listOf(
            receta("1", EstadoReceta.Activa), receta("2", EstadoReceta.Activa),
            receta("3", EstadoReceta.Dispensada), receta("4", EstadoReceta.Cancelada), receta("5", EstadoReceta.Desconocido),
        )
        assertEquals(Carga.Listo(2), vm(recetas = Resultado.Exito(recetas)).estado.value.recetasActivas)
    }

    @Test
    fun elUltimoResultadoEsElMasReciente() = runTest {
        val lista = listOf(resultado("viejo", 30), resultado("nuevo", 2), resultado("medio", 9))
        val e = vm(resultados = Resultado.Exito(lista)).estado.value
        assertEquals(Carga.Listo(resultado("nuevo", 2)), e.ultimoResultado)
    }

    @Test
    fun sinDatosTodoQuedaVacioSinError() = runTest {
        val e = vm().estado.value
        assertEquals(Carga.Listo(null), e.proximaCita)
        assertEquals(Carga.Listo(0), e.recetasActivas)
        assertEquals(Carga.Listo(null), e.ultimoResultado)
        assertFalse(e.hayError)
    }

    // Cada bloque falla por separado: un error en uno no oculta los demás.
    @Test
    fun unBloqueConErrorNoAfectaALosOtros() = runTest {
        val e = vm(
            citas = Resultado.Fallo(ApiError.Red),
            recetas = Resultado.Exito(listOf(receta("1", EstadoReceta.Activa))),
            resultados = Resultado.Fallo(ApiError.SinPermisos),
        ).estado.value
        assertEquals(Carga.Error(ApiError.Red), e.proximaCita)
        assertEquals(Carga.Listo(1), e.recetasActivas)
        assertEquals(Carga.Error(ApiError.SinPermisos), e.ultimoResultado)
        assertTrue(e.hayError)
    }

    @Test
    fun cargarReintentaTodoElResumen() = runTest {
        val citas = Citas(Resultado.Fallo(ApiError.Red))
        val modelo = ResumenViewModel(citas, Recetas(Resultado.Exito(emptyList())), Resultados(Resultado.Exito(emptyList())), reloj)
        assertTrue(modelo.estado.value.hayError)

        citas.respuesta = Resultado.Exito(listOf(cita("c", 1)))
        modelo.cargar()

        assertFalse(modelo.estado.value.hayError)
        assertEquals(Carga.Listo(cita("c", 1)), modelo.estado.value.proximaCita)
    }

    // El backend no tiene citas: esa fila dice "No disponible" y no ofrece reintentar, el resto del resumen sigue.
    @Test
    fun unaSeccionNoDisponibleNoCuentaComoError() = runTest {
        val vm = ResumenViewModel(
            Citas(Resultado.Fallo(ApiError.NoDisponible)),
            Recetas(Resultado.Exito(listOf(receta("r1", EstadoReceta.Activa)))),
            Resultados(Resultado.Exito(emptyList())),
            reloj,
        )

        assertTrue(vm.estado.value.proximaCita is Carga.Error)
        assertFalse(vm.estado.value.hayError)
        assertEquals(Carga.Listo(1), vm.estado.value.recetasActivas)
    }

    @Test
    fun unFalloDeRedSiCuentaComoError() = runTest {
        val vm = ResumenViewModel(
            Citas(Resultado.Fallo(ApiError.Red)),
            Recetas(Resultado.Exito(emptyList())),
            Resultados(Resultado.Exito(emptyList())),
            reloj,
        )

        assertTrue(vm.estado.value.hayError)
    }
}
