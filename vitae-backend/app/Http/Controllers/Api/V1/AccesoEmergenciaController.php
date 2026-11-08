<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\AccesoEmergencia;
use App\Services\BreakTheGlassService;
use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class AccesoEmergenciaController extends Controller
{
    public function __construct(
        protected BreakTheGlassService $breakTheGlassService
    ) {}

    /**
     * Activación justificada del protocolo de emergencia Break-the-Glass (PROV-RN-010 / NOM-004 / ISO-27001).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'motivo' => 'required|string|min:15',
            'duracion_minutos' => 'nullable|integer|min:15|max:480',
        ]);

        $usuario = Auth::user();

        try {
            $acceso = $this->breakTheGlassService->otorgarAccesoEmergencia(
                medico: $usuario,
                idPaciente: $validated['id_paciente'],
                motivo: $validated['motivo'],
                duracionMinutos: $validated['duracion_minutos'] ?? BreakTheGlassService::DURACION_DEFAULT_MINUTOS
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Protocolo Break-the-Glass activado. Acceso de emergencia concedido por 120 minutos con alerta crítica de auditoría interna.',
                'data' => $acceso,
            ], 201);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'code' => 'BREAK_THE_GLASS_ERROR',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Consulta de accesos de emergencia registrados para auditoría de seguridad.
     */
    public function index(): JsonResponse
    {
        $accesos = AccesoEmergencia::with(['usuario', 'paciente'])
            ->orderBy('created_at', 'desc')
            ->paginate(20);

        return response()->json([
            'status' => 'success',
            'data' => $accesos,
        ], 200);
    }
}
