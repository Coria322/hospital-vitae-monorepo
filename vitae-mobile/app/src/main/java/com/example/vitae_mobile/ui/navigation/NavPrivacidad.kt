package com.example.vitae_mobile.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.vitae_mobile.AppContainer
import com.example.vitae_mobile.ui.arco.ArcoScreen
import com.example.vitae_mobile.ui.arco.ArcoViewModel
import com.example.vitae_mobile.ui.privacidad.AvisoScreen
import com.example.vitae_mobile.ui.privacidad.AvisoViewModel
import com.example.vitae_mobile.ui.privacidad.ModoAviso

/**
 * Privacidad: el aviso que se acepta tras iniciar sesión (Login → OTP → Aviso → Inicio), la pantalla de preferencias
 * desde Inicio y la solicitud ARCO. [alRechazarAviso] cierra la sesión.
 */
fun NavGraphBuilder.privacidad(navController: NavHostController, contenedor: AppContainer, alRechazarAviso: () -> Unit) {
    composable(Ruta.Aviso.ruta) {
        val vm: AvisoViewModel = viewModel(
            factory = viewModelFactory { initializer { AvisoViewModel(contenedor.privacidadRepository, ModoAviso.Aceptacion) } },
        )
        AvisoScreen(vm, ModoAviso.Aceptacion, alTerminar = { navController.irLimpiando(Ruta.Inicio) }, alRechazar = alRechazarAviso)
    }
    composable(Ruta.Privacidad.ruta) {
        val vm: AvisoViewModel = viewModel(
            factory = viewModelFactory { initializer { AvisoViewModel(contenedor.privacidadRepository, ModoAviso.Gestion) } },
        )
        AvisoScreen(vm, ModoAviso.Gestion, alTerminar = { navController.popBackStack() }, alRechazar = {})
    }
    composable(Ruta.Arco.ruta) {
        val vm: ArcoViewModel = viewModel(
            factory = viewModelFactory { initializer { ArcoViewModel(contenedor.arcoRepository) } },
        )
        ArcoScreen(vm)
    }
}
