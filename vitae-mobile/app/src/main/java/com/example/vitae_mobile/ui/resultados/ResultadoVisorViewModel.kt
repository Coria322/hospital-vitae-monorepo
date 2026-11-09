package com.example.vitae_mobile.ui.resultados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.resultados.ArchivoDescargado
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ArchivoUiState {
    /** [progreso] va de 0f a 1f. */
    data class Descargando(val progreso: Float) : ArchivoUiState
    data class Listo(val archivo: ArchivoDescargado) : ArchivoUiState
    data class Error(val error: ApiError) : ArchivoUiState
}

/** La descarga corre fuera del hilo principal (RF-010) y el archivo se borra al salir del visor. */
class ResultadoVisorViewModel(
    private val repositorio: ResultadosRepository,
    private val id: String,
) : ViewModel() {
    private val _estado = MutableStateFlow<ArchivoUiState>(ArchivoUiState.Descargando(0f))
    val estado: StateFlow<ArchivoUiState> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = ArchivoUiState.Descargando(0f)
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.archivo(id) { _estado.value = ArchivoUiState.Descargando(it) }) {
                is Resultado.Exito -> ArchivoUiState.Listo(r.valor)
                is Resultado.Fallo -> ArchivoUiState.Error(r.error)
            }
        }
    }

    // El resultado es información clínica: no debe quedar en disco al cerrar el visor (los restos de procesos
    // muertos los limpia el contenedor al arrancar y al cerrar sesión).
    override fun onCleared() {
        (_estado.value as? ArchivoUiState.Listo)?.archivo?.archivo?.delete()
    }
}
