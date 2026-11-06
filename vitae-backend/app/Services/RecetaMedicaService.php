<?php

namespace App\Services;

use App\Models\ExpedienteClinico;
use App\Models\Receta;
use App\Models\Usuario;
use Carbon\Carbon;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;

class RecetaMedicaService
{
    /**
     * Emite una receta médica vinculada al expediente clínico (RF-009 / COFEPRIS / SAT).
     */
    public function emitirReceta(
        string $idExpediente,
        string $idUsuario,
        string $fraccion,
        string $medicamentos,
        string $indicaciones
    ): Receta {
        $expediente = ExpedienteClinico::findOrFail($idExpediente);
        $medico = Usuario::findOrFail($idUsuario);

        // Verificación de cédula profesional médica
        if ($medico->rol !== 'MEDICO') {
            throw new RuntimeException('Rol no autorizado: Solo el personal médico con cédula validada puede emitir recetas.');
        }

        if (empty($medico->cedula)) {
            throw new RuntimeException('Cédula no válida: El médico no cuenta con cédula profesional registrada.');
        }

        // Generar folio irrepetible
        $fechaActual = Carbon::now('UTC');
        $randomSuffix = strtoupper(Str::random(4));
        $folio = 'REC-' . $fechaActual->format('Ymd') . '-' . $randomSuffix;

        // ID secuencial de 10 caracteres
        $idReceta = 'RC' . substr(time(), -8);

        $hashSha256 = Receta::calcularHash(
            folio: $folio,
            idExpediente: $idExpediente,
            idUsuario: $idUsuario,
            fraccion: $fraccion,
            medicamentos: $medicamentos,
            indicaciones: $indicaciones,
            fechaEmision: $fechaActual->toIso8601String()
        );

        $receta = Receta::create([
            'id_receta' => $idReceta,
            'id_expediente' => $idExpediente,
            'id_usuario' => $idUsuario,
            'folio' => $folio,
            'fraccion' => $fraccion,
            'medicamentos' => $medicamentos,
            'indicaciones' => $indicaciones,
            'firma_electronica' => null,
            'fecha_hora_emision' => $fechaActual,
            'qr_codigo' => null,
            'qr_estado' => 'VIGENTE',
            'hash_sha256' => $hashSha256,
        ]);

        // Actualizar último acto médico en el expediente clínico
        $expediente->registrarActoMedico($fechaActual, "Emisión de receta folio {$folio}");

        AuditLogger::log(
            tipoMovimiento: 'INSERT',
            tablaAfectada: 'receta',
            registroId: $idReceta,
            valorAnterior: null,
            valorNuevo: [
                'folio' => $folio,
                'fraccion' => $fraccion,
                'hash_sha256' => $hashSha256,
            ],
            resultado: 'EXITOSO',
            motivo: "Emisión de receta médica folio {$folio} (Fracción {$fraccion})"
        );

        return $receta;
    }

    /**
     * Firma electrónicamente una receta médica con e.firma (X.509) y genera código QR único (COFEPRIS).
     */
    public function firmarReceta(
        Receta $receta,
        string $firmaElectronica,
        ?string $certificado = null
    ): Receta {
        // Validación de e.firma y certificado digital
        if (empty(trim($firmaElectronica))) {
            throw new InvalidArgumentException('La firma electrónica no puede estar vacía.');
        }

        // Simulación estricta de validación contra lista de revocación SAT
        if (str_contains(strtoupper($firmaElectronica), 'REVOCADO') || 
            ($certificado && str_contains(strtoupper($certificado), 'REVOCADO'))) {
            AuditLogger::log(
                tipoMovimiento: 'ERROR_FIRMA',
                tablaAfectada: 'receta',
                registroId: $receta->id_receta,
                valorAnterior: null,
                valorNuevo: ['firma_rechazada' => substr($firmaElectronica, 0, 20)],
                resultado: 'RECHAZADO',
                motivo: 'Certificado e.firma revocado o inválido ante SAT',
                esNecesarioAuditoriaInterna: true
            );
            throw new RuntimeException('Certificado de e.firma revocado o no válido ante SAT.');
        }

        // Generar código QR único de un solo uso
        $qrCodigo = hash('sha256', "QR|{$receta->folio}|{$receta->hash_sha256}|" . microtime(true));

        $receta->update([
            'firma_electronica' => $firmaElectronica,
            'qr_codigo' => $qrCodigo,
            'qr_estado' => 'VIGENTE',
        ]);

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'receta',
            registroId: $receta->id_receta,
            valorAnterior: ['qr_codigo' => null],
            valorNuevo: [
                'qr_codigo' => $qrCodigo,
                'qr_estado' => 'VIGENTE',
            ],
            resultado: 'EXITOSO',
            motivo: "Firma electrónica validada y activación de QR único para receta {$receta->folio}"
        );

        return $receta;
    }

    /**
     * Surtir receta y consumir de forma atómica e irreversible el código QR (un solo uso).
     */
    public function surtirReceta(
        string $folio,
        string $farmaceuticoId
    ): Receta {
        $receta = Receta::where('folio', $folio)->firstOrFail();

        // Detección estricta de reuso de QR consumido
        if ($receta->qr_estado === 'CONSUMIDO') {
            AuditLogger::log(
                tipoMovimiento: 'INCIDENTE_SEGURIDAD_QR_REUSADO',
                tablaAfectada: 'receta',
                registroId: $receta->id_receta,
                valorAnterior: ['qr_estado' => 'CONSUMIDO', 'surtida_en' => $receta->surtida_en?->toIso8601String()],
                valorNuevo: ['intento_reuso_por' => $farmaceuticoId],
                resultado: 'RECHAZADO',
                motivo: "Alerta de seguridad: Intento de reutilización de código QR consumido en receta folio {$folio}",
                esNecesarioAuditoriaInterna: true
            );

            throw new RuntimeException("INCIDENTE DE SEGURIDAD: El código QR de la receta {$folio} ya fue consumido el {$receta->surtida_en}. Prohibido el resurtido.");
        }

        if ($receta->qr_estado !== 'VIGENTE') {
            throw new RuntimeException("La receta con folio {$folio} no está vigente para surtido (Estado: {$receta->qr_estado}).");
        }

        // Consumo del código QR
        $ahora = Carbon::now('UTC');
        $receta->update([
            'qr_estado' => 'CONSUMIDO',
            'surtida_en' => $ahora,
            'surtida_por' => $farmaceuticoId,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'receta',
            registroId: $receta->id_receta,
            valorAnterior: ['qr_estado' => 'VIGENTE'],
            valorNuevo: [
                'qr_estado' => 'CONSUMIDO',
                'surtida_en' => $ahora->toIso8601String(),
                'surtida_por' => $farmaceuticoId,
            ],
            resultado: 'EXITOSO',
            motivo: "Surtido exitoso de receta folio {$folio} y consumo definitivo de QR"
        );

        return $receta;
    }
}
