package com.example.vitae_mobile.ui.privacidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.AvisoPrivacidad
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.privacidad.SolicitudConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import com.example.vitae_mobile.ui.components.Carga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModoAviso {
    /** Tras iniciar sesión, mientras la versión vigente no esté aceptada: Login → OTP → Aviso → Inicio. */
    Aceptacion,

    /** Desde Inicio: ver el aviso y aceptar o revocar las finalidades opcionales. */
    Gestion,
}

data class AvisoUiState(
    val aviso: Carga<AvisoPrivacidad> = Carga.Cargando,
    /** Códigos de las finalidades opcionales marcadas. */
    val seleccion: Set<String> = emptySet(),
    val enviando: Boolean = false,
    val errorEnvio: ApiError? = null,
    /** Aceptación: ya aceptado o aceptado ahora. Gestión: preferencias registradas. La pantalla puede salir. */
    val terminado: Boolean = false,
)

/**
 * PROV-RN-008. En [ModoAviso.Aceptacion] las finalidades opcionales arrancan siempre desmarcadas, aunque se hubieran
 * aceptado en una versión anterior: el consentimiento no se presupone. En [ModoAviso.Gestion] arrancan como las tiene
 * el servidor, y cada cambio crea un registro nuevo (CON-016).
 */
class AvisoViewModel(
    private val repositorio: PrivacidadRepository,
    private val modo: ModoAviso,
) : ViewModel() {
    private val _estado = MutableStateFlow(AvisoUiState())
    val estado: StateFlow<AvisoUiState> = _estado

    // Lo que el servidor tiene registrado; sirve para saber qué cambió y qué enviar.
    private var registrado: Set<String> = emptySet()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = AvisoUiState()
        viewModelScope.launch {
            when (val r = repositorio.aviso()) {
                is Resultado.Fallo -> _estado.update { it.copy(aviso = Carga.Error(r.error)) }
                is Resultado.Exito -> {
                    val aviso = r.valor
                    registrado = aviso.finalidadesSecundarias.filter { it.aceptada }.map { it.codigo }.toSet()
                    _estado.update {
                        it.copy(
                            aviso = Carga.Listo(aviso),
                            seleccion = if (modo == ModoAviso.Gestion) registrado else emptySet(),
                            terminado = modo == ModoAviso.Aceptacion && aviso.aceptada,
                        )
                    }
                }
            }
        }
    }

    fun alternar(codigo: String) = _estado.update {
        it.copy(seleccion = if (codigo in it.seleccion) it.seleccion - codigo else it.seleccion + codigo, errorEnvio = null)
    }

    /** En gestión, el botón solo se habilita si hay algo que cambiar. */
    fun hayCambios(estado: AvisoUiState = _estado.value) = modo == ModoAviso.Aceptacion || estado.seleccion != registrado

    fun confirmar() {
        val actual = _estado.value
        val aviso = (actual.aviso as? Carga.Listo)?.valor ?: return
        if (actual.enviando || actual.terminado) return
        _estado.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            val seleccion = actual.seleccion
            val solicitudes = when (modo) {
                ModoAviso.Aceptacion ->
                    listOf(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, aviso.version, seleccion.sorted(), AccionConsentimiento.ACEPTAR))
                ModoAviso.Gestion -> listOfNotNull(
                    (seleccion - registrado).takeIf { it.isNotEmpty() }
                        ?.let { SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, aviso.version, it.sorted(), AccionConsentimiento.ACEPTAR) },
                    (registrado - seleccion).takeIf { it.isNotEmpty() }
                        ?.let { SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, aviso.version, it.sorted(), AccionConsentimiento.REVOCAR) },
                )
            }
            for (solicitud in solicitudes) {
                when (val r = repositorio.registrar(solicitud)) {
                    is Resultado.Fallo -> {
                        _estado.update { it.copy(enviando = false, errorEnvio = r.error) }
                        return@launch
                    }
                    is Resultado.Exito -> registrado = when (solicitud.accion) {
                        AccionConsentimiento.ACEPTAR -> registrado + solicitud.finalidades
                        AccionConsentimiento.REVOCAR -> registrado - solicitud.finalidades.toSet()
                    }
                }
            }
            _estado.update { it.copy(enviando = false, terminado = true) }
        }
    }
}
