package com.example.vitae_mobile.ui.recetas

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrTest {
    @Test
    fun laMatrizEsCuadradaYTieneModulosOscuros() {
        val matriz = matrizQr("VITAE-DEMO-rec-001")
        assertEquals(matriz.width, matriz.height)
        assertTrue(matriz.width >= 21) // el QR más pequeño (versión 1) es de 21 módulos, más el margen
        val oscuros = (0 until matriz.height).sumOf { y -> (0 until matriz.width).count { x -> matriz[x, y] } }
        assertTrue(oscuros > 0)
    }

    @Test
    fun textosDistintosDanCodigosDistintos() {
        assertNotEquals(matrizQr("VITAE-DEMO-rec-001").toString(), matrizQr("VITAE-DEMO-rec-002").toString())
    }

    @Test
    fun elMismoTextoDaSiempreElMismoCodigo() {
        assertEquals(matrizQr("abc").toString(), matrizQr("abc").toString())
    }

    // El QR que dibuja la app debe poder leerlo un lector: se pinta la matriz como imagen y se decodifica.
    @Test
    fun elQrSeLeeDeVueltaConElMismoTexto() {
        listOf("VITAE-DEMO-rec-001", "VITAE-DEMO-rec-002", "FOLIO-0001-ÑÁÉ").forEach { texto ->
            val matriz = matrizQr(texto)
            val escala = 8
            val lado = matriz.width * escala
            val pixeles = IntArray(lado * lado) { i ->
                if (matriz[i % lado / escala, i / lado / escala]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
            val lectura = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(lado, lado, pixeles))))
            assertEquals(texto, lectura.text)
        }
    }

    // Sin fracciones de píxel no hay costuras entre módulos al dibujar el QR.
    @Test
    fun losModulosSeDibujanConTamanoYDesfaseEnteros() {
        listOf(240f to 25, 720f to 29, 600f to 33, 101f to 25).forEach { (lado, modulos) ->
            val (modulo, desfase) = disposicionQr(lado, modulos)
            assertEquals("módulo entero para $lado/$modulos", modulo, kotlin.math.floor(modulo))
            assertEquals("desfase entero para $lado/$modulos", desfase, kotlin.math.floor(desfase))
            assertTrue("cabe en el lienzo", desfase * 2 + modulo * modulos <= lado)
            assertTrue("desfase no negativo", desfase >= 0f)
        }
    }

    @Test
    fun conUnLienzoMuyPequenoElModuloNuncaEsCero() {
        assertEquals(1f, disposicionQr(10f, 25).first)
    }
}
