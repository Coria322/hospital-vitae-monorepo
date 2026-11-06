<?php

namespace App\Services;

use App\Models\Paciente;
use App\Models\Receta;
use App\Models\Triage;

class FhirSerializerService
{
    /**
     * Serializa un modelo Paciente a un recurso estándar HL7 FHIR R4 Patient (RNF-007).
     */
    public function toPatient(Paciente $paciente): array
    {
        $generoFhir = match (strtoupper($paciente->sexo)) {
            'MASCULINO', 'HOMBRE', 'M' => 'male',
            'FEMENINO', 'MUJER', 'F' => 'female',
            default => 'other',
        };

        return [
            'resourceType' => 'Patient',
            'id' => $paciente->id_paciente,
            'meta' => [
                'versionId' => '1',
                'lastUpdated' => $paciente->updated_at ? $paciente->updated_at->toIso8601String() : now('UTC')->toIso8601String(),
                'profile' => ['http://hl7.org/fhir/StructureDefinition/Patient'],
            ],
            'identifier' => [
                [
                    'use' => 'official',
                    'type' => [
                        'coding' => [
                            [
                                'system' => 'http://terminology.hl7.org/CodeSystem/v2-0203',
                                'code' => 'CZ', // Personal Identification Number
                                'display' => 'CURP',
                            ],
                        ],
                    ],
                    'system' => 'urn:oid:2.16.840.1.113883.4.629',
                    'value' => $paciente->curp ?? 'DESCONOCIDO',
                ],
            ],
            'active' => (bool) $paciente->estado_activo,
            'name' => [
                [
                    'use' => 'official',
                    'text' => $paciente->nombre_completo,
                ],
            ],
            'gender' => $generoFhir,
            'birthDate' => $paciente->fecha_nacimiento ? $paciente->fecha_nacimiento->format('Y-m-d') : null,
            'address' => [
                [
                    'use' => 'home',
                    'text' => $paciente->domicilio,
                ],
            ],
        ];
    }

    /**
     * Serializa un registro de Triage a un recurso estándar HL7 FHIR R4 Encounter (RNF-007).
     */
    public function toEncounter(Triage $triage): array
    {
        $nivel = $triage->color_asig ?? 'VERDE';

        return [
            'resourceType' => 'Encounter',
            'id' => (string) $triage->id_triage,
            'meta' => [
                'versionId' => '1',
                'lastUpdated' => $triage->updated_at ? $triage->updated_at->toIso8601String() : now('UTC')->toIso8601String(),
                'profile' => ['http://hl7.org/fhir/StructureDefinition/Encounter'],
            ],
            'status' => $triage->estado_atencion === 'ATENDIDO' ? 'finished' : 'in-progress',
            'class' => [
                'system' => 'http://terminology.hl7.org/CodeSystem/v3-ActCode',
                'code' => 'EMER',
                'display' => 'Emergency',
            ],
            'subject' => [
                'reference' => "Patient/{$triage->id_paciente}",
            ],
            'priority' => [
                'coding' => [
                    [
                        'system' => 'http://hospitalvitae.salud.gob.mx/fhir/CodeSystem/triage-manchester',
                        'code' => $nivel,
                        'display' => "Triage Manchester Prioridad {$nivel}",
                    ],
                ],
            ],
            'period' => [
                'start' => $triage->fecha_hora_ingreso ? $triage->fecha_hora_ingreso->toIso8601String() : null,
                'end' => $triage->updated_at ? $triage->updated_at->toIso8601String() : null,
            ],
        ];
    }

    /**
     * Serializa los signos vitales de un Triage a un recurso estándar HL7 FHIR R4 Observation (LOINC).
     */
    public function toObservation(Triage $triage): array
    {
        $components = [];

        // Presión arterial sistólica / diastólica (LOINC 85354-9)
        if ($triage->presion_art && str_contains($triage->presion_art, '/')) {
            [$sistolica, $diastolica] = explode('/', $triage->presion_art);
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '8480-6',
                            'display' => 'Systolic blood pressure',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) trim($sistolica),
                    'unit' => 'mmHg',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => 'mm[Hg]',
                ],
            ];
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '8462-4',
                            'display' => 'Diastolic blood pressure',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) trim($diastolica),
                    'unit' => 'mmHg',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => 'mm[Hg]',
                ],
            ];
        }

        // Frecuencia cardíaca (LOINC 8867-4)
        if ($triage->frecuencia_card) {
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '8867-4',
                            'display' => 'Heart rate',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) $triage->frecuencia_card,
                    'unit' => 'beats/minute',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => '/min',
                ],
            ];
        }

        // Frecuencia respiratoria (LOINC 9279-1)
        if ($triage->frecuencia_resp) {
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '9279-1',
                            'display' => 'Respiratory rate',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) $triage->frecuencia_resp,
                    'unit' => 'breaths/minute',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => '/min',
                ],
            ];
        }

        // Temperatura corporal (LOINC 8310-5)
        if ($triage->temperatura) {
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '8310-5',
                            'display' => 'Body temperature',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) $triage->temperatura,
                    'unit' => 'Cel',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => 'Cel',
                ],
            ];
        }

        // Saturación de oxígeno SpO2 (LOINC 2708-6)
        if ($triage->spo2) {
            $components[] = [
                'code' => [
                    'coding' => [
                        [
                            'system' => 'http://loinc.org',
                            'code' => '2708-6',
                            'display' => 'Oxygen saturation in Arterial blood',
                        ],
                    ],
                ],
                'valueQuantity' => [
                    'value' => (float) $triage->spo2,
                    'unit' => '%',
                    'system' => 'http://unitsofmeasure.org',
                    'code' => '%',
                ],
            ];
        }

        return [
            'resourceType' => 'Observation',
            'id' => "obs-vital-{$triage->id_triage}",
            'meta' => [
                'versionId' => '1',
                'lastUpdated' => $triage->updated_at ? $triage->updated_at->toIso8601String() : now('UTC')->toIso8601String(),
                'profile' => ['http://hl7.org/fhir/StructureDefinition/vitalsigns'],
            ],
            'status' => 'final',
            'category' => [
                [
                    'coding' => [
                        [
                            'system' => 'http://terminology.hl7.org/CodeSystem/observation-category',
                            'code' => 'vital-signs',
                            'display' => 'Vital Signs',
                        ],
                    ],
                ],
            ],
            'code' => [
                'coding' => [
                    [
                        'system' => 'http://loinc.org',
                        'code' => '85354-9',
                        'display' => 'Blood pressure panel with all children optional',
                    ],
                ],
            ],
            'subject' => [
                'reference' => "Patient/{$triage->id_paciente}",
            ],
            'effectiveDateTime' => $triage->fecha_hora_ingreso ? $triage->fecha_hora_ingreso->toIso8601String() : null,
            'component' => $components,
        ];
    }

    /**
     * Serializa una Receta a un recurso estándar HL7 FHIR R4 MedicationRequest (RNF-007).
     */
    public function toMedicationRequest(Receta $receta): array
    {
        $statusFhir = match ($receta->qr_estado) {
            'VIGENTE' => 'active',
            'CONSUMIDO' => 'completed',
            'CANCELADO' => 'cancelled',
            default => 'unknown',
        };

        $idPaciente = $receta->expediente ? $receta->expediente->id_paciente : 'PACIENTE-DESCONOCIDO';

        return [
            'resourceType' => 'MedicationRequest',
            'id' => $receta->id_receta,
            'meta' => [
                'versionId' => '1',
                'lastUpdated' => $receta->updated_at ? $receta->updated_at->toIso8601String() : now('UTC')->toIso8601String(),
                'profile' => ['http://hl7.org/fhir/StructureDefinition/MedicationRequest'],
            ],
            'identifier' => [
                [
                    'use' => 'official',
                    'system' => 'http://hospitalvitae.salud.gob.mx/fhir/recetas',
                    'value' => $receta->folio,
                ],
            ],
            'status' => $statusFhir,
            'intent' => 'order',
            'subject' => [
                'reference' => "Patient/{$idPaciente}",
            ],
            'authoredOn' => $receta->fecha_hora_emision ? $receta->fecha_hora_emision->toIso8601String() : null,
            'requester' => [
                'reference' => "Practitioner/{$receta->id_usuario}",
            ],
            'medicationCodeableConcept' => [
                'text' => $receta->medicamentos,
            ],
            'dosageInstruction' => [
                [
                    'text' => $receta->indicaciones,
                ],
            ],
        ];
    }

    /**
     * Construye un recurso FHIR OperationOutcome para respuestas de rechazo o error (HL7 FHIR R4).
     */
    public function toOperationOutcome(string $severity, string $code, string $diagnostics): array
    {
        return [
            'resourceType' => 'OperationOutcome',
            'issue' => [
                [
                    'severity' => $severity, // fatal | error | warning | information
                    'code' => $code, // processing | forbidden | not-found | etc.
                    'diagnostics' => $diagnostics,
                ],
            ],
        ];
    }
}
