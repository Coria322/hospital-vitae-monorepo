<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\SolicitudArco;
use App\Services\AuditLogger;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class SolicitudArcoController extends Controller
{
    /**
     * Registro de solicitud de ejercicio de derechos ARCO con cómputo de días hábiles (PROV-RN-009 / LFPDPPP).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'derecho' => 'required|string|in:ACCESO,RECTIFICACION,CANCELACION,OPOSICION',
            'acreditacion_identidad' => 'required|boolean',
            'documento_acreditacion' => 'nullable|string|max:255',
            'observaciones' => 'nullable|string',
        ]);

        if (!$validated['acreditacion_identidad']) {
            return response()->json([
                'status' => 'error',
                'code' => 'IDENTIDAD_NO_ACREDITADA',
                'message' => 'LFPDPPP: Es requisito legal indispensable que el titular o su representante acredite fehacientemente su identidad para dar trámite a solicitudes ARCO.',
            ], 422);
        }

        $fechaRecepcion = Carbon::now('UTC');

        // Cómputo de plazos en días hábiles (Lunes a Viernes)
        // Respuesta legal: <= 20 días hábiles
        $respuestaLimite = $fechaRecepcion->copy()->addWeekdays(20);
        // Ejecución legal posterior: <= 15 días hábiles adicionales
        $ejecucionLimite = $respuestaLimite->copy()->addWeekdays(15);

        $folio = 'ARCO-' . $fechaRecepcion->format('Ymd') . '-' . strtoupper(Str::random(4));

        $solicitud = SolicitudArco::create([
            'folio' => $folio,
            'id_paciente' => $validated['id_paciente'],
            'derecho' => $validated['derecho'],
            'fecha_recepcion' => $fechaRecepcion->toDateString(),
            'respuesta_limite' => $respuestaLimite->toDateString(),
            'ejecucion_limite' => $ejecucionLimite->toDateString(),
            'estado' => 'RECIBIDA',
            'motivo_improcedencia' => null,
            'acreditacion_identidad' => true,
            'documento_acreditacion' => $validated['documento_acreditacion'] ?? null,
            'observaciones' => $validated['observaciones'] ?? null,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'solicitud_arco',
            registroId: $solicitud->folio,
            valorAnterior: null,
            valorNuevo: [
                'folio' => $folio,
                'derecho' => $solicitud->derecho,
                'fecha_recepcion' => $solicitud->fecha_recepcion,
                'respuesta_limite' => $solicitud->respuesta_limite,
                'ejecucion_limite' => $solicitud->ejecucion_limite,
            ],
            resultado: 'EXITOSO',
            motivo: "Registro formal de solicitud ARCO derecho {$solicitud->derecho} para paciente {$solicitud->id_paciente}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Solicitud ARCO radicada formalmente con cómputo de plazos hábiles conforme a LFPDPPP.',
            'data' => $solicitud,
        ], 201);
    }

    /**
     * Consulta y listado de solicitudes ARCO.
     */
    public function index(Request $request): JsonResponse
    {
        $query = SolicitudArco::with('paciente');

        if ($request->filled('id_paciente')) {
            $query->where('id_paciente', $request->input('id_paciente'));
        }

        if ($request->filled('derecho')) {
            $query->where('derecho', $request->input('derecho'));
        }

        if ($request->filled('estado')) {
            $query->where('estado', $request->input('estado'));
        }

        return response()->json([
            'status' => 'success',
            'data' => $query->orderBy('fecha_recepcion', 'desc')->get(),
        ], 200);
    }

    /**
     * Resolución de solicitud ARCO (evaluación de procedencia vs retención legal NOM-004 de 5 años).
     */
    public function resolver(Request $request, string $folio): JsonResponse
    {
        $solicitud = SolicitudArco::where('folio', $folio)->firstOrFail();

        $expediente = ExpedienteClinico::where('id_paciente', $solicitud->id_paciente)->first();

        // Escenario: Cancelación de datos clínicos durante período de retención legal obligatoria
        if ($solicitud->derecho === 'CANCELACION') {
            if ($expediente && !$expediente->tieneRetencionConcluida()) {
                $motivoImprocedencia = 'IMPROCEDENTE: De conformidad con el artículo 26 de la LFPDPPP y el numeral 5.4 de la NOM-004-SSA3-2012, no procede la cancelación o supresión de datos del expediente clínico debido a la obligación legal y sanitaria de conservar la información durante un plazo mínimo obligatorio de 5 años (1 825 días) posteriores al último acto médico.';

                $solicitud->update([
                    'estado' => 'IMPROCEDENTE',
                    'motivo_improcedencia' => $motivoImprocedencia,
                ]);

                AuditLogger::log(
                    tipoMovimiento: 'RESOLUCION_ARCO',
                    tablaAfectada: 'solicitud_arco',
                    registroId: $solicitud->folio,
                    valorAnterior: ['estado' => 'RECIBIDA'],
                    valorNuevo: [
                        'estado' => 'IMPROCEDENTE',
                        'fundamento' => 'NOM-004-SSA3-2012 / LFPDPPP Art 26',
                    ],
                    resultado: 'EXITOSO',
                    motivo: "Resolución de solicitud ARCO {$folio}: Cancelación improcedente por retención sanitaria obligatoria de 5 años"
                );

                return response()->json([
                    'status' => 'success',
                    'message' => 'Solicitud ARCO resuelta formalmente como improcedente con fundamento legal.',
                    'data' => $solicitud,
                ], 200);
            }
        }

        // Para otros derechos o cancelaciones post-retención
        $nuevoEstado = $request->input('estado', 'PROCEDENTE');
        $motivo = $request->input('motivo', 'Solicitud procedente y ejecutada conforme a normativa.');

        $solicitud->update([
            'estado' => $nuevoEstado,
            'motivo_improcedencia' => $nuevoEstado === 'IMPROCEDENTE' ? $motivo : null,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'RESOLUCION_ARCO',
            tablaAfectada: 'solicitud_arco',
            registroId: $solicitud->folio,
            valorAnterior: null,
            valorNuevo: ['estado' => $nuevoEstado],
            resultado: 'EXITOSO',
            motivo: "Resolución de solicitud ARCO {$folio} a estado {$nuevoEstado}"
        );

        return response()->json([
            'status' => 'success',
            'message' => "Solicitud ARCO resuelta con estado {$nuevoEstado}.",
            'data' => $solicitud,
        ], 200);
    }
}
