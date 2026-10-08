<?php

namespace Tests\Feature;

use Tests\TestCase;

class ApiEndpointsTest extends TestCase
{
    public function test_ping_endpoint(): void
    {
        $response = $this->getJson('/api/ping');

        $response->assertStatus(200)
                 ->assertJson([
                     'status' => 'success',
                     'message' => 'API is running',
                 ]);
    }

    public function test_hello_get_endpoint(): void
    {
        $response = $this->getJson('/api/hello');

        $response->assertStatus(200)
                 ->assertJson([
                     'status' => 200,
                     'message' => '¡Hola Mundo! (GET request exitoso)',
                 ]);
    }

    public function test_hello_post_endpoint(): void
    {
        $response = $this->postJson('/api/hello', ['name' => 'Maria']);

        $response->assertStatus(201)
                 ->assertJson([
                     'status' => 201,
                     'message' => '¡Hola, Maria! (POST request exitoso)',
                 ]);
    }

    public function test_hello_put_endpoint(): void
    {
        $response = $this->putJson('/api/hello/5', ['role' => 'admin']);

        $response->assertStatus(200)
                 ->assertJson([
                     'status' => 200,
                     'id' => '5',
                 ]);
    }

    public function test_hello_delete_endpoint(): void
    {
        $response = $this->deleteJson('/api/hello/5');

        $response->assertStatus(200)
                 ->assertJson([
                     'status' => 200,
                     'id' => '5',
                 ]);
    }
}
