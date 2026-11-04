package com.example.vitae_mobile.data.fhir

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.perfil.Perfil
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object FhirParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * RNF-007: un recurso ilegible, sin `resourceType` o de otro tipo es un error controlado, nunca una excepción.
     * Los campos que no conocemos se ignoran.
     */
    fun perfilDe(texto: String): Resultado<Perfil> {
        val paciente = try {
            json.decodeFromString<FhirPatient>(texto)
        } catch (e: SerializationException) {
            return Resultado.Fallo(ApiError.Desconocido)
        }
        if (paciente.resourceType != "Patient") return Resultado.Fallo(ApiError.Desconocido)
        return Resultado.Exito(
            Perfil(
                nombre = paciente.name.firstNotNullOfOrNull { it.comoTexto() },
                curp = paciente.identifier.firstOrNull { it.system == SISTEMA_CURP }?.value,
                fechaNacimiento = paciente.birthDate,
                seguroActivo = paciente.extension.firstOrNull { it.url == EXTENSION_SEGURO_ACTIVO }?.valueBoolean,
            )
        )
    }
}

private fun FhirHumanName.comoTexto(): String? =
    text?.takeIf { it.isNotBlank() }
        ?: listOf(given.joinToString(" "), family.orEmpty()).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
