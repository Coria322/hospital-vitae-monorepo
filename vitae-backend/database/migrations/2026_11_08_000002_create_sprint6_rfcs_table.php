<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Sprint 6 (Auditoría AI-06): Control formal de cambios post-liberación (RFC) y gobernanza ISO/IEC-20000.
     */
    public function up(): void
    {
        if (!Schema::hasTable('rfc_cambio')) {
            Schema::create('rfc_cambio', function (Blueprint $table) {
                $table->uuid('id_rfc')->primary();
                $table->string('codigo_rfc', 20)->unique();
                $table->string('titulo', 200);
                $table->text('descripcion');
                $table->string('severidad', 5); // P1, P2, P3
                $table->string('modulo', 50);
                $table->text('impacto_clinico');
                $table->text('analisis_riesgo');
                $table->text('plan_rollback');
                $table->string('estado', 20)->default('BORRADOR'); // BORRADOR, EN_REVISION, APROBADO, RECHAZADO, APLICADO
                $table->string('solicitante_id', 20);
                $table->string('aprobado_por', 20)->nullable();
                $table->timestamp('fecha_aprobacion')->nullable();
                $table->unsignedInteger('tiempo_estimado_resolucion_horas')->default(72);
                $table->timestamps();

                $table->foreign('solicitante_id')->references('id_usuario')->on('usuario');
                $table->foreign('aprobado_por')->references('id_usuario')->on('usuario');
                $table->index(['severidad', 'estado']);
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('rfc_cambio');
    }
};
