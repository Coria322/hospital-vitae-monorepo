package com.example.vitae_mobile.data.network

import okhttp3.Interceptor
import okhttp3.Response

/** Agrega `Authorization: Bearer` cuando hay token y la petición no lo trae (login y refresh no lo llevan). */
class AuthInterceptor(private val token: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val valor = token()
        if (valor == null || peticion.header("Authorization") != null) return chain.proceed(peticion)
        return chain.proceed(peticion.newBuilder().header("Authorization", "Bearer $valor").build())
    }
}

/**
 * RF-003: cualquier 401 de una petición con sesión invalida la sesión. Un 401 de login (sin Bearer) es
 * "credenciales incorrectas" y no avisa; un 403 nunca cierra la sesión.
 */
class SesionInterceptor(private val alSesionInvalida: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val respuesta = chain.proceed(chain.request())
        if (respuesta.code == 401 && chain.request().header("Authorization") != null) alSesionInvalida()
        return respuesta
    }
}
