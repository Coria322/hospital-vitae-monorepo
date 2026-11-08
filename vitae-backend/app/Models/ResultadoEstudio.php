<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ResultadoEstudio extends Model
{
    use HasUuids;

    protected $table = 'resultado_estudio';
    protected $primaryKey = 'id_resultado';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_resultado',
        'id_solicitud',
        'archivo_cifrado_ref',
        'mime',
        'cargado_por',
        'observaciones',
        'hash_sha256',
        'fecha_hora',
    ];

    protected $casts = [
        'fecha_hora' => 'datetime',
    ];

    public function solicitud(): BelongsTo
    {
        return $this->belongsTo(SolicitudEstudio::class, 'id_solicitud', 'id_solicitud');
    }

    public function laboratorista(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'cargado_por', 'id_usuario');
    }
}
