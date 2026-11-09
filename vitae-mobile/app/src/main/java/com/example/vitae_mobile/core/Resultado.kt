package com.example.vitae_mobile.core

/** Errores de la API que la UI sabe mostrar (ARCHITECTURE.md, manejo de errores). */
enum class ApiError {
    /** 401: sesión inválida; ir a Login y limpiar el estado. */
    SesionInvalida,

    /** 403: sin permisos; mensaje sin cerrar sesión. */
    SinPermisos,

    /** 423: cuenta bloqueada; contactar a administración. */
    CuentaBloqueada,

    /** 426: versión de la app incompatible con la API. */
    AppIncompatible,

    /** Sin conexión o tiempo agotado; error con reintento. */
    Red,

    /** 5xx; error con reintento. */
    Servidor,

    /** El servidor aún no ofrece ese recurso (p. ej. citas: no hay endpoint de agenda). */
    NoDisponible,

    /** Cualquier otro 4xx o una respuesta ilegible. */
    Desconocido,
}

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ApiError) : Resultado<Nothing>
}

/** Transforma el valor de un éxito; un fallo pasa tal cual. */
inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(transformar(valor))
    is Resultado.Fallo -> this
}

/** Encadena otra operación que también puede fallar. */
inline fun <T, R> Resultado<T>.luego(siguiente: (T) -> Resultado<R>): Resultado<R> = when (this) {
    is Resultado.Exito -> siguiente(valor)
    is Resultado.Fallo -> this
}
