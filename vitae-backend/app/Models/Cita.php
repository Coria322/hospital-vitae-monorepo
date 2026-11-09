<?php

namespace App\Models;

use Carbon\Carbon;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Str;

class Cita extends Model
{
    protected $table = 'cita';
    protected $primaryKey = 'id_cita';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_cita',
        'id_paciente',
        'id_usuario',
        'fecha_hora',
        'estado',
        'especialidad',
        'motivo',
        'consultorio',
        'confirmada_por',
        'confirmada_en',
    ];

    protected function casts(): array
    {
        return [
            'fecha_hora' => 'datetime',
            'confirmada_en' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (Cita $cita) {
            if (empty($cita->id_cita)) {
                $cita->id_cita = (string) Str::uuid();
            }
            if (empty($cita->estado)) {
                $cita->estado = 'PENDIENTE';
            }
        });
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    public function confirmadaPorUsuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'confirmada_por', 'id_usuario');
    }

    /**
     * Confirma la cita médica (PROV-RF-002 / Recepción).
     */
    public function confirmar(string $idRecepcionista): void
    {
        if ($this->fecha_hora->isPast()) {
            throw new \InvalidArgumentException('No se puede confirmar una cita médica cuya fecha programada ya ha expirado.');
        }

        $this->update([
            'estado' => 'CONFIRMADA',
            'confirmada_por' => $idRecepcionista,
            'confirmada_en' => Carbon::now('UTC'),
        ]);
    }

    /**
     * Cancela la cita médica.
     */
    public function cancelar(?string $motivo = null): void
    {
        $this->update([
            'estado' => 'CANCELADA',
            'motivo' => $motivo ? ($this->motivo ? $this->motivo . " | Cancelación: " . $motivo : $motivo) : $this->motivo,
        ]);
    }
}

