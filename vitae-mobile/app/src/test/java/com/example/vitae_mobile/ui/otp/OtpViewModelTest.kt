package com.example.vitae_mobile.ui.otp

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.FakeAuthRepository
import com.example.vitae_mobile.data.auth.MemoriaTokenStore
import com.example.vitae_mobile.data.auth.SESION_PRUEBA
import com.example.vitae_mobile.data.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
class OtpViewModelTest {
    private val repo = FakeAuthRepository()
    private val almacen = MemoriaTokenStore()

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    private fun TestScope.sesion() = SessionManager(almacen, repo, backgroundScope)

    @Test
    fun soloAceptaDigitosYSeisCaracteres() = runTest {
        val vm = OtpViewModel(repo, sesion())
        vm.cambiarCodigo("12a3-45 6789")
        assertEquals("123456", vm.estado.value.codigo)
    }

    @Test
    fun noVerificaConUnCodigoIncompleto() = runTest {
        val vm = OtpViewModel(repo, sesion())
        vm.cambiarCodigo("12345")
        vm.verificar()
        assertTrue(repo.codigosOtp.isEmpty())
    }

    @Test
    fun unCodigoCorrectoIniciaLaSesion() = runTest {
        repo.otp = Resultado.Exito(SESION_PRUEBA)
        val sesion = sesion()
        val vm = OtpViewModel(repo, sesion)
        vm.cambiarCodigo("123456")

        vm.verificar()

        assertEquals(listOf("123456"), repo.codigosOtp)
        assertEquals(EstadoSesion.Autenticada(SESION_PRUEBA), sesion.estado.value)
    }

    @Test
    fun unCodigoIncorrectoMuestraErrorYLimpiaElCampo() = runTest {
        repo.otp = Resultado.Fallo(ApiError.SesionInvalida)
        val sesion = sesion()
        val vm = OtpViewModel(repo, sesion)
        vm.cambiarCodigo("000000")

        vm.verificar()

        assertTrue(vm.estado.value.error)
        assertFalse(vm.estado.value.sinConexion)
        assertEquals("", vm.estado.value.codigo)
        assertEquals(EstadoSesion.SinSesion(), sesion.estado.value)
    }

    @Test
    fun unFalloDeRedNoSePresentaComoCodigoIncorrecto() = runTest {
        repo.otp = Resultado.Fallo(ApiError.Red)
        val vm = OtpViewModel(repo, sesion())
        vm.cambiarCodigo("123456")

        vm.verificar()

        assertTrue(vm.estado.value.sinConexion)
        assertFalse(vm.estado.value.error)
    }
}
