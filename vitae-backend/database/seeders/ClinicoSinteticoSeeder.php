<?php

namespace Database\Seeders;

use App\Models\ExpedienteClinico;
use App\Models\Paciente;
use App\Models\Usuario;
use Carbon\Carbon;
use Illuminate\Database\Seeder;
use Illuminate\Support\Str;

class ClinicoSinteticoSeeder extends Seeder
{
    /**
     * Run the database seeds.
     * Conforme a PROV-SEC-010 / LFPDPPP: Datos 100% sintéticos sin PII real.
     */
    public function run(): void
    {
        $medico = Usuario::where('rol', 'MEDICO')->first();
        $idMedico = $medico ? $medico->id_usuario : 'MED-001';

        // 1. Paciente sintético estándar con expediente activo
        $paciente1 = Paciente::where('curp', 'SINT900101HDFR01')->first();
        if (!$paciente1) {
            $paciente1 = Paciente::create([
                'id_paciente' => (string) Str::uuid(),
                'curp' => 'SINT900101HDFR01',
                'nombre_completo' => 'Paciente Sintético Alfa',
                'fecha_nacimiento' => '1990-01-01',
                'sexo' => 'MASCULINO',
                'domicilio' => 'Calle Ficticia 100, Sector Pruebas, CDMX',
                'seguro_activo' => true,
                'estado_activo' => true,
                'identificado' => true,
            ]);
        }

        ExpedienteClinico::updateOrCreate(
            ['id_expediente' => 'EXP-SINT01'],
            [
                'id_paciente' => $paciente1->id_paciente,
                'id_usuario' => $idMedico,
                'motivo_consulta' => 'Consulta de control general sintética',
                'diagnostico' => 'Saludable para pruebas',
                'estudios_previos' => 'Biometría hemática sintética en parámetros normales',
                'fecha_creacion' => Carbon::now('UTC')->subMonths(6),
                'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subMonths(6),
                'estado_activo' => true,
                'bloqueo_legal' => false,
            ]
        );

        // 2. Paciente sintético con retención cumplida (> 5 años)
        $paciente2 = Paciente::where('curp', 'SINT750515MDFR02')->first();
        if (!$paciente2) {
            $paciente2 = Paciente::create([
                'id_paciente' => (string) Str::uuid(),
                'curp' => 'SINT750515MDFR02',
                'nombre_completo' => 'Paciente Sintética Beta',
                'fecha_nacimiento' => '1975-05-15',
                'sexo' => 'FEMENINO',
                'domicilio' => 'Avenida Imaginaria 200, Sector Pruebas, CDMX',
                'seguro_activo' => true,
                'estado_activo' => true,
                'identificado' => true,
            ]);
        }

        ExpedienteClinico::updateOrCreate(
            ['id_expediente' => 'EXP-SINT02'],
            [
                'id_paciente' => $paciente2->id_paciente,
                'id_usuario' => $idMedico,
                'motivo_consulta' => 'Atención médica histórica (2018)',
                'diagnostico' => 'Rinofaringitis aguda remitida',
                'fecha_creacion' => Carbon::now('UTC')->subDays(2100),
                'fecha_ultimo_acto_medico' => Carbon::now('UTC')->subDays(2100), // > 1825 días
                'estado_activo' => true,
                'bloqueo_legal' => false,
            ]
        );

        // 3. Paciente no identificado (urgencias / PROV-RN-007)
        $paciente3 = Paciente::where('folio_urgencias', 'URG-NN-SINT01')
            ->orWhere('folio_urgencias', 'URG-SINT-001')
            ->first();

        if (!$paciente3) {
            $paciente3 = Paciente::create([
                'id_paciente' => (string) Str::uuid(),
                'curp' => null,
                'nombre_completo' => 'Paciente Desconocido Temporal',
                'fecha_nacimiento' => '2000-01-01',
                'sexo' => 'INDETERMINADO',
                'domicilio' => 'Ubicación de rescate en vía pública',
                'seguro_activo' => false,
                'estado_activo' => true,
                'identificado' => false,
                'folio_urgencias' => 'URG-NN-SINT01',
            ]);
        } else {
            $paciente3->update(['folio_urgencias' => 'URG-NN-SINT01']);
        }

        // 4. Triage sintético con folio homologado
        $enfermera = Usuario::where('rol', 'ENFERMERIA')->first();
        $idEnfermera = $enfermera ? $enfermera->id_usuario : 'ENF-001';

        \App\Models\Triage::firstOrCreate(
            ['folio_ingreso' => 'FOL-TRI-SINT01'],
            [
                'id_triage' => 'TRG-SINT01',
                'id_paciente' => $paciente1->id_paciente,
                'id_usuario' => $idEnfermera,
                'presion_art' => '135/85',
                'frecuencia_card' => 88,
                'frecuencia_resp' => 18,
                'temperatura' => 37.1,
                'spo2' => 96,
                'nivel_glucosa' => 110,
                'escala_eva' => 6,
                'nivel_conciencia' => 'ALERTA',
                'color_asig' => 'AMARILLO',
                'color_original' => 'AMARILLO',
                'fecha_hora_ingreso' => Carbon::now('UTC')->subMinutes(25),
                'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(35),
                'sla_vencido' => false,
                'estado_atencion' => 'EN_ESPERA',
            ]
        );

        // 5. Vincular cuentas de usuarios PACIENTE con registros clínicos
        Usuario::where('id_usuario', 'PAC-001')->update(['id_paciente' => $paciente1->id_paciente]);
        Usuario::where('id_usuario', 'PAC-002')->update(['id_paciente' => $paciente2->id_paciente]);

        // 6. Citas médicas sintéticas (PROV-RF-002 / Sprint 5)
        \App\Models\Cita::updateOrCreate(
            ['id_cita' => 'a0000000-0000-0000-0000-000000000001'],
            [
                'id_paciente' => $paciente1->id_paciente,
                'id_usuario' => $idMedico,
                'fecha_hora' => Carbon::now('UTC')->addDays(3)->setTime(10, 0, 0),
                'estado' => 'CONFIRMADA',
                'especialidad' => 'Medicina Interna',
                'motivo' => 'Seguimiento de control clínico sintético',
                'consultorio' => 'Consultorio 104',
                'confirmada_por' => 'REC-001',
                'confirmada_en' => Carbon::now('UTC')->subHours(2),
            ]
        );

        \App\Models\Cita::updateOrCreate(
            ['id_cita' => 'a0000000-0000-0000-0000-000000000002'],
            [
                'id_paciente' => $paciente1->id_paciente,
                'id_usuario' => $idMedico,
                'fecha_hora' => Carbon::now('UTC')->addDays(7)->setTime(16, 30, 0),
                'estado' => 'PENDIENTE',
                'especialidad' => 'Cardiología Preventiva',
                'motivo' => 'Revisión periódica de presión arterial',
                'consultorio' => 'Consultorio 202',
            ]
        );

        // 7. Solicitudes de Laboratorio Pendientes y en Proceso (NOM-007 / RF-010)
        \App\Models\SolicitudEstudio::updateOrCreate(
            ['id_solicitud' => '01a1c000-0000-7000-8000-000000000001'],
            [
                'id_expediente' => 'EXP-SINT01',
                'id_usuario' => $idMedico,
                'tipo_estudio' => 'Biometría Hemática Completa + Perfil Bioquímico',
                'prioridad' => 'URGENTE',
                'indicaciones_clinicas' => 'Consulta Externa Consultorio 104 - Evaluación metabólica general',
                'estado' => 'PENDIENTE',
                'fecha_hora_solicitud' => Carbon::now('UTC')->subMinutes(30),
            ]
        );

        $expMorales = ExpedienteClinico::where('id_expediente', 'EXP-9KA8P3')->first();
        if ($expMorales) {
            \App\Models\SolicitudEstudio::updateOrCreate(
                ['id_solicitud' => '01a1c000-0000-7000-8000-000000000002'],
                [
                    'id_expediente' => 'EXP-9KA8P3',
                    'id_usuario' => $idMedico,
                    'tipo_estudio' => 'Gasometría Arterial + Perfil Coronario Enzimático (hs-cTnI)',
                    'prioridad' => 'STAT',
                    'indicaciones_clinicas' => 'Urgencias Box 01 (Choque) - Protocolo de Dolor Torácico',
                    'estado' => 'EN_PROCESO',
                    'fecha_hora_solicitud' => Carbon::now('UTC')->subMinutes(15),
                ]
            );
        }
    }
}
