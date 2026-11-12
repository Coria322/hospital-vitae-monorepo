package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.core.Resultado
import com.example.vitae_mobile.core.luego
import com.example.vitae_mobile.core.mapear
import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.arco.SolicitudArco
import com.example.vitae_mobile.data.network.llamarDatos
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.AvisoPrevio
import com.example.vitae_mobile.data.privacidad.AvisoPrivacidad
import com.example.vitae_mobile.data.privacidad.Consentimiento
import com.example.vitae_mobile.data.privacidad.FinalidadSecundaria
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.privacidad.SolicitudConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import java.time.Instant
import java.time.LocalDate

private const val ORIGEN = "APP_MOVIL"
private const val OTORGADO = "OTORGADO"
private const val REVOCADO = "REVOCADO"

/** El backend no sirve el texto del aviso ni sus finalidades: van en la app (marcadores a validar con Legal). */
private val FINALIDADES = listOf(
    "RECORDATORIOS" to "Recibir recordatorios y comunicaciones del hospital.",
    "ESTADISTICA" to "Usar mis datos de forma anónima para fines estadísticos.",
)
private val VIGENTE_DESDE: Instant = Instant.parse("2026-10-01T12:00:00Z") // a mediodía UTC: en cualquier zona horaria cae el 1 de octubre

/**
 * Consentimientos con `GET/POST /consentimientos` y `POST /consentimientos/{id}/revocaciones`. Los registros del backend
 * son inmutables: revocar crea una fila nueva (estado REVOCADO) que apunta a la original. Aceptar el aviso guarda una
 * fila de aviso y, si hay finalidades opcionales, otra aparte, para poder revocar estas sin tocar el aviso. Los códigos de
 * las finalidades viajan en el texto: `... | SECUNDARIAS: A,B`.
 */
class PrivacidadRemoto(
    private val api: ApiBackend,
    private val backend: SesionBackend,
    private val textoAviso: String,
) : PrivacidadRepository {
    override suspend fun aviso(): Resultado<AvisoPrivacidad> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        return llamarDatos { api.consentimientos(paciente.id) }.mapear { lista ->
            val activos = lista.activos()
            val aceptados = activos.filter { it.tipo == TipoConsentimiento.FINALIDADES_SECUNDARIAS.name }.flatMap { codigosDe(it.finalidades) }.toSet()
            AvisoPrivacidad(
                version = AvisoPrevio.VERSION,
                vigenteDesde = VIGENTE_DESDE,
                textoSimplificado = textoAviso,
                urlIntegral = AvisoPrevio.URL_INTEGRAL,
                finalidadesSecundarias = FINALIDADES.map { (codigo, texto) -> FinalidadSecundaria(codigo, texto, codigo in aceptados) },
                aceptada = activos.any { it.tipo == TipoConsentimiento.AVISO_PRIVACIDAD.name && it.version == AvisoPrevio.VERSION },
            )
        }
    }

    override suspend fun registrar(solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        return when (solicitud.accion) {
            AccionConsentimiento.ACEPTAR -> aceptar(paciente, solicitud)
            AccionConsentimiento.REVOCAR -> revocar(paciente, solicitud)
        }
    }

    private suspend fun aceptar(paciente: PacienteActivo, solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
        val primero = if (solicitud.tipo == TipoConsentimiento.AVISO_PRIVACIDAD) {
            conceder(paciente, TipoConsentimiento.AVISO_PRIVACIDAD, "Aviso de privacidad simplificado v${solicitud.versionDocumento}", solicitud.versionDocumento)
        } else {
            null
        }
        if (primero is Resultado.Fallo) return primero
        if (solicitud.finalidades.isEmpty()) return primero ?: Resultado.Fallo(ApiError.Desconocido)
        val secundarias = conceder(paciente, TipoConsentimiento.FINALIDADES_SECUNDARIAS, textoSecundarias(solicitud.finalidades), solicitud.versionDocumento)
        return if (primero is Resultado.Exito) primero else secundarias
    }

    // Revocar solo ciertas finalidades: se revoca la fila que las contiene y se vuelven a otorgar las que sobran.
    private suspend fun revocar(paciente: PacienteActivo, solicitud: SolicitudConsentimiento): Resultado<Consentimiento> {
        val lista = when (val r = llamarDatos { api.consentimientos(paciente.id) }) {
            is Resultado.Fallo -> return r
            is Resultado.Exito -> r.valor
        }
        val afectadas = lista.activos().filter {
            it.tipo == TipoConsentimiento.FINALIDADES_SECUNDARIAS.name && codigosDe(it.finalidades).any { c -> c in solicitud.finalidades }
        }
        var ultima: Resultado<Consentimiento>? = null
        for (fila in afectadas) {
            val revocacion = llamarDatos { api.revocar(fila.id, RevocacionCuerpo("Revocado desde la app", paciente.titular())) }
            if (revocacion is Resultado.Fallo) return revocacion
            val sobran = codigosDe(fila.finalidades) - solicitud.finalidades.toSet()
            if (sobran.isNotEmpty()) {
                val otorgada = conceder(paciente, TipoConsentimiento.FINALIDADES_SECUNDARIAS, textoSecundarias(sobran), fila.version)
                if (otorgada is Resultado.Fallo) return otorgada
            }
            ultima = revocacion.mapear { it.aConsentimiento() }
        }
        return ultima ?: Resultado.Exito(Consentimiento("", Instant.now()))
    }

    private suspend fun conceder(paciente: PacienteActivo, tipo: TipoConsentimiento, finalidades: String, version: String): Resultado<Consentimiento> {
        val responsable = backend.usuarioId ?: return Resultado.Fallo(ApiError.SesionInvalida)
        val cuerpo = ConsentimientoCuerpo(paciente.id, tipo.name, finalidades, paciente.titular(), responsable, version, ORIGEN)
        return llamarDatos { api.consentir(cuerpo) }.mapear { it.aConsentimiento() }
    }
}

private fun PacienteActivo.titular() = nombre ?: "Titular"

private fun textoSecundarias(codigos: Collection<String>) = "Finalidades opcionales | SECUNDARIAS: ${codigos.sorted().joinToString(",")}"

private fun codigosDe(finalidades: String): List<String> =
    Regex("""SECUNDARIAS:\s*([A-Z_,]+)""").find(finalidades)?.groupValues?.get(1)?.split(',')?.filter { it.isNotBlank() }.orEmpty()

private fun ConsentimientoDto.aConsentimiento() =
    Consentimiento(id, runCatching { Instant.parse(fechaHora) }.getOrDefault(Instant.EPOCH))

/** Otorgados que nadie ha revocado: la revocación es una fila REVOCADO que apunta a la original. */
internal fun List<ConsentimientoDto>.activos(): List<ConsentimientoDto> {
    val revocados = filter { it.estado == REVOCADO }.mapNotNull { it.original }.toSet()
    return filter { it.estado == OTORGADO && it.id !in revocados }
}

/**
 * Solicitudes ARCO con `GET/POST /solicitudes-arco`. El backend exige `acreditacion_identidad = true` o rechaza la
 * solicitud (LFPDPPP): la app lo envía siempre, porque quien opera es personal del hospital que identifica al titular.
 * Es un supuesto a confirmar con Legal. No hay consulta por folio: se busca en la lista del paciente.
 */
class ArcoRemoto(private val api: ApiBackend, private val backend: SesionBackend) : ArcoRepository {
    override suspend fun solicitudes(): Resultado<List<SolicitudArco>> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        return llamarDatos { api.solicitudesArco(paciente.id) }.mapear { lista -> lista.mapNotNull { it.aSolicitud() } }
    }

    override suspend fun crear(derecho: DerechoArco): Resultado<SolicitudArco> {
        val paciente = backend.activo ?: return Resultado.Fallo(ApiError.Desconocido)
        return llamarDatos { api.crearArco(ArcoCuerpo(paciente.id, derecho.name.uppercase(), true)) }
            .luego { it.aSolicitud()?.let { s -> Resultado.Exito(s) } ?: Resultado.Fallo(ApiError.Desconocido) }
    }

    override suspend fun consultar(folio: String): Resultado<SolicitudArco> = solicitudes().luego { lista ->
        lista.firstOrNull { it.folio == folio }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ApiError.Desconocido)
    }
}

// Las fechas llegan como `2026-11-08T00:00:00.000000Z`: solo importa el día.
private fun ArcoDto.aSolicitud(): SolicitudArco? = runCatching {
    SolicitudArco(
        folio = folio,
        derecho = DerechoArco.entries.first { it.name.equals(derecho, ignoreCase = true) },
        fechaRecepcion = LocalDate.parse(fechaRecepcion.take(10)),
        respuestaLimite = LocalDate.parse(respuestaLimite.take(10)),
        ejecucionLimite = LocalDate.parse(ejecucionLimite.take(10)),
        estado = estado,
    )
}.getOrNull()
