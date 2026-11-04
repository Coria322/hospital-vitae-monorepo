package com.example.vitae_mobile.data.privacidad

/** ponytail: el release no lleva datos simulados; falla de forma explícita hasta que exista el backend móvil (ver PLAN.md). */
fun crearPrivacidadRepository(): PrivacidadRepository =
    error("Sin implementación remota de privacidad: pendiente del backend móvil")
