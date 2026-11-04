package com.example.vitae_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vitae_mobile.ui.screens.InicioScreen
import com.example.vitae_mobile.ui.screens.PantallaBase

@Composable
fun VitaeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // ponytail: arranca en Inicio para poder recorrer las pantallas; con la etapa de sesión (RF-002) arrancará en Login.
    NavHost(navController, startDestination = Ruta.Inicio.ruta, modifier = modifier) {
        composable(Ruta.Inicio.ruta) {
            InicioScreen(onAbrir = { navController.navigate(it.ruta) })
        }
        Ruta.entries.filter { it != Ruta.Inicio }.forEach { destino ->
            composable(destino.ruta) { PantallaBase(titulo = stringResource(destino.titulo)) }
        }
    }
}
