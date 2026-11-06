package com.example.vitae_mobile.data.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param baseUrl termina en `/`, p. ej. `https://host/api/v1/`.
     * @param pines pines `sha256/...` del certificado del host (RNF-003); vacío = sin pinning (solo debug).
     */
    fun crear(
        baseUrl: String,
        token: () -> String?,
        alSesionInvalida: () -> Unit,
        pines: List<String> = emptyList(),
        bloqueado: () -> Boolean = { false },
        alIncompatible: () -> Unit = {},
    ): Retrofit {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(DispositivoInterceptor(bloqueado, alIncompatible))
            .addInterceptor(AuthInterceptor(token))
            .addInterceptor(SesionInterceptor(alSesionInvalida))
            .apply {
                if (pines.isNotEmpty()) {
                    certificatePinner(CertificatePinner.Builder().add(baseUrl.toHttpUrl().host, *pines.toTypedArray()).build())
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
