<?php

require __DIR__ . '/vendor/autoload.php';

$app = require_once __DIR__ . '/bootstrap/app.php';
$kernel = $app->make(Illuminate\Contracts\Console\Kernel::class);
$kernel->bootstrap();

// Configurar entorno aislado de pruebas en memoria para reproducibilidad total
config(['database.default' => 'sqlite']);
config(['database.connections.sqlite.database' => ':memory:']);

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
use App\Services\Pdf\PdfPlantillasService;
use Carbon\Carbon;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\File;
use Illuminate\Support\Str;

echo "====================================================================\n";
echo "   HOSPITAL VITAE - PRUEBA INTEGRAL DE GENERACIÓN DE TODOS LOS PDF\n";
echo "====================================================================\n\n";

echo "1. Ejecutando migraciones en base de datos de prueba en memoria...\n";
Artisan::call('migrate', ['--force' => true]);
echo "   Migraciones aplicadas correctamente.\n\n";

$outputDir = __DIR__ . '/storage/app/public/pdfs_generados';
if (!File::exists($outputDir)) {
    File::makeDirectory($outputDir, 0755, true);
}

// Iniciar transacción de base de datos en memoria / rollback para aislamiento limpio
DB::beginTransaction();

try {
    // 1. Usuarios sintéticos de prueba
    $medico = Usuario::updateOrCreate(
        ['id_usuario' => 'MED-TEST-PDF-01'],
        [
            'nombre_completo' => 'Dr. Fernando Zúñiga Rivas',
            'email' => 'dr.zuniga.pdf@hospitalvitae.mx',
            'password_hash' => bcrypt('Password123!'),
            'rol' => 'MEDICO',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-MED-849201',
            'cedula_profesional' => 'CED-MED-849201',
            'especialidad' => 'Cirugía General y Urgencias Quirúrgicas',
        ]
    );

    $enfermera = Usuario::updateOrCreate(
        ['id_usuario' => 'ENF-TEST-PDF-01'],
        [
            'nombre_completo' => 'Enf. Patricia Morales Solís',
            'email' => 'enf.morales.pdf@hospitalvitae.mx',
            'password_hash' => bcrypt('Password123!'),
            'rol' => 'ENFERMERIA',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-ENF-119284',
            'cedula_profesional' => 'CED-ENF-119284',
        ]
    );

    $laboratorista = Usuario::updateOrCreate(
        ['id_usuario' => 'LAB-TEST-PDF-01'],
        [
            'nombre_completo' => 'Q.F.B. Roberto Mendoza Castillo',
            'email' => 'qfb.mendoza.pdf@hospitalvitae.mx',
            'password_hash' => bcrypt('Password123!'),
            'rol' => 'LABORATORISTA',
            'estado_cuenta' => 'ACTIVA',
            'cedula' => 'CED-QFB-558291',
            'cedula_profesional' => 'CED-QFB-558291',
        ]
    );

    // 2. Paciente sintético
    $paciente = Paciente::updateOrCreate(
        ['curp' => 'GOSV950312MDFRRN09'],
        [
            'id_paciente' => (string) Str::uuid(),
            'nombre_completo' => 'Valeria Gómez Salgado',
            'fecha_nacimiento' => '1995-03-12',
            'sexo' => 'F',
            'domicilio' => 'Av. Insurgentes Sur 1200, Col. del Valle, Benito Juárez, CDMX',
            'identificado' => true,
        ]
    );

    // 3. Sala de atención
    $sala = SalaAtencion::updateOrCreate(
        ['id_sala' => 'SALA-URG-01'],
        [
            'nombre' => 'Sala de Choque y Reanimación A',
            'tipo' => 'CHOQUE',
            'ocupada' => true,
        ]
    );

    // 4. Expediente Clínico
    $expediente = ExpedienteClinico::create([
        'id_expediente' => (string) Str::uuid(),
        'id_paciente' => $paciente->id_paciente,
        'id_usuario' => $medico->id_usuario,
        'motivo_consulta' => 'Cefalea pulsátil intensa holocraneana acompañada de dolor torácico opresivo y diaforesis.',
        'diagnostico' => 'I10 - Hipertensión Esencial / G44.2 - Cefalea Tensional / R07.2 - Dolor Precordial',
        'fecha_creacion' => Carbon::now('UTC')->subHours(6),
        'fecha_ultimo_acto_medico' => Carbon::now('UTC'),
        'estado_activo' => true,
    ]);

    // Antecedentes
    AntecedenteClinico::create([
        'id_antecedente' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'tipo' => 'ALERGIA',
        'descripcion' => 'Alergia severa a Penicilinas y Betalactámicos (Reacción anafiláctica previa).',
        'fecha_diagnostico' => '2018-04-15',
    ]);
    AntecedenteClinico::create([
        'id_antecedente' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'tipo' => 'PATOLOGICO',
        'descripcion' => 'Hipertensión arterial primaria diagnosticada hace 4 años con control irregular.',
        'fecha_diagnostico' => '2022-09-10',
    ]);

    // Notas Médicas
    NotaMedica::create([
        'id_nota' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'id_usuario' => $medico->id_usuario,
        'tipo' => 'URGENCIA',
        'contenido' => 'Paciente ingresa con TA 160/100 mmHg. Consciente y orientada. Se administra terapia antihipertensiva sublingual y analgésica parenteral.',
        'firma_digital' => 'FIRMA-MED-ZUNIGA-HASH8921',
        'firma_electronica' => 'EFIRMA-SHA256-AUTHENTICATED',
        'hash_sha256' => hash('sha256', 'NOTA-URG-01-EXPEDIENTE'),
        'fecha_hora' => Carbon::now('UTC')->subHours(4),
        'cerrada' => true,
    ]);

    $pdfService = app(PdfPlantillasService::class);
    $reportes = [];

    // -------------------------------------------------------------
    // PRUEBA 1: RECETA MÉDICA INSTITUCIONAL (ESTÁNDAR)
    // -------------------------------------------------------------
    $recetaNormal = Receta::create([
        'id_receta' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'id_usuario' => $medico->id_usuario,
        'folio' => 'REC-2026-N001',
        'fraccion' => 'NINGUNA',
        'medicamentos' => "1. Paracetamol 500 mg tabletas\n2. Captopril 25 mg tabletas orales\n3. Ketorolaco 10 mg tabletas sublinguales",
        'indicaciones' => 'Tomar Captopril 1 tab cada 12 hrs. Paracetamol 1 tab c/8 hrs condicionado a dolor. Dieta hiposódica.',
        'fecha_hora_emision' => Carbon::now('UTC'),
        'qr_codigo' => 'https://vitae.hospital/verificar-receta?folio=REC-2026-N001',
        'qr_estado' => 'VIGENTE',
        'hash_sha256' => hash('sha256', 'REC-2026-N001-DATOS-INTEGROS'),
    ]);

    $builderReceta = $pdfService->generarRecetaPdf($recetaNormal);
    $binReceta = $builderReceta->build();
    $pathReceta = $outputDir . '/01_Receta_Medica_Institucional.pdf';
    file_put_contents($pathReceta, $binReceta);
    $reportes[] = [
        'doc' => 'Receta Médica Institucional (NOM-004 / COFEPRIS)',
        'folio' => $recetaNormal->folio,
        'archivo' => basename($pathReceta),
        'bytes' => strlen($binReceta),
        'sha256' => hash('sha256', $binReceta),
        'valido' => str_starts_with($binReceta, '%PDF-1.4') && str_ends_with($binReceta, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 2: RECETA MÉDICA DE CONTROLADOS (FRACCIÓN II - ART. 65 RIS)
    // -------------------------------------------------------------
    $recetaControlada = Receta::create([
        'id_receta' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'id_usuario' => $medico->id_usuario,
        'folio' => 'REC-2026-CTRL-88',
        'fraccion' => 'II',
        'medicamentos' => 'Morfina 10 mg/mL solución inyectable (Frasco ámpula con 1 mL) - 3 piezas.',
        'indicaciones' => 'Administración hospitalaria estricta por médico adscrito cada 6 hrs en infusión controlada.',
        'fecha_hora_emision' => Carbon::now('UTC'),
        'qr_codigo' => 'https://vitae.hospital/verificar-receta?folio=REC-2026-CTRL-88&token=VERIF_SEC',
        'qr_estado' => 'VIGENTE',
        'firma_electronica' => 'EFIRMA-MED-COFEPRIS-VALIDADA-TOKEN-7718',
        'hash_sha256' => hash('sha256', 'REC-2026-CTRL-88-MURF'),
    ]);

    $builderCtrl = $pdfService->generarRecetaPdf($recetaControlada);
    $binCtrl = $builderCtrl->build();
    $pathCtrl = $outputDir . '/02_Receta_Medicamento_Controlado_FracII.pdf';
    file_put_contents($pathCtrl, $binCtrl);
    $reportes[] = [
        'doc' => 'Receta Controlada Fracción II (Art. 65 RIS)',
        'folio' => $recetaControlada->folio,
        'archivo' => basename($pathCtrl),
        'bytes' => strlen($binCtrl),
        'sha256' => hash('sha256', $binCtrl),
        'valido' => str_starts_with($binCtrl, '%PDF-1.4') && str_ends_with($binCtrl, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 3: HOJA DE CONSENTIMIENTO INFORMADO
    // -------------------------------------------------------------
    $consentimiento = Consentimiento::create([
        'id_consentimiento' => (string) Str::uuid(),
        'id_paciente' => $paciente->id_paciente,
        'tipo' => 'PROCEDIMIENTO_QUIRURGICO_MAYOR',
        'version_documento' => '2.1',
        'finalidades' => 'Autorización libre, informada y voluntaria para cateterismo diagnóstico y angioplastia coronaria de urgencia bajo anestesia general y sedación monitorizada.',
        'hash_documento' => hash('sha256', 'CONSENTIMIENTO-LEGAL-NOM004-7729'),
        'estado' => 'VIGENTE',
        'medio' => 'DIGITAL_TABLET',
        'firmante_nombre' => 'Valeria Gómez Salgado',
        'firmante_parentesco' => 'PROPIO PACIENTE',
        'medico_responsable_id' => $medico->id_usuario,
        'testigo_1' => 'Enf. Patricia Morales Solís',
        'testigo_2' => 'Lic. Carlos Vega Trejo',
        'fecha_hora' => Carbon::now('UTC'),
    ]);

    $builderCons = $pdfService->generarConsentimientoPdf($consentimiento);
    $binCons = $builderCons->build();
    $pathCons = $outputDir . '/03_Consentimiento_Informado.pdf';
    file_put_contents($pathCons, $binCons);
    $reportes[] = [
        'doc' => 'Consentimiento Informado (NOM-004-SSA3-2012 / LFPDPPP)',
        'folio' => $consentimiento->id_consentimiento,
        'archivo' => basename($pathCons),
        'bytes' => strlen($binCons),
        'sha256' => hash('sha256', $binCons),
        'valido' => str_starts_with($binCons, '%PDF-1.4') && str_ends_with($binCons, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 4: HOJA DE ENFERMERÍA Y SOMATOMETRÍA (TRIAGE)
    // -------------------------------------------------------------
    $triage = Triage::create([
        'id_triage' => (string) Str::uuid(),
        'id_paciente' => $paciente->id_paciente,
        'id_usuario' => $enfermera->id_usuario,
        'presion_art' => '160/100',
        'frecuencia_card' => 96,
        'frecuencia_resp' => 22,
        'temperatura' => 37.1,
        'spo2' => 95,
        'nivel_glucosa' => 118,
        'escala_eva' => 8,
        'nivel_conciencia' => 'ALERTA',
        'color_asig' => 'AMARILLO',
        'color_original' => 'AMARILLO',
        'fecha_hora_ingreso' => Carbon::now('UTC')->subMinutes(45),
        'folio_ingreso' => 'TRI-2026-URG-1092',
        'sla_vencimiento_en' => Carbon::now('UTC')->addMinutes(15),
        'id_sala' => $sala->id_sala,
        'estado_atencion' => 'ATENDIENDO',
        'observaciones' => 'Paciente inquieta, diaforética con facies de dolor. Se canaliza vía periférica en MSI.',
        'captura_retroactiva' => false,
    ]);

    $builderTriage = $pdfService->generarTriageEnfermeriaPdf($triage);
    $binTriage = $builderTriage->build();
    $pathTriage = $outputDir . '/04_Hoja_Enfermeria_Triage.pdf';
    file_put_contents($pathTriage, $binTriage);
    $reportes[] = [
        'doc' => 'Hoja Oficial de Enfermería / Triage (NOM-004 num 9)',
        'folio' => $triage->folio_ingreso,
        'archivo' => basename($pathTriage),
        'bytes' => strlen($binTriage),
        'sha256' => hash('sha256', $binTriage),
        'valido' => str_starts_with($binTriage, '%PDF-1.4') && str_ends_with($binTriage, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 5: VOLANTE Y ORDEN DE ESTUDIOS DE LABORATORIO
    // -------------------------------------------------------------
    $solicitud = SolicitudEstudio::create([
        'id_solicitud' => (string) Str::uuid(),
        'id_expediente' => $expediente->id_expediente,
        'id_usuario' => $medico->id_usuario,
        'tipo_estudio' => 'Biometría Hemática + Química Sanguínea 6 + Troponina I Cuantitativa',
        'prioridad' => 'URGENTE',
        'indicaciones_clinicas' => 'Descartar síndrome coronario agudo y disfunción renal aguda.',
        'estado' => 'COMPLETADA',
        'fecha_hora_solicitud' => Carbon::now('UTC')->subHours(2),
    ]);

    $builderSol = $pdfService->generarSolicitudEstudioPdf($solicitud);
    $binSol = $builderSol->build();
    $pathSol = $outputDir . '/05_Volante_Orden_Laboratorio.pdf';
    file_put_contents($pathSol, $binSol);
    $reportes[] = [
        'doc' => 'Volante y Orden de Estudios Clínicos (NOM-007-SSA3-2011)',
        'folio' => $solicitud->id_solicitud,
        'archivo' => basename($pathSol),
        'bytes' => strlen($binSol),
        'sha256' => hash('sha256', $binSol),
        'valido' => str_starts_with($binSol, '%PDF-1.4') && str_ends_with($binSol, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 6: REPORTE DE RESULTADOS DE LABORATORIO VALIDADOS
    // -------------------------------------------------------------
    $resultado = ResultadoEstudio::create([
        'id_resultado' => (string) Str::uuid(),
        'id_solicitud' => $solicitud->id_solicitud,
        'archivo_cifrado_ref' => 'laboratorio/2026/11/res-789.enc',
        'mime' => 'application/pdf',
        'cargado_por' => $laboratorista->id_usuario,
        'observaciones' => "Glucosa: 110 mg/dL (Normal: 70-100)\nUrea: 32 mg/dL (Normal: 15-45)\nCreatinina: 0.85 mg/dL (Normal: 0.6-1.1)\nTroponina I: 0.01 ng/mL (Negativo <0.04)\nLeucocitos: 7,800 /uL (Normal: 4,500-11,000)\nHemoglobina: 14.1 g/dL (Normal: 12.0-16.0)\nPlaquetas: 245,000 /uL (Normal: 150,000-450,000)\nConclusión: Sin evidencia bioquímica de infarto miocárdico agudo.",
        'hash_sha256' => hash('sha256', 'RESULTADO-LAB-OFICIAL-VALIDADO-MOCK'),
        'fecha_hora' => Carbon::now('UTC')->subHour(),
    ]);

    $builderRes = $pdfService->generarResultadoEstudioPdf($resultado);
    $binRes = $builderRes->build();
    $pathRes = $outputDir . '/06_Reporte_Resultados_Laboratorio.pdf';
    file_put_contents($pathRes, $binRes);
    $reportes[] = [
        'doc' => 'Reporte Oficial de Resultados Validados (NOM-007-SSA3-2011)',
        'folio' => $resultado->id_resultado,
        'archivo' => basename($pathRes),
        'bytes' => strlen($binRes),
        'sha256' => hash('sha256', $binRes),
        'valido' => str_starts_with($binRes, '%PDF-1.4') && str_ends_with($binRes, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 7: EXPEDIENTE CLÍNICO INTEGRAL AUDITADO (ASISTENCIAL)
    // -------------------------------------------------------------
    $builderExp = $pdfService->generarExpedienteCompletoPdf(
        expediente: $expediente,
        solicitante: $medico,
        ipOrigen: '192.168.1.100',
        enmascararPii: false
    );
    $binExp = $builderExp->build();
    $pathExp = $outputDir . '/07_Expediente_Clinico_Integral_Asistencial.pdf';
    file_put_contents($pathExp, $binExp);
    $reportes[] = [
        'doc' => 'Expediente Clínico Integral Completo (NOM-004 nums 5.6 y 5.7)',
        'folio' => $expediente->id_expediente,
        'archivo' => basename($pathExp),
        'bytes' => strlen($binExp),
        'sha256' => hash('sha256', $binExp),
        'valido' => str_starts_with($binExp, '%PDF-1.4') && str_ends_with($binExp, "%%EOF\n"),
    ];

    // -------------------------------------------------------------
    // PRUEBA 8: EXPEDIENTE CLÍNICO AUDITADO CON PII ENMASCARADA (LFPDPPP)
    // -------------------------------------------------------------
    $builderExpMask = $pdfService->generarExpedienteCompletoPdf(
        expediente: $expediente,
        solicitante: $medico,
        ipOrigen: '192.168.1.100',
        enmascararPii: true
    );
    $binExpMask = $builderExpMask->build();
    $pathExpMask = $outputDir . '/08_Expediente_Clinico_PII_Enmascarada.pdf';
    file_put_contents($pathExpMask, $binExpMask);
    $reportes[] = [
        'doc' => 'Expediente Clínico Auditado PII Enmascarada (LFPDPPP)',
        'folio' => $expediente->id_expediente,
        'archivo' => basename($pathExpMask),
        'bytes' => strlen($binExpMask),
        'sha256' => hash('sha256', $binExpMask),
        'valido' => str_starts_with($binExpMask, '%PDF-1.4') && str_ends_with($binExpMask, "%%EOF\n"),
    ];

    // Imprimir tabla resumen
    echo sprintf("%-4s | %-48s | %-10s | %-12s | %-6s\n", "#", "Documento Clínico", "Tamaño", "Archivo", "Válido");
    echo str_repeat("-", 95) . "\n";
    $idx = 1;
    $todoValido = true;
    foreach ($reportes as $r) {
        $estado = $r['valido'] ? "OK" : "FALLO";
        if (!$r['valido']) $todoValido = false;
        echo sprintf(
            "%-4d | %-48s | %-10s | %-20s | %-6s\n",
            $idx++,
            mb_strimwidth($r['doc'], 0, 48, "..."),
            number_format($r['bytes']) . " B",
            $r['archivo'],
            $estado
        );
    }
    echo str_repeat("-", 95) . "\n";
    echo "Directorio de guardado: " . realpath($outputDir) . "\n\n";

    if ($todoValido) {
        echo "RESULTADO: Todos los PDFs generados exitosamente cumplen al 100% las especificaciones PDF-1.4.\n";
    } else {
        echo "ERROR: Uno o más PDFs presentaron anomalías en la estructura binaria.\n";
    }

} finally {
    DB::rollBack();
}
