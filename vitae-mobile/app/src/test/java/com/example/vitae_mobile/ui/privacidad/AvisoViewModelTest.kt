package com.example.vitae_mobile.ui.privacidad

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.AvisoPrivacidad
import com.example.vitae_mobile.data.privacidad.Consentimiento
import com.example.vitae_mobile.data.privacidad.FinalidadSecundaria
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.privacidad.SolicitudConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import com.example.vitae_mobile.ui.components.Carga
import java.time.Instant
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

// PROV-RN-008 / TASK-M-012: aviso de privacidad y consentimientos.
@OptIn(ExperimentalCoroutinesApi::class)
class AvisoViewModelTest {
    private fun avisoDePrueba(aceptada: Boolean = false, aceptadas: Set<String> = emptySet()) = AvisoPrivacidad(
        version = "2.0",
        vigenteDesde = Instant.parse("2026-10-01T00:00:00Z"),
        textoSimplificado = "Texto de ejemplo",
        urlIntegral = "https://example.com/integral",
        finalidadesSecundarias = listOf("A", "B", "C").map { FinalidadSecundaria(it, "Finalidad $it", it in aceptadas) },
        aceptada = aceptada,
    )

    private class Repo(var respuestaAviso: Resultado<AvisoPrivacidad>) : PrivacidadRepository {
        val enviadas = mutableListOf<SolicitudConsentimiento>()
        var respuestaRegistro: (SolicitudConsentimiento) -> Resultado<Consentimiento> = {
            Resultado.Exito(Consentimiento("con-${enviadas.size}", Instant.EPOCH))
        }

        override suspend fun aviso() = respuestaAviso

        override suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
            enviadas += solicitud
            return respuestaRegistro(solicitud)
        }
    }

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    private fun aceptacion(repo: Repo) = AvisoViewModel(repo, ModoAviso.Aceptacion)

    private fun gestion(repo: Repo) = AvisoViewModel(repo, ModoAviso.Gestion)

    // Escenario 1: un paciente nuevo ve el aviso con las finalidades opcionales sin marcar.
    @Test
    fun enLaAceptacionLasFinalidadesOpcionalesArrancanDesmarcadasAunqueYaSeHubierenAceptado() = runTest {
        val vm = aceptacion(Repo(Resultado.Exito(avisoDePrueba(aceptada = false, aceptadas = setOf("A", "B")))))

        assertTrue(vm.estado.value.aviso is Carga.Listo)
        assertEquals(emptySet<String>(), vm.estado.value.seleccion)
        assertFalse(vm.estado.value.terminado)
    }

    @Test
    fun unaVersionYaAceptadaNoPideNadaYContinua() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba(aceptada = true)))
        val vm = aceptacion(repo)

        assertTrue(vm.estado.value.terminado)
        assertTrue(repo.enviadas.isEmpty())
    }

    // Registrar la versión mostrada y las finalidades que el paciente marcó.
    @Test
    fun aceptarRegistraLaVersionMostradaYLasFinalidadesMarcadas() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba()))
        val vm = aceptacion(repo)
        vm.alternar("C")
        vm.alternar("A")

        vm.confirmar()

        assertEquals(
            listOf(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "2.0", listOf("A", "C"), AccionConsentimiento.ACEPTAR)),
            repo.enviadas,
        )
        assertTrue(vm.estado.value.terminado)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun sePuedeAceptarSinMarcarNingunaFinalidadOpcional() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba()))
        val vm = aceptacion(repo)

        vm.confirmar()

        assertEquals(emptyList<String>(), repo.enviadas.single().finalidades)
        assertTrue(vm.estado.value.terminado)
    }

    @Test
    fun alternarMarcaYDesmarca() = runTest {
        val vm = aceptacion(Repo(Resultado.Exito(avisoDePrueba())))
        vm.alternar("B")
        assertEquals(setOf("B"), vm.estado.value.seleccion)
        vm.alternar("B")
        assertEquals(emptySet<String>(), vm.estado.value.seleccion)
    }

    @Test
    fun unErrorAlRegistrarNoContinuaYSePuedeReintentar() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba()))
        repo.respuestaRegistro = { Resultado.Fallo(ApiError.Red) }
        val vm = aceptacion(repo)

        vm.confirmar()

        assertEquals(ApiError.Red, vm.estado.value.errorEnvio)
        assertFalse(vm.estado.value.terminado)
        assertFalse(vm.estado.value.enviando)

        repo.respuestaRegistro = { Resultado.Exito(Consentimiento("con-ok", Instant.EPOCH)) }
        vm.confirmar()

        assertTrue(vm.estado.value.terminado)
        assertEquals(2, repo.enviadas.size)
    }

    @Test
    fun unSegundoToqueMientrasSeEnviaNoDuplicaElRegistro() = runTest {
        val respuesta = CompletableDeferred<Resultado<Consentimiento>>()
        val repo = object : PrivacidadRepository {
            val enviadas = mutableListOf<SolicitudConsentimiento>()
            override suspend fun aviso() = Resultado.Exito(avisoDePrueba())
            override suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
                enviadas += solicitud
                return respuesta.await()
            }
        }
        val vm = AvisoViewModel(repo, ModoAviso.Aceptacion)

        vm.confirmar()
        vm.confirmar()
        respuesta.complete(Resultado.Exito(Consentimiento("con-1", Instant.EPOCH)))

        assertEquals(1, repo.enviadas.size)
    }

    @Test
    fun siNoCargaElAvisoSeMuestraElErrorConReintento() = runTest {
        val repo = Repo(Resultado.Fallo(ApiError.Red))
        val vm = aceptacion(repo)
        assertEquals(Carga.Error(ApiError.Red), vm.estado.value.aviso)

        repo.respuestaAviso = Resultado.Exito(avisoDePrueba())
        vm.cargar()

        assertTrue(vm.estado.value.aviso is Carga.Listo)
    }

    @Test
    fun sinElAvisoCargadoConfirmarNoEnviaNada() = runTest {
        val repo = Repo(Resultado.Fallo(ApiError.Red))
        aceptacion(repo).confirmar()
        assertTrue(repo.enviadas.isEmpty())
    }

    // Gestión desde Inicio.
    @Test
    fun laGestionArrancaConLoQueElServidorTieneRegistrado() = runTest {
        val vm = gestion(Repo(Resultado.Exito(avisoDePrueba(aceptada = true, aceptadas = setOf("A", "C")))))

        assertEquals(setOf("A", "C"), vm.estado.value.seleccion)
        assertFalse(vm.estado.value.terminado) // la gestión no sale sola aunque el aviso esté aceptado
        assertFalse(vm.hayCambios())
    }

    @Test
    fun aceptarUnaFinalidadNuevaEnviaSoloLaNueva() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba(aceptada = true, aceptadas = setOf("A"))))
        val vm = gestion(repo)
        vm.alternar("B")
        assertTrue(vm.hayCambios())

        vm.confirmar()

        assertEquals(
            listOf(SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "2.0", listOf("B"), AccionConsentimiento.ACEPTAR)),
            repo.enviadas,
        )
        assertTrue(vm.estado.value.terminado)
    }

    // Escenario 2: revocar un consentimiento secundario vigente envía accion = REVOCAR.
    @Test
    fun revocarUnaFinalidadEnviaLaAccionRevocar() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba(aceptada = true, aceptadas = setOf("A", "B"))))
        val vm = gestion(repo)
        vm.alternar("A")

        vm.confirmar()

        assertEquals(
            listOf(SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "2.0", listOf("A"), AccionConsentimiento.REVOCAR)),
            repo.enviadas,
        )
    }

    @Test
    fun unCambioMixtoEnviaUnaAceptacionYUnaRevocacion() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba(aceptada = true, aceptadas = setOf("A"))))
        val vm = gestion(repo)
        vm.alternar("A")
        vm.alternar("C")

        vm.confirmar()

        assertEquals(listOf(AccionConsentimiento.ACEPTAR, AccionConsentimiento.REVOCAR), repo.enviadas.map { it.accion })
        assertEquals(listOf(listOf("C"), listOf("A")), repo.enviadas.map { it.finalidades })
    }

    // Si la segunda solicitud falla, lo ya registrado no se vuelve a enviar al reintentar.
    @Test
    fun siFallaLaSegundaSolicitudElReintentoSoloEnviaLaPendiente() = runTest {
        val repo = Repo(Resultado.Exito(avisoDePrueba(aceptada = true, aceptadas = setOf("A"))))
        repo.respuestaRegistro = { if (it.accion == AccionConsentimiento.REVOCAR) Resultado.Fallo(ApiError.Red) else Resultado.Exito(Consentimiento("ok", Instant.EPOCH)) }
        val vm = gestion(repo)
        vm.alternar("A")
        vm.alternar("C")

        vm.confirmar()
        assertEquals(ApiError.Red, vm.estado.value.errorEnvio)
        assertFalse(vm.estado.value.terminado)
        assertTrue(vm.hayCambios()) // queda pendiente la revocación

        repo.enviadas.clear()
        repo.respuestaRegistro = { Resultado.Exito(Consentimiento("ok", Instant.EPOCH)) }
        vm.confirmar()

        assertEquals(listOf(AccionConsentimiento.REVOCAR), repo.enviadas.map { it.accion })
        assertTrue(vm.estado.value.terminado)
    }
}
