package com.example.vitae_mobile.ui.login

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.FakeAuthRepository
import com.example.vitae_mobile.data.auth.MemoriaTokenStore
import com.example.vitae_mobile.data.auth.ResultadoLogin
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val repo = FakeAuthRepository()
    private val almacen = MemoriaTokenStore()

    @Before
    fun iniciar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun terminar() = Dispatchers.resetMain()

    private class Escenario(val vm: LoginViewModel, val sesion: SessionManager)

    private fun TestScope.escenario(): Escenario {
        val sesion = SessionManager(almacen, repo, backgroundScope)
        return Escenario(LoginViewModel(repo, sesion), sesion)
    }

    private fun LoginViewModel.llenar() {
        cambiarCorreo("  ana@vitae.test ")
        cambiarContrasena("secreta")
    }

    @Test
    fun noLlamaAlRepositorioConCamposVacios() = runTest {
        val e = escenario()
        e.vm.iniciarSesion()
        e.vm.cambiarCorreo("ana@vitae.test")
        e.vm.iniciarSesion()

        assertTrue(repo.intentosLogin.isEmpty())
    }

    @Test
    fun credencialesIncorrectasDanUnMensajeGenericoYLimpianLaContrasena() = runTest {
        repo.login = Resultado.Fallo(ApiError.SesionInvalida)
        val e = escenario()
        e.vm.llenar()

        e.vm.iniciarSesion()

        assertEquals(ErrorLogin.Credenciales, e.vm.estado.value.error)
        assertEquals("", e.vm.estado.value.contrasena)
        assertEquals("  ana@vitae.test ", e.vm.estado.value.correo)
        assertFalse(e.vm.estado.value.cargando)
        assertEquals(listOf("ana@vitae.test" to "secreta"), repo.intentosLogin) // el correo se envía sin espacios
        assertEquals(EstadoSesion.SinSesion(), e.sesion.estado.value)
    }

    // Caso borde de PROV-SEC-001: tercer intento fallido → HTTP 423 → alerta de contactar a administración.
    @Test
    fun laCuentaBloqueadaMuestraLaAlerta() = runTest {
        repo.login = Resultado.Fallo(ApiError.CuentaBloqueada)
        val e = escenario()
        e.vm.llenar()
        e.vm.iniciarSesion()

        assertEquals(ErrorLogin.CuentaBloqueada, e.vm.estado.value.error)
        e.vm.cerrarAlerta()
        assertNull(e.vm.estado.value.error)
    }

    // RF-002, escenario 2: credenciales de personal → 403 → "Acceso no disponible en la app".
    @Test
    fun lasCredencialesDePersonalNoTienenAcceso() = runTest {
        repo.login = Resultado.Fallo(ApiError.SinPermisos)
        val e = escenario()
        e.vm.llenar()
        e.vm.iniciarSesion()

        assertEquals(ErrorLogin.SinAcceso, e.vm.estado.value.error)
    }

    @Test
    fun loserroresDeConexionTienenSuPropioMensaje() = runTest {
        val e = escenario()
        e.vm.llenar()
        repo.login = Resultado.Fallo(ApiError.Red)
        e.vm.iniciarSesion()
        assertEquals(ErrorLogin.Red, e.vm.estado.value.error)

        e.vm.cambiarContrasena("secreta")
        repo.login = Resultado.Fallo(ApiError.Servidor)
        e.vm.iniciarSesion()
        assertEquals(ErrorLogin.Red, e.vm.estado.value.error)

        e.vm.cambiarContrasena("secreta")
        repo.login = Resultado.Fallo(ApiError.AppIncompatible)
        e.vm.iniciarSesion()
        assertEquals(ErrorLogin.Incompatible, e.vm.estado.value.error)
    }

    // PROV-SEC-001, escenario 1: dispositivo nuevo → OTP, sin tokens.
    @Test
    fun unDispositivoNuevoPideElOtpYNoIniciaSesion() = runTest {
        repo.login = Resultado.Exito(ResultadoLogin.RequiereOtp)
        val e = escenario()
        e.vm.llenar()
        e.vm.iniciarSesion()

        assertTrue(e.vm.estado.value.requiereOtp)
        assertEquals("", e.vm.estado.value.contrasena)
        assertEquals(EstadoSesion.SinSesion(), e.sesion.estado.value)
        assertNull(almacen.sesion)

        e.vm.otpAbierto()
        assertFalse(e.vm.estado.value.requiereOtp)
    }

    @Test
    fun unLoginCorrectoIniciaLaSesion() = runTest {
        repo.login = Resultado.Exito(ResultadoLogin.Autenticado(SESION_PRUEBA))
        val e = escenario()
        e.vm.llenar()
        e.vm.iniciarSesion()

        assertEquals(EstadoSesion.Autenticada(SESION_PRUEBA), e.sesion.estado.value)
        assertEquals("", e.vm.estado.value.contrasena)
        assertNull(e.vm.estado.value.error)
    }

    @Test
    fun escribirOcultaElErrorEnLinea() = runTest {
        repo.login = Resultado.Fallo(ApiError.SesionInvalida)
        val e = escenario()
        e.vm.llenar()
        e.vm.iniciarSesion()

        e.vm.cambiarContrasena("otra")

        assertNull(e.vm.estado.value.error)
    }
}
