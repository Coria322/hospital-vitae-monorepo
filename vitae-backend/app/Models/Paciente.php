<?php

namespace App\Models;

use App\Services\AuditLogger;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Support\Str;
use RuntimeException;

class Paciente extends Model
{
    protected $table = 'paciente';
    protected $primaryKey = 'id_paciente';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_paciente',
        'curp',
        'nombre_completo',
        'fecha_nacimiento',
        'sexo',
        'domicilio',
        'seguro_activo',
        'estado_activo',
        'identificado',
        'folio_urgencias',
        'representante_legal',
    ];

    protected $casts = [
        'fecha_nacimiento' => 'date',
        'seguro_activo' => 'boolean',
        'estado_activo' => 'boolean',
        'identificado' => 'boolean',
    ];

    protected static function booted(): void
    {
        static::creating(function (Paciente $paciente) {
            if (empty($paciente->id_paciente)) {
                $paciente->id_paciente = (string) Str::uuid();
            }
        });

        // Invariante NOM-004-SSA3-2012: Prohibido DELETE físico
        static::deleting(function () {
            throw new RuntimeException('Operación DELETE rechazada: Prohibido eliminar físicamente registros de pacientes conforme a NOM-004-SSA3-2012. Use baja lógica.');
        });
    }

    public function expediente(): HasOne
    {
        return $this->hasOne(ExpedienteClinico::class, 'id_paciente', 'id_paciente');
    }

    /**
     * Ejecuta la baja lógica del paciente y genera evento en AUDITORIA_I.
     */
    public function bajaLogica(?string $motivo = null): void
    {
        $estadoAnterior = $this->estado_activo;
        $this->update(['estado_activo' => false]);

        AuditLogger::log(
            tipoMovimiento: 'SOFT_DELETE',
            tablaAfectada: 'paciente',
            registroId: $this->id_paciente,
            valorAnterior: ['estado_activo' => $estadoAnterior],
            valorNuevo: ['estado_activo' => false],
            resultado: 'EXITOSO',
            motivo: $motivo ?? 'Baja lógica de paciente (NOM-004-SSA3-2012)'
        );
    }
}
