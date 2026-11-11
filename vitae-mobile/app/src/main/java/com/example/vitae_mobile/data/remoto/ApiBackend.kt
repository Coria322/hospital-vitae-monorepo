package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.data.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API del portal móvil de `vitae-backend` (`/api/v1/mobile/…`, Sprint 5) y el login. La identidad sale del token del
 * paciente: ninguna petición lleva el identificador del paciente (RF-002.1). Las formas salen de respuestas reales del backend
 * (`app/src/test/resources/backend`). Cada petición lleva `X-Client-Version` (ver `VersionInterceptor`).
 *
 * La única llamada fuera de `/mobile` es [resultado]: el portal móvil lista los resultados pero no entrega su contenido
 * (ver PLAN.md, decisión 19).
 */
interface ApiBackend {
    @POST("auth/login")
    suspend fun login(@Body cuerpo: LoginCuerpo): ApiEnvelope<LoginDatos>

    // El backend no tiene refresh token: renueva con el propio Bearer vigente, que revoca.
    @POST("auth/refresh")
    suspend fun refrescar(@Header("Authorization") bearer: String): ApiEnvelope<TokenDatos>

    @POST("auth/logout")
    suspend fun cerrarSesion(@Header("Authorization") bearer: String): ApiEnvelope<JsonElement>

    @GET("mobile/perfil")
    suspend fun perfil(): ApiEnvelope<PerfilDto>

    @GET("mobile/citas")
    suspend fun citas(): ApiEnvelope<List<CitaDto>>

    @POST("mobile/citas")
    suspend fun solicitarCita(@Body cuerpo: CitaCuerpo): ApiEnvelope<CitaDto>

    @GET("mobile/recetas")
    suspend fun recetas(): ApiEnvelope<List<RecetaDto>>

    @GET("mobile/resultados")
    suspend fun resultados(): ApiEnvelope<List<ResultadoDto>>

    @POST("mobile/consentimientos")
    suspend fun consentir(@Body cuerpo: ConsentimientoCuerpo): ApiEnvelope<ConsentimientoDto>

    @POST("mobile/solicitudes-arco")
    suspend fun crearArco(@Body cuerpo: ArcoCuerpo): ApiEnvelope<ArcoDto>

    // Contenido del resultado, descifrado y como texto. Se pide solo con el id de un resultado de la lista del propio
    // paciente (`mobile/resultados`); el backend aún no filtra este endpoint por dueño.
    @GET("solicitudes-estudio/{id}/resultados")
    suspend fun resultado(@Path("id") idSolicitud: String): ApiEnvelope<ResultadoDetalleDto>
}

@Serializable
data class LoginCuerpo(@SerialName("id_usuario") val idUsuario: String, val password: String)

@Serializable
data class LoginDatos(val token: String, @SerialName("expires_in") val expiraEn: Long, val usuario: UsuarioDto)

/** [rol] decide si la cuenta es de paciente: el portal móvil solo admite `PACIENTE`. */
@Serializable
data class UsuarioDto(@SerialName("id_usuario") val idUsuario: String, val rol: String? = null)

@Serializable
data class TokenDatos(val token: String, @SerialName("expires_in") val expiraEn: Long)

@Serializable
data class PerfilDto(
    @SerialName("nombre_completo") val nombre: String? = null,
    val curp: String? = null,
    @SerialName("fecha_nacimiento") val fechaNacimiento: String? = null,
    @SerialName("seguro_activo") val seguroActivo: Boolean? = null,
)

/** [idMedico] es el médico de la cita; sirve de responsable al registrar un consentimiento. */
@Serializable
data class CitaDto(
    @SerialName("id_cita") val id: String,
    @SerialName("id_usuario") val idMedico: String? = null,
    @SerialName("fecha_hora") val fechaHora: String,
    val estado: String,
)

/** Cuerpo de `POST /mobile/citas`: la hora va en UTC y los dos textos son opcionales (el servidor limita 60 y 500 caracteres). */
@Serializable
data class CitaCuerpo(
    @SerialName("fecha_hora") val fechaHora: String,
    val especialidad: String? = null,
    val motivo: String? = null,
)

@Serializable
data class RecetaDto(
    @SerialName("id_receta") val id: String,
    @SerialName("id_usuario") val idMedico: String? = null,
    val medicamentos: String? = null,
    val indicaciones: String? = null,
    @SerialName("qr_codigo") val qrCodigo: String? = null,
    @SerialName("qr_estado") val qrEstado: String? = null,
)

@Serializable
data class ResultadoDto(
    @SerialName("id_solicitud") val idSolicitud: String,
    val mime: String,
    @SerialName("fecha_hora") val fechaHora: String,
    val solicitud: SolicitudDto? = null,
)

@Serializable
data class SolicitudDto(@SerialName("tipo_estudio") val tipo: String? = null)

@Serializable
data class ResultadoDetalleDto(
    val resultado: ResultadoContenidoDto,
    @SerialName("contenido_descifrado") val contenido: String? = null,
)

@Serializable
data class ResultadoContenidoDto(val mime: String)

@Serializable
data class ConsentimientoCuerpo(
    val tipo: String,
    val finalidades: String,
    @SerialName("firmante_nombre") val firmante: String,
    @SerialName("medico_responsable_id") val responsable: String,
)

@Serializable
data class ConsentimientoDto(
    @SerialName("id_consentimiento") val id: String,
    @SerialName("fecha_hora") val fechaHora: String,
)

@Serializable
data class ArcoCuerpo(
    val derecho: String,
    @SerialName("descripcion_peticion") val descripcion: String,
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
