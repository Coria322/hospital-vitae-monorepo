<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Services\NtpService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Artisan;
use Tests\TestCase;

class Sprint2SincronizacionHorariaTest extends TestCase
{
    use RefreshDatabase;

    public function test_ntp_service_retorna_estructura_conforme_y_en_utc(): void
    {
        $service = new NtpService();
        $resultado = $service->medirDerivaHoraria(timeout: 1.5);

        $this->assertIsArray($resultado);
        $this->assertArrayHasKey('sincronizado', $resultado);
        $this->assertArrayHasKey('offset_segundos', $resultado);
        $this->assertArrayHasKey('offset_ms', $resultado);
        $this->assertArrayHasKey('fuente', $resultado);
        $this->assertArrayHasKey('metodo', $resultado);
        $this->assertArrayHasKey('hora_local_utc', $resultado);
        $this->assertArrayHasKey('hora_referencia_utc', $resultado);
        $this->assertArrayHasKey('deriva_conforme', $resultado);

        // La marca horaria local debe terminar con 'Z' o '+00:00' (formato UTC)
        $this->assertTrue(
            str_ends_with($resultado['hora_local_utc'], 'Z') || str_ends_with($resultado['hora_local_utc'], '+00:00')
        );
    }

    public function test_comando_verificar_sincronizacion_retorna_exito_con_tolerancia_adecuada(): void
    {
        // Con una tolerancia suficiente (ej. 10s) para entornos de test locales
        $exitCode = Artisan::call('ntp:verificar-sincronizacion', [
            '--max-offset' => '10.0',
            '--json' => true,
        ]);

        $this->assertSame(0, $exitCode);

        $output = json_decode(Artisan::output(), true);
        $this->assertIsArray($output);
        $this->assertTrue($output['cumple_normativa_cenam']);
        $this->assertArrayHasKey('timestamp_ejecucion_utc', $output);
    }

    public function test_comando_alerta_y_registra_auditoria_cuando_deriva_supera_tolerancia(): void
    {
        // Forzar tolerancia de 0.0000001 segundos para provocar desincronización deliberada
        $exitCode = Artisan::call('ntp:verificar-sincronizacion', [
            '--max-offset' => '0.00000001',
        ]);

        $this->assertSame(1, $exitCode);

        // Debe haberse registrado el evento de seguridad en auditoria_i
        $registroAuditoria = AuditoriaI::where('tipo_movimiento', 'ALERTA_DESINCRONIZACION_HORARIA')->first();
        $this->assertNotNull($registroAuditoria, 'Debe registrarse evento en auditoria_i ante desincronización horaria');
        $this->assertSame('FALLIDO', $registroAuditoria->resultado);
        $this->assertSame('infraestructura', $registroAuditoria->tabla_afectada);
        $this->assertTrue((bool)$registroAuditoria->es_necesario_auditoria_interna);
    }

    public function test_configuracion_global_de_laravel_esta_estrictamente_en_utc(): void
    {
        $timezoneApp = config('app.timezone');
        $this->assertSame('UTC', $timezoneApp, 'La aplicación Laravel debe operar en UTC');

        $timezonePgsql = config('database.connections.pgsql.timezone');
        $this->assertSame('UTC', $timezonePgsql, 'La conexión PostgreSQL debe forzar zona horaria UTC');
    }

    public function test_endpoint_sistema_tiempo_retorna_diagnostico_exitoso_en_utc(): void
    {
        $response = $this->getJson('/api/v1/sistema/tiempo');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'status',
                'timezone',
                'data' => [
                    'sincronizado',
                    'offset_segundos',
                    'offset_ms',
                    'fuente',
                    'metodo',
                    'hora_local_utc',
                    'hora_referencia_utc',
                    'deriva_conforme',
                ],
            ]);

        $this->assertSame('UTC', $response->json('timezone'));
        $this->assertTrue($response->json('data.sincronizado'));
    }
}
