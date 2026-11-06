<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\AntecedenteClinico;
use App\Models\ExpedienteClinico;
use App\Models\NotaMedica;
use App\Models\Paciente;
use App\Services\AuditLogger;
use App\Services\BreakTheGlassService;
use App\Services\PdfExpedienteService;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Str;

class ExpedienteClinicoController extends Controller
{
    public function __construct(
        protected BreakTheGlassService $breakTheGlassService,
        protected PdfExpedienteService $pdfExpedienteService
    ) {}

    /**
     * Creación y apertura de expediente clínico (RF-008.1 / NOM-004-SSA3-2012).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_paciente' => 'required|uuid|exists:paciente,id_paciente',
            'motivo_consulta' => 'required|string|min:5',
            'diagnostico' => 'nullable|string',
            'estudios_previos' => 'nullable|string',
        ]);

        $usuario = Auth::user();

        // Verificar unicidad 1:1 de expediente clínico por paciente
        $expedienteExistente = ExpedienteClinico::where('id_paciente', $validated['id_paciente'])
            ->where('estado_activo', true)
            ->first();

        if ($expedienteExistente) {
            return response()->json([
                'status' => 'error',
                'code' => 'EXPEDIENTE_DUPLICADO',
                'message' => 'El paciente ya cuenta con un expediente clínico activo registrado (relación 1:1 obligatoria NOM-004).',
                'id_expediente_existente' => $expedienteExistente->id_expediente,
            ], 409);
        }

        $ahora = Carbon::now('UTC');
        $idExpediente = 'EXP-' . strtoupper(Str::random(6));

        $expediente = ExpedienteClinico::create([
            'id_expediente' => $idExpediente,
            'id_paciente' => $validated['id_paciente'],
            'id_usuario' => $usuario->id_usuario,
            'motivo_consulta' => $validated['motivo_consulta'],
            'diagnostico' => $validated['diagnostico'] ?? null,
            'estudios_previos' => $validated['estudios_previos'] ?? null,
            'fecha_creacion' => $ahora,
            'fecha_ultimo_acto_medico' => $ahora,
            'estado_activo' => true,
            'bloqueo_legal' => false,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'expediente_c',
            registroId: $idExpediente,
            valorAnterior: null,
            valorNuevo: [
                'id_expediente' => $idExpediente,
                'id_paciente' => $validated['id_paciente'],
                'id_usuario' => $usuario->id_usuario,
            ],
            resultado: 'EXITOSO',
            motivo: "Apertura formal de expediente clínico conforme a NOM-004-SSA3-2012"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Expediente clínico aperturado exitosamente.',
            'data' => $expediente->load('paciente'),
        ], 201);
    }

    /**
     * Consulta integral de expediente clínico con verificación de relación asistencial (RF-008).
     */
    public function show(Request $request, string $id): JsonResponse
    {
        $expediente = ExpedienteClinico::with(['paciente', 'antecedentes', 'notasMedicas', 'recetas', 'solicitudesEstudio.resultado'])
            ->where('id_expediente', $id)
            ->firstOrFail();

        $usuario = Auth::user();

        // Separación de funciones: Administrador no puede leer contenido clínico directo
        if ($usuario->rol === 'ADMINISTRADOR') {
            return response()->json([
                'status' => 'error',
                'code' => 'ACCESO_DENEGADO_ADMIN',
                'message' => 'Separación de funciones (ISO-27001): El perfil ADMINISTRADOR no tiene acceso a notas ni diagnósticos clínicos.',
            ], 403);
        }

        // Verificación de relación asistencial o Break-the-Glass
        $tieneAcceso = $this->breakTheGlassService->tieneRelacionAsistencial($usuario, $expediente);

        if (!$tieneAcceso) {
            AuditLogger::log(
                tipoMovimiento: 'ACCESO_EXPEDIENTE_DENEGADO',
                tablaAfectada: 'expediente_c',
                registroId: $expediente->id_expediente,
                valorAnterior: null,
                valorNuevo: ['solicitante' => $usuario->id_usuario, 'paciente' => $expediente->id_paciente],
                resultado: 'DENEGADO',
                motivo: 'Médico sin relación asistencial vigente intentó consultar expediente',
                esNecesarioAuditoriaInterna: true
            );

            return response()->json([
                'status' => 'error',
                'code' => 'BREAK_THE_GLASS_REQUIRED',
                'message' => 'Acceso denegado: No cuenta con relación asistencial activa con este paciente. En situación de urgencia, active el protocolo Break-the-Glass mediante POST /api/v1/accesos-emergencia.',
                'id_paciente' => $expediente->id_paciente,
            ], 403);
        }

        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_EXPEDIENTE',
            tablaAfectada: 'expediente_c',
            registroId: $expediente->id_expediente,
            valorAnterior: null,
            valorNuevo: ['consultado_por' => $usuario->id_usuario],
            resultado: 'EXITOSO',
            motivo: "Consulta de expediente clínico asistencial por {$usuario->id_usuario}"
        );

        return response()->json([
            'status' => 'success',
            'data' => $expediente,
        ], 200);
    }

    /**
     * Registro de antecedentes clínicos (RF-008.1 / NOM-004-SSA3-2012).
     */
    public function storeAntecedente(Request $request, string $id): JsonResponse
    {
        $expediente = ExpedienteClinico::findOrFail($id);

        $validated = $request->validate([
            'tipo' => 'required|string|in:HEREDOFAMILIAR,PATOLOGICO,NO_PATOLOGICO,GINECOOBSTETRICO,ALERGIA',
            'descripcion' => 'required|string|min:3',
            'fecha_diagnostico' => 'nullable|date',
            'observaciones' => 'nullable|string',
        ]);

        $antecedente = AntecedenteClinico::create([
            'id_expediente' => $expediente->id_expediente,
            'tipo' => $validated['tipo'],
            'descripcion' => $validated['descripcion'],
            'fecha_diagnostico' => $validated['fecha_diagnostico'] ?? null,
            'observaciones' => $validated['observaciones'] ?? null,
        ]);

        $expediente->registrarActoMedico(null, "Registro de antecedente clínico {$validated['tipo']}");

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'antecedente_clinico',
            registroId: $antecedente->id_antecedente,
            valorAnterior: null,
            valorNuevo: $antecedente->toArray(),
            resultado: 'EXITOSO',
            motivo: "Registro de antecedente clínico tipo {$antecedente->tipo} en expediente {$expediente->id_expediente}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Antecedente clínico registrado exitosamente.',
            'data' => $antecedente,
        ], 201);
    }

    /**
     * Emisión de nota médica de evolución/ingreso/egreso inmutable (RF-008.2 / NOM-004).
     */
    public function storeNota(Request $request, string $id): JsonResponse
    {
        $expediente = ExpedienteClinico::findOrFail($id);
        $usuario = Auth::user();

        if ($usuario->rol !== 'MEDICO') {
            return response()->json([
                'status' => 'error',
                'message' => 'Rol no autorizado: Solo los médicos pueden asentar notas de evolución.',
            ], 403);
        }

        $validated = $request->validate([
            'tipo' => 'required|string|in:EVOLUCION,INGRESO,EGRESO,INTERCONSULTA',
            'contenido' => 'required|string|min:10',
            'diagnostico_cie' => 'nullable|string|max:10',
            'plan_tratamiento' => 'nullable|string',
        ]);

        $ahora = Carbon::now('UTC');
        $firmaAutomatica = "SHA256-EFIRMA-MED-{$usuario->id_usuario}-" . hash('sha256', $validated['contenido'] . $ahora->toIso8601String());

        $hashSha256 = NotaMedica::calcularHash(
            idExpediente: $expediente->id_expediente,
            idUsuario: $usuario->id_usuario,
            tipo: $validated['tipo'],
            contenido: $validated['contenido'],
            fechaHora: $ahora->toIso8601String()
        );

        $nota = NotaMedica::create([
            'id_expediente' => $expediente->id_expediente,
            'id_usuario' => $usuario->id_usuario,
            'tipo' => $validated['tipo'],
            'id_nota_original' => null,
            'contenido' => $validated['contenido'],
            'diagnostico_cie' => $validated['diagnostico_cie'] ?? null,
            'plan_tratamiento' => $validated['plan_tratamiento'] ?? null,
            'firma_electronica' => $firmaAutomatica,
            'fecha_hora' => $ahora,
            'cerrada' => true,
            'hash_sha256' => $hashSha256,
        ]);

        $expediente->registrarActoMedico($ahora, "Emisión de nota médica inmutable {$nota->id_nota}");

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'nota_medica',
            registroId: $nota->id_nota,
            valorAnterior: null,
            valorNuevo: [
                'id_expediente' => $expediente->id_expediente,
                'tipo' => $nota->tipo,
                'hash_sha256' => $hashSha256,
                'cerrada' => true,
            ],
            resultado: 'EXITOSO',
            motivo: "Registro de nota médica inmutable tipo {$nota->tipo} por {$usuario->id_usuario}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Nota médica firmada y sellada inmutablemente.',
            'data' => $nota,
        ], 201);
    }

    /**
     * Emisión de nota aclaratoria vinculada a nota médica previa (RF-008.2 / NOM-004-SSA3-2012).
     */
    public function storeAclaracion(Request $request, string $id, string $nid): JsonResponse
    {
        $expediente = ExpedienteClinico::findOrFail($id);
        $notaOriginal = NotaMedica::where('id_expediente', $expediente->id_expediente)
            ->where('id_nota', $nid)
            ->firstOrFail();

        $usuario = Auth::user();

        if ($usuario->rol !== 'MEDICO') {
            return response()->json([
                'status' => 'error',
                'message' => 'Rol no autorizado: Solo personal médico puede emitir notas aclaratorias.',
            ], 403);
        }

        $validated = $request->validate([
            'contenido' => 'required|string|min:10',
            'plan_tratamiento' => 'nullable|string',
        ]);

        $ahora = Carbon::now('UTC');
        $firmaAutomatica = "SHA256-ACLARACION-{$usuario->id_usuario}-" . hash('sha256', $validated['contenido'] . $ahora->toIso8601String());

        $hashSha256 = NotaMedica::calcularHash(
            idExpediente: $expediente->id_expediente,
            idUsuario: $usuario->id_usuario,
            tipo: 'ACLARATORIA',
            contenido: $validated['contenido'],
            fechaHora: $ahora->toIso8601String()
        );

        $notaAclaratoria = NotaMedica::create([
            'id_expediente' => $expediente->id_expediente,
            'id_usuario' => $usuario->id_usuario,
            'tipo' => 'ACLARATORIA',
            'id_nota_original' => $notaOriginal->id_nota,
            'contenido' => $validated['contenido'],
            'diagnostico_cie' => $notaOriginal->diagnostico_cie,
            'plan_tratamiento' => $validated['plan_tratamiento'] ?? null,
            'firma_electronica' => $firmaAutomatica,
            'fecha_hora' => $ahora,
            'cerrada' => true,
            'hash_sha256' => $hashSha256,
        ]);

        $expediente->registrarActoMedico($ahora, "Emisión de nota aclaratoria enlazada a nota {$notaOriginal->id_nota}");

        AuditLogger::log(
            tipoMovimiento: 'NOTA_ACLARATORIA',
            tablaAfectada: 'nota_medica',
            registroId: $notaAclaratoria->id_nota,
            valorAnterior: ['id_nota_original' => $notaOriginal->id_nota],
            valorNuevo: [
                'id_nota_aclaratoria' => $notaAclaratoria->id_nota,
                'hash_sha256' => $hashSha256,
            ],
            resultado: 'EXITOSO',
            motivo: "Registro de nota médica aclaratoria vinculada a nota original {$notaOriginal->id_nota}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Nota aclaratoria emitida exitosamente conservando íntegra la nota original.',
            'data' => $notaAclaratoria,
        ], 201);
    }

    /**
     * Bloqueo explícito de verbos PUT, PATCH y DELETE sobre notas médicas (NOM-004-SSA3-2012 / AI-04).
     */
    public function bloquearModificacionNota(): JsonResponse
    {
        return response()->json([
            'status' => 'error',
            'code' => 'METODO_NO_PERMITIDO',
            'message' => 'NOM-004-SSA3-2012 / NOM-024-SSA3-2012: Las notas médicas guardadas y firmadas son inmutables. Operaciones PUT, PATCH y DELETE prohibidas; use notas aclaratorias.',
        ], 405);
    }

    /**
     * Exportación de expediente clínico con marcas de agua de auditoría (PROV-SEC-008).
     */
    public function exportarPdf(Request $request, string $id): JsonResponse
    {
        $expediente = ExpedienteClinico::with(['paciente', 'antecedentes', 'notasMedicas', 'recetas'])
            ->where('id_expediente', $id)
            ->firstOrFail();

        $usuario = Auth::user();

        // Médicos y enfermeros en relación asistencial o Break-the-Glass
        $tieneAcceso = $this->breakTheGlassService->tieneRelacionAsistencial($usuario, $expediente);

        if (!$tieneAcceso && $usuario->rol !== 'MEDICO') {
            return response()->json([
                'status' => 'error',
                'message' => 'No autorizado para exportar información clínica confidencial.',
            ], 403);
        }

        $enmascarar = $request->boolean('enmascarar_pii', false);
        $ipOrigen = $request->ip() ?? '127.0.0.1';

        $documentoExportado = $this->pdfExpedienteService->exportarExpedienteSeguro(
            expediente: $expediente,
            solicitante: $usuario,
            ipOrigen: $ipOrigen,
            enmascararPii: $enmascarar
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Expediente clínico exportado con sellos de agua dinámicos de auditoría.',
            'data' => $documentoExportado,
        ], 200);
    }
}
