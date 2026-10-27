<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\AuditoriaI;
use App\Services\AuditLogger;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AuditoriaIController extends Controller
{
    /**
     * Consulta de bitácora inalterable AUDITORIA_I (NOM-024 / RF-001).
     * Solo lectura para rol ADMINISTRADOR con auto-registro del evento.
     */
    public function index(Request $request): JsonResponse
    {
        $query = AuditoriaI::query();

        // Filtro por rango de fechas (UTC)
        if ($request->filled('desde')) {
            $query->where('fecha_hora', '>=', $request->input('desde'));
        }
        if ($request->filled('hasta')) {
            $query->where('fecha_hora', '<=', $request->input('hasta'));
        }

        // Filtros específicos
        if ($request->filled('id_usuario')) {
            $query->where('id_usuario', $request->input('id_usuario'));
        }
        if ($request->filled('tabla_afectada')) {
            $query->where('tabla_afectada', $request->input('tabla_afectada'));
        }
        if ($request->filled('tipo_movimiento')) {
            $query->where('tipo_movimiento', strtoupper($request->input('tipo_movimiento')));
        }
        if ($request->filled('resultado')) {
            $query->where('resultado', strtoupper($request->input('resultado')));
        }

        $perPage = min(max((int) $request->input('per_page', 15), 1), 100);
        $registros = $query->orderBy('id_auditoria', 'desc')->paginate($perPage);

        // Auto-auditoría obligatoria: cada consulta genera su propio evento
        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_AUDITORIA',
            tablaAfectada: 'auditoria_i',
            registroId: 'QUERY',
            valorAnterior: null,
            valorNuevo: $request->only(['desde', 'hasta', 'id_usuario', 'tabla_afectada', 'tipo_movimiento', 'resultado']),
            resultado: 'EXITOSO',
            motivo: 'Consulta de bitácora de auditoría por administrador'
        );

        return response()->json([
            'status' => 'success',
            'data' => $registros,
        ], 200);
    }

    /**
     * Invariante de inalterabilidad: Prohibición de escritura directa en auditoría.
     * Retorna HTTP 405 Method Not Allowed.
     */
    public function methodNotAllowed(): JsonResponse
    {
        return response()->json([
            'status' => 'error',
            'message' => 'Método no permitido: La bitácora AUDITORIA_I es de solo lectura y no admite modificaciones directas (NOM-024-SSA3-2012).',
        ], 405);
    }
}
