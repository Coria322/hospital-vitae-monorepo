package com.example.vitae_mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Carga
import com.example.vitae_mobile.ui.components.CargandoEsqueleto
import com.example.vitae_mobile.ui.components.CirculoIcono
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.EstadoError
import com.example.vitae_mobile.ui.components.Flecha
import com.example.vitae_mobile.ui.components.IconoFlecha
import com.example.vitae_mobile.ui.components.TarjetaDestacada
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.formatearHora
import com.example.vitae_mobile.ui.navigation.Ruta
import com.example.vitae_mobile.ui.perfil.PerfilUiState
import com.example.vitae_mobile.ui.seguridad.PantallaSegura
import com.example.vitae_mobile.ui.theme.VitaeChip

// Secciones del paciente (UI_UX_SPEC.md, navigation flow): solo lectura, sin acciones de edición (RF-011).
private val Secciones = listOf(Ruta.Citas, Ruta.Recetas, Ruta.Resultados, Ruta.Perfil, Ruta.Arco, Ruta.Privacidad)

/**
 * Inicio según la guía de estilo: título y saludo, la tarjeta azul destacada con el resumen y las secciones en una
 * cuadrícula de dos columnas de tarjetas claras con su círculo ↗. Las mismas secciones y acciones de siempre.
 */
@Composable
fun InicioScreen(
    perfil: PerfilUiState,
    resumen: ResumenUiState,
    alReintentarResumen: () -> Unit,
    onAbrir: (Ruta) -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Encabezado(stringResource(R.string.app_name))
                // El saludo es opcional: si el perfil falla, Inicio sigue siendo usable; el error se ve en Perfil.
                when (perfil) {
                    PerfilUiState.Cargando -> CargandoEsqueleto(filas = 1)
                    is PerfilUiState.Listo -> perfil.perfil.nombre?.let {
                        Text(text = stringResource(R.string.inicio_saludo, it), style = MaterialTheme.typography.titleMedium)
                    }
                    is PerfilUiState.Error -> Unit
                }
                Resumen(resumen)
                BotonSecundario(onClick = alReintentarResumen) {
                    Text("Sincronizar con servidor")
                }
                // El error de reintento va fuera de la tarjeta azul: el rojo no se lee sobre ese azul.
                if (resumen.hayError) {
                    EstadoError(mensaje = stringResource(R.string.error_carga), alReintentar = alReintentarResumen)
                }
                Cuadricula(onAbrir)
                BotonSecundario(onClick = onCerrarSesion) {
                    Text(stringResource(R.string.inicio_cerrar_sesion))
                }
            }
        }
    }
}

@Composable
private fun Resumen(resumen: ResumenUiState) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    val sinCita = stringResource(R.string.resumen_sin_cita)
    val sinResultado = stringResource(R.string.resumen_sin_resultado)
    TarjetaDestacada {
        Cifra(stringResource(R.string.resumen_proxima_cita), resumen.proximaCita, noDisponible) { cita ->
            if (cita == null) sinCita else "${formatearFecha(cita.fechaHora)} ${formatearHora(cita.fechaHora)}"
        }
        Cifra(stringResource(R.string.resumen_recetas_activas), resumen.recetasActivas, noDisponible) { it.toString() }
        Cifra(stringResource(R.string.resumen_ultimo_resultado), resumen.ultimoResultado, noDisponible) { resultado ->
            if (resultado == null) sinResultado else "${resultado.tipoEstudio} · ${formatearFecha(resultado.fechaHora)}"
        }
    }
}

// Cifra de la guía: el valor arriba (16 / 500) y la etiqueta debajo (12 / 400), en blanco puro sobre el azul.
@Composable
private fun <T> Cifra(etiqueta: String, carga: Carga<T>, noDisponible: String, texto: (T) -> String) {
    when (carga) {
        Carga.Cargando -> CargandoEsqueleto(filas = 1)
        is Carga.Listo -> CifraTexto(texto(carga.valor), etiqueta)
        is Carga.Error -> CifraTexto(noDisponible, etiqueta)
    }
}

@Composable
private fun CifraTexto(valor: String, etiqueta: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(text = valor, style = MaterialTheme.typography.titleMedium)
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Cuadricula(onAbrir: (Ruta) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Secciones.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { seccion -> TarjetaSeccion(seccion, onAbrir, Modifier.weight(1f).fillMaxHeight()) }
                // Una fila de una sola sección conserva el ancho de media columna.
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// Tarjeta de opción: nombre abajo a la izquierda (18 / 500) y el círculo ↗ arriba a la derecha. Toda la tarjeta es el
// botón (objetivo táctil de sobra) y el círculo es solo adorno.
@Composable
private fun TarjetaSeccion(seccion: Ruta, onAbrir: (Ruta) -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { onAbrir(seccion) },
        modifier = modifier.heightIn(min = 128.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        // Toda la fila comparte la altura de la más alta (IntrinsicSize.Max): el nombre queda abajo y el círculo arriba.
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CirculoIcono(fondo = VitaeChip, tamano = 40.dp) { IconoFlecha(Flecha.Salida) }
            }
            Spacer(Modifier.height(12.dp))
            Text(text = stringResource(seccion.titulo), style = MaterialTheme.typography.titleLarge)
        }
    }
}
