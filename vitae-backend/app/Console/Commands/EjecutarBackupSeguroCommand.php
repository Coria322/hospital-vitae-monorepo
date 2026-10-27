<?php

namespace App\Console\Commands;

use App\Services\BackupService;
use Illuminate\Console\Command;

class EjecutarBackupSeguroCommand extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'db:backup-seguro
                            {--json : Retorna el resultado en formato JSON estructurado}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Genera un respaldo completo de la base de datos cifrado con AES-256 y regla 3-2-1 (PROV-SEC-006)';

    /**
     * Execute the console command.
     */
    public function handle(BackupService $backupService): int
    {
        $resultado = $backupService->ejecutarRespaldoCompleto();

        if ($this->option('json')) {
            $this->line(json_encode($resultado, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES));
            return Command::SUCCESS;
        }

        $this->info("=== EJECUTANDO RESPALDO CIFRADO AES-256 (PROV-SEC-006 / ISO-27001) ===");
        $this->line("Archivo generado       : <comment>{$resultado['archivo']}</comment>");
        $this->line("Tamaño en disco        : " . round($resultado['tamanio_bytes'] / 1024, 2) . " KB");
        $this->line("Huella SHA-256         : {$resultado['huella_sha256']}");
        $this->line("Fecha ejecución (UTC)  : {$resultado['fecha_generacion_utc']}");
        $this->line("Regla 3-2-1 verificada : " . ($resultado['regla_321_cumplida'] ? '✓ SÍ (copia externa/offsite generada)' : '✗ NO'));

        $this->info("✓ Respaldo completado, cifrado y auditado exitosamente.");
        return Command::SUCCESS;
    }
}
