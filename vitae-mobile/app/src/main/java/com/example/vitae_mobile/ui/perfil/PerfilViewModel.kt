package com.example.vitae_mobile.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.perfil.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PerfilUiState {
    data object Cargando : PerfilUiState
    data class Listo(val perfil: Perfil) : PerfilUiState
    data class Error(val error: ApiError) : PerfilUiState
}

/**
 * Lo usan Inicio (saludo) y Perfil. Cada pantalla tiene su propia instancia, ligada a su entrada de navegación,
 * así los datos del paciente no sobreviven al cierre de sesión.
 */
class PerfilViewModel(private val repositorio: PerfilRepository) : ViewModel() {
    private val _estado = MutableStateFlow<PerfilUiState>(PerfilUiState.Cargando)
    val estado: StateFlow<PerfilUiState> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = PerfilUiState.Cargando
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.perfil()) {
                is Resultado.Exito -> PerfilUiState.Listo(r.valor)
                is Resultado.Fallo -> PerfilUiState.Error(r.error)
            }
        }
    }
}
