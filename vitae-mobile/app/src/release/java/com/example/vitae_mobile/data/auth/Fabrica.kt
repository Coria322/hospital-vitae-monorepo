package com.example.vitae_mobile.data.auth

/**
 * Release no lleva datos simulados (traen credenciales de demostración) y habla siempre con el backend real, así que
 * `AppContainer` no llama a esta función. Si algún día se llamara, falla de forma explícita.
 */
@Suppress("UNUSED_PARAMETER")
fun crearAuthRepository(idInstalacion: () -> String): AuthRepository =
    error("Sin datos simulados en release de autenticación: release usa siempre el backend real (BACKEND_REAL)")
