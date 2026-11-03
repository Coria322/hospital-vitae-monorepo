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

    // Endpoint normativo de alta disponibilidad y salud global de la API (RNF-004 / ISO/IEC-25010)
    Route::get('/salud', function () {
        $dbStatus = 'DISCONNECTED';
        try {
            \Illuminate\Support\Facades\DB::connection()->getPdo();
            $dbStatus = 'CONNECTED';
        } catch (\Throwable $e) {
            $dbStatus = 'ERROR';
        }

        $storageStatus = 'UNAVAILABLE';
        try {
            $testFile = 'healthcheck_probe_' . time() . '.tmp';
            \Illuminate\Support\Facades\Storage::disk('local')->put($testFile, 'probe');
            \Illuminate\Support\Facades\Storage::disk('local')->delete($testFile);
            $storageStatus = 'WRITABLE';
        } catch (\Throwable $e) {
            $storageStatus = 'ERROR';
        }

        $isHealthy = $dbStatus === 'CONNECTED' && $storageStatus === 'WRITABLE';

        return response()->json([
            'status' => $isHealthy ? 'UP' : 'DEGRADED',
            'database' => $dbStatus,
            'storage' => $storageStatus,
            'service' => 'hospital-vitae-api',
            'timestamp_utc' => now('UTC')->toIso8601String(),
            'alta_disponibilidad' => [
                'aislamiento_fallos' => true,
                'rto_objetivo_segundos' => 300,
            ],
        ], $isHealthy ? 200 : 503);
    });

    // TODO: [BORRADO / REMOVER TRAS AUDITORÍA FINAL] Endpoint temporal para verificación de sincronización horaria CENAM/NTP con Bruno (PROV-SEC-007)
    Route::get('/sistema/tiempo', function (\App\Services\NtpService $ntpService) {
        $diagnostico = $ntpService->medirDerivaHoraria();
        return response()->json([
            'status' => 'success',
            'timezone' => config('app.timezone'),
            'data' => $diagnostico,
        ]);
    });

    // Sala de espera pública disociada (sin PII, LFPDPPP / PROV-UX-002) y canal en tiempo real SSE (PROV-UX-001)
    Route::get('/sala-espera', [\App\Http\Controllers\Api\V1\SalaEsperaController::class, 'publica']);
    Route::get('/triage/stream', [\App\Http\Controllers\Api\V1\SalaEsperaController::class, 'stream']);

    // Auth protegida con Sanctum + verificación de cuenta activa
    Route::middleware(['auth:sanctum', 'account.active'])->group(function () {
        Route::post('/auth/logout', [AuthController::class, 'logout']);
        Route::post('/auth/refresh', [AuthController::class, 'refresh']);
        Route::get('/auth/me', [AuthController::class, 'me']);

        // Módulo de Pacientes y Admisión (NOM-004-SSA3-2012, PROV-RN-007)
        Route::post('/pacientes', [\App\Http\Controllers\Api\V1\PacienteController::class, 'store']);
        Route::get('/pacientes', [\App\Http\Controllers\Api\V1\PacienteController::class, 'index']);
        Route::get('/pacientes/{id}', [\App\Http\Controllers\Api\V1\PacienteController::class, 'show']);
        Route::post('/pacientes/{id}/fusiones', [\App\Http\Controllers\Api\V1\PacienteController::class, 'fusionar'])
            ->middleware('role:MEDICO,ADMINISTRADOR');

        // Módulo de Triage Manchester y Salas de Urgencias (NOM-206-SSA1-2002, RF-004, RF-005, RF-006, RF-007)
        Route::post('/triages', [\App\Http\Controllers\Api\V1\TriageController::class, 'store'])
            ->middleware('role:ENFERMERIA,ADMINISTRADOR');
        Route::get('/triages/cola', [\App\Http\Controllers\Api\V1\TriageController::class, 'cola']);
        Route::get('/triages/{id}', [\App\Http\Controllers\Api\V1\TriageController::class, 'show']);
        Route::post('/triages/{id}/reclasificaciones', [\App\Http\Controllers\Api\V1\TriageController::class, 'reclasificar'])
            ->middleware('role:MEDICO,ADMINISTRADOR');

        // Gestión de Salas de Atención y Liberación de Consultorios
        Route::get('/salas', [\App\Http\Controllers\Api\V1\TriageController::class, 'salas']);
        Route::post('/salas/{id}/liberar', [\App\Http\Controllers\Api\V1\TriageController::class, 'liberarSala'])
            ->middleware('role:MEDICO,ADMINISTRADOR,ENFERMERIA');

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
