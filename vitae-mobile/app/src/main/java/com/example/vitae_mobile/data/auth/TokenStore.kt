package com.example.vitae_mobile.data.auth

interface TokenStore {
    fun guardar(sesion: Sesion)
    fun leer(): Sesion?

    /** Borra la sesión; el id de instalación se conserva para que el OTP no se pida en cada login. */
    fun borrar()

    /** UUID aleatorio de esta instalación; identifica al dispositivo ante el servidor (supuesto, ver PLAN.md). */
    fun idInstalacion(): String
}
