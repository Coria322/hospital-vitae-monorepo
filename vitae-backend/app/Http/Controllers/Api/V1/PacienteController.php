<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Paciente;
use App\Services\AdmisionService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use InvalidArgumentException;
use Throwable;

class PacienteController extends Controller
{
    public function __construct(
        protected AdmisionService $admisionService
    ) {}

    /**
     * Registro de paciente en admisión (identificado o N.N. desconocido).
     * Conforme a NOM-004-SSA3-2012 y PROV-RN-007.
     */
    public function store(Request $request): JsonResponse
    {
        $identificado = $request->boolean('identificado', true);

        if ($identificado) {
            $request->validate([
                'curp' => 'required|string|size:18',
                'nombre_completo' => 'required|string|max:100',
                'fecha_nacimiento' => 'required|date',
                'sexo' => 'required|string|max:15',
                'domicilio' => 'nullable|string|max:200',
            ]);
        } else {
            $request->validate([
                'sexo' => 'nullable|string|max:15',
                'domicilio' => 'nullable|string|max:200',
            ]);
        }

        try {
            $paciente = $this->admisionService->registrarPaciente($request->all());

            return response()->json([
                'status' => 'success',
                'message' => $identificado
                    ? 'Paciente registrado exitosamente.'
                    : 'Paciente no identificado (N.N.) admitido temporalmente con folio de urgencias.',
                'data' => $paciente,
            ], 201);
        } catch (InvalidArgumentException $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Búsqueda y listado de pacientes activos.
     */
    public function index(Request $request): JsonResponse
    {
        $query = Paciente::where('estado_activo', true);

        if ($request->filled('q')) {
            $termino = '%' . $request->input('q') . '%';
            $query->where(function ($q) use ($termino) {
                $q->where('nombre_completo', 'ilike', $termino)
                  ->orWhere('curp', 'ilike', $termino)
                  ->orWhere('folio_urgencias', 'ilike', $termino);
            });
        }

        if ($request->has('identificado')) {
            $query->where('identificado', $request->boolean('identificado'));
        }

        $pacientes = $query->orderBy('created_at', 'desc')->paginate(20);

        return response()->json([
            'status' => 'success',
            'data' => $pacientes,
        ]);
    }

    /**
     * Detalle de un paciente con su historial clínico y triages.
     */
    public function show(string $id): JsonResponse
    {
        $paciente = Paciente::with(['expediente', 'triages.sala'])->findOrFail($id);

        return response()->json([
            'status' => 'success',
            'data' => $paciente,
        ]);
    }

    /**
     * Fusión autorizada de expediente de paciente N.N. con paciente definitivo (PROV-RN-007).
     */
    public function fusionar(Request $request, string $id): JsonResponse
    {
        $request->validate([
            'id_paciente_definitivo' => 'required|uuid|exists:paciente,id_paciente',
            'motivo' => 'required|string|min:10|max:500',
        ]);

        try {
            $resultado = $this->admisionService->fusionarPacientes(
                idTemporal: $id,
                idDefinitivo: $request->input('id_paciente_definitivo'),
                motivo: $request->input('motivo'),
                idUsuario: $request->user()?->id_usuario
            );

            return response()->json($resultado, 200);
        } catch (InvalidArgumentException $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        } catch (Throwable $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Fallo en la transacción de fusión: ' . $e->getMessage(),
            ], 500);
        }
    }
}
