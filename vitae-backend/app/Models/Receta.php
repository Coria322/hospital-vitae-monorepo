<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Receta extends Model
{
    protected $table = 'receta';
    protected $primaryKey = 'id_receta';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_receta',
        'id_expediente',
        'id_usuario',
        'folio',
        'fraccion',
        'medicamentos',
        'indicaciones',
        'firma_electronica',
        'fecha_hora_emision',
        'qr_codigo',
        'qr_estado',
        'hash_sha256',
        'surtida_en',
        'surtida_por',
    ];

    protected $casts = [
        'fecha_hora_emision' => 'datetime',
        'surtida_en' => 'datetime',
    ];

    public function expediente(): BelongsTo
    {
        return $this->belongsTo(ExpedienteClinico::class, 'id_expediente', 'id_expediente');
    }

    public function medico(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    /**
     * Determina si la receta es de medicamento controlado (Fracc. I, II o III).
     */
    public function esControlada(): bool
    {
        return in_array($this->fraccion, ['I', 'II', 'III'], true);
    }

    /**
     * Verifica si la receta está vigente para surtido.
     */
    public function puedeSurtirse(): bool
    {
        return $this->qr_estado === 'VIGENTE';
    }

    /**
     * Calcula el hash criptográfico SHA-256 de la receta.
     */
    public static function calcularHash(
        string $folio,
        string $idExpediente,
        string $idUsuario,
        string $fraccion,
        string $medicamentos,
        string $indicaciones,
        string $fechaEmision
    ): string {
        return hash('sha256', "{$folio}|{$idExpediente}|{$idUsuario}|{$fraccion}|{$medicamentos}|{$indicaciones}|{$fechaEmision}");
    }
}
