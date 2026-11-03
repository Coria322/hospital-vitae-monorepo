<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class SalaAtencion extends Model
{
    protected $table = 'sala_atencion';
    protected $primaryKey = 'id_sala';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_sala',
        'nombre',
        'tipo',
        'estado',
        'id_triage_actual',
    ];

    public function triages(): HasMany
    {
        return $this->hasMany(Triage::class, 'id_sala', 'id_sala');
    }

    public function triageActual(): BelongsTo
    {
        return $this->belongsTo(Triage::class, 'id_triage_actual', 'id_triage');
    }

    public function liberar(): void
    {
        $this->update([
            'estado' => 'LIBRE',
            'id_triage_actual' => null,
        ]);
    }

    public function ocupar(string $idTriage): void
    {
        $this->update([
            'estado' => 'OCUPADA',
            'id_triage_actual' => $idTriage,
        ]);
    }
}
