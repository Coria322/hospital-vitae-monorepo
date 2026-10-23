<?php

use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes — Hospital Vitae SIH RESTful Gateway
|--------------------------------------------------------------------------
| Este servicio opera exclusivamente como API RESTful (NOM-024 / ISO-27001).
| Se eliminan vistas Blade para evitar fuga de información de frameworks y
| se proporcionan respuestas JSON estandarizadas y cabeceras de seguridad.
*/

Route::get('/', function () {
    return response()->json([
        'status' => 'online',
        'service' => 'Hospital Vitae SIH API Gateway',
        'version' => 'v1',
        'endpoints' => [
            'health' => '/up',
            'api' => '/api/v1',
        ],
        'timestamp' => now('UTC')->toIso8601String(),
    ], 200, [
        'X-Content-Type-Options' => 'nosniff',
        'X-Frame-Options' => 'DENY',
        'Cache-Control' => 'no-store, no-cache, must-revalidate',
    ]);
});

Route::fallback(function () {
    return response()->json([
        'status' => 'error',
        'message' => 'Recurso no encontrado. La API de Hospital Vitae opera bajo el prefijo /api/v1.',
    ], 404, [
        'X-Content-Type-Options' => 'nosniff',
        'X-Frame-Options' => 'DENY',
    ]);
});
