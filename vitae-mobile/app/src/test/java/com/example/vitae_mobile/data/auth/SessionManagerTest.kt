package com.example.vitae_mobile.data.auth

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    private val repo = FakeAuthRepository()
    private val almacen = MemoriaTokenStore()

    private fun TestScope.gestor() = SessionManager(almacen, repo, backgroundScope)

    @Test
    fun iniciarGuardaLaSesionYExponeElToken() = runTest {
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        assertEquals(SESION_PRUEBA, almacen.sesion)
        assertEquals("acc-1", gestor.token())
        assertEquals(EstadoSesion.Autenticada(SESION_PRUEBA), gestor.estado.value)
    }

    // PROV-SEC-002, escenario 2: al cerrar se borran los datos locales y se revoca el access token.
    @Test
    fun cerrarBorraLoLocalYRevocaElToken() = runTest {
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        gestor.cerrar(MotivoCierre.Inactividad)

        assertNull(almacen.sesion)
        assertNull(gestor.token())
        assertEquals(EstadoSesion.SinSesion(MotivoCierre.Inactividad), gestor.estado.value)
        assertEquals(listOf("acc-1"), repo.revocados)
    }

    @Test
    fun expirarLimpiaSinRevocar() = runTest {
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        gestor.expirar()

        assertNull(almacen.sesion)
        assertEquals(EstadoSesion.SinSesion(MotivoCierre.Expirada), gestor.estado.value)
        assertTrue(repo.revocados.isEmpty())
    }

    @Test
    fun alArrancarSeDescartaYRevocaLaSesionDeUnProcesoAnterior() = runTest {
        almacen.sesion = SESION_PRUEBA
        val gestor = gestor()

        gestor.descartarSesionPrevia()

        assertNull(almacen.sesion)
        assertEquals(listOf("acc-1"), repo.revocados)
        assertEquals(EstadoSesion.SinSesion(), gestor.estado.value)
    }

    @Test
    fun renuevaUnMinutoAntesDeQueVenzaElAccessToken() = runTest {
        repo.renovacion = Resultado.Exito(Sesion("acc-2", "ref-2", 900))
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        advanceTimeBy(839_999)
        runCurrent()
        assertTrue(repo.refreshUsados.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("ref-1"), repo.refreshUsados)
        assertEquals("acc-2", gestor.token())
        assertEquals("ref-2", almacen.sesion?.refreshToken)
    }

    // Caso borde de PROV-SEC-002: refresh reutilizado o revocado → nuevo login.
    @Test
    fun siElRefreshEsRechazadoSeFuerzaElLogin() = runTest {
        repo.renovacion = Resultado.Fallo(ApiError.SesionInvalida)
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        advanceTimeBy(840_000)
        runCurrent()

        assertEquals(EstadoSesion.SinSesion(MotivoCierre.Expirada), gestor.estado.value)
        assertNull(almacen.sesion)
    }

    @Test
    fun unFalloDeRedAlRenovarNoCierraLaSesion() = runTest {
        repo.renovacion = Resultado.Fallo(ApiError.Red)
        val gestor = gestor()
        gestor.iniciar(SESION_PRUEBA)

        advanceTimeBy(840_000)
        runCurrent()

        assertEquals(EstadoSesion.Autenticada(SESION_PRUEBA), gestor.estado.value)
    }
}
