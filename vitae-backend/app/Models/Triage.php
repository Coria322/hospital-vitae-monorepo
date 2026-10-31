<?php

namespace App\Models;

use App\Services\AuditLogger;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use RuntimeException;

class Triage extends Model
{
    protected $table = 'triage';
    protected $primaryKey = 'id_triage';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_triage',
        'id_paciente',
        'id_usuario',
        'presion_art',
        'frecuencia_card',
        'frecuencia_resp',
        'temperatura',
        'spo2',
        'nivel_glucosa',
        'escala_eva',
        'nivel_conciencia',
        'color_asig',
        'color_original',
        'fecha_hora_ingreso',
        'estimacion_sangre_perdida',
        'folio_ingreso',
        'sla_vencimiento_en',
        'sla_vencido',
        'desviacion_minutos',
        'id_sala',
        'estado_atencion',
        'motivo_reclasificacion',
        'es_necesario_auditoria_interna',
        'captura_retroactiva',
    ];

    protected $casts = [
        'fecha_hora_ingreso' => 'datetime',
        'sla_vencimiento_en' => 'datetime',
        'sla_vencido' => 'boolean',
        'es_necesario_auditoria_interna' => 'boolean',
        'captura_retroactiva' => 'boolean',
        'frecuencia_card' => 'integer',
        'frecuencia_resp' => 'integer',
        'temperatura' => 'decimal:1',
        'spo2' => 'integer',
        'nivel_glucosa' => 'integer',
        'escala_eva' => 'integer',
        'desviacion_minutos' => 'integer',
    ];

    protected static function booted(): void
    {
        // Invariante NOM-004-SSA3-2012: Prohibido DELETE físico
        static::deleting(function () {
            throw new RuntimeException('Operación DELETE rechazada: Prohibido eliminar físicamente registros de triage conforme a NOM-004-SSA3-2012 y NOM-206-SSA1-2002.');
        });
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }

    public function enfermera(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    public function sala(): BelongsTo
    {
        return $this->belongsTo(SalaAtencion::class, 'id_sala', 'id_sala');
    }
}
