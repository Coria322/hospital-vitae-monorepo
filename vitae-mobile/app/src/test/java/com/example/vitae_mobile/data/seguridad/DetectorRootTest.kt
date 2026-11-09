package com.example.vitae_mobile.data.seguridad

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dispositivo configurable: por omisión es un teléfono oficial, sin root y con Android 14. */
class EntornoFalso(
    var archivos: Set<String> = emptySet(),
    var paquetes: Set<String> = emptySet(),
    var etiquetas: String = "release-keys",
    var montajes: List<String> = listOf("/dev/block/dm-3 /system ext4 ro,seclabel,relatime 0 0"),
    var sdk: Int = 34,
) : EntornoDispositivo {
    override fun existeArchivo(ruta: String) = ruta in archivos
    override fun paqueteInstalado(paquete: String) = paquete in paquetes
    override fun etiquetasBuild() = etiquetas
    override fun montajes() = montajes
    override fun versionSdk() = sdk
}

class DetectorRootTest {
    private fun detector(entorno: EntornoFalso) = DetectorRoot(entorno)

    @Test
    fun telefonoOficialNoEsRoot() {
        val d = detector(EntornoFalso())
        assertFalse(d.esRoot())
        assertTrue(d.senales().isEmpty())
    }

    @Test
    fun cualquierBinarioSuEsRoot() {
        DetectorRoot.RUTAS_SU.forEach { ruta ->
            assertTrue(ruta, detector(EntornoFalso(archivos = setOf(ruta))).esRoot())
        }
    }

    @Test
    fun rastrosDeMagiskSonRoot() {
        DetectorRoot.RUTAS_MAGISK.forEach { ruta ->
            assertTrue(ruta, detector(EntornoFalso(archivos = setOf(ruta))).esRoot())
        }
    }

    @Test
    fun gestoresDeRootInstaladosSonRoot() {
        DetectorRoot.PAQUETES_ROOT.forEach { paquete ->
            assertTrue(paquete, detector(EntornoFalso(paquetes = setOf(paquete))).esRoot())
        }
    }

    @Test
    fun firmwareDeDesarrolloEsRoot() {
        assertTrue(detector(EntornoFalso(etiquetas = "test-keys")).esRoot())
        assertTrue(detector(EntornoFalso(etiquetas = "dev-keys,test-keys")).esRoot())
    }

    @Test
    fun systemEnLecturaYEscrituraEsRootPeroEnSoloLecturaNo() {
        assertTrue(detector(EntornoFalso(montajes = listOf("/dev/block/dm-3 /system ext4 rw,seclabel,relatime 0 0"))).esRoot())
        assertFalse(detector(EntornoFalso(montajes = listOf("/dev/block/dm-3 /system ext4 ro,seclabel,relatime 0 0"))).esRoot())
        // "rw" tiene que ser una opción completa, no parte de otra palabra.
        assertFalse(detector(EntornoFalso(montajes = listOf("/dev/block/dm-3 /system ext4 ro,errors=rwx 0 0"))).esRoot())
        // Otros puntos de montaje en rw (datos, caché) son normales.
        assertFalse(detector(EntornoFalso(montajes = listOf("/dev/block/dm-9 /data ext4 rw,seclabel 0 0"))).esRoot())
    }

    @Test
    fun lasSenalesNombranQueSeEncontro() {
        val d = detector(EntornoFalso(archivos = setOf("/system/xbin/su"), paquetes = setOf("com.topjohnwu.magisk"), etiquetas = "test-keys"))
        assertEquals(listOf("su:/system/xbin/su", "paquete:com.topjohnwu.magisk", "build:test-keys"), d.senales())
    }

    // RNF-006: Android anterior a 10 no es compatible, y se dice antes que lo del root.
    @Test
    fun androidAnteriorADiezEsIncompatibleAunqueTengaRoot() {
        val rooteado = EntornoFalso(sdk = SDK_MINIMO - 1, archivos = setOf("/sbin/su"))
        assertEquals(EstadoDispositivo.Incompatible, verificarDispositivo(rooteado, detector(rooteado)))
    }

    @Test
    fun androidDiezOSuperiorLimpioEsApto() {
        val limpio = EntornoFalso(sdk = SDK_MINIMO)
        assertEquals(EstadoDispositivo.Apto, verificarDispositivo(limpio, detector(limpio)))
    }

    @Test
    fun androidSoportadoConRootSeBloquea() {
        val rooteado = EntornoFalso(archivos = setOf("/sbin/su"))
        assertEquals(EstadoDispositivo.RootDetectado, verificarDispositivo(rooteado, detector(rooteado)))
    }

    // El manifiesto declara los paquetes en <queries>; sin eso, desde Android 11 getPackageInfo no los vería.
    @Test
    fun elManifiestoDeclaraLosMismosPaquetesQueElDetector() {
        val manifiesto = File("src/main/AndroidManifest.xml").readText()
        val declarados = Regex("""<package\s+android:name="([^"]+)"""").findAll(manifiesto).map { it.groupValues[1] }.toSet()
        assertEquals(DetectorRoot.PAQUETES_ROOT.toSet(), declarados)
    }

    // PROV-SEC-011: nada de la app sale en respaldos ni transferencias.
    @Test
    fun noHayRespaldoNiTransferenciaDeDatos() {
        val manifiesto = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifiesto.contains("""android:allowBackup="false""""))
        listOf("src/main/res/xml/data_extraction_rules.xml", "src/main/res/xml/backup_rules.xml").forEach { ruta ->
            val reglas = File(ruta).readText()
            listOf("root", "file", "database", "sharedpref", "external").forEach { dominio ->
                assertTrue("$ruta no excluye $dominio", reglas.contains("""<exclude domain="$dominio" path="." />"""))
            }
            assertFalse("$ruta incluye algo", reglas.contains("<include"))
        }
    }
}
