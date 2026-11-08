<?php

namespace App\Models;

use Carbon\Carbon;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class AccesoEmergencia extends Model
{
    use HasUuids;

    protected $table = 'acceso_emergencia';
    protected $primaryKey = 'id_acceso';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_acceso',
        'id_usuario',
        'id_paciente',
        'motivo',
        'expira_en',
        'estado',
    ];

    protected $casts = [
        'expira_en' => 'datetime',
    ];

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }

    /**
     * Valida si el pase de emergencia sigue activo y no ha expirado.
     */
    public function esVigente(): bool
    {
        return $this->estado === 'ACTIVO' && $this->expira_en && $this->expira_en->isFuture();
    }
}
