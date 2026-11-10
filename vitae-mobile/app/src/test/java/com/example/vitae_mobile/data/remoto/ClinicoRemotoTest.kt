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

/** Perfil, citas, recetas y resultados del portal móvil, con respuestas reales del backend. */
class ClinicoRemotoTest : BackendTest() {
    private val directorio = createTempDirectory("resultados").toFile()

    @Test
    fun elPerfilSaleDeMobilePerfilSinMandarNingunId() = runTest {
        responder(fixture("mobile-perfil"))

        val perfil = exito(PerfilRemoto(api).perfil())

        assertEquals(Perfil("Paciente Sintético Alfa", "SINT900101HDFR01", "1990-01-01", true), perfil)
        val p = peticion()
        assertEquals("/api/v1/mobile/perfil", p.url.encodedPath)
        assertNull("la identidad sale del token, no de la URL", p.url.query)
        assertEquals("Bearer TOKEN", p.headers["Authorization"])
    }

    @Test
    fun unPerfilIlegibleOUnErrorFalla() = runTest {
        responder("""{"status":"success"}""")
        assertEquals(ApiError.Desconocido, error(PerfilRemoto(api).perfil()))

        responder("""{"status":"error","code":"FORBIDDEN_ROLE"}""", 403)
        assertEquals(ApiError.SinPermisos, error(PerfilRemoto(api).perfil()))
    }

    @Test
    fun lasCitasSalenDeMobileCitas() = runTest {
        responder(fixture("mobile-citas"))

        val citas = exito(CitasRemoto(api).citas())

        assertEquals("/api/v1/mobile/citas", peticion().url.encodedPath)
        assertEquals(2, citas.size)
        assertEquals("a0000000-0000-0000-0000-000000000001", citas[0].id)
        assertEquals("CONFIRMADA", citas[0].estado)
        assertEquals("2026-11-13T10:00:00Z", citas[0].fechaHora.toString())
        assertEquals("PENDIENTE", citas[1].estado)
    }

    @Test
    fun unaCitaConFechaIlegibleSeOmite() = runTest {
        responder("""{"status":"success","data":[{"id_cita":"c1","fecha_hora":"mañana","estado":"PENDIENTE"},{"id_cita":"c2","fecha_hora":"2026-11-13T10:00:00.000000Z","estado":"PENDIENTE"}]}""")
        assertEquals(listOf("c2"), exito(CitasRemoto(api).citas()).map { it.id })
    }

    @Test
    fun lasRecetasTraenSuEstadoYSuQr() = runTest {
        responder(fixture("mobile-recetas"))

        val recetas = exito(RecetasRemoto(api).recetas())

        assertEquals("/api/v1/mobile/recetas", peticion().url.encodedPath)
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

    // El portal móvil no tiene detalle de receta: la lista ya trae todo y el detalle sale de ella.
    @Test
    fun elDetalleDeUnaRecetaSaleDeLaLista() = runTest {
        responder(fixture("mobile-recetas"))
        assertEquals("Paracetamol 500 mg tabletas", exito(RecetasRemoto(api).receta("RC94158939")).medicamento)
        assertEquals("/api/v1/mobile/recetas", peticion().url.encodedPath)

        responder(fixture("mobile-recetas"))
        assertEquals(ApiError.Desconocido, error(RecetasRemoto(api).receta("no-existe")))
    }

    @Test
    fun losResultadosSalenDeMobileResultados() = runTest {
        responder(fixture("mobile-resultados"))

        val resultados = exito(ResultadosRemoto(api, directorio).resultados())

        assertEquals("/api/v1/mobile/resultados", peticion().url.encodedPath)
        assertEquals(1, resultados.size)
        val biometria = resultados.single()
        // El id es el de la solicitud de estudio: con él se pide el contenido.
        assertEquals("01a1bc34-d0ad-70c1-9f05-242686ea2472", biometria.id)
        assertEquals("Biometria hematica", biometria.tipoEstudio)
        assertEquals("text/plain", biometria.mime)
        assertTrue(biometria.esTexto)
        assertEquals("2026-11-08T17:29:00Z", biometria.fechaHora.toString())
    }

    @Test
    fun unPacienteSinResultadosNiRecetasRecibeListasVacias() = runTest {
        responder("""{"status":"success","data":[]}""")
        responder("""{"status":"success","data":[]}""")

        assertEquals(emptyList<Any>(), exito(RecetasRemoto(api).recetas()))
        assertEquals(emptyList<Any>(), exito(ResultadosRemoto(api, directorio).resultados()))
    }

    // Un token de personal recibe 403 en todo el portal móvil.
    @Test
    fun unTokenDePersonalDaSinPermisos() = runTest {
        responder("""{"status":"error","code":"FORBIDDEN_ROLE"}""", 403)
        assertEquals(ApiError.SinPermisos, error(RecetasRemoto(api).recetas()))
    }

    @Test
    fun elArchivoSeGuardaEnElDirectorioInternoConSuTipo() = runTest {
        responder(fixture("resultado-detalle"))
        var avance = 0f

        val archivo = exito(ResultadosRemoto(api, directorio).archivo("01a1bc34-d0ad-70c1-9f05-242686ea2472") { avance = it })

        // Única lectura fuera de /mobile (decisión 19): el portal móvil no entrega el contenido.
        assertEquals("/api/v1/solicitudes-estudio/01a1bc34-d0ad-70c1-9f05-242686ea2472/resultados", peticion().url.encodedPath)
        assertEquals("text/plain", archivo.mime)
        assertEquals(directorio, archivo.archivo.parentFile)
        assertEquals("BIOMETRIA HEMATICA: hemoglobina 14.2 g/dL, leucocitos 6.8 x10^3/uL. Valores dentro de rango.", archivo.archivo.readText())
        assertEquals(1f, avance)
    }

    @Test
    fun unResultadoSinContenidoOAjenoFalla() = runTest {
        responder("""{"status":"success","data":{"resultado":{"mime":"application/pdf"},"contenido_descifrado":null}}""")
        assertEquals(ApiError.Desconocido, error(ResultadosRemoto(api, directorio).archivo("x") {}))

        responder("""{"status":"error"}""", 403)
        assertEquals(ApiError.SinPermisos, error(ResultadosRemoto(api, directorio).archivo("x") {}))
    }
}
