<?php

namespace Tests\Feature;

use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Tests\TestCase;

class Sprint2AltaDisponibilidadTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Valida que el endpoint de salud responda con 200 OK y estado UP cuando los subsistemas operan (RNF-004 / MC-06).
     */
    public function test_endpoint_salud_retorna_200_ok_y_estado_up(): void
    {
        $response = $this->getJson('/api/v1/salud');

        $response->assertStatus(200)
            ->assertJson([
                'status' => 'UP',
                'database' => 'CONNECTED',
                'storage' => 'WRITABLE',
                'service' => 'hospital-vitae-api',
            ]);

        $this->assertSame(300, $response->json('alta_disponibilidad.rto_objetivo_segundos'));
        $this->assertTrue($response->json('alta_disponibilidad.aislamiento_fallos'));
    }

    /**
     * Valida que el esquema de base de datos clínico permanezca íntegro e intacto (Escenario 2 de RNF-004).
     */
    public function test_datos_clinicos_permanecen_integros_y_disponibles(): void
    {
        $medico = Usuario::create([
            'id_usuario' => 'MED-HA-01',
            'nombre_completo' => 'Dra. Resiliencia Sistemas',
            'rol' => 'MEDICO',
            'cedula' => '11998877',
            'password_hash' => bcrypt('PasswordSegura123!'),
            'estado_cuenta' => 'ACTIVA',
            'intentos_fallidos' => 0,
        ]);

        $paciente = Paciente::create([
            'id_paciente' => (string) Str::uuid(),
            'curp' => 'RESI900101HDFRSL01',
            'nombre_completo' => 'Paciente Alta Disponibilidad',
            'fecha_nacimiento' => '1990-01-01',
            'sexo' => 'MASCULINO',
            'domicilio' => 'Hospital Central Modulo B',
            'seguro_activo' => true,
            'estado_activo' => true,
        ]);

        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-HA01',
            'id_paciente' => $paciente->id_paciente,
            'id_usuario' => $medico->id_usuario,
            'motivo_consulta' => 'Prueba de resiliencia y aislamiento de fallos',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        // Verificar que los datos son recuperables exactamente
        $recuperado = ExpedienteClinico::with('paciente')->find('EXP-HA01');
        $this->assertNotNull($recuperado);
        $this->assertSame('Paciente Alta Disponibilidad', $recuperado->paciente->nombre_completo);
        $this->assertFalse($recuperado->bloqueo_legal);
    }

    /**
     * Valida estáticamente la orquestación docker-compose: réplicas >= 2, healthcheck, límites de recursos y 0 ':latest'.
     */
    public function test_orquestacion_docker_compose_cumple_criterios_alta_disponibilidad(): void
    {
        $composePath = base_path('docker-compose.yml');
        $this->assertFileExists($composePath);

        $contenido = file_get_contents($composePath);

        // 1. Cero ocurrencias de :latest
        $this->assertStringNotContainsString(':latest', $contenido);

        // 2. Réplicas >= 2
        $this->assertStringContainsString('replicas: 2', $contenido);

        // 3. Healthcheck configurado
        $this->assertStringContainsString('healthcheck:', $contenido);
        $this->assertStringContainsString('/api/v1/salud', $contenido);

        // 4. Política de reinicio
        $this->assertStringContainsString('restart: unless-stopped', $contenido);

        // 5. Límites de recursos definidos (CPU / Memoria)
        $this->assertStringContainsString('limits:', $contenido);
        $this->assertStringContainsString('cpus:', $contenido);
        $this->assertStringContainsString('memory:', $contenido);
    }
}
