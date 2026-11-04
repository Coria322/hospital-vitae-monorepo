package com.example.vitae_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R

/** Bloque gris estático de "cargando" (UI_UX_SPEC: skeleton). Sin animación: el ERS no define motion. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, alto: Dp = 20.dp) {
    Column(modifier = modifier.fillMaxWidth().height(alto).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))) {}
}

/** Varias filas de esqueleto anunciadas como una sola cosa a TalkBack. */
@Composable
fun CargandoEsqueleto(filas: Int, modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.cargando)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(filas) { Esqueleto(alto = 28.dp) }
    }
}

/** Estado de error con reintento (UI_UX_SPEC). Sin [alReintentar] solo informa (p. ej. un 403). */
@Composable
fun EstadoError(mensaje: String, alReintentar: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = mensaje, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        if (alReintentar != null) {
            Button(onClick = alReintentar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reintentar))
            }
        }
    }
}
