package com.example.vitae_mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de la guía de estilo "Aetna Health App" (lienzo gris perla, tarjetas cálidas, un solo azul cobalto y negro para
// la acción principal). Donde un par de la guía no llega a WCAG AA (PROV-UX-003) se oscurece lo mínimo y se anota aquí.

/** Fondo de pantalla (Pearl Canvas). */
val VitaeLienzo = Color(0xFFF2F2F2)

/** Hojas, píldoras inactivas y botones circulares sobre el lienzo (Paper White). */
val VitaeBlanco = Color(0xFFFFFFFF)

/** Tarjetas de lista y de opción, siempre con borde de 1 dp (Warm Card). */
val VitaeTarjeta = Color(0xFFFAF9F7)

/** Fondo de botones circulares dentro de tarjetas claras y de chips deshabilitados (Soft Chip). */
val VitaeChip = Color(0xFFEDEBE8)

/** Títulos y texto principal (Graphite). */
val VitaeTinta = Color(0xFF1A1A1A)

/** Acción principal y estado activo (Ink Black). */
val VitaeNegro = Color(0xFF000000)

/**
 * Texto secundario. La guía usa #8A8A8A (Slate Gray), que da 3,1:1 sobre el lienzo y no llega a 4,5:1; #6B6B6B da 4,8:1.
 * No usar sobre Soft Chip (4,5:1 justo).
 */
val VitaeTextoSuave = Color(0xFF6B6B6B)

/** Borde de las tarjetas claras: decorativo, no es un control (Hairline). */
val VitaeBorde = Color(0xFFE3E1DE)

/**
 * Borde y pulgar de los controles (campos de texto, interruptores): WCAG 1.4.11 pide 3:1 contra el fondo, la tarjeta y la
 * pista apagada del interruptor. Con #767676 sale 4,1:1 sobre el lienzo y 3,5:1 sobre Hairline.
 */
val VitaeBordeControl = Color(0xFF767676)

/**
 * Fondo de la tarjeta destacada. La guía usa #5584D9 con texto blanco (3,7:1, no llega a AA); se usa su propia variante
 * "Deep Cobalt" #4A74C4, que da 4,6:1 con blanco.
 */
val VitaeCobalto = Color(0xFF4A74C4)

/** Elementos sobre la tarjeta azul (círculos de ícono, botones): blanco da 5,6:1. */
val VitaeCobaltoProfundo = Color(0xFF4267A9)

/** Cobalt Blue de la guía: solo para bordes y resaltes sin texto (3,3:1 sobre el lienzo). */
val VitaeCobaltoBrillante = Color(0xFF5584D9)

/** Chips con disponibilidad y rayado de gráficas (Azure Tint). */
val VitaeAzulSuave = Color(0xFFDCE6F7)

/**
 * Error. La guía limita el rojo a puntos de notificación (#E5484D, 3,5:1); un mensaje de error necesita texto legible, así
 * que se usa #C62F34 (4,9:1).
 */
val VitaeError = Color(0xFFC62F34)
