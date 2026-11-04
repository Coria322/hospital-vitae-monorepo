package com.example.vitae_mobile.ui.theme

import androidx.compose.ui.graphics.Color

// BRAND_SYSTEM.md deja la marca sin definir (AMB-010); se adoptan los tokens de vitae-front/src/assets/main.css.
val VitaePrimary = Color(0xFF0F4C81)
val VitaeAccent = Color(0xFF2563EB)
val VitaeFondo = Color(0xFFF4F6F9)
val VitaeSuperficie = Color(0xFFFFFFFF)
val VitaeTexto = Color(0xFF1E293B)
// La web usa #64748B (4.40:1 sobre VitaeFondo, no llega a AA); aquí se oscurece lo mínimo para cumplir 4.5:1 (PROV-UX-003).
val VitaeTextoSuave = Color(0xFF5F6F86)
// Decorativo (bordes de tarjetas, esqueletos, divisores): no es un control, no necesita contraste.
val VitaeBorde = Color(0xFFE2E8F0)

// Borde y pulgar de los controles (campos de texto, interruptores, botón de contorno): WCAG 1.4.11 pide 3:1. La web
// usa #E2E8F0 (1.2:1) para todo; aquí se separa el decorativo (VitaeBorde) del de controles.
val VitaeBordeControl = Color(0xFF74839A)
