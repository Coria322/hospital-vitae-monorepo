<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use RuntimeException;

class NotaMedica extends Model
{
    use HasUuids;

    protected $table = 'nota_medica';
    protected $primaryKey = 'id_nota';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_nota',
        'id_expediente',
        'id_usuario',
        'tipo',
        'id_nota_original',
        'contenido',
        'diagnostico_cie',
        'plan_tratamiento',
        'firma_electronica',
        'fecha_hora',
        'cerrada',
        'hash_sha256',
    ];

    protected $casts = [
        'fecha_hora' => 'datetime',
        'cerrada' => 'boolean',
    ];

    protected static function booted(): void
    {
        // NOM-004-SSA3-2012 / NOM-024-SSA3-2012: Inmutabilidad de notas médicas cerradas
        static::updating(function (NotaMedica $nota) {
            if ($nota->getOriginal('cerrada')) {
                throw new RuntimeException('Operación UPDATE rechazada: Conforme a NOM-004-SSA3-2012 las notas médicas cerradas son inmutables. Use notas aclaratorias.');
            }
        });

        static::deleting(function (NotaMedica $nota) {
            throw new RuntimeException('Operación DELETE rechazada: Conforme a NOM-004-SSA3-2012 está prohibido eliminar notas médicas del expediente clínico.');
        });
    }

    public function expediente(): BelongsTo
    {
        return $this->belongsTo(ExpedienteClinico::class, 'id_expediente', 'id_expediente');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    public function notaOriginal(): BelongsTo
    {
        return $this->belongsTo(NotaMedica::class, 'id_nota_original', 'id_nota');
    }

    public function aclaraciones(): HasMany
    {
        return $this->hasMany(NotaMedica::class, 'id_nota_original', 'id_nota');
    }

    /**
     * Calcula el hash criptográfico SHA-256 de la nota médica.
     */
    public static function calcularHash(
        string $idExpediente,
        string $idUsuario,
        string $tipo,
        string $contenido,
        string $fechaHora
    ): string {
        return hash('sha256', "{$idExpediente}|{$idUsuario}|{$tipo}|{$contenido}|{$fechaHora}");
    }
}
