package com.example.vitae_mobile.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.example.vitae_mobile.core.ApiError

/** Bloque gris estático de "cargando" (UI_UX_SPEC: skeleton). Sin animación: el ERS no define motion. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, alto: Dp = 20.dp) {
    Column(modifier = modifier.fillMaxWidth().height(alto).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))) {}
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

/**
 * Pinta los tres estados de una carga: esqueleto, error con reintento (sin reintento en un 403, RF-003) y el
 * contenido, o [mensajeVacio] si la lista llegó vacía.
 */
@Composable
fun <T> ContenidoCarga(
    estado: Carga<T>,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    estaVacio: (T) -> Boolean = { false },
    @StringRes mensajeVacio: Int? = null,
    contenido: @Composable (T) -> Unit,
) {
    when (estado) {
        Carga.Cargando -> CargandoEsqueleto(filas = 4, modifier = modifier)
        is Carga.Error -> EstadoError(
            mensaje = stringResource(mensajeDeError(estado.error)),
            alReintentar = if (estado.error == ApiError.SinPermisos) null else alReintentar,
            modifier = modifier,
        )
        is Carga.Listo ->
            if (mensajeVacio != null && estaVacio(estado.valor)) {
                Text(text = stringResource(mensajeVacio), style = MaterialTheme.typography.bodyLarge, modifier = modifier)
            } else {
                contenido(estado.valor)
            }
    }
}

// RF-003: 403 → permisos; red o 5xx → reintento; recurso ilegible o inválido (RNF-007) → error controlado.
@StringRes
fun mensajeDeError(error: ApiError): Int = when (error) {
    ApiError.SinPermisos -> R.string.error_permisos
    ApiError.Red, ApiError.Servidor -> R.string.error_carga
    ApiError.SesionInvalida, ApiError.CuentaBloqueada, ApiError.AppIncompatible, ApiError.Desconocido -> R.string.error_recurso
}
