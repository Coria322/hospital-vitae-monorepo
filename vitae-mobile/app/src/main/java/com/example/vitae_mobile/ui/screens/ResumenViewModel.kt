package com.example.vitae_mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import com.example.vitae_mobile.ui.components.Carga
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cada bloque del resumen carga y falla por separado: un error en uno no oculta los demás. */
data class ResumenUiState(
    val proximaCita: Carga<Cita?> = Carga.Cargando,
    val recetasActivas: Carga<Int> = Carga.Cargando,
    val ultimoResultado: Carga<ResultadoEstudio?> = Carga.Cargando,
) {
    // Un dato que el servidor no ofrece (NoDisponible) se muestra como "No disponible"; no es un fallo que se pueda reintentar.
    val hayError get() = listOf(proximaCita, recetasActivas, ultimoResultado).any { it is Carga.Error && it.error != ApiError.NoDisponible }
}

/** Resumen de Inicio: próxima cita, recetas activas y último resultado, solo datos propios (TASK-M-008). */
class ResumenViewModel(
    private val citas: CitasRepository,
    private val recetas: RecetasRepository,
    private val resultados: ResultadosRepository,
    private val reloj: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val _estado = MutableStateFlow(ResumenUiState())
    val estado: StateFlow<ResumenUiState> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = ResumenUiState()
        viewModelScope.launch {
            launch {
                val carga = citas.citas().aCarga { lista ->
                    val ahora = reloj.instant()
                    lista.filter { it.fechaHora >= ahora }.minByOrNull { it.fechaHora }
                }
                _estado.update { it.copy(proximaCita = carga) }
            }
            launch {
                val carga = recetas.recetas().aCarga { lista -> lista.count { it.estado == EstadoReceta.Activa } }
                _estado.update { it.copy(recetasActivas = carga) }
            }
            launch {
                val carga = resultados.resultados().aCarga { lista -> lista.maxByOrNull { it.fechaHora } }
                _estado.update { it.copy(ultimoResultado = carga) }
            }
        }
    }
}

private fun <T, R> Resultado<T>.aCarga(transformar: (T) -> R): Carga<R> = when (this) {
    is Resultado.Exito -> Carga.Listo(transformar(valor))
    is Resultado.Fallo -> Carga.Error(error)
}
