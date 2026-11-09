package com.example.vitae_mobile.data.arco

/** Debug usa datos simulados mientras no exista `/api/v1/mobile/solicitudes-arco`. */
fun crearArcoRepository(): ArcoRepository = MockArcoRepository()
