package com.example.vitae_mobile.data.citas

import com.example.vitae_mobile.data.clinico.MockCitasRepository

/** Debug usa datos simulados mientras no exista `/api/v1/mobile/citas`. */
fun crearCitasRepository(): CitasRepository = MockCitasRepository()
