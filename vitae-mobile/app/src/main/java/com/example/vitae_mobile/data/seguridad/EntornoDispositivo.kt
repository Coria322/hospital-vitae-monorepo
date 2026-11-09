package com.example.vitae_mobile.data.seguridad

import android.content.Context
import android.os.Build
import java.io.File

/** Lo que el detector necesita saber del dispositivo; separado para poder probar la lógica sin Android. */
interface EntornoDispositivo {
    fun existeArchivo(ruta: String): Boolean
    fun paqueteInstalado(paquete: String): Boolean

    /** `Build.TAGS`: `release-keys` en un firmware oficial, `test-keys` en uno de desarrollo o modificado. */
    fun etiquetasBuild(): String

    /** Líneas de `/proc/mounts`. */
    fun montajes(): List<String>

    fun versionSdk(): Int
}

class EntornoAndroid(private val contexto: Context) : EntornoDispositivo {
    // Sin permiso de lectura el archivo "no existe" para la app: un root bien oculto no se ve desde aquí (ver PLAN.md).
    override fun existeArchivo(ruta: String) = runCatching { File(ruta).exists() }.getOrDefault(false)

    override fun paqueteInstalado(paquete: String) =
        runCatching { contexto.packageManager.getPackageInfo(paquete, 0) }.isSuccess

    override fun etiquetasBuild(): String = Build.TAGS.orEmpty()

    override fun montajes(): List<String> = runCatching { File("/proc/mounts").readLines() }.getOrDefault(emptyList())

    override fun versionSdk(): Int = Build.VERSION.SDK_INT
}
