package com.example.vitae_mobile.data.fhir

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** El QR no es un campo de MedicationRequest: el contrato de `/mobile/recetas` no existe aún, se asume como extensiones. */
const val EXTENSION_QR_CODIGO = "urn:vitae:qr-codigo"
const val EXTENSION_QR_ESTADO = "urn:vitae:qr-estado"

/** Subconjunto de HL7 FHIR R4 `MedicationRequest` que consume la app (RNF-007). */
@Serializable
data class FhirMedicationRequest(
    val resourceType: String,
    val id: String? = null,
    val status: String? = null,
    val medicationCodeableConcept: FhirCodeableConcept? = null,
    val dosageInstruction: List<FhirDosage> = emptyList(),
    val dispenseRequest: FhirDispenseRequest? = null,
    val extension: List<FhirExtension> = emptyList(),
)

@Serializable
data class FhirCodeableConcept(val text: String? = null, val coding: List<FhirCoding> = emptyList())

@Serializable
data class FhirCoding(val display: String? = null, val code: String? = null)

@Serializable
data class FhirDosage(
    val text: String? = null,
    val route: FhirCodeableConcept? = null,
    val doseAndRate: List<FhirDoseAndRate> = emptyList(),
)

@Serializable
data class FhirDoseAndRate(val doseQuantity: FhirQuantity? = null)

@Serializable
data class FhirQuantity(val value: Double? = null, val unit: String? = null)

@Serializable
data class FhirDispenseRequest(val expectedSupplyDuration: FhirQuantity? = null)

/** Respuesta de la lista de recetas: `Bundle` con un `MedicationRequest` por entrada. */
@Serializable
data class FhirBundle(val resourceType: String, val entry: List<FhirBundleEntry> = emptyList())

@Serializable
data class FhirBundleEntry(val resource: JsonElement? = null)
