<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::dropIfExists('usuario');
        Schema::create('usuario', function (Blueprint $table) {
            $table->string('id_usuario', 10)->primary();
            $table->string('nombre_completo', 100);
            $table->string('rol', 20);
            $table->string('password_hash', 60);
            $table->string('cedula', 10)->nullable();
            $table->string('estado_cuenta', 12)->default('ACTIVA');
            $table->smallInteger('intentos_fallidos')->default(0);
            $table->timestamp('bloqueado_hasta')->nullable();
            $table->boolean('mfa_habilitado')->default(false);
            $table->timestamp('ultimo_acceso_en')->nullable();
            $table->boolean('debe_cambiar_password')->default(false);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('usuario');
    }
};
