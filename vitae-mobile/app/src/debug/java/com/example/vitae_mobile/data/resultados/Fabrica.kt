package com.example.vitae_mobile.data.resultados

import com.example.vitae_mobile.data.clinico.MockResultadosRepository
import java.io.File

/** Debug usa datos simulados mientras no exista `/api/v1/mobile/resultados`. Los archivos se generan en [directorio]. */
fun crearResultadosRepository(directorio: File): ResultadosRepository = MockResultadosRepository(directorio)
