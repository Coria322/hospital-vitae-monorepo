<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Receta;
use App\Services\Pdf\PdfPlantillasService;
use App\Services\RecetaMedicaService;
use Exception;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Symfony\Component\HttpFoundation\Response;

class RecetaController extends Controller
{
    public function __construct(
        protected RecetaMedicaService $recetaMedicaService,
        protected PdfPlantillasService $pdfPlantillasService
    ) {}

    /**
     * Emisión de receta médica (RF-009 / COFEPRIS).
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'id_expediente' => 'required|string|exists:expediente_c,id_expediente',
            'fraccion' => 'required|string|in:I,II,III,NINGUNA',
            'medicamentos' => 'required|string|min:5',
            'indicaciones' => 'required|string|min:5',
        ]);

        $usuario = Auth::user();

        if ($usuario->rol !== 'MEDICO') {
            return response()->json([
                'status' => 'error',
                'message' => 'Rol no autorizado: La emisión de recetas está reservada estrictamente a médicos con cédula profesional.',
            ], 403);
        }

        try {
            $receta = $this->recetaMedicaService->emitirReceta(
                idExpediente: $validated['id_expediente'],
                idUsuario: $usuario->id_usuario,
                fraccion: $validated['fraccion'],
                medicamentos: $validated['medicamentos'],
                indicaciones: $validated['indicaciones']
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Receta médica emitida exitosamente.',
                'data' => $receta,
            ], 201);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Firma digital de la receta con e.firma y activación de código QR único (RF-009 / SAT).
     */
    public function firmar(Request $request, string $id): JsonResponse
    {
        $receta = Receta::findOrFail($id);

        $validated = $request->validate([
            'firma_electronica' => 'required|string|min:10',
            'certificado' => 'nullable|string',
        ]);

        try {
            $recetaFirmada = $this->recetaMedicaService->firmarReceta(
                receta: $receta,
                firmaElectronica: $validated['firma_electronica'],
                certificado: $validated['certificado'] ?? null
            );

            return response()->json([
                'status' => 'success',
                'message' => 'Receta médica firmada con e.firma y código QR único activado.',
                'data' => $recetaFirmada,
            ], 200);
        } catch (Exception $e) {
            return response()->json([
                'status' => 'error',
                'code' => 'FIRMA_INVALIDA',
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Consulta de receta por ID o Folio.
     */
    public function show(string $id): JsonResponse
    {
        $receta = Receta::where('id_receta', $id)
            ->orWhere('folio', $id)
            ->firstOrFail();

        return response()->json([
            'status' => 'success',
            'data' => $receta->load(['expediente.paciente', 'medico']),
        ], 200);
    }

    /**
     * Surtido de receta y consumo irreversible del código QR de un solo uso (RF-009 / COFEPRIS).
     */
    public function surtir(Request $request, string $folio): JsonResponse
    {
        $usuario = Auth::user();

        try {
            $recetaSurtida = $this->recetaMedicaService->surtirReceta(
                folio: $folio,
                farmaceuticoId: $usuario->id_usuario
            );

            return response()->json([
                'status' => 'success',
                'message' => "Receta con folio {$folio} surtida exitosamente. Código QR consumido de forma irreversible.",
                'data' => $recetaSurtida,
            ], 200);
        } catch (Exception $e) {
            // Reuso de QR consumido genera 409 Conflict
            return response()->json([
                'status' => 'error',
                'code' => 'REUSO_QR_BLOQUEADO',
                'message' => $e->getMessage(),
            ], 409);
        }
    }

    /**
     * Generación de PDF / Carga y descarga de impresión de receta médica oficial.
     */
    public function descargarPdf(Request $request, string $id): Response|JsonResponse
    {
        $receta = Receta::where('id_receta', $id)
            ->orWhere('folio', $id)
            ->firstOrFail();

        // Si es controlada y no está firmada, bloquea impresión
        if ($receta->esControlada() && empty($receta->firma_electronica)) {
            return response()->json([
                'status' => 'error',
                'code' => 'RECETA_CONTROLADA_SIN_FIRMA',
                'message' => 'Prohibida la emisión o impresión de recetas controladas sin validación previa de e.firma.',
            ], 409);
        }

        // Si solicita explícitamente JSON estructurado de metadatos
        if ($request->query('format') === 'json' || ($request->wantsJson() && !$request->has('download') && !$request->accepts('application/pdf'))) {
            return response()->json([
                'status' => 'success',
                'message' => 'Documento de receta médica listo para impresión.',
                'data' => [
                    'folio' => $receta->folio,
                    'fraccion' => $receta->fraccion,
                    'qr_codigo' => $receta->qr_codigo,
                    'qr_estado' => $receta->qr_estado,
                    'pdf_url' => url("/api/v1/recetas/{$receta->folio}/pdf?download=1"),
                    'sellado_tiempo_utc' => now('UTC')->toIso8601String(),
                ],
            ], 200);
        }

        $builder = $this->pdfPlantillasService->generarRecetaPdf($receta);
        $filename = "Receta-{$receta->folio}.pdf";

        return $request->boolean('inline')
            ? $builder->inlineResponse($filename)
            : $builder->downloadResponse($filename);
    }
}
