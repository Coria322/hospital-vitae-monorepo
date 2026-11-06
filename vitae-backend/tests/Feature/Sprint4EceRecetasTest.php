<?php

namespace Tests\Feature;

use App\Models\AuditoriaI;
use App\Models\Consentimiento;
use App\Models\ExpedienteClinico;
use App\Models\NotaMedica;
use App\Models\Paciente;
use App\Models\Receta;
use App\Models\SolicitudArco;
use App\Models\SolicitudEstudio;
use App\Models\Triage;
use App\Models\Usuario;
use Carbon\Carbon;
use Exception;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use RuntimeException;
use Tests\TestCase;

class Sprint4EceRecetasTest extends TestCase
{
    use RefreshDatabase;

    protected Usuario $medico;
    protected Usuario $medicoSecundario;
    protected Usuario $laboratorista;
    protected Usuario $admin;
    protected Usuario $enfermera;
    protected Paciente $paciente;

    protected function setUp(): void
    {
        parent::setUp();

        $this->medico = Usuario::updateOrCreate(
            ['id_usuario' => 'MED-001'],
            [
                'nombre_completo' => 'Dr. Carlos Rivera Urgenciólogo',
                'rol' => 'MEDICO',
                'cedula' => '1234567890',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->medicoSecundario = Usuario::updateOrCreate(
            ['id_usuario' => 'MED-002'],
            [
                'nombre_completo' => 'Dra. Elena Vasconcelos',
                'rol' => 'MEDICO',
                'cedula' => '9876543210',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->laboratorista = Usuario::updateOrCreate(
            ['id_usuario' => 'LAB-001'],
            [
                'nombre_completo' => 'Q.F.B. Mariana Téllez',
                'rol' => 'LABORATORISTA',
                'cedula' => 'LAB-449102',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->admin = Usuario::updateOrCreate(
            ['id_usuario' => 'ADM-001'],
            [
                'nombre_completo' => 'Ing. Roberto Silva Administrador',
                'rol' => 'ADMINISTRADOR',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->enfermera = Usuario::updateOrCreate(
            ['id_usuario' => 'ENF-001'],
            [
                'nombre_completo' => 'Lic. Enf. Carlos Ramos',
                'rol' => 'ENFERMERIA',
                'cedula' => '8921102',
                'password_hash' => Hash::make('password123', ['rounds' => 12]),
                'estado_cuenta' => 'ACTIVA',
            ]
        );

        $this->paciente = Paciente::create([
            'curp' => 'MORA800101HDFRRN01',
            'nombre_completo' => 'Juan Carlos Morales Ramírez',
            'fecha_nacimiento' => '1980-01-01',
            'sexo' => 'MASCULINO',
            'domicilio' => 'Av. Insurgentes Sur 1234, Ciudad de México',
            'seguro_activo' => true,
            'estado_activo' => true,
            'identificado' => true,
        ]);
    }

    /**
     * RF-008.1: Apertura de expediente clínico NOM-004 y rechazo de duplicados (1:1).
     */
    public function test_creacion_expediente_con_campos_nom004_y_bloqueo_duplicados(): void
    {
        $response = $this->actingAs($this->medico)
            ->postJson('/api/v1/expedientes-clinicos', [
                'id_paciente' => $this->paciente->id_paciente,
                'motivo_consulta' => 'Cefalea intensa refractaria y visión borrosa súbita.',
                'diagnostico' => 'Crisis hipertensiva en estudio',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.id_paciente', $this->paciente->id_paciente);

        $this->assertDatabaseHas('expediente_c', [
            'id_paciente' => $this->paciente->id_paciente,
            'estado_activo' => true,
        ]);

        // Intento de duplicar expediente para el mismo paciente debe responder 409 Conflict
        $responseDuplicado = $this->actingAs($this->medico)
            ->postJson('/api/v1/expedientes-clinicos', [
                'id_paciente' => $this->paciente->id_paciente,
                'motivo_consulta' => 'Segunda consulta en urgencias',
            ]);

        $responseDuplicado->assertStatus(409)
            ->assertJsonPath('code', 'EXPEDIENTE_DUPLICADO');
    }

    /**
     * RF-008.1: Registro de antecedentes clínicos estructurados.
     */
    public function test_registro_antecedentes_clinicos_asociados_al_expediente(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST01',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Dolor abdominal agudo',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $response = $this->actingAs($this->medico)
            ->postJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/antecedentes", [
                'tipo' => 'PATOLOGICO',
                'descripcion' => 'Diabetes Mellitus Tipo 2 diagnosticada hace 8 años en tratamiento con metformina.',
                'fecha_diagnostico' => '2018-05-10',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.tipo', 'PATOLOGICO');

        $this->assertDatabaseHas('antecedente_clinico', [
            'id_expediente' => $expediente->id_expediente,
            'tipo' => 'PATOLOGICO',
        ]);
    }

    /**
     * RF-008.2: Inmutabilidad de nota médica cerrada (rechazo a nivel de API, Eloquent y trigger).
     */
    public function test_inmutabilidad_nota_medica_cerrada_y_rechazo_update_delete(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST02',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Evaluación post-operatoria',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $resNota = $this->actingAs($this->medico)
            ->postJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/notas", [
                'tipo' => 'EVOLUCION',
                'contenido' => 'Paciente masculino hemodinámicamente estable, afebril, tolerando vía oral.',
                'diagnostico_cie' => 'K35.8',
            ]);

        $resNota->assertStatus(201)
            ->assertJsonPath('data.cerrada', true);

        $idNota = $resNota->json('data.id_nota');

        // Verbos PUT, PATCH, DELETE sobre notas responden 405 Method Not Allowed
        $this->actingAs($this->medico)
            ->putJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/notas/{$idNota}", [
                'contenido' => 'Intento de edición alterando diagnóstico original.',
            ])
            ->assertStatus(405);

        $this->actingAs($this->medico)
            ->deleteJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/notas/{$idNota}")
            ->assertStatus(405);

        // Intento directo vía Eloquent debe disparar RuntimeException
        $notaModel = NotaMedica::find($idNota);
        $this->expectException(RuntimeException::class);
        $notaModel->update(['contenido' => 'Edición directa bloqueada por Eloquent']);
    }

    /**
     * RF-008.2: Creación de nota aclaratoria vinculada a nota original.
     */
    public function test_creacion_nota_aclaratoria_vinculada_a_nota_original(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST03',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Consulta de control',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $notaOriginal = NotaMedica::create([
            'id_expediente' => $expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'tipo' => 'EVOLUCION',
            'contenido' => 'Se indicó dosis de paracetamol 500mg cada 8 horas.',
            'firma_electronica' => 'FIRMA-MED-001',
            'fecha_hora' => now('UTC'),
            'cerrada' => true,
            'hash_sha256' => hash('sha256', 'original'),
        ]);

        $response = $this->actingAs($this->medico)
            ->postJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/notas/{$notaOriginal->id_nota}/aclaraciones", [
                'contenido' => 'NOTA ACLARATORIA: Se rectifica dosis a 1g cada 8 horas por peso corporal de 95kg.',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('data.tipo', 'ACLARATORIA')
            ->assertJsonPath('data.id_nota_original', $notaOriginal->id_nota);

        // La nota original debe permanecer intacta
        $notaFresca = NotaMedica::find($notaOriginal->id_nota);
        $this->assertEquals('Se indicó dosis de paracetamol 500mg cada 8 horas.', $notaFresca->contenido);
    }

    /**
     * RF-009: Emisión de receta controlada con e.firma y activación de código QR único.
     */
    public function test_emision_receta_controlada_con_validacion_efirma_y_generacion_qr(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST04',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Manejo de dolor crónico oncológico',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        // Emisión de receta Fracción I (Estupefacientes)
        $resReceta = $this->actingAs($this->medico)
            ->postJson('/api/v1/recetas', [
                'id_expediente' => $expediente->id_expediente,
                'fraccion' => 'I',
                'medicamentos' => 'Morfina solución inyectable 10mg/ml, 5 ampolletas.',
                'indicaciones' => 'Aplicar 1 ampolleta IV cada 8 horas en caso de dolor severo EVA > 7.',
            ]);

        $resReceta->assertStatus(201)
            ->assertJsonPath('data.fraccion', 'I');

        $idReceta = $resReceta->json('data.id_receta');

        // Intento de firmar con e.firma revocada ante SAT debe responder 422
        $resFirmaInvalida = $this->actingAs($this->medico)
            ->postJson("/api/v1/recetas/{$idReceta}/firma", [
                'firma_electronica' => 'CERTIFICADO_REVOCADO_SAT_MOCK_ERROR',
            ]);

        $resFirmaInvalida->assertStatus(422)
            ->assertJsonPath('code', 'FIRMA_INVALIDA');

        // Firma válida con e.firma activa
        $resFirmaValida = $this->actingAs($this->medico)
            ->postJson("/api/v1/recetas/{$idReceta}/firma", [
                'firma_electronica' => 'MIICXAIBAAKCAQEA081293819283VALIDO_EFIRMA_SAT_HOSPITAL_VITAE',
            ]);

        $resFirmaValida->assertStatus(200)
            ->assertJsonPath('data.qr_estado', 'VIGENTE')
            ->assertJsonStructure(['data' => ['qr_codigo']]);
    }

    /**
     * RF-009: Surtido de receta médica y bloqueo estricto de reuso de código QR (un solo uso).
     */
    public function test_surtido_receta_y_bloqueo_estricto_de_reuso_qr(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST05',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Tratamiento postquirúrgico',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $receta = Receta::create([
            'id_receta' => 'RC00998877',
            'id_expediente' => $expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'folio' => 'REC-20261103-SURT',
            'fraccion' => 'II',
            'medicamentos' => 'Clonazepam 2mg tabletas',
            'indicaciones' => 'Tomar media tableta por la noche',
            'firma_electronica' => 'FIRMA_VALIDA',
            'fecha_hora_emision' => now('UTC'),
            'qr_codigo' => 'QR-TOKEN-UNICO-SURTIDO-1234',
            'qr_estado' => 'VIGENTE',
            'hash_sha256' => hash('sha256', 'receta-surtido'),
        ]);

        // Primer surtido exitoso en farmacia
        $resSurtido1 = $this->actingAs($this->admin)
            ->postJson("/api/v1/recetas/{$receta->folio}/surtir");

        $resSurtido1->assertStatus(200)
            ->assertJsonPath('data.qr_estado', 'CONSUMIDO');

        // Segundo intento de surtido con el mismo QR debe ser bloqueado con 409 Conflict
        $resSurtido2 = $this->actingAs($this->admin)
            ->postJson("/api/v1/recetas/{$receta->folio}/surtir");

        $resSurtido2->assertStatus(409)
            ->assertJsonPath('code', 'REUSO_QR_BLOQUEADO');

        // Verificar que se insertó el evento de seguridad en AUDITORIA_I
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'INCIDENTE_SEGURIDAD_QR_REUSADO',
            'es_necesario_auditoria_interna' => true,
        ]);
    }

    /**
     * RF-010: Solicitud de estudio y carga de resultado cifrado con AES-256-GCM.
     */
    public function test_solicitud_estudios_y_carga_resultado_cifrado_aes256gcm(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-TEST06',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Estudio de síndrome anémico',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        // Solicitud por el médico
        $resSolicitud = $this->actingAs($this->medico)
            ->postJson('/api/v1/solicitudes-estudio', [
                'id_expediente' => $expediente->id_expediente,
                'tipo_estudio' => 'BIOMETRIA_HEMATICA_COMPLETA',
                'prioridad' => 'URGENTE',
            ]);

        $resSolicitud->assertStatus(201)
            ->assertJsonPath('data.estado', 'PENDIENTE');

        $idSolicitud = $resSolicitud->json('data.id_solicitud');

        // Laboratorista carga resultado cifrado
        $resCarga = $this->actingAs($this->laboratorista)
            ->postJson("/api/v1/solicitudes-estudio/{$idSolicitud}/resultados", [
                'observaciones' => 'Hemoglobina 8.2 g/dL, Hematocrito 25%. Anemia microcítica hipocrómica.',
                'contenido_texto' => 'REPORTE LABORATORIO VINTAGE: LEUCOCITOS: 6,500/uL, PLAQUETAS: 250,000/uL.',
            ]);

        $resCarga->assertStatus(201)
            ->assertJsonPath('status', 'success');

        $this->assertDatabaseHas('solicitud_estudio', [
            'id_solicitud' => $idSolicitud,
            'estado' => 'COMPLETADA',
        ]);

        // Consulta y descifrado seguro
        $resConsulta = $this->actingAs($this->medico)
            ->getJson("/api/v1/solicitudes-estudio/{$idSolicitud}/resultados");

        $resConsulta->assertStatus(200)
            ->assertJsonPath('data.resultado.cargado_por', $this->laboratorista->id_usuario)
            ->assertJsonPath('data.contenido_descifrado', 'REPORTE LABORATORIO VINTAGE: LEUCOCITOS: 6,500/uL, PLAQUETAS: 250,000/uL.');
    }

    /**
     * RNF-007: Interoperabilidad semántica estándar HL7 FHIR R4.
     */
    public function test_interoperabilidad_fhir_r4_patient_encounter_observation_medicationrequest(): void
    {
        // 1. Recurso FHIR Patient
        $resPatient = $this->getJson("/api/v1/fhir/Patient/{$this->paciente->id_paciente}");
        $resPatient->assertStatus(200)
            ->assertJsonPath('resourceType', 'Patient')
            ->assertJsonPath('id', $this->paciente->id_paciente)
            ->assertJsonPath('gender', 'male');

        // 2. Recurso FHIR Encounter y Observation basados en Triage
        $triage = Triage::create([
            'id_triage' => 'TRG-FHIR01',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->enfermera->id_usuario,
            'folio_ingreso' => 'FOL-FHIR-01',
            'presion_art' => '120/80',
            'frecuencia_card' => 75,
            'frecuencia_resp' => 18,
            'temperatura' => 36.5,
            'spo2' => 98,
            'escala_eva' => 2,
            'nivel_conciencia' => 'ALERTA',
            'color_asig' => 'VERDE',
            'color_original' => 'VERDE',
            'fecha_hora_ingreso' => now('UTC'),
            'sla_vencimiento_en' => now('UTC')->addMinutes(120),
        ]);

        $resEncounter = $this->getJson("/api/v1/fhir/Encounter/{$triage->id_triage}");
        $resEncounter->assertStatus(200)
            ->assertJsonPath('resourceType', 'Encounter')
            ->assertJsonPath('class.code', 'EMER');

        $resObs = $this->getJson("/api/v1/fhir/Observation/{$triage->id_triage}");
        $resObs->assertStatus(200)
            ->assertJsonPath('resourceType', 'Observation')
            ->assertJsonPath('code.coding.0.code', '85354-9');

        // 3. Recurso FHIR MedicationRequest basado en Receta
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-FHIR01',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Prueba de interoperabilidad',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $receta = Receta::create([
            'id_receta' => 'RC-FHIR-01',
            'id_expediente' => $expediente->id_expediente,
            'id_usuario' => $this->medico->id_usuario,
            'folio' => 'REC-FHIR-202611',
            'fraccion' => 'NINGUNA',
            'medicamentos' => 'Amoxicilina 500mg cápsulas',
            'indicaciones' => '1 cápsula cada 8 horas por 7 días',
            'fecha_hora_emision' => now('UTC'),
            'qr_codigo' => 'QR-FHIR-123',
            'qr_estado' => 'CONSUMIDO',
            'hash_sha256' => hash('sha256', 'fhir'),
        ]);

        $resMedReq = $this->getJson("/api/v1/fhir/MedicationRequest/{$receta->id_receta}");
        $resMedReq->assertStatus(200)
            ->assertJsonPath('resourceType', 'MedicationRequest')
            ->assertJsonPath('status', 'completed');
    }

    /**
     * PROV-RN-008: Consentimiento informado inmutable y revocación vinculada.
     */
    public function test_consentimiento_informado_inmutable_y_revocacion(): void
    {
        // 1. Registro de consentimiento
        $resCons = $this->actingAs($this->medico)
            ->postJson('/api/v1/consentimientos', [
                'id_paciente' => $this->paciente->id_paciente,
                'tipo' => 'PROCEDIMIENTO_QUIRURGICO_MAYOR',
                'finalidades' => 'Autorización de apendicectomía laparoscópica y anestesia general.',
                'firmante_nombre' => $this->paciente->nombre_completo,
                'medico_responsable_id' => $this->medico->id_usuario,
            ]);

        $resCons->assertStatus(201)
            ->assertJsonPath('data.estado', 'OTORGADO');

        $idConsentimiento = $resCons->json('data.id_consentimiento');

        // Intento directo de UPDATE o DELETE en base de datos debe ser rechazado
        $consModel = Consentimiento::find($idConsentimiento);
        try {
            $consModel->update(['finalidades' => 'Modificación ilegal']);
            $this->fail('Se esperaba RuntimeException por inmutabilidad de consentimiento.');
        } catch (RuntimeException $e) {
            $this->assertStringContainsString('inmutable', $e->getMessage());
        }

        // 2. Revocación legal mediante nuevo registro enlazado
        $resRevoc = $this->actingAs($this->medico)
            ->postJson("/api/v1/consentimientos/{$idConsentimiento}/revocaciones", [
                'motivo_revocacion' => 'El paciente decide solicitar una segunda opinión médica.',
                'firmante_nombre' => $this->paciente->nombre_completo,
            ]);

        $resRevoc->assertStatus(201)
            ->assertJsonPath('data.estado', 'REVOCADO')
            ->assertJsonPath('data.id_consentimiento_original', $idConsentimiento);

        // El original permanece con estado OTORGADO
        $originalFresco = Consentimiento::find($idConsentimiento);
        $this->assertEquals('OTORGADO', $originalFresco->estado);
    }

    /**
     * PROV-RN-008: Excepción de urgencia sin consentimiento exige al menos 2 médicos autorizados.
     */
    public function test_excepcion_urgencia_consentimiento_requiere_dos_medicos_autorizados(): void
    {
        // Intento con un solo médico repetido debe fallar con 422
        $resInvalido = $this->actingAs($this->medico)
            ->postJson('/api/v1/consentimientos/urgencia-excepcion', [
                'id_paciente' => $this->paciente->id_paciente,
                'procedimiento' => 'Laparotomía exploradora urgente',
                'justificacion_urgencia' => 'Paciente inconsciente con choque hipovolémico por trauma.',
                'medico_1_id' => $this->medico->id_usuario,
                'medico_2_id' => $this->medico->id_usuario, // Mismo médico
            ]);

        $resInvalido->assertStatus(422)
            ->assertJsonPath('code', 'EXCEPCION_URGENCIA_INVALIDA');

        // Con 2 médicos autorizados distintos
        $resValido = $this->actingAs($this->medico)
            ->postJson('/api/v1/consentimientos/urgencia-excepcion', [
                'id_paciente' => $this->paciente->id_paciente,
                'procedimiento' => 'Laparotomía exploradora urgente',
                'justificacion_urgencia' => 'Paciente en paro cardiorrespiratorio inminente.',
                'medico_1_id' => $this->medico->id_usuario,
                'medico_2_id' => $this->medicoSecundario->id_usuario,
            ]);

        $resValido->assertStatus(201)
            ->assertJsonPath('data.tipo', 'EXCEPCION_URGENCIA');
    }

    /**
     * PROV-RN-009: Derechos ARCO (cómputo de días hábiles e improcedencia por retención de 5 años).
     */
    public function test_solicitud_arco_calculo_plazos_e_improcedencia_retencion_5_anos(): void
    {
        // Crear expediente clínico reciente dentro del período legal de 5 años
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-ARCO01',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Consulta general',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        // Radicar solicitud de cancelación
        $resArco = $this->actingAs($this->admin)
            ->postJson('/api/v1/solicitudes-arco', [
                'id_paciente' => $this->paciente->id_paciente,
                'derecho' => 'CANCELACION',
                'acreditacion_identidad' => true,
                'documento_acreditacion' => 'INE-COPIA-CERTIFICADA-PDF',
                'observaciones' => 'Solicito la supresión de mis expedientes.',
            ]);

        $resArco->assertStatus(201)
            ->assertJsonPath('data.derecho', 'CANCELACION')
            ->assertJsonPath('data.estado', 'RECIBIDA');

        $folio = $resArco->json('data.folio');

        // Resolver: debe resolverse como IMPROCEDENTE por NOM-004 de 5 años
        $resResolver = $this->actingAs($this->admin)
            ->postJson("/api/v1/solicitudes-arco/{$folio}/resolver");

        $resResolver->assertStatus(200)
            ->assertJsonPath('data.estado', 'IMPROCEDENTE')
            ->assertJsonPath('data.motivo_improcedencia', function ($motivo) {
                return str_contains($motivo, 'NOM-004-SSA3-2012') && str_contains($motivo, '5 años');
            });
    }

    /**
     * PROV-RN-010: Break-the-Glass (acceso de emergencia y auditoría de seguridad reforzada).
     */
    public function test_protocolo_break_the_glass_acceso_emergencia_y_auditoria_reforzada(): void
    {
        // Expediente perteneciente al Dr. Rivera (MED-001)
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-BTG001',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Traumatismo craneoencefálico',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        // Dra. Elena (MED-002) NO tiene relación asistencial previa -> debe recibir 403 Forbidden
        $resBloqueo = $this->actingAs($this->medicoSecundario)
            ->getJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}");

        $resBloqueo->assertStatus(403)
            ->assertJsonPath('code', 'BREAK_THE_GLASS_REQUIRED');

        // Dra. Elena activa protocolo Break-the-Glass con justificación clínica
        $resBtg = $this->actingAs($this->medicoSecundario)
            ->postJson('/api/v1/accesos-emergencia', [
                'id_paciente' => $this->paciente->id_paciente,
                'motivo' => 'Urgencia vital: Paciente con deterioro neurológico súbito y paro cardiorrespiratorio en piso.',
            ]);

        $resBtg->assertStatus(201)
            ->assertJsonPath('status', 'success');

        // Ahora Dra. Elena sí puede consultar el expediente clínico
        $resAccesoConcedido = $this->actingAs($this->medicoSecundario)
            ->getJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}");

        $resAccesoConcedido->assertStatus(200)
            ->assertJsonPath('data.id_expediente', $expediente->id_expediente);

        // Verificar que la auditoría registró la alerta interna crítica
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'ACCESO_BREAK_THE_GLASS',
            'es_necesario_auditoria_interna' => true,
        ]);
    }

    /**
     * PROV-SEC-008: Exportación segura de expediente con marcas de agua dinámicas y auditoría.
     */
    public function test_exportacion_expediente_con_marca_de_agua_y_registro_auditoria(): void
    {
        $expediente = ExpedienteClinico::create([
            'id_expediente' => 'EXP-EXP001',
            'id_paciente' => $this->paciente->id_paciente,
            'id_usuario' => $this->medico->id_usuario,
            'motivo_consulta' => 'Exportación de resumen clínico legal',
            'fecha_creacion' => now('UTC'),
            'fecha_ultimo_acto_medico' => now('UTC'),
            'estado_activo' => true,
        ]);

        $response = $this->actingAs($this->medico)
            ->postJson("/api/v1/expedientes-clinicos/{$expediente->id_expediente}/exportaciones", [
                'enmascarar_pii' => true,
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('status', 'success')
            ->assertJsonStructure([
                'data' => [
                    'encabezado' => ['marca_agua_seguridad', 'generado_en_utc'],
                    'expediente',
                    'paciente',
                    'hash_integridad_exportacion',
                ],
            ]);

        // Verificar que el evento de exportación quedó en AUDITORIA_I
        $this->assertDatabaseHas('auditoria_i', [
            'tipo_movimiento' => 'EXPORTACION_DOCUMENTO',
            'registro_id' => $expediente->id_expediente,
        ]);
    }
}
