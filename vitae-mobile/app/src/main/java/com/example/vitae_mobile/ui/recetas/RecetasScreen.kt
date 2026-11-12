package com.example.vitae_mobile.ui.recetas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

@StringRes
fun textoEstado(estado: EstadoReceta): Int = when (estado) {
    EstadoReceta.Activa -> R.string.receta_estado_activa
    EstadoReceta.Dispensada -> R.string.receta_estado_dispensada
    EstadoReceta.Cancelada -> R.string.receta_estado_cancelada
    EstadoReceta.Desconocido -> R.string.dato_no_disponible
}

/** Lista de recetas propias (RF-009). Solo lectura: la única acción por fila es "Abrir" (UI_UX_SPEC). */
@Composable
fun RecetasScreen(viewModel: CargaViewModel<List<Receta>>, alAbrir: (String) -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_recetas))
                BotonSecundario(onClick = viewModel::cargar) {
                    Text("Sincronizar recetas")
                }
                ContenidoCarga(
                    estado = estado,
                    alReintentar = viewModel::cargar,
                    estaVacio = { it.isEmpty() },
                    mensajeVacio = R.string.recetas_vacio,
                ) { recetas ->
                    recetas.forEach { FilaReceta(it, alAbrir) }
                }
            }
        }
    }
}

@Composable
private fun FilaReceta(receta: Receta, alAbrir: (String) -> Unit) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    Tarjeta {
        Campo(stringResource(R.string.receta_medicamento), receta.medicamento ?: noDisponible)
        // El estado va siempre como texto: nunca solo con color (WCAG 2.1 AA).
        Campo(stringResource(R.string.receta_estado), stringResource(textoEstado(receta.estado)))
        Button(onClick = { alAbrir(receta.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.abrir))
        }
    }
}
