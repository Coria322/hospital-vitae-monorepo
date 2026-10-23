<?php

namespace Tests\Feature;

use Tests\TestCase;

class ExampleTest extends TestCase
{
    /**
     * Valida que la raíz del gateway retorne estado en JSON con cabeceras de seguridad.
     */
    public function test_the_application_returns_a_successful_response(): void
    {
        $response = $this->get('/');

        $response->assertStatus(200)
            ->assertHeader('X-Content-Type-Options', 'nosniff')
            ->assertHeader('X-Frame-Options', 'DENY')
            ->assertJson([
                'status' => 'online',
                'service' => 'Hospital Vitae SIH API Gateway',
                'version' => 'v1',
            ]);
    }

    /**
     * Valida que rutas no definidas en la web retornen 404 estandarizado en JSON sin fugar información del servidor.
     */
    public function test_ruta_no_existente_retorna_404_json_seguro(): void
    {
        $response = $this->get('/ruta-inexistente');

        $response->assertStatus(404)
            ->assertHeader('X-Content-Type-Options', 'nosniff')
            ->assertHeader('X-Frame-Options', 'DENY')
            ->assertJson([
                'status' => 'error',
            ]);
    }
}
