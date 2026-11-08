<?php

namespace App\Services;

use App\Models\ExpedienteClinico;
use App\Models\Usuario;
use Carbon\Carbon;

class PdfExpedienteService
{
    /**
     * Genera un extracto de expediente clínico auditado y protegido con marcas de agua dinámicas (PROV-SEC-008).
     */
    public function exportarExpedienteSeguro(
        ExpedienteClinico $expediente,
        Usuario $solicitante,
        string $ipOrigen,
        bool $enmascararPii = false
    ): array {
        $fechaHoraUtc = Carbon::now('UTC')->toIso8601String();
        $textoMarcaAgua = "COPIA CLÍNICA AUDITADA - USO INTERNO | DOC-ID: {$expediente->id_expediente} | SOLICITANTE: {$solicitante->id_usuario} | FECHA: {$fechaHoraUtc} | IP: {$ipOrigen}";

        $paciente = $expediente->paciente;

        // Enmascaramiento de datos personales si no es vista asistencial directa
        $curpVisible = $paciente->curp;
        $domicilioVisible = $paciente->domicilio;

        if ($enmascararPii && $curpVisible) {
            $curpVisible = substr($curpVisible, 0, 4) . '**********' . substr($curpVisible, -4);
            $domicilioVisible = '[DIRECCIÓN OFICIAL RESERVADA - LFPDPPP]';
        }

        $documentoEstructurado = [
            'encabezado' => [
                'institucion' => 'Hospital Vitae — Sistema de Información Hospitalaria',
                'certificaciones' => ['NOM-004-SSA3-2012', 'NOM-024-SSA3-2012', 'ISO/IEC-27001'],
                'marca_agua_seguridad' => $textoMarcaAgua,
                'generado_en_utc' => $fechaHoraUtc,
                'solicitado_por' => [
                    'id_usuario' => $solicitante->id_usuario,
                    'nombre' => $solicitante->nombre_completo,
                    'rol' => $solicitante->rol,
                ],
            ],
            'expediente' => [
                'id_expediente' => $expediente->id_expediente,
                'fecha_creacion' => $expediente->fecha_creacion?->toIso8601String(),
                'fecha_ultimo_acto_medico' => $expediente->fecha_ultimo_acto_medico?->toIso8601String(),
                'motivo_consulta' => $expediente->motivo_consulta,
                'diagnostico' => $expediente->diagnostico,
            ],
            'paciente' => [
                'id_paciente' => $paciente->id_paciente,
                'nombre_completo' => $paciente->nombre_completo,
                'curp' => $curpVisible,
                'fecha_nacimiento' => $paciente->fecha_nacimiento?->format('Y-m-d'),
                'sexo' => $paciente->sexo,
                'domicilio' => $domicilioVisible,
            ],
            'antecedentes' => $expediente->antecedentes->map(fn($a) => [
                'tipo' => $a->tipo,
                'descripcion' => $a->descripcion,
                'fecha_diagnostico' => $a->fecha_diagnostico?->format('Y-m-d'),
            ]),
            'notas_medicas' => $expediente->notasMedicas->map(fn($n) => [
                'id_nota' => $n->id_nota,
                'tipo' => $n->tipo,
                'autor' => $n->id_usuario,
                'fecha_hora' => $n->fecha_hora?->toIso8601String(),
                'contenido' => $n->contenido,
                'hash_sha256' => $n->hash_sha256,
                'es_aclaratoria' => !empty($n->id_nota_original),
            ]),
            'recetas' => $expediente->recetas->map(fn($r) => [
                'folio' => $r->folio,
                'fraccion' => $r->fraccion,
                'medicamentos' => $r->medicamentos,
                'qr_estado' => $r->qr_estado,
            ]),
            'hash_integridad_exportacion' => hash('sha256', "{$expediente->id_expediente}|{$fechaHoraUtc}|{$solicitante->id_usuario}"),
        ];

        // Registro obligatorio en AUDITORIA_I
        AuditLogger::log(
            tipoMovimiento: 'EXPORTACION_DOCUMENTO',
            tablaAfectada: 'expediente_c',
            registroId: $expediente->id_expediente,
            valorAnterior: null,
            valorNuevo: [
                'id_expediente' => $expediente->id_expediente,
                'solicitado_por' => $solicitante->id_usuario,
                'ip_origen' => $ipOrigen,
                'pii_enmascarada' => $enmascararPii,
                'hash_exportacion' => $documentoEstructurado['hash_integridad_exportacion'],
            ],
            resultado: 'EXITOSO',
            motivo: "Exportación clínica segura de expediente con marca de agua dinámica por {$solicitante->id_usuario}"
        );

        return $documentoEstructurado;
    }
}
