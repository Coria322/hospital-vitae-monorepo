<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Models\Usuario;
use App\Services\AuditLogger;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class Sprint2AuditoriaTest extends TestCase
{
    use RefreshDatabase;

    protected Usuario $admin;
    protected Usuario $medico;

    protected function setUp(): void
    {
        parent::setUp();

        $this->admin = Usuario::create([
            'id_usuario' => 'ADM-AUDIT',
            'nombre_completo' => 'Auditor Admin',
            'rol' => 'ADMINISTRADOR',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
        ]);

        $this->medico = Usuario::create([
            'id_usuario' => 'MED-AUDIT',
            'nombre_completo' => 'Dr. Auditor',
            'rol' => 'MEDICO',
            'cedula' => '1234567890',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
        ]);
    }

    public function test_registro_en_auditoria_i_con_encadenamiento_sha256(): void
    {
        $reg1 = AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'usuario',
            registroId: 'USR-001',
            valorAnterior: null,
            valorNuevo: ['nombre' => 'Test 1'],
            resultado: 'EXITOSO',
            motivo: 'Alta inicial'
        );

        $genesis = str_repeat('0', 64);
        $canonico1 = AuditoriaI::buildCanonico($reg1->getAttributes());
        $expectedHash1 = hash('sha256', $canonico1 . $genesis);

        $this->assertEquals($expectedHash1, $reg1->huella_integridad);

        $reg2 = AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'usuario',
            registroId: 'USR-001',
            valorAnterior: ['nombre' => 'Test 1'],
            valorNuevo: ['nombre' => 'Test 1 Editado'],
            resultado: 'EXITOSO',
            motivo: 'Actualización de datos'
        );

        $canonico2 = AuditoriaI::buildCanonico($reg2->getAttributes());
        $expectedHash2 = hash('sha256', $canonico2 . $reg1->huella_integridad);

        $this->assertEquals($expectedHash2, $reg2->huella_integridad);
    }

    public function test_inalterabilidad_bloquea_update_y_delete_en_eloquent_y_base_de_datos(): void
    {
        $reg = AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'paciente',
            registroId: 'PAC-001',
            valorAnterior: null,
            valorNuevo: ['curp' => 'TEST123456HDFR00'],
            resultado: 'EXITOSO',
            motivo: 'Registro de paciente'
        );

        // Intento de UPDATE vía Eloquent debe lanzar excepción
        $this->expectException(\RuntimeException::class);
        $reg->update(['motivo' => 'Alteración fraudulenta']);

        // Intento de DELETE vía Eloquent debe lanzar excepción
        $this->expectException(\RuntimeException::class);
        $reg->delete();
    }

    public function test_triggers_bloquean_update_y_delete_directos_en_base_de_datos(): void
    {
        $reg = AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'paciente',
            registroId: 'PAC-002',
            valorAnterior: null,
            valorNuevo: ['curp' => 'TEST123456HDFR01'],
            resultado: 'EXITOSO'
        );

        $updateFailed = false;
        try {
            DB::table('auditoria_i')
                ->where('id_auditoria', $reg->id_auditoria)
                ->update(['motivo' => 'Hack']);
        } catch (\Throwable $e) {
            $updateFailed = true;
        }
        $this->assertTrue($updateFailed, 'El trigger debió rechazar el UPDATE directo en auditoria_i');

        $deleteFailed = false;
        try {
            DB::table('auditoria_i')
                ->where('id_auditoria', $reg->id_auditoria)
                ->delete();
        } catch (\Throwable $e) {
            $deleteFailed = true;
        }
        $this->assertTrue($deleteFailed, 'El trigger debió rechazar el DELETE directo en auditoria_i');
    }

    public function test_sanitizacion_no_almacena_passwords_ni_tokens(): void
    {
        $reg = AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'usuario',
            registroId: 'USR-SECRET',
            valorAnterior: null,
            valorNuevo: [
                'id_usuario' => 'USR-SECRET',
                'password' => 'SuperSecreto123!',
                'password_hash' => '$2y$12$abcdefghijklmnopqrstuv',
                'token' => 'secret_token_value',
                'nombre' => 'Doctor Seguro',
            ],
            resultado: 'EXITOSO'
        );

        $this->assertStringNotContainsString('SuperSecreto123!', $reg->valor_nuevo);
        $this->assertStringNotContainsString('secret_token_value', $reg->valor_nuevo);
        $this->assertStringContainsString('[REDACTED]', $reg->valor_nuevo);
        $this->assertStringContainsString('Doctor Seguro', $reg->valor_nuevo);
    }

    public function test_consulta_auditoria_por_administrador_retorna_200_y_genera_autoauditoria(): void
    {
        $token = $this->admin->createToken('admin-token')->plainTextToken;

        // Insertar un registro previo para consultar
        AuditLogger::log(
            tipoMovimiento: 'LOGIN',
            tablaAfectada: 'auth',
            registroId: $this->admin->id_usuario,
            valorAnterior: null,
            valorNuevo: ['status' => 'session_started'],
            resultado: 'EXITOSO'
        );

        $response = $this->withHeader('Authorization', "Bearer {$token}")
            ->getJson('/api/v1/auditoria');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'status',
                'data' => [
                    'current_page',
                    'data' => [
                        '*' => [
                            'id_auditoria',
                            'fecha_hora',
                            'tipo_movimiento',
                            'id_usuario',
                            'tabla_afectada',
                            'resultado',
                            'huella_integridad',
                        ],
                    ],
                ],
            ]);

        // Verificar que la consulta misma generó un registro CONSULTA_AUDITORIA
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'CONSULTA_AUDITORIA',
            'tabla_afectada' => 'auditoria_i',
            'id_usuario' => $this->admin->id_usuario,
        ]);
    }

    public function test_consulta_auditoria_por_medico_retorna_403_y_registra_denegado(): void
    {
        $token = $this->medico->createToken('medico-token')->plainTextToken;

        $response = $this->withHeader('Authorization', "Bearer {$token}")
            ->getJson('/api/v1/auditoria');

        $response->assertStatus(403)
            ->assertJson([
                'status' => 'error',
            ]);

        // Verificar que el 403 generó una auditoría con resultado DENEGADO
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'ACCESO_DENEGADO',
            'resultado' => 'DENEGADO',
            'id_usuario' => $this->medico->id_usuario,
        ]);
    }

    public function test_metodos_de_escritura_sobre_auditoria_retornan_405(): void
    {
        $token = $this->admin->createToken('admin-token')->plainTextToken;

        // POST a /api/v1/auditoria debe retornar 405
        $this->withHeader('Authorization', "Bearer {$token}")
            ->postJson('/api/v1/auditoria', ['motivo' => 'Intento'])
            ->assertStatus(405);

        // DELETE a /api/v1/auditoria/1 debe retornar 405
        $this->withHeader('Authorization', "Bearer {$token}")
            ->deleteJson('/api/v1/auditoria/1')
            ->assertStatus(405);

        // PUT a /api/v1/auditoria/1 debe retornar 405
        $this->withHeader('Authorization', "Bearer {$token}")
            ->putJson('/api/v1/auditoria/1', ['motivo' => 'Intento'])
            ->assertStatus(405);
    }

    public function test_comando_verificar_cadena_valida_integridad(): void
    {
        // Registrar dos eventos válidos
        AuditLogger::log('INSERT', 'usuario', 'U1', null, ['a' => 1]);
        AuditLogger::log('INSERT', 'usuario', 'U2', null, ['a' => 2]);

        $this->artisan('auditoria:verificar-cadena')
            ->expectsOutputToContain('100% íntegra')
            ->assertExitCode(0);
    }
}
