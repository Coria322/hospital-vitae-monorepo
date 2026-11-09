<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Conforme a DOMAIN_SCHEMA.json (ENT-011 CITA), PROV-RF-002, RF-011, RF-002.1 (Sprint 5 / AI-05)
     */
    public function up(): void
    {
        // 1. Agregar id_paciente a tabla usuario para vincular cuentas de pacientes (RF-002.1 / RF-011)
        if (Schema::hasTable('usuario') && !Schema::hasColumn('usuario', 'id_paciente')) {
            Schema::table('usuario', function (Blueprint $table) {
                $table->uuid('id_paciente')->nullable()->index();
            });
        }

        // 2. Tabla CITA (ENT-011 / PROV-RF-002 / NOM-024)
        Schema::dropIfExists('cita');
        Schema::create('cita', function (Blueprint $table) {
            $table->uuid('id_cita')->primary();
            $table->uuid('id_paciente');
            $table->string('id_usuario', 10)->nullable(); // Médico asignado
            $table->timestampTz('fecha_hora');
            $table->string('estado', 12)->default('PENDIENTE'); // PENDIENTE, CONFIRMADA, CANCELADA, ATENDIDA
            $table->string('especialidad', 60)->nullable();
            $table->text('motivo')->nullable();
            $table->string('consultorio', 30)->nullable();
            $table->string('confirmada_por', 10)->nullable(); // Usuario de RECEPCION que confirmó
            $table->timestampTz('confirmada_en')->nullable();
            $table->timestamps();

            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index(['id_paciente', 'estado']);
            $table->index('fecha_hora');
            $table->index('estado');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('cita');

        if (Schema::hasTable('usuario') && Schema::hasColumn('usuario', 'id_paciente')) {
            Schema::table('usuario', function (Blueprint $table) {
                $table->dropColumn('id_paciente');
            });
        }
    }
};

