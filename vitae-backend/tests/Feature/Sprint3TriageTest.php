<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Models\Paciente;
use App\Models\SalaAtencion;
use App\Models\Triage;
use App\Models\Usuario;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class Sprint3TriageTest extends TestCase
{
    use RefreshDatabase;

    protected Usuario $enfermera;
    protected Usuario $medico;
    protected Usuario $recepcionista;
    protected Usuario $admin;
    protected Paciente $pacientePrueba;

    protected function setUp(): void
    {
        parent::setUp();

        $this->enfermera = Usuario::updateOrCreate(
            ['id_usuario' => 'ENF-TEST'],
            [
                'nombre_completo' => 'Lic. Gabriela Ramos',
                'rol' => 'ENFERMERIA',
                'cedula' => 'ENF-123456',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->medico = Usuario::updateOrCreate(
            ['id_usuario' => 'MED-TEST'],
            [
                'nombre_completo' => 'Dr. Alejandro Cruz',
                'rol' => 'MEDICO',
                'cedula' => 'MED-987654',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->recepcionista = Usuario::updateOrCreate(
            ['id_usuario' => 'REC-TEST'],
            [
                'nombre_completo' => 'Recepción Ventanilla 1',
                'rol' => 'RECEPCION',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->admin = Usuario::updateOrCreate(
            ['id_usuario' => 'ADM-TEST'],
            [
                'nombre_completo' => 'Administrador de Pruebas',
                'rol' => 'ADMINISTRADOR',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->pacientePrueba = Paciente::create([
            'curp' => 'VITE800101HDFRRN01',
            'nombre_completo' => 'Juan Carlos Morales',
            'fecha_nacimiento' => '1980-01-01',
            'sexo' => 'MASCULINO',
            'domicilio' => 'Av. Insurgentes Sur 1234, CDMX',
            'seguro_activo' => true,
            'estado_activo' => true,
            'identificado' => true,
        ]);
    }

    public function test_rf004_captura_completa_de_triage_por_enfermeria_retorna_201_y_audita(): void
    {
        $response = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'frecuencia_resp' => 18,
            'temperatura' => 36.5,
            'spo2' => 98,
            'nivel_glucosa' => 95,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 0,
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.triage.color_asig', 'AZUL');

        $this->assertDatabaseHas('triage', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'color_asig' => 'AZUL',
        ]);

        // Verificar que se registró en AUDITORIA_I
        $auditoria = AuditoriaI::where('tabla_afectada', 'triage')
            ->where('tipo_movimiento', 'INSERT')
            ->orderBy('id_auditoria', 'desc')
            ->first();

        $this->assertNotNull($auditoria);
        $this->assertEquals('EXITOSO', $auditoria->resultado);
    }

    public function test_rf004_campo_obligatorio_faltante_retorna_422(): void
    {
        $response = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            // Falta nivel_conciencia
            'escala_eva' => 0,
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['nivel_conciencia']);

        $this->assertEquals(0, Triage::count());
    }

    public function test_rf004_rol_no_permitido_recepcion_retorna_403(): void
    {
        $response = $this->actingAs($this->recepcionista, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 0,
        ]);

        $response->assertStatus(403);
    }

    public function test_rf004_frecuencia_cardiaca_fuera_de_rango_fisiologico_retorna_422(): void
    {
        $response = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 999, // TC-TRG-005
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 0,
        ]);

        $response->assertStatus(422)
            ->assertJsonPath('status', 'error');
    }

    public function test_rf005_paciente_inconsciente_fuerza_nivel_rojo_y_asigna_sala_emergencia(): void
    {
        $response = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'nivel_conciencia' => 'INCONSCIENTE', // RF-005 Escenario 1
            'escala_eva' => 0,
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('data.triage.color_asig', 'ROJO')
            ->assertJsonPath('data.asignacion.asignado', true);

        $triage = Triage::first();
        $this->assertEquals('ROJO', $triage->color_asig);
        $this->assertNotNull($triage->id_sala);

        // Verificar que la sala asignada está marcada como OCUPADA
        $sala = SalaAtencion::find($triage->id_sala);
        $this->assertEquals('OCUPADA', $sala->estado);
        $this->assertEquals($triage->id_triage, $sala->id_triage_actual);
    }

    public function test_rf005_degradacion_manual_de_rojo_es_bloqueada(): void
    {
        // Creamos un caso en ROJO
        $triage = Triage::create([
            'id_triage' => 'TRG-ROJO01',
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '60/40',
            'frecuencia_card' => 150,
            'nivel_conciencia' => 'INCONSCIENTE',
            'escala_eva' => 10,
            'color_asig' => 'ROJO',
            'color_original' => 'ROJO',
            'fecha_hora_ingreso' => Carbon::now('UTC'),
            'folio_ingreso' => 'FOL-ROJO-001',
            'sla_vencimiento_en' => Carbon::now('UTC'),
            'sla_vencido' => false,
            'estado_atencion' => 'EN_ATENCION',
        ]);

        // El médico intenta degradar a AMARILLO
        $response = $this->actingAs($this->medico, 'sanctum')->postJson("/api/v1/triages/{$triage->id_triage}/reclasificaciones", [
            'nuevo_color' => 'AMARILLO',
            'motivo' => 'Intento de degradar paciente crítico',
        ]);

        $response->assertStatus(422)
            ->assertJsonPath('status', 'error');

        $triage->refresh();
        $this->assertEquals('ROJO', $triage->color_asig);
    }

    public function test_rf006_liberacion_de_consultorio_asigna_al_siguiente_paciente_en_espera(): void
    {
        // Paciente 1 formado en espera (AMARILLO)
        $triage1 = Triage::create([
            'id_triage' => 'TRG-ESP001',
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '130/85',
            'frecuencia_card' => 90,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 6,
            'color_asig' => 'AMARILLO',
            'color_original' => 'AMARILLO',
            'fecha_hora_ingreso' => Carbon::now('UTC')->subMinutes(30),
            'folio_ingreso' => 'FOL-ESP-001',
            'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(30),
            'estado_atencion' => 'EN_ESPERA',
        ]);

        // Consultorio 1 ocupado inicialmente
        $consultorio = SalaAtencion::where('id_sala', 'CONS-01')->first();
        $consultorio->update(['estado' => 'OCUPADA', 'id_triage_actual' => 'TRG-PREVIO']);

        // Médico libera el consultorio
        $response = $this->actingAs($this->medico, 'sanctum')->postJson("/api/v1/salas/{$consultorio->id_sala}/liberar");

        $response->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.siguiente_paciente_asignado', 'FOL-ESP-001');

        $triage1->refresh();
        $this->assertEquals('EN_ATENCION', $triage1->estado_atencion);
        $this->assertEquals('CONS-01', $triage1->id_sala);
    }

    public function test_rf007_reclasificacion_medica_exige_justificacion_y_registra_auditoria(): void
    {
        $triage = Triage::create([
            'id_triage' => 'TRG-REC001',
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '120/80',
            'frecuencia_card' => 85,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 4,
            'color_asig' => 'VERDE',
            'color_original' => 'VERDE',
            'fecha_hora_ingreso' => Carbon::now('UTC')->subMinutes(15),
            'folio_ingreso' => 'FOL-REC-001',
            'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(105),
            'estado_atencion' => 'EN_ESPERA',
        ]);

        // 1. Sin motivo -> 422
        $respSinMotivo = $this->actingAs($this->medico, 'sanctum')->postJson("/api/v1/triages/{$triage->id_triage}/reclasificaciones", [
            'nuevo_color' => 'NARANJA',
            'motivo' => '',
        ]);
        $respSinMotivo->assertStatus(422);

        // 2. Con motivo válido -> 200 OK y auditado
        $respValida = $this->actingAs($this->medico, 'sanctum')->postJson("/api/v1/triages/{$triage->id_triage}/reclasificaciones", [
            'nuevo_color' => 'NARANJA',
            'motivo' => 'Deterioro hemodinámico agudo y diaforesis profusa detectada en revaloración',
        ]);

        $respValida->assertStatus(200)
            ->assertJsonPath('data.color_asig', 'NARANJA');

        $triage->refresh();
        $this->assertEquals('NARANJA', $triage->color_asig);

        $auditoria = AuditoriaI::where('tabla_afectada', 'triage')
            ->where('tipo_movimiento', 'UPDATE')
            ->first();

        $this->assertNotNull($auditoria);
        $this->assertStringContainsString('Deterioro hemodinámico', $auditoria->motivo);
    }

    public function test_prov_rn004_calculo_estricto_de_slas_por_nivel(): void
    {
        $ahora = Carbon::now('UTC');

        // Test NARANJA (15 min)
        $triageNaranja = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '190/110',
            'frecuencia_card' => 135,
            'nivel_conciencia' => 'DOLOR',
            'escala_eva' => 9,
        ]);
        $triageNaranja->assertStatus(201)->assertJsonPath('data.triage.color_asig', 'NARANJA');
        $objNaranja = Triage::where('color_asig', 'NARANJA')->first();
        $this->assertEquals(15, (int) $objNaranja->fecha_hora_ingreso->diffInMinutes($objNaranja->sla_vencimiento_en));

        // Test AMARILLO (60 min)
        $triageAmarillo = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '140/90',
            'frecuencia_card' => 110,
            'nivel_conciencia' => 'VERBAL',
            'escala_eva' => 7,
        ]);
        $triageAmarillo->assertStatus(201)->assertJsonPath('data.triage.color_asig', 'AMARILLO');
        $objAmarillo = Triage::where('color_asig', 'AMARILLO')->first();
        $this->assertEquals(60, (int) $objAmarillo->fecha_hora_ingreso->diffInMinutes($objAmarillo->sla_vencimiento_en));
    }

    public function test_prov_rn005_comando_verificar_sla_detecta_vencimiento_y_audita_evento(): void
    {
        // Triage con SLA vencido hace 20 minutos
        $triageVencido = Triage::create([
            'id_triage' => 'TRG-VENC01',
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 3,
            'color_asig' => 'VERDE',
            'color_original' => 'VERDE',
            'fecha_hora_ingreso' => Carbon::now('UTC')->subMinutes(140),
            'folio_ingreso' => 'FOL-VENC-001',
            'sla_vencimiento_en' => Carbon::now('UTC')->subMinutes(20),
            'sla_vencido' => false,
            'estado_atencion' => 'EN_ESPERA',
        ]);

        $this->artisan('triage:verificar-sla')
            ->expectsOutputToContain('Monitoreo SLA completado')
            ->assertExitCode(0);

        $triageVencido->refresh();
        $this->assertTrue($triageVencido->sla_vencido);
        $this->assertGreaterThanOrEqual(20, $triageVencido->desviacion_minutos);

        // Verificar log en AUDITORIA_I con tipo_movimiento SLA_VENCIDO
        $auditoria = AuditoriaI::where('tipo_movimiento', 'SLA_VENCIDO')
            ->where('registro_id', 'TRG-VENC01')
            ->first();

        $this->assertNotNull($auditoria);
        $this->assertEquals('EXITOSO', $auditoria->resultado);
    }

    public function test_prov_rn006_captura_retroactiva_de_folios_conserva_timestamp_fisico(): void
    {
        $horaFisica = Carbon::now('UTC')->subHours(2);

        $response = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 82,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 1,
            'captura_retroactiva' => true,
            'fecha_hora_ingreso' => $horaFisica->toIso8601String(),
        ]);

        $response->assertStatus(201);

        $triage = Triage::latest('created_at')->first();
        $this->assertTrue($triage->captura_retroactiva);
        $this->assertEquals($horaFisica->format('Y-m-d H:i'), $triage->fecha_hora_ingreso->format('Y-m-d H:i'));

        // Auditoría registra CAPTURA_RETROACTIVA como motivo
        $auditoria = AuditoriaI::where('tabla_afectada', 'triage')
            ->where('registro_id', $triage->id_triage)
            ->first();

        $this->assertEquals('CAPTURA_RETROACTIVA', $auditoria->motivo);
    }

    public function test_prov_rn007_admision_paciente_desconocido_nn_y_fusion_autorizada(): void
    {
        // 1. Admisión de paciente N.N. inconsciente
        $respNN = $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/pacientes', [
            'identificado' => false,
            'sexo' => 'MASCULINO',
        ]);

        $respNN->assertStatus(201)
            ->assertJsonPath('data.identificado', false);

        $idPacienteNN = $respNN->json('data.id_paciente');

        // Se le registra triage a ese paciente N.N.
        $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $idPacienteNN,
            'presion_art' => '70/50',
            'frecuencia_card' => 140,
            'nivel_conciencia' => 'INCONSCIENTE',
            'escala_eva' => 10,
        ]);

        $this->assertEquals(1, Triage::where('id_paciente', $idPacienteNN)->count());

        // 2. Al confirmar identidad del paciente con familiares, el médico ejecuta la fusión
        $respFusion = $this->actingAs($this->medico, 'sanctum')->postJson("/api/v1/pacientes/{$idPacienteNN}/fusiones", [
            'id_paciente_definitivo' => $this->pacientePrueba->id_paciente,
            'motivo' => 'Familiar directo presenta INE y acta de nacimiento acreditando identidad de Juan Carlos Morales',
        ]);

        $respFusion->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('triages_migrados', 1);

        // Triages ahora pertenecen al paciente definitivo
        $this->assertEquals(0, Triage::where('id_paciente', $idPacienteNN)->count());
        $this->assertEquals(1, Triage::where('id_paciente', $this->pacientePrueba->id_paciente)->count());

        // Paciente temporal queda con estado_activo = false
        $pacienteNN = Paciente::find($idPacienteNN);
        $this->assertFalse($pacienteNN->estado_activo);

        // Registro inalterable en AUDITORIA_I
        $auditoria = AuditoriaI::where('tipo_movimiento', 'FUSION_PACIENTES')
            ->where('registro_id', $idPacienteNN)
            ->first();

        $this->assertNotNull($auditoria);
        $this->assertEquals('EXITOSO', $auditoria->resultado);
    }

    public function test_prov_ux002_pantalla_sala_espera_publica_garantiza_cero_pii(): void
    {
        // Registrar un triage visible en cola
        $this->actingAs($this->enfermera, 'sanctum')->postJson('/api/v1/triages', [
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 2,
        ]);

        $response = $this->getJson('/api/v1/sala-espera');

        $response->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonStructure([
                'data' => [
                    'total_en_espera',
                    'pacientes' => [
                        '*' => [
                            'posicion',
                            'folio',
                            'iniciales',
                            'color',
                            'icono',
                            'estado_clinico',
                            'estado_atencion',
                            'sala',
                            'sla_vencido',
                        ],
                    ],
                    'ultima_actualizacion_utc',
                ],
            ]);

        $content = $response->getContent();
        // Garantía estricta LFPDPPP: Ni CURP ni nombre completo aparecen en la respuesta pública
        $this->assertStringNotContainsString('Juan Carlos Morales', $content);
        $this->assertStringNotContainsString('VITE800101HDFRRN01', $content);
        // Debe figurar como iniciales
        $this->assertStringContainsString('J.C.M.', $content);
    }

    public function test_nom004_prohibido_delete_fisico_en_triage(): void
    {
        $triage = Triage::create([
            'id_triage' => 'TRG-DEL001',
            'id_paciente' => $this->pacientePrueba->id_paciente,
            'id_usuario' => 'ENF-TEST',
            'presion_art' => '120/80',
            'frecuencia_card' => 80,
            'nivel_conciencia' => 'ALERTA',
            'escala_eva' => 0,
            'color_asig' => 'AZUL',
            'color_original' => 'AZUL',
            'fecha_hora_ingreso' => Carbon::now('UTC'),
            'folio_ingreso' => 'FOL-DEL-001',
            'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(240),
            'estado_atencion' => 'EN_ESPERA',
        ]);

        // 1. Intento de DELETE por Eloquent
        $this->expectException(\RuntimeException::class);
        $this->expectExceptionMessage('Prohibido eliminar físicamente registros de triage');
        $triage->delete();

        // 2. Intento de DELETE directo por DB Query (trigger nativo)
        DB::table('triage')->where('id_triage', 'TRG-DEL001')->delete();
    }
}
