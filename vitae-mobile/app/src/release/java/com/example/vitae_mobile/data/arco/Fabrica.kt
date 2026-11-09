package com.example.vitae_mobile.data.arco

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
fun crearArcoRepository(): ArcoRepository =
    error("Sin datos simulados en release de ARCO: release usa siempre el backend real (BACKEND_REAL)")
