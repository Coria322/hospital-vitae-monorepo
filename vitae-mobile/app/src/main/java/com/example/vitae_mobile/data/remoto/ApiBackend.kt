package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.data.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints normales de `vitae-backend` (los de personal bajo `/api/v1`; no hay namespace móvil). Las formas salen de
 * respuestas reales del backend (`app/src/test/resources/backend`). Estos endpoints identifican al paciente con un id
 * en la ruta o el cuerpo, por eso este archivo es la excepción documentada a RF-002.1 (ver PLAN.md, decisión 16).
 */
interface ApiBackend {
    @POST("auth/login")
    suspend fun login(@Body cuerpo: LoginCuerpo): ApiEnvelope<LoginDatos>

    // El backend no tiene refresh token: renueva con el propio Bearer vigente, que revoca.
    @POST("auth/refresh")
    suspend fun refrescar(@Header("Authorization") bearer: String): ApiEnvelope<TokenDatos>

    @POST("auth/logout")
    suspend fun cerrarSesion(@Header("Authorization") bearer: String): ApiEnvelope<JsonElement>

    @GET("pacientes")
    suspend fun pacientes(): ApiEnvelope<PaginaDto<PacienteDto>>

    @GET("pacientes/{id}")
    suspend fun paciente(@Path("id") id: String): ApiEnvelope<PacienteDto>

    // FHIR R4 crudo: lo lee `FhirParser`.
    @GET("fhir/Patient/{id}")
    suspend fun fhirPatient(@Path("id") id: String): ResponseBody

    @GET("expedientes-clinicos/{id}")
    suspend fun expediente(@Path("id") id: String): ApiEnvelope<ExpedienteDto>

    @GET("recetas/{id}")
    suspend fun receta(@Path("id") id: String): ApiEnvelope<RecetaDto>

    @GET("solicitudes-estudio/{id}/resultados")
    suspend fun resultado(@Path("id") idSolicitud: String): ApiEnvelope<ResultadoDetalleDto>

    @GET("consentimientos")
    suspend fun consentimientos(@Query("id_paciente") idPaciente: String): ApiEnvelope<List<ConsentimientoDto>>

    @POST("consentimientos")
    suspend fun consentir(@Body cuerpo: ConsentimientoCuerpo): ApiEnvelope<ConsentimientoDto>

    @POST("consentimientos/{id}/revocaciones")
    suspend fun revocar(@Path("id") id: String, @Body cuerpo: RevocacionCuerpo): ApiEnvelope<ConsentimientoDto>

    @GET("solicitudes-arco")
    suspend fun solicitudesArco(@Query("id_paciente") idPaciente: String): ApiEnvelope<List<ArcoDto>>

    @POST("solicitudes-arco")
    suspend fun crearArco(@Body cuerpo: ArcoCuerpo): ApiEnvelope<ArcoDto>
}

@Serializable
data class LoginCuerpo(@SerialName("id_usuario") val idUsuario: String, val password: String)

@Serializable
data class LoginDatos(val token: String, @SerialName("expires_in") val expiraEn: Long, val usuario: UsuarioDto)

@Serializable
data class UsuarioDto(@SerialName("id_usuario") val idUsuario: String, val rol: String? = null)

@Serializable
data class TokenDatos(val token: String, @SerialName("expires_in") val expiraEn: Long)

/** Página de Laravel: la lista viene en `data`. */
@Serializable
data class PaginaDto<T>(val data: List<T> = emptyList())

@Serializable
data class PacienteDto(
    @SerialName("id_paciente") val id: String,
    @SerialName("nombre_completo") val nombre: String? = null,
    val curp: String? = null,
    @SerialName("folio_urgencias") val folioUrgencias: String? = null,
    @SerialName("seguro_activo") val seguroActivo: Boolean? = null,
    val expediente: ExpedienteRefDto? = null,
)

@Serializable
data class ExpedienteRefDto(@SerialName("id_expediente") val id: String)

/** Del expediente solo se leen las recetas y las solicitudes de estudio (con su resultado); lo demás se ignora. */
@Serializable
data class ExpedienteDto(
    val recetas: List<RecetaDto> = emptyList(),
    @SerialName("solicitudes_estudio") val solicitudes: List<SolicitudDto> = emptyList(),
)

@Serializable
data class RecetaDto(
    @SerialName("id_receta") val id: String,
    val folio: String? = null,
    val medicamentos: String? = null,
    val indicaciones: String? = null,
    @SerialName("qr_codigo") val qrCodigo: String? = null,
    @SerialName("qr_estado") val qrEstado: String? = null,
)

@Serializable
data class SolicitudDto(
    @SerialName("id_solicitud") val id: String,
    @SerialName("tipo_estudio") val tipo: String,
    val resultado: ResultadoDto? = null,
)

@Serializable
data class ResultadoDto(val mime: String, @SerialName("fecha_hora") val fechaHora: String)

@Serializable
data class ResultadoDetalleDto(
    val resultado: ResultadoDto,
    @SerialName("contenido_descifrado") val contenido: String? = null,
)

@Serializable
data class ConsentimientoDto(
    @SerialName("id_consentimiento") val id: String,
    val tipo: String,
    @SerialName("version_documento") val version: String,
    val finalidades: String,
    val estado: String,
    @SerialName("id_consentimiento_original") val original: String? = null,
    @SerialName("fecha_hora") val fechaHora: String,
)

@Serializable
data class ConsentimientoCuerpo(
    @SerialName("id_paciente") val idPaciente: String,
    val tipo: String,
    val finalidades: String,
    @SerialName("firmante_nombre") val firmante: String,
    @SerialName("medico_responsable_id") val responsable: String,
    @SerialName("version_documento") val version: String,
    val medio: String,
)

@Serializable
data class RevocacionCuerpo(
    @SerialName("motivo_revocacion") val motivo: String,
    @SerialName("firmante_nombre") val firmante: String,
)

@Serializable
data class ArcoCuerpo(
    @SerialName("id_paciente") val idPaciente: String,
    val derecho: String,
    @SerialName("acreditacion_identidad") val acreditacionIdentidad: Boolean,
)

@Serializable
data class ArcoDto(
    val folio: String,
    val derecho: String,
    @SerialName("fecha_recepcion") val fechaRecepcion: String,
    @SerialName("respuesta_limite") val respuestaLimite: String,
    @SerialName("ejecucion_limite") val ejecucionLimite: String,
    val estado: String,
)
