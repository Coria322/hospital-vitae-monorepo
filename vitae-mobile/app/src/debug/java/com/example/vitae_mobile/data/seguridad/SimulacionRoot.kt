package com.example.vitae_mobile.data.seguridad

/** Solo debug: permite a las pruebas instrumentadas simular un dispositivo con root sin rootear el teléfono. */
object SimulacionRoot {
    @Volatile
    var activo = false
}

private class EntornoDeDesarrollo(private val real: EntornoDispositivo) : EntornoDispositivo by real {
    // Una ruta de su inventada que el detector reconoce.
    override fun existeArchivo(ruta: String) = (SimulacionRoot.activo && ruta == DetectorRoot.RUTAS_SU.first()) || real.existeArchivo(ruta)

    // Los emuladores sin Play Store traen `test-keys` y no son root: en debug esa señal se ignora para poder desarrollar y
    // probar en ellos. El release la mantiene (ver release/.../Fabrica.kt).
    override fun etiquetasBuild() = "release-keys"
}

fun crearDetectorRoot(entorno: EntornoDispositivo): DetectorRoot = DetectorRoot(EntornoDeDesarrollo(entorno))
