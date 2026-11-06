package com.example.vitae_mobile.data.network

import com.example.vitae_mobile.BuildConfig
import retrofit2.Retrofit

/** Configuración de red por tipo de build (ver `buildTypes` en app/build.gradle.kts). */
object ApiConfig {
    val baseUrl: String = BuildConfig.API_BASE_URL
    val pines: List<String> = BuildConfig.API_PINES.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun crearCliente(
        token: () -> String?,
        alSesionInvalida: () -> Unit,
        bloqueado: () -> Boolean,
        alIncompatible: () -> Unit,
    ): Retrofit {
        // RNF-003: un release sin pines no debe llegar a producción.
        check(BuildConfig.DEBUG || pines.isNotEmpty()) { "Release sin certificate pinning (RNF-003)" }
        return ApiClient.crear(baseUrl, token, alSesionInvalida, pines, bloqueado, alIncompatible)
    }
}
