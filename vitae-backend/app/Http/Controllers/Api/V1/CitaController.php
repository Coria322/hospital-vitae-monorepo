<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Cita;
use App\Models\Paciente;
use App\Services\AuditLogger;
use Carbon\Carbon;
use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class CitaController extends Controller
{
    /**
     * Listado de citas hospitalarias con filtros (PROV-RF-002 / Recepción).
     */
    public function index(Request $request): JsonResponse
    {
        $query = Cita::with(['paciente', 'medico']);

        if ($request->filled('id_paciente')) {
            $query->where('id_paciente', $request->query('id_paciente'));
        }

        if ($request->filled('estado')) {
            $query->where('estado', strtoupper($request->query('estado')));
        }

        if ($request->filled('fecha')) {
            $query->whereDate('fecha_hora', $request->query('fecha'));
        }

        $citas = $query->orderBy('fecha_hora', 'asc')->paginate(20);

        return response()->json([
            'status' => 'success',
            'data' => $citas,
        ], 200);
    }

    /**
     * Programación / Agendamiento de una nueva cita médica (PROV-RF-002).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'id_usuario' => 'nullable|string|exists:usuario,id_usuario',
            'fecha_hora' => 'required|date',
            'especialidad' => 'nullable|string|max:60',
            'motivo' => 'nullable|string|max:500',
            'consultorio' => 'nullable|string|max:30',
        ]);

        $usuario = Auth::user();

        // Si el usuario es un paciente autenticado, garantizar que solo pueda agendar para sí mismo
        if ($usuario && $usuario->rol === 'PACIENTE') {
            $pacientePropioId = $usuario->id_paciente ?? $usuario->id_usuario;
            if ($validated['id_paciente'] !== $pacientePropioId) {
                return response()->json([
                    'status' => 'error',
                    'code' => 'IDOR_DETECTED',
                    'message' => 'Acceso denegado: El paciente no puede programar citas para terceros.',
                ], 403);
            }
        }

        $fechaHoraUtc = Carbon::parse($validated['fecha_hora'], 'UTC');

        $cita = Cita::create([
            'id_paciente' => $validated['id_paciente'],
            'id_usuario' => $validated['id_usuario'] ?? null,
            'fecha_hora' => $fechaHoraUtc,
            'estado' => 'PENDIENTE',
            'especialidad' => $validated['especialidad'] ?? 'Medicina General',
            'motivo' => $validated['motivo'] ?? null,
            'consultorio' => $validated['consultorio'] ?? 'Consultorio 1',
        ]);

        AuditLogger::log(
            tipoMovimiento: 'CREACION_CITA',
            tablaAfectada: 'cita',
            registroId: $cita->id_cita,
            valorAnterior: null,
            valorNuevo: [
                'id_paciente' => $cita->id_paciente,
                'fecha_hora' => $cita->fecha_hora->toIso8601String(),
                'estado' => $cita->estado,
            ],
            resultado: 'EXITOSO',
            motivo: 'Agendamiento de cita médica hospitalaria (PROV-RF-002)',
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario ? $usuario->id_usuario : 'SISTEMA'
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Cita médica agendada exitosamente.',
            'data' => $cita->load(['paciente', 'medico']),
        ], 201);
    }

    /**
     * Consulta detallada de una cita médica.
     */
    public function show(string $id): JsonResponse
    {
        $cita = Cita::with(['paciente', 'medico', 'confirmadaPorUsuario'])->findOrFail($id);

        return response()->json([
            'status' => 'success',
            'data' => $cita,
        ], 200);
    }

    /**
     * Confirmación de cita médica por parte de Recepción (PROV-RF-002).
     */
    public function confirmar(string $id): JsonResponse
    {
        $cita = Cita::findOrFail($id);
        $usuario = Auth::user();

        // Validar caso borde: fecha ya expirada
        if ($cita->fecha_hora->isPast()) {
            return response()->json([
                'status' => 'error',
                'code' => 'CITA_EXPIRADA',
                'message' => 'No se puede confirmar una cita médica cuya fecha programada ya ha expirado.',
            ], 422);
        }

        $estadoAnterior = $cita->estado;

        try {
            $cita->confirmar($usuario->id_usuario);

            AuditLogger::log(
                tipoMovimiento: 'CONFIRMACION_CITA',
                tablaAfectada: 'cita',
                registroId: $cita->id_cita,
                valorAnterior: ['estado' => $estadoAnterior],
                valorNuevo: [
                    'estado' => $cita->estado,
                    'confirmada_por' => $cita->confirmada_por,
                    'confirmada_en' => $cita->confirmada_en->toIso8601String(),
                ],
                resultado: 'EXITOSO',
                motivo: 'Confirmación presencial/telefónica de cita médica por recepción (PROV-RF-002)',
                esNecesarioAuditoriaInterna: true,
                idUsuario: $usuario->id_usuario
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Cita confirmada correctamente.',
                'data' => $cita->fresh(['paciente', 'medico', 'confirmadaPorUsuario']),
            ], 200);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Cancelación de cita médica.
     */
    public function cancelar(Request $request, string $id): JsonResponse
    {
        $cita = Cita::findOrFail($id);
        $usuario = Auth::user();

        // Si es rol PACIENTE, verificar que sea el propietario
        if ($usuario && $usuario->rol === 'PACIENTE') {
            $pacientePropioId = $usuario->id_paciente ?? $usuario->id_usuario;
            if ($cita->id_paciente !== $pacientePropioId) {
                return response()->json([
                    'status' => 'error',
                    'code' => 'IDOR_DETECTED',
                    'message' => 'Acceso denegado: No puede cancelar citas de otros pacientes.',
                ], 403);
            }
        }

        $estadoAnterior = $cita->estado;
        $motivo = $request->input('motivo', 'Cancelada por el usuario');

        $cita->cancelar($motivo);

        AuditLogger::log(
            tipoMovimiento: 'CANCELACION_CITA',
            tablaAfectada: 'cita',
            registroId: $cita->id_cita,
            valorAnterior: ['estado' => $estadoAnterior],
            valorNuevo: ['estado' => 'CANCELADA', 'motivo' => $motivo],
            resultado: 'EXITOSO',
            motivo: "Cancelación de cita: {$motivo}",
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario ? $usuario->id_usuario : 'SISTEMA'
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Cita médica cancelada.',
            'data' => $cita,
        ], 200);
    }
}

