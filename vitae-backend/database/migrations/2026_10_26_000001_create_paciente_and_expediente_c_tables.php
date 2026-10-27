<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Conforme a NOM-004-SSA3-2012, NOM-024-SSA3-2012 y DOMAIN_SCHEMA.json (TASK-W-022)
     */
    public function up(): void
    {
        // 1. Tabla PACIENTE
        Schema::create('paciente', function (Blueprint $table) {
            $table->uuid('id_paciente')->primary();
            $table->string('curp', 18)->nullable()->unique();
            $table->string('nombre_completo', 100);
            $table->date('fecha_nacimiento');
            $table->string('sexo', 15);
            $table->string('domicilio', 200);
            $table->boolean('seguro_activo')->default(true);
            $table->boolean('estado_activo')->default(true); // Soft delete / Retención legal
            $table->boolean('identificado')->default(true); // false para paciente desconocido/inconsciente
            $table->string('folio_urgencias', 20)->nullable();
            $table->string('representante_legal', 100)->nullable();
            $table->timestamps();

            $table->index('estado_activo');
            $table->index('curp');
        });

        // 2. Tabla EXPEDIENTE_C
        Schema::create('expediente_c', function (Blueprint $table) {
            $table->string('id_expediente', 10)->primary();
            $table->uuid('id_paciente')->unique();
            $table->string('id_usuario', 10); // Médico tratante que apertura
            $table->text('motivo_consulta');
            $table->text('diagnostico')->nullable();
            $table->text('estudios_previos')->nullable();
            $table->timestampTz('fecha_creacion');
            $table->timestampTz('fecha_ultimo_acto_medico'); // Base para retención legal de 5 años
            $table->boolean('estado_activo')->default(true); // Soft delete
            $table->boolean('bloqueo_legal')->default(false); // Congelamiento para peritaje médico-legal
            $table->timestamps();

            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');

            $table->index('fecha_ultimo_acto_medico');
            $table->index('estado_activo');
            $table->index('bloqueo_legal');
        });

        // 3. Triggers Anti-DELETE nativos por norma NOM-004-SSA3-2012
        $driver = DB::getDriverName();

        if ($driver === 'sqlite') {
            DB::unprepared("
                CREATE TRIGGER trg_expediente_c_no_delete
                BEFORE DELETE ON expediente_c
                BEGIN
                    SELECT RAISE(ABORT, 'NOM-004-SSA3-2012: Prohibida la eliminación física de expedientes clínicos. Se exige baja lógica.');
                END;
            ");

            DB::unprepared("
                CREATE TRIGGER trg_paciente_no_delete
                BEFORE DELETE ON paciente
                BEGIN
                    SELECT RAISE(ABORT, 'NOM-004-SSA3-2012: Prohibida la eliminación física de registros de pacientes. Se exige baja lógica.');
                END;
            ");
        } elseif ($driver === 'pgsql') {
            DB::unprepared("
                CREATE OR REPLACE FUNCTION fn_bloquear_delete_clinico()
                RETURNS TRIGGER AS $$
                BEGIN
                    RAISE EXCEPTION 'NOM-004-SSA3-2012: Operación % prohibida en registros clínicos. Se exige baja lógica y retención legal mínima de 5 años.', TG_OP;
                END;
                $$ LANGUAGE plpgsql;

                CREATE TRIGGER trg_expediente_c_no_delete
                BEFORE DELETE ON expediente_c
                FOR EACH ROW
                EXECUTE FUNCTION fn_bloquear_delete_clinico();

                CREATE TRIGGER trg_paciente_no_delete
                BEFORE DELETE ON paciente
                FOR EACH ROW
                EXECUTE FUNCTION fn_bloquear_delete_clinico();
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
            DB::unprepared("DROP TRIGGER IF EXISTS trg_expediente_c_no_delete ON expediente_c;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_paciente_no_delete ON paciente;");
            DB::unprepared("DROP FUNCTION IF EXISTS fn_bloquear_delete_clinico();");
        } elseif ($driver === 'sqlite') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_expediente_c_no_delete;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_paciente_no_delete;");
        }

        Schema::dropIfExists('expediente_c');
        Schema::dropIfExists('paciente');
    }
};
