package com.example.vitae_mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.BotonSecundario
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.Carga
import com.example.vitae_mobile.ui.components.CargandoEsqueleto
import com.example.vitae_mobile.ui.components.EstadoError
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.components.formatearFecha
import com.example.vitae_mobile.ui.components.formatearHora
import com.example.vitae_mobile.ui.navigation.Ruta
import com.example.vitae_mobile.ui.perfil.PerfilUiState
import com.example.vitae_mobile.ui.seguridad.PantallaSegura

// Secciones del paciente (UI_UX_SPEC.md, navigation flow): solo lectura, sin acciones de edición (RF-011).
private val Secciones = listOf(Ruta.Citas, Ruta.Recetas, Ruta.Resultados, Ruta.Perfil, Ruta.Arco, Ruta.Privacidad)

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
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                // El saludo es opcional: si el perfil falla, Inicio sigue siendo usable; el error se ve en Perfil.
                when (perfil) {
                    PerfilUiState.Cargando -> CargandoEsqueleto(filas = 1)
                    is PerfilUiState.Listo -> perfil.perfil.nombre?.let {
                        Text(text = stringResource(R.string.inicio_saludo, it), style = MaterialTheme.typography.titleMedium)
                    }
                    is PerfilUiState.Error -> Unit
                }
                Resumen(resumen, alReintentarResumen)
                Secciones.forEach { seccion ->
                    // PROV-UX-003: objetivo táctil >= 48 dp.
                    Button(
                        onClick = { onAbrir(seccion) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(seccion.titulo))
                    }
                }
                BotonSecundario(onClick = onCerrarSesion) {
                    Text(stringResource(R.string.inicio_cerrar_sesion))
                }
            }
        }
    }
}

@Composable
private fun Resumen(resumen: ResumenUiState, alReintentar: () -> Unit) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    val sinCita = stringResource(R.string.resumen_sin_cita)
    val sinResultado = stringResource(R.string.resumen_sin_resultado)

    Tarjeta(espacio = 12.dp) {
        FilaResumen(stringResource(R.string.resumen_proxima_cita), resumen.proximaCita, noDisponible) { cita ->
            if (cita == null) sinCita else "${formatearFecha(cita.fechaHora)} ${formatearHora(cita.fechaHora)}"
        }
        FilaResumen(stringResource(R.string.resumen_recetas_activas), resumen.recetasActivas, noDisponible) { it.toString() }
        FilaResumen(stringResource(R.string.resumen_ultimo_resultado), resumen.ultimoResultado, noDisponible) { resultado ->
            if (resultado == null) sinResultado else "${resultado.tipoEstudio} · ${formatearFecha(resultado.fechaHora)}"
        }
        if (resumen.hayError) {
            EstadoError(mensaje = stringResource(R.string.error_carga), alReintentar = alReintentar)
        }
    }
}

@Composable
private fun <T> FilaResumen(etiqueta: String, carga: Carga<T>, noDisponible: String, texto: (T) -> String) {
    when (carga) {
        Carga.Cargando -> CargandoEsqueleto(filas = 1)
        is Carga.Listo -> Campo(etiqueta, texto(carga.valor))
        is Carga.Error -> Campo(etiqueta, noDisponible)
    }
}
