package com.example.vitae_mobile.data.perfil

/** ponytail: el release no lleva datos simulados; falla de forma explícita hasta que exista el backend móvil (ver PLAN.md). */
fun crearPerfilRepository(): PerfilRepository =
    error("Sin implementación remota de perfil: pendiente del backend móvil")
