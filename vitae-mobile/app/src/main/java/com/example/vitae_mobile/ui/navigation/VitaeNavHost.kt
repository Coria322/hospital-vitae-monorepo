package com.example.vitae_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vitae_mobile.AppContainer
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.MotivoCierre
import com.example.vitae_mobile.ui.login.LoginScreen
import com.example.vitae_mobile.ui.login.LoginViewModel
import com.example.vitae_mobile.ui.otp.OtpScreen
import com.example.vitae_mobile.ui.otp.OtpViewModel
import com.example.vitae_mobile.ui.screens.InicioScreen
import com.example.vitae_mobile.ui.screens.PantallaBase
import com.example.vitae_mobile.ui.sesion.SesionExpiradaScreen
import kotlinx.coroutines.launch

// Pantallas con implementación propia; el resto usa PantallaBase hasta su etapa.
private val Implementadas = setOf(Ruta.Login, Ruta.Otp, Ruta.Inicio, Ruta.SesionExpirada)

@Composable
fun VitaeNavHost(
    contenedor: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val estadoSesion by contenedor.sesion.estado.collectAsStateWithLifecycle()
    var previo by remember { mutableStateOf(estadoSesion) }
    val alcance = rememberCoroutineScope()

    // La sesión manda la navegación: al iniciar → Inicio; al cerrarse → Login, o "Sesión expirada" si no fue voluntario.
    LaunchedEffect(estadoSesion) {
        if (estadoSesion != previo) {
            val destino = when (val e = estadoSesion) {
                is EstadoSesion.Autenticada -> Ruta.Inicio
                is EstadoSesion.SinSesion ->
                    if (e.motivo == MotivoCierre.Inactividad || e.motivo == MotivoCierre.Expirada) Ruta.SesionExpirada else Ruta.Login
            }
            navController.irLimpiando(destino)
            previo = estadoSesion
        }
    }

    NavHost(navController, startDestination = Ruta.Login.ruta, modifier = modifier) {
        composable(Ruta.Login.ruta) {
            val vm: LoginViewModel = viewModel(
                factory = viewModelFactory { initializer { LoginViewModel(contenedor.authRepository, contenedor.sesion) } },
            )
            LoginScreen(vm, onOtp = { navController.navigate(Ruta.Otp.ruta) })
        }
        composable(Ruta.Otp.ruta) {
            val vm: OtpViewModel = viewModel(
                factory = viewModelFactory { initializer { OtpViewModel(contenedor.authRepository, contenedor.sesion) } },
            )
            OtpScreen(vm)
        }
        composable(Ruta.Inicio.ruta) {
            InicioScreen(
                onAbrir = { navController.navigate(it.ruta) },
                onCerrarSesion = { alcance.launch { contenedor.sesion.cerrar(MotivoCierre.Voluntario) } },
            )
        }
        composable(Ruta.SesionExpirada.ruta) {
            SesionExpiradaScreen(onIniciarSesion = { navController.irLimpiando(Ruta.Login) })
        }
        Ruta.entries.filter { it !in Implementadas }.forEach { destino ->
            composable(destino.ruta) { PantallaBase(titulo = stringResource(destino.titulo)) }
        }
    }
}

/** Navega y vacía la pila: tras iniciar o cerrar sesión no se debe poder volver con "atrás". */
private fun NavHostController.irLimpiando(destino: Ruta) =
    navigate(destino.ruta) { popUpTo(graph.id) { inclusive = true } }
