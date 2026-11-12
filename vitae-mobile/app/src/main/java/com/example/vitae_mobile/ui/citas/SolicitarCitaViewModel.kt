package com.example.vitae_mobile.ui.citas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Longitudes máximas que acepta el servidor (`POST /mobile/citas`). */
const val ESPECIALIDAD_MAX = 60
const val MOTIVO_MAX = 500

/** Por qué no salió la solicitud: la fecha no es futura (se detecta antes de llamar) o lo que respondió el servidor. */
sealed interface ErrorSolicitud {
    data object FechaPasada : ErrorSolicitud

    data class Servidor(val error: ApiError) : ErrorSolicitud
}

data class SolicitarCitaUiState(
    val fecha: LocalDate? = null,
    val hora: LocalTime? = null,
    val especialidad: String = "",
    val motivo: String = "",
    val enviando: Boolean = false,
    val error: ErrorSolicitud? = null,
    /** La cita que creó el servidor (201): `PENDIENTE` hasta que recepción la confirme. */
    val creada: Cita? = null,
) {
    val completa get() = fecha != null && hora != null
}

/**
 * PROV-RF-002: el paciente pide una cita propia. La fecha y la hora se eligen en la zona del teléfono y viajan en UTC.
 * Una fecha que ya pasó se rechaza aquí; el servidor también la rechaza (422), pero el aviso es más claro sin ir y volver.
 */
class SolicitarCitaViewModel(
    private val repositorio: CitasRepository,
    private val reloj: () -> Instant = Instant::now,
    private val zona: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {
    private val _estado = MutableStateFlow(SolicitarCitaUiState())
    val estado: StateFlow<SolicitarCitaUiState> = _estado

    fun elegirFecha(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, error = null) }

    fun elegirHora(hora: LocalTime) = _estado.update { it.copy(hora = hora, error = null) }

    fun cambiarEspecialidad(texto: String) = _estado.update { it.copy(especialidad = texto.take(ESPECIALIDAD_MAX), error = null) }

    fun cambiarMotivo(texto: String) = _estado.update { it.copy(motivo = texto.take(MOTIVO_MAX), error = null) }

    fun enviar() {
        val actual = _estado.value
        val fecha = actual.fecha ?: return
        val hora = actual.hora ?: return
        if (actual.enviando || actual.creada != null) return
        val instante = LocalDateTime.of(fecha, hora).atZone(zona()).toInstant()
        if (!instante.isAfter(reloj())) {
            _estado.update { it.copy(error = ErrorSolicitud.FechaPasada) }
            return
        }
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            // Los textos vacíos no se mandan: el servidor pone sus valores por omisión.
            val r = repositorio.solicitar(instante, actual.especialidad.trim().ifEmpty { null }, actual.motivo.trim().ifEmpty { null })
            _estado.update {
                when (r) {
                    is Resultado.Exito -> it.copy(enviando = false, creada = r.valor)
                    is Resultado.Fallo -> it.copy(enviando = false, error = ErrorSolicitud.Servidor(r.error))
                }
            }
        }
    }
}
