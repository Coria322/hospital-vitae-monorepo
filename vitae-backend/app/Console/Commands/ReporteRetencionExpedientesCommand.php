<?php

namespace App\Console\Commands;

use App\Models\ExpedienteClinico;
use App\Services\AuditLogger;
use Carbon\Carbon;
use Illuminate\Console\Command;

class ReporteRetencionExpedientesCommand extends Command
{
    /**
     * The name and signature of the console command.
     */
    protected $signature = 'expedientes:reporte-retencion';

    /**
     * The console command description.
     */
    protected $description = 'Genera la relación periódica de expedientes clínicos con plazo legal de custodia cumplido (NOM-004-SSA3-2012 / TASK-W-033)';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $this->info('📋 Generando relación de expedientes con retención legal concluida (>= 1,825 días)...');

        $limiteRetencion = Carbon::now('UTC')->subDays(1825);

        $expedientes = ExpedienteClinico::where('fecha_ultimo_acto_medico', '<=', $limiteRetencion)
            ->where('estado_activo', true)
            ->with(['paciente', 'medicoTratante'])
            ->get();

        if ($expedientes->isEmpty()) {
            $this->info('✅ 0 expedientes con plazo concluido. Todos los expedientes se encuentran dentro del periodo de retención activa.');
            return Command::SUCCESS;
        }

        $filas = [];
        foreach ($expedientes as $exp) {
            $diasCustodia = (int) $exp->fecha_ultimo_acto_medico->diffInDays(Carbon::now('UTC'));
            $filas[] = [
                $exp->id_expediente,
                $exp->id_paciente,
                $exp->paciente?->nombre_completo ?? 'N/A',
                $exp->fecha_ultimo_acto_medico->format('Y-m-d H:i:s T'),
                "{$diasCustodia} días",
                $exp->bloqueo_legal ? 'BLOQUEADO' : 'CUSTODIA_CUMPLIDA',
            ];
        }

        $this->table(
            ['ID Expediente', 'UUID Paciente', 'Nombre Paciente', 'Último Acto Médico', 'Tiempo Custodia', 'Estado Legal'],
            $filas
        );

        $this->warn('⚠️ Conforme a NOM-004-SSA3-2012 y LFPDPPP, este reporte es informativo:');
        $this->line('   - Prohibida la eliminación física automatizada.');
        $this->line('   - La supresión o expurgo exige dictamen del Comité de Ética/Calidad, validación jurídica y acta de hechos.');

        // Auto-auditoría del reporte de retención
        AuditLogger::log(
            tipoMovimiento: 'REPORTE_RETENCION',
            tablaAfectada: 'expediente_c',
            registroId: 'RELACION_MENSUAL',
            valorAnterior: null,
            valorNuevo: ['total_expedientes_concluidos' => count($filas)],
            resultado: 'EXITOSO',
            motivo: 'Generación de reporte periódico de retención legal (NOM-004-SSA3-2012)'
        );

        return Command::SUCCESS;
    }
}
