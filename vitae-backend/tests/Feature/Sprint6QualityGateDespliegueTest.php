<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Models\RfcCambio;
use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\File;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class Sprint6QualityGateDespliegueTest extends TestCase
{
    use RefreshDatabase;

    private Usuario $admin;
    private Usuario $medico;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(\Database\Seeders\UsuarioSeeder::class);

        $this->admin = Usuario::where('id_usuario', 'ADM-001')->firstOrFail();
        $this->medico = Usuario::where('id_usuario', 'MED-001')->firstOrFail();
    }

    /**
     * PROV-RNF-003: Comando deploy:rollback ejecuta reversión en <= 5 minutos y audita en AUDITORIA_I.
     */
    public function test_prov_rnf003_comando_deploy_rollback_certifica_rto_y_audita(): void
    {
        $exitCode = Artisan::call('deploy:rollback', [
            '--target-version' => 'v1.5.0',
            '--timeout' => 300,
            '--dry-run' => true,
        ]);

        $this->assertSame(0, $exitCode);

        $output = Artisan::output();
        $this->assertStringContainsString('Protocolo de Rollback Automatizado', $output);
        $this->assertStringContainsString('EXITOSO', $output);

        $audit = AuditoriaI::where('tipo_movimiento', 'ROLLBACK_DESPLIEGUE')
            ->orderBy('id_auditoria', 'desc')
            ->first();

        $this->assertNotNull($audit);
        $this->assertSame('sistema_despliegue', $audit->tabla_afectada);
    }

    /**
     * PROV-RN-011: Comando release:quality-gate evalúa las 4 dimensiones ISO-25010 y genera acta firmada.
     */
    public function test_prov_rn011_quality_gate_evalua_dimensiones_y_genera_acta(): void
    {
        $actaPath = storage_path('app/release/ACTA_QUALITY_GATE_AI06.json');
        File::delete($actaPath);

        $exitCode = Artisan::call('release:quality-gate', ['--strict' => true]);
        $this->assertSame(0, $exitCode);

        $this->assertFileExists($actaPath);
        $acta = json_decode(File::get($actaPath), true);

        $this->assertSame('APROBADO', $acta['resultado_global']);
        $this->assertNotEmpty($acta['checksum_acta_sha256']);
        $this->assertCount(3, $acta['firmas_digitales']);

        $audit = AuditoriaI::where('tipo_movimiento', 'EVALUACION_QUALITY_GATE')
            ->orderBy('id_auditoria', 'desc')
            ->first();

        $this->assertNotNull($audit);
        $this->assertSame('sistema_liberacion', $audit->tabla_afectada);
    }

    /**
     * PROV-RN-011: Endpoint de Quality Gate solo para ADMINISTRADOR y rechaza roles no autorizados.
     */
    public function test_prov_rn011_endpoint_quality_gate_control_acceso_rbac(): void
    {
        // 1. Médicos son rechazados con 403
        Sanctum::actingAs($this->medico, ['*']);
        $respForbidden = $this->getJson('/api/v1/admin/quality-gate/status');
        $respForbidden->assertStatus(403)
            ->assertJsonPath('status', 'error');

        // 2. Administrador autorizado recibe 200 y acta
        Sanctum::actingAs($this->admin, ['*']);
        $respAdmin = $this->getJson('/api/v1/admin/quality-gate/status');
        $respAdmin->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.resultado_global', 'APROBADO');
    }

    /**
     * PROV-RNF-004: Comando ops:capacity-monitor mide recursos y audita telemetría BCP/DRP.
     */
    public function test_prov_rnf004_comando_monitoreo_capacidad_y_telemetria_bcp_drp(): void
    {
        $exitCode = Artisan::call('ops:capacity-monitor', ['--umbral' => 99]);
        $this->assertSame(0, $exitCode);

        $output = Artisan::output();
        $this->assertStringContainsString('Monitoreo Proactivo de Capacidad', $output);
        $this->assertStringContainsString('BCP/DRP Failover', $output);
    }

    /**
     * PROV-RNF-004: Endpoint de capacidad para administradores retorna métricas y estado BCP/DRP.
     */
    public function test_prov_rnf004_endpoint_capacidad_retorna_metricas_completas(): void
    {
        Sanctum::actingAs($this->admin, ['*']);

        $response = $this->getJson('/api/v1/admin/infraestructura/capacidad?umbral=85');
        $response->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.metricas.bcp_drp.rto_objetivo_horas', 4)
            ->assertJsonPath('data.metricas.bcp_drp.rpo_objetivo_minutos', 15)
            ->assertJsonStructure([
                'status',
                'data' => [
                    'umbral_alerta_porcentaje',
                    'alerta_capacidad',
                    'metricas' => [
                        'disco',
                        'memoria',
                        'base_datos',
                        'bcp_drp',
                    ],
                    'fecha_consulta_utc',
                ],
            ]);
    }

    /**
     * PROV-RN-012: Creación de RFC post-liberación exige plan de rollback y calcula SLA según severidad.
     */
    public function test_prov_rn012_creacion_rfc_con_severidad_p1_p2_p3_y_plan_rollback(): void
    {
        Sanctum::actingAs($this->admin, ['*']);

        // 1. Falla si falta plan de rollback
        $invalido = $this->postJson('/api/v1/admin/rfcs', [
            'titulo' => 'Ajuste de índices en colas',
            'descripcion' => 'Optimización de consultas urgencias',
            'severidad' => 'P1',
            'modulo' => 'triage',
            'impacto_clinico' => 'Ninguno en tiempo real',
            'analisis_riesgo' => 'Riesgo bajo',
            // 'plan_rollback' faltante
        ]);
        $invalido->assertStatus(422)
            ->assertJsonValidationErrors(['plan_rollback']);

        // 2. Creación exitosa de RFC severidad P1 (SLA 2 horas)
        $valido = $this->postJson('/api/v1/admin/rfcs', [
            'titulo' => 'Actualización de índices en colas de urgencias',
            'descripcion' => 'Optimización de consultas concurrentes para salas rojas',
            'severidad' => 'P1',
            'modulo' => 'triage',
            'impacto_clinico' => 'Mejora en tiempos de respuesta de colas',
            'analisis_riesgo' => 'Bloqueo transitorio en tabla si no es concurrente',
            'plan_rollback' => 'Reversión con DROP INDEX CONCURRENTLY y reinicio de réplicas en slot Blue',
        ]);

        $valido->assertStatus(201)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.severidad', 'P1')
            ->assertJsonPath('data.tiempo_estimado_resolucion_horas', 2)
            ->assertJsonPath('data.estado', 'EN_REVISION');

        $rfcId = $valido->json('data.id_rfc');
        $this->assertNotEmpty($rfcId);

        // 3. Aprobación formal del RFC por Administrador/PO
        $aprobacion = $this->patchJson("/api/v1/admin/rfcs/{$rfcId}/aprobar");
        $aprobacion->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.estado', 'APROBADO')
            ->assertJsonPath('data.aprobado_por', 'ADM-001');

        $audit = AuditoriaI::where('tipo_movimiento', 'APROBACION_RFC')
            ->where('registro_id', $rfcId)
            ->first();
        $this->assertNotNull($audit);
    }

    /**
     * PROV-RN-013: Comando auditoria:consolidar-evidencias compila puntos AI-01 a AI-06 y genera SHA-256.
     */
    public function test_prov_rn013_comando_consolidar_evidencias_ai01_a_ai06_con_firma(): void
    {
        $repoPath = storage_path('app/auditoria/REPOSITORIO_MAESTRO_EVIDENCIAS_AI01_AI06.json');
        File::delete($repoPath);
        File::delete("{$repoPath}.sha256");

        $exitCode = Artisan::call('auditoria:consolidar-evidencias', ['--firmar' => true]);
        $this->assertSame(0, $exitCode);

        $this->assertFileExists($repoPath);
        $this->assertFileExists("{$repoPath}.sha256");

        $contenido = json_decode(File::get($repoPath), true);
        $this->assertEquals(100, $contenido['resumen_cumplimiento']['porcentaje_cumplimiento']);
        $this->assertCount(6, $contenido['checkpoints']);
        $this->assertSame('CONCLUIDO_Y_CERTIFICADO', $contenido['checkpoints']['AI-06']['estado']);
        $this->assertNotEmpty($contenido['checksum_paquete_sha256']);

        $audit = AuditoriaI::where('tipo_movimiento', 'CONSOLIDACION_EVIDENCIAS_AUDITORIA')
            ->orderBy('id_auditoria', 'desc')
            ->first();
        $this->assertNotNull($audit);
    }

    /**
     * PROV-RN-013: Endpoint de evidencias maestras accesible por Administrador y auditable.
     */
    public function test_prov_rn013_endpoint_evidencias_maestras_retorna_paquete_completo(): void
    {
        Sanctum::actingAs($this->admin, ['*']);

        $response = $this->getJson('/api/v1/admin/auditoria/evidencias-maestras');
        $response->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.resumen_cumplimiento.total_checkpoints', 6)
            ->assertJsonPath('data.resumen_cumplimiento.porcentaje_cumplimiento', 100);

        $audit = AuditoriaI::where('tipo_movimiento', 'CONSULTA_REPOSITORIO_EVIDENCIAS')
            ->orderBy('id_auditoria', 'desc')
            ->first();
        $this->assertNotNull($audit);
    }
}
