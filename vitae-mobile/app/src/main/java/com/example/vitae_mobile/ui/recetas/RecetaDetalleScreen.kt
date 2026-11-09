package com.example.vitae_mobile.ui.recetas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitae_mobile.R
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.ui.components.Campo
import com.example.vitae_mobile.ui.components.CargaViewModel
import com.example.vitae_mobile.ui.components.ContenidoCarga
import com.example.vitae_mobile.ui.components.Encabezado
import com.example.vitae_mobile.ui.components.Tarjeta
import com.example.vitae_mobile.ui.seguridad.PantallaSegura
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.floor

/**
 * Matriz de módulos del QR con un margen de un módulo (zona silenciosa mínima para que lo lea una cámara).
 * ZXing codifica en ISO-8859-1 sin declararlo y los lectores lo interpretan a su manera (una "Ñ" se leería como
 * katakana): solo si el texto no es ASCII se declara UTF-8, para no añadir cabecera ECI a los códigos ASCII.
 */
fun matrizQr(texto: String): BitMatrix {
    val pistas = mutableMapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 1)
    if (texto.any { it.code > 127 }) pistas[EncodeHintType.CHARACTER_SET] = "UTF-8"
    return QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, pistas)
}

/**
 * Tamaño de módulo y desfase en píxeles para dibujar un QR de [modulos] módulos en [ladoPx]. Ambos son enteros: con
 * módulos de tamaño fraccionario el antialiasing deja líneas claras entre ellos y dificulta la lectura con la cámara.
 */
internal fun disposicionQr(ladoPx: Float, modulos: Int): Pair<Float, Float> {
    val modulo = floor(ladoPx / modulos).coerceAtLeast(1f)
    return modulo to floor((ladoPx - modulo * modulos) / 2).coerceAtLeast(0f)
}

/**
 * Detalle de una receta propia (RF-009): medicamento, dosis, vía, duración, estado y QR. Sin imprimir ni modificar.
 * Una receta dispensada o cancelada muestra el QR como no válido.
 */
@Composable
fun RecetaDetalleScreen(viewModel: CargaViewModel<Receta>, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    PantallaSegura {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Encabezado(stringResource(R.string.pantalla_receta_detalle))
                ContenidoCarga(estado = estado, alReintentar = viewModel::cargar) { DatosReceta(it) }
            }
        }
    }
}

@Composable
private fun DatosReceta(receta: Receta) {
    val noDisponible = stringResource(R.string.dato_no_disponible)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Tarjeta(espacio = 16.dp) {
            Campo(stringResource(R.string.receta_medicamento), receta.medicamento ?: noDisponible)
            Campo(stringResource(R.string.receta_dosis), receta.dosis ?: noDisponible)
            Campo(stringResource(R.string.receta_via), receta.via ?: noDisponible)
            Campo(stringResource(R.string.receta_duracion), receta.duracion ?: noDisponible)
            Campo(stringResource(R.string.receta_estado), stringResource(textoEstado(receta.estado)))
        }
        Tarjeta {
            val codigo = receta.codigoQr
            if (codigo == null) {
                Text(stringResource(R.string.receta_sin_qr), style = MaterialTheme.typography.bodyLarge)
            } else {
                CodigoQr(texto = codigo, vigente = receta.qrVigente)
            }
        }
    }
}

/** QR dibujado módulo a módulo. Si ya no es vigente se atenúa, se tacha y se rotula: nunca solo con color. */
@Composable
private fun CodigoQr(texto: String, vigente: Boolean) {
    val matriz = remember(texto) { matrizQr(texto) }
    val descripcion = stringResource(if (vigente) R.string.receta_qr_desc else R.string.receta_qr_invalido_desc)
    val rojo = MaterialTheme.colorScheme.error
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(240.dp).semantics { contentDescription = descripcion }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color.White)
                val (modulo, desfase) = disposicionQr(size.width, matriz.width)
                val tinta = if (vigente) Color.Black else Color(0xFF9AA5B4)
                for (y in 0 until matriz.height) {
                    for (x in 0 until matriz.width) {
                        if (matriz[x, y]) drawRect(tinta, Offset(desfase + x * modulo, desfase + y * modulo), Size(modulo, modulo))
                    }
                }
                if (!vigente) {
                    drawLine(rojo, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 8f)
                    drawLine(rojo, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 8f)
                }
            }
        }
        if (!vigente) {
            Text(
                text = stringResource(R.string.receta_qr_invalido),
                style = MaterialTheme.typography.titleMedium,
                color = rojo,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
