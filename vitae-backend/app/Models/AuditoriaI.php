<?php

namespace App\Models;

use DateTimeInterface;
use Illuminate\Database\Eloquent\Model;
use RuntimeException;

class AuditoriaI extends Model
{
    protected $table = 'auditoria_i';
    protected $primaryKey = 'id_auditoria';

    // Desactivamos timestamps por defecto (se usa fecha_hora UTC)
    public $timestamps = false;

    protected $fillable = [
        'fecha_hora',
        'tipo_movimiento',
        'id_usuario',
        'tabla_afectada',
        'registro_id',
        'valor_anterior',
        'valor_nuevo',
        'origen',
        'resultado',
        'motivo',
        'es_necesario_auditoria_interna',
        'huella_integridad',
    ];

    protected $casts = [
        'fecha_hora' => 'datetime',
        'es_necesario_auditoria_interna' => 'boolean',
    ];

    /**
     * Invariante de Inalterabilidad NOM-024-SSA3-2012:
     * Prohibición absoluta de UPDATE y DELETE en Eloquent.
     */
    protected static function booted(): void
    {
        static::updating(function () {
            throw new RuntimeException('Operación UPDATE rechazada: Los registros en auditoria_i son inalterables (NOM-024-SSA3-2012).');
        });

        static::deleting(function () {
            throw new RuntimeException('Operación DELETE rechazada: Prohibido eliminar registros en auditoria_i (NOM-024-SSA3-2012).');
        });
    }

    /**
     * Construye la representación canónica del registro para el cálculo criptográfico.
     */
    public static function buildCanonico(array $data): string
    {
        $rawFechaHora = $data['fecha_hora'] ?? '';
        if ($rawFechaHora instanceof DateTimeInterface) {
            $fechaHora = $rawFechaHora->format('Y-m-d\TH:i:s\Z');
        } else {
            try {
                $fechaHora = \Carbon\Carbon::parse($rawFechaHora)->setTimezone('UTC')->format('Y-m-d\TH:i:s\Z');
            } catch (\Throwable) {
                $fechaHora = (string) $rawFechaHora;
            }
        }

        $valorAnterior = is_array($data['valor_anterior'] ?? null)
            ? json_encode($data['valor_anterior'], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE)
            : (string) ($data['valor_anterior'] ?? '');

        $valorNuevo = is_array($data['valor_nuevo'] ?? null)
            ? json_encode($data['valor_nuevo'], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE)
            : (string) ($data['valor_nuevo'] ?? '');

        $origen = is_array($data['origen'] ?? null)
            ? json_encode($data['origen'], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE)
            : (string) ($data['origen'] ?? '');

        return implode('|', [
            $fechaHora,
            (string) ($data['tipo_movimiento'] ?? ''),
            (string) ($data['id_usuario'] ?? ''),
            (string) ($data['tabla_afectada'] ?? ''),
            (string) ($data['registro_id'] ?? ''),
            $valorAnterior,
            $valorNuevo,
            $origen,
            (string) ($data['resultado'] ?? ''),
            (string) ($data['motivo'] ?? ''),
            !empty($data['es_necesario_auditoria_interna']) ? '1' : '0',
        ]);
    }
}
