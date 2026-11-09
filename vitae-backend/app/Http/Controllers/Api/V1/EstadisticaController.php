<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Cita;
use App\Models\Receta;
use App\Models\Triage;
use App\Models\Usuario;
use App\Services\AuditLogger;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Symfony\Component\HttpFoundation\StreamedResponse;

class EstadisticaController extends Controller
{
    public const UMBRAL_K_ANONIMATO = 3; // Regla de k-anonimidad NOM-035 / AMB-017

    /**
     * Obtiene el identificador CLUES del establecimiento (PROV-RF-001).
     */
    protected function getClues(): string
    {
        return config('services.clues', env('CLUES_ESTABLECIMIENTO', 'CSSA001234'));
    }

    /**
     * Tablero de control estadístico agregado y disociado (PROV-RF-001 / NOM-035-STPS-2018).
     */
    public function tableros(Request $request): JsonResponse
    {
        $usuario = Auth::user();
        $clues = $this->getClues();

        // 1. Distribución de Triage Manchester (Rojo, Naranja, Amarillo, Verde, Azul)
        $triagesPorColor = Triage::selectRaw('color_asig, count(*) as total, avg(case when sla_vencido then 1 else 0 end)*100 as pct_sla_vencido')
            ->groupBy('color_asig')
            ->get()
            ->mapWithKeys(function ($item) {
                // Aplicar umbral k-anonimidad si total < 3
                $totalMostrado = $item->total < self::UMBRAL_K_ANONIMATO ? '<3' : (int) $item->total;
                return [$item->color_asig => [
                    'total' => $totalMostrado,
                    'pct_sla_vencido' => round((float) $item->pct_sla_vencido, 1),
                ]];
            });

        // 2. Recetas médicas emitidas por fracción legal (COFEPRIS)
        $recetasPorFraccion = Receta::selectRaw('fraccion, count(*) as total')
            ->groupBy('fraccion')
            ->get()
            ->mapWithKeys(function ($item) {
                $totalMostrado = $item->total < self::UMBRAL_K_ANONIMATO ? '<3' : (int) $item->total;
                return [$item->fraccion => $totalMostrado];
            });

        // 3. Citas hospitalarias por estado (PROV-RF-002)
        $citasPorEstado = Cita::selectRaw('estado, count(*) as total')
            ->groupBy('estado')
            ->get()
            ->mapWithKeys(function ($item) {
                $totalMostrado = $item->total < self::UMBRAL_K_ANONIMATO ? '<3' : (int) $item->total;
                return [$item->estado => $totalMostrado];
            });

        // 4. Indicadores de Salud Ocupacional y Estrés Asistencial (NOM-035-STPS-2018)
        // Agregados por rol sanitario, sin datos individuales
        $personalPorRol = Usuario::selectRaw('rol, count(*) as personal_activo')
            ->where('estado_cuenta', 'ACTIVA')
            ->groupBy('rol')
            ->get()
            ->mapWithKeys(function ($item) {
                return [$item->rol => (int) $item->personal_activo];
            });

        $totalPacientesAtendidos = Triage::where('estado_atencion', 'ATENDIDO')->count();
        $ratioCargaMedico = isset($personalPorRol['MEDICO']) && $personalPorRol['MEDICO'] > 0
            ? round($totalPacientesAtendidos / $personalPorRol['MEDICO'], 1)
            : 0;

        $payload = [
            'clues' => $clues,
            'establecimiento' => 'Hospital Vitae — Nivel de Atención II/III',
            'fecha_corte_utc' => Carbon::now('UTC')->toIso8601String(),
            'cumplimiento_privacidad' => [
                'disociacion_irreversible' => true,
                'cero_pii' => true,
                'k_anonimato_umbral' => self::UMBRAL_K_ANONIMATO,
            ],
            'metricas_urgencias_manchester' => $triagesPorColor,
            'metricas_recetas_controladas' => $recetasPorFraccion,
            'metricas_agenda_citas' => $citasPorEstado,
            'factores_riesgo_laboral_nom035' => [
                'personal_sanitario_distribucion' => $personalPorRol,
                'ratio_carga_asistencial_por_medico' => $ratioCargaMedico,
                'nivel_estres_ocupacional_estimado' => $ratioCargaMedico > 25 ? 'ALTO' : ($ratioCargaMedico > 10 ? 'MEDIO' : 'NORMAL'),
            ],
        ];

        // Registro de auditoría obligatorio (MN-09)
        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_ESTADISTICA_NOM035',
            tablaAfectada: 'estadisticas_nom035',
            registroId: $clues,
            valorAnterior: null,
            valorNuevo: ['clues' => $clues, 'fecha_corte' => $payload['fecha_corte_utc']],
            resultado: 'EXITOSO',
            motivo: 'Generación de tableros de control estadístico y salud laboral (PROV-RF-001 / NOM-035)',
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario->id_usuario
        );

        return response()->json([
            'status' => 'success',
            'data' => $payload,
        ], 200);
    }

    /**
     * Exportación de reporte estadístico NOM-035 en formato JSON o CSV disociado (PROV-RF-001).
     */
    public function exportar(Request $request)
    {
        $usuario = Auth::user();
        $clues = $this->getClues();
        $formato = strtolower($request->query('formato', 'json'));

        // Registro de auditoría para la exportación (MN-09)
        AuditLogger::log(
            tipoMovimiento: 'EXPORTACION_NOM035',
            tablaAfectada: 'estadisticas_nom035',
            registroId: $clues,
            valorAnterior: null,
            valorNuevo: ['clues' => $clues, 'formato' => $formato],
            resultado: 'EXITOSO',
            motivo: "Exportación formal de estadísticas disociadas NOM-035 en formato {$formato}",
            esNecesarioAuditoriaInterna: true,
            idUsuario: $usuario->id_usuario
        );

        if ($formato === 'csv') {
            $headers = [
                'Content-Type' => 'text/csv; charset=utf-8',
                'Content-Disposition' => "attachment; filename=\"reporte_nom035_{$clues}_" . date('Ymd_His') . ".csv\"",
            ];

            return response()->stream(function () use ($clues) {
                $handle = fopen('php://output', 'w');
                // Encabezados SIN ninguna columna de PII (nombre, curp, domicilio)
                fputcsv($handle, ['clues', 'categoria_metrica', 'concepto', 'valor', 'fecha_utc']);

                $ahora = Carbon::now('UTC')->toIso8601String();

                // Fila 1: Triages
                $totalTriages = Triage::count();
                fputcsv($handle, [$clues, 'URGENCIAS', 'TOTAL_PACIENTES_TRIAGE', $totalTriages, $ahora]);

                // Fila 2..N: Colores
                foreach (['ROJO', 'NARANJA', 'AMARILLO', 'VERDE', 'AZUL'] as $c) {
                    $cnt = Triage::where('color_asig', $c)->count();
                    fputcsv($handle, [$clues, 'TRIAGE_MANCHESTER', $c, $cnt < self::UMBRAL_K_ANONIMATO ? '<3' : $cnt, $ahora]);
                }

                // Fila Recetas
                foreach (['I', 'II', 'III', 'NINGUNA'] as $f) {
                    $cnt = Receta::where('fraccion', $f)->count();
                    fputcsv($handle, [$clues, 'RECETAS_CONTROLADAS', "FRACCION_{$f}", $cnt < self::UMBRAL_K_ANONIMATO ? '<3' : $cnt, $ahora]);
                }

                // Fila Citas
                foreach (['PENDIENTE', 'CONFIRMADA', 'CANCELADA'] as $st) {
                    $cnt = Cita::where('estado', $st)->count();
                    fputcsv($handle, [$clues, 'AGENDA_CITAS', $st, $cnt < self::UMBRAL_K_ANONIMATO ? '<3' : $cnt, $ahora]);
                }

                fclose($handle);
            }, 200, $headers);
        }

        // Formato JSON por defecto
        return $this->tableros($request);
    }
}

