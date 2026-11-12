package com.example.vitae_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vitae_mobile.AppContainer
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.MotivoCierre
import com.example.vitae_mobile.data.seguridad.EstadoDispositivo
import com.example.vitae_mobile.ui.components.LocalAtras
import com.example.vitae_mobile.ui.login.LoginScreen
import com.example.vitae_mobile.ui.login.LoginViewModel
import com.example.vitae_mobile.ui.otp.OtpScreen
import com.example.vitae_mobile.ui.otp.OtpViewModel
import com.example.vitae_mobile.ui.paciente.PacienteScreen
import com.example.vitae_mobile.ui.paciente.PacienteViewModel
import com.example.vitae_mobile.ui.perfil.PerfilScreen
import com.example.vitae_mobile.ui.perfil.PerfilViewModel
import com.example.vitae_mobile.ui.privacidad.AvisoPrevioScreen
import com.example.vitae_mobile.ui.screens.InicioScreen
import com.example.vitae_mobile.ui.screens.ResumenViewModel
import com.example.vitae_mobile.ui.seguridad.PantallaBloqueo
import com.example.vitae_mobile.ui.sesion.SesionExpiradaScreen
import kotlinx.coroutines.launch

@Composable
fun VitaeNavHost(
    contenedor: AppContainer,
    alSalir: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val estadoSesion by contenedor.sesion.estado.collectAsStateWithLifecycle()
    var previo by remember { mutableStateOf(estadoSesion) }
    val estadoDispositivo by contenedor.dispositivo.estado.collectAsStateWithLifecycle()
    var dispositivoPrevio by remember { mutableStateOf(estadoDispositivo) }
    val alcance = rememberCoroutineScope()

    // Sin sesión se entra por el aviso de privacidad la primera vez (y cuando cambia su versión), después por Login.
    fun destinoSinSesion() = if (contenedor.avisoPrevio.visto()) Ruta.Login else Ruta.AvisoPrevio

    // En el portal del paciente, tras iniciar sesión se pasa al aviso de privacidad y de ahí a Inicio.
    fun destinoTrasLogin(): Ruta = Ruta.Aviso

    // PROV-SEC-011 / RNF-006: un dispositivo bloqueado manda sobre todo lo demás y no tiene salida.
    LaunchedEffect(estadoDispositivo) {
        if (estadoDispositivo != dispositivoPrevio) {
            navController.irLimpiando(
                when (estadoDispositivo) {
                    EstadoDispositivo.RootDetectado -> Ruta.RootBloqueado
                    EstadoDispositivo.Incompatible -> Ruta.Incompatible
                    EstadoDispositivo.Apto -> destinoSinSesion()
                },
            )
            dispositivoPrevio = estadoDispositivo
        }
    }

    // La sesión manda la navegación: al iniciar → Inicio; al cerrarse → Login, o "Sesión expirada" si no fue voluntario.
    LaunchedEffect(estadoSesion) {
        if (estadoSesion != previo) {
            // Si el cierre vino de un bloqueo del dispositivo, la pantalla de bloqueo es la que manda.
            val destino = if (contenedor.dispositivo.bloqueado()) null else when (val e = estadoSesion) {
                // Tras iniciar sesión se pasa por el aviso de privacidad; si ya está aceptado, sigue solo a Inicio.
                is EstadoSesion.Autenticada -> destinoTrasLogin()
                is EstadoSesion.SinSesion ->
                    if (e.motivo == MotivoCierre.Inactividad || e.motivo == MotivoCierre.Expirada) Ruta.SesionExpirada else Ruta.Login
            }
            if (destino != null) navController.irLimpiando(destino)
            previo = estadoSesion
        }
    }

    // Ruta inicial: bloqueo del dispositivo primero; si la actividad se reabre con la sesión aún viva (el proceso sigue),
    // la puerta del aviso, que reenvía a Inicio si ya está aceptado, y no Login.
    val destinoInicial = remember {
        when {
            estadoDispositivo == EstadoDispositivo.RootDetectado -> Ruta.RootBloqueado
            estadoDispositivo == EstadoDispositivo.Incompatible -> Ruta.Incompatible
            estadoSesion is EstadoSesion.Autenticada -> destinoTrasLogin()
            else -> destinoSinSesion()
        }
    }

    // El botón de volver de cada cabecera aparece solo si hay una pantalla anterior; hace lo mismo que el gesto de atrás.
    val entradaActual by navController.currentBackStackEntryAsState()
    val atras: (() -> Unit)? = if (entradaActual != null && navController.previousBackStackEntry != null) {
        { navController.popBackStack() }
    } else {
        null
    }

    CompositionLocalProvider(LocalAtras provides atras) {
    NavHost(navController, startDestination = destinoInicial.ruta, modifier = modifier) {
        composable(Ruta.AvisoPrevio.ruta) {
            AvisoPrevioScreen(
                onContinuar = {
                    contenedor.avisoPrevio.marcarVisto()
                    navController.irLimpiando(Ruta.Login)
                },
            )
        }
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
        composable(Ruta.Paciente.ruta) {
            contenedor.backend?.let { backend ->
                val vm: PacienteViewModel = viewModel(
                    factory = viewModelFactory { initializer { PacienteViewModel(backend) } },
                )
                PacienteScreen(
                    vm,
                    alElegir = { navController.irLimpiando(Ruta.Aviso) },
                    alCerrarSesion = { alcance.launch { contenedor.sesion.cerrar(MotivoCierre.Voluntario) } },
                )
            }
        }
        composable(Ruta.Inicio.ruta) {
            val vm: PerfilViewModel = viewModel(
                factory = viewModelFactory { initializer { PerfilViewModel(contenedor.perfilRepository) } },
            )
            val perfil by vm.estado.collectAsStateWithLifecycle()
            val resumenVm: ResumenViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ResumenViewModel(contenedor.citasRepository, contenedor.recetasRepository, contenedor.resultadosRepository)
                    }
                },
            )
            val resumen by resumenVm.estado.collectAsStateWithLifecycle()
            InicioScreen(
                perfil = perfil,
                resumen = resumen,
                alReintentarResumen = {
                    vm.cargar()
                    resumenVm.cargar()
                },
                onAbrir = { navController.navigate(it.ruta) },
                onCerrarSesion = { alcance.launch { contenedor.sesion.cerrar(MotivoCierre.Voluntario) } },
            )
        }
        composable(Ruta.Perfil.ruta) {
            val vm: PerfilViewModel = viewModel(
                factory = viewModelFactory { initializer { PerfilViewModel(contenedor.perfilRepository) } },
            )
            PerfilScreen(vm)
        }
        clinico(navController, contenedor)
        privacidad(navController, contenedor, alRechazarAviso = { alcance.launch { contenedor.sesion.cerrar(MotivoCierre.Voluntario) } })
        composable(Ruta.SesionExpirada.ruta) {
            SesionExpiradaScreen(onIniciarSesion = { navController.irLimpiando(Ruta.Login) })
        }
        composable(Ruta.Incompatible.ruta) {
            PantallaBloqueo(R.string.pantalla_incompatible, R.string.bloqueo_incompatible_texto, alSalir)
        }
        composable(Ruta.RootBloqueado.ruta) {
            PantallaBloqueo(R.string.pantalla_root, R.string.bloqueo_root_texto, alSalir)
        }
    }
    }
}

/** Navega y vacía la pila: tras iniciar o cerrar sesión no se debe poder volver con "atrás". */
internal fun NavHostController.irLimpiando(destino: Ruta) =
    navigate(destino.ruta) { popUpTo(graph.id) { inclusive = true } }
