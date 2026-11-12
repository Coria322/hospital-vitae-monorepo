package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.arco.DerechoArco
import com.example.vitae_mobile.data.privacidad.AccionConsentimiento
import com.example.vitae_mobile.data.privacidad.SolicitudConsentimiento
import com.example.vitae_mobile.data.privacidad.TipoConsentimiento
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Consentimientos y ARCO contra los endpoints normales del backend. */
class PrivacidadArcoRemotoTest : BackendTest() {
    private val privacidad get() = PrivacidadRemoto(api, backend, "Texto del aviso")

    private fun fila(id: String, tipo: String, finalidades: String, estado: String = "OTORGADO", original: String? = null) =
        """{"id_consentimiento":"$id","tipo":"$tipo","version_documento":"1.0","finalidades":"$finalidades","estado":"$estado",""" +
            """"id_consentimiento_original":${original?.let { "\"$it\"" }},"fecha_hora":"2026-11-08T17:00:00.000000Z"}"""

    private fun lista(vararg filas: String) = """{"status":"success","data":[${filas.joinToString(",")}]}"""

    private suspend fun entrarComoMedico() {
        responder(fixture("auth-login"))
        exito(AuthRemoto(api, backend).iniciarSesion("MED-001", "x"))
        servidor.takeRequest()
        elegirPacienteAlfa()
    }

    @Test
    fun sinConsentimientosElAvisoNoEstaAceptado() = runTest {
        elegirPacienteAlfa()
        responder(lista())

        val aviso = exito(privacidad.aviso())

        assertFalse(aviso.aceptada)
        assertEquals("1.0", aviso.version)
        assertEquals("Texto del aviso", aviso.textoSimplificado)
        assertEquals(listOf("RECORDATORIOS", "ESTADISTICA"), aviso.finalidadesSecundarias.map { it.codigo })
        assertTrue(aviso.finalidadesSecundarias.none { it.aceptada })
        val p = peticion()
        assertEquals("/api/v1/consentimientos", p.url.encodedPath)
        assertEquals(ID_ALFA, p.url.queryParameter("id_paciente"))
    }

    @Test
    fun elAvisoOtorgadoYSusFinalidadesSeLeenDeLaLista() = runTest {
        elegirPacienteAlfa()
        responder(
            lista(
                fila("c1", "AVISO_PRIVACIDAD", "Aviso de privacidad simplificado v1.0"),
                fila("c2", "FINALIDADES_SECUNDARIAS", "Finalidades opcionales | SECUNDARIAS: ESTADISTICA"),
            ),
        )

        val aviso = exito(privacidad.aviso())

        assertTrue(aviso.aceptada)
        assertEquals(listOf(false, true), aviso.finalidadesSecundarias.map { it.aceptada })
    }

    // La lista real del backend: una fila OTORGADA y otra REVOCADO que apunta a ella. Revocado = no aceptado.
    @Test
    fun unAvisoRevocadoNoCuentaComoAceptado() = runTest {
        elegirPacienteAlfa()
        responder(fixture("consentimientos-lista"))

        assertFalse(exito(privacidad.aviso()).aceptada)
    }

    private val consentimientoCreado = """{"status":"success","data":${fila("nuevo", "AVISO_PRIVACIDAD", "x")}}"""

    @Test
    fun aceptarConFinalidadesEnvia_elAviso_yLasFinalidadesAparte() = runTest {
        entrarComoMedico()
        responder(consentimientoCreado, 201)
        responder(consentimientoCreado, 201)

        val r = exito(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "1.0", listOf("ESTADISTICA", "RECORDATORIOS"), AccionConsentimiento.ACEPTAR)))

        assertEquals("nuevo", r.id)
        assertEquals(2, servidor.requestCount - 2) // descontando el login y la elección del paciente
        val primera = peticion().body!!.utf8()
        val segunda = peticion().body!!.utf8()
        assertTrue(primera, """"tipo":"AVISO_PRIVACIDAD"""" in primera)
        assertTrue(primera, """"id_paciente":"$ID_ALFA"""" in primera)
        // Quien registra es el usuario que inició sesión; el firmante es el paciente.
        assertTrue(primera, """"medico_responsable_id":"MED-001"""" in primera)
        assertTrue(primera, """"firmante_nombre":"Paciente Sintético Alfa"""" in primera)
        assertTrue(primera, """"medio":"APP_MOVIL"""" in primera)
        assertTrue(primera, """"finalidades":"Aviso de privacidad simplificado v1.0"""" in primera)
        assertTrue(segunda, """"tipo":"FINALIDADES_SECUNDARIAS"""" in segunda)
        assertTrue(segunda, "SECUNDARIAS: ESTADISTICA,RECORDATORIOS" in segunda)
    }

    @Test
    fun aceptarSinFinalidadesEnviaSoloElAviso() = runTest {
        entrarComoMedico()
        responder(consentimientoCreado, 201)

        exito(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "1.0", emptyList(), AccionConsentimiento.ACEPTAR)))

        assertEquals(1, servidor.requestCount - 2)
    }

    @Test
    fun revocarUnaFinalidadRevocaLaFilaYVuelveAOtorgarLasQueSobran() = runTest {
        entrarComoMedico()
        responder(lista(fila("aviso", "AVISO_PRIVACIDAD", "Aviso"), fila("sec", "FINALIDADES_SECUNDARIAS", "Finalidades opcionales | SECUNDARIAS: ESTADISTICA,RECORDATORIOS")))
        responder(consentimientoCreado, 201) // la revocación
        responder(consentimientoCreado, 201) // se vuelve a otorgar ESTADISTICA

        exito(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "1.0", listOf("RECORDATORIOS"), AccionConsentimiento.REVOCAR)))

        assertEquals("GET", peticion().method)
        val revocacion = peticion()
        assertEquals("/api/v1/consentimientos/sec/revocaciones", revocacion.url.encodedPath)
        assertTrue(revocacion.body!!.utf8().contains("motivo_revocacion"))
        val otorgada = peticion().body!!.utf8()
        assertTrue(otorgada, """"tipo":"FINALIDADES_SECUNDARIAS"""" in otorgada)
        assertTrue(otorgada, "SECUNDARIAS: ESTADISTICA\"" in otorgada)
    }

    @Test
    fun revocarLoQueNoEstaOtorgadoNoMandaNada() = runTest {
        entrarComoMedico()
        responder(lista())

        exito(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "1.0", listOf("ESTADISTICA"), AccionConsentimiento.REVOCAR)))

        assertEquals(1, servidor.requestCount - 2) // solo la consulta de la lista
    }

    @Test
    fun sinPacienteElegidoNoSePuedeConsultarNiRegistrar() = runTest {
        assertEquals(ApiError.Desconocido, error(privacidad.aviso()))
        assertEquals(ApiError.Desconocido, error(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "1.0", emptyList(), AccionConsentimiento.ACEPTAR))))
    }

    @Test
    fun crearUnaSolicitudArcoEnviaElDerechoYLaAcreditacion() = runTest {
        elegirPacienteAlfa()
        responder("""{"status":"success","data":${JSON_ARCO}}""", 201)

        val solicitud = exito(ArcoRemoto(api, backend).crear(DerechoArco.Acceso))

        assertEquals("ARCO-20261108-JTQX", solicitud.folio)
        assertEquals(DerechoArco.Acceso, solicitud.derecho)
        assertEquals(LocalDate.of(2026, 11, 8), solicitud.fechaRecepcion)
        assertEquals(LocalDate.of(2026, 12, 4), solicitud.respuestaLimite)
        assertEquals(LocalDate.of(2026, 12, 25), solicitud.ejecucionLimite)
        assertEquals("RECIBIDA", solicitud.estado)
        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/solicitudes-arco", p.url.encodedPath)
        assertEquals("""{"id_paciente":"$ID_ALFA","derecho":"ACCESO","acreditacion_identidad":true}""", p.body?.utf8())
    }

    @Test
    fun cadaDerechoViajaConElNombreDelBackend() = runTest {
        elegirPacienteAlfa()
        DerechoArco.entries.forEach { derecho ->
            responder("""{"status":"success","data":${JSON_ARCO.replace("ACCESO", derecho.name.uppercase())}}""", 201)
            assertEquals(derecho, exito(ArcoRemoto(api, backend).crear(derecho)).derecho)
            assertTrue(peticion().body!!.utf8().contains(""""derecho":"${derecho.name.uppercase()}""""))
        }
    }

    @Test
    fun laListaYLaConsultaPorFolioSalenDeLasSolicitudesDelPaciente() = runTest {
        elegirPacienteAlfa()
        responder(fixture("arco-lista"))
        val lista = exito(ArcoRemoto(api, backend).solicitudes())
        assertEquals(listOf("ARCO-20261108-JTQX"), lista.map { it.folio })
        assertEquals(ID_ALFA, peticion().url.queryParameter("id_paciente"))

        responder(fixture("arco-lista"))
        assertEquals("ARCO-20261108-JTQX", exito(ArcoRemoto(api, backend).consultar("ARCO-20261108-JTQX")).folio)

        responder(fixture("arco-lista"))
        assertEquals(ApiError.Desconocido, error(ArcoRemoto(api, backend).consultar("ARCO-NO-EXISTE")))
    }

    @Test
    fun unaSolicitudConDatosIlegiblesSeOmite() = runTest {
        elegirPacienteAlfa()
        responder("""{"status":"success","data":[{"folio":"A","derecho":"OTRO","fecha_recepcion":"x","respuesta_limite":"x","ejecucion_limite":"x","estado":"RECIBIDA"}]}""")
        assertEquals(emptyList<Any>(), exito(ArcoRemoto(api, backend).solicitudes()))
    }

    private companion object {
        val JSON_ARCO = """{"folio":"ARCO-20261108-JTQX","id_paciente":"fb7d76f1","derecho":"ACCESO","fecha_recepcion":"2026-11-08T00:00:00.000000Z","respuesta_limite":"2026-12-04T00:00:00.000000Z","ejecucion_limite":"2026-12-25T00:00:00.000000Z","estado":"RECIBIDA"}"""
    }
}
