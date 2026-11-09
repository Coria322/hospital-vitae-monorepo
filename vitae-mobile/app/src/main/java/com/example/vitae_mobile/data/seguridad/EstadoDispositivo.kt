package com.example.vitae_mobile.data.seguridad

/** RNF-006: la app se instala y corre en Android 10 (API 29) o superior. */
const val SDK_MINIMO = 29

/** Resultado de comprobar el dispositivo al arrancar y cada vez que la app vuelve a primer plano. */
sealed interface EstadoDispositivo {
    data object Apto : EstadoDispositivo

    /** Android anterior a [SDK_MINIMO], o el servidor respondió 426 (versión de la app incompatible). */
    data object Incompatible : EstadoDispositivo

    /** PROV-SEC-011: root o Magisk. La app se bloquea y no envía ninguna petición. */
    data object RootDetectado : EstadoDispositivo
}

/** La versión de Android se mira antes que el root: en un Android no soportado no hay nada más que comprobar. */
fun verificarDispositivo(entorno: EntornoDispositivo, detector: DetectorRoot): EstadoDispositivo = when {
    entorno.versionSdk() < SDK_MINIMO -> EstadoDispositivo.Incompatible
    detector.esRoot() -> EstadoDispositivo.RootDetectado
    else -> EstadoDispositivo.Apto
}
