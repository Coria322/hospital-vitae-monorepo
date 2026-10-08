<?php

use App\Http\Controllers\AuditoriaController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

// Rutas CRUD para la entidad Auditoria en Base de Datos
Route::apiResource('auditorias', AuditoriaController::class);

// GET /api/ping - Check de estado
Route::get('/ping', function () {
    return response()->json([
        'status' => 'success',
        'message' => 'API is running',
        'timestamp' => now()->toIso8601String()
    ]);
});

// GET /api/hello - Hello World básico
Route::get('/hello', function () {
    return response()->json([
        'message' => '¡Hola Mundo! (GET request exitoso)',
        'status' => 200
    ]);
});

// POST /api/hello - Recibe datos (ej. {"name": "TuNombre"})
Route::post('/hello', function (Request $request) {
    $name = $request->input('name', 'Mundo');

    return response()->json([
        'message' => "¡Hola, {$name}! (POST request exitoso)",
        'received_data' => $request->all(),
        'status' => 201
    ], 201);
});

// PUT /api/hello/{id} - Actualización simulada por ID
Route::put('/hello/{id}', function (Request $request, $id) {
    return response()->json([
        'message' => "Elemento con ID {$id} actualizado (PUT request exitoso)",
        'id' => $id,
        'updated_fields' => $request->all(),
        'status' => 200
    ]);
});

// DELETE /api/hello/{id} - Eliminación simulada por ID
Route::delete('/hello/{id}', function ($id) {
    return response()->json([
        'message' => "Elemento con ID {$id} eliminado (DELETE request exitoso)",
        'id' => $id,
        'status' => 200
    ]);
});

Route::get('/user', function (Request $request) {
    return $request->user();
})->middleware('auth:sanctum');

