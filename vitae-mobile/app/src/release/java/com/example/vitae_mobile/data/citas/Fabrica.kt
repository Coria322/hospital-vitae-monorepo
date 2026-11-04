package com.example.vitae_mobile.data.citas

/** ponytail: el release no lleva datos simulados; falla de forma explícita hasta que exista el backend móvil (ver PLAN.md). */
fun crearCitasRepository(): CitasRepository =
    error("Sin implementación remota de citas: pendiente del backend móvil")
