<?php

namespace Tests\Feature;

use App\Models\Auditoria;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AuditoriaTest extends TestCase
{
    use RefreshDatabase;

    public function test_puede_crear_una_auditoria_en_base_de_datos(): void
    {
        $payload = [
            'titulo' => 'Auditoría de Seguridad Informática',
            'descripcion' => 'Revisión de vulnerabilidades y controles de acceso',
            'auditor' => 'Juan Pérez',
            'estado' => 'en_proceso',
            'fecha_inicio' => '2026-10-10',
        ];

        $response = $this->postJson('/api/auditorias', $payload);

        $response->assertStatus(201)
                 ->assertJson([
                     'status' => 'success',
                     'message' => 'Auditoría creada correctamente en la base de datos',
                     'data' => [
                         'titulo' => 'Auditoría de Seguridad Informática',
                         'auditor' => 'Juan Pérez',
                     ]
                 ]);

        $this->assertDatabaseHas('auditorias', [
            'titulo' => 'Auditoría de Seguridad Informática',
            'auditor' => 'Juan Pérez',
        ]);
    }

    public function test_puede_listar_auditorias(): void
    {
        Auditoria::create([
            'titulo' => 'Auditoría Financiera',
            'auditor' => 'Ana Gómez',
            'estado' => 'pendiente'
        ]);

        $response = $this->getJson('/api/auditorias');

        $response->assertStatus(200)
                 ->assertJsonStructure([
                     'status',
                     'data' => [
                         '*' => ['id', 'titulo', 'auditor', 'estado', 'created_at', 'updated_at']
                     ]
                 ]);
    }

    public function test_puede_actualizar_una_auditoria(): void
    {
        $auditoria = Auditoria::create([
            'titulo' => 'Auditoría Inicial',
            'auditor' => 'Pedro Rivas',
            'estado' => 'pendiente'
        ]);

        $response = $this->putJson("/api/auditorias/{$auditoria->id}", [
            'estado' => 'completada'
        ]);

        $response->assertStatus(200)
                 ->assertJson([
                     'status' => 'success',
                     'data' => [
                         'estado' => 'completada'
                     ]
                 ]);

        $this->assertDatabaseHas('auditorias', [
            'id' => $auditoria->id,
            'estado' => 'completada'
        ]);
    }

    public function test_puede_eliminar_una_auditoria(): void
    {
        $auditoria = Auditoria::create([
            'titulo' => 'Auditoría a Borrar',
            'auditor' => 'Carlos M.',
            'estado' => 'pendiente'
        ]);

        $response = $this->deleteJson("/api/auditorias/{$auditoria->id}");

        $response->assertStatus(200);

        $this->assertDatabaseMissing('auditorias', [
            'id' => $auditoria->id
        ]);
    }
}
