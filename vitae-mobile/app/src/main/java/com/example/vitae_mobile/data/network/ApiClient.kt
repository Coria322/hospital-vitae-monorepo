package com.example.vitae_mobile.data.network

import android.os.Build
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

    /** Los pines (`sha256/...`) valen solo para el host de [baseUrl]; sin pines, `null` (solo debug). */
    internal fun certificatePinner(baseUrl: String, pines: List<String>): CertificatePinner? =
        if (pines.isEmpty()) null else CertificatePinner.Builder().add(baseUrl.toHttpUrl().host, *pines.toTypedArray()).build()

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
        nivelApi: Int = Build.VERSION.SDK_INT,
    ): Retrofit {
        val fijarCertificados = certificatePinner(baseUrl, pines)
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            // Render duerme el servidor tras un rato sin tráfico: la primera petición tarda de 25 s a un minuto en despertarlo
            // (medido: 24 s). Con 10 s el primer login fallaba como "sin conexión".
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(DispositivoInterceptor(bloqueado, alIncompatible))
            .addInterceptor(VersionInterceptor(nivelApi))
            .addInterceptor(AuthInterceptor(token))
            .addInterceptor(SesionInterceptor(alSesionInvalida))
            .apply { fijarCertificados?.let { certificatePinner(it) } }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
