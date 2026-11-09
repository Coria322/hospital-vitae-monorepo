package com.example.vitae_mobile.data.seguridad

/** El release usa solo el entorno real: no hay forma de forzar una detección de root. */
fun crearDetectorRoot(entorno: EntornoDispositivo): DetectorRoot = DetectorRoot(entorno)
