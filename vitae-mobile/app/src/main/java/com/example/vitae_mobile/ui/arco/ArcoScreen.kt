package com.example.vitae_mobile.ui.arco

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha

@StringRes
private fun nombre(derecho: DerechoArco) = when (derecho) {
    DerechoArco.Acceso -> R.string.derecho_acceso
    DerechoArco.Rectificacion -> R.string.derecho_rectificacion
    DerechoArco.Cancelacion -> R.string.derecho_cancelacion
    DerechoArco.Oposicion -> R.string.derecho_oposicion
}

@StringRes
private fun descripcion(derecho: DerechoArco) = when (derecho) {
    DerechoArco.Acceso -> R.string.derecho_acceso_desc
    DerechoArco.Rectificacion -> R.string.derecho_rectificacion_desc
    DerechoArco.Cancelacion -> R.string.derecho_cancelacion_desc
    DerechoArco.Oposicion -> R.string.derecho_oposicion_desc
}

/** Crear una solicitud ARCO y consultar el estado y los plazos de las ya enviadas (UI_UX_SPEC: "crear / estado"). */
@Composable
fun ArcoScreen(viewModel: ArcoViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pantalla_arco),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            Text(
                text = stringResource(R.string.arco_nueva),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.arco_elige), style = MaterialTheme.typography.bodyMedium)
            DerechoArco.entries.forEach { derecho ->
                val elegido = estado.derecho == derecho
                // Toda la fila es el botón de opción (objetivo táctil >= 48 dp y una sola parada en TalkBack).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = elegido, enabled = !estado.enviando, role = Role.RadioButton, onClick = { viewModel.seleccionar(derecho) }),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = elegido, onClick = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(nombre(derecho)), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(descripcion(derecho)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            estado.errorEnvio?.let {
                Text(text = stringResource(R.string.arco_error_envio), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = viewModel::enviar,
                enabled = estado.derecho != null && !estado.enviando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (estado.enviando) R.string.login_cargando else R.string.arco_enviar))
            }
            estado.creada?.let { Confirmacion(it) }

            Text(
                text = stringResource(R.string.arco_mis_solicitudes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            ContenidoCarga(
                estado = estado.solicitudes,
                alReintentar = viewModel::cargar,
                estaVacio = { it.isEmpty() },
                mensajeVacio = R.string.arco_vacio,
            ) { solicitudes ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    solicitudes.forEach { Solicitud(it, actualizando = estado.actualizando == it.folio, alActualizar = viewModel::actualizar) }
                }
            }
        }
    }
}

/** Respuesta 201: el folio y las fechas límite. */
@Composable
private fun Confirmacion(solicitud: SolicitudArco) {
    Tarjeta {
        Text(text = stringResource(R.string.arco_enviada), style = MaterialTheme.typography.titleMedium)
        Campo(stringResource(R.string.arco_folio), solicitud.folio)
        Campo(stringResource(R.string.arco_plazo_respuesta), formatearFecha(solicitud.respuestaLimite))
        Campo(stringResource(R.string.arco_plazo_ejecucion), formatearFecha(solicitud.ejecucionLimite))
    }
}

@Composable
private fun Solicitud(solicitud: SolicitudArco, actualizando: Boolean, alActualizar: (String) -> Unit) {
    Tarjeta {
        Campo(stringResource(R.string.arco_folio), solicitud.folio)
        Campo(stringResource(R.string.arco_derecho), stringResource(nombre(solicitud.derecho)))
        Campo(stringResource(R.string.arco_estado), solicitud.estado)
        Campo(stringResource(R.string.arco_recibida), formatearFecha(solicitud.fechaRecepcion))
        Campo(stringResource(R.string.arco_plazo_respuesta), formatearFecha(solicitud.respuestaLimite))
        Campo(stringResource(R.string.arco_plazo_ejecucion), formatearFecha(solicitud.ejecucionLimite))
        BotonSecundario(onClick = { alActualizar(solicitud.folio) }, enabled = !actualizando) {
            Text(stringResource(if (actualizando) R.string.login_cargando else R.string.arco_actualizar))
        }
    }
}
