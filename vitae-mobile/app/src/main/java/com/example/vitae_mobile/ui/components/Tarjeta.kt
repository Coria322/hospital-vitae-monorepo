package com.example.vitae_mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de lista (Warm Card): fondo cálido, esquinas de 28 dp y borde de 1 dp; se separa del lienzo por color, no por
 * sombra. El color de contenido se fija en tinta: Material lo derivaría del gris de las variantes de superficie.
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    espacio: Dp = 8.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(espacio), content = contenido)
    }
}

/**
 * Tarjeta destacada (Featured Card): el azul cobalto, esquinas de 32 dp y texto blanco. Una pantalla lleva a lo sumo una o
 * dos. El texto va en blanco puro: con transparencia deja de llegar a 4,5:1 sobre este azul.
 */
@Composable
fun TarjetaDestacada(
    modifier: Modifier = Modifier,
    espacio: Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(espacio), content = contenido)
    }
}
