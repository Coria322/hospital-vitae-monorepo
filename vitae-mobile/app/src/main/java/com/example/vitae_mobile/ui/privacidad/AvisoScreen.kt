package com.example.vitae_mobile.ui.privacidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.privacidad.AvisoPrivacidad
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Carga
import com.example.vitae_mobile.ui.components.CargandoEsqueleto
import com.example.vitae_mobile.ui.components.EstadoError
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.mensajeDeError

/**
 * Aviso de privacidad simplificado con enlace al integral, y las finalidades opcionales en interruptores
 * independientes y desmarcados por defecto (PROV-RN-008). En [ModoAviso.Aceptacion] hay "Aceptar" y "No acepto"
 * ([alRechazar] cierra la sesión); en [ModoAviso.Gestion] solo se actualizan las preferencias.
 */
@Composable
fun AvisoScreen(
    viewModel: AvisoViewModel,
    modo: ModoAviso,
    alTerminar: () -> Unit,
    alRechazar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) alTerminar()
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (modo == ModoAviso.Aceptacion) R.string.pantalla_aviso else R.string.pantalla_privacidad),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            when (val carga = estado.aviso) {
                Carga.Cargando -> CargandoEsqueleto(filas = 4)
                is Carga.Error -> EstadoError(stringResource(mensajeDeError(carga.error)), alReintentar = viewModel::cargar)
                is Carga.Listo -> ContenidoAviso(carga.valor, estado, viewModel, modo)
            }
            // Sin aceptar el aviso no se puede usar el servicio: la salida es cerrar sesión, incluso si no cargó.
            if (modo == ModoAviso.Aceptacion) {
                BotonSecundario(onClick = alRechazar) {
                    Text(stringResource(R.string.aviso_no_acepto))
                }
            }
        }
    }
}

@Composable
private fun ContenidoAviso(aviso: AvisoPrivacidad, estado: AvisoUiState, viewModel: AvisoViewModel, modo: ModoAviso) {
    val uri = LocalUriHandler.current
    Text(text = aviso.textoSimplificado, style = MaterialTheme.typography.bodyLarge)
    Text(
        text = stringResource(R.string.aviso_version, aviso.version, formatearFecha(aviso.vigenteDesde)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = { uri.openUri(aviso.urlIntegral) }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.aviso_enlace_integral))
    }

    Text(
        text = stringResource(R.string.aviso_finalidades),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(text = stringResource(R.string.aviso_finalidades_ayuda), style = MaterialTheme.typography.bodyMedium)
    aviso.finalidadesSecundarias.forEach { finalidad ->
        val marcada = finalidad.codigo in estado.seleccion
        // Toda la fila es el interruptor (objetivo táctil >= 48 dp y una sola parada en TalkBack).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = marcada, enabled = !estado.enviando, role = Role.Switch, onValueChange = { viewModel.alternar(finalidad.codigo) }),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = finalidad.descripcion, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = marcada, onCheckedChange = null)
        }
    }

    estado.errorEnvio?.let {
        Text(
            text = stringResource(if (it == ApiError.SinPermisos) R.string.error_permisos else R.string.aviso_error_envio),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Button(
        onClick = viewModel::confirmar,
        enabled = !estado.enviando && viewModel.hayCambios(estado),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(
            stringResource(
                when {
                    estado.enviando -> R.string.login_cargando
                    modo == ModoAviso.Aceptacion -> R.string.aviso_aceptar
                    else -> R.string.aviso_actualizar
                }
            )
        )
    }
}
