<?php

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\UsuarioController;
use App\Http\Controllers\AuditoriaController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

// Health check
Route::get('/ping', function () {
    return response()->json([
        'status' => 'success',
        'message' => 'API is running',
        'timestamp' => now()->toIso8601String(),
    ]);
});

// Hello World endpoints
Route::get('/hello', function () {
    return response()->json([
        'message' => '¡Hola Mundo! (GET request exitoso)',
        'status' => 200,
    ]);
});

Route::post('/hello', function (Request $request) {
    $name = $request->input('name', 'Mundo');
    return response()->json([
        'message' => "¡Hola, {$name}! (POST request exitoso)",
        'received_data' => $request->all(),
        'status' => 201,
    ], 201);
});

Route::put('/hello/{id}', function (Request $request, $id) {
    return response()->json([
        'message' => "Elemento con ID {$id} actualizado (PUT request exitoso)",
        'id' => $id,
        'updated_fields' => $request->all(),
        'status' => 200,
    ]);
});

Route::delete('/hello/{id}', function ($id) {
    return response()->json([
        'message' => "Elemento con ID {$id} eliminado (DELETE request exitoso)",
        'id' => $id,
        'status' => 200,
    ]);
});

// Rutas de Auditoría existentes
Route::apiResource('auditorias', AuditoriaController::class);

// API v1: Prefijo oficial para Hospital Vitae
Route::prefix('v1')->group(function () {
    // Auth pública
    Route::post('/auth/login', [AuthController::class, 'login']);

    // Auth protegida con Sanctum + verificación de cuenta activa
    Route::middleware(['auth:sanctum', 'account.active'])->group(function () {
        Route::post('/auth/logout', [AuthController::class, 'logout']);
        Route::post('/auth/refresh', [AuthController::class, 'refresh']);
        Route::get('/auth/me', [AuthController::class, 'me']);

        // Endpoints administrativos protegidos por rol ADMINISTRADOR
        Route::middleware(['role:ADMINISTRADOR'])->group(function () {
            Route::get('/usuarios', [UsuarioController::class, 'index']);
            Route::post('/usuarios', [UsuarioController::class, 'store']);
            Route::get('/usuarios/{id}', [UsuarioController::class, 'show']);
            Route::post('/usuarios/{id}/baja', [UsuarioController::class, 'baja']);
            Route::post('/usuarios/{id}/reactivar', [UsuarioController::class, 'reactivar']);

            // Bitácora inalterable AUDITORIA_I (NOM-024 / AI-02)
            Route::get('/auditoria', [\App\Http\Controllers\Api\V1\AuditoriaIController::class, 'index']);
            Route::match(['post', 'put', 'patch', 'delete'], '/auditoria/{id?}', [\App\Http\Controllers\Api\V1\AuditoriaIController::class, 'methodNotAllowed']);
        });
    });
});
