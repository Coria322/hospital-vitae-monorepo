package com.example.vitae_mobile.data.network

import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * RNF-003: certificate pinning y TLS 1.2 o superior. Los dos certificados de `src/test/resources/seguridad` son
 * autofirmados y solo públicos (no hay clave privada en el repositorio); sirven de "certificado del servidor" y de
 * "certificado de un atacante".
 */
class PinningTest {
    private val base = "https://api.hospital.vitae.test/api/v1/"
    private val host = "api.hospital.vitae.test"

    private fun certificado(nombre: String): X509Certificate =
        File("src/test/resources/seguridad/$nombre").inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private val servidor = certificado("certificado-a.pem")
    private val atacante = certificado("certificado-b.pem")

    private fun pinner(vararg pines: String) = ApiClient.certificatePinner(base, pines.toList())!!

    @Test
    fun conElPinDelServidorLaConexionSePermite() {
        pinner(CertificatePinner.pin(servidor)).check(host, listOf(servidor))
    }

    @Test
    fun conOtroCertificadoLaConexionSeRechaza() {
        try {
            pinner(CertificatePinner.pin(servidor)).check(host, listOf(atacante))
            fail("Debía rechazar un certificado con otro pin")
        } catch (e: SSLPeerUnverifiedException) {
            assertTrue(e.message.orEmpty().contains("Certificate pinning failure"))
        }
    }

    // Se admite más de un pin (rotación de certificados): basta con que coincida uno.
    @Test
    fun conVariosPinesBastaUnoQueCoincida() {
        pinner(CertificatePinner.pin(atacante), CertificatePinner.pin(servidor)).check(host, listOf(servidor))
    }

    // Los pines valen solo para el host de la API: otro host no queda atado a ellos.
    @Test
    fun losPinesSoloAplicanAlHostDeLaApi() {
        pinner(CertificatePinner.pin(servidor)).check("otro.ejemplo.test", listOf(atacante))
    }

    @Test
    fun sinPinesNoHayPinner() {
        assertNull(ApiClient.certificatePinner(base, emptyList()))
    }

    // El formato de BuildConfig.API_PINES es `sha256/<base64>`; así lo produce OkHttp y así se prueba aquí.
    @Test
    fun losPinesTienenElFormatoDeBuildConfig() {
        assertTrue(CertificatePinner.pin(servidor).matches(Regex("sha256/[A-Za-z0-9+/]{43}=")))
    }

    // El cliente real queda con el pinner puesto cuando hay pines, y sin él cuando no (debug).
    @Test
    fun elClienteLlevaElPinnerSoloSiHayPines() {
        val sinPines = ApiClient.crear(base, { null }, {}).callFactory() as OkHttpClient
        val conPines = ApiClient.crear(base, { null }, {}, pines = listOf(CertificatePinner.pin(servidor))).callFactory() as OkHttpClient
        assertTrue(sinPines.certificatePinner.pins.isEmpty())
        assertEquals(1, conPines.certificatePinner.pins.size)
        assertEquals(host, conPines.certificatePinner.pins.single().pattern)
    }

    // RNF-003: nunca TLS 1.0 ni 1.1; las especificaciones de conexión del cliente son las modernas (1.2 y 1.3) o texto plano.
    @Test
    fun elClienteSoloUsaTls12OSuperior() {
        val cliente = ApiClient.crear(base, { null }, {}).callFactory() as OkHttpClient
        val versiones = cliente.connectionSpecs.flatMap { it.tlsVersions.orEmpty() }.toSet()
        assertTrue(versiones.isNotEmpty())
        assertTrue(versiones.all { it == TlsVersion.TLS_1_2 || it == TlsVersion.TLS_1_3 })
        assertFalse(cliente.connectionSpecs.contains(ConnectionSpec.COMPATIBLE_TLS))
    }
}
