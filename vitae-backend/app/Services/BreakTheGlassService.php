<?php

namespace App\Services;

use App\Models\AccesoEmergencia;
use App\Models\ExpedienteClinico;
use App\Models\Triage;
use App\Models\Usuario;
use Carbon\Carbon;
use InvalidArgumentException;
use RuntimeException;

class BreakTheGlassService
{
    /**
     * Duración por defecto del acceso de emergencia temporal (120 minutos).
     */
    public const DURACION_DEFAULT_MINUTOS = 120;

    /**
     * Determina si el médico solicitante tiene relación asistencial activa con el paciente (RF-008).
     */
    public function tieneRelacionAsistencial(Usuario $usuario, ExpedienteClinico $expediente): bool
    {
        // 1. Médicos tratantes creadores del expediente
        if ($expediente->id_usuario === $usuario->id_usuario) {
            return true;
        }

        // 2. Pase de emergencia Break-the-Glass vigente
        return $this->tienePaseEmergenciaVigente($usuario->id_usuario, $expediente->id_paciente);
    }

    /**
     * Verifica si existe un pase de emergencia vigente en BD.
     */
    public function tienePaseEmergenciaVigente(string $idUsuario, string $idPaciente): bool
    {
        return AccesoEmergencia::where('id_usuario', $idUsuario)
            ->where('id_paciente', $idPaciente)
            ->where('estado', 'ACTIVO')
            ->where('expira_en', '>', Carbon::now('UTC'))
            ->exists();
    }

    /**
     * Activa el protocolo de emergencia Break-the-Glass y emite alerta de auditoría reforzada (PROV-RN-010).
     */
    public function otorgarAccesoEmergencia(
        Usuario $medico,
        string $idPaciente,
        string $motivo,
        int $duracionMinutos = self::DURACION_DEFAULT_MINUTOS
    ): AccesoEmergencia {
        if ($medico->rol !== 'MEDICO') {
            throw new RuntimeException('Rol no autorizado: El protocolo Break-the-Glass es exclusivo para personal médico.');
        }

        if (strlen(trim($motivo)) < 15) {
            throw new InvalidArgumentException('Justificación médica insuficiente: El motivo clínico de emergencia debe detallar al menos 15 caracteres.');
        }

        $ahora = Carbon::now('UTC');
        $expiraEn = $ahora->copy()->addMinutes($duracionMinutos);

        $acceso = AccesoEmergencia::create([
            'id_usuario' => $medico->id_usuario,
            'id_paciente' => $idPaciente,
            'motivo' => trim($motivo),
            'expira_en' => $expiraEn,
            'estado' => 'ACTIVO',
        ]);

        // Auditoría inalterable de máxima prioridad (NOM-024 / ISO-27001)
        AuditLogger::log(
            tipoMovimiento: 'ACCESO_BREAK_THE_GLASS',
            tablaAfectada: 'expediente_c',
            registroId: $idPaciente,
            valorAnterior: null,
            valorNuevo: [
                'id_acceso' => $acceso->id_acceso,
                'id_usuario' => $medico->id_usuario,
                'motivo_emergencia' => $motivo,
                'expira_en' => $expiraEn->toIso8601String(),
                'duracion_minutos' => $duracionMinutos,
            ],
            resultado: 'EXITOSO',
            motivo: "ALERTA CRÍTICA BREAK-THE-GLASS: Acceso de emergencia al expediente por {$medico->id_usuario}. Motivo: {$motivo}",
            esNecesarioAuditoriaInterna: true
        );

        return $acceso;
    }
}
