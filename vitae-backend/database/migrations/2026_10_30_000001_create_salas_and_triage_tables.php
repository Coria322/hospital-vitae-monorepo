<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Conforme a NOM-206-SSA1-2002, NOM-004-SSA3-2012, DOMAIN_SCHEMA.json (TASK-W-035, TASK-W-036, TASK-W-037)
     */
    public function up(): void
    {
        // 1. Tabla SALA_ATENCION (Salas de emergencia y consultorios de urgencias)
        Schema::create('sala_atencion', function (Blueprint $table) {
            $table->string('id_sala', 10)->primary();
            $table->string('nombre', 50);
            $table->string('tipo', 20); // 'EMERGENCIA' | 'CONSULTORIO'
            $table->string('estado', 20)->default('LIBRE'); // 'LIBRE' | 'OCUPADA'
            $table->string('id_triage_actual', 10)->nullable();
            $table->timestamps();

            $table->index('tipo');
            $table->index('estado');
        });

        // Seed inicial de 3 salas de emergencia (Shock/Reanimación) y consultorios
        DB::table('sala_atencion')->insert([
            ['id_sala' => 'SALA-E01', 'nombre' => 'Shock Traumatología (Cama 1)', 'tipo' => 'EMERGENCIA', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
            ['id_sala' => 'SALA-E02', 'nombre' => 'Shock Cardiovascular (Cama 2)', 'tipo' => 'EMERGENCIA', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
            ['id_sala' => 'SALA-E03', 'nombre' => 'Shock Reanimación (Cama 3)', 'tipo' => 'EMERGENCIA', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
            ['id_sala' => 'CONS-01',  'nombre' => 'Consultorio Filtro 1', 'tipo' => 'CONSULTORIO', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
            ['id_sala' => 'CONS-02',  'nombre' => 'Consultorio Filtro 2', 'tipo' => 'CONSULTORIO', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
            ['id_sala' => 'CONS-03',  'nombre' => 'Consultorio Filtro 3', 'tipo' => 'CONSULTORIO', 'estado' => 'LIBRE', 'created_at' => now(), 'updated_at' => now()],
        ]);

        // 2. Tabla TRIAGE
        Schema::create('triage', function (Blueprint $table) {
            $table->string('id_triage', 10)->primary();
            $table->uuid('id_paciente');
            $table->string('id_usuario', 10); // Enfermería responsable
            $table->string('presion_art', 10); // '120/80'
            $table->integer('frecuencia_card'); // lpm
            $table->integer('frecuencia_resp')->nullable(); // rpm
            $table->decimal('temperatura', 4, 1)->nullable(); // °C
            $table->integer('spo2')->nullable(); // %
            $table->integer('nivel_glucosa')->nullable(); // mg/dL
            $table->smallInteger('escala_eva'); // 0..10
            $table->string('nivel_conciencia', 15); // 'ALERTA', 'VERBAL', 'DOLOR', 'INCONSCIENTE'
            $table->string('color_asig', 20); // 'ROJO', 'NARANJA', 'AMARILLO', 'VERDE', 'AZUL'
            $table->string('color_original', 20)->nullable();
            $table->timestampTz('fecha_hora_ingreso'); // Admisión (UTC)
            $table->string('estimacion_sangre_perdida', 20)->nullable();
            $table->string('folio_ingreso', 20)->unique();
            $table->timestampTz('sla_vencimiento_en'); // Admisión + SLA Manchester
            $table->boolean('sla_vencido')->default(false);
            $table->integer('desviacion_minutos')->nullable();
            $table->string('id_sala', 10)->nullable(); // FK a SALA_ATENCION
            $table->string('estado_atencion', 20)->default('EN_ESPERA'); // 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDO'
            $table->text('motivo_reclasificacion')->nullable();
            $table->boolean('es_necesario_auditoria_interna')->default(false);
            $table->boolean('captura_retroactiva')->default(false);
            $table->timestamps();

            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->foreign('id_sala')->references('id_sala')->on('sala_atencion')->onDelete('set null');

            $table->index(['id_paciente', 'fecha_hora_ingreso']);
            $table->index(['estado_atencion', 'color_asig']);
            $table->index('sla_vencido');
            $table->index('sla_vencimiento_en');
        });

        // 3. Triggers Anti-DELETE nativos por norma NOM-004-SSA3-2012 y NOM-206-SSA1-2002
        $driver = DB::getDriverName();

        if ($driver === 'sqlite') {
            DB::unprepared("
                CREATE TRIGGER trg_triage_no_delete
                BEFORE DELETE ON triage
                BEGIN
                    SELECT RAISE(ABORT, 'NOM-004-SSA3-2012: Prohibida la eliminación física de registros de triage. Se exige conservación histórica inalterable.');
                END;
            ");
        } elseif ($driver === 'pgsql') {
            DB::unprepared("
                CREATE TRIGGER trg_triage_no_delete
                BEFORE DELETE ON triage
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
            DB::unprepared("DROP TRIGGER IF EXISTS trg_triage_no_delete ON triage;");
        } elseif ($driver === 'sqlite') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_triage_no_delete;");
        }

        Schema::dropIfExists('triage');
        Schema::dropIfExists('sala_atencion');
    }
};
