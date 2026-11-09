package com.example.vitae_mobile.ui.paciente

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.remoto.PacienteDto
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.mensajeDeError
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

@Composable
fun PacienteScreen(
    viewModel: PacienteViewModel,
    alElegir: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.elegido) {
        if (estado.elegido) alElegir()
    }

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_paciente))
                Text(text = stringResource(R.string.paciente_ayuda), style = MaterialTheme.typography.bodyLarge)
                estado.error?.let {
                    Text(text = stringResource(mensajeDeError(it)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                ContenidoCarga(
                    estado = estado.lista,
                    alReintentar = viewModel::cargar,
                    estaVacio = { it.isEmpty() },
                    mensajeVacio = R.string.paciente_vacio,
                ) { pacientes ->
                    pacientes.forEach { FilaPaciente(it, habilitado = !estado.eligiendo, alElegir = viewModel::elegir) }
                }
                BotonSecundario(onClick = alCerrarSesion) {
                    Text(stringResource(R.string.inicio_cerrar_sesion))
                }
            }
        }
    }
}

@Composable
private fun FilaPaciente(paciente: PacienteDto, habilitado: Boolean, alElegir: (String) -> Unit) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    Tarjeta {
        Campo(stringResource(R.string.perfil_nombre), paciente.nombre ?: noDisponible)
        // Los pacientes de urgencias sin identificar no tienen CURP: se identifican por su folio.
        paciente.curp?.let { Campo(stringResource(R.string.perfil_curp), it) }
            ?: Campo(stringResource(R.string.paciente_folio), paciente.folioUrgencias ?: noDisponible)
        Button(
            onClick = { alElegir(paciente.id) },
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.paciente_seleccionar))
        }
    }
}
