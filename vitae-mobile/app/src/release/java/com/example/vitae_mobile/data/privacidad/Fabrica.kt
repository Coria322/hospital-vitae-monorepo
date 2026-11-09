package com.example.vitae_mobile.data.privacidad

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
fun crearPrivacidadRepository(): PrivacidadRepository =
    error("Sin datos simulados en release de privacidad: release usa siempre el backend real (BACKEND_REAL)")
