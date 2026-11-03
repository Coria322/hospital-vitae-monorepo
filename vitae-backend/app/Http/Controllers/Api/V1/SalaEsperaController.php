<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Services\ColaUrgenciasService;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpFoundation\StreamedResponse;

class SalaEsperaController extends Controller
{
    public function __construct(
        protected ColaUrgenciasService $colaService
    ) {}

    /**
     * Pantalla de sala de espera pública disociada (PROV-UX-002 / LFPDPPP).
     * Garantiza 0 campos de PII (sin nombres completos, diagnósticos ni CURP).
     * Solo iniciales, folio, nivel Manchester con color e icono para accesibilidad.
     */
    public function publica(): JsonResponse
    {
        $datos = $this->colaService->obtenerSalaEsperaPublica();

        return response()->json([
            'status' => 'success',
            'data' => $datos,
        ]);
    }

    /**
     * Canal SSE (Server-Sent Events) en tiempo real para actualización de pantallas de urgencias (PROV-UX-001).
     */
    public function stream(): StreamedResponse
    {
        $colaService = $this->colaService;

        return response()->stream(function () use ($colaService) {
            $datos = $colaService->obtenerSalaEsperaPublica();
            echo "event: sala_actualizada\n";
            echo 'data: ' . json_encode($datos) . "\n\n";
            ob_flush();
            flush();
        }, 200, [
            'Content-Type' => 'text/event-stream',
            'Cache-Control' => 'no-cache',
            'Connection' => 'keep-alive',
            'X-Accel-Buffering' => 'no',
        ]);
    }
}
