<?php

namespace App\Console\Commands;

use App\Models\AuditoriaI;
use Illuminate\Console\Command;

class VerificarCadenaAuditoriaCommand extends Command
{
    /**
     * The name and signature of the console command.
     */
    protected $signature = 'auditoria:verificar-cadena';

    /**
     * The console command description.
     */
    protected $description = 'Verifica la integridad criptográfica de la cadena SHA-256 en la bitácora AUDITORIA_I (NOM-024 / PROV-SEC-005)';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $this->info('Iniciando verificación de integridad de la cadena AUDITORIA_I...');

        $registros = AuditoriaI::orderBy('id_auditoria', 'asc')->get();

        if ($registros->isEmpty()) {
            $this->info('La bitácora AUDITORIA_I está vacía. Cadena en estado génesis.');
            return Command::SUCCESS;
        }

        $prevHash = str_repeat('0', 64); // Génesis
        $totalVerificados = 0;

        foreach ($registros as $registro) {
            $canonico = AuditoriaI::buildCanonico($registro->getAttributes());
            $hashCalculado = hash('sha256', $canonico . $prevHash);

            if ($registro->huella_integridad !== $hashCalculado) {
                $this->error("RUPTURA DE INTEGRIDAD DETECTADA en el registro ID #{$registro->id_auditoria}");
                $this->line("   - Huella almacenada: {$registro->huella_integridad}");
                $this->line("   - Huella recalculada: {$hashCalculado}");
                $this->line("   - Huella previa:     {$prevHash}");
                $this->line("   - Fecha/Hora (UTC):   {$registro->fecha_hora}");
                $this->line("   - Tabla afectada:     {$registro->tabla_afectada}");

                return Command::FAILURE;
            }

            $prevHash = $registro->huella_integridad;
            $totalVerificados++;
        }

        $this->info("✅ Cadena 100% íntegra: {$totalVerificados} registros verificados con éxito conforme a NOM-024-SSA3-2012.");
        return Command::SUCCESS;
    }
}
