package com.example.vitae_mobile.data.recetas

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
fun crearRecetasRepository(): RecetasRepository =
    error("Sin datos simulados en release de recetas: release usa siempre el backend real (BACKEND_REAL)")
