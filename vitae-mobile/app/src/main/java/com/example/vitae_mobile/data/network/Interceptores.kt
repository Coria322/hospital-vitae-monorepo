package com.example.vitae_mobile.data.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * PROV-SEC-011 / RNF-006: con el dispositivo bloqueado (root, Android no soportado) no sale ninguna petición; el
 * [IOException] llega a la UI como error de red. Un 426 avisa que esta versión de la app ya no es compatible.
 * Va primero en la cadena: nada, ni el token, sale de un dispositivo bloqueado.
 */
class DispositivoInterceptor(private val bloqueado: () -> Boolean, private val alIncompatible: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (bloqueado()) throw IOException("Dispositivo bloqueado")
        val respuesta = chain.proceed(chain.request())
        if (respuesta.code == 426) alIncompatible()
        return respuesta
    }
}

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
