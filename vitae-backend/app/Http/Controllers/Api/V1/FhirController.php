<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Paciente;
use App\Models\Receta;
use App\Models\Triage;
use App\Services\AuditLogger;
use App\Services\FhirSerializerService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class FhirController extends Controller
{
    public function __construct(
        protected FhirSerializerService $fhirService
    ) {}

    /**
     * Endpoint HL7 FHIR R4: Patient (RNF-007).
     */
    public function getPatient(string $id): JsonResponse
    {
        $paciente = Paciente::where('id_paciente', $id)
            ->orWhere('curp', $id)
            ->first();

        if (!$paciente) {
            $outcome = $this->fhirService->toOperationOutcome(
                severity: 'error',
                code: 'not-found',
                diagnostics: "Recurso Patient con identificador {$id} no encontrado en Hospital Vitae."
            );

            return response()->json($outcome, 404, ['Content-Type' => 'application/fhir+json']);
        }

        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_FHIR',
            tablaAfectada: 'paciente',
            registroId: $paciente->id_paciente,
            valorAnterior: null,
            valorNuevo: ['recurso_fhir' => 'Patient'],
            resultado: 'EXITOSO',
            motivo: "Interoperabilidad semántica FHIR: Lectura de recurso Patient/{$paciente->id_paciente}"
        );

        return response()->json(
            $this->fhirService->toPatient($paciente),
            200,
            ['Content-Type' => 'application/fhir+json; charset=utf-8']
        );
    }

    /**
     * Endpoint HL7 FHIR R4: Encounter (RNF-007).
     */
    public function getEncounter(string $id): JsonResponse
    {
        $triage = Triage::find($id);

        if (!$triage) {
            $outcome = $this->fhirService->toOperationOutcome(
                severity: 'error',
                code: 'not-found',
                diagnostics: "Recurso Encounter con identificador {$id} no encontrado."
            );

            return response()->json($outcome, 404, ['Content-Type' => 'application/fhir+json']);
        }

        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_FHIR',
            tablaAfectada: 'triage',
            registroId: (string) $triage->id_triage,
            valorAnterior: null,
            valorNuevo: ['recurso_fhir' => 'Encounter'],
            resultado: 'EXITOSO',
            motivo: "Interoperabilidad semántica FHIR: Lectura de recurso Encounter/{$triage->id_triage}"
        );

        return response()->json(
            $this->fhirService->toEncounter($triage),
            200,
            ['Content-Type' => 'application/fhir+json; charset=utf-8']
        );
    }

    /**
     * Endpoint HL7 FHIR R4: Observation (Signos vitales con códigos LOINC) (RNF-007).
     */
    public function getObservation(string $id): JsonResponse
    {
        // Admitir ID directo de triage o prefijo 'obs-vital-'
        $cleanId = str_replace('obs-vital-', '', $id);
        $triage = Triage::find($cleanId);

        if (!$triage) {
            $outcome = $this->fhirService->toOperationOutcome(
                severity: 'error',
                code: 'not-found',
                diagnostics: "Recurso Observation con identificador {$id} no encontrado."
            );

            return response()->json($outcome, 404, ['Content-Type' => 'application/fhir+json']);
        }

        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_FHIR',
            tablaAfectada: 'triage',
            registroId: (string) $triage->id_triage,
            valorAnterior: null,
            valorNuevo: ['recurso_fhir' => 'Observation'],
            resultado: 'EXITOSO',
            motivo: "Interoperabilidad semántica FHIR: Lectura de Observation/obs-vital-{$triage->id_triage}"
        );

        return response()->json(
            $this->fhirService->toObservation($triage),
            200,
            ['Content-Type' => 'application/fhir+json; charset=utf-8']
        );
    }

    /**
     * Endpoint HL7 FHIR R4: MedicationRequest (RNF-007).
     */
    public function getMedicationRequest(string $id): JsonResponse
    {
        $receta = Receta::with('expediente')
            ->where('id_receta', $id)
            ->orWhere('folio', $id)
            ->first();

        if (!$receta) {
            $outcome = $this->fhirService->toOperationOutcome(
                severity: 'error',
                code: 'not-found',
                diagnostics: "Recurso MedicationRequest con identificador {$id} no encontrado."
            );

            return response()->json($outcome, 404, ['Content-Type' => 'application/fhir+json']);
        }

        AuditLogger::log(
            tipoMovimiento: 'CONSULTA_FHIR',
            tablaAfectada: 'receta',
            registroId: $receta->id_receta,
            valorAnterior: null,
            valorNuevo: ['recurso_fhir' => 'MedicationRequest'],
            resultado: 'EXITOSO',
            motivo: "Interoperabilidad semántica FHIR: Lectura de recurso MedicationRequest/{$receta->id_receta}"
        );

        return response()->json(
            $this->fhirService->toMedicationRequest($receta),
            200,
            ['Content-Type' => 'application/fhir+json; charset=utf-8']
        );
    }
}
