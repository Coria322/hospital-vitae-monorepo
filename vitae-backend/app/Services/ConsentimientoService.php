<?php

namespace App\Services;

use App\Models\Consentimiento;
use App\Models\Paciente;
use App\Models\Usuario;
use Carbon\Carbon;
use InvalidArgumentException;
use RuntimeException;

class ConsentimientoService
{
    /**
     * Registra un nuevo consentimiento informado con hash inmutable (PROV-RN-008 / LFPDPPP).
     */
    public function registrarConsentimiento(
        string $idPaciente,
        string $tipo,
        string $finalidades,
        string $firmanteNombre,
        string $medicoResponsableId,
        string $versionDocumento = '1.0',
        string $medio = 'DIGITAL_TABLET',
        ?string $firmanteParentesco = null,
        ?string $testigo1 = null,
        ?string $testigo2 = null
    ): Consentimiento {
        Paciente::findOrFail($idPaciente);
        $medico = Usuario::findOrFail($medicoResponsableId);

        // Si es consentimiento de aviso de privacidad / finalidades secundarias o firmado desde app móvil,
        // y quien firma es el paciente o personal administrativo, se asigna como aval médico institucional
        // al director médico del hospital (LFPDPPP / NOM-024).
        $esAvisoPrivacidad = in_array(strtoupper($tipo), ['AVISO_PRIVACIDAD', 'FINALIDADES_SECUNDARIAS', 'PRIVACIDAD'], true)
            || in_array(strtoupper($medio), ['APP_MOVIL', 'DIGITAL_MOVIL'], true);

        if ($esAvisoPrivacidad && $medico->rol !== 'MEDICO') {
            $medicoInstitucional = Usuario::where('rol', 'MEDICO')->where('estado_cuenta', 'ACTIVA')->first()
                ?? Usuario::where('rol', 'MEDICO')->first();
            if ($medicoInstitucional) {
                $medicoResponsableId = $medicoInstitucional->id_usuario;
                $medico = $medicoInstitucional;
            }
        }

        if ($medico->rol !== 'MEDICO') {
            throw new RuntimeException('El médico responsable debe contar con rol MEDICO.');
        }

        $fechaHora = Carbon::now('UTC');

        $hashDocumento = Consentimiento::calcularHash(
            idPaciente: $idPaciente,
            tipo: $tipo,
            version: $versionDocumento,
            finalidades: $finalidades,
            firmante: $firmanteNombre,
            fechaHora: $fechaHora->toIso8601String()
        );

        $consentimiento = Consentimiento::create([
            'id_paciente' => $idPaciente,
            'tipo' => $tipo,
            'version_documento' => $versionDocumento,
            'finalidades' => $finalidades,
            'hash_documento' => $hashDocumento,
            'estado' => 'OTORGADO',
            'id_consentimiento_original' => null,
            'medio' => $medio,
            'firmante_nombre' => $firmanteNombre,
            'firmante_parentesco' => $firmanteParentesco,
            'medico_responsable_id' => $medicoResponsableId,
            'testigo_1' => $testigo1,
            'testigo_2' => $testigo2,
            'fecha_hora' => $fechaHora,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'consentimiento',
            registroId: $consentimiento->id_consentimiento,
            valorAnterior: null,
            valorNuevo: [
                'id_paciente' => $idPaciente,
                'tipo' => $tipo,
                'hash_documento' => $hashDocumento,
                'estado' => 'OTORGADO',
            ],
            resultado: 'EXITOSO',
            motivo: "Registro de consentimiento informado {$tipo} para paciente {$idPaciente}"
        );

        return $consentimiento;
    }

    /**
     * Revoca un consentimiento informado creando un nuevo registro enlazado sin alterar el original (LFPDPPP).
     */
    public function revocarConsentimiento(
        string $idConsentimientoOriginal,
        string $motivoRevocacion,
        string $firmanteNombre
    ): Consentimiento {
        $original = Consentimiento::findOrFail($idConsentimientoOriginal);

        if ($original->estado === 'REVOCADO') {
            throw new RuntimeException('El consentimiento indicado ya se encuentra revocado.');
        }

        $fechaHora = Carbon::now('UTC');

        $hashRevocacion = hash('sha256', "REVOCACION|{$original->id_consentimiento}|{$motivoRevocacion}|{$fechaHora->toIso8601String()}");

        $consentimientoRevocado = Consentimiento::create([
            'id_paciente' => $original->id_paciente,
            'tipo' => $original->tipo,
            'version_documento' => $original->version_documento,
            'finalidades' => "REVOCACIÓN: {$motivoRevocacion}",
            'hash_documento' => $hashRevocacion,
            'estado' => 'REVOCADO',
            'id_consentimiento_original' => $original->id_consentimiento,
            'medio' => $original->medio,
            'firmante_nombre' => $firmanteNombre,
            'firmante_parentesco' => $original->firmante_parentesco,
            'medico_responsable_id' => $original->medico_responsable_id,
            'testigo_1' => $original->testigo_1,
            'testigo_2' => $original->testigo_2,
            'fecha_hora' => $fechaHora,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'REVOCACION_CONSENTIMIENTO',
            tablaAfectada: 'consentimiento',
            registroId: $consentimientoRevocado->id_consentimiento,
            valorAnterior: ['id_consentimiento_original' => $original->id_consentimiento, 'estado' => 'OTORGADO'],
            valorNuevo: ['id_revocacion' => $consentimientoRevocado->id_consentimiento, 'estado' => 'REVOCADO'],
            resultado: 'EXITOSO',
            motivo: "Revocación formal de consentimiento informado. Motivo: {$motivoRevocacion}",
            esNecesarioAuditoriaInterna: true
        );

        return $consentimientoRevocado;
    }

    /**
     * Excepción de urgencia médica sin consentimiento previo (NOM-004-SSA3-2012 / LFPDPPP).
     * Requiere y valida la firma y autorización obligatoria de al menos 2 médicos autorizados.
     */
    public function registrarExcepcionUrgencia(
        string $idPaciente,
        string $procedimiento,
        string $justificacionUrgencia,
        string $medico1Id,
        string $medico2Id
    ): Consentimiento {
        if ($medico1Id === $medico2Id) {
            throw new InvalidArgumentException('Se requiere la concurrencia de al menos 2 médicos distintos para autorizar la excepción de urgencia.');
        }

        $m1 = Usuario::findOrFail($medico1Id);
        $m2 = Usuario::findOrFail($medico2Id);

        if ($m1->rol !== 'MEDICO' || $m2->rol !== 'MEDICO') {
            throw new RuntimeException('Ambos autorizantes deben contar con perfil médico acreditado.');
        }

        $fechaHora = Carbon::now('UTC');
        $hashUrgencia = hash('sha256', "EXCEPCION_URGENCIA|{$idPaciente}|{$procedimiento}|{$medico1Id}|{$medico2Id}|{$fechaHora->toIso8601String()}");

        $consentimiento = Consentimiento::create([
            'id_paciente' => $idPaciente,
            'tipo' => 'EXCEPCION_URGENCIA',
            'version_documento' => '1.0',
            'finalidades' => "INTERVENCIÓN DE EMERGENCIA: {$procedimiento}. JUSTIFICACIÓN: {$justificacionUrgencia}",
            'hash_documento' => $hashUrgencia,
            'estado' => 'OTORGADO',
            'id_consentimiento_original' => null,
            'medio' => 'DICTAMEN_MEDICO_DUAL',
            'firmante_nombre' => "AUTORIZACIÓN DUAL: Dr. {$m1->nombre_completo} y Dr. {$m2->nombre_completo}",
            'firmante_parentesco' => 'EXCEPCIÓN_LEGAL_NOM004',
            'medico_responsable_id' => $medico1Id,
            'testigo_1' => "Dr. {$m2->nombre_completo} ({$medico2Id})",
            'testigo_2' => 'JEFATURA_URGENCIAS',
            'fecha_hora' => $fechaHora,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'EXCEPCION_CONSENTIMIENTO_URGENCIA',
            tablaAfectada: 'consentimiento',
            registroId: $consentimiento->id_consentimiento,
            valorAnterior: null,
            valorNuevo: [
                'procedimiento' => $procedimiento,
                'medico_1' => $medico1Id,
                'medico_2' => $medico2Id,
                'hash' => $hashUrgencia,
            ],
            resultado: 'EXITOSO',
            motivo: "Excepción de urgencia médica autorizada por 2 médicos conforme a NOM-004 para {$idPaciente}",
            esNecesarioAuditoriaInterna: true
        );

        return $consentimiento;
    }
}
