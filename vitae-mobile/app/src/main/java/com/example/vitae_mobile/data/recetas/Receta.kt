package com.example.vitae_mobile.data.recetas

import com.example.vitae_mobile.core.Resultado

enum class EstadoReceta { Activa, Dispensada, Cancelada, Desconocido }

/**
 * Receta propia (RF-009). Todo es opcional salvo [id]: el servidor puede no enviarlo y no se inventa nada.
 * [qrVigente] es verdadero solo si la receta sigue activa y su código no se ha consumido (CON-012).
 */
data class Receta(
    val id: String,
    val medicamento: String?,
    val dosis: String?,
    val via: String?,
    val duracion: String?,
    val estado: EstadoReceta,
    val codigoQr: String?,
    val qrVigente: Boolean,
)

interface RecetasRepository {
    suspend fun recetas(): Resultado<List<Receta>>
    suspend fun receta(id: String): Resultado<Receta>
}
