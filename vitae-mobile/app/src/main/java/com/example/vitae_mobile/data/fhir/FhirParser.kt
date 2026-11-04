package com.example.vitae_mobile.data.fhir

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.recetas.EstadoReceta
import com.example.vitae_mobile.data.recetas.Receta
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * RNF-007: un recurso ilegible, sin `resourceType` o de otro tipo es un error controlado, nunca una excepción.
 * Los campos que no conocemos se ignoran.
 */
object FhirParser {
    private val json = Json { ignoreUnknownKeys = true }

    private val invalido = Resultado.Fallo(ApiError.Desconocido)

    fun perfilDe(texto: String): Resultado<Perfil> {
        val paciente = try {
            json.decodeFromString<FhirPatient>(texto)
        } catch (e: SerializationException) {
            return invalido
        }
        if (paciente.resourceType != "Patient") return invalido
        return Resultado.Exito(
            Perfil(
                nombre = paciente.name.firstNotNullOfOrNull { it.comoTexto() },
                curp = paciente.identifier.firstOrNull { it.system == SISTEMA_CURP }?.value,
                fechaNacimiento = paciente.birthDate,
                seguroActivo = paciente.extension.firstOrNull { it.url == EXTENSION_SEGURO_ACTIVO }?.valueBoolean,
            )
        )
    }

    fun recetaDe(texto: String): Resultado<Receta> {
        val receta = try {
            json.decodeFromString<FhirMedicationRequest>(texto)
        } catch (e: SerializationException) {
            return invalido
        }
        return receta.aReceta()?.let { Resultado.Exito(it) } ?: invalido
    }

    /** Lista de recetas: un `Bundle` donde todas las entradas deben ser `MedicationRequest` válidos. */
    fun recetasDe(texto: String): Resultado<List<Receta>> {
        val bundle = try {
            json.decodeFromString<FhirBundle>(texto)
        } catch (e: SerializationException) {
            return invalido
        }
        if (bundle.resourceType != "Bundle") return invalido
        val recetas = bundle.entry.map { entrada ->
            val recurso = entrada.resource ?: return invalido
            val receta = try {
                json.decodeFromJsonElement(FhirMedicationRequest.serializer(), recurso)
            } catch (e: SerializationException) {
                return invalido
            }
            receta.aReceta() ?: return invalido
        }
        return Resultado.Exito(recetas)
    }
}

private fun FhirHumanName.comoTexto(): String? =
    text?.takeIf { it.isNotBlank() }
        ?: listOf(given.joinToString(" "), family.orEmpty()).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }

/** `null` si no valida: no es un `MedicationRequest` o no trae `id` (sin él no se puede abrir el detalle). */
private fun FhirMedicationRequest.aReceta(): Receta? {
    if (resourceType != "MedicationRequest") return null
    val identificador = id?.takeIf { it.isNotBlank() } ?: return null
    val qrEstado = extension.firstOrNull { it.url == EXTENSION_QR_ESTADO }?.valueString
    val codigo = extension.firstOrNull { it.url == EXTENSION_QR_CODIGO }?.valueString?.takeIf { it.isNotBlank() }

    // El QR consumido o cancelado también marca la receta aunque `status` no lo diga (S6 del plan).
    val estado = when {
        qrEstado == "CONSUMIDO" || status == "completed" -> EstadoReceta.Dispensada
        qrEstado == "CANCELADO" || status == "cancelled" || status == "stopped" -> EstadoReceta.Cancelada
        status == "active" -> EstadoReceta.Activa
        else -> EstadoReceta.Desconocido
    }
    val dosis = dosageInstruction.firstOrNull()
    return Receta(
        id = identificador,
        medicamento = medicationCodeableConcept?.comoTexto(),
        dosis = dosis?.doseAndRate?.firstNotNullOfOrNull { it.doseQuantity?.comoTexto() } ?: dosis?.text?.takeIf { it.isNotBlank() },
        via = dosis?.route?.comoTexto(),
        duracion = dispenseRequest?.expectedSupplyDuration?.comoTexto(),
        estado = estado,
        codigoQr = codigo,
        qrVigente = codigo != null && estado == EstadoReceta.Activa && qrEstado == "VIGENTE",
    )
}

private fun FhirCodeableConcept.comoTexto(): String? =
    text?.takeIf { it.isNotBlank() } ?: coding.firstNotNullOfOrNull { it.display?.takeIf { d -> d.isNotBlank() } }

/** `500.0 mg` → `500 mg`; sin unidad, solo el número. */
private fun FhirQuantity.comoTexto(): String? {
    val numero = value ?: return null
    val texto = if (numero % 1.0 == 0.0) numero.toLong().toString() else numero.toString()
    return listOf(texto, unit.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
}
