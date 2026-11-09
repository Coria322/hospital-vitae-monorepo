package com.example.vitae_mobile.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    // Apéndice B del RFC 7636: PROV-SEC-002 exige code_challenge_method=S256.
    @Test
    fun challengeCoincideConElVectorDelRfc7636() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeS256("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun verifierTieneLongitudYAlfabetoValidos() {
        repeat(50) {
            val verifier = Pkce.generarVerifier()
            assertTrue(verifier.length in 43..128)
            assertTrue(verifier.all { it.isLetterOrDigit() || it in "-._~" })
        }
    }

    @Test
    fun cadaVerifierEsDistinto() {
        assertNotEquals(Pkce.generarVerifier(), Pkce.generarVerifier())
    }
}
