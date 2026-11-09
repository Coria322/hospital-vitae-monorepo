package com.example.vitae_mobile.data.resultados

import java.io.File

/** Release no lleva datos simulados y usa siempre el backend real: `AppContainer` no llama a esta función. */
@Suppress("UNUSED_PARAMETER")
fun crearResultadosRepository(directorio: File): ResultadosRepository =
    error("Sin datos simulados en release de resultados: release usa siempre el backend real (BACKEND_REAL)")
