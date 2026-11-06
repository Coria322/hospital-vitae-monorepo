<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class AntecedenteClinico extends Model
{
    use HasUuids;

    protected $table = 'antecedente_clinico';
    protected $primaryKey = 'id_antecedente';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_antecedente',
        'id_expediente',
        'tipo',
        'descripcion',
        'fecha_diagnostico',
        'observaciones',
    ];

    protected $casts = [
        'fecha_diagnostico' => 'date',
    ];

    public function expediente(): BelongsTo
    {
        return $this->belongsTo(ExpedienteClinico::class, 'id_expediente', 'id_expediente');
    }
}
