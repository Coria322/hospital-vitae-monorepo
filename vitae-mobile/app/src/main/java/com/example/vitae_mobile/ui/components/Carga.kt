package com.example.vitae_mobile.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Estado de cualquier dato que se pide al servidor: cargando, listo o error (UI_UX_SPEC). */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Listo<T>(val valor: T) : Carga<T>
    data class Error(val error: ApiError) : Carga<Nothing>
}

/** ViewModel de una pantalla que solo carga un dato; [cargar] sirve también para el reintento. */
class CargaViewModel<T>(private val cargador: suspend () -> Resultado<T>) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<T>>(Carga.Cargando)
    val estado: StateFlow<Carga<T>> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch {
            _estado.value = when (val r = cargador()) {
                is Resultado.Exito -> Carga.Listo(r.valor)
                is Resultado.Fallo -> Carga.Error(r.error)
            }
        }
    }
}
