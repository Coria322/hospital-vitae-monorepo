<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use RuntimeException;

class Consentimiento extends Model
{
    use HasUuids;

    protected $table = 'consentimiento';
    protected $primaryKey = 'id_consentimiento';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_consentimiento',
        'id_paciente',
        'tipo',
        'version_documento',
        'finalidades',
        'hash_documento',
        'estado',
        'id_consentimiento_original',
        'medio',
        'firmante_nombre',
        'firmante_parentesco',
        'medico_responsable_id',
        'testigo_1',
        'testigo_2',
        'fecha_hora',
    ];

    protected $casts = [
        'fecha_hora' => 'datetime',
    ];

    protected static function booted(): void
    {
        // LFPDPPP / NOM-004-SSA3-2012: Inmutabilidad del consentimiento informado
        static::updating(function () {
            throw new RuntimeException('Operación UPDATE rechazada: El consentimiento informado es inmutable. Registre una revocación.');
        });

        static::deleting(function () {
            throw new RuntimeException('Operación DELETE rechazada: Prohibido eliminar registros de consentimiento informado.');
        });
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }

    public function medicoResponsable(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'medico_responsable_id', 'id_usuario');
    }

    public function consentimientoOriginal(): BelongsTo
    {
        return $this->belongsTo(Consentimiento::class, 'id_consentimiento_original', 'id_consentimiento');
    }

    /**
     * Calcula el hash criptográfico SHA-256 del consentimiento informado.
     */
    public static function calcularHash(
        string $idPaciente,
        string $tipo,
        string $version,
        string $finalidades,
        string $firmante,
        string $fechaHora
    ): string {
        return hash('sha256', "{$idPaciente}|{$tipo}|{$version}|{$finalidades}|{$firmante}|{$fechaHora}");
    }
}
