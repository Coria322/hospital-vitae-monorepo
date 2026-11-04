package com.example.vitae_mobile.data.recetas

/** ponytail: el release no lleva datos simulados; falla de forma explícita hasta que exista el backend móvil (ver PLAN.md). */
fun crearRecetasRepository(): RecetasRepository =
    error("Sin implementación remota de recetas: pendiente del backend móvil")
