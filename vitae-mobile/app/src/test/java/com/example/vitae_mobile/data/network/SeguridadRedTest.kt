package com.example.vitae_mobile.data.network

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// TASK-M-006: la configuración de red no admite tráfico en claro (los tests corren con el directorio del módulo app/).
class SeguridadRedTest {
    private val main = File("src/main/res/xml/network_security_config.xml").readText()
    private val debug = File("src/debug/res/xml/network_security_config.xml").readText()
    private val manifiesto = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun releaseNoPermiteTraficoEnClaro() {
        assertTrue(main.contains("""cleartextTrafficPermitted="false""""))
        assertFalse(main.contains("""cleartextTrafficPermitted="true""""))
    }

    @Test
    fun debugSoloPermiteElBackendLocal() {
        val dominios = Regex("<domain[^>]*>([^<]+)</domain>").findAll(debug).map { it.groupValues[1] }.toSet()
        assertTrue(dominios == setOf("10.0.2.2", "localhost"))
        assertTrue(debug.contains("""<base-config cleartextTrafficPermitted="false""""))
    }

    @Test
    fun elManifiestoUsaLaConfiguracionDeRed() {
        assertTrue(manifiesto.contains("""android:networkSecurityConfig="@xml/network_security_config""""))
        assertFalse(manifiesto.contains("usesCleartextTraffic"))
    }
}
