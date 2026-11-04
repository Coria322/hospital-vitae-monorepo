package com.example.vitae_mobile.data.fhir

import kotlinx.serialization.Serializable

/** Sistema de identificación de la CURP en HL7 México. Supuesto: el contrato de `/mobile/perfil` aún no existe. */
const val SISTEMA_CURP = "urn:oid:2.16.840.1.113883.4.629"

/** El esquema solo tiene `seguro_activo` (AMB-018); se asume como extensión del Patient. Supuesto a confirmar. */
const val EXTENSION_SEGURO_ACTIVO = "urn:vitae:seguro-activo"

/**
 * Subconjunto de HL7 FHIR R4 `Patient` que consume la app (RNF-007). `resourceType` no tiene valor por defecto:
 * un recurso sin él no valida y se rechaza.
 */
@Serializable
data class FhirPatient(
    val resourceType: String,
    val identifier: List<FhirIdentifier> = emptyList(),
    val name: List<FhirHumanName> = emptyList(),
    val birthDate: String? = null,
    val extension: List<FhirExtension> = emptyList(),
)

@Serializable
data class FhirIdentifier(val system: String? = null, val value: String? = null)

@Serializable
data class FhirHumanName(
    val text: String? = null,
    val family: String? = null,
    val given: List<String> = emptyList(),
)

@Serializable
data class FhirExtension(val url: String, val valueBoolean: Boolean? = null)
