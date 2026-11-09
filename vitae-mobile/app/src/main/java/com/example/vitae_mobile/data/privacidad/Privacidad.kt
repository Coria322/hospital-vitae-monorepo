package com.example.vitae_mobile.data.privacidad

import com.example.vitae_mobile.core.Resultado
import java.time.Instant

/** Finalidad secundaria: opcional y en una opción independiente, nunca preseleccionada (PROV-RN-008). */
data class FinalidadSecundaria(val codigo: String, val descripcion: String, val aceptada: Boolean)

/**
 * Aviso de privacidad vigente (AVISO_PRIVACIDAD). El texto, el enlace al aviso integral y el estado de aceptación del
 * paciente no están en el esquema de la especificación: son supuestos a confirmar con el backend (ver PLAN.md).
 */
data class AvisoPrivacidad(
    val version: String,
    val vigenteDesde: Instant,
    val textoSimplificado: String,
    val urlIntegral: String,
    val finalidadesSecundarias: List<FinalidadSecundaria>,
    /** El paciente ya aceptó esta versión del aviso. */
    val aceptada: Boolean,
)

enum class TipoConsentimiento { AVISO_PRIVACIDAD, FINALIDADES_SECUNDARIAS }

enum class AccionConsentimiento { ACEPTAR, REVOCAR }

/** Cuerpo de `POST /mobile/consentimientos`; [versionDocumento] es la versión del aviso que se mostró. */
data class SolicitudConsentimiento(
    val tipo: TipoConsentimiento,
    val versionDocumento: String,
    val finalidades: List<String>,
    val accion: AccionConsentimiento,
)

/** Los consentimientos son inmutables (CON-016): revocar crea un registro nuevo, no modifica el anterior. */
data class Consentimiento(val id: String, val fechaHora: Instant)

/** Escrituras de la lista blanca (SUP-002): la identidad sale del token, no hay parámetro de paciente (RF-002.1). */
interface PrivacidadRepository {
    suspend fun aviso(): Resultado<AvisoPrivacidad>

    /** Responde 201 con el registro creado. */
    suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento>
}
