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
        $uuid1 = (string) Str::uuid();
        $paciente1 = Paciente::updateOrCreate(
            ['curp' => 'SINT900101HDFR01'],
            [
                'id_paciente' => $uuid1,
                'nombre_completo' => 'Paciente Sintético Alfa',
                'fecha_nacimiento' => '1990-01-01',
                'sexo' => 'MASCULINO',
                'domicilio' => 'Calle Ficticia 100, Sector Pruebas, CDMX',
                'seguro_activo' => true,
                'estado_activo' => true,
                'identificado' => true,
            ]
        );

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
        $uuid2 = (string) Str::uuid();
        $paciente2 = Paciente::updateOrCreate(
            ['curp' => 'SINT750515MDFR02'],
            [
                'id_paciente' => $uuid2,
                'nombre_completo' => 'Paciente Sintética Beta',
                'fecha_nacimiento' => '1975-05-15',
                'sexo' => 'FEMENINO',
                'domicilio' => 'Avenida Imaginaria 200, Sector Pruebas, CDMX',
                'seguro_activo' => true,
                'estado_activo' => true,
                'identificado' => true,
            ]
        );

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
        $uuid3 = (string) Str::uuid();
        Paciente::updateOrCreate(
            ['folio_urgencias' => 'URG-SINT-001'],
            [
                'id_paciente' => $uuid3,
                'curp' => null,
                'nombre_completo' => 'Paciente Desconocido Temporal',
                'fecha_nacimiento' => '2000-01-01',
                'sexo' => 'INDETERMINADO',
                'domicilio' => 'Ubicación de rescate en vía pública',
                'seguro_activo' => false,
                'estado_activo' => true,
                'identificado' => false,
                'folio_urgencias' => 'URG-SINT-001',
            ]
        );
    }
}
