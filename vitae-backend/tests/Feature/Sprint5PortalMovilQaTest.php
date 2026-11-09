<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Models\Cita;
use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Receta;
use App\Models\Usuario;
use Carbon\Carbon;
use Database\Seeders\ClinicoSinteticoSeeder;
use Database\Seeders\UsuarioSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\File;
use Illuminate\Support\Str;
use Tests\TestCase;

class Sprint5PortalMovilQaTest extends TestCase
{
    use RefreshDatabase;

    protected Paciente $paciente1;
    protected Paciente $paciente2;
    protected Usuario $userPaciente1;
    protected Usuario $userPaciente2;
    protected Usuario $userMedico;
    protected Usuario $userRecepcion;
    protected Usuario $userAdmin;

    protected string $tokenPaciente1;
    protected string $tokenPaciente2;
    protected string $tokenMedico;
    protected string $tokenRecepcion;
    protected string $tokenAdmin;

    protected function setUp(): void
    {
        parent::setUp();

        $this->seed(UsuarioSeeder::class);
        $this->seed(ClinicoSinteticoSeeder::class);

        $this->paciente1 = Paciente::where('curp', 'SINT900101HDFR01')->firstOrFail();
        $this->paciente2 = Paciente::where('curp', 'SINT750515MDFR02')->firstOrFail();

        $this->userPaciente1 = Usuario::where('id_usuario', 'PAC-001')->firstOrFail();
        $this->userPaciente2 = Usuario::where('id_usuario', 'PAC-002')->firstOrFail();
        $this->userMedico = Usuario::where('id_usuario', 'MED-001')->firstOrFail();
        $this->userRecepcion = Usuario::where('id_usuario', 'REC-001')->firstOrFail();
        $this->userAdmin = Usuario::where('id_usuario', 'ADM-001')->firstOrFail();

        $this->tokenPaciente1 = $this->userPaciente1->createToken('test-pac1')->plainTextToken;
        $this->tokenPaciente2 = $this->userPaciente2->createToken('test-pac2')->plainTextToken;
        $this->tokenMedico = $this->userMedico->createToken('test-med')->plainTextToken;
        $this->tokenRecepcion = $this->userRecepcion->createToken('test-rec')->plainTextToken;
        $this->tokenAdmin = $this->userAdmin->createToken('test-adm')->plainTextToken;
    }

    /**
     * RF-011: Paciente accede con perfil de solo lectura a citas, recetas, resultados y perfil propio.
     */
    public function test_rf011_paciente_accede_solo_lectura_a_perfil_citas_recetas_y_resultados(): void
    {
        $headers = [
            'Authorization' => 'Bearer ' . $this->tokenPaciente1,
            'X-Client-Version' => '29',
        ];

        // 1. Perfil del paciente
        $resPerfil = $this->getJson('/api/v1/mobile/perfil', $headers);
        $resPerfil->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.id_paciente', $this->paciente1->id_paciente)
            ->assertJsonPath('data.nombre_completo', $this->paciente1->nombre_completo);

        // 2. Consulta de citas propias
        $resCitas = $this->getJson('/api/v1/mobile/citas', $headers);
        $resCitas->assertStatus(200)
            ->assertJsonPath('status', 'success');
        $this->assertGreaterThanOrEqual(1, count($resCitas->json('data')));

        // 3. Consulta de recetas propias
        $resRecetas = $this->getJson('/api/v1/mobile/recetas', $headers);
        $resRecetas->assertStatus(200)
            ->assertJsonPath('status', 'success');

        // 4. Consulta de resultados
        $resResultados = $this->getJson('/api/v1/mobile/resultados', $headers);
        $resResultados->assertStatus(200)
            ->assertJsonPath('status', 'success');

        // 5. Expediente resumido
        $resExpediente = $this->getJson('/api/v1/mobile/expediente', $headers);
        $resExpediente->assertStatus(200)
            ->assertJsonPath('status', 'success');
    }

    /**
     * RF-011: Rechazo HTTP 403 a cualquier operación de modificación sobre recursos clínicos desde móvil.
     */
    public function test_rf011_rechazo_http_403_a_escritura_clinica_desde_movil(): void
    {
        $headers = [
            'Authorization' => 'Bearer ' . $this->tokenPaciente1,
            'X-Client-Version' => '29',
        ];

        // Intento de emitir receta
        $resReceta = $this->postJson('/api/v1/mobile/recetas', [
            'medicamentos' => 'Clonazepam 2mg',
        ], $headers);
        $resReceta->assertStatus(403)
            ->assertJsonPath('code', 'CLINICAL_WRITE_FORBIDDEN');

        // Intento de mutar expediente o notas
        $resNotas = $this->postJson('/api/v1/mobile/notas', [
            'contenido' => 'Intento de nota médica falsa',
        ], $headers);
        $resNotas->assertStatus(403);

        // Verificar que se auditó el evento denegado en AUDITORIA_I
        $auditoria = AuditoriaI::where('tipo_movimiento', 'INTENTO_ESCRITURA_CLINICA_MOVIL')->first();
        $this->assertNotNull($auditoria);
        $this->assertEquals('DENEGADO', $auditoria->resultado);
    }

    /**
     * RF-011: Los tokens de personal (MEDICO, ADMIN, etc.) son rechazados en /api/v1/mobile/* (HTTP 403).
     */
    public function test_rf011_token_de_personal_sanitario_es_rechazado_en_api_movil(): void
    {
        $headersMedico = [
            'Authorization' => 'Bearer ' . $this->tokenMedico,
            'X-Client-Version' => '29',
        ];

        $response = $this->getJson('/api/v1/mobile/perfil', $headersMedico);
        $response->assertStatus(403)
            ->assertJsonPath('code', 'FORBIDDEN_ROLE');

        $auditoria = AuditoriaI::where('tipo_movimiento', 'ACCESO_DENEGADO_MOVIL')->first();
        $this->assertNotNull($auditoria);
        $this->assertEquals('DENEGADO', $auditoria->resultado);
    }

    /**
     * RF-002.1: Aislamiento horizontal anti-IDOR rechaza inyección de otro id_paciente con HTTP 403.
     */
    public function test_rf002_1_aislamiento_horizontal_anti_idor_rechaza_inyeccion_otro_paciente(): void
    {
        $headers = [
            'Authorization' => 'Bearer ' . $this->tokenPaciente1,
            'X-Client-Version' => '29',
        ];

        // Escenario 1: Inyección vía query string
        $resQuery = $this->getJson("/api/v1/mobile/recetas?id_paciente={$this->paciente2->id_paciente}", $headers);
        $resQuery->assertStatus(403)
            ->assertJsonPath('code', 'IDOR_VIOLATION_DETECTED');

        // Escenario 2: Inyección vía body en solicitud de cita
        $resBody = $this->postJson('/api/v1/citas', [
            'id_paciente' => $this->paciente2->id_paciente,
            'fecha_hora' => Carbon::now('UTC')->addDays(2)->toIso8601String(),
        ], [
            'Authorization' => 'Bearer ' . $this->tokenPaciente1,
        ]);
        $resBody->assertStatus(403);

        // Escenario 3: Intento de cancelar cita ajena
        $citaPaciente2 = Cita::create([
            'id_paciente' => $this->paciente2->id_paciente,
            'fecha_hora' => Carbon::now('UTC')->addDays(4),
            'estado' => 'PENDIENTE',
        ]);

        $resCancelar = $this->patchJson("/api/v1/mobile/citas/{$citaPaciente2->id_cita}/cancelar", [], $headers);
        $resCancelar->assertStatus(403);

        // Verificar que se auditó el evento denegado anti-IDOR
        $auditoria = AuditoriaI::where('tipo_movimiento', 'INTENTO_IDOR_MOVIL')->first();
        $this->assertNotNull($auditoria);
        $this->assertEquals('DENEGADO', $auditoria->resultado);
    }

    /**
     * RNF-006: Compatibilidad de cliente móvil: rechaza si falta cabecera (400) o si API level < 29 (426).
     */
    public function test_rnf006_compatibilidad_version_movil_valida_android_10_api_29(): void
    {
        $authHeader = ['Authorization' => 'Bearer ' . $this->tokenPaciente1];

        // 1. Cabecera ausente -> 400 Bad Request
        $resSinHeader = $this->getJson('/api/v1/mobile/perfil', $authHeader);
        $resSinHeader->assertStatus(400)
            ->assertJsonPath('code', 'CLIENT_VERSION_REQUIRED');

        // 2. Versión incompatible (Android 9 / API 28) -> 426 Upgrade Required
        $resIncompatible = $this->getJson('/api/v1/mobile/perfil', array_merge($authHeader, [
            'X-Client-Version' => '28',
        ]));
        $resIncompatible->assertStatus(426)
            ->assertJsonPath('code', 'UPGRADE_REQUIRED');

        // 3. Versión compatible exacta (Android 10 / API 29) -> 200 OK
        $resExacta = $this->getJson('/api/v1/mobile/perfil', array_merge($authHeader, [
            'X-Client-Version' => '29',
        ]));
        $resExacta->assertStatus(200);

        // 4. Versión compatible superior (Android 14 / API 34) -> 200 OK
        $resSuperior = $this->getJson('/api/v1/mobile/perfil', array_merge($authHeader, [
            'X-Client-Version' => 'Android 14/API-34',
        ]));
        $resSuperior->assertStatus(200);
    }

    /**
     * PROV-RF-002: Agenda hospitalaria: creación de cita, consulta y confirmación por recepción.
     */
    public function test_prov_rf002_agenda_creacion_consulta_y_confirmacion_por_recepcion(): void
    {
        $headersRecepcion = ['Authorization' => 'Bearer ' . $this->tokenRecepcion];

        // 1. Recepción crea cita para paciente
        $fechaFutura = Carbon::now('UTC')->addDays(5)->setTime(11, 0, 0);
        $resCrear = $this->postJson('/api/v1/citas', [
            'id_paciente' => $this->paciente1->id_paciente,
            'id_usuario' => $this->userMedico->id_usuario,
            'fecha_hora' => $fechaFutura->toIso8601String(),
            'especialidad' => 'Medicina General',
            'motivo' => 'Chequeo de rutina',
            'consultorio' => 'Consultorio 102',
        ], $headersRecepcion);

        $resCrear->assertStatus(201)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.estado', 'PENDIENTE');

        $citaId = $resCrear->json('data.id_cita');

        // 2. Recepción confirma cita
        $resConfirmar = $this->patchJson("/api/v1/citas/{$citaId}/confirmacion", [], $headersRecepcion);
        $resConfirmar->assertStatus(200)
            ->assertJsonPath('data.estado', 'CONFIRMADA')
            ->assertJsonPath('data.confirmada_por', 'REC-001');

        // 3. Caso borde: cita pasada no puede ser confirmada (HTTP 422)
        $citaPasada = Cita::create([
            'id_paciente' => $this->paciente1->id_paciente,
            'fecha_hora' => Carbon::now('UTC')->subDays(2),
            'estado' => 'PENDIENTE',
        ]);

        $resCitaPasada = $this->patchJson("/api/v1/citas/{$citaPasada->id_cita}/confirmacion", [], $headersRecepcion);
        $resCitaPasada->assertStatus(422)
            ->assertJsonPath('code', 'CITA_EXPIRADA');
    }

    /**
     * PROV-RF-001: Tableros estadísticos NOM-035 rechazan acceso a personal no administrativo (HTTP 403).
     */
    public function test_prov_rf001_tableros_estadisticos_nom035_rechaza_roles_no_autorizados(): void
    {
        $resMedico = $this->getJson('/api/v1/estadisticas/tableros', [
            'Authorization' => 'Bearer ' . $this->tokenMedico,
        ]);
        $resMedico->assertStatus(403);

        $resPaciente = $this->getJson('/api/v1/estadisticas/tableros', [
            'Authorization' => 'Bearer ' . $this->tokenPaciente1,
        ]);
        $resPaciente->assertStatus(403);
    }

    /**
     * PROV-RF-001: Tableros estadísticos NOM-035 con disociación estricta (cero PII) e inclusión de CLUES.
     */
    public function test_prov_rf001_tableros_estadisticos_nom035_solo_admin_y_cero_pii(): void
    {
        // Acceso concedido a administrador
        $resAdmin = $this->getJson('/api/v1/estadisticas/tableros', [
            'Authorization' => 'Bearer ' . $this->tokenAdmin,
        ]);
        $resAdmin->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonStructure([
                'status',
                'data' => [
                    'clues',
                    'establecimiento',
                    'fecha_corte_utc',
                    'cumplimiento_privacidad',
                    'metricas_urgencias_manchester',
                    'metricas_recetas_controladas',
                    'metricas_agenda_citas',
                    'factores_riesgo_laboral_nom035',
                ],
            ]);

        $jsonStr = $resAdmin->getContent();
        // Verificar ausencia total de PII en la respuesta
        $this->assertStringNotContainsString('curp', strtolower($jsonStr));
        $this->assertStringNotContainsString('nombre_completo', strtolower($jsonStr));
        $this->assertStringNotContainsString('domicilio', strtolower($jsonStr));
        $this->assertNotEmpty($resAdmin->json('data.clues'));

        // Exportación en CSV
        $resCsv = $this->get('/api/v1/estadisticas/exportar?formato=csv', [
            'Authorization' => 'Bearer ' . $this->tokenAdmin,
        ]);
        $resCsv->assertStatus(200);
        $resCsv->assertHeader('Content-Type', 'text/csv; charset=utf-8');

        // Verificar auditoría insertada (MN-09)
        $auditoria = AuditoriaI::where('tipo_movimiento', 'CONSULTA_ESTADISTICA_NOM035')->first();
        $this->assertNotNull($auditoria);
        $this->assertEquals('EXITOSO', $auditoria->resultado);
    }

    /**
     * RNF-005: Eficiencia clínica y tiempo de respuesta P95 <= 2.0 s en consulta de expediente clínico.
     */
    public function test_rnf005_rendimiento_consulta_ece_p95_menor_o_igual_2_segundos(): void
    {
        $headers = ['Authorization' => 'Bearer ' . $this->tokenMedico];
        $idExpediente = 'EXP-SINT01';

        $tiemposMs = [];

        // Ejecutar 15 consultas de ECE completo con relaciones
        for ($i = 0; $i < 15; $i++) {
            $inicio = microtime(true);
            $response = $this->getJson("/api/v1/expedientes-clinicos/{$idExpediente}", $headers);
            $fin = microtime(true);

            $response->assertStatus(200);
            $tiemposMs[] = ($fin - $inicio) * 1000.0;
        }

        // Calcular Percentil 95
        sort($tiemposMs);
        $indiceP95 = (int) ceil(0.95 * count($tiemposMs)) - 1;
        $latenciaP95Ms = $tiemposMs[$indiceP95];

        // Criterio normativo RNF-005: P95 <= 2000 ms (2.0 segundos)
        $this->assertLessThanOrEqual(
            2000.0,
            $latenciaP95Ms,
            "La latencia P95 ({$latenciaP95Ms} ms) supera el límite normativo de 2000 ms (RNF-005)."
        );
    }

    /**
     * PROV-SEC-009: Generación automatizada de inventario SBOM y verificación de vulnerabilidades.
     */
    public function test_prov_sec009_secops_generacion_sbom_y_evaluacion_vulnerabilidades(): void
    {
        $sbomOutputPath = 'storage/app/test_sbom.json';

        $exitCode = \Illuminate\Support\Facades\Artisan::call('secops:sbom', [
            '--output' => $sbomOutputPath,
            '--strict' => true,
        ]);

        $this->assertEquals(0, $exitCode);

        $fullPath = base_path($sbomOutputPath);
        $this->assertTrue(File::exists($fullPath));

        $sbomContent = json_decode(File::get($fullPath), true);
        $this->assertEquals('CycloneDX', $sbomContent['bomFormat']);
        $this->assertNotEmpty($sbomContent['components']);
        $this->assertEquals(0, $sbomContent['security_summary']['vulnerabilidades_criticas_cvss_9_10']);

        // Limpieza de prueba
        File::delete($fullPath);
    }
}
