package com.example.vitae_mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// La guía pide una sans geométrica (Manrope, Plus Jakarta Sans o Inter Tight). Ninguna viene con Android y no se descargó
// ninguna: se usa la sans del sistema; para cambiarla basta con poner aquí la FontFamily.
private val Sans = FontFamily.Default

// Pesos 400 y 500 (nada de 700 o más): la jerarquía sale del tamaño, el color y el espacio. El mínimo es 12 sp; la guía usa
// 11 px para las etiquetas, que en Android quedarían ilegibles con la fuente grande del sistema.
val Typography = Typography(
    // Título de pantalla: 24 / 500, interlineado 1,15, tracking -0,01 em.
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.24).sp),
    // Título de tarjeta u opción: 18 / 500.
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 23.sp, letterSpacing = (-0.09).sp),
    // Valores y cifras: 16 / 500.
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    // Texto corrido: se mantiene en 16 / 400 para que los párrafos (aviso de privacidad) se lean cómodos.
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    // Texto de interfaz: 14 / 500.
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.sp),
    // Etiquetas y descripciones: 12 / 400, en gris.
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
)
