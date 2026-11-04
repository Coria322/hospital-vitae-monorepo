package com.example.vitae_mobile.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Necesita Android Keystore real: se ejecuta en dispositivo o emulador (TASK-M-004).
@RunWith(AndroidJUnit4::class)
class KeystoreTokenStoreTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val sesion = Sesion("access-secreto-123", "refresh-secreto-456", 900)

    @Before
    fun limpiar() {
        contexto.deleteSharedPreferences("sesion")
    }

    @Test
    fun guardaYLeeLaSesionEntreInstancias() {
        KeystoreTokenStore(contexto).guardar(sesion)
        assertEquals(sesion, KeystoreTokenStore(contexto).leer())
    }

    // PROV-SEC-002, escenario 1: no existen tokens en texto plano.
    @Test
    fun noDejaTokensEnClaro() {
        KeystoreTokenStore(contexto).guardar(sesion)

        val archivo = File(contexto.dataDir, "shared_prefs/sesion.xml").readText()

        assertFalse(archivo.contains("access-secreto-123"))
        assertFalse(archivo.contains("refresh-secreto-456"))
    }

    @Test
    fun borrarEliminaLaSesionPeroConservaElIdDeInstalacion() {
        val almacen = KeystoreTokenStore(contexto)
        val id = almacen.idInstalacion()
        almacen.guardar(sesion)

        almacen.borrar()

        assertNull(almacen.leer())
        assertEquals(id, KeystoreTokenStore(contexto).idInstalacion())
        assertNotNull(id)
    }
}
