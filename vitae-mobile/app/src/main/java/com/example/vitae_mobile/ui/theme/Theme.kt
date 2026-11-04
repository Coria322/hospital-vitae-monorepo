package com.example.vitae_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val EsquemaClaro = lightColorScheme(
    primary = VitaePrimary,
    onPrimary = Color.White,
    secondary = VitaeAccent,
    onSecondary = Color.White,
    background = VitaeFondo,
    onBackground = VitaeTexto,
    surface = VitaeSuperficie,
    onSurface = VitaeTexto,
    onSurfaceVariant = VitaeTextoSuave,
    outline = VitaeBorde,
)

// ponytail: solo tema claro y sin color dinamico; la marca y el contraste (PROV-UX-003) no deben depender del
// dispositivo. Agregar tema oscuro si el producto lo pide.
@Composable
fun VitaemobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
