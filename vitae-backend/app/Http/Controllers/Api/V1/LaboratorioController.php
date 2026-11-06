<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\ExpedienteClinico;
use App\Models\ResultadoEstudio;
use App\Models\SolicitudEstudio;
use App\Services\AuditLogger;
use App\Services\CifradoAdjuntosService;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

class LaboratorioController extends Controller
{
    public function __construct(
        protected CifradoAdjuntosService $cifradoService
    ) {}

    /**
     * Solicitud de estudio clínico o de gabinete vinculado a expediente (RF-010 / NOM-004).
     */
    public function storeSolicitud(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_expediente' => 'required|string|exists:expediente_c,id_expediente',
            'tipo_estudio' => 'required|string|max:60',
            'prioridad' => 'nullable|string|in:NORMAL,URGENTE',
            'indicaciones_clinicas' => 'nullable|string',
        ]);

        $usuario = Auth::user();

        if ($usuario->rol !== 'MEDICO') {
            return response()->json([
                'status' => 'error',
                'message' => 'Rol no autorizado: Solo el personal médico puede solicitar estudios de laboratorio.',
            ], 403);
        }

        $ahora = Carbon::now('UTC');

        $solicitud = SolicitudEstudio::create([
            'id_expediente' => $validated['id_expediente'],
            'id_usuario' => $usuario->id_usuario,
            'tipo_estudio' => $validated['tipo_estudio'],
            'prioridad' => $validated['prioridad'] ?? 'NORMAL',
            'indicaciones_clinicas' => $validated['indicaciones_clinicas'] ?? null,
            'estado' => 'PENDIENTE',
            'fecha_hora_solicitud' => $ahora,
        ]);

        // Registrar actualización en expediente
        $expediente = ExpedienteClinico::findOrFail($validated['id_expediente']);
        $expediente->registrarActoMedico($ahora, "Solicitud de estudio {$solicitud->tipo_estudio}");

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'solicitud_estudio',
            registroId: $solicitud->id_solicitud,
            valorAnterior: null,
            valorNuevo: $solicitud->toArray(),
            resultado: 'EXITOSO',
            motivo: "Solicitud de estudio {$solicitud->tipo_estudio} para expediente {$expediente->id_expediente}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Solicitud de estudio registrada exitosamente en la bandeja del laboratorio.',
            'data' => $solicitud,
        ], 201);
    }

    /**
     * Bandeja de solicitudes de laboratorio (filtro por estado PENDIENTE).
     */
    public function indexSolicitudes(Request $request): JsonResponse
    {
        $query = SolicitudEstudio::with(['expediente.paciente', 'medicoSolicitante']);

        if ($request->filled('estado')) {
            $query->where('estado', $request->input('estado'));
        }

        if ($request->filled('id_expediente')) {
            $query->where('id_expediente', $request->input('id_expediente'));
        }

        $solicitudes = $query->orderBy('fecha_hora_solicitud', 'desc')->paginate(15);

        return response()->json([
            'status' => 'success',
            'data' => $solicitudes,
        ], 200);
    }

    /**
     * Carga y almacenamiento seguro de resultados cifrados con AES-256-GCM (RF-010 / RNF-003).
     */
    public function storeResultado(Request $request, string $id): JsonResponse
    {
        $solicitud = SolicitudEstudio::findOrFail($id);
        $usuario = Auth::user();

        if (!in_array($usuario->rol, ['LABORATORISTA', 'ADMINISTRADOR'], true)) {
            return response()->json([
                'status' => 'error',
                'message' => 'Rol no autorizado: Solo el personal de laboratorio clínico puede registrar resultados.',
            ], 403);
        }

        $request->validate([
            'observaciones' => 'nullable|string',
            'archivo' => 'nullable|file|max:10240', // 10MB max
            'contenido_texto' => 'nullable|string',
            'mime' => 'nullable|string|in:application/pdf,image/png,image/jpeg,text/plain',
        ]);

        $mime = 'application/pdf';
        $contenidoPlano = '';

        if ($request->hasFile('archivo')) {
            $file = $request->file('archivo');
            $mime = $file->getMimeType();
            $contenidoPlano = file_get_contents($file->getRealPath());
        } elseif ($request->filled('contenido_texto')) {
            $mime = $request->input('mime', 'text/plain');
            $contenidoPlano = $request->input('contenido_texto');
        } else {
            $contenidoPlano = "RESULTADO DE ESTUDIO {$solicitud->tipo_estudio} - NORMAL. VALORES DE REFERENCIA DENTRO DE LÍMITES.";
        }

        // Cifrado militar con AES-256-GCM
        $blobCifrado = $this->cifradoService->cifrar($contenidoPlano);
        $hashSha256 = hash('sha256', $contenidoPlano);

        $nombreArchivoCifrado = 'laboratorio/' . Str::uuid() . '.enc';
        Storage::disk('local')->put($nombreArchivoCifrado, $blobCifrado);

        $ahora = Carbon::now('UTC');

        $resultado = ResultadoEstudio::create([
            'id_solicitud' => $solicitud->id_solicitud,
            'archivo_cifrado_ref' => $nombreArchivoCifrado,
            'mime' => $mime,
            'cargado_por' => $usuario->id_usuario,
            'observaciones' => $request->input('observaciones'),
            'hash_sha256' => $hashSha256,
            'fecha_hora' => $ahora,
        ]);

        $solicitud->update(['estado' => 'COMPLETADA']);

        // Actualizar último acto médico en expediente
        $expediente = $solicitud->expediente;
        $expediente->registrarActoMedico($ahora, "Carga de resultados de estudio {$solicitud->tipo_estudio}");

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'resultado_estudio',
            registroId: $resultado->id_resultado,
            valorAnterior: ['estado_solicitud' => 'PENDIENTE'],
            valorNuevo: [
                'id_resultado' => $resultado->id_resultado,
                'hash_sha256' => $hashSha256,
                'cifrado' => 'AES-256-GCM',
                'estado_solicitud' => 'COMPLETADA',
            ],
            resultado: 'EXITOSO',
            motivo: "Carga de resultado cifrado AES-256-GCM para estudio {$solicitud->id_solicitud}"
        );

        return response()->json([
            'status' => 'success',
            'message' => 'Resultado de estudio cifrado con AES-256-GCM y vinculado al expediente clínico.',
            'data' => $resultado,
        ], 201);
    }

    /**
     * Consulta y descifrado seguro de resultado de estudio.
     */
    public function showResultado(string $id): JsonResponse
    {
        $solicitud = SolicitudEstudio::with('resultado')->findOrFail($id);

        if (!$solicitud->resultado) {
            return response()->json([
                'status' => 'error',
                'message' => 'La solicitud no cuenta aún con resultados cargados.',
            ], 404);
        }

        $resultado = $solicitud->resultado;
        $contenidoPlano = null;

        if (Storage::disk('local')->exists($resultado->archivo_cifrado_ref)) {
            $blobCifrado = Storage::disk('local')->get($resultado->archivo_cifrado_ref);
            $contenidoPlano = $this->cifradoService->descifrar($blobCifrado);
        }

        return response()->json([
            'status' => 'success',
            'data' => [
                'resultado' => $resultado,
                'contenido_descifrado' => $contenidoPlano,
                'integridad_sha256' => $resultado->hash_sha256,
            ],
        ], 200);
    }
}
