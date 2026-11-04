package com.example.vitae_mobile.data.auth

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json

/**
 * Tokens cifrados con AES-GCM; la clave vive en Android Keystore y no es exportable (PROV-SEC-002).
 * ponytail: Keystore directo en vez de EncryptedSharedPreferences (security-crypto está marcado como obsoleto).
 */
class KeystoreTokenStore(contexto: Context) : TokenStore {
    private val prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun guardar(sesion: Sesion) {
        prefs.edit().putString(CLAVE_SESION, cifrar(Json.encodeToString(sesion))).apply()
    }

    override fun leer(): Sesion? {
        val guardado = prefs.getString(CLAVE_SESION, null) ?: return null
        val texto = descifrar(guardado) ?: return null
        return runCatching { Json.decodeFromString<Sesion>(texto) }.getOrNull()
    }

    // commit() y no apply(): al cerrar sesión el token debe estar borrado antes de que el proceso pueda morir.
    @SuppressLint("ApplySharedPref")
    override fun borrar() {
        prefs.edit().remove(CLAVE_SESION).commit()
    }

    override fun idInstalacion(): String =
        prefs.getString(CLAVE_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(CLAVE_ID, it).apply() }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, clave()) }
        val resultado = cifrador.iv + cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(resultado, Base64.NO_WRAP)
    }

    // Si la clave ya no sirve (p. ej. se restauró un respaldo en otro dispositivo) no hay sesión.
    private fun descifrar(guardado: String): String? = runCatching {
        val datos = Base64.decode(guardado, Base64.NO_WRAP)
        val cifrador = Cipher.getInstance(TRANSFORMACION)
            .apply { init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, datos, 0, TAMANO_IV)) }
        String(cifrador.doFinal(datos, TAMANO_IV, datos.size - TAMANO_IV), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val ARCHIVO = "sesion"
        const val CLAVE_SESION = "sesion"
        const val CLAVE_ID = "id_instalacion"
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "vitae_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAMANO_IV = 12
    }
}
