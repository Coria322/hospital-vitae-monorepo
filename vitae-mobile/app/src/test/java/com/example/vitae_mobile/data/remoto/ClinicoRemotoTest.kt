package com.example.vitae_mobile.data.remoto

import com.example.vitae_mobile.core.ApiError
import com.example.vitae_mobile.data.perfil.Perfil
import com.example.vitae_mobile.data.recetas.EstadoReceta
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Perfil, citas, recetas y resultados, con respuestas reales del backend. */
class ClinicoRemotoTest : BackendTest() {
    private val directorio = createTempDirectory("resultados").toFile()

    @Test
    fun elPerfilSaleDelPatientFhirYElSeguroDelPaciente() = runTest {
        elegirPacienteAlfa()
        responder(fixture("fhir-patient"))

        val perfil = exito(PerfilRemoto(api, backend).perfil())

        // El Patient FHIR del backend no trae el seguro: lo aporta el detalle del paciente.
        assertEquals(Perfil("Paciente Sintético Alfa", "SINT900101HDFR01", "1990-01-01", true), perfil)
        val p = peticion()
        assertEquals("/api/v1/fhir/Patient/$ID_ALFA", p.url.encodedPath)
        assertEquals("Bearer TOKEN", p.headers["Authorization"])
    }

    @Test
    fun elPerfilSinPacienteElegidoOConFhirInvalidoFalla() = runTest {
        assertEquals(ApiError.Desconocido, error(PerfilRemoto(api, backend).perfil()))

        elegirPacienteAlfa()
        responder("""{"resourceType":"OperationOutcome","issue":[]}""")
        assertEquals(ApiError.Desconocido, error(PerfilRemoto(api, backend).perfil()))

        responder("""{"resourceType":"OperationOutcome"}""", 404)
        assertEquals(ApiError.Desconocido, error(PerfilRemoto(api, backend).perfil()))
    }

    // El backend no tiene agenda: las citas no se inventan.
    @Test
    fun lasCitasNoEstanDisponibles() = runTest {
        assertEquals(ApiError.NoDisponible, error(CitasRemoto().citas()))
    }

    @Test
    fun lasRecetasSalenDelExpedienteConSuEstadoYSuQr() = runTest {
        elegirPacienteAlfa()
        responder(fixture("expediente-detalle"))

        val recetas = exito(RecetasRemoto(api, backend).recetas())

        assertEquals("/api/v1/expedientes-clinicos/EXP-SINT01", peticion().url.encodedPath)
        assertEquals(3, recetas.size)
        val sinFirmar = recetas.single { it.id == "RC94158974" }
        // Sin firmar: el estado es vigente pero todavía no hay QR.
        assertEquals(EstadoReceta.Activa, sinFirmar.estado)
        assertNull(sinFirmar.codigoQr)
        assertFalse(sinFirmar.qrVigente)
        assertEquals("Ibuprofeno 400 mg tabletas", sinFirmar.medicamento)
        assertEquals("1 tableta cada 12 horas por 3 dias", sinFirmar.dosis)

        val vigente = recetas.single { it.id == "RC94158939" }
        assertEquals(EstadoReceta.Activa, vigente.estado)
        assertTrue(vigente.qrVigente)
        assertEquals(64, vigente.codigoQr?.length)

        val surtida = recetas.single { it.id == "RC94158973" }
        assertEquals(EstadoReceta.Dispensada, surtida.estado)
        assertFalse("un QR consumido no es válido", surtida.qrVigente)
    }

    @Test
    fun elDetalleDeUnaRecetaUsaGetRecetas() = runTest {
        responder(fixture("receta-detalle"))
        val vigente = exito(RecetasRemoto(api, backend).receta("RC94158939"))
        assertEquals("/api/v1/recetas/RC94158939", peticion().url.encodedPath)
        assertEquals("Paracetamol 500 mg tabletas", vigente.medicamento)
        assertTrue(vigente.qrVigente)

        responder(fixture("receta-detalle-consumida"))
        val surtida = exito(RecetasRemoto(api, backend).receta("RC94158973"))
        assertEquals(EstadoReceta.Dispensada, surtida.estado)
        assertFalse(surtida.qrVigente)
    }

    // Solo el médico dueño del expediente (o con acceso de emergencia) lo ve: otro usuario recibe 403.
    @Test
    fun unExpedienteAjenoDaSinPermisos() = runTest {
        elegirPacienteAlfa()
        responder("""{"status":"error","code":"BREAK_THE_GLASS_REQUIRED"}""", 403)
        assertEquals(ApiError.SinPermisos, error(RecetasRemoto(api, backend).recetas()))
    }

    @Test
    fun unPacienteSinExpedienteNoTieneRecetasNiResultados() = runTest {
        responder("""{"status":"success","data":{"id_paciente":"p-1","nombre_completo":"Sin Expediente","expediente":null}}""")
        exito(backend.elegir("p-1"))
        servidor.takeRequest()
        val antes = servidor.requestCount

        assertEquals(emptyList<Any>(), exito(RecetasRemoto(api, backend).recetas()))
        assertEquals(emptyList<Any>(), exito(ResultadosRemoto(api, backend, directorio).resultados()))
        assertEquals("sin expediente no se pregunta nada al servidor", antes, servidor.requestCount)
    }

    @Test
    fun losResultadosSonLasSolicitudesQueYaTienenResultado() = runTest {
        elegirPacienteAlfa()
        responder(fixture("expediente-detalle"))

        val resultados = exito(ResultadosRemoto(api, backend, directorio).resultados())

        // La solicitud pendiente (radiografía) no aparece: aún no hay nada que ver.
        assertEquals(1, resultados.size)
        val biometria = resultados.single()
        assertEquals("01a1bc34-d0ad-70c1-9f05-242686ea2472", biometria.id)
        assertEquals("Biometria hematica", biometria.tipoEstudio)
        assertEquals("text/plain", biometria.mime)
        assertTrue(biometria.esTexto)
        assertEquals("2026-11-08T17:29:00Z", biometria.fechaHora.toString())
    }

    @Test
    fun elArchivoSeGuardaEnElDirectorioInternoConSuTipo() = runTest {
        responder(fixture("resultado-detalle"))
        var avance = 0f

        val archivo = exito(ResultadosRemoto(api, backend, directorio).archivo("01a1bc34-d0ad-70c1-9f05-242686ea2472") { avance = it })

        assertEquals("/api/v1/solicitudes-estudio/01a1bc34-d0ad-70c1-9f05-242686ea2472/resultados", peticion().url.encodedPath)
        assertEquals("text/plain", archivo.mime)
        assertEquals(directorio, archivo.archivo.parentFile)
        assertEquals("BIOMETRIA HEMATICA: hemoglobina 14.2 g/dL, leucocitos 6.8 x10^3/uL. Valores dentro de rango.", archivo.archivo.readText())
        assertEquals(1f, avance)
    }

    @Test
    fun unResultadoSinContenidoOAjenoFalla() = runTest {
        responder("""{"status":"success","data":{"resultado":{"mime":"application/pdf","fecha_hora":"2026-11-08T17:29:00.000000Z"},"contenido_descifrado":null}}""")
        assertEquals(ApiError.Desconocido, error(ResultadosRemoto(api, backend, directorio).archivo("x") {}))

        responder("""{"status":"error"}""", 403)
        assertEquals(ApiError.SinPermisos, error(ResultadosRemoto(api, backend, directorio).archivo("x") {}))
    }
}
