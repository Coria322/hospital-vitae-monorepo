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

/**
 * El portal móvil (`/api/v1/mobile/…`) exige `X-Client-Version` con el nivel de API de Android (RNF-006): sin la cabecera
 * responde 400, y con un nivel menor que 29 responde 426. Se manda en todas las peticiones; el servidor la ignora fuera
 * de `/mobile`.
 */
class VersionInterceptor(private val nivelApi: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("X-Client-Version", "API-$nivelApi").build())
}

/** Agrega `Authorization: Bearer` cuando hay token y la cabecera `X-Client-Version` para compatibilidad RNF-006. */
class AuthInterceptor(private val token: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val builder = peticion.newBuilder()
        if (peticion.header("X-Client-Version") == null) {
            builder.header("X-Client-Version", "Android 14 (API 34)")
        }
        val valor = token()
        if (valor != null && peticion.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $valor")
        }
        return chain.proceed(builder.build())
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
