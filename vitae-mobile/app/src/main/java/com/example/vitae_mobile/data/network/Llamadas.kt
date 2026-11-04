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
