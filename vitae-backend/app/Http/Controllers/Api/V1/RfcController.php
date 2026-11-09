<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\RfcCambio;
use App\Services\AuditLogger;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class RfcController extends Controller
{
    /**
     * Listado de Solicitudes Formales de Cambio (RFCs) post-liberación (PROV-RN-012).
     */
    public function index(Request $request): JsonResponse
    {
        $query = RfcCambio::query()->with(['solicitante', 'aprobador']);

        if ($request->filled('severidad')) {
            $query->where('severidad', strtoupper($request->query('severidad')));
        }

        if ($request->filled('estado')) {
            $query->where('estado', strtoupper($request->query('estado')));
        }

        $rfcs = $query->orderBy('created_at', 'desc')->paginate(20);

        return response()->json([
            'status' => 'success',
            'data' => $rfcs,
        ], 200);
    }

    /**
     * Registro formal de una Solicitud de Cambio (RFC) con análisis de impacto y rollback obligatorio.
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'titulo' => 'required|string|max:200',
            'descripcion' => 'required|string',
            'severidad' => 'required|in:P1,P2,P3',
            'modulo' => 'required|string|max:50',
            'impacto_clinico' => 'required|string',
            'analisis_riesgo' => 'required|string',
            'plan_rollback' => 'required|string|min:10',
            'tiempo_estimado_resolucion_horas' => 'nullable|integer|min:1',
        ]);

        $usuario = Auth::user();

        $rfc = RfcCambio::create([
            ...$validated,
            'estado' => 'EN_REVISION',
            'solicitante_id' => $usuario->id_usuario,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'CREACION_RFC',
            tablaAfectada: 'rfc_cambio',
            registroId: $rfc->id_rfc,
            valorAnterior: null,
            valorNuevo: [
                'codigo_rfc' => $rfc->codigo_rfc,
                'severidad' => $rfc->severidad,
                'modulo' => $rfc->modulo,
                'estado' => $rfc->estado,
                'sla_horas' => $rfc->tiempo_estimado_resolucion_horas,
            ],
            resultado: 'EXITOSO',
            motivo: "Registro formal de cambio post-liberación ISO/IEC-20000 ({$rfc->codigo_rfc})",
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Solicitud formal de cambio registrada exitosamente.',
            'data' => $rfc->load(['solicitante']),
        ], 201);
    }

    /**
     * Consulta detallada de una Solicitud de Cambio.
     */
    public function show(string $id): JsonResponse
    {
        $rfc = RfcCambio::with(['solicitante', 'aprobador'])
            ->where('id_rfc', $id)
            ->orWhere('codigo_rfc', $id)
            ->firstOrFail();

        return response()->json([
            'status' => 'success',
            'data' => $rfc,
        ], 200);
    }

    /**
     * Aprobación formal de un RFC por el Comité de Cambios / Administrador / PO (PROV-RN-012).
     */
    public function aprobar(string $id): JsonResponse
    {
        $usuario = Auth::user();
        if ($usuario->rol !== 'ADMINISTRADOR') {
            return response()->json([
                'status' => 'error',
                'codigo' => 'ROL_NO_AUTORIZADO',
                'message' => 'Solo el Administrador del Sistema o Product Owner puede aprobar un RFC.',
            ], 403);
        }

        $rfc = RfcCambio::where('id_rfc', $id)->orWhere('codigo_rfc', $id)->firstOrFail();

        $estadoPrevio = $rfc->estado;
        $rfc->aprobar($usuario->id_usuario);

        AuditLogger::log(
            tipoMovimiento: 'APROBACION_RFC',
            tablaAfectada: 'rfc_cambio',
            registroId: $rfc->id_rfc,
            valorAnterior: ['estado' => $estadoPrevio],
            valorNuevo: [
                'estado' => $rfc->estado,
                'aprobado_por' => $usuario->id_usuario,
                'fecha_aprobacion' => $rfc->fecha_aprobacion->toIso8601String(),
            ],
            resultado: 'EXITOSO',
            motivo: "Aprobación formal de RFC {$rfc->codigo_rfc} severidad {$rfc->severidad}",
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'message' => "RFC {$rfc->codigo_rfc} aprobado formalmente para aplicación.",
            'data' => $rfc->load(['solicitante', 'aprobador']),
        ], 200);
    }

    /**
     * Rechazo fundamentado de un RFC.
     */
    public function rechazar(string $id, Request $request): JsonResponse
    {
        $usuario = Auth::user();
        if ($usuario->rol !== 'ADMINISTRADOR') {
            return response()->json([
                'status' => 'error',
                'codigo' => 'ROL_NO_AUTORIZADO',
                'message' => 'Solo el Administrador del Sistema puede rechazar un RFC.',
            ], 403);
        }

        $rfc = RfcCambio::where('id_rfc', $id)->orWhere('codigo_rfc', $id)->firstOrFail();
        $motivo = $request->input('motivo', 'Rechazo fundamentado por análisis de riesgo o viabilidad.');

        $estadoPrevio = $rfc->estado;
        $rfc->rechazar($usuario->id_usuario);

        AuditLogger::log(
            tipoMovimiento: 'RECHAZO_RFC',
            tablaAfectada: 'rfc_cambio',
            registroId: $rfc->id_rfc,
            valorAnterior: ['estado' => $estadoPrevio],
            valorNuevo: [
                'estado' => $rfc->estado,
                'rechazado_por' => $usuario->id_usuario,
                'motivo_rechazo' => $motivo,
            ],
            resultado: 'EXITOSO',
            motivo: "Rechazo de RFC {$rfc->codigo_rfc}: {$motivo}",
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'message' => "RFC {$rfc->codigo_rfc} rechazado.",
            'data' => $rfc->load(['solicitante', 'aprobador']),
        ], 200);
    }
}
