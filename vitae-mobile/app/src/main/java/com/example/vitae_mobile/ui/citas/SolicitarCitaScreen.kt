package com.example.vitae_mobile.ui.citas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.formatearHora
import com.example.vitae_mobile.ui.seguridad.PantallaSegura
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("HH:mm")

/** PROV-RF-002: el paciente elige día y hora, y opcionalmente especialidad y motivo; la cita queda pendiente de confirmación. */
@Composable
fun SolicitarCitaScreen(viewModel: SolicitarCitaViewModel, alVerCitas: () -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var eligiendoFecha by remember { mutableStateOf(false) }
    var eligiendoHora by remember { mutableStateOf(false) }

    // El motivo es un dato de salud: la pantalla se protege igual que las clínicas.
    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_solicitar_cita))

                val creada = estado.creada
                if (creada != null) {
                    Tarjeta {
                        Text(
                            text = stringResource(R.string.solicitar_enviada),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Campo(stringResource(R.string.cita_fecha), formatearFecha(creada.fechaHora))
                        Campo(stringResource(R.string.cita_hora), formatearHora(creada.fechaHora))
                        Campo(stringResource(R.string.cita_estado), creada.estado)
                        Text(text = stringResource(R.string.solicitar_enviada_desc), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = alVerCitas, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.solicitar_ver_citas))
                    }
                } else {
                    Text(text = stringResource(R.string.solicitar_ayuda), style = MaterialTheme.typography.bodyMedium)

                    BotonSecundario(onClick = { eligiendoFecha = true }, enabled = !estado.enviando) {
                        Text(
                            estado.fecha?.let { stringResource(R.string.solicitar_fecha_elegida, formatearFecha(it)) }
                                ?: stringResource(R.string.solicitar_elegir_fecha),
                        )
                    }
                    BotonSecundario(onClick = { eligiendoHora = true }, enabled = !estado.enviando) {
                        Text(
                            estado.hora?.let { stringResource(R.string.solicitar_hora_elegida, HORA.format(it)) }
                                ?: stringResource(R.string.solicitar_elegir_hora),
                        )
                    }
                    OutlinedTextField(
                        value = estado.especialidad,
                        onValueChange = viewModel::cambiarEspecialidad,
                        label = { Text(stringResource(R.string.solicitar_especialidad)) },
                        singleLine = true,
                        enabled = !estado.enviando,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = estado.motivo,
                        onValueChange = viewModel::cambiarMotivo,
                        label = { Text(stringResource(R.string.solicitar_motivo)) },
                        minLines = 3,
                        maxLines = 6,
                        enabled = !estado.enviando,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    estado.error?.let {
                        Text(text = stringResource(mensaje(it)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = viewModel::enviar,
                        enabled = estado.completa && !estado.enviando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(if (estado.enviando) R.string.login_cargando else R.string.solicitar_enviar))
                    }
                }
            }
        }
    }

    if (eligiendoFecha) DialogoFecha(estado.fecha, alElegir = viewModel::elegirFecha, alCerrar = { eligiendoFecha = false })
    if (eligiendoHora) DialogoHora(estado.hora, alElegir = viewModel::elegirHora, alCerrar = { eligiendoHora = false })
}

private fun mensaje(error: ErrorSolicitud): Int = when (error) {
    ErrorSolicitud.FechaPasada -> R.string.solicitar_error_pasada
    is ErrorSolicitud.Servidor -> if (error.error == ApiError.SinPermisos) R.string.solicitar_error_permisos else R.string.solicitar_error_envio
}

/** Solo se pueden elegir días desde hoy (en la zona del teléfono); el selector trabaja en UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFecha(inicial: LocalDate?, alElegir: (LocalDate) -> Unit, alCerrar: () -> Unit) {
    val hoyMs = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val selector = rememberDatePickerState(
        initialSelectedDateMillis = inicial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyMs
        },
    )
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(
                enabled = selector.selectedDateMillis != null,
                onClick = {
                    selector.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    alCerrar()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.dialogo_elegir)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.dialogo_cancelar)) }
        },
    ) { DatePicker(state = selector) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHora(inicial: LocalTime?, alElegir: (LocalTime) -> Unit, alCerrar: () -> Unit) {
    val selector = rememberTimePickerState(initialHour = inicial?.hour ?: 9, initialMinute = inicial?.minute ?: 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(
                onClick = {
                    alElegir(LocalTime.of(selector.hour, selector.minute))
                    alCerrar()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.dialogo_elegir)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.dialogo_cancelar)) }
        },
        text = { TimePicker(state = selector) },
    )
}
