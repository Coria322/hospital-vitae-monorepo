<?php

namespace Tests\Feature;

use App\Services\CifradoAdjuntosService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use RuntimeException;
use Tests\TestCase;

class Sprint2SeguridadRedYReposoTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Valida que las cabeceras de transporte seguro y HSTS >= 1 año se inyecten en las respuestas HTTP (RNF-003 / TASK-W-028).
     */
    public function test_respuestas_http_incluyen_cabeceras_hsts_y_seguridad_estricta(): void
    {
        $response = $this->getJson('/api/ping');

        $response->assertStatus(200);

        // HSTS >= 1 año (31536000 segundos) con preload y subdominios
        $hsts = $response->headers->get('Strict-Transport-Security');
        $this->assertNotNull($hsts);
        $this->assertStringContainsString('max-age=31536000', $hsts);
        $this->assertStringContainsString('includeSubDomains', $hsts);

        // Cabeceras defensivas contra ataques web
        $this->assertSame('nosniff', $response->headers->get('X-Content-Type-Options'));
        $this->assertSame('DENY', $response->headers->get('X-Frame-Options'));
        $this->assertStringContainsString("frame-ancestors 'none'", $response->headers->get('Content-Security-Policy'));
        $this->assertSame('strict-origin-when-cross-origin', $response->headers->get('Referrer-Policy'));
    }

    /**
     * Valida que el servicio cifre documentos clínicos con AES-256-GCM y que el blob no revele contenido plano (TASK-W-027).
     */
    public function test_cifrado_aes_256_gcm_protege_adjuntos_clinicos_sin_fuga_de_texto_plano(): void
    {
        $servicio = new CifradoAdjuntosService();

        // Simulación de documento médico confidencial con magic bytes PDF
        $documentoOriginal = "%PDF-1.7\n1 0 obj\n<< /Title (Receta Medica Confidencial) /Paciente (Juan Perez) >>\nendobj\n%%EOF";

        $blobCifrado = $servicio->cifrar($documentoOriginal);

        // 1. Debe iniciar con el identificador VENC\x01
        $this->assertStringStartsWith("VENC\x01", $blobCifrado);

        // 2. El blob cifrado no debe contener ninguna de las cadenas originales (cero coincidencias)
        $this->assertStringNotContainsString('%PDF', $blobCifrado);
        $this->assertStringNotContainsString('Receta', $blobCifrado);
        $this->assertStringNotContainsString('Juan Perez', $blobCifrado);
        $this->assertStringNotContainsString('Confidencial', $blobCifrado);

        // 3. Al descifrar con la llave correcta, el contenido es idéntico byte por byte
        $documentoDescifrado = $servicio->descifrar($blobCifrado);
        $this->assertSame($documentoOriginal, $documentoDescifrado);
    }

    /**
     * Valida que la integridad del blob sea verificada por el Authentication Tag de GCM ante alteraciones.
     */
    public function test_descifrado_falla_ante_tampering_o_modificacion_de_bytes(): void
    {
        $servicio = new CifradoAdjuntosService();
        $documentoOriginal = "Diagnostico Confidencial: Paciente estable con tratamiento antibiotico.";

        $blobCifrado = $servicio->cifrar($documentoOriginal);

        // Alterar un byte en el cuerpo del ciphertext
        $posicionAlterada = strlen($blobCifrado) - 2;
        $blobCorrupto = $blobCifrado;
        $blobCorrupto[$posicionAlterada] = chr(ord($blobCorrupto[$posicionAlterada]) ^ 0xFF);

        $this->expectException(RuntimeException::class);
        $this->expectExceptionMessageMatches('/descifrado fallido/i');

        $servicio->descifrar($blobCorrupto);
    }

    /**
     * Valida el almacenamiento y recuperación transparente en disco seguro (Storage).
     */
    public function test_almacenamiento_y_recuperacion_de_adjunto_cifrado_en_storage(): void
    {
        Storage::fake('seguro');
        $servicio = new CifradoAdjuntosService();

        $ruta = 'expedientes/2026/receta_exp123.pdf.enc';
        $contenido = "%PDF-1.5 Datos Clinicos del Expediente No. EXP-10029";

        $guardado = $servicio->almacenarAdjuntoCifrado($ruta, $contenido, disco: 'seguro');
        $this->assertTrue($guardado);

        // Verificar que en disco esté cifrado
        $contenidoEnDisco = Storage::disk('seguro')->get($ruta);
        $this->assertStringNotContainsString('EXP-10029', $contenidoEnDisco);
        $this->assertStringNotContainsString('%PDF', $contenidoEnDisco);

        // Recuperar y descifrar
        $recuperado = $servicio->obtenerAdjuntoDescifrado($ruta, disco: 'seguro');
        $this->assertSame($contenido, $recuperado);
    }

    /**
     * Valida que la conexión de base de datos PostgreSQL obligue a TLS mediante sslmode=require (TASK-W-021).
     */
    public function test_conexion_postgresql_fuerza_cifrado_tls_con_sslmode_require(): void
    {
        $sslmode = config('database.connections.pgsql.sslmode');
        $this->assertSame('require', $sslmode, 'La conexión PostgreSQL debe requerir TLS (sslmode=require)');
    }
}
