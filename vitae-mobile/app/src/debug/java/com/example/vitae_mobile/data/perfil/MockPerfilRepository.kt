package com.example.vitae_mobile.data.perfil

import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.data.fhir.FhirParser
import kotlinx.coroutines.delay

// Datos sintéticos (CURP ficticia): ningún dato real de pacientes en el repositorio.
const val PACIENTE_DEMO_JSON = """
{
  "resourceType": "Patient",
  "id": "paciente-demo",
  "identifier": [{ "system": "urn:oid:2.16.840.1.113883.4.629", "value": "DEMO900517MDFRRN09" }],
  "name": [{ "family": "Pérez Demo", "given": ["Ana"] }],
  "birthDate": "1990-05-17",
  "extension": [{ "url": "urn:vitae:seguro-activo", "valueBoolean": true }]
}
"""

/** Devuelve un Patient FHIR de ejemplo por el mismo parser que usará el servidor real. */
class MockPerfilRepository : PerfilRepository {
    override suspend fun perfil(): Resultado<Perfil> {
        delay(RETRASO_MS) // deja ver el estado de carga
        return FhirParser.perfilDe(PACIENTE_DEMO_JSON)
    }

    private companion object {
        const val RETRASO_MS = 500L
    }
}
