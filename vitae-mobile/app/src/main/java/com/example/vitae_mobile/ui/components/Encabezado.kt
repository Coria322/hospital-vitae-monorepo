package com.example.vitae_mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R

/** Cómo volver a la pantalla anterior; `null` si no hay (pantallas raíz). Lo provee `VitaeNavHost`. */
val LocalAtras = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Cabecera de pantalla: botón circular de retroceso (solo si hay a dónde volver) y el título en 24 / 500, a la izquierda.
 * El botón hace lo mismo que el gesto de atrás del sistema.
 */
@Composable
fun Encabezado(titulo: String, modifier: Modifier = Modifier) {
    val atras = LocalAtras.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (atras != null) {
            BotonCircular(onClick = atras, descripcion = stringResource(R.string.volver)) { IconoFlecha(Flecha.Atras) }
        }
        Text(text = titulo, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    }
}

/** Botón circular de 44 dp (blanco sobre el lienzo) con objetivo táctil de 48 dp y descripción para TalkBack. */
@Composable
fun BotonCircular(
    onClick: () -> Unit,
    descripcion: String,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.surface,
    contenido: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = descripcion }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CirculoIcono(fondo = fondo, tamano = 44.dp, contenido = contenido)
    }
}

/** Solo el círculo, para los botones que van dentro de tarjetas que ya son tocables. */
@Composable
fun CirculoIcono(fondo: Color, tamano: Dp, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Box(modifier = modifier.size(tamano).background(fondo, CircleShape), contentAlignment = Alignment.Center) { contenido() }
}

enum class Flecha { Atras, Salida }

/** Flecha lineal de trazo fino (1,5 dp) con puntas redondeadas: ← para volver, ↗ para abrir. */
@Composable
fun IconoFlecha(flecha: Flecha, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier = modifier.size(20.dp)) {
        val ancho = size.width
        val alto = size.height
        val trazo = Path().apply {
            when (flecha) {
                Flecha.Atras -> {
                    moveTo(ancho * 0.78f, alto * 0.5f)
                    lineTo(ancho * 0.22f, alto * 0.5f)
                    moveTo(ancho * 0.46f, alto * 0.26f)
                    lineTo(ancho * 0.22f, alto * 0.5f)
                    lineTo(ancho * 0.46f, alto * 0.74f)
                }
                Flecha.Salida -> {
                    moveTo(ancho * 0.28f, alto * 0.72f)
                    lineTo(ancho * 0.72f, alto * 0.28f)
                    moveTo(ancho * 0.34f, alto * 0.28f)
                    lineTo(ancho * 0.72f, alto * 0.28f)
                    lineTo(ancho * 0.72f, alto * 0.66f)
                }
            }
        }
        drawPath(trazo, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
