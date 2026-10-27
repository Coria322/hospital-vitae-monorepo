<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Services\BackupService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class Sprint2RespaldosTest extends TestCase
{
    use RefreshDatabase;

    public function test_ejecutar_respaldo_completo_genera_archivo_cifrado_aes_256_y_audita_evento(): void
    {
        Storage::fake('local');
        $service = app(BackupService::class);

        $resultado = $service->ejecutarRespaldoCompleto();

        $this->assertArrayHasKey('archivo', $resultado);
        $this->assertArrayHasKey('huella_sha256', $resultado);
        $this->assertTrue($resultado['regla_321_cumplida']);

        // El archivo en storage debe existir y estar cifrado
        $contenidoCifrado = Storage::disk('local')->get("backups/{$resultado['archivo']}");
        $this->assertStringStartsWith("VENC\x01", $contenidoCifrado);

        // No debe contener texto SQL plano
        $this->assertStringNotContainsString('INSERT INTO', $contenidoCifrado);
        $this->assertStringNotContainsString('paciente', $contenidoCifrado);

        // Debe haberse auditado el evento
        $auditoria = AuditoriaI::where('tipo_movimiento', 'RESPALDO_SISTEMA_EXITOSO')->first();
        $this->assertNotNull($auditoria);
        $this->assertSame('EXITOSO', $auditoria->resultado);
    }

    public function test_verificacion_de_integridad_valida_restauracion_sin_corrupcion(): void
    {
        Storage::fake('local');
        $service = app(BackupService::class);

        $resultado = $service->ejecutarRespaldoCompleto();
        $verificacion = $service->verificarIntegridadRespaldo($resultado['archivo']);

        $this->assertTrue($verificacion['integro']);
        $this->assertSame($resultado['huella_sha256'], $verificacion['huella_sha256']);
        $this->assertGreaterThan(0, $verificacion['lineas_sql']);
    }

    public function test_monitor_de_lag_wal_alerta_cuando_desfase_supera_600_segundos(): void
    {
        $service = app(BackupService::class);

        // Caso 1: Lag óptimo (<= 600s)
        $resultadoOptimo = $service->verificarLagArchivado(lagSimulado: 120);
        $this->assertSame('OPTIMO', $resultadoOptimo['estado']);
        $this->assertFalse($resultadoOptimo['alerta_firing']);

        // Caso 2: Lag crítico (> 600s)
        $resultadoCritico = $service->verificarLagArchivado(lagSimulado: 650);
        $this->assertSame('CRITICO', $resultadoCritico['estado']);
        $this->assertTrue($resultadoCritico['alerta_firing']);

        // Debe registrarse alerta crítica en auditoria_i
        $alerta = AuditoriaI::where('tipo_movimiento', 'ALERTA_LAG_REPLICACION')->first();
        $this->assertNotNull($alerta);
        $this->assertSame('FALLIDO', $alerta->resultado);
        $this->assertTrue((bool)$alerta->es_necesario_auditoria_interna);
    }

    public function test_comandos_artisan_de_respaldo_y_monitoreo_ejecutan_conforme(): void
    {
        Storage::fake('local');

        // 1. Comando de respaldo con JSON
        $exitCodeBackup = Artisan::call('db:backup-seguro', ['--json' => true]);
        $this->assertSame(0, $exitCodeBackup);
        $salidaBackup = json_decode(Artisan::output(), true);
        $this->assertArrayHasKey('archivo', $salidaBackup);

        // 2. Comando de monitoreo óptimo
        $exitCodeLagOptimo = Artisan::call('db:verificar-lag-wal', [
            '--simulate-lag' => '200',
            '--json' => true,
        ]);
        $this->assertSame(0, $exitCodeLagOptimo);

        // 3. Comando de monitoreo crítico (lag > 600s falla con código 1)
        $exitCodeLagCritico = Artisan::call('db:verificar-lag-wal', [
            '--simulate-lag' => '720',
            '--json' => true,
        ]);
        $this->assertSame(1, $exitCodeLagCritico);
    }

    public function test_politica_de_retencion_cumple_con_normativa_30_dias_y_12_meses(): void
    {
        $service = app(BackupService::class);
        $politica = $service->obtenerPoliticaRetencion();

        $this->assertSame(30, $politica['retencion_diaria_dias']);
        $this->assertSame(12, $politica['retencion_mensual_meses']);
        $this->assertSame(3, $politica['regla_321']['copias_totales']);
        $this->assertSame(2, $politica['regla_321']['medios_distintos']);
        $this->assertSame(1, $politica['regla_321']['copia_fuera_sitio']);
        $this->assertLessThanOrEqual(4, $politica['rto_objetivo_horas']);
    }
}
