<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Paciente;
use App\Services\AdmisionService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
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
        $query = Paciente::with(['expediente', 'triages.sala'])->where('estado_activo', true);

        if ($request->filled('q')) {
            $rawTerm = trim((string) $request->input('q'));
            $cleanTerm = trim(preg_replace('/^#?EXP[-_#\s]*/i', '', $rawTerm));
            $termino = '%' . $rawTerm . '%';
            $terminoClean = '%' . ($cleanTerm ?: $rawTerm) . '%';
            $palabras = array_values(array_filter(preg_split('/\s+/', $rawTerm)));
            $cleanCurp = strtoupper(preg_replace('/[^A-Za-z0-9]/', '', $rawTerm));

            $driver = DB::getDriverName();
            $likeOp = $driver === 'pgsql' ? 'ilike' : 'like';
            $castUuid = $driver === 'pgsql' ? 'CAST(id_paciente AS TEXT)' : 'id_paciente';

            $query->where(function ($q) use ($termino, $terminoClean, $castUuid, $likeOp, $palabras, $cleanCurp) {
                $q->where('nombre_completo', $likeOp, $termino)
                  ->orWhere('nombre_completo', $likeOp, $terminoClean)
                  ->orWhere('curp', $likeOp, $termino)
                  ->orWhere('curp', $likeOp, $terminoClean)
                  ->orWhere('folio_urgencias', $likeOp, $termino)
                  ->orWhere('folio_urgencias', $likeOp, $terminoClean)
                  ->orWhereRaw("{$castUuid} {$likeOp} ?", [$termino])
                  ->orWhereRaw("{$castUuid} {$likeOp} ?", [$terminoClean])
                  ->orWhereHas('expediente', function ($eq) use ($termino, $terminoClean, $likeOp) {
                      $eq->where('id_expediente', $likeOp, $termino)
                         ->orWhere('id_expediente', $likeOp, $terminoClean);
                  });

                if (strlen($cleanCurp) >= 8) {
                    $q->orWhere('curp', $likeOp, '%' . $cleanCurp . '%');
                }

                if (count($palabras) > 1) {
                    $q->orWhere(function ($subQ) use ($palabras, $likeOp) {
                        foreach ($palabras as $p) {
                            $subQ->where('nombre_completo', $likeOp, '%' . $p . '%');
                        }
                    });
                }
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
     * Restringido a personal asistencial autorizado conforme a la Matriz RBAC (Tabla 44 / NOM-024).
     */
    public function show(Request $request, string $id): JsonResponse
    {
        $user = $request->user();
        if ($user && in_array(strtoupper($user->rol), ['ADMINISTRADOR', 'LABORATORISTA'])) {
            return response()->json([
                'status' => 'error',
                'message' => 'Acceso denegado: El rol ' . $user->rol . ' no tiene autorización asistencial para consultar expedientes clínicos (NOM-024-SSA3-2012 / Tabla 44).',
            ], 403);
        }

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
