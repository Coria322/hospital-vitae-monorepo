<?php

namespace Tests\Feature;

use App\Models\AntecedenteClinico;
use App\Models\Consentimiento;
use App\Models\ExpedienteClinico;
use App\Models\NotaMedica;
use App\Models\Paciente;
use App\Models\Receta;
use App\Models\ResultadoEstudio;
use App\Models\SalaAtencion;
use App\Models\SolicitudEstudio;
use App\Models\Triage;
use App\Models\Usuario;
use Carbon\Carbon;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

class PlantillasImpresionPdfTest extends TestCase
{
    use RefreshDatabase;

    protected Usuario $medico;
    protected Usuario $enfermera;
    protected Usuario $laboratorista;
    protected Paciente $paciente;
    protected ExpedienteClinico $expediente;

    protected function setUp(): void
    {
        parent::setUp();

        // 1. Usuarios con estado_cuenta ACTIVA
        $this->medico = Usuario::create([
            'id_usuario' => 'MED-PDF-001',
            'nombre_completo' => 'Dr. Fernando Zúñiga',
            'email' => 'dr.zuniga@hospitalvitae.mx',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'rol' => 'MEDICO',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-MED-849201',
            'cedula_profesional' => 'CED-MED-849201',
            'especialidad' => 'Cirugía y Urgencias',
        ]);

        $this->enfermera = Usuario::create([
            'id_usuario' => 'ENF-PDF-001',
            'nombre_completo' => 'Enf. Patricia Morales',
            'email' => 'enf.morales@hospitalvitae.mx',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'rol' => 'ENFERMERIA',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-ENF-119284',
            'cedula_profesional' => 'CED-ENF-119284',
        ]);

        $this->laboratorista = Usuario::create([
            'id_usuario' => 'LAB-PDF-001',
            'nombre_completo' => 'Q.F.B. Roberto Mendoza',
            'email' => 'qfb.mendoza@hospitalvitae.mx',
            'password_hash' => Hash::make('password123', ['rounds' => 12]),
            'rol' => 'LABORATORISTA',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-QFB-558291',
            'cedula_profesional' => 'CED-QFB-558291',
        ]);

        // 2. Paciente sintético
        $this->paciente = Paciente::create([
            'id_paciente' => (string) Str::uuid(),
            'nombre_completo' => 'Valeria Gómez Salgado',
            'curp' => 'GOSV950312MDFRRN09',
            'fecha_nacimiento' => '1995-03-12',
            'sexo' => 'F',
            'domicilio' => 'Av. Insurgentes Sur 1200, CDMX',
            'identificado' => true,
        ]);

        // 3. Expediente clínico
        $this->expediente = ExpedienteClinico::create([
            'id_expediente' => (string) Str::uuid(),
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Cefalea intensa y dolor torácico opresivo',
            'diagnostico' => 'G44.2 - Cefalea tensional / Crisis hipertensiva',
            'fecha_creacion' => Carbon::now('UTC'),
            'fecha_ultimo_acto_medico' => Carbon::now('UTC'),
            'estado_activo' => true,
        ]);
    }

    public function test_descarga_receta_medica_pdf_exitosa(): void
    {
        $receta = Receta::create([
            'id_receta' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'folio' => 'REC-2026-9901',
            'fraccion' => 'NINGUNA',
            'medicamentos' => "Paracetamol 500mg tabletas\nKetorolaco 10mg sublingual",
            'indicaciones' => 'Tomar 1 tableta cada 8 horas con alimentos por 5 días.',
            'fecha_hora_emision' => Carbon::now('UTC'),
            'qr_codigo' => 'https://vitae.hospital/verificar-receta?folio=REC-2026-9901',
            'qr_estado' => 'VIGENTE',
            'hash_sha256' => hash('sha256', 'REC-2026-9901-DATA'),
        ]);

        $response = $this->actingAs($this->medico)
            ->get("/api/v1/recetas/{$receta->id_receta}/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString('attachment; filename="Receta-REC-2026-9901.pdf"', $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringEndsWith("%%EOF\n", $content);
        $this->assertStringContainsString('COMPLEJO HOSPITALARIO VITAE', $content);
        $this->assertStringContainsString('RECETA', $content);
        $this->assertStringContainsString('REC-2026-9901', $content);
    }

    public function test_bloqueo_receta_controlada_sin_firma_en_descarga_pdf(): void
    {
        $recetaControlada = Receta::create([
            'id_receta' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'folio' => 'REC-CTRL-001',
            'fraccion' => 'II',
            'medicamentos' => 'Morfina 10mg solución inyectable',
            'indicaciones' => 'Uso hospitalario exclusivo',
            'fecha_hora_emision' => Carbon::now('UTC'),
            'qr_codigo' => 'PENDIENTE',
            'qr_estado' => 'PENDIENTE_FIRMA',
            'firma_electronica' => null, // Sin firma
            'hash_sha256' => hash('sha256', 'REC-CTRL-001-PENDIENTE'),
        ]);

        $response = $this->actingAs($this->medico)
            ->get("/api/v1/recetas/{$recetaControlada->id_receta}/pdf");

        $response->assertStatus(409);
        $response->assertJson([
            'status' => 'error',
            'code' => 'RECETA_CONTROLADA_SIN_FIRMA',
        ]);
    }

    public function test_descarga_consentimiento_informado_pdf_exitoso(): void
    {
        $consentimiento = Consentimiento::create([
            'id_consentimiento' => (string) Str::uuid(),
            'id_paciente' => $this->paciente->id_paciente,
            'tipo' => 'PROCEDIMIENTO_QUIRURGICO_MAYOR',
            'version_documento' => '1.0',
            'finalidades' => 'Autorización para intervención quirúrgica de urgencia y anestesia general.',
            'hash_documento' => hash('sha256', 'CONSENTIMIENTO-TEST-DATA'),
            'estado' => 'VIGENTE',
            'medio' => 'DIGITAL_TABLET',
            'firmante_nombre' => 'Valeria Gómez Salgado',
            'firmante_parentesco' => 'PROPIO PACIENTE',
            'medico_responsable_id' => $this->medico->id_usuario,
            'testigo_1' => 'Enf. Patricia Morales',
            'testigo_2' => 'Lic. Carlos Vega',
            'fecha_hora' => Carbon::now('UTC'),
        ]);

        $response = $this->actingAs($this->medico)
            ->get("/api/v1/consentimientos/{$consentimiento->id_consentimiento}/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString("Consentimiento-{$consentimiento->id_consentimiento}.pdf", $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringContainsString('HOJA OFICIAL DE CONSENTIMIENTO INFORMADO', $content);
        $this->assertStringContainsString('NOM-004-SSA3-2012', $content);
        $this->assertStringContainsString('Valeria', $content);
    }

    public function test_descarga_hoja_enfermeria_triage_pdf_exitosa(): void
    {
        $sala = SalaAtencion::create([
            'id_sala' => 'SALA-TRI-01',
            'nombre' => 'Sala de Choque y Reanimación',
            'tipo' => 'CHOQUE',
            'ocupada' => true,
        ]);

        $triage = Triage::create([
            'id_triage' => (string) Str::uuid(),
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->enfermera->id_usuario,
            'presion_art' => '135/85',
            'frecuencia_card' => 88,
            'frecuencia_resp' => 20,
            'temperatura' => 36.8,
            'spo2' => 97,
            'nivel_glucosa' => 105,
            'escala_eva' => 6,
            'nivel_conciencia' => 'ALERTA',
            'color_asig' => 'AMARILLO',
            'color_original' => 'AMARILLO',
            'fecha_hora_ingreso' => Carbon::now('UTC'),
            'folio_ingreso' => 'TRI-2026-0045',
            'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(60),
            'id_sala' => $sala->id_sala,
            'estado_atencion' => 'ESPERA',
            'captura_retroactiva' => false,
        ]);

        $response = $this->actingAs($this->enfermera)
            ->get("/api/v1/triages/{$triage->id_triage}/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString("Hoja-Enfermeria-Triage-TRI-2026-0045.pdf", $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringContainsString('HOJA OFICIAL DE ENFERMER', $content);
        $this->assertStringContainsString('TRI-2026-0045', $content);
        $this->assertStringContainsString('AMARILLO', $content);
    }

    public function test_descarga_volante_solicitud_laboratorio_pdf_exitoso(): void
    {
        $solicitud = SolicitudEstudio::create([
            'id_solicitud' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'tipo_estudio' => 'Química Sanguínea de 6 Elementos',
            'prioridad' => 'URGENTE',
            'indicaciones_clinicas' => 'Sospecha de descompensación metabólica aguda.',
            'estado' => 'PENDIENTE',
            'fecha_hora_solicitud' => Carbon::now('UTC'),
        ]);

        $response = $this->actingAs($this->medico)
            ->get("/api/v1/solicitudes-estudio/{$solicitud->id_solicitud}/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString("Volante-Laboratorio-{$solicitud->id_solicitud}.pdf", $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringContainsString('VOLANTE Y ORDEN DE ESTUDIOS DE LABORATORIO', $content);
        $this->assertStringContainsString('URGENTE', $content);
        $this->assertStringContainsString('FLEBOTOM', $content);
    }

    public function test_descarga_reporte_resultado_laboratorio_pdf_exitoso(): void
    {
        $solicitud = SolicitudEstudio::create([
            'id_solicitud' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'tipo_estudio' => 'Biometría Hemática Completa',
            'prioridad' => 'NORMAL',
            'indicaciones_clinicas' => 'Control postoperatorio',
            'estado' => 'COMPLETADA',
            'fecha_hora_solicitud' => Carbon::now('UTC'),
        ]);

        $resultado = ResultadoEstudio::create([
            'id_resultado' => (string) Str::uuid(),
            'id_solicitud' => $solicitud->id_solicitud,
            'archivo_cifrado_ref' => 'laboratorio/mock.enc',
            'mime' => 'application/pdf',
            'cargado_por' => $this->laboratorista->id_usuario,
            'observaciones' => 'Serie blanca y roja sin alteraciones morfológicas.',
            'hash_sha256' => hash('sha256', 'RESULTADO-LAB-MOCK'),
            'fecha_hora' => Carbon::now('UTC'),
        ]);

        $response = $this->actingAs($this->laboratorista)
            ->get("/api/v1/solicitudes-estudio/{$solicitud->id_solicitud}/resultados/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString("Resultado-Laboratorio-{$resultado->id_resultado}.pdf", $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringContainsString('REPORTE OFICIAL DE RESULTADOS DE LABORATORIO', $content);
        $this->assertStringContainsString('NOM-007-SSA3-2011', $content);
        $this->assertStringContainsString('Q.F.B.', $content);
    }

    public function test_descarga_expediente_clinico_completo_pdf_auditado(): void
    {
        // Registrar antecedente
        AntecedenteClinico::create([
            'id_antecedente' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'tipo' => 'ALERGIA',
            'descripcion' => 'Alergia severa a Penicilina y derivados beta-lactámicos',
            'fecha_diagnostico' => '2015-05-10',
        ]);

        // Registrar nota médica
        NotaMedica::create([
            'id_nota' => (string) Str::uuid(),
            'id_expediente' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'tipo' => 'EVOLUCION',
            'contenido' => 'Paciente femenina con adecuada respuesta a terapia analgésica y antihipertensiva.',
            'firma_digital' => 'FIRMA-MED-001',
            'firma_electronica' => 'EFIRMA-MED-001-VAL',
            'hash_sha256' => hash('sha256', 'NOTA-EVOLUCION-01'),
            'fecha_hora' => Carbon::now('UTC'),
            'cerrada' => true,
        ]);

        $response = $this->actingAs($this->medico)
            ->get("/api/v1/expedientes-clinicos/{$this->expediente->id_expediente}/pdf");

        $response->assertStatus(200);
        $response->assertHeader('Content-Type', 'application/pdf');
        $this->assertStringContainsString("Expediente-{$this->expediente->id_expediente}.pdf", $response->headers->get('Content-Disposition'));

        $content = $response->getContent();
        $this->assertStringStartsWith('%PDF-1.4', $content);
        $this->assertStringContainsString('EXPEDIENTE CL', $content);
        $this->assertStringContainsString('COPIA FIDEDIGNA DEL EXPEDIENTE', $content);

        // Verificar registro estricto en AUDITORIA_I
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'EXPORTACION_DOCUMENTO_PDF',
            'tabla_afectada' => 'expediente_c',
            'registro_id' => $this->expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'resultado' => 'EXITOSO',
        ]);
    }
}
