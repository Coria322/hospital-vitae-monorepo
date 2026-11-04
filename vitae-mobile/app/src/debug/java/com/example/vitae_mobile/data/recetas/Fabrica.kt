package com.example.vitae_mobile.data.recetas

import com.example.vitae_mobile.data.clinico.MockRecetasRepository

/** Debug usa datos simulados mientras no exista `/api/v1/mobile/recetas`. */
fun crearRecetasRepository(): RecetasRepository = MockRecetasRepository()
