package com.example.vitae_mobile.ui.navigation

import androidx.annotation.StringRes
import com.example.vitae_mobile.R

/**
 * Pantallas del portal (UI_UX_SPEC.md, PROV-UI-003). El detalle de receta y el visor de resultados
 * se agregan con su etapa; Splash con la verificación de compatibilidad y root. `Aviso` es la puerta tras iniciar sesión y
 * `Privacidad` la pantalla de preferencias desde Inicio.
 */
enum class Ruta(val ruta: String, @param:StringRes val titulo: Int) {
    Login("login", R.string.pantalla_login),
    Otp("otp", R.string.pantalla_otp),
    Aviso("aviso-privacidad", R.string.pantalla_aviso),
    Inicio("inicio", R.string.pantalla_inicio),
    Citas("citas", R.string.pantalla_citas),
    Recetas("recetas", R.string.pantalla_recetas),
    Resultados("resultados", R.string.pantalla_resultados),
    Perfil("perfil", R.string.pantalla_perfil),
    Arco("solicitud-arco", R.string.pantalla_arco),
    Privacidad("privacidad", R.string.pantalla_privacidad),
    SesionExpirada("sesion-expirada", R.string.pantalla_sesion_expirada),
    Incompatible("incompatible", R.string.pantalla_incompatible),
    RootBloqueado("root-bloqueado", R.string.pantalla_root),
}
