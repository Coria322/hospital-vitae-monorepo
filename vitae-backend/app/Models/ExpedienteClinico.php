<?php

namespace App\Models;

use App\Services\AuditLogger;
use Carbon\Carbon;
use DateTimeInterface;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use RuntimeException;

class ExpedienteClinico extends Model
{
    protected $table = 'expediente_c';
    protected $primaryKey = 'id_expediente';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id_expediente',
        'id_paciente',
        'id_usuario',
        'motivo_consulta',
        'diagnostico',
        'estudios_previos',
        'fecha_creacion',
        'fecha_ultimo_acto_medico',
        'estado_activo',
        'bloqueo_legal',
    ];

    protected $casts = [
        'fecha_creacion' => 'datetime',
        'fecha_ultimo_acto_medico' => 'datetime',
        'estado_activo' => 'boolean',
        'bloqueo_legal' => 'boolean',
    ];

    protected static function booted(): void
    {
        // Invariante NOM-004-SSA3-2012: Prohibido DELETE físico
        static::deleting(function () {
            throw new RuntimeException('Operación DELETE rechazada: Prohibido eliminar físicamente expedientes clínicos conforme a NOM-004-SSA3-2012. Use baja lógica.');
        });
    }

    public function paciente(): BelongsTo
    {
        return $this->belongsTo(Paciente::class, 'id_paciente', 'id_paciente');
    }

    public function medicoTratante(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'id_usuario', 'id_usuario');
    }

    /**
     * Actualiza la fecha del último acto médico y registra auditoría (TASK-W-033 / NOM-004).
     */
    public function registrarActoMedico(?DateTimeInterface $fecha = null, ?string $motivo = null): void
    {
        $fechaNueva = $fecha ? Carbon::instance($fecha) : Carbon::now('UTC');
        $fechaAnterior = $this->fecha_ultimo_acto_medico;

        $this->update([
            'fecha_ultimo_acto_medico' => $fechaNueva,
        ]);

        AuditLogger::log(
            tipoMovimiento: 'UPDATE',
            tablaAfectada: 'expediente_c',
            registroId: $this->id_expediente,
            valorAnterior: ['fecha_ultimo_acto_medico' => $fechaAnterior?->toIso8601String()],
            valorNuevo: ['fecha_ultimo_acto_medico' => $fechaNueva->toIso8601String()],
            resultado: 'EXITOSO',
            motivo: $motivo ?? 'Actualización de fecha_ultimo_acto_medico'
        );
    }

    /**
     * Valida si el expediente ya cumplió el plazo legal mínimo de 5 años (1 825 días) de retención.
     */
    public function tieneRetencionConcluida(): bool
    {
        if (!$this->fecha_ultimo_acto_medico) {
            return false;
        }

        return $this->fecha_ultimo_acto_medico->copy()->addDays(1825)->isPast();
    }

    /**
     * Ejecuta la baja lógica del expediente clínico y genera evento en AUDITORIA_I.
     */
    public function bajaLogica(?string $motivo = null): void
    {
        $estadoAnterior = $this->estado_activo;
        $this->update(['estado_activo' => false]);

        AuditLogger::log(
            tipoMovimiento: 'SOFT_DELETE',
            tablaAfectada: 'expediente_c',
            registroId: $this->id_expediente,
            valorAnterior: ['estado_activo' => $estadoAnterior],
            valorNuevo: ['estado_activo' => false],
            resultado: 'EXITOSO',
            motivo: $motivo ?? 'Baja lógica de expediente clínico (NOM-004-SSA3-2012)'
        );
    }
}
