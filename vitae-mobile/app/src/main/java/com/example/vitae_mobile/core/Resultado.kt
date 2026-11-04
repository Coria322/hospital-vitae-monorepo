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

    /** Cualquier otro 4xx o una respuesta ilegible. */
    Desconocido,
}

sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ApiError) : Resultado<Nothing>
}
