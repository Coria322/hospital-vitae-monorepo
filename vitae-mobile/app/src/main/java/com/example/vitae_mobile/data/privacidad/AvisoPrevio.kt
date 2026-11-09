package com.example.vitae_mobile.data.privacidad

import android.content.Context

/**
 * Aviso de privacidad simplificado que se muestra ANTES del login (PROV-RN-008, escenario 1: "ve el aviso antes de
 * cualquier formulario"). Antes de autenticarse no se puede pedir el aviso al servidor, así que el texto va en la app
 * (`aviso_previo_texto`); la aceptación formal de finalidades y su registro con versión siguen siendo la pantalla
 * posterior al login, que sí tiene sesión.
 *
 * El texto y [VERSION] cambian juntos: un cambio sustancial del aviso lo vuelve a mostrar (caso borde de PROV-RN-008).
 * El texto actual es un marcador a validar con el área legal; la URL también.
 */
object AvisoPrevio {
    const val VERSION = "1.0"
    const val URL_INTEGRAL = "https://example.com/aviso-de-privacidad-integral"
}

/** Última versión del aviso previo que vio este dispositivo. No es un dato sensible, por eso va en preferencias simples. */
class AvisoPrevioLocal(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("aviso_previo", Context.MODE_PRIVATE)

    fun visto(): Boolean = preferencias.getString(CLAVE, null) == AvisoPrevio.VERSION

    fun marcarVisto() = preferencias.edit().putString(CLAVE, AvisoPrevio.VERSION).apply()

    private companion object {
        const val CLAVE = "version_vista"
    }
}
