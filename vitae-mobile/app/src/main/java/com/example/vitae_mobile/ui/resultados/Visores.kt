package com.example.vitae_mobile.ui.resultados

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vitae_mobile.R
import com.example.vitae_mobile.ui.components.Carga
import com.example.vitae_mobile.ui.components.Esqueleto
import com.example.vitae_mobile.ui.components.EstadoError
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Páginas de un PDF con el `PdfRenderer` de la plataforma (sin dependencias). El renderer solo admite una página
 * abierta a la vez, por eso se serializa con un mutex; las páginas se dibujan bajo demanda.
 */
class PdfPaginas(archivo: File) {
    private val descriptor = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try {
        PdfRenderer(descriptor)
    } catch (e: Exception) {
        descriptor.close()
        throw e
    }
    private val candado = Mutex()

    @Volatile
    private var cerrado = false

    val total: Int = renderer.pageCount

    /** `null` si el visor ya se cerró mientras se esperaba el turno. */
    suspend fun renderizar(indice: Int, anchoPx: Int): Bitmap? = candado.withLock {
        if (cerrado) return@withLock null
        withContext(Dispatchers.Default) {
            val pagina = renderer.openPage(indice)
            try {
                val alto = (anchoPx.toFloat() * pagina.height / pagina.width).roundToInt()
                Bitmap.createBitmap(anchoPx, alto, Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(Color.WHITE)
                    pagina.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            } finally {
                pagina.close()
            }
        }
    }

    /** Se cierra cuando termine la página en curso, para no cerrar el renderer en mitad de un render. */
    fun cerrar(alcance: CoroutineScope) {
        cerrado = true
        alcance.launch {
            candado.withLock {
                renderer.close()
                descriptor.close()
            }
        }
    }
}

@Composable
fun VisorPdf(archivo: File, modifier: Modifier = Modifier) {
    val pdf = remember(archivo) { runCatching { PdfPaginas(archivo) }.getOrNull() }
    if (pdf == null) {
        EstadoError(mensaje = stringResource(R.string.error_recurso), alReintentar = null, modifier = modifier)
        return
    }
    DisposableEffect(pdf) { onDispose { pdf.cerrar(CoroutineScope(Dispatchers.Default)) } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val ancho = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pdf.total) { indice -> PaginaPdf(pdf, indice, ancho) }
        }
    }
}

@Composable
private fun PaginaPdf(pdf: PdfPaginas, indice: Int, ancho: Int) {
    val pagina by produceState<ImageBitmap?>(initialValue = null, pdf, indice, ancho) {
        value = pdf.renderizar(indice, ancho)?.asImageBitmap()
    }
    val imagen = pagina
    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = stringResource(R.string.visor_pagina_desc, indice + 1, pdf.total),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Esqueleto(alto = 320.dp)
    }
}

@Composable
fun VisorImagen(archivo: File, modifier: Modifier = Modifier) {
    val imagen by produceState<Carga<ImageBitmap>>(initialValue = Carga.Cargando, archivo) {
        value = withContext(Dispatchers.Default) {
            runCatching { decodificar(archivo) }
                .fold({ Carga.Listo(it) }, { Carga.Error(com.example.vitae_mobile.core.ApiError.Desconocido) })
        }
    }
    when (val estado = imagen) {
        Carga.Cargando -> Esqueleto(modifier = modifier, alto = 320.dp)
        is Carga.Error -> EstadoError(mensaje = stringResource(R.string.error_recurso), alReintentar = null, modifier = modifier)
        is Carga.Listo -> Image(
            bitmap = estado.valor,
            contentDescription = stringResource(R.string.visor_imagen_desc),
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

// Se limita el lado mayor a ~2048 px para no agotar la memoria con imágenes de cientos de megapíxeles.
private fun decodificar(archivo: File): ImageBitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(archivo)) { decoder, info, _ ->
        val lado = max(info.size.width, info.size.height)
        if (lado > LADO_MAXIMO_PX) decoder.setTargetSampleSize(lado / LADO_MAXIMO_PX + 1)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }.asImageBitmap()

private const val LADO_MAXIMO_PX = 2048

/** Resultado en texto plano (el backend entrega el contenido descifrado como texto). */
@Composable
fun VisorTexto(archivo: File, modifier: Modifier = Modifier) {
    val texto = remember(archivo) { archivo.readText(Charsets.UTF_8) }
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}
