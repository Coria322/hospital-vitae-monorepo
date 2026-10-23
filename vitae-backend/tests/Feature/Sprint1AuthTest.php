<?php

namespace Tests\Feature;

use App\Models\Usuario;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class Sprint1AuthTest extends TestCase
{
    use RefreshDatabase;
    protected function setUp(): void
    {
        parent::setUp();

        // Aseguramos usuario Admin para pruebas
        Usuario::updateOrCreate(
            ['id_usuario' => 'ADM-TEST'],
            [
                'nombre_completo' => 'Admin de Pruebas',
                'rol' => 'ADMINISTRADOR',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'bloqueado_hasta' => null,
            ]
        );

        // Aseguramos usuario Medico para pruebas
        Usuario::updateOrCreate(
            ['id_usuario' => 'MED-TEST'],
            [
                'nombre_completo' => 'Dr. Prueba',
                'rol' => 'MEDICO',
                'cedula' => '1234567890',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
                'intentos_fallidos' => 0,
                'bloqueado_hasta' => null,
            ]
        );
    }

    public function test_login_exitoso_retorna_token_y_datos_de_usuario(): void
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'id_usuario' => 'ADM-TEST',
            'password' => 'password123',
        ]);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'status',
                'message',
                'data' => [
                    'token',
                    'token_type',
                    'expires_in',
                    'usuario' => [
                        'id_usuario',
                        'nombre_completo',
                        'rol',
                    ],
                ],
            ]);
    }

    public function test_login_fallido_retorna_mensaje_generico(): void
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'id_usuario' => 'ADM-TEST',
            'password' => 'password_incorrecto',
        ]);

        $response->assertStatus(401)
            ->assertJson([
                'status' => 'error',
                'message' => 'Credenciales inválidas.',
            ]);
    }

    public function test_bloqueo_al_tercer_intento_fallido_retorna_423(): void
    {
        // Intento 1
        $this->postJson('/api/v1/auth/login', ['id_usuario' => 'ADM-TEST', 'password' => 'err1']);
        // Intento 2
        $this->postJson('/api/v1/auth/login', ['id_usuario' => 'ADM-TEST', 'password' => 'err2']);
        // Intento 3 -> Bloqueo
        $response = $this->postJson('/api/v1/auth/login', ['id_usuario' => 'ADM-TEST', 'password' => 'err3']);

        $response->assertStatus(423)
            ->assertJsonStructure([
                'status',
                'message',
            ]);
    }

    public function test_acceso_sin_token_a_rutas_protegidas_retorna_401(): void
    {
        $response = $this->getJson('/api/v1/usuarios');

        $response->assertStatus(401);
    }

    public function test_acceso_con_rol_no_autorizado_retorna_403(): void
    {
        $medico = Usuario::where('id_usuario', 'MED-TEST')->first();
        $token = $medico->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/usuarios');

        $response->assertStatus(403)
            ->assertJson([
                'status' => 'error',
            ]);
    }

    public function test_alta_de_medico_sin_cedula_retorna_422(): void
    {
        $admin = Usuario::where('id_usuario', 'ADM-TEST')->first();
        $token = $admin->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/usuarios', [
                'id_usuario' => 'MED-NOCED',
                'nombre_completo' => 'Dr. Sin Cédula',
                'rol' => 'MEDICO',
                'password' => 'password123',
                'cedula' => null,
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['cedula']);
    }

    public function test_alta_de_medico_valida_retorna_201(): void
    {
        Usuario::where('id_usuario', 'MED-NEW')->delete();

        $admin = Usuario::where('id_usuario', 'ADM-TEST')->first();
        $token = $admin->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/usuarios', [
                'id_usuario' => 'MED-NEW',
                'nombre_completo' => 'Dr. Nuevo Valido',
                'rol' => 'MEDICO',
                'password' => 'password123',
                'cedula' => '9876543210',
            ]);

        $response->assertStatus(201)
            ->assertJson([
                'status' => 'success',
                'data' => [
                    'id_usuario' => 'MED-NEW',
                    'rol' => 'MEDICO',
                    'cedula' => '9876543210',
                ],
            ]);
    }

    public function test_baja_de_usuario_cambia_estado_a_baja(): void
    {
        $admin = Usuario::where('id_usuario', 'ADM-TEST')->first();
        $token = $admin->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/usuarios/MED-TEST/baja');

        $response->assertStatus(200)
            ->assertJson([
                'status' => 'success',
            ]);

        $this->assertEquals('BAJA', Usuario::where('id_usuario', 'MED-TEST')->value('estado_cuenta'));
    }

    public function test_refresh_token_retorna_nuevo_token_y_revoca_el_anterior(): void
    {
        $medico = Usuario::where('id_usuario', 'MED-TEST')->first();
        $token = $medico->createToken('test-token')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/auth/refresh');

        $response->assertStatus(200)
            ->assertJsonStructure([
                'status',
                'message',
                'data' => [
                    'token',
                    'token_type',
                    'expires_in',
                ],
            ]);

        $newToken = $response->json('data.token');
        $this->assertNotEmpty($newToken);
        $this->assertNotEquals($token, $newToken);

        // Olvidar guard en memoria de Laravel para forzar re-autenticacion por header
        app('auth')->forgetGuards();

        // Verificar que el token anterior ya no puede usarse
        $oldResponse = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/auth/me');
        $oldResponse->assertStatus(401);

        app('auth')->forgetGuards();

        // Verificar que el nuevo token es valido
        $newResponse = $this->withHeader('Authorization', 'Bearer ' . $newToken)
            ->getJson('/api/v1/auth/me');
        $newResponse->assertStatus(200);
    }
}

