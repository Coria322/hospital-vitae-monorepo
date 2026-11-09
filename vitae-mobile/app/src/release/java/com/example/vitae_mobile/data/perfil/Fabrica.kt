package com.example.vitae_mobile.data.perfil

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
fun crearPerfilRepository(): PerfilRepository =
    error("Sin datos simulados en release de perfil: release usa siempre el backend real (BACKEND_REAL)")
