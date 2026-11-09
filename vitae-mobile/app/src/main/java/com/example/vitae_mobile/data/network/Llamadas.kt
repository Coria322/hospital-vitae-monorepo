package com.example.vitae_mobile.data.network

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Ejecuta una llamada de Retrofit y la convierte en [Resultado]; la UI nunca ve excepciones de red. */
suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Resultado.Fallo(errorHttp(e.code()))
} catch (e: IOException) {
    Resultado.Fallo(ApiError.Red)
} catch (e: SerializationException) {
    Resultado.Fallo(ApiError.Desconocido)
}

internal fun errorHttp(codigo: Int): ApiError = when {
    codigo == 401 -> ApiError.SesionInvalida
    codigo == 403 -> ApiError.SinPermisos
    codigo == 423 -> ApiError.CuentaBloqueada
    codigo == 426 -> ApiError.AppIncompatible
    codigo >= 500 -> ApiError.Servidor
    else -> ApiError.Desconocido
}

/** Como [llamar], pero devuelve el `data` del envoltorio; un `data` ausente es una respuesta ilegible. */
suspend fun <T> llamarDatos(bloque: suspend () -> ApiEnvelope<T>): Resultado<T> = when (val r = llamar(bloque)) {
    is Resultado.Fallo -> r
    is Resultado.Exito -> r.valor.data?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.Desconocido)
}
