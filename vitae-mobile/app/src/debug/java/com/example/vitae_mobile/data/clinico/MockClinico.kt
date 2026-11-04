package com.example.vitae_mobile.data.clinico

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.citas.Cita
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.fhir.FhirParser
import com.example.vitae_mobile.data.recetas.Receta
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.resultados.ArchivoDescargado
import com.example.vitae_mobile.data.resultados.ResultadoEstudio
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Datos sintéticos: ningún dato real de pacientes en el repositorio.
private const val RETRASO_MS = 500L

/** Dos citas futuras y una pasada, relativas al reloj para que el resumen de Inicio siempre tenga una "próxima". */
class MockCitasRepository(private val reloj: Clock = Clock.systemUTC()) : CitasRepository {
    override suspend fun citas(): Resultado<List<Cita>> {
        delay(RETRASO_MS)
        val hoy = reloj.instant().truncatedTo(ChronoUnit.HOURS)
        return Resultado.Exito(
            listOf(
                Cita("cita-001", hoy.plus(Duration.ofDays(3)).plusSeconds(60 * 30), "Programada"),
                Cita("cita-002", hoy.plus(Duration.ofDays(10)), "Programada"),
                Cita("cita-003", hoy.minus(Duration.ofDays(20)), "Atendida"),
            )
        )
    }
}

private fun medicamento(id: String, nombre: String, status: String, duracion: Int, qrEstado: String) = """
    {
      "resourceType": "MedicationRequest",
      "id": "$id",
      "status": "$status",
      "medicationCodeableConcept": { "text": "$nombre" },
      "dosageInstruction": [{
        "text": "1 cada 8 horas",
        "route": { "text": "Oral" },
        "doseAndRate": [{ "doseQuantity": { "value": 500, "unit": "mg" } }]
      }],
      "dispenseRequest": { "expectedSupplyDuration": { "value": $duracion, "unit": "días" } },
      "extension": [
        { "url": "urn:vitae:qr-codigo", "valueString": "VITAE-DEMO-$id" },
        { "url": "urn:vitae:qr-estado", "valueString": "$qrEstado" }
      ]
    }
""".trimIndent()

private val RECETAS_DEMO = listOf(
    medicamento("rec-001", "Paracetamol 500 mg tabletas", "active", 7, "VIGENTE"),
    medicamento("rec-002", "Amoxicilina 500 mg cápsulas", "completed", 10, "CONSUMIDO"),
    medicamento("rec-003", "Ibuprofeno 400 mg tabletas", "cancelled", 5, "CANCELADO"),
)

/** Recetas como FHIR: la lista es un `Bundle` y el detalle un `MedicationRequest`, ambos por el parser real. */
class MockRecetasRepository : RecetasRepository {
    override suspend fun recetas(): Resultado<List<Receta>> {
        delay(RETRASO_MS)
        val entradas = RECETAS_DEMO.joinToString(",") { """{ "resource": $it }""" }
        return FhirParser.recetasDe("""{ "resourceType": "Bundle", "type": "searchset", "entry": [$entradas] }""")
    }

    override suspend fun receta(id: String): Resultado<Receta> {
        delay(RETRASO_MS)
        val json = RECETAS_DEMO.firstOrNull { it.contains("\"id\": \"$id\"") }
            ?: return Resultado.Fallo(ApiError.SinPermisos) // una receta ajena responde 403, como el servidor real
        return FhirParser.recetaDe(json)
    }
}

/** Un PDF de dos páginas y una imagen, generados al abrirlos; simula la descarga por tramos para ver el avance. */
class MockResultadosRepository(
    private val directorio: File,
    private val reloj: Clock = Clock.systemUTC(),
) : ResultadosRepository {
    private val pasos = 5

    override suspend fun resultados(): Resultado<List<ResultadoEstudio>> {
        delay(RETRASO_MS)
        val hoy = reloj.instant().truncatedTo(ChronoUnit.HOURS)
        return Resultado.Exito(
            listOf(
                ResultadoEstudio("res-001", "Biometría hemática", hoy.minus(Duration.ofDays(5)), "application/pdf"),
                ResultadoEstudio("res-002", "Radiografía de tórax", hoy.minus(Duration.ofDays(12)), "image/png"),
            )
        )
    }

    override suspend fun archivo(id: String, progreso: (Float) -> Unit): Resultado<ArchivoDescargado> {
        val mime = when (id) {
            "res-001" -> "application/pdf"
            "res-002" -> "image/png"
            else -> return Resultado.Fallo(ApiError.SinPermisos) // un resultado ajeno responde 403 (RF-010, escenario 2)
        }
        return withContext(Dispatchers.IO) {
            directorio.mkdirs()
            val destino = File(directorio, id)
            for (paso in 1..pasos) {
                delay(150)
                progreso(paso.toFloat() / pasos)
            }
            if (mime == "application/pdf") escribirPdf(destino) else escribirImagen(destino)
            Resultado.Exito(ArchivoDescargado(destino, mime))
        }
    }

    private fun escribirPdf(destino: File) {
        val documento = PdfDocument()
        val pintura = Paint().apply { textSize = 22f; isAntiAlias = true }
        repeat(2) { numero ->
            val pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numero + 1).create())
            pagina.canvas.drawText("Biometría hemática", 48f, 80f, pintura)
            pagina.canvas.drawText("Datos sintéticos - página ${numero + 1} de 2", 48f, 120f, pintura)
            documento.finishPage(pagina)
        }
        destino.outputStream().use { documento.writeTo(it) }
        documento.close()
    }

    private fun escribirImagen(destino: File) {
        val imagen = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(imagen)
        lienzo.drawColor(Color.rgb(30, 41, 59))
        val pintura = Paint().apply { color = Color.WHITE; textSize = 40f; isAntiAlias = true }
        lienzo.drawText("Radiografía de tórax", 60f, 100f, pintura)
        lienzo.drawText("Datos sintéticos", 60f, 160f, pintura)
        destino.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
