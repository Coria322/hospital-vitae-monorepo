<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Paciente;
use App\Models\SalaAtencion;
use App\Models\Triage;
use App\Services\AuditLogger;
use App\Services\ColaUrgenciasService;
use App\Services\TriageManchesterService;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use InvalidArgumentException;

class TriageController extends Controller
{
    public function __construct(
        protected TriageManchesterService $manchesterService,
        protected ColaUrgenciasService $colaService
    ) {}

    /**
     * Registra un nuevo triage con captura obligatoria de signos vitales (RF-004)
     * y cálculo automático de prioridad Manchester (RF-005, NOM-206-SSA1-2002).
     */
    public function store(Request $request): JsonResponse
    {
        $usuario = $request->user();

        // RF-004 Escenario 3: Solo rol ENFERMERIA o ADMINISTRADOR puede registrar triage
        if (!in_array($usuario->rol, ['ENFERMERIA', 'ADMINISTRADOR'])) {
            return response()->json([
                'status' => 'error',
                'message' => 'No autorizado. Solo el personal de ENFERMERIA puede realizar la captura de Triage.',
            ], 403);
        }

        // RF-004 Escenario 2: Validación de campos obligatorios
        $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'presion_art' => 'required|string',
            'frecuencia_card' => 'required|integer',
            'nivel_conciencia' => 'required|string|in:ALERTA,VERBAL,DOLOR,INCONSCIENTE',
            'escala_eva' => 'required|integer|between:0,10',
            'frecuencia_resp' => 'nullable|integer',
            'temperatura' => 'nullable|numeric',
            'spo2' => 'nullable|integer',
            'nivel_glucosa' => 'nullable|integer',
            'estimacion_sangre_perdida' => 'nullable|string',
            'captura_retroactiva' => 'nullable|boolean',
            'fecha_hora_ingreso' => 'nullable|date',
        ]);

        $datos = $request->all();

        // Validar rangos biológicos creíbles (TC-TRG-005)
        try {
            $clasificacion = $this->manchesterService->clasificar($datos);
        } catch (InvalidArgumentException $e) {
            return response()->json([
                'status' => 'error',
                'message' => 'Validación clínica rechazada: ' . $e->getMessage(),
            ], 422);
        }

        $colorAsignado = $clasificacion['color'];
        $slaMinutos = $clasificacion['sla_minutos'];

        // Manejo de captura retroactiva (PROV-RN-006)
        $esRetroactiva = (bool) ($datos['captura_retroactiva'] ?? false);
        $ahoraUtc = Carbon::now('UTC');

        if ($esRetroactiva && !empty($datos['fecha_hora_ingreso'])) {
            $fechaIngreso = Carbon::parse($datos['fecha_hora_ingreso'], 'UTC');
            if ($fechaIngreso->isAfter($ahoraUtc)) {
                return response()->json([
                    'status' => 'error',
                    'message' => 'La fecha y hora de arribo físico retroactivo no puede ser posterior a la hora actual.',
                ], 422);
            }
        } else {
            $fechaIngreso = $ahoraUtc;
        }

        $slaVencimientoEn = (clone $fechaIngreso)->addMinutes($slaMinutos);

        $idTriage = 'TRG-' . strtoupper(substr(md5(uniqid((string) mt_rand(), true)), 0, 6));
        $folioIngreso = 'FOL-TRI-' . strtoupper(substr(uniqid(), -6));

        return DB::transaction(function () use (
            $idTriage,
            $folioIngreso,
            $datos,
            $colorAsignado,
            $fechaIngreso,
            $slaVencimientoEn,
            $esRetroactiva,
            $usuario,
            $clasificacion
        ) {
            $triage = Triage::create([
                'id_triage' => $idTriage,
                'id_paciente' => $datos['id_paciente'],
                'id_usuario' => $usuario->id_usuario,
                'presion_art' => $datos['presion_art'],
                'frecuencia_card' => $datos['frecuencia_card'],
                'frecuencia_resp' => $datos['frecuencia_resp'] ?? null,
                'temperatura' => $datos['temperatura'] ?? null,
                'spo2' => $datos['spo2'] ?? null,
                'nivel_glucosa' => $datos['nivel_glucosa'] ?? null,
                'escala_eva' => $datos['escala_eva'],
                'nivel_conciencia' => strtoupper($datos['nivel_conciencia']),
                'color_asig' => $colorAsignado,
                'color_original' => $colorAsignado,
                'fecha_hora_ingreso' => $fechaIngreso,
                'estimacion_sangre_perdida' => $datos['estimacion_sangre_perdida'] ?? null,
                'folio_ingreso' => $folioIngreso,
                'sla_vencimiento_en' => $slaVencimientoEn,
                'sla_vencido' => false,
                'desviacion_minutos' => 0,
                'estado_atencion' => 'EN_ESPERA',
                'captura_retroactiva' => $esRetroactiva,
            ]);

            // Asignación de sala de emergencia si es ROJO o asignación a cola regular
            $resultadoAsignacion = $this->colaService->asignarSalaOCola($triage);

            // Registro inalterable en AUDITORIA_I (NOM-024 / NOM-004)
            AuditLogger::log(
                tipoMovimiento: 'INSERT',
                tablaAfectada: 'triage',
                registroId: $triage->id_triage,
                valorAnterior: null,
                valorNuevo: [
                    'folio_ingreso' => $folioIngreso,
                    'color_asig' => $colorAsignado,
                    'es_retroactiva' => $esRetroactiva,
                    'fecha_hora_ingreso' => $fechaIngreso->toIso8601String(),
                ],
                resultado: 'EXITOSO',
                motivo: $esRetroactiva ? 'CAPTURA_RETROACTIVA' : 'Alta de triage y clasificación Manchester'
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Triage registrado exitosamente.',
                'data' => [
                    'triage' => $triage->fresh(['paciente', 'sala']),
                    'clasificacion' => $clasificacion,
                    'asignacion' => $resultadoAsignacion,
                ],
            ], 201);
        });
    }

    /**
     * Reclasificación médica manual de nivel de triage con justificación obligatoria (RF-007).
     * Prohibido degradar casos en ROJO (TC-TRG-004).
     */
    public function reclasificar(Request $request, string $id): JsonResponse
    {
        $usuario = $request->user();

        // RF-007: Privilegios superiores (MEDICO o ADMINISTRADOR)
        if (!in_array($usuario->rol, ['MEDICO', 'ADMINISTRADOR'])) {
            return response()->json([
                'status' => 'error',
                'message' => 'Acción denegada. Solo un MÉDICO o ADMINISTRADOR tiene privilegios para reclasificar el nivel de triage.',
            ], 403);
        }

        $request->validate([
            'nuevo_color' => 'required|string|in:ROJO,NARANJA,AMARILLO,VERDE,AZUL',
            'motivo' => 'required|string|min:5|max:500',
        ]);

        $triage = Triage::findOrFail($id);
        $colorAnterior = $triage->color_asig;
        $nuevoColor = strtoupper($request->input('nuevo_color'));
        $motivo = trim($request->input('motivo'));

        // Validar lógica de reclasificación (bloqueo de degradar ROJO)
        try {
            $this->manchesterService->validarReclasificacion($colorAnterior, $nuevoColor, $motivo);
        } catch (InvalidArgumentException $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }

        // Recalcular SLA para el nuevo nivel
        $slaMinutos = TriageManchesterService::SLA_MINUTOS[$nuevoColor];
        $nuevoSlaVencimiento = (clone $triage->fecha_hora_ingreso)->addMinutes($slaMinutos);

        $triage->update([
            'color_asig' => $nuevoColor,
            'motivo_reclasificacion' => $motivo,
            'sla_vencimiento_en' => $nuevoSlaVencimiento,
            'sla_vencido' => false,
        ]);

        // Si se escala a ROJO, intentar asignación inmediata a sala de emergencia
        if ($nuevoColor === TriageManchesterService::ROJO && !$triage->id_sala) {
            $this->colaService->asignarSalaOCola($triage);
        }

        // Auditoría inalterable de reclasificación
        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'triage',
            registroId: $triage->id_triage,
            valorAnterior: ['color_asig' => $colorAnterior],
            valorNuevo: ['color_asig' => $nuevoColor],
            resultado: 'EXITOSO',
            motivo: $motivo
        );

        return response()->json([
            'status' => 'success',
            'message' => "Triage reclasificado exitosamente de {$colorAnterior} a {$nuevoColor}.",
            'data' => $triage->fresh(['paciente', 'sala']),
        ], 200);
    }

    /**
     * Consulta la cola dinámica de urgencias (RF-006).
     */
    public function cola(): JsonResponse
    {
        $cola = $this->colaService->obtenerColaViva();

        return response()->json([
            'status' => 'success',
            'total' => $cola->count(),
            'data' => $cola,
        ]);
    }

    /**
     * Libera un consultorio o sala de emergencia y llama al siguiente paciente (RF-006).
     */
    public function liberarSala(string $idSala): JsonResponse
    {
        $resultado = $this->colaService->liberarSala($idSala);

        return response()->json([
            'status' => 'success',
            'data' => $resultado,
        ]);
    }

    /**
     * Lista las salas de atención y consultorios con su disponibilidad.
     */
    public function salas(): JsonResponse
    {
        $salas = SalaAtencion::with('triageActual.paciente')->get();

        return response()->json([
            'status' => 'success',
            'data' => $salas,
        ]);
    }

    /**
     * Detalle de un triage específico.
     */
    public function show(string $id): JsonResponse
    {
        $triage = Triage::with(['paciente', 'enfermera', 'sala'])->findOrFail($id);

        return response()->json([
            'status' => 'success',
            'data' => $triage,
        ]);
    }
}
