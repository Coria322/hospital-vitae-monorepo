package com.example.vitae_mobile.ui.citas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.formatearHora
import com.example.vitae_mobile.ui.seguridad.PantallaSegura
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Citas del paciente (PROV-RF-002): consulta y agendamiento directo desde el portal. */
@Composable
fun CitasScreen(
    viewModel: CargaViewModel<List<Cita>>,
    alAgendar: (suspend (fechaHora: String, especialidad: String, motivo: String) -> Resultado<Cita>)? = null,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var especialidad by remember { mutableStateOf("Medicina General") }
    var motivo by remember { mutableStateOf("Consulta de seguimiento") }
    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_citas))

                if (alAgendar != null) {
                    Button(
                        onClick = { mostrarDialogo = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Agendar Nueva Cita Médica")
                    }
                }

                ContenidoCarga(
                    estado = estado,
                    alReintentar = viewModel::cargar,
                    estaVacio = { it.isEmpty() },
                    mensajeVacio = R.string.citas_vacio,
                ) { citas ->
                    citas.forEach { FilaCita(it) }
                }
            }
        }
    }

    if (mostrarDialogo && alAgendar != null) {
        AlertDialog(
            onDismissRequest = { if (!guardando) mostrarDialogo = false },
            title = { Text("Agendar Cita Médica") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = especialidad,
                        onValueChange = { especialidad = it },
                        label = { Text("Especialidad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo de consulta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "La cita se registrará en el sistema hospitalario y se reflejará en la sala del médico y recepción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    mensajeError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            guardando = true
                            mensajeError = null
                            val manana = LocalDate.now().plusDays(1).toString() + " 10:00:00"
                            val res = alAgendar(manana, especialidad, motivo)
                            guardando = false
                            if (res is Resultado.Exito) {
                                mostrarDialogo = false
                                viewModel.cargar()
                            } else {
                                mensajeError = "No se pudo agendar la cita. Intente nuevamente."
                            }
                        }
                    },
                    enabled = !guardando && especialidad.isNotBlank(),
                ) {
                    Text(if (guardando) "Agendando…" else "Confirmar Cita")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogo = false },
                    enabled = !guardando,
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun FilaCita(cita: Cita) {
    Tarjeta {
        Campo(stringResource(R.string.cita_fecha), formatearFecha(cita.fechaHora))
        Campo(stringResource(R.string.cita_hora), formatearHora(cita.fechaHora))
        if (!cita.especialidad.isNullOrBlank()) {
            Campo("Especialidad", cita.especialidad)
        }
        if (!cita.consultorio.isNullOrBlank()) {
            Campo("Consultorio", cita.consultorio)
        }
        Campo(stringResource(R.string.cita_estado), cita.estado)
    }
}
