<?php

namespace App\Console\Commands;

use App\Services\AuditLogger;
use App\Services\NtpService;
use Illuminate\Console\Command;

class VerificarSincronizacionHorariaCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'ntp:verificar-sincronizacion
                            {--max-offset=1.0 : Tolerancia máxima permitida de deriva horaria en segundos}
                            {--json : Salida en formato JSON estructurado}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Verifica la sincronización horaria contra fuentes NTP/CENAM y valida deriva <= 1s (PROV-SEC-007)';

    /**
     * Execute the console command.
     */
    public function handle(NtpService $ntpService): int
    {
        $maxOffset = (float) $this->option('max-offset');
        $asJson = (bool) $this->option('json');

        $resultado = $ntpService->medirDerivaHoraria();
        $offsetAbs = abs($resultado['offset_segundos']);
        $cumpleNormativa = $offsetAbs <= $maxOffset;

        if ($asJson) {
            $payload = array_merge($resultado, [
                'max_tolerancia_segundos' => $maxOffset,
                'cumple_normativa_cenam' => $cumpleNormativa,
                'timestamp_ejecucion_utc' => now('UTC')->toIso8601String(),
            ]);

            $this->line(json_encode($payload, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES));
            return $cumpleNormativa ? Command::SUCCESS : Command::FAILURE;
        }

        $this->info("=== VERIFICACIÓN DE SINCRONIZACIÓN HORARIA (PROV-SEC-007 / CENAM) ===");
        $this->line("Fuente de referencia : <comment>{$resultado['fuente']}</comment> ({$resultado['metodo']})");
        $this->line("Hora local (UTC)     : {$resultado['hora_local_utc']}");
        $this->line("Hora referencia (UTC): {$resultado['hora_referencia_utc']}");
        $this->line("Deriva horaria (offset): {$resultado['offset_ms']} ms ({$resultado['offset_segundos']} s)");
        $this->line("Tolerancia máxima    : {$maxOffset} s");

        if ($cumpleNormativa) {
            $this->info("✓ ESTADO: Sincronización horaria conforme con la norma (|offset| <= {$maxOffset}s).");
            return Command::SUCCESS;
        }

        $this->error("✗ ALERTA DE SEGURIDAD: Deriva horaria ({$resultado['offset_segundos']}s) supera el límite de {$maxOffset}s.");

        // Registrar evento de seguridad en bitácora inalterable
        try {
            AuditLogger::log(
                tipoMovimiento: 'ALERTA_DESINCRONIZACION_HORARIA',
                tablaAfectada: 'infraestructura',
                registroId: 'reloj_ntp',
                valorAnterior: null,
                valorNuevo: json_encode($resultado),
                resultado: 'FALLIDO',
                motivo: "Deriva horaria de {$resultado['offset_segundos']}s detectada contra fuente {$resultado['fuente']}",
                esNecesarioAuditoriaInterna: true,
                idUsuario: 'SISTEMA'
            );
        } catch (\Throwable $e) {
            $this->warn("No se pudo persistir auditoría: " . $e->getMessage());
        }

        return Command::FAILURE;
    }
}
