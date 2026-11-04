package com.example.vitae_mobile.ui.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

/** Lista de resultados propios (RF-010). La única acción por fila es "Abrir". */
@Composable
fun ResultadosScreen(viewModel: CargaViewModel<List<ResultadoEstudio>>, alAbrir: (String) -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.pantalla_resultados),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                ContenidoCarga(
                    estado = estado,
                    alReintentar = viewModel::cargar,
                    estaVacio = { it.isEmpty() },
                    mensajeVacio = R.string.resultados_vacio,
                ) { resultados ->
                    resultados.forEach { FilaResultado(it, alAbrir) }
                }
            }
        }
    }
}

@Composable
private fun FilaResultado(resultado: ResultadoEstudio, alAbrir: (String) -> Unit) {
    Tarjeta {
        Campo(stringResource(R.string.resultado_estudio), resultado.tipoEstudio)
        Campo(stringResource(R.string.resultado_fecha), formatearFecha(resultado.fechaHora))
        Campo(
            stringResource(R.string.resultado_archivo),
            stringResource(if (resultado.esPdf) R.string.resultado_pdf else R.string.resultado_imagen),
        )
        Button(onClick = { alAbrir(resultado.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.abrir))
        }
    }
}
