package com.example.vitae_mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.CargandoEsqueleto
import com.example.vitae_mobile.ui.navigation.Ruta
import com.example.vitae_mobile.ui.perfil.PerfilUiState

// Secciones del paciente (UI_UX_SPEC.md, navigation flow): solo lectura, sin acciones de edición (RF-011).
private val Secciones = listOf(Ruta.Citas, Ruta.Recetas, Ruta.Resultados, Ruta.Perfil, Ruta.Arco)

@Composable
fun InicioScreen(
    perfil: PerfilUiState,
    onAbrir: (Ruta) -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            // El saludo es opcional: si el perfil falla, Inicio sigue siendo usable; el error se ve en Perfil.
            when (perfil) {
                PerfilUiState.Cargando -> CargandoEsqueleto(filas = 1)
                is PerfilUiState.Listo -> perfil.perfil.nombre?.let {
                    Text(text = stringResource(R.string.inicio_saludo, it), style = MaterialTheme.typography.titleMedium)
                }
                is PerfilUiState.Error -> Unit
            }
            Secciones.forEach { seccion ->
                // PROV-UX-003: objetivo táctil >= 48 dp.
                Button(
                    onClick = { onAbrir(seccion) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(seccion.titulo))
                }
            }
            OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.inicio_cerrar_sesion))
            }
        }
    }
}
