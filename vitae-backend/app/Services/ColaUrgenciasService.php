<?php

namespace App\Services;

use App\Models\SalaAtencion;
use App\Models\Triage;
use Carbon\Carbon;
use Illuminate\Support\Collection;

class ColaUrgenciasService
{
    /**
     * Procesa la asignación inicial de sala según prioridad Manchester.
     * Si el paciente es ROJO, salta la cola y busca una de las 3 salas de emergencia libres.
     * Si no hay sala disponible, se marca para Jefatura de Urgencias y queda en posición 0.
     */
    public function asignarSalaOCola(Triage $triage): array
    {
        if ($triage->color_asig === TriageManchesterService::ROJO) {
            $salaLibre = SalaAtencion::where('tipo', 'EMERGENCIA')
                ->where('estado', 'LIBRE')
                ->orderBy('id_sala', 'asc')
                ->first();

            if ($salaLibre) {
                $salaLibre->ocupar($triage->id_triage);
                $triage->update([
                    'id_sala' => $salaLibre->id_sala,
                    'estado_atencion' => 'EN_ATENCION',
                ]);

                return [
                    'asignado' => true,
                    'sala' => $salaLibre,
                    'alerta_jefatura' => false,
                    'mensaje' => "Paciente crítico ingresado de inmediato a {$salaLibre->nombre}.",
                ];
            }

            // Salas de emergencia saturadas: Alerta crítica inmediata a Jefatura
            return [
                'asignado' => false,
                'sala' => null,
                'alerta_jefatura' => true,
                'mensaje' => 'ALERTA CRÍTICA: 3/3 Salas de emergencia ocupadas. Paciente ROJO en posición 0 en espera de desahogo.',
            ];
        }

        // Casos regulares (NARANJA, AMARILLO, VERDE, AZUL) se forman en cola de espera
        return [
            'asignado' => false,
            'sala' => null,
            'alerta_jefatura' => false,
            'mensaje' => 'Paciente formado en cola de espera conforme a prioridad Manchester y tiempo transcurrido.',
        ];
    }

    /**
     * Obtiene la cola de urgencias viva con cálculo dinámico de prioridad, tiempo transcurrido y SLA.
     * Orden: Prioridad clínica (1 a 5), luego pacientes con SLA vencido o más próximos a vencer, luego FIFO.
     */
    public function obtenerColaViva(): Collection
    {
        $ahora = Carbon::now('UTC');

        $triages = Triage::with(['paciente', 'sala', 'enfermera'])
            ->whereIn('estado_atencion', ['EN_ESPERA', 'EN_ATENCION'])
            ->get();

        return $triages->map(function (Triage $t) use ($ahora) {
            $minutosEspera = (int) $t->fecha_hora_ingreso->diffInMinutes($ahora);
            $minutosRestantesSla = (int) $ahora->diffInMinutes($t->sla_vencimiento_en, false);
            $slaVencido = $minutosRestantesSla < 0;

            $pesoPrioridad = match ($t->color_asig) {
                'ROJO' => 1,
                'NARANJA' => 2,
                'AMARILLO' => 3,
                'VERDE' => 4,
                'AZUL' => 5,
                default => 6,
            };

            return [
                'id_triage' => $t->id_triage,
                'folio_ingreso' => $t->folio_ingreso,
                'id_paciente' => $t->id_paciente,
                'paciente_nombre' => $t->paciente ? $t->paciente->nombre_completo : 'N.N. No Identificado',
                'identificado' => $t->paciente ? $t->paciente->identificado : false,
                'color_asig' => $t->color_asig,
                'prioridad_nivel' => $pesoPrioridad,
                'fecha_hora_ingreso' => $t->fecha_hora_ingreso->toIso8601String(),
                'minutos_espera' => $minutosEspera,
                'sla_vencimiento_en' => $t->sla_vencimiento_en->toIso8601String(),
                'minutos_restantes_sla' => $minutosRestantesSla,
                'sla_vencido' => $slaVencido,
                'desviacion_minutos' => $slaVencido ? abs($minutosRestantesSla) : 0,
                'estado_atencion' => $t->estado_atencion,
                'id_sala' => $t->id_sala,
                'sala_nombre' => $t->sala?->nombre,
                'signos' => [
                    'presion_art' => $t->presion_art,
                    'frecuencia_card' => $t->frecuencia_card,
                    'frecuencia_resp' => $t->frecuencia_resp,
                    'temperatura' => $t->temperatura,
                    'spo2' => $t->spo2,
                    'nivel_glucosa' => $t->nivel_glucosa,
                    'escala_eva' => $t->escala_eva,
                    'nivel_conciencia' => $t->nivel_conciencia,
                ],
            ];
        })->sort(function ($a, $b) {
            // ROJO siempre primero
            if ($a['color_asig'] === 'ROJO' && $b['color_asig'] !== 'ROJO') return -1;
            if ($b['color_asig'] === 'ROJO' && $a['color_asig'] !== 'ROJO') return 1;

            // Por nivel de prioridad
            if ($a['prioridad_nivel'] !== $b['prioridad_nivel']) {
                return $a['prioridad_nivel'] <=> $b['prioridad_nivel'];
            }

            // Si tienen igual prioridad, los que tienen SLA vencido van antes
            if ($a['sla_vencido'] !== $b['sla_vencido']) {
                return $a['sla_vencido'] ? -1 : 1;
            }

            // Orden de ingreso (FIFO)
            return strcmp($a['fecha_hora_ingreso'], $b['fecha_hora_ingreso']);
        })->values();
    }

    /**
     * Retorna la vista pública de sala de espera disociada (PROV-UX-002 / LFPDPPP).
     * Cero datos sensibles (sin nombres completos, diagnósticos ni CURP).
     * Solo iniciales, folio, nivel Manchester con color e icono para accesibilidad.
     */
    public function obtenerSalaEsperaPublica(): array
    {
        $cola = $this->obtenerColaViva();

        $pacientesDisociados = $cola->map(function ($item, $indice) {
            $iniciales = $this->obtenerIniciales($item['paciente_nombre']);

            $iconoInfo = match ($item['color_asig']) {
                'ROJO' => ['icono' => 'alert-octagon', 'descripcion' => 'Reanimación Inmediata'],
                'NARANJA' => ['icono' => 'alert-triangle', 'descripcion' => 'Muy Urgente'],
                'AMARILLO' => ['icono' => 'clock', 'descripcion' => 'Urgente'],
                'VERDE' => ['icono' => 'check-circle', 'descripcion' => 'Poco Urgente'],
                'AZUL' => ['icono' => 'info', 'descripcion' => 'No Urgente'],
                default => ['icono' => 'help-circle', 'descripcion' => 'Valoración'],
            };

            return [
                'posicion' => $indice + 1,
                'folio' => $item['folio_ingreso'],
                'iniciales' => $iniciales,
                'color' => $item['color_asig'],
                'icono' => $iconoInfo['icono'],
                'estado_clinico' => $iconoInfo['descripcion'],
                'estado_atencion' => $item['estado_atencion'],
                'sala' => $item['sala_nombre'] ?? 'Sala de Espera Urgencias',
                'sla_vencido' => $item['sla_vencido'],
            ];
        });

        return [
            'total_en_espera' => $pacientesDisociados->where('estado_atencion', 'EN_ESPERA')->count(),
            'total_en_atencion' => $pacientesDisociados->where('estado_atencion', 'EN_ATENCION')->count(),
            'pacientes' => $pacientesDisociados,
            'ultima_actualizacion_utc' => Carbon::now('UTC')->toIso8601String(),
        ];
    }

    /**
     * Libera un consultorio o sala de emergencia y llama al siguiente paciente en espera.
     */
    public function liberarSala(string $idSala): array
    {
        $sala = SalaAtencion::findOrFail($idSala);

        // Si la sala tenía un triage asignado, lo marcamos como atendido
        if ($sala->id_triage_actual) {
            $triageAnterior = Triage::find($sala->id_triage_actual);
            if ($triageAnterior) {
                $triageAnterior->update(['estado_atencion' => 'ATENDIDO']);
            }
        }

        $sala->liberar();

        // Si es consultorio, asignar automáticamente al siguiente paciente regular en espera
        if ($sala->tipo === 'CONSULTORIO') {
            $siguiente = Triage::where('estado_atencion', 'EN_ESPERA')
                ->where('color_asig', '!=', 'ROJO')
                ->orderByRaw("
                    CASE color_asig
                        WHEN 'NARANJA' THEN 2
                        WHEN 'AMARILLO' THEN 3
                        WHEN 'VERDE' THEN 4
                        WHEN 'AZUL' THEN 5
                        ELSE 6
                    END ASC, fecha_hora_ingreso ASC
                ")
                ->first();

            if ($siguiente) {
                $sala->ocupar($siguiente->id_triage);
                $siguiente->update([
                    'id_sala' => $sala->id_sala,
                    'estado_atencion' => 'EN_ATENCION',
                ]);

                return [
                    'sala_liberada' => $sala->id_sala,
                    'siguiente_paciente_asignado' => $siguiente->folio_ingreso,
                    'mensaje' => "Consultorio {$sala->nombre} asignado al paciente con folio {$siguiente->folio_ingreso}.",
                ];
            }
        }

        return [
            'sala_liberada' => $sala->id_sala,
            'siguiente_paciente_asignado' => null,
            'mensaje' => "Sala {$sala->nombre} liberada y disponible.",
        ];
    }

    private function obtenerIniciales(string $nombre): string
    {
        $partes = preg_split('/\s+/', trim($nombre));
        $iniciales = '';
        foreach ($partes as $p) {
            if ($p !== '') {
                $iniciales .= mb_strtoupper(mb_substr($p, 0, 1)) . '.';
            }
        }
        return $iniciales !== '' ? $iniciales : 'N.N.';
    }
}
