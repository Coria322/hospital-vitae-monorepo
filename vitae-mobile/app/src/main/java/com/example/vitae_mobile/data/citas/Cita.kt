package com.example.vitae_mobile.data.citas

import com.example.vitae_mobile.core.Resultado
import java.time.Instant

/** Cita propia (PROV-RF-002). La hora llega en UTC; los valores de [estado] no están definidos en el ERS (AMB-018). */
data class Cita(
    val id: String,
    val fechaHora: Instant,
    val estado: String,
    val especialidad: String? = null,
    val consultorio: String? = null,
)

/** La identidad sale del token: no hay parámetro de paciente (RF-002.1). */
interface CitasRepository {
    suspend fun citas(): Resultado<List<Cita>>

    /**
     * `POST /mobile/citas` (PROV-RF-002): el paciente pide una cita propia y queda `PENDIENTE` hasta que recepción la confirme.
     */
    suspend fun solicitar(fechaHora: Instant, especialidad: String?, motivo: String?): Resultado<Cita> =
        solicitarCita(fechaHora.toString(), especialidad, motivo)

    suspend fun solicitarCita(fechaHora: String, especialidad: String?, motivo: String?): Resultado<Cita> =
        Resultado.Fallo(com.example.vitae_mobile.core.ApiError.Desconocido)
}
