package com.example.vitae_mobile.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE con S256 (RFC 7636, PROV-SEC-002). */
object Pkce {
    private val aleatorio = SecureRandom()

    /** 32 bytes aleatorios → 43 caracteres base64url, el mínimo que permite el RFC. */
    fun generarVerifier(): String {
        val bytes = ByteArray(32)
        aleatorio.nextBytes(bytes)
        return base64Url(bytes)
    }

    fun challengeS256(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
