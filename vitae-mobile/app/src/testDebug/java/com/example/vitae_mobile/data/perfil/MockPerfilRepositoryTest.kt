package com.example.vitae_mobile.data.perfil

import com.example.vitae_mobile.core.Resultado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPerfilRepositoryTest {
    @Test
    fun elPacienteDeDemoEsUnPatientFhirValido() = runTest {
        val perfil = (MockPerfilRepository().perfil() as Resultado.Exito).valor

        assertEquals("Ana Pérez Demo", perfil.nombre)
        assertEquals(18, perfil.curp?.length)
        assertEquals("1990-05-17", perfil.fechaNacimiento)
        assertEquals(true, perfil.seguroActivo)
    }

    @Test
    fun losDatosDeDemoSonClaramenteSinteticos() {
        assertTrue(PACIENTE_DEMO_JSON.contains("DEMO"))
        assertFalse(PACIENTE_DEMO_JSON.contains("id_paciente"))
    }
}
