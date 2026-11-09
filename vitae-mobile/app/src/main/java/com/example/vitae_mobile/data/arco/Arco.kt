package com.example.vitae_mobile.data.arco

import com.example.vitae_mobile.core.Resultado
import java.time.LocalDate

/** Derechos ARCO (LFPDPPP). Los valores exactos que usa el servidor no están definidos (supuesto a confirmar). */
enum class DerechoArco { Acceso, Rectificacion, Cancelacion, Oposicion }

/** SOLICITUD_ARCO: el folio y las fechas límite los calcula el servidor (20 y 15 días, AMB-014 deja abierto el calendario). */
data class SolicitudArco(
    val folio: String,
    val derecho: DerechoArco,
    val fechaRecepcion: LocalDate,
    val respuestaLimite: LocalDate,
    val ejecucionLimite: LocalDate,
    val estado: String,
)

/** Escrituras de la lista blanca (SUP-002). La identidad sale del token (RF-002.1). */
interface ArcoRepository {
    suspend fun solicitudes(): Resultado<List<SolicitudArco>>

    /** Responde 201 con el folio y las fechas límite. */
    suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco>

    /** `GET .../solicitudes-arco/{folio}`; un folio ajeno responde 403. */
    suspend fun consultar(folio: String): Resultado<SolicitudArco>
}
