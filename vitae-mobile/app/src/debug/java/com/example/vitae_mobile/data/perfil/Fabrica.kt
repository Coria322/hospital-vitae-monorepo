package com.example.vitae_mobile.data.perfil

/** Debug usa datos simulados mientras no exista `/api/v1/mobile/perfil`. */
fun crearPerfilRepository(): PerfilRepository = MockPerfilRepository()
