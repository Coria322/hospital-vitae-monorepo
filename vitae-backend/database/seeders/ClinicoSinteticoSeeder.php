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
    }
}
