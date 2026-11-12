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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que el dispositivo recuerda, en memoria para las pruebas. */
private class MemoriaFalsa : MemoriaConsentimientos {
    var aviso: String? = null
    var finalidades: Set<String> = emptySet()

    override fun avisoAceptado(usuario: String) = aviso

    override fun finalidadesAceptadas(usuario: String) = finalidades

    override fun guardar(usuario: String, aviso: String?, finalidades: Set<String>) {
        this.aviso = aviso
        this.finalidades = finalidades
    }
}

/** Consentimientos y ARCO contra el portal móvil del backend (`POST /mobile/consentimientos` y `/mobile/solicitudes-arco`). */
class PrivacidadArcoRemotoTest : BackendTest() {
    private val memoria = MemoriaFalsa()
    private val privacidad get() = PrivacidadRemoto(api, backend, memoria, "Texto del aviso")
    private val arco get() = ArcoRemoto(api, backend)

    private val sinCitasNiRecetas = """{"status":"success","data":[]}"""

    private fun aceptarAviso(vararg finalidades: String) =
        SolicitudConsentimiento(TipoConsentimiento.AVISO_PRIVACIDAD, "1.0", finalidades.toList(), AccionConsentimiento.ACEPTAR)

    // Quién firma y quién es el médico responsable salen del perfil y de la cita más reciente.
    private fun prepararFirma() {
        responder(fixture("mobile-perfil"))
        responder(fixture("mobile-citas"))
    }

    @Test
    fun sinNadaRecordadoElAvisoNoEstaAceptadoYNoSePreguntaAlServidor() = runTest {
        val aviso = exito(privacidad.aviso())

        assertFalse(aviso.aceptada)
        assertEquals("1.0", aviso.version)
        assertEquals("Texto del aviso", aviso.textoSimplificado)
        assertEquals(listOf("RECORDATORIOS", "ESTADISTICA"), aviso.finalidadesSecundarias.map { it.codigo })
        assertTrue(aviso.finalidadesSecundarias.none { it.aceptada })
        // El portal móvil no tiene consulta de consentimientos.
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun elAvisoYLasFinalidadesAceptadasSeLeenDeLaMemoriaDelDispositivo() = runTest {
        memoria.aviso = "1.0"
        memoria.finalidades = setOf("ESTADISTICA")

        val aviso = exito(privacidad.aviso())

        assertTrue(aviso.aceptada)
        assertEquals(listOf(false, true), aviso.finalidadesSecundarias.map { it.aceptada })
    }

    @Test
    fun unaVersionAnteriorDelAvisoNoCuentaComoAceptada() = runTest {
        memoria.aviso = "0.9"
        assertFalse(exito(privacidad.aviso()).aceptada)
    }

    @Test
    fun sinSesionNoHayAvisoNiRegistro() = runTest {
        backend.usuarioId = null
        assertEquals(ApiError.SesionInvalida, error(privacidad.aviso()))
        assertEquals(ApiError.SesionInvalida, error(privacidad.registrar(aceptarAviso())))
    }

    @Test
    fun aceptarElAvisoManda_unPost_conFirmanteYMedicoResponsableSinIdPaciente() = runTest {
        prepararFirma()
        responder(fixture("mobile-consentimiento"), 201)

        val consentimiento = exito(privacidad.registrar(aceptarAviso()))

        assertEquals("01a1c2ef-fe85-7033-8292-29f83006239f", consentimiento.id)
        assertEquals("/api/v1/mobile/perfil", peticion().url.encodedPath)
        assertEquals("/api/v1/mobile/citas", peticion().url.encodedPath)
        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/mobile/consentimientos", p.url.encodedPath)
        assertEquals(
            """{"tipo":"AVISO_PRIVACIDAD","finalidades":"Aviso de privacidad simplificado v1.0","firmante_nombre":"Paciente Sintético Alfa","medico_responsable_id":"MED-001"}""",
            p.body?.utf8(),
        )
        assertEquals("1.0", memoria.aviso)
        assertEquals(emptySet<String>(), memoria.finalidades)
    }

    @Test
    fun aceptarConFinalidadesOpcionalesCreaUnaSegundaFilaYLasRecuerda() = runTest {
        prepararFirma()
        responder(fixture("mobile-consentimiento"), 201)
        responder(fixture("mobile-consentimiento"), 201)

        exito(privacidad.registrar(aceptarAviso("RECORDATORIOS", "ESTADISTICA")))

        repeat(2) { peticion() } // perfil y citas
        assertTrue(peticion().body!!.utf8().contains(""""tipo":"AVISO_PRIVACIDAD""""))
        val secundarias = peticion().body!!.utf8()
        assertTrue(secundarias, secundarias.contains(""""tipo":"FINALIDADES_SECUNDARIAS""""))
        assertTrue(secundarias, secundarias.contains("Finalidades opcionales | SECUNDARIAS: ESTADISTICA,RECORDATORIOS"))
        assertEquals(setOf("RECORDATORIOS", "ESTADISTICA"), memoria.finalidades)
    }

    @Test
    fun aceptarSoloFinalidadesNoTocaElAviso() = runTest {
        memoria.aviso = "1.0"
        prepararFirma()
        responder(fixture("mobile-consentimiento"), 201)

        exito(privacidad.registrar(SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "1.0", listOf("ESTADISTICA"), AccionConsentimiento.ACEPTAR)))

        assertEquals("1.0", memoria.aviso)
        assertEquals(setOf("ESTADISTICA"), memoria.finalidades)
        assertEquals(3, servidor.requestCount)
    }

    @Test
    fun sinCitasElMedicoResponsableSaleDeLaPrimeraReceta() = runTest {
        responder(fixture("mobile-perfil"))
        responder(sinCitasNiRecetas)
        responder(fixture("mobile-recetas"))
        responder(fixture("mobile-consentimiento"), 201)

        exito(privacidad.registrar(aceptarAviso()))

        repeat(3) { peticion() }
        assertTrue(peticion().body!!.utf8().contains(""""medico_responsable_id":"MED-001""""))
    }

    // El servidor exige un médico responsable existente: sin cita ni receta no hay a quién nombrar y nada se registra.
    @Test
    fun sinCitasNiRecetasNoSePuedeRegistrar() = runTest {
        responder(fixture("mobile-perfil"))
        responder(sinCitasNiRecetas)
        responder(sinCitasNiRecetas)

        assertEquals(ApiError.NoDisponible, error(privacidad.registrar(aceptarAviso())))

        assertEquals("solo se leyeron el perfil, las citas y las recetas", 3, servidor.requestCount)
        assertNull(memoria.aviso)
    }

    @Test
    fun siElServidorRechazaElRegistroNoSeRecuerdaNada() = runTest {
        prepararFirma()
        responder("""{"message":"The finalidades field must be at least 10 characters."}""", 422)

        assertEquals(ApiError.Desconocido, error(privacidad.registrar(aceptarAviso())))

        assertNull(memoria.aviso)
    }

    // El portal móvil no tiene revocación: no se simula ni se escribe nada.
    @Test
    fun revocarNoEstaDisponible() = runTest {
        memoria.finalidades = setOf("ESTADISTICA")
        val revocar = SolicitudConsentimiento(TipoConsentimiento.FINALIDADES_SECUNDARIAS, "1.0", listOf("ESTADISTICA"), AccionConsentimiento.REVOCAR)

        assertEquals(ApiError.NoDisponible, error(privacidad.registrar(revocar)))

        assertEquals(0, servidor.requestCount)
        assertEquals(setOf("ESTADISTICA"), memoria.finalidades)
    }

    @Test
    fun crearUnaSolicitudArcoManda_derechoYDescripcion_yLaRecuerdaEnLaSesion() = runTest {
        responder(fixture("mobile-arco"), 201)

        val solicitud = exito(arco.crear(DerechoArco.Acceso))

        assertEquals("ARCO-MOV-7236876A", solicitud.folio)
        assertEquals(DerechoArco.Acceso, solicitud.derecho)
        assertEquals(LocalDate.of(2026, 11, 10), solicitud.fechaRecepcion)
        assertEquals(LocalDate.of(2026, 12, 8), solicitud.respuestaLimite)
        assertEquals(LocalDate.of(2026, 12, 29), solicitud.ejecucionLimite)
        assertEquals("RECIBIDA", solicitud.estado)
        val p = peticion()
        assertEquals("POST", p.method)
        assertEquals("/api/v1/mobile/solicitudes-arco", p.url.encodedPath)
        val cuerpo = p.body!!.utf8()
        assertTrue(cuerpo, cuerpo.startsWith("""{"derecho":"ACCESO","descripcion_peticion":"""))
        assertFalse("sin id_paciente ni acreditación: el portal móvil no los pide", cuerpo.contains("id_paciente") || cuerpo.contains("acreditacion"))
        assertEquals(listOf(solicitud), exito(arco.solicitudes()))
        assertEquals(solicitud, exito(arco.consultar("ARCO-MOV-7236876A")))
    }

    // El servidor pide al menos 15 caracteres de descripción y solo acepta el derecho en mayúsculas.
    @Test
    fun todosLosDerechosViajanEnMayusculasConUnaDescripcionSuficiente() = runTest {
        DerechoArco.entries.forEach { derecho ->
            responder(fixture("mobile-arco"), 201)
            exito(arco.crear(derecho))
            val cuerpo = peticion().body!!.utf8()
            assertTrue(cuerpo, cuerpo.contains(""""derecho":"${derecho.name.uppercase()}""""))
            val descripcion = Regex(""""descripcion_peticion":"([^"]*)"""").find(cuerpo)!!.groupValues[1]
            assertTrue("$derecho: ${descripcion.length}", descripcion.length >= 15)
        }
    }

    @Test
    fun noHayConsultaDeSolicitudesAjenasNiDeFoliosDesconocidos() = runTest {
        assertEquals(emptyList<Any>(), exito(arco.solicitudes()))
        assertEquals(ApiError.NoDisponible, error(arco.consultar("ARCO-DESCONOCIDO")))
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun siElServidorRechazaLaSolicitudNoQuedaNadaEnLaSesion() = runTest {
        responder("""{"message":"The selected derecho is invalid.","errors":{"derecho":["The selected derecho is invalid."]}}""", 422)
        assertEquals(ApiError.Desconocido, error(arco.crear(DerechoArco.Oposicion)))

        responder("""{"status":"error","code":"FORBIDDEN_ROLE"}""", 403)
        assertEquals(ApiError.SinPermisos, error(arco.crear(DerechoArco.Oposicion)))

        assertEquals(emptyList<Any>(), backend.arcoEnviadas.toList())
    }
}
