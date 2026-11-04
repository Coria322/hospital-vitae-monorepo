package com.example.vitae_mobile.data.auth

/**
 * ponytail: el release no puede llevar el mock (trae credenciales de demostración). Falla de forma explícita hasta
 * que exista la implementación remota contra los endpoints de `/api/v1/mobile/auth` (cambio de backend pendiente, ver PLAN.md).
 */
@Suppress("UNUSED_PARAMETER")
fun crearAuthRepository(idInstalacion: () -> String): AuthRepository =
    error("Sin implementación remota de autenticación: pendiente del backend móvil")
