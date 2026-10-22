<?php

namespace App\Services;

use App\Models\AuditoriaI;
use Carbon\Carbon;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Request;

class AuditLogger
{
    /**
     * Claves sensibles que nunca deben persistirse en los logs de auditoría (ISO-27001).
     */
    protected const SENSITIVE_KEYS = [
        'password',
        'password_hash',
        'password_confirmation',
        'token',
        'access_token',
        'refresh_token',
        'bearer_token',
        'otp',
        'secret',
        'authorization',
    ];

    /**
     * Registra un evento de auditoría con encadenamiento criptográfico SHA-256 (NOM-024-SSA3-2012).
     */
    public static function log(
        string $tipoMovimiento,
        string $tablaAfectada,
        string $registroId,
        mixed $valorAnterior = null,
        mixed $valorNuevo = null,
        string $resultado = 'EXITOSO',
        ?string $motivo = null,
        bool $esNecesarioAuditoriaInterna = false,
        ?string $idUsuario = null
    ): AuditoriaI {
        return DB::transaction(function () use (
            $tipoMovimiento,
            $tablaAfectada,
            $registroId,
            $valorAnterior,
            $valorNuevo,
            $resultado,
            $motivo,
            $esNecesarioAuditoriaInterna,
            $idUsuario
        ) {
            // 1. Resolver usuario actor
            $usuarioActivo = Auth::user();
            $actorId = $idUsuario 
                ?? ($usuarioActivo ? $usuarioActivo->id_usuario : null)
                ?? 'SISTEMA';

            $rolActivo = $usuarioActivo ? ($usuarioActivo->rol ?? 'DESCONOCIDO') : 'PUBLIC';

            // 2. Sanitizar datos sensibles
            $sanitizedAnterior = self::sanitize($valorAnterior);
            $sanitizedNuevo = self::sanitize($valorNuevo);

            // 3. Estructurar origen en JSON
            $origenPayload = [
                'ip' => Request::ip() ?? '127.0.0.1',
                'client' => Request::header('User-Agent') ?? 'CLI/System',
                'rol_activo' => $rolActivo,
            ];

            // 4. Marca de tiempo en UTC estricto
            $fechaHoraUtc = Carbon::now('UTC')->format('Y-m-d\TH:i:s\Z');

            $data = [
                'fecha_hora' => $fechaHoraUtc,
                'tipo_movimiento' => strtoupper($tipoMovimiento),
                'id_usuario' => (string) $actorId,
                'tabla_afectada' => (string) $tablaAfectada,
                'registro_id' => (string) $registroId,
                'valor_anterior' => $sanitizedAnterior ? json_encode($sanitizedAnterior, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : null,
                'valor_nuevo' => $sanitizedNuevo ? json_encode($sanitizedNuevo, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : null,
                'origen' => json_encode($origenPayload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE),
                'resultado' => strtoupper($resultado),
                'motivo' => $motivo,
                'es_necesario_auditoria_interna' => $esNecesarioAuditoriaInterna,
            ];

            // 5. Obtener huella previa (Génesis: 64 ceros)
            $prevHuella = AuditoriaI::orderBy('id_auditoria', 'desc')
                ->lockForUpdate()
                ->value('huella_integridad') ?? str_repeat('0', 64);

            // 6. Generar string canónico y calcular nuevo SHA-256
            $canonico = AuditoriaI::buildCanonico($data);
            $data['huella_integridad'] = hash('sha256', $canonico . $prevHuella);

            return AuditoriaI::create($data);
        });
    }

    /**
     * Sanitiza arrays u objetos removiendo claves que contengan secretos o credenciales.
     */
    protected static function sanitize(mixed $data): mixed
    {
        if (is_null($data)) {
            return null;
        }

        if (is_string($data)) {
            $decoded = json_decode($data, true);
            if (json_last_error() === JSON_ERROR_NONE && is_array($decoded)) {
                return self::sanitize($decoded);
            }
            return $data;
        }

        if (is_array($data)) {
            $cleaned = [];
            foreach ($data as $key => $value) {
                if (in_array(strtolower((string) $key), self::SENSITIVE_KEYS, true)) {
                    $cleaned[$key] = '[REDACTED]';
                } elseif (is_array($value)) {
                    $cleaned[$key] = self::sanitize($value);
                } else {
                    $cleaned[$key] = $value;
                }
            }
            return $cleaned;
        }

        return $data;
    }
}
