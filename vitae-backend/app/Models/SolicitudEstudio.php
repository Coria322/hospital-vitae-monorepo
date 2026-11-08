<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

class SolicitudEstudio extends Model
{
    use HasUuids;

    protected $table = 'solicitud_estudio';
    protected $primaryKey = 'id_solicitud';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_solicitud',
        'id_expediente',
        'id_usuario',
        'tipo_estudio',
        'prioridad',
        'indicaciones_clinicas',
        'estado',
        'fecha_hora_solicitud',
    ];

    protected $casts = [
        'fecha_hora_solicitud' => 'datetime',
    ];

    public function expediente(): BelongsTo
    {
        return $this->belongsTo(ExpedienteClinico::class, 'id_expediente', 'id_expediente');
    }

    public function medicoSolicitante(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    public function resultado(): HasOne
    {
        return $this->hasOne(ResultadoEstudio::class, 'id_solicitud', 'id_solicitud');
    }
}
