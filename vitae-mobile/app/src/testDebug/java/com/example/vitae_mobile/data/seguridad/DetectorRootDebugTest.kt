package com.example.vitae_mobile.data.seguridad

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El detector de debug (el que usa la app en desarrollo): ignora `test-keys` y se puede simular root. */
class DetectorRootDebugTest {
    @After
    fun restaurar() {
        SimulacionRoot.activo = false
    }

    // Emuladores de AOSP o sin Play Store: son `test-keys` pero no son root.
    @Test
    fun enDebugElTestKeysNoCuentaComoRoot() {
        assertFalse(crearDetectorRoot(EntornoFalso(etiquetas = "test-keys")).esRoot())
    }

    // Lo demás se sigue detectando: ignorar test-keys no vuelve ciego al detector.
    @Test
    fun enDebugSigueDetectandoSuMagiskYMontajes() {
        assertTrue(crearDetectorRoot(EntornoFalso(archivos = setOf("/sbin/su"))).esRoot())
        assertTrue(crearDetectorRoot(EntornoFalso(paquetes = setOf("com.topjohnwu.magisk"))).esRoot())
        assertTrue(crearDetectorRoot(EntornoFalso(montajes = listOf("/dev/block/dm-3 /system ext4 rw,seclabel 0 0"))).esRoot())
    }

    @Test
    fun laSimulacionDeRootSoloActivaCuandoSePide() {
        val detector = crearDetectorRoot(EntornoFalso())
        assertFalse(detector.esRoot())
        SimulacionRoot.activo = true
        assertTrue(detector.esRoot())
    }
}
