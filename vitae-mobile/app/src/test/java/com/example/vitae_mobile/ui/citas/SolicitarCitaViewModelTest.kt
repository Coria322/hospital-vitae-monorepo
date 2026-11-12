package com.example.vitae_mobile.ui.citas

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// PROV-RF-002: el paciente pide una cita propia desde la app.
@OptIn(ExperimentalCoroutinesApi::class)
class SolicitarCitaViewModelTest {
    // Ciudad de México en invierno: UTC-6 todo el año desde 2022. Las pruebas usan una zona fija para no depender del equipo.
    private val zona = ZoneId.of("America/Mexico_City")
    private val ahora = Instant.parse("2026-11-10T16:00:00Z") // 10:00 hora local
    private val manana = LocalDate.of(2026, 11, 11)

    private class Pedida(val fechaHora: Instant, val especialidad: String?, val motivo: String?)

    private class Repo : CitasRepository {
        val pedidas = mutableListOf<Pedida>()
        var respuesta: Resultado<Cita>? = null
        var espera: CompletableDeferred<Unit>? = null

        override suspend fun citas(): Resultado<List<Cita>> = Resultado.Exito(emptyList())

        override suspend fun solicitar(fechaHora: Instant, especialidad: String?, motivo: String?): Resultado<Cita> {
            pedidas += Pedida(fechaHora, especialidad, motivo)
            espera?.await()
            return respuesta ?: Resultado.Exito(Cita("c-1", fechaHora, "PENDIENTE"))
        }
    }

    private val repo = Repo()
    private fun vm() = SolicitarCitaViewModel(repo, reloj = { ahora }, zona = { zona })

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    @Test
    fun alAbrirNoHayNadaElegidoYNoSePuedeEnviar() {
        val estado = vm().estado.value
        assertFalse(estado.completa)
        assertNull(estado.creada)
        assertNull(estado.error)
    }

    @Test
    fun sinFechaOSinHoraEnviarNoHaceNada() {
        val vm = vm()
        vm.enviar()
        vm.elegirFecha(manana)
        vm.enviar()
        assertEquals(emptyList<Any>(), repo.pedidas.toList())
        assertFalse(vm.estado.value.completa)
        vm.elegirHora(LocalTime.of(9, 30))
        assertTrue(vm.estado.value.completa)
    }

    // La hora se elige en la zona del teléfono y viaja en UTC: 09:30 en México son las 15:30 UTC.
    @Test
    fun laFechaYLaHoraLocalesViajanEnUtc() {
        val vm = vm()
        vm.elegirFecha(manana)
        vm.elegirHora(LocalTime.of(9, 30))

        vm.enviar()

        assertEquals(Instant.parse("2026-11-11T15:30:00Z"), repo.pedidas.single().fechaHora)
        assertEquals("c-1", vm.estado.value.creada?.id)
        assertEquals("PENDIENTE", vm.estado.value.creada?.estado)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun unaFechaPasadaOIgualAAhoraNoSeEnvia() {
        val vm = vm()
        vm.elegirFecha(LocalDate.of(2026, 11, 10))
        vm.elegirHora(LocalTime.of(10, 0)) // justo ahora

        vm.enviar()

        assertEquals(ErrorSolicitud.FechaPasada, vm.estado.value.error)
        assertEquals(emptyList<Any>(), repo.pedidas.toList())
        assertFalse(vm.estado.value.enviando)

        vm.elegirHora(LocalTime.of(10, 1)) // un minuto después ya vale, y cambiar algo limpia el aviso
        assertNull(vm.estado.value.error)
        vm.enviar()
        assertEquals(1, repo.pedidas.size)
    }

    @Test
    fun losTextosOpcionalesSeRecortanYLosVaciosNoSeMandan() {
        val vm = vm()
        vm.elegirFecha(manana)
        vm.elegirHora(LocalTime.NOON)
        vm.cambiarEspecialidad("   ")
        vm.cambiarMotivo("  Control de presión  ")

        vm.enviar()

        val p = repo.pedidas.single()
        assertNull("un texto en blanco no se manda", p.especialidad)
        assertEquals("Control de presión", p.motivo)
    }

    @Test
    fun losTextosSeLimitanALoQueAceptaElServidor() {
        val vm = vm()
        vm.cambiarEspecialidad("e".repeat(ESPECIALIDAD_MAX + 20))
        vm.cambiarMotivo("m".repeat(MOTIVO_MAX + 20))
        assertEquals(ESPECIALIDAD_MAX, vm.estado.value.especialidad.length)
        assertEquals(MOTIVO_MAX, vm.estado.value.motivo.length)
    }

    @Test
    fun siElServidorFallaSeMuestraElErrorYSePuedeReintentar() {
        val vm = vm()
        vm.elegirFecha(manana)
        vm.elegirHora(LocalTime.NOON)
        repo.respuesta = Resultado.Fallo(ApiError.Red)

        vm.enviar()

        assertEquals(ErrorSolicitud.Servidor(ApiError.Red), vm.estado.value.error)
        assertNull(vm.estado.value.creada)
        assertFalse(vm.estado.value.enviando)

        repo.respuesta = null
        vm.enviar()
        assertEquals(2, repo.pedidas.size)
        assertEquals("c-1", vm.estado.value.creada?.id)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun mientrasEnviaNoSeDuplicaLaSolicitud() {
        val vm = vm()
        vm.elegirFecha(manana)
        vm.elegirHora(LocalTime.NOON)
        repo.espera = CompletableDeferred()

        vm.enviar()
        assertTrue(vm.estado.value.enviando)
        vm.enviar() // doble toque
        assertEquals(1, repo.pedidas.size)

        repo.espera!!.complete(Unit)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun conLaCitaYaCreadaNoSeEnviaOtraVez() {
        val vm = vm()
        vm.elegirFecha(manana)
        vm.elegirHora(LocalTime.NOON)
        vm.enviar()
        vm.enviar()
        assertEquals(1, repo.pedidas.size)
    }
}
