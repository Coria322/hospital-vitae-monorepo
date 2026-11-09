package com.example.vitae_mobile.ui.paciente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.remoto.PacienteDto
import com.example.vitae_mobile.data.remoto.SesionBackend
import com.example.vitae_mobile.ui.components.Carga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacienteUiState(
    val lista: Carga<List<PacienteDto>> = Carga.Cargando,
    val eligiendo: Boolean = false,
    val error: ApiError? = null,
    val elegido: Boolean = false,
)

/**
 * Con los endpoints actuales entra el personal del hospital, no el paciente: antes de ver datos se elige de qué paciente
 * (`GET /pacientes`). Es el reemplazo de "mis datos", que el backend no ofrece (ver PLAN.md, decisión 16).
 */
class PacienteViewModel(private val backend: SesionBackend) : ViewModel() {
    private val _estado = MutableStateFlow(PacienteUiState())
    val estado: StateFlow<PacienteUiState> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = PacienteUiState()
        viewModelScope.launch {
            val lista = when (val r = backend.pacientes()) {
                is Resultado.Exito -> Carga.Listo(r.valor)
                is Resultado.Fallo -> Carga.Error(r.error)
            }
            _estado.update { it.copy(lista = lista) }
        }
    }

    fun elegir(id: String) {
        if (_estado.value.eligiendo) return
        _estado.update { it.copy(eligiendo = true, error = null) }
        viewModelScope.launch {
            when (val r = backend.elegir(id)) {
                is Resultado.Exito -> _estado.update { it.copy(eligiendo = false, elegido = true) }
                is Resultado.Fallo -> _estado.update { it.copy(eligiendo = false, error = r.error) }
            }
        }
    }
}
