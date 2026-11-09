package com.example.vitae_mobile.ui.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.vitae_mobile.ui.components.EstadoError
import com.example.vitae_mobile.ui.components.mensajeDeError
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

/**
 * Visor interno de un resultado (RF-010): PDF o imagen, sin guardar en almacenamiento compartido ni compartir.
 * Un archivo grande muestra el avance de la descarga y no bloquea la interfaz.
 */
@Composable
fun ResultadoVisorScreen(viewModel: ResultadoVisorViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_resultado_visor))
                when (val e = estado) {
                    is ArchivoUiState.Descargando -> Descarga(e.progreso)
                    is ArchivoUiState.Error -> EstadoError(
                        mensaje = stringResource(mensajeDeError(e.error)),
                        // Un 403 (resultado ajeno) no muestra datos ni ofrece reintento.
                        alReintentar = if (e.error == ApiError.SinPermisos) null else viewModel::cargar,
                    )
                    is ArchivoUiState.Listo -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val (archivo, mime) = e.archivo
                        when {
                            mime == "application/pdf" -> VisorPdf(archivo)
                            mime.startsWith("image/") -> Column(Modifier.verticalScroll(rememberScrollState())) { VisorImagen(archivo) }
                            mime.startsWith("text/") -> VisorTexto(archivo)
                            else -> EstadoError(stringResource(R.string.error_recurso), alReintentar = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Descarga(progreso: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.resultado_descargando, (progreso * 100).toInt()),
            style = MaterialTheme.typography.bodyLarge,
        )
        LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
    }
}
