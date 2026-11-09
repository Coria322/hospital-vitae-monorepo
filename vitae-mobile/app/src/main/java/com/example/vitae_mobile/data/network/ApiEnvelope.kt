package com.example.vitae_mobile.data.network

import kotlinx.serialization.Serializable

/** Envoltorio de toda respuesta de la API: `{ status: "success" | "error", message?, data? }`. */
@Serializable
data class ApiEnvelope<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null,
)
