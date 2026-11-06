<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     * Conforme a NOM-004-SSA3-2012, NOM-024-SSA3-2012, COFEPRIS/SAT, HL7 FHIR R4 y LFPDPPP (Sprint 4 / AI-04)
     */
    public function up(): void
    {
        // Limpieza previa en caso de reintentos
        Schema::dropIfExists('acceso_emergencia');
        Schema::dropIfExists('solicitud_arco');
        Schema::dropIfExists('consentimiento');
        Schema::dropIfExists('resultado_estudio');
        Schema::dropIfExists('solicitud_estudio');
        Schema::dropIfExists('receta');
        Schema::dropIfExists('nota_medica');
        Schema::dropIfExists('antecedente_clinico');

        // 1. Tabla ANTECEDENTE_CLINICO (RF-008.1 / NOM-004)
        Schema::create('antecedente_clinico', function (Blueprint $table) {
            $table->uuid('id_antecedente')->primary();
            $table->string('id_expediente', 10);
            $table->string('tipo', 30); // HEREDOFAMILIAR, PATOLOGICO, NO_PATOLOGICO, GINECOOBSTETRICO, ALERGIA
            $table->text('descripcion');
            $table->date('fecha_diagnostico')->nullable();
            $table->text('observaciones')->nullable();
            $table->timestamps();

            $table->foreign('id_expediente')->references('id_expediente')->on('expediente_c')->onDelete('restrict');
            $table->index(['id_expediente', 'tipo']);
        });

        // 2. Tabla NOTA_MEDICA (RF-008.2 / NOM-004 / NOM-024)
        Schema::create('nota_medica', function (Blueprint $table) {
            $table->uuid('id_nota')->primary();
            $table->string('id_expediente', 10);
            $table->string('id_usuario', 10); // Médico autor
            $table->string('tipo', 20); // EVOLUCION, INGRESO, EGRESO, INTERCONSULTA, ACLARATORIA
            $table->uuid('id_nota_original')->nullable(); // Enlace a nota previa si es ACLARATORIA
            $table->text('contenido');
            $table->string('diagnostico_cie', 10)->nullable();
            $table->text('plan_tratamiento')->nullable();
            $table->text('firma_electronica');
            $table->timestampTz('fecha_hora');
            $table->boolean('cerrada')->default(true);
            $table->string('hash_sha256', 64);
            $table->timestamps();

            $table->foreign('id_expediente')->references('id_expediente')->on('expediente_c')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index('id_expediente');
            $table->index('tipo');
            $table->index('cerrada');
        });

        // 3. Tabla RECETA (RF-009 / COFEPRIS / SAT)
        Schema::create('receta', function (Blueprint $table) {
            $table->string('id_receta', 10)->primary();
            $table->string('id_expediente', 10);
            $table->string('id_usuario', 10); // Médico emisor
            $table->string('folio', 20)->unique();
            $table->string('fraccion', 10); // I, II, III, NINGUNA
            $table->text('medicamentos');
            $table->text('indicaciones');
            $table->text('firma_electronica')->nullable();
            $table->timestampTz('fecha_hora_emision');
            $table->string('qr_codigo', 64)->nullable()->unique();
            $table->string('qr_estado', 12)->default('VIGENTE'); // VIGENTE, CONSUMIDO, CANCELADO
            $table->string('hash_sha256', 64);
            $table->timestampTz('surtida_en')->nullable();
            $table->string('surtida_por', 20)->nullable();
            $table->timestamps();

            $table->foreign('id_expediente')->references('id_expediente')->on('expediente_c')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index('id_expediente');
            $table->index('folio');
            $table->index('qr_estado');
            $table->index('fraccion');
        });

        // 4. Tabla SOLICITUD_ESTUDIO (RF-010 / NOM-004)
        Schema::create('solicitud_estudio', function (Blueprint $table) {
            $table->uuid('id_solicitud')->primary();
            $table->string('id_expediente', 10);
            $table->string('id_usuario', 10); // Médico solicitante
            $table->string('tipo_estudio', 60);
            $table->string('prioridad', 10)->default('NORMAL');
            $table->text('indicaciones_clinicas')->nullable();
            $table->string('estado', 15)->default('PENDIENTE'); // PENDIENTE, EN_PROCESO, COMPLETADA, CANCELADA
            $table->timestampTz('fecha_hora_solicitud');
            $table->timestamps();

            $table->foreign('id_expediente')->references('id_expediente')->on('expediente_c')->onDelete('restrict');
            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index('id_expediente');
            $table->index('estado');
        });

        // 5. Tabla RESULTADO_ESTUDIO (RF-010 / RNF-003 / AES-256-GCM)
        Schema::create('resultado_estudio', function (Blueprint $table) {
            $table->uuid('id_resultado')->primary();
            $table->uuid('id_solicitud');
            $table->string('archivo_cifrado_ref', 255);
            $table->string('mime', 50);
            $table->string('cargado_por', 10); // Q.F.B. / Laboratorista
            $table->text('observaciones')->nullable();
            $table->string('hash_sha256', 64);
            $table->timestampTz('fecha_hora');
            $table->timestamps();

            $table->foreign('id_solicitud')->references('id_solicitud')->on('solicitud_estudio')->onDelete('restrict');
            $table->foreign('cargado_por')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index('id_solicitud');
        });

        // 6. Tabla CONSENTIMIENTO (PROV-RN-008 / LFPDPPP / NOM-004)
        Schema::create('consentimiento', function (Blueprint $table) {
            $table->uuid('id_consentimiento')->primary();
            $table->uuid('id_paciente');
            $table->string('tipo', 50);
            $table->string('version_documento', 10)->default('1.0');
            $table->text('finalidades');
            $table->string('hash_documento', 64);
            $table->string('estado', 15)->default('OTORGADO'); // OTORGADO, REVOCADO
            $table->uuid('id_consentimiento_original')->nullable();
            $table->string('medio', 30)->default('DIGITAL_TABLET');
            $table->string('firmante_nombre', 100);
            $table->string('firmante_parentesco', 50)->nullable();
            $table->string('medico_responsable_id', 10);
            $table->string('testigo_1', 100)->nullable();
            $table->string('testigo_2', 100)->nullable();
            $table->timestampTz('fecha_hora');
            $table->timestamps();

            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->foreign('medico_responsable_id')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->index('id_paciente');
            $table->index('estado');
        });

        // 7. Tabla SOLICITUD_ARCO (PROV-RN-009 / LFPDPPP)
        Schema::create('solicitud_arco', function (Blueprint $table) {
            $table->string('folio', 20)->primary();
            $table->uuid('id_paciente');
            $table->string('derecho', 15); // ACCESO, RECTIFICACION, CANCELACION, OPOSICION
            $table->date('fecha_recepcion');
            $table->date('respuesta_limite'); // +20 días hábiles
            $table->date('ejecucion_limite'); // +15 días hábiles
            $table->string('estado', 20)->default('RECIBIDA'); // RECIBIDA, EN_TRAMITE, PROCEDENTE, IMPROCEDENTE, CONCLUIDA
            $table->text('motivo_improcedencia')->nullable();
            $table->boolean('acreditacion_identidad')->default(false);
            $table->string('documento_acreditacion', 255)->nullable();
            $table->text('observaciones')->nullable();
            $table->timestamps();

            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->index('id_paciente');
            $table->index('derecho');
            $table->index('estado');
        });

        // 8. Tabla ACCESO_EMERGENCIA (PROV-RN-010 / Break-the-Glass / NOM-004 / ISO-27001)
        Schema::create('acceso_emergencia', function (Blueprint $table) {
            $table->uuid('id_acceso')->primary();
            $table->string('id_usuario', 10);
            $table->uuid('id_paciente');
            $table->string('motivo', 500);
            $table->timestampTz('expira_en');
            $table->string('estado', 15)->default('ACTIVO'); // ACTIVO, EXPIRADO, REVOCADO
            $table->timestamps();

            $table->foreign('id_usuario')->references('id_usuario')->on('usuario')->onDelete('restrict');
            $table->foreign('id_paciente')->references('id_paciente')->on('paciente')->onDelete('restrict');
            $table->index(['id_usuario', 'id_paciente']);
            $table->index('expira_en');
        });

        // 8b. Llaves foráneas autorreferenciales
        Schema::table('nota_medica', function (Blueprint $table) {
            $table->foreign('id_nota_original')->references('id_nota')->on('nota_medica')->onDelete('restrict');
        });

        Schema::table('consentimiento', function (Blueprint $table) {
            $table->foreign('id_consentimiento_original')->references('id_consentimiento')->on('consentimiento')->onDelete('restrict');
        });

        // 9. Triggers anti-modificación para notas médicas cerradas y consentimientos
        $driver = DB::getDriverName();

        if ($driver === 'sqlite') {
            DB::unprepared("
                CREATE TRIGGER trg_nota_medica_no_update_cerrada
                BEFORE UPDATE ON nota_medica
                FOR EACH ROW
                WHEN OLD.cerrada = 1
                BEGIN
                    SELECT RAISE(ABORT, 'NOM-004-SSA3-2012: Las notas médicas cerradas son inmutables. Use notas aclaratorias.');
                END;
            ");

            DB::unprepared("
                CREATE TRIGGER trg_nota_medica_no_delete
                BEFORE DELETE ON nota_medica
                FOR EACH ROW
                BEGIN
                    SELECT RAISE(ABORT, 'NOM-004-SSA3-2012: Prohibido eliminar notas médicas del expediente clínico.');
                END;
            ");

            DB::unprepared("
                CREATE TRIGGER trg_consentimiento_no_update
                BEFORE UPDATE ON consentimiento
                FOR EACH ROW
                BEGIN
                    SELECT RAISE(ABORT, 'LFPDPPP: El consentimiento informado es inmutable. Registre una revocación.');
                END;
            ");

            DB::unprepared("
                CREATE TRIGGER trg_consentimiento_no_delete
                BEFORE DELETE ON consentimiento
                FOR EACH ROW
                BEGIN
                    SELECT RAISE(ABORT, 'LFPDPPP: Prohibido eliminar registros de consentimiento informado.');
                END;
            ");
        } elseif ($driver === 'pgsql') {
            DB::unprepared("
                CREATE OR REPLACE FUNCTION fn_bloquear_modificacion_nota_cerrada()
                RETURNS TRIGGER AS $$
                BEGIN
                    IF TG_OP = 'UPDATE' AND OLD.cerrada = true THEN
                        RAISE EXCEPTION 'NOM-004-SSA3-2012: Las notas médicas cerradas son inmutables. Use notas aclaratorias.';
                    ELSIF TG_OP = 'DELETE' THEN
                        RAISE EXCEPTION 'NOM-004-SSA3-2012: Prohibido eliminar notas médicas del expediente clínico.';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;

                CREATE TRIGGER trg_nota_medica_inmutable
                BEFORE UPDATE OR DELETE ON nota_medica
                FOR EACH ROW
                EXECUTE FUNCTION fn_bloquear_modificacion_nota_cerrada();

                CREATE OR REPLACE FUNCTION fn_bloquear_modificacion_consentimiento()
                RETURNS TRIGGER AS $$
                BEGIN
                    RAISE EXCEPTION 'LFPDPPP: El consentimiento informado es un registro inmutable. No se permite %.', TG_OP;
                END;
                $$ LANGUAGE plpgsql;

                CREATE TRIGGER trg_consentimiento_inmutable
                BEFORE UPDATE OR DELETE ON consentimiento
                FOR EACH ROW
                EXECUTE FUNCTION fn_bloquear_modificacion_consentimiento();
            ");
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        $driver = DB::getDriverName();
        if ($driver === 'sqlite') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_nota_medica_no_update_cerrada;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_nota_medica_no_delete;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_consentimiento_no_update;");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_consentimiento_no_delete;");
        } elseif ($driver === 'pgsql') {
            DB::unprepared("DROP TRIGGER IF EXISTS trg_nota_medica_inmutable ON nota_medica;");
            DB::unprepared("DROP FUNCTION IF EXISTS fn_bloquear_modificacion_nota_cerrada();");
            DB::unprepared("DROP TRIGGER IF EXISTS trg_consentimiento_inmutable ON consentimiento;");
            DB::unprepared("DROP FUNCTION IF EXISTS fn_bloquear_modificacion_consentimiento();");
        }

        Schema::dropIfExists('acceso_emergencia');
        Schema::dropIfExists('solicitud_arco');
        Schema::dropIfExists('consentimiento');
        Schema::dropIfExists('resultado_estudio');
        Schema::dropIfExists('solicitud_estudio');
        Schema::dropIfExists('receta');
        Schema::dropIfExists('nota_medica');
        Schema::dropIfExists('antecedente_clinico');
    }
};
