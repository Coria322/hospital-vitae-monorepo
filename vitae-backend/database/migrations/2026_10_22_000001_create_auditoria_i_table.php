<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Conforme a NOM-024-SSA3-2012, ISO/IEC-27001 y Tabla 46 del ERS.
     */
    public function up(): void
    {
        Schema::create('auditoria_i', function (Blueprint $table) {
            $table->id('id_auditoria');
            $table->timestampTz('fecha_hora');
            $table->string('tipo_movimiento', 40);
            $table->string('id_usuario', 36);
            $table->string('tabla_afectada', 50);
            $table->string('registro_id', 36);
            $table->text('valor_anterior')->nullable();
            $table->text('valor_nuevo')->nullable();
            $table->text('origen'); // JSON {ip, estacion_id, rol_activo, client}
            $table->string('resultado', 10); // EXITOSO, DENEGADO, FALLIDO
            $table->string('motivo', 500)->nullable();
            $table->boolean('es_necesario_auditoria_interna')->default(false);
            $table->char('huella_integridad', 64)->unique();

            // Índices para optimizar consultas de auditoría
            $table->index('fecha_hora');
            $table->index('id_usuario');
            $table->index('tipo_movimiento');
            $table->index('tabla_afectada');
            $table->index('resultado');
        });

        // Triggers de base de datos para inalterabilidad estricta (INSERT-only)
        $driver = DB::getDriverName();

        if ($driver === 'sqlite') {
            DB::unprepared("
                CREATE TRIGGER trg_auditoria_i_no_update
                BEFORE UPDATE ON auditoria_i
                BEGIN
                    SELECT RAISE(ABORT, 'Inalterabilidad: Operación UPDATE prohibida en auditoria_i por NOM-024');
                END;
            ");

            DB::unprepared("
                CREATE TRIGGER trg_auditoria_i_no_delete
                BEFORE DELETE ON auditoria_i
                BEGIN
                    SELECT RAISE(ABORT, 'Inalterabilidad: Operación DELETE prohibida en auditoria_i por NOM-024');
                END;
            ");
        } elseif ($driver === 'pgsql') {
            DB::unprepared("
                CREATE OR REPLACE FUNCTION fn_auditoria_i_bloquear_modificacion()
                RETURNS TRIGGER AS $$
                BEGIN
                    RAISE EXCEPTION 'Inalterabilidad: Operación % prohibida en auditoria_i conforme a NOM-024-SSA3-2012', TG_OP;
                END;
                $$ LANGUAGE plpgsql;

                CREATE TRIGGER trg_auditoria_i_no_modificacion
                BEFORE UPDATE OR DELETE ON auditoria_i
                FOR EACH ROW
                EXECUTE FUNCTION fn_auditoria_i_bloquear_modificacion();
            ");
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        $driver = DB::getDriverName();
        if ($driver === 'pgsql') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_auditoria_i_no_modificacion ON auditoria_i;");
            DB::unprepared("DROP FUNCTION IF EXISTS fn_auditoria_i_bloquear_modificacion();");
        } elseif ($driver === 'sqlite') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_auditoria_i_no_update;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_auditoria_i_no_delete;");
        }

        Schema::dropIfExists('auditoria_i');
    }
};
