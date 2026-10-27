<?php

namespace App\Console\Commands;

use App\Services\BackupService;
use Illuminate\Console\Command;

class VerificarLagWalCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'db:verificar-lag-wal
                            {--simulate-lag= : Simular un lag en segundos para pruebas}
                            {--json : Salida en formato JSON estructurado para monitoreo}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Monitorea el lag de replicación y archivado continuo WAL (alerta si lag > 600s por PROV-SEC-006)';

    /**
     * Execute the console command.
     */
    public function handle(BackupService $backupService): int
    {
        $lagSimulado = $this->option('simulate-lag') !== null ? (int) $this->option('simulate-lag') : null;
        $resultado = $backupService->verificarLagArchivado($lagSimulado);

        if ($this->option('json')) {
            $this->line(json_encode($resultado, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES));
            return $resultado['alerta_firing'] ? Command::FAILURE : Command::SUCCESS;
        }

        $this->info("=== MONITOR DE ARCHIVADO CONTINUO / REPLICACIÓN WAL (PROV-SEC-006) ===");
        $this->line("Lag actual de replicación : {$resultado['lag_segundos']} segundos");
        $this->line("Límite crítico permitido  : {$resultado['limite_maximo_segundos']} segundos (10 min)");
        $this->line("Estado                    : {$resultado['estado']}");

        if ($resultado['alerta_firing']) {
            $this->error("✗ ALERTA CRÍTICA: wal_lag_seconds ({$resultado['lag_segundos']}s) supera los {$resultado['limite_maximo_segundos']}s.");
            $this->line("  Se ha notificado al equipo DevOps y registrado evento en auditoria_i.");
            return Command::FAILURE;
        }

        $this->info("✓ ESTADO ÓPTIMO: El archivado continuo se encuentra dentro del SLA.");
        return Command::SUCCESS;
    }
}
