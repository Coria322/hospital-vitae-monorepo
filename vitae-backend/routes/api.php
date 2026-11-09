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

    // Interoperabilidad HL7 FHIR R4 (RNF-007 / NOM-024)
    Route::prefix('fhir')->group(function () {
        Route::get('/Patient/{id}', [\App\Http\Controllers\Api\V1\FhirController::class, 'getPatient']);
        Route::get('/Encounter/{id}', [\App\Http\Controllers\Api\V1\FhirController::class, 'getEncounter']);
        Route::get('/Observation/{id}', [\App\Http\Controllers\Api\V1\FhirController::class, 'getObservation']);
        Route::get('/MedicationRequest/{id}', [\App\Http\Controllers\Api\V1\FhirController::class, 'getMedicationRequest']);
    });

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
            ->middleware('role:MEDICO,ADMINISTRADOR,ENFERMERIA');
        Route::post('/triages/{id}/egreso', [\App\Http\Controllers\Api\V1\TriageController::class, 'egreso'])
            ->middleware('role:ENFERMERIA,MEDICO,ADMINISTRADOR');

        // Gestión de Salas de Atención y Liberación de Consultorios
        Route::get('/salas', [\App\Http\Controllers\Api\V1\TriageController::class, 'salas']);
        Route::post('/salas/{id}/liberar', [\App\Http\Controllers\Api\V1\TriageController::class, 'liberarSala'])
            ->middleware('role:MEDICO,ADMINISTRADOR,ENFERMERIA');

        // SPRINT 4: Expediente Clínico Electrónico (ECE) y Notas Médicas (RF-008, RF-008.1, RF-008.2 / NOM-004)
        Route::post('/expedientes-clinicos', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'store'])
            ->middleware('role:MEDICO,ADMINISTRADOR');
        Route::get('/expedientes-clinicos/{id}', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'show']);
        Route::post('/expedientes-clinicos/{id}/antecedentes', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'storeAntecedente'])
            ->middleware('role:MEDICO,ADMINISTRADOR');
        Route::post('/expedientes-clinicos/{id}/notas', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'storeNota'])
            ->middleware('role:MEDICO');
        Route::post('/expedientes-clinicos/{id}/notas/{nid}/aclaraciones', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'storeAclaracion'])
            ->middleware('role:MEDICO');
        Route::match(['put', 'patch', 'delete'], '/expedientes-clinicos/{id}/notas/{nid}', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'bloquearModificacionNota']);
        Route::post('/expedientes-clinicos/{id}/exportaciones', [\App\Http\Controllers\Api\V1\ExpedienteClinicoController::class, 'exportarPdf']);

        // SPRINT 4: Recetas Médicas Controladas (RF-009 / COFEPRIS / SAT)
        Route::post('/recetas', [\App\Http\Controllers\Api\V1\RecetaController::class, 'store'])
            ->middleware('role:MEDICO');
        Route::post('/recetas/{id}/firma', [\App\Http\Controllers\Api\V1\RecetaController::class, 'firmar'])
            ->middleware('role:MEDICO');
        Route::get('/recetas/{id}', [\App\Http\Controllers\Api\V1\RecetaController::class, 'show']);
        Route::post('/recetas/{folio}/surtir', [\App\Http\Controllers\Api\V1\RecetaController::class, 'surtir']);
        Route::get('/recetas/{id}/pdf', [\App\Http\Controllers\Api\V1\RecetaController::class, 'descargarPdf']);

        // SPRINT 4: Solicitudes y Resultados de Laboratorio e Imagen (RF-010 / RNF-003)
        Route::post('/solicitudes-estudio', [\App\Http\Controllers\Api\V1\LaboratorioController::class, 'storeSolicitud'])
            ->middleware('role:MEDICO');
        Route::get('/solicitudes-estudio', [\App\Http\Controllers\Api\V1\LaboratorioController::class, 'indexSolicitudes']);
        Route::post('/solicitudes-estudio/{id}/resultados', [\App\Http\Controllers\Api\V1\LaboratorioController::class, 'storeResultado'])
            ->middleware('role:LABORATORISTA,ADMINISTRADOR');
        Route::get('/solicitudes-estudio/{id}/resultados', [\App\Http\Controllers\Api\V1\LaboratorioController::class, 'showResultado']);

        // SPRINT 4: Consentimiento Informado (PROV-RN-008 / LFPDPPP)
        Route::post('/consentimientos', [\App\Http\Controllers\Api\V1\ConsentimientoController::class, 'store']);
        Route::get('/consentimientos', [\App\Http\Controllers\Api\V1\ConsentimientoController::class, 'index']);
        Route::post('/consentimientos/{id}/revocaciones', [\App\Http\Controllers\Api\V1\ConsentimientoController::class, 'revocar']);
        Route::post('/consentimientos/urgencia-excepcion', [\App\Http\Controllers\Api\V1\ConsentimientoController::class, 'urgenciaExcepcion']);

        // SPRINT 4: Derechos ARCO (PROV-RN-009 / LFPDPPP)
        Route::post('/solicitudes-arco', [\App\Http\Controllers\Api\V1\SolicitudArcoController::class, 'store']);
        Route::get('/solicitudes-arco', [\App\Http\Controllers\Api\V1\SolicitudArcoController::class, 'index']);
        Route::post('/solicitudes-arco/{folio}/resolver', [\App\Http\Controllers\Api\V1\SolicitudArcoController::class, 'resolver'])
            ->middleware('role:ADMINISTRADOR');

        // SPRINT 4: Protocolo Break-the-Glass de Acceso de Emergencia (PROV-RN-010 / NOM-004)
        Route::post('/accesos-emergencia', [\App\Http\Controllers\Api\V1\AccesoEmergenciaController::class, 'store'])
            ->middleware('role:MEDICO');
        Route::get('/accesos-emergencia', [\App\Http\Controllers\Api\V1\AccesoEmergenciaController::class, 'index'])
            ->middleware('role:ADMINISTRADOR,MEDICO');

        // SPRINT 5: Agenda y Confirmación de Citas Médicas (PROV-RF-002 / Recepción)
        Route::get('/citas', [\App\Http\Controllers\Api\V1\CitaController::class, 'index'])
            ->middleware('role:RECEPCION,ADMINISTRADOR,MEDICO');
        Route::post('/citas', [\App\Http\Controllers\Api\V1\CitaController::class, 'store'])
            ->middleware('role:RECEPCION,ADMINISTRADOR');
        Route::get('/citas/{id}', [\App\Http\Controllers\Api\V1\CitaController::class, 'show'])
            ->middleware('role:RECEPCION,ADMINISTRADOR,MEDICO');
        Route::patch('/citas/{id}/confirmacion', [\App\Http\Controllers\Api\V1\CitaController::class, 'confirmar'])
            ->middleware('role:RECEPCION,ADMINISTRADOR');
        Route::patch('/citas/{id}/cancelacion', [\App\Http\Controllers\Api\V1\CitaController::class, 'cancelar'])
            ->middleware('role:RECEPCION,ADMINISTRADOR');

        // SPRINT 5: Tableros Estadísticos NOM-035 con Cero PII y CLUES (PROV-RF-001 / NOM-035-STPS-2018)
        Route::get('/estadisticas/tableros', [\App\Http\Controllers\Api\V1\EstadisticaController::class, 'tableros'])
            ->middleware('role:ADMINISTRADOR');
        Route::get('/estadisticas/exportar', [\App\Http\Controllers\Api\V1\EstadisticaController::class, 'exportar'])
            ->middleware('role:ADMINISTRADOR');

        // SPRINT 5: Portal Móvil del Paciente (RF-011, RF-002.1, RNF-006 / Android 10+ / Anti-IDOR)
        Route::prefix('mobile')->middleware(['mobile.version', 'mobile.isolation'])->group(function () {
            Route::get('/perfil', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'perfil']);
            Route::get('/citas', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'citas']);
            Route::post('/citas', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'solicitarCita']);
            Route::patch('/citas/{id}/cancelar', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'cancelarCita']);
            Route::get('/recetas', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'recetas']);
            Route::get('/resultados', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'resultados']);
            Route::get('/expediente', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'expediente']);
            Route::post('/consentimientos', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'consentimiento']);
            Route::post('/solicitudes-arco', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'solicitudArco']);

            // Interceptores normativos de bloqueo a mutaciones clínicas desde móvil (RF-011)
            Route::match(['post', 'put', 'patch', 'delete'], '/recetas/{id?}', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'bloquearMutacionClinica']);
            Route::match(['post', 'put', 'patch', 'delete'], '/resultados/{id?}', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'bloquearMutacionClinica']);
            Route::match(['post', 'put', 'patch', 'delete'], '/expediente/{id?}', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'bloquearMutacionClinica']);
            Route::match(['post', 'put', 'patch', 'delete'], '/notas/{id?}', [\App\Http\Controllers\Api\V1\MobileApiController::class, 'bloquearMutacionClinica']);
        });

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
