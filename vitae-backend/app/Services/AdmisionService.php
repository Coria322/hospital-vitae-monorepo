<?php

namespace App\Services;

use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Triage;
use App\Services\AuditLogger;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;

class AdmisionService
{
    /**
     * Registra un paciente, permitiendo el caso especial de paciente no identificado (N.N. / Inconsciente).
     * Conforme a NOM-004-SSA3-2012 y PROV-RN-007.
     */
    public function registrarPaciente(array $datos): Paciente
    {
        $identificado = $datos['identificado'] ?? true;

        if (!$identificado) {
            $uuid = (string) Str::uuid();
            $folioUrgencias = 'URG-NN-' . strtoupper(substr(md5($uuid), 0, 6));
            $sexo = $datos['sexo'] ?? 'DESCONOCIDO';
            $nombre = $datos['nombre_completo'] ?? "DESCONOCIDO ({$sexo}) - FOLIO {$folioUrgencias}";

            $paciente = Paciente::create([
                'id_paciente' => $uuid,
                'curp' => null, // N.N. no tiene CURP
                'nombre_completo' => $nombre,
                'fecha_nacimiento' => $datos['fecha_nacimiento'] ?? '2000-01-01',
                'sexo' => $sexo,
                'domicilio' => $datos['domicilio'] ?? 'Encontrado en vía pública / Sin domicilio confirmado',
                'seguro_activo' => false,
                'estado_activo' => true,
                'identificado' => false,
                'folio_urgencias' => $folioUrgencias,
                'representante_legal' => $datos['representante_legal'] ?? null,
            ]);

            AuditLogger::log(
                tipoMovimiento: 'INSERT',
                tablaAfectada: 'paciente',
                registroId: $paciente->id_paciente,
                valorAnterior: null,
                valorNuevo: ['identificado' => false, 'folio_urgencias' => $folioUrgencias],
                resultado: 'EXITOSO',
                motivo: 'Registro de paciente no identificado / desconocido en urgencias (PROV-RN-007)'
            );

            return $paciente;
        }

        // Paciente regular identificado
        if (empty($datos['curp'])) {
            throw new InvalidArgumentException("La CURP es obligatoria para pacientes identificados.");
        }

        $pacienteExistente = Paciente::where('curp', $datos['curp'])->first();
        if ($pacienteExistente) {
            throw new InvalidArgumentException("Ya existe un paciente registrado con la CURP {$datos['curp']}.");
        }

        $paciente = Paciente::create([
            'curp' => strtoupper(trim($datos['curp'])),
            'nombre_completo' => trim($datos['nombre_completo']),
            'fecha_nacimiento' => $datos['fecha_nacimiento'],
            'sexo' => $datos['sexo'],
            'domicilio' => $datos['domicilio'] ?? 'No especificado',
            'seguro_activo' => $datos['seguro_activo'] ?? true,
            'estado_activo' => true,
            'identificado' => true,
            'folio_urgencias' => $datos['folio_urgencias'] ?? null,
            'representante_legal' => $datos['representante_legal'] ?? null,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'paciente',
            registroId: $paciente->id_paciente,
            valorAnterior: null,
            valorNuevo: ['curp' => $paciente->curp, 'nombre_completo' => $paciente->nombre_completo],
            resultado: 'EXITOSO',
            motivo: 'Alta de paciente identificado en admisión'
        );

        return $paciente;
    }

    /**
     * Fusiona un paciente no identificado / temporal con un paciente definitivo autenticado.
     * Reasigna todo el historial clínico (triages, expedientes), inhabilita el registro temporal
     * y genera una entrada auditada inalterable (NOM-004-SSA3-2012 / PROV-RN-007).
     */
    public function fusionarPacientes(string $idTemporal, string $idDefinitivo, string $motivo, ?string $idUsuario = null): array
    {
        if (trim($motivo) === '') {
            throw new InvalidArgumentException("La justificación clínica y legal para la fusión de expedientes es obligatoria.");
        }

        if ($idTemporal === $idDefinitivo) {
            throw new InvalidArgumentException("No se puede fusionar un paciente consigo mismo.");
        }

        return DB::transaction(function () use ($idTemporal, $idDefinitivo, $motivo, $idUsuario) {
            $pacienteTemporal = Paciente::findOrFail($idTemporal);
            $pacienteDefinitivo = Paciente::findOrFail($idDefinitivo);

            if (!$pacienteDefinitivo->estado_activo) {
                throw new RuntimeException("El paciente definitivo se encuentra en estado inactivo.");
            }

            // Reasignar triages del paciente temporal al definitivo
            $triagesAfectados = Triage::where('id_paciente', $idTemporal)->count();
            Triage::where('id_paciente', $idTemporal)->update([
                'id_paciente' => $idDefinitivo,
            ]);

            // Reasignar expedientes si existieran
            $expedientesAfectados = ExpedienteClinico::where('id_paciente', $idTemporal)->count();
            if ($expedientesAfectados > 0) {
                // Si el definitivo ya tiene expediente, actualizamos fecha del último acto médico
                $expedienteDefinitivo = ExpedienteClinico::where('id_paciente', $idDefinitivo)->first();
                if ($expedienteDefinitivo) {
                    $expedienteTemporal = ExpedienteClinico::where('id_paciente', $idTemporal)->first();
                    $expedienteDefinitivo->update([
                        'fecha_ultimo_acto_medico' => now('UTC'),
                        'motivo_consulta' => $expedienteDefinitivo->motivo_consulta . "\n[FUSIÓN DE EXPEDIENTE N.N.]: " . ($expedienteTemporal?->motivo_consulta ?? ''),
                    ]);
                    // Baja lógica del expediente temporal
                    $expedienteTemporal?->update(['estado_activo' => false]);
                } else {
                    ExpedienteClinico::where('id_paciente', $idTemporal)->update([
                        'id_paciente' => $idDefinitivo,
                    ]);
                }
            }

            // Baja lógica del paciente temporal
            $pacienteTemporal->update([
                'estado_activo' => false,
                'representante_legal' => "FUSIONADO CON PACIENTE DEFINITIVO: {$idDefinitivo}",
            ]);

            // Auditoría inalterable
            AuditLogger::log(
                tipoMovimiento: 'FUSION_PACIENTES',
                tablaAfectada: 'paciente',
                registroId: $idTemporal,
                valorAnterior: [
                    'id_paciente_temporal' => $idTemporal,
                    'nombre_temporal' => $pacienteTemporal->nombre_completo,
                    'triages_migrados' => $triagesAfectados,
                ],
                valorNuevo: [
                    'id_paciente_definitivo' => $idDefinitivo,
                    'nombre_definitivo' => $pacienteDefinitivo->nombre_completo,
                    'estado_activo_temporal' => false,
                ],
                resultado: 'EXITOSO',
                motivo: $motivo
            );

            return [
                'status' => 'success',
                'id_temporal' => $idTemporal,
                'id_definitivo' => $idDefinitivo,
                'triages_migrados' => $triagesAfectados,
                'expedientes_migrados' => $expedientesAfectados,
                'mensaje' => 'Fusión de pacientes y unificación de expediente clínico completada exitosamente.',
            ];
        });
    }
}
