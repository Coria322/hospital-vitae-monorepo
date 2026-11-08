<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class SolicitudArco extends Model
{
    protected $table = 'solicitud_arco';
    protected $primaryKey = 'folio';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'folio',
        'id_paciente',
        'derecho',
        'fecha_recepcion',
        'respuesta_limite',
        'ejecucion_limite',
        'estado',
        'motivo_improcedencia',
        'acreditacion_identidad',
        'documento_acreditacion',
        'observaciones',
    ];

    protected $casts = [
        'fecha_recepcion' => 'date',
        'respuesta_limite' => 'date',
        'ejecucion_limite' => 'date',
        'acreditacion_identidad' => 'boolean',
    ];

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }
}
