package com.example.vitae_mobile.ui.navigation

import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vitae_mobile.AppContainer
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.ui.citas.CitasScreen
import com.example.vitae_mobile.ui.citas.SolicitarCitaScreen
import com.example.vitae_mobile.ui.citas.SolicitarCitaViewModel
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.recetas.RecetaDetalleScreen
import com.example.vitae_mobile.ui.recetas.RecetasScreen
import com.example.vitae_mobile.ui.resultados.ResultadoVisorScreen
import com.example.vitae_mobile.ui.resultados.ResultadoVisorViewModel
import com.example.vitae_mobile.ui.resultados.ResultadosScreen

/** Rutas con argumento: el detalle de una receta y el visor de un resultado (UI_UX_SPEC). */
object Destinos {
    const val ARG_ID = "id"
    const val RECETA = "recetas/{$ARG_ID}"
    const val RESULTADO = "resultados/{$ARG_ID}"

    fun receta(id: String) = "recetas/${Uri.encode(id)}"
    fun resultado(id: String) = "resultados/${Uri.encode(id)}"
}

private val argumentoId = listOf(navArgument(Destinos.ARG_ID) { type = NavType.StringType })

/** Pantallas clínicas: citas (con la solicitud de una nueva), recetas (lista y detalle) y resultados (lista y visor). */
fun NavGraphBuilder.clinico(navController: NavHostController, contenedor: AppContainer) {
    composable(Ruta.Citas.ruta) {
        val vm: CargaViewModel<List<Cita>> = viewModel(
            factory = viewModelFactory { initializer { CargaViewModel { contenedor.citasRepository.citas() } } },
        )
        CitasScreen(vm, alSolicitar = { navController.navigate(Ruta.SolicitarCita.ruta) })
    }
    composable(Ruta.SolicitarCita.ruta) {
        val vm: SolicitarCitaViewModel = viewModel(
            factory = viewModelFactory { initializer { SolicitarCitaViewModel(contenedor.citasRepository) } },
        )
        SolicitarCitaScreen(
            vm,
            // Citas se crea de nuevo para que lea la lista con la cita recién pedida.
            alVerCitas = { navController.navigate(Ruta.Citas.ruta) { popUpTo(Ruta.Citas.ruta) { inclusive = true } } },
        )
    }
    composable(Ruta.Recetas.ruta) {
        val vm: CargaViewModel<List<Receta>> = viewModel(
            factory = viewModelFactory { initializer { CargaViewModel { contenedor.recetasRepository.recetas() } } },
        )
        RecetasScreen(vm, alAbrir = { navController.navigate(Destinos.receta(it)) })
    }
    composable(Destinos.RECETA, arguments = argumentoId) { entrada ->
        val id = entrada.arguments?.getString(Destinos.ARG_ID).orEmpty()
        val vm: CargaViewModel<Receta> = viewModel(
            factory = viewModelFactory { initializer { CargaViewModel { contenedor.recetasRepository.receta(id) } } },
        )
        RecetaDetalleScreen(vm)
    }
    composable(Ruta.Resultados.ruta) {
        val vm: CargaViewModel<List<ResultadoEstudio>> = viewModel(
            factory = viewModelFactory { initializer { CargaViewModel { contenedor.resultadosRepository.resultados() } } },
        )
        ResultadosScreen(vm, alAbrir = { navController.navigate(Destinos.resultado(it)) })
    }
    composable(Destinos.RESULTADO, arguments = argumentoId) { entrada ->
        val id = entrada.arguments?.getString(Destinos.ARG_ID).orEmpty()
        val vm: ResultadoVisorViewModel = viewModel(
            factory = viewModelFactory { initializer { ResultadoVisorViewModel(contenedor.resultadosRepository, id) } },
        )
        ResultadoVisorScreen(vm)
    }
}
