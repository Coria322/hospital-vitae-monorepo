<?php

namespace Tests\Feature;

use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Usuario;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

class Sprint2PersistenciaClinicaTest extends TestCase
{
    use RefreshDatabase;

    protected Usuario $medico;
    protected Paciente $paciente;

    protected function setUp(): void
    {
        parent::setUp();

        // 1. Crear médico tratante
        $this->medico = Usuario::create([
            'id_usuario' => 'MED-CLINIC',
            'nombre_completo' => 'Dr. Roberto Clínico',
            'rol' => 'MEDICO',
            'cedula' => '9876543210',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
        ]);

        // 2. Crear paciente con UUID automático
        $this->paciente = Paciente::create([
            'curp' => 'ROBC800101HDFR00',
            'nombre_completo' => 'Carlos Robles',
            'fecha_nacimiento' => '1980-01-01',
            'sexo' => 'MASCULINO',
            'domicilio' => 'Av. Universidad 123, CDMX',
            'seguro_activo' => true,
            'estado_activo' => true,
        ]);
    }

    public function test_creacion_de_paciente_genera_uuid_valido(): void
    {
        $this->assertNotEmpty($this->paciente->id_paciente);
        $this->assertTrue(Str::isUuid($this->paciente->id_paciente));
        $this->assertEquals('ROBC800101HDFR00', $this->paciente->curp);
        $this->assertTrue($this->paciente->estado_activo);
    }

    public function test_creacion_de_expediente_clinico_con_relaciones_estrictas(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000001',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Cefalea intensa y dolor torácico',
            'diagnostico' => 'Sospecha de hipertensión grado II',
            'fecha_creacion' => Carbon::now('UTC'),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC'),
            'estado_activo' => true,
            'bloqueo_legal' => false,
        ]);

        $this->assertEquals('EXP-000001', $expediente->id_expediente);
        $this->assertEquals($this->paciente->id_paciente, $expediente->paciente->id_paciente);
        $this->assertEquals($this->medico->id_usuario, $expediente->medicoTratante->id_usuario);
    }

    public function test_trigger_y_eloquent_bloquean_delete_fisico_en_expediente_y_paciente(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000002',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Control rutinario',
            'fecha_creacion' => Carbon::now('UTC'),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC'),
            'estado_activo' => true,
        ]);

        // 1. DELETE vía Eloquent debe arrojar excepción
        $eloquentExpFailed = false;
        try {
            $expediente->delete();
        } catch (\RuntimeException $e) {
            $eloquentExpFailed = true;
        }
        $this->assertTrue($eloquentExpFailed, 'Eloquent debió abortar el DELETE en expediente_c');

        // 2. DELETE directo en base de datos debe ser rechazado por el trigger
        $sqlExpFailed = false;
        try {
            DB::table('expediente_c')->where('id_expediente', 'EXP-000002')->delete();
        } catch (\Throwable $e) {
            $sqlExpFailed = true;
        }
        $this->assertTrue($sqlExpFailed, 'El trigger debió rechazar el DELETE SQL en expediente_c');

        // 3. DELETE directo en tabla paciente debe ser rechazado por el trigger
        $sqlPacFailed = false;
        try {
            DB::table('paciente')->where('id_paciente', $this->paciente->id_paciente)->delete();
        } catch (\Throwable $e) {
            $sqlPacFailed = true;
        }
        $this->assertTrue($sqlPacFailed, 'El trigger debió rechazar el DELETE SQL en paciente');
    }

    public function test_baja_logica_cambia_estado_y_registra_auditoria(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000003',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Consulta general',
            'fecha_creacion' => Carbon::now('UTC'),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC'),
            'estado_activo' => true,
        ]);

        $expediente->bajaLogica('Cierre de expediente por traslado');

        $this->assertFalse($expediente->fresh()->estado_activo);

        // Validar que se insertó registro en auditoria_i
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'SOFT_DELETE',
            'tabla_afectada' => 'expediente_c',
            'registro_id' => 'EXP-000003',
        ]);
    }

    public function test_actualizacion_de_fecha_ultimo_acto_medico(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000004',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Ingreso',
            'fecha_creacion' => Carbon::now('UTC')->subDays(10),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subDays(10),
            'estado_activo' => true,
        ]);

        $fechaNueva = Carbon::now('UTC');
        $expediente->registrarActoMedico($fechaNueva, 'Nueva nota de evolución');

        $this->assertEquals($fechaNueva->format('Y-m-d H:i:s'), $expediente->fresh()->fecha_ultimo_acto_medico->format('Y-m-d H:i:s'));

        // Validar auditoría del acto médico
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'UPDATE',
            'tabla_afectada' => 'expediente_c',
            'registro_id' => 'EXP-000004',
        ]);
    }

    public function test_calculo_de_retencion_legal_5_anos(): void
    {
        // Caso 1: Expediente reciente (retención vigente, no concluida)
        $expReciente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000005',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Ingreso reciente',
            'fecha_creacion' => Carbon::now('UTC')->subYears(2),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subYears(2),
            'estado_activo' => true,
        ]);
        $this->assertFalse($expReciente->tieneRetencionConcluida());

        // Caso 2: Expediente con más de 5 años desde último acto médico (retención cumplida)
        $pacienteAntiguo = Paciente::create([
            'nombre_completo' => 'Paciente Antiguo',
            'fecha_nacimiento' => '1950-01-01',
            'sexo' => 'MASCULINO',
            'domicilio' => 'Calle Vieja 1',
        ]);

        $expAntiguo = ExpedienteClinico::create([
            'id_expediente' => 'EXP-000006',
            'id_paciente' => $pacienteAntiguo->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Atención en 2018',
            'fecha_creacion' => Carbon::now('UTC')->subDays(1900),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subDays(1900), // > 1825 días
            'estado_activo' => true,
        ]);
        $this->assertTrue($expAntiguo->tieneRetencionConcluida());
    }

    public function test_comando_reporte_retencion_detecta_expedientes_concluidos_y_registra_auditoria(): void
    {
        // 1. Expediente con retención concluida
        $pacienteConcluido = Paciente::create([
            'nombre_completo' => 'Paciente Plazo Concluido',
            'fecha_nacimiento' => '1960-05-15',
            'sexo' => 'FEMENINO',
            'domicilio' => 'Calle Reforma 500',
        ]);

        ExpedienteClinico::create([
            'id_expediente' => 'EXP-CONCLU',
            'id_paciente' => $pacienteConcluido->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Tratamiento 2017',
            'fecha_creacion' => Carbon::now('UTC')->subDays(2000),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subDays(2000),
            'estado_activo' => true,
        ]);

        $this->artisan('expedientes:reporte-retencion')
            ->expectsOutputToContain('EXP-CONCLU')
            ->assertExitCode(0);

        // Validar que el reporte generó evento de auditoría
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'REPORTE_RETENCION',
            'tabla_afectada' => 'expediente_c',
            'registro_id' => 'RELACION_MENSUAL',
        ]);
    }
}
