<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Consentimiento;
use App\Services\ConsentimientoService;
use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ConsentimientoController extends Controller
{
    public function __construct(
        protected ConsentimientoService $consentimientoService
    ) {}

    /**
     * Registro de consentimiento informado con hash SHA-256 inmutable (PROV-RN-008 / LFPDPPP).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'tipo' => 'required|string|max:50',
            'finalidades' => 'required|string|min:10',
            'firmante_nombre' => 'required|string|max:100',
            'medico_responsable_id' => 'required|string|exists:usuario,id_usuario',
            'version_documento' => 'nullable|string|max:10',
            'medio' => 'nullable|string|max:30',
            'firmante_parentesco' => 'nullable|string|max:50',
            'testigo_1' => 'nullable|string|max:100',
            'testigo_2' => 'nullable|string|max:100',
        ]);

        try {
            $consentimiento = $this->consentimientoService->registrarConsentimiento(
                idPaciente: $validated['id_paciente'],
                tipo: $validated['tipo'],
                finalidades: $validated['finalidades'],
                firmanteNombre: $validated['firmante_nombre'],
                medicoResponsableId: $validated['medico_responsable_id'],
                versionDocumento: $validated['version_documento'] ?? '1.0',
                medio: $validated['medio'] ?? 'DIGITAL_TABLET',
                firmanteParentesco: $validated['firmante_parentesco'] ?? null,
                testigo1: $validated['testigo_1'] ?? null,
                testigo2: $validated['testigo_2'] ?? null
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Consentimiento informado registrado exitosamente con hash criptográfico inmutable.',
                'data' => $consentimiento,
            ], 201);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Consulta de consentimientos por paciente.
     */
    public function index(Request $request): JsonResponse
    {
        $query = Consentimiento::with(['paciente', 'medicoResponsable']);

        if ($request->filled('id_paciente')) {
            $query->where('id_paciente', $request->input('id_paciente'));
        }

        return response()->json([
            'status' => 'success',
            'data' => $query->orderBy('fecha_hora', 'desc')->get(),
        ], 200);
    }

    /**
     * Revocación formal de consentimiento sin alterar el original (LFPDPPP).
     */
    public function revocar(Request $request, string $id): JsonResponse
    {
        $validated = $request->validate([
            'motivo_revocacion' => 'required|string|min:5',
            'firmante_nombre' => 'required|string|max:100',
        ]);

        try {
            $revocacion = $this->consentimientoService->revocarConsentimiento(
                idConsentimientoOriginal: $id,
                motivoRevocacion: $validated['motivo_revocacion'],
                firmanteNombre: $validated['firmante_nombre']
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Consentimiento informado revocado formalmente mediante registro nuevo inmutable enlazado.',
                'data' => $revocacion,
            ], 201);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Excepción de urgencia médica (exige >= 2 médicos autorizados).
     */
    public function urgenciaExcepcion(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'procedimiento' => 'required|string|min:5',
            'justificacion_urgencia' => 'required|string|min:10',
            'medico_1_id' => 'required|string|exists:usuario,id_usuario',
            'medico_2_id' => 'required|string|exists:usuario,id_usuario',
        ]);

        try {
            $excepcion = $this->consentimientoService->registrarExcepcionUrgencia(
                idPaciente: $validated['id_paciente'],
                procedimiento: $validated['procedimiento'],
                justificacionUrgencia: $validated['justificacion_urgencia'],
                medico1Id: $validated['medico_1_id'],
                medico2Id: $validated['medico_2_id']
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Excepción de consentimiento por urgencia médica autorizada y sellada por 2 médicos conforme a norma.',
                'data' => $excepcion,
            ], 201);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'code' => 'EXCEPCION_URGENCIA_INVALIDA',
                'message' => $e->getMessage(),
            ], 422);
        }
    }
}
