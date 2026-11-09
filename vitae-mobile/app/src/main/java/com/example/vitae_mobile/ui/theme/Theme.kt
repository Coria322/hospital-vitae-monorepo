package com.example.vitae_mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val EsquemaClaro = lightColorScheme(
    // El negro marca la acción principal y lo activo (botones, interruptores encendidos, foco); el azul, lo destacado.
    primary = VitaeNegro,
    onPrimary = Color.White,
    secondary = VitaeCobalto,
    onSecondary = Color.White,
    background = VitaeLienzo,
    onBackground = VitaeTinta,
    surface = VitaeBlanco,
    onSurface = VitaeTinta,
    surfaceVariant = VitaeTarjeta,
    onSurfaceVariant = VitaeTextoSuave,
    outline = VitaeBordeControl,
    outlineVariant = VitaeBorde,
    error = VitaeError,
    onError = Color.White,
    // Pista de un interruptor apagado (Material la toma de aquí).
    surfaceContainerHighest = VitaeBorde,
)

// Esquinas generosas: nada por debajo de 12 dp, ni siquiera en los campos de texto. Tarjetas 28 dp, destacada y hojas 32 dp;
// botones, píldoras y círculos usan la forma completa por su cuenta.
internal val Formas = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ponytail: solo tema claro y sin color dinamico; la marca y el contraste (PROV-UX-003) no deben depender del
// dispositivo. Agregar tema oscuro si el producto lo pide.
@Composable
fun VitaemobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
