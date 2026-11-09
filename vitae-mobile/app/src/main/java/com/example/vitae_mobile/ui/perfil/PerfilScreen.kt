package com.example.vitae_mobile.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.perfil.formatearFechaFhir
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargandoEsqueleto
import com.example.vitae_mobile.ui.components.EstadoError
import com.example.vitae_mobile.ui.components.mensajeDeError
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

/** Solo lectura (RF-011): sin controles de edición. */
@Composable
fun PerfilScreen(viewModel: PerfilViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_perfil))
                when (val e = estado) {
                    PerfilUiState.Cargando -> CargandoEsqueleto(filas = 4)
                    is PerfilUiState.Listo -> DatosPerfil(e.perfil)
                    is PerfilUiState.Error -> EstadoError(
                        mensaje = stringResource(mensajeDeError(e.error)),
                        // Un 403 no se arregla reintentando (RF-003).
                        alReintentar = if (e.error == ApiError.SinPermisos) null else viewModel::cargar,
                    )
                }
            }
        }
    }
}

@Composable
private fun DatosPerfil(perfil: Perfil) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    Tarjeta(espacio = 16.dp) {
        Campo(stringResource(R.string.perfil_nombre), perfil.nombre ?: noDisponible)
        Campo(stringResource(R.string.perfil_curp), perfil.curp ?: noDisponible)
        Campo(stringResource(R.string.perfil_fecha_nacimiento), formatearFechaFhir(perfil.fechaNacimiento) ?: noDisponible)
        Campo(
            stringResource(R.string.perfil_seguro),
            when (perfil.seguroActivo) {
                true -> stringResource(R.string.perfil_seguro_activo)
                false -> stringResource(R.string.perfil_seguro_inactivo)
                null -> noDisponible
            },
        )
    }
}
