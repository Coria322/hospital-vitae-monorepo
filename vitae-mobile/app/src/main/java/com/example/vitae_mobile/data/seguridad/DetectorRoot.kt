package com.example.vitae_mobile.data.seguridad

/**
 * Detección local de root y Magisk (PROV-SEC-011, SUP-007). Es de mejor esfuerzo: un root bien oculto (Zygisk, Shamiko)
 * no se ve desde la app, y Play Integrity necesita verificación en servidor, que no existe. Las señales son internas:
 * nunca se muestran al usuario (los mensajes de error no llevan datos técnicos).
 *
 * Posibles falsos positivos: firmware personalizado o de desarrollo (`test-keys`) y emuladores. Cuando ocurra hay
 * que documentarlo y reportarlo a Seguridad (caso borde de PROV-SEC-011).
 */
class DetectorRoot(private val entorno: EntornoDispositivo) {
    fun senales(): List<String> = buildList {
        RUTAS_SU.firstOrNull(entorno::existeArchivo)?.let { add("su:$it") }
        RUTAS_MAGISK.firstOrNull(entorno::existeArchivo)?.let { add("magisk:$it") }
        PAQUETES_ROOT.firstOrNull(entorno::paqueteInstalado)?.let { add("paquete:$it") }
        if ("test-keys" in entorno.etiquetasBuild()) add("build:test-keys")
        if (sistemaEscribible()) add("montaje:system-rw")
    }

    fun esRoot(): Boolean = senales().isNotEmpty()

    // /proc/mounts: dispositivo, punto de montaje, sistema de archivos, opciones... `/system` en lectura y escritura
    // solo ocurre con el sistema remontado por un root.
    private fun sistemaEscribible() = entorno.montajes().any { linea ->
        val campos = linea.trim().split(Regex("\\s+"))
        campos.size >= 4 && campos[1] == "/system" && "rw" in campos[3].split(',')
    }

    companion object {
        val RUTAS_SU = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su", "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup", "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
            "/su/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su",
        )
        val RUTAS_MAGISK = listOf("/sbin/.magisk", "/data/adb/magisk", "/data/adb/modules", "/cache/magisk.log")

        /** Gestores de root y Magisk; el manifiesto declara estos mismos paquetes en `<queries>` (hay un test). */
        val PAQUETES_ROOT = listOf(
            "com.topjohnwu.magisk", "eu.chainfire.supersu", "com.koushikdutta.superuser", "com.noshufou.android.su",
            "com.thirdparty.superuser", "me.phh.superuser", "com.kingroot.kinguser", "com.alephzain.framaroot",
        )
    }
}
