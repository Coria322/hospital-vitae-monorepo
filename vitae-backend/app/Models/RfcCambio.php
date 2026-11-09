<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Str;

class RfcCambio extends Model
{
    use HasFactory;

    protected $table = 'rfc_cambio';
    protected $primaryKey = 'id_rfc';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_rfc',
        'codigo_rfc',
        'titulo',
        'descripcion',
        'severidad',
        'modulo',
        'impacto_clinico',
        'analisis_riesgo',
        'plan_rollback',
        'estado',
        'solicitante_id',
        'aprobado_por',
        'fecha_aprobacion',
        'tiempo_estimado_resolucion_horas',
    ];

    protected $casts = [
        'fecha_aprobacion' => 'datetime',
        'tiempo_estimado_resolucion_horas' => 'integer',
    ];

    protected static function boot()
    {
        parent::boot();

        static::creating(function ($model) {
            if (empty($model->id_rfc)) {
                $model->id_rfc = (string) Str::uuid();
            }
            if (empty($model->codigo_rfc)) {
                $count = static::count() + 1;
                $model->codigo_rfc = 'RFC-' . date('Y') . '-' . str_pad($count, 3, '0', STR_PAD_LEFT);
            }
            if (empty($model->tiempo_estimado_resolucion_horas)) {
                // SLAs normativos según severidad ISO/IEC-20000
                $model->tiempo_estimado_resolucion_horas = match ($model->severidad) {
                    'P1' => 2,   // Crítico: <= 2 horas
                    'P2' => 8,   // Alto: <= 8 horas
                    default => 72 // P3 Medio/Bajo: <= 72 horas
                };
            }
        });
    }

    public function solicitante(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'solicitante_id', 'id_usuario');
    }

    public function aprobador(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'aprobado_por', 'id_usuario');
    }

    public function aprobar(string $aprobadorId): void
    {
        $this->update([
            'estado' => 'APROBADO',
            'aprobado_por' => $aprobadorId,
            'fecha_aprobacion' => now('UTC'),
        ]);
    }

    public function rechazar(string $aprobadorId): void
    {
        $this->update([
            'estado' => 'RECHAZADO',
            'aprobado_por' => $aprobadorId,
            'fecha_aprobacion' => now('UTC'),
        ]);
    }
}
