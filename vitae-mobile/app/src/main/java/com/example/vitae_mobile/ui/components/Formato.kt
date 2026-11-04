package com.example.vitae_mobile.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Las fechas del servidor llegan en UTC (CON-015); se muestran en la zona del dispositivo.
private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
private val HORA = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun formatearFecha(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String = FECHA.format(instante.atZone(zona))

fun formatearHora(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String = HORA.format(instante.atZone(zona))

/** Fecha sin hora (p. ej. los plazos ARCO): no lleva zona, se muestra como llega. */
fun formatearFecha(fecha: LocalDate): String = FECHA.format(fecha)
