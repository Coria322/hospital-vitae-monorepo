package com.example.vitae_mobile.data.citas

import com.example.vitae_mobile.core.Resultado
import java.time.Instant

/** Cita propia (PROV-RF-002). La hora llega en UTC; los valores de [estado] no están definidos en el ERS (AMB-018). */
data class Cita(val id: String, val fechaHora: Instant, val estado: String)

/** La identidad sale del token: no hay parámetro de paciente (RF-002.1). */
interface CitasRepository {
    suspend fun citas(): Resultado<List<Cita>>
}
