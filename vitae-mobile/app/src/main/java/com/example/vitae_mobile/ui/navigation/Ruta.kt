package com.example.vitae_mobile.ui.navigation

import androidx.annotation.StringRes
import com.example.vitae_mobile.R

/**
 * Pantallas del portal (UI_UX_SPEC.md, PROV-UI-003). El detalle de receta y el visor de resultados viven en
 * `NavClinico`. `AvisoPrevio` es el aviso simplificado antes del login (primera apertura), `Aviso` la puerta tras
 * iniciar sesión y `Privacidad` la pantalla de preferencias desde Inicio. No hay Splash: la verificación de
 * compatibilidad y root es local e instantánea y decide la ruta inicial (ver `VitaeNavHost`).
 */
enum class Ruta(val ruta: String, @param:StringRes val titulo: Int) {
    AvisoPrevio("aviso-previo", R.string.pantalla_aviso),
    Login("login", R.string.pantalla_login),
    Otp("otp", R.string.pantalla_otp),
    Aviso("aviso-privacidad", R.string.pantalla_aviso),
    Inicio("inicio", R.string.pantalla_inicio),
    Citas("citas", R.string.pantalla_citas),
    SolicitarCita("solicitar-cita", R.string.pantalla_solicitar_cita),
    Recetas("recetas", R.string.pantalla_recetas),
    Resultados("resultados", R.string.pantalla_resultados),
    Perfil("perfil", R.string.pantalla_perfil),
    Arco("solicitud-arco", R.string.pantalla_arco),
    Privacidad("privacidad", R.string.pantalla_privacidad),
    SesionExpirada("sesion-expirada", R.string.pantalla_sesion_expirada),
    Incompatible("incompatible", R.string.pantalla_incompatible),
    RootBloqueado("root-bloqueado", R.string.pantalla_root),
}
