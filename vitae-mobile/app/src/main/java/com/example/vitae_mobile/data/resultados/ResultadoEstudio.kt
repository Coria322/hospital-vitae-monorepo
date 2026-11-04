package com.example.vitae_mobile.data.resultados

import com.example.vitae_mobile.core.Resultado
import java.io.File
import java.time.Instant

/** Resultado de laboratorio o imagen (RF-010). */
data class ResultadoEstudio(
    val id: String,
    val tipoEstudio: String,
    val fechaHora: Instant,
    val mime: String,
) {
    val esPdf get() = mime == "application/pdf"
    val esImagen get() = mime.startsWith("image/")
}

/** Archivo ya descargado en la caché interna de la app, nunca en almacenamiento compartido. */
data class ArchivoDescargado(val archivo: File, val mime: String)

interface ResultadosRepository {
    suspend fun resultados(): Resultado<List<ResultadoEstudio>>

    /**
     * Transmite el archivo a la caché interna. [progreso] va de 0f a 1f y se invoca fuera del hilo principal
     * (RF-010: un archivo grande muestra avance y no bloquea la interfaz).
     */
    suspend fun archivo(id: String, progreso: (Float) -> Unit): Resultado<ArchivoDescargado>
}
