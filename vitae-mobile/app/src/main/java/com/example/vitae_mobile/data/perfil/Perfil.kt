package com.example.vitae_mobile.data.perfil

import com.example.vitae_mobile.core.Resultado

/** Datos del paciente que muestra la app. Todo es opcional: el servidor puede no enviarlo. Solo en memoria (sin PII en disco). */
data class Perfil(
    val nombre: String?,
    val curp: String?,
    /** Fecha FHIR tal como llega (`YYYY`, `YYYY-MM` o `YYYY-MM-DD`). */
    val fechaNacimiento: String?,
    val seguroActivo: Boolean?,
)

/** La identidad sale del token: no hay parámetro de paciente (RF-002.1). */
interface PerfilRepository {
    suspend fun perfil(): Resultado<Perfil>
}

/** `1990-05-17` → `17/05/1990`; una fecha parcial se muestra tal cual. */
fun formatearFechaFhir(fecha: String?): String? {
    val completa = Regex("""(\d{4})-(\d{2})-(\d{2})""").matchEntire(fecha ?: return null) ?: return fecha
    val (anio, mes, dia) = completa.destructured
    return "$dia/$mes/$anio"
}
