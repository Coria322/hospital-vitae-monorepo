package com.example.vitae_mobile.data.privacidad

/** Debug usa datos simulados mientras no exista el backend móvil de privacidad. */
fun crearPrivacidadRepository(): PrivacidadRepository = MockPrivacidadRepository()
