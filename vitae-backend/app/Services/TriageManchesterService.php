<?php

namespace App\Services;

use InvalidArgumentException;

class TriageManchesterService
{
    public const ROJO = 'ROJO';
    public const NARANJA = 'NARANJA';
    public const AMARILLO = 'AMARILLO';
    public const VERDE = 'VERDE';
    public const AZUL = 'AZUL';

    public const NIVELES_PRIORIDAD = [
        self::ROJO => 1,
        self::NARANJA => 2,
        self::AMARILLO => 3,
        self::VERDE => 4,
        self::AZUL => 5,
    ];

    public const SLA_MINUTOS = [
        self::ROJO => 0,       // Atención Inmediata
        self::NARANJA => 15,   // Muy urgente
        self::AMARILLO => 60,  // Urgente
        self::VERDE => 120,    // Poco urgente
        self::AZUL => 240,     // No urgente
    ];

    /**
     * Evalúa los signos vitales y estado clínico del paciente para determinar
     * de forma determinista el color Manchester según NOM-206-SSA1-2002.
     *
     * @param array $datos
     * @return array ['color' => string, 'sla_minutos' => int, 'justificacion_algoritmo' => string]
     */
    public function clasificar(array $datos): array
    {
        $this->validarRangosFisiologicos($datos);

        $nivelConciencia = strtoupper($datos['nivel_conciencia'] ?? 'ALERTA');
        $frecuenciaCard = (int) ($datos['frecuencia_card'] ?? 80);
        $escalaEva = (int) ($datos['escala_eva'] ?? 0);
        $presionArt = $datos['presion_art'] ?? '120/80';
        $estimacionSangre = strtoupper($datos['estimacion_sangre_perdida'] ?? 'NINGUNA');
        $spo2 = isset($datos['spo2']) ? (int) $datos['spo2'] : null;
        $temperatura = isset($datos['temperatura']) ? (float) $datos['temperatura'] : null;

        // Parsear presión arterial
        [$sistolica, $diastolica] = $this->parsePresionArterial($presionArt);

        // 1. REGLA CRÍTICA INVIOLABLE (RF-005): Si nivel_conciencia='INCONSCIENTE' fuerza ROJO
        if ($nivelConciencia === 'INCONSCIENTE') {
            return [
                'color' => self::ROJO,
                'sla_minutos' => self::SLA_MINUTOS[self::ROJO],
                'justificacion_algoritmo' => 'Paciente inconsciente (AVDI=I): Emergencia crítica con riesgo inminente de vida.',
            ];
        }

        // Otros criterios de ROJO (Resucitación / Shock)
        if ($estimacionSangre === 'MASIVA' || $estimacionSangre === 'INCONTROLABLE' ||
            $frecuenciaCard < 40 || $frecuenciaCard > 160 ||
            $sistolica < 70 || ($spo2 !== null && $spo2 < 80)) {
            return [
                'color' => self::ROJO,
                'sla_minutos' => self::SLA_MINUTOS[self::ROJO],
                'justificacion_algoritmo' => 'Signos vitales incompatibles con estabilidad hemodinámica inmediata (Shock / Paro potencial).',
            ];
        }

        // 2. CRITERIOS DE NARANJA (Muy urgente - 15 min)
        if ($nivelConciencia === 'DOLOR' ||
            $escalaEva >= 9 ||
            $frecuenciaCard > 130 ||
            $sistolica >= 200 || $diastolica >= 120 ||
            $sistolica <= 85 ||
            ($spo2 !== null && $spo2 < 90) ||
            $estimacionSangre === 'MODERADA') {
            return [
                'color' => self::NARANJA,
                'sla_minutos' => self::SLA_MINUTOS[self::NARANJA],
                'justificacion_algoritmo' => 'Dolor severo (EVA >= 9), compromiso respiratorio o hipertensión/hipotensión crítica.',
            ];
        }

        // 3. CRITERIOS DE AMARILLO (Urgente - 60 min)
        if ($nivelConciencia === 'VERBAL' ||
            ($escalaEva >= 6 && $escalaEva <= 8) ||
            ($frecuenciaCard >= 105 && $frecuenciaCard <= 130) ||
            ($sistolica >= 160 && $sistolica < 200) ||
            ($temperatura !== null && ($temperatura >= 39.0 || $temperatura < 35.5)) ||
            ($spo2 !== null && $spo2 < 94) ||
            $estimacionSangre === 'LEVE') {
            return [
                'color' => self::AMARILLO,
                'sla_minutos' => self::SLA_MINUTOS[self::AMARILLO],
                'justificacion_algoritmo' => 'Dolor moderado a severo, respuesta solo a la voz o fiebre elevada sin descompensación.',
            ];
        }

        // 4. CRITERIOS DE VERDE (Poco urgente - 120 min)
        if (($escalaEva >= 3 && $escalaEva <= 5) ||
            ($temperatura !== null && $temperatura >= 38.0 && $temperatura < 39.0)) {
            return [
                'color' => self::VERDE,
                'sla_minutos' => self::SLA_MINUTOS[self::VERDE],
                'justificacion_algoritmo' => 'Condición aguda estable, dolor leve a moderado sin compromiso sistémico.',
            ];
        }

        // 5. CRITERIOS DE AZUL (No urgente - 240 min)
        return [
            'color' => self::AZUL,
            'sla_minutos' => self::SLA_MINUTOS[self::AZUL],
            'justificacion_algoritmo' => 'Condición no urgente, signos vitales normales y dolor mínimo o nulo.',
        ];
    }

    /**
     * Valida que los valores ingresados correspondan a rangos biológicos creíbles.
     * Rechaza casos anómalos (ej. FC=999) con HTTP 422 / InvalidArgumentException (TC-TRG-005).
     */
    public function validarRangosFisiologicos(array $datos): void
    {
        if (isset($datos['frecuencia_card'])) {
            $fc = (int) $datos['frecuencia_card'];
            if ($fc < 20 || $fc > 300) {
                throw new InvalidArgumentException("La frecuencia cardíaca ({$fc} lpm) está fuera del rango fisiológico permitido (20 - 300).");
            }
        }

        if (isset($datos['frecuencia_resp'])) {
            $fr = (int) $datos['frecuencia_resp'];
            if ($fr < 4 || $fr > 80) {
                throw new InvalidArgumentException("La frecuencia respiratoria ({$fr} rpm) está fuera del rango fisiológico permitido (4 - 80).");
            }
        }

        if (isset($datos['temperatura'])) {
            $temp = (float) $datos['temperatura'];
            if ($temp < 25.0 || $temp > 45.0) {
                throw new InvalidArgumentException("La temperatura ({$temp} °C) está fuera del rango fisiológico permitido (25.0 - 45.0).");
            }
        }

        if (isset($datos['spo2'])) {
            $spo2 = (int) $datos['spo2'];
            if ($spo2 < 40 || $spo2 > 100) {
                throw new InvalidArgumentException("La saturación de oxígeno ({$spo2} %) debe estar entre 40 y 100.");
            }
        }

        if (isset($datos['nivel_glucosa']) && $datos['nivel_glucosa'] !== null) {
            $glucosa = (int) $datos['nivel_glucosa'];
            if ($glucosa < 10 || $glucosa > 1200) {
                throw new InvalidArgumentException("El nivel de glucosa ({$glucosa} mg/dL) está fuera del rango medible (10 - 1200).");
            }
        }

        if (isset($datos['escala_eva'])) {
            $eva = (int) $datos['escala_eva'];
            if ($eva < 0 || $eva > 10) {
                throw new InvalidArgumentException("La escala EVA de dolor ({$eva}) debe estar comprendida entre 0 y 10.");
            }
        }

        if (isset($datos['presion_art'])) {
            $this->parsePresionArterial($datos['presion_art']);
        }
    }

    /**
     * Parsea y valida el formato 'sistolica/diastolica'.
     */
    public function parsePresionArterial(string $pa): array
    {
        if (!preg_match('/^(\d{2,3})\/(\d{2,3})$/', trim($pa), $matches)) {
            throw new InvalidArgumentException("Formato de presión arterial inválido ('{$pa}'). Se espera formato 'sis/dia', ej: '120/80'.");
        }

        $sistolica = (int) $matches[1];
        $diastolica = (int) $matches[2];

        if ($sistolica < 40 || $sistolica > 300) {
            throw new InvalidArgumentException("Presión sistólica ({$sistolica} mmHg) fuera de rango biológico (40 - 300).");
        }
        if ($diastolica < 20 || $diastolica > 200) {
            throw new InvalidArgumentException("Presión diastólica ({$diastolica} mmHg) fuera de rango biológico (20 - 200).");
        }
        if ($diastolica >= $sistolica) {
            throw new InvalidArgumentException("La presión diastólica ({$diastolica}) no puede ser mayor o igual a la sistólica ({$sistolica}).");
        }

        return [$sistolica, $diastolica];
    }

    /**
     * Valida si una reclasificación manual es permitida.
     * Bloquea terminantemente la degradación de casos en ROJO (TC-TRG-004).
     */
    public function validarReclasificacion(string $colorActual, string $nuevoColor, string $motivo): void
    {
        if (trim($motivo) === '') {
            throw new InvalidArgumentException("La reclasificación de triage exige una justificación clínica obligatoria.");
        }

        $colorActual = strtoupper($colorActual);
        $nuevoColor = strtoupper($nuevoColor);

        if (!array_key_exists($colorActual, self::NIVELES_PRIORIDAD) || !array_key_exists($nuevoColor, self::NIVELES_PRIORIDAD)) {
            throw new InvalidArgumentException("Color de triage desconocido.");
        }

        // Invariante de seguridad: Prohibido degradar un triage ROJO a menor urgencia
        if ($colorActual === self::ROJO && $nuevoColor !== self::ROJO) {
            throw new InvalidArgumentException("NOM-206-SSA1-2002: Prohibida la degradación de un nivel ROJO (Emergencia crítica). Debe mantenerse bajo atención médica inmediata en shock.");
        }
    }
}
