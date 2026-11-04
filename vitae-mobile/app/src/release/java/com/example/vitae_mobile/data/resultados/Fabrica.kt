package com.example.vitae_mobile.data.resultados

import java.io.File

/** ponytail: el release no lleva datos simulados; falla de forma explícita hasta que exista el backend móvil (ver PLAN.md). */
@Suppress("UNUSED_PARAMETER")
fun crearResultadosRepository(directorio: File): ResultadosRepository =
    error("Sin implementación remota de resultados: pendiente del backend móvil")
