package com.example.vitae_mobile.data.auth

/** Debug usa el servidor simulado mientras no exista los endpoints de `/api/v1/mobile/auth`. */
fun crearAuthRepository(idInstalacion: () -> String): AuthRepository = MockAuthRepository(idInstalacion)
