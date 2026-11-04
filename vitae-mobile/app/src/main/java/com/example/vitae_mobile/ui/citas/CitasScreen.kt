package com.example.vitae_mobile.ui.citas

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.formatearHora
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

/** Solo lectura (PROV-RF-002): fecha, hora y estado de cada cita propia; sin acciones por fila. */
@Composable
fun CitasScreen(viewModel: CargaViewModel<List<Cita>>, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.pantalla_citas),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                ContenidoCarga(
                    estado = estado,
                    alReintentar = viewModel::cargar,
                    estaVacio = { it.isEmpty() },
                    mensajeVacio = R.string.citas_vacio,
                ) { citas ->
                    // ponytail: Column y no LazyColumn; sobra para decenas de citas. Pasar a LazyColumn si hay cientos.
                    citas.forEach { FilaCita(it) }
                }
            }
        }
    }
}

@Composable
private fun FilaCita(cita: Cita) {
    Tarjeta {
        Campo(stringResource(R.string.cita_fecha), formatearFecha(cita.fechaHora))
        Campo(stringResource(R.string.cita_hora), formatearHora(cita.fechaHora))
        Campo(stringResource(R.string.cita_estado), cita.estado)
    }
}
