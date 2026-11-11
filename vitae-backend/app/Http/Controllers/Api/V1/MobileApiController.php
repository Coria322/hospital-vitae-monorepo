<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Cita;
use App\Models\Consentimiento;
use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Receta;
use App\Models\ResultadoEstudio;
use App\Models\SolicitudArco;
use App\Services\AuditLogger;
use App\Services\ConsentimientoService;
use Carbon\Carbon;
use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class MobileApiController extends Controller
{
    /**
     * Obtiene el ID del paciente autenticado y validado por el middleware de aislamiento.
     */
    protected function getAuthPacienteId(Request $request): string
    {
        $usuario = Auth::user();
        return (string) ($request->attributes->get('auth_paciente_id') ?? $usuario->id_paciente ?? $usuario->id_usuario);
    }

    /**
     * Perfil del paciente autenticado (RF-011 / NOM-024).
     */
    public function perfil(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);
        $paciente = Paciente::findOrFail($idPaciente);

        return response()->json([
            'status' => 'success',
            'data' => [
                'id_paciente' => $paciente->id_paciente,
                'nombre_completo' => $paciente->nombre_completo,
                'curp' => $paciente->curp,
                'fecha_nacimiento' => $paciente->fecha_nacimiento?->format('Y-m-d'),
                'sexo' => $paciente->sexo,
                'domicilio' => $paciente->domicilio,
                'seguro_activo' => $paciente->seguro_activo,
                'identificado' => $paciente->identificado,
            ],
        ], 200);
    }

    /**
     * Consulta de citas del paciente autenticado con aislamiento horizontal (RF-011 / RF-002.1).
     */
    public function citas(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $citas = Cita::with(['medico:id_usuario,nombre_completo,cedula'])
            ->where('id_paciente', $idPaciente)
            ->orderBy('fecha_hora', 'asc')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => $citas,
        ], 200);
    }

    /**
     * Solicitud de nueva cita médica desde el portal móvil (PROV-RF-002 / SUP-002 lista blanca).
     */
    public function solicitarCita(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $validated = $request->validate([
            'fecha_hora' => 'required|date|after:now',
            'especialidad' => 'nullable|string|max:60',
            'motivo' => 'nullable|string|max:500',
        ]);

        $cita = Cita::create([
            'id_paciente' => $idPaciente,
            'fecha_hora' => Carbon::parse($validated['fecha_hora'], 'UTC'),
            'estado' => 'PENDIENTE',
            'especialidad' => $validated['especialidad'] ?? 'Medicina General',
            'motivo' => $validated['motivo'] ?? 'Cita solicitada desde aplicación móvil',
            'consultorio' => 'Por Asignar',
        ]);

        AuditLogger::log(
            tipoMovimiento: 'SOLICITUD_CITA_MOVIL',
            tablaAfectada: 'cita',
            registroId: $cita->id_cita,
            valorAnterior: null,
            valorNuevo: ['fecha_hora' => $cita->fecha_hora->toIso8601String(), 'estado' => 'PENDIENTE'],
            resultado: 'EXITOSO',
            motivo: 'Cita programada por el paciente desde app móvil (PROV-RF-002)',
            esNecesarioAuditoriaInterna: true,
            idUsuario: Auth::user()->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Cita solicitada correctamente.',
            'data' => $cita,
        ], 201);
    }

    /**
     * Cancelación de cita médica por parte del propio paciente (Anti-IDOR RF-002.1).
     */
    public function cancelarCita(Request $request, string $id): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);
        $cita = Cita::findOrFail($id);

        if ($cita->id_paciente !== $idPaciente) {
            AuditLogger::log(
                tipoMovimiento: 'INTENTO_IDOR_MOVIL',
                tablaAfectada: 'cita',
                registroId: $id,
                valorAnterior: ['id_paciente_cita' => $cita->id_paciente],
                valorNuevo: ['id_paciente_solicitante' => $idPaciente],
                resultado: 'DENEGADO',
                motivo: 'Intento de cancelar cita médica de otro paciente (Anti-IDOR RF-002.1)',
                esNecesarioAuditoriaInterna: true,
                idUsuario: Auth::user()->id_usuario
            );

            return response()->json([
                'status' => 'error',
                'code' => 'IDOR_DETECTED',
                'message' => 'Acceso denegado: No puede modificar recursos de otro paciente.',
            ], 403);
        }

        $cita->cancelar($request->input('motivo', 'Cancelada por el paciente desde app móvil'));

        return response()->json([
            'status' => 'success',
            'message' => 'Cita médica cancelada exitosamente.',
            'data' => $cita,
        ], 200);
    }

    /**
     * Consulta de recetas médicas de solo lectura con aislamiento horizontal (RF-011 / RF-002.1).
     */
    public function recetas(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $recetas = Receta::whereHas('expediente', function ($q) use ($idPaciente) {
            $q->where('id_paciente', $idPaciente);
        })
        ->with(['medico:id_usuario,nombre_completo,cedula'])
        ->orderBy('fecha_hora_emision', 'desc')
        ->get();

        return response()->json([
            'status' => 'success',
            'data' => $recetas,
        ], 200);
    }

    /**
     * Consulta de resultados de laboratorio con aislamiento horizontal (RF-011 / RF-002.1).
     */
    public function resultados(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $resultados = ResultadoEstudio::whereHas('solicitud.expediente', function ($q) use ($idPaciente) {
            $q->where('id_paciente', $idPaciente);
        })
        ->with(['solicitud:id_solicitud,id_expediente,tipo_estudio,fecha_hora_solicitud'])
        ->orderBy('fecha_hora', 'desc')
        ->get();

        return response()->json([
            'status' => 'success',
            'data' => $resultados,
        ], 200);
    }

    /**
     * Consulta y descifrado seguro de un resultado de estudio del paciente autenticado (RF-011 / Anti-IDOR).
     */
    public function resultadoDetalle(Request $request, string $id): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $resultado = ResultadoEstudio::where(function ($q) use ($id) {
                $q->where('id_resultado', $id)
                  ->orWhere('id_solicitud', $id);
            })
            ->whereHas('solicitud.expediente', function ($q) use ($idPaciente) {
                $q->where('id_paciente', $idPaciente);
            })
            ->first();

        if (!$resultado) {
            return response()->json([
                'status' => 'error',
                'code' => 'NOT_FOUND',
                'message' => 'Resultado de estudio no encontrado o no pertenece al paciente.',
            ], 404);
        }

        $contenidoPlano = null;
        if (\Illuminate\Support\Facades\Storage::disk('local')->exists($resultado->archivo_cifrado_ref)) {
            $blobCifrado = \Illuminate\Support\Facades\Storage::disk('local')->get($resultado->archivo_cifrado_ref);
            $cifradoService = app(\App\Services\CifradoService::class);
            $contenidoPlano = $cifradoService->descifrar($blobCifrado);
        }

        return response()->json([
            'status' => 'success',
            'data' => [
                'resultado' => [
                    'id_resultado' => $resultado->id_resultado,
                    'id_solicitud' => $resultado->id_solicitud,
                    'mime' => $resultado->mime,
                    'fecha_hora' => $resultado->fecha_hora->toIso8601String(),
                ],
                'contenido_descifrado' => $contenidoPlano ?? $resultado->observaciones,
                'integridad_sha256' => $resultado->hash_sha256,
            ],
        ], 200);
    }

    /**
     * Consulta de expediente clínico resumido del paciente (RF-011 / NOM-004).
     */
    public function expediente(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $expediente = ExpedienteClinico::where('id_paciente', $idPaciente)
            ->with([
                'paciente',
                'antecedentes',
                'notas' => fn($q) => $q->orderBy('fecha_hora', 'desc'),
            ])
            ->first();

        if (!$expediente) {
            return response()->json([
                'status' => 'success',
                'message' => 'El paciente no cuenta con expediente clínico activo en este momento.',
                'data' => null,
            ], 200);
        }

        return response()->json([
            'status' => 'success',
            'data' => $expediente,
        ], 200);
    }

    /**
     * Registro de consentimiento informado desde el portal móvil (SUP-002 lista blanca).
     */
    public function consentimiento(Request $request, ConsentimientoService $consentimientoService): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $validated = $request->validate([
            'tipo' => 'required|string|max:50',
            'finalidades' => 'required|string|min:10',
            'medico_responsable_id' => 'required|string|exists:usuario,id_usuario',
            'firmante_nombre' => 'required|string|max:100',
            'firmante_parentesco' => 'nullable|string|max:50',
        ]);

        $consentimiento = $consentimientoService->registrarConsentimiento(
            idPaciente: $idPaciente,
            tipo: $validated['tipo'],
            finalidades: $validated['finalidades'],
            firmanteNombre: $validated['firmante_nombre'],
            medicoResponsableId: $validated['medico_responsable_id'],
            firmanteParentesco: $validated['firmante_parentesco'] ?? 'TITULAR',
            medio: 'DIGITAL_MOVIL'
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Consentimiento informado registrado exitosamente desde app móvil.',
            'data' => $consentimiento,
        ], 201);
    }

    /**
     * Solicitud de derechos ARCO desde el portal móvil (SUP-002 lista blanca).
     */
    public function solicitudArco(Request $request): JsonResponse
    {
        $idPaciente = $this->getAuthPacienteId($request);

        $validated = $request->validate([
            'derecho' => 'required|string|in:ACCESO,RECTIFICACION,CANCELACION,OPOSICION',
            'descripcion_peticion' => 'required|string|min:15',
        ]);

        $fechaRecepcion = Carbon::now('UTC');
        $respuestaLimite = $fechaRecepcion->copy()->addWeekdays(20);
        $ejecucionLimite = $respuestaLimite->copy()->addWeekdays(15);
        $folio = 'ARCO-MOV-' . strtoupper(bin2hex(random_bytes(4)));

        $solicitud = SolicitudArco::create([
            'folio' => $folio,
            'id_paciente' => $idPaciente,
            'derecho' => $validated['derecho'],
            'descripcion_peticion' => $validated['descripcion_peticion'],
            'fecha_recepcion' => $fechaRecepcion,
            'respuesta_limite' => $respuestaLimite,
            'ejecucion_limite' => $ejecucionLimite,
            'estado' => 'RECIBIDA',
        ]);

        AuditLogger::log(
            tipoMovimiento: 'SOLICITUD_ARCO_MOVIL',
            tablaAfectada: 'solicitud_arco',
            registroId: $solicitud->folio,
            valorAnterior: null,
            valorNuevo: ['derecho' => $solicitud->derecho, 'folio' => $solicitud->folio],
            resultado: 'EXITOSO',
            motivo: "Recepción de solicitud ARCO ({$solicitud->derecho}) desde portal móvil",
            esNecesarioAuditoriaInterna: true,
            idUsuario: Auth::user()->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Solicitud ARCO radicada correctamente.',
            'data' => $solicitud,
        ], 201);
    }

    /**
     * Interceptor normativo para bloquear operaciones de mutación clínica desde el móvil (RF-011).
     */
    public function bloquearMutacionClinica(Request $request): JsonResponse
    {
        AuditLogger::log(
            tipoMovimiento: 'INTENTO_ESCRITURA_CLINICA_MOVIL',
            tablaAfectada: $request->path(),
            registroId: (string) ($request->route('id') ?? 'RECURSO_CLINICO'),
            valorAnterior: null,
            valorNuevo: ['metodo' => $request->method(), 'path' => $request->path()],
            resultado: 'DENEGADO',
            motivo: 'Intento de escritura sobre recurso clínico por rol PACIENTE en API móvil (RF-011)',
            esNecesarioAuditoriaInterna: true,
            idUsuario: Auth::user()?->id_usuario ?? 'ANONIMO'
        );

        return response()->json([
            'status' => 'error',
            'code' => 'CLINICAL_WRITE_FORBIDDEN',
            'message' => 'Operación no permitida: Las modificaciones sobre recursos clínicos están estrictamente prohibidas para el portal móvil (RF-011 / NOM-024).',
        ], 403);
    }
}

