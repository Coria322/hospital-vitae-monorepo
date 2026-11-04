package com.example.vitae_mobile.ui.arco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.ui.components.Carga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArcoUiState(
    val solicitudes: Carga<List<SolicitudArco>> = Carga.Cargando,
    val derecho: DerechoArco? = null,
    val enviando: Boolean = false,
    val errorEnvio: ApiError? = null,
    /** La última solicitud enviada, con su folio y plazos (201). */
    val creada: SolicitudArco? = null,
    /** Folio cuyo estado se está consultando. */
    val actualizando: String? = null,
)

/** PROV-RN-009: enviar una solicitud ARCO y consultar su estado y plazos. */
class ArcoViewModel(private val repositorio: ArcoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ArcoUiState())
    val estado: StateFlow<ArcoUiState> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(solicitudes = Carga.Cargando) }
        viewModelScope.launch { recargarLista() }
    }

    fun seleccionar(derecho: DerechoArco) = _estado.update { it.copy(derecho = derecho, errorEnvio = null) }

    fun enviar() {
        val actual = _estado.value
        val derecho = actual.derecho ?: return
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, errorEnvio = null, creada = null) }
        viewModelScope.launch {
            when (val r = repositorio.crear(derecho)) {
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, errorEnvio = r.error) }
                is Resultado.Exito -> {
                    _estado.update { it.copy(enviando = false, derecho = null, creada = r.valor) }
                    recargarLista()
                }
            }
        }
    }

    /** Consulta el estado de una solicitud y lo refleja en la lista (GET .../solicitudes-arco/{folio}). */
    fun actualizar(folio: String) {
        if (_estado.value.actualizando != null) return
        _estado.update { it.copy(actualizando = folio) }
        viewModelScope.launch {
            val respuesta = repositorio.consultar(folio)
            _estado.update { estado ->
                val lista = (estado.solicitudes as? Carga.Listo)?.valor
                val nueva = (respuesta as? Resultado.Exito)?.valor
                estado.copy(
                    actualizando = null,
                    solicitudes = if (lista != null && nueva != null) Carga.Listo(lista.map { if (it.folio == folio) nueva else it }) else estado.solicitudes,
                )
            }
        }
    }

    private suspend fun recargarLista() {
        val carga = when (val r = repositorio.solicitudes()) {
            is Resultado.Exito -> Carga.Listo(r.valor)
            is Resultado.Fallo -> Carga.Error(r.error)
        }
        _estado.update { it.copy(solicitudes = carga) }
    }
}
