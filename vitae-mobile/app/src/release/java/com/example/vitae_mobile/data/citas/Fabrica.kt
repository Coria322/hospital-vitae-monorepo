package com.example.vitae_mobile.data.citas

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
fun crearCitasRepository(): CitasRepository =
    error("Sin datos simulados en release de citas: release usa siempre el backend real (BACKEND_REAL)")
