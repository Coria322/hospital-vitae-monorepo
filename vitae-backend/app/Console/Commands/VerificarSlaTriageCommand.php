<?php

namespace App\Console\Commands;

use App\Models\Triage;
use App\Services\AuditLogger;
use Carbon\Carbon;
use Illuminate\Console\Command;

class VerificarSlaTriageCommand extends Command
{
    protected $signature = 'triage:verificar-sla';
    protected $description = 'Verifica y escala pacientes con SLA excedido en Urgencias conforme a NOM-206-SSA1-2002 y PROV-RN-005';

    public function handle(): int
    {
        $ahora = Carbon::now('UTC');

        $triagesPorVencer = Triage::where('estado_atencion', 'EN_ESPERA')
            ->where('sla_vencimiento_en', '<=', $ahora)
            ->get();

        $contadorVencidos = 0;

        foreach ($triagesPorVencer as $triage) {
            $desviacionMinutos = (int) $triage->sla_vencimiento_en->diffInMinutes($ahora);

            // Si no estaba marcado previamente como vencido, lo marcamos y auditamos
            if (!$triage->sla_vencido) {
                $triage->update([
                    'sla_vencido' => true,
                    'desviacion_minutos' => $desviacionMinutos,
                ]);

                AuditLogger::log(
                    tipoMovimiento: 'SLA_VENCIDO',
                    tablaAfectada: 'triage',
                    registroId: $triage->id_triage,
                    valorAnterior: ['sla_vencido' => false],
                    valorNuevo: [
                        'sla_vencido' => true,
                        'desviacion_minutos' => $desviacionMinutos,
                        'color_asig' => $triage->color_asig,
                        'folio_ingreso' => $triage->folio_ingreso,
                    ],
                    resultado: 'EXITOSO',
                    motivo: "SLA rebasado para nivel {$triage->color_asig}: desviación de {$desviacionMinutos} minutos sin atención iniciada."
                );

                $contadorVencidos++;
            } else {
                // Actualizar la desviación en minutos acumulada
                $triage->update([
                    'desviacion_minutos' => $desviacionMinutos,
                ]);
            }
        }

        $this->info("Monitoreo SLA completado. Pacientes recién marcados con SLA vencido: {$contadorVencidos}.");

        return Command::SUCCESS;
    }
}
