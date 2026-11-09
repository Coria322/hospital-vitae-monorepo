# Matriz de Cumplimiento — Hospital Vitae SIH
**Sprint Activo:** Sprint Final (Auditoría AI-06 — Quality Gate, Despliegue Inmutable Blue-Green, BCP/DRP y Cierre Definitivo)  
**Fecha de Actualización:** 2026-11-08  
**Rama:** `feature/sprint-6-quality-gate-release`

---

## 1. Resumen Ejecutivo de Cumplimiento por Sprint

### Sprint 2: Infraestructura, Base de Datos y Trazabilidad Inalterable (Auditoría AI-02)
> *Normativas: NOM-024-SSA3-2012, NOM-004-SSA3-2012, ISO/IEC-27001, LFPDPPP, ISO/IEC-25010.*  
> *Estado del Sprint: ✅ **CERRADO (100% Cumplido - Certificado en `dev` y `main`)**.*

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Actual | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RNF-001** (NOM-024) | Bitácora inalterable `AUDITORIA_I` de 13 columnas con encadenamiento SHA-256 en UTC. | Tabla `auditorias` básica sin encadenamiento ni campos normativos. | Migración `auditoria_i` (13 columnas), SHA-256 encadenado y triggers nativos anti-modificación. | ✅ **Cerrada (100%)** |
| **PROV-SEC-005** (ISO-27001) | Garantía INSERT-only; bloqueo de UPDATE, DELETE y TRUNCATE; sanitización de credenciales. | La tabla previa admitía DELETE y UPDATE. No había sanitización de secretos. | Reglas de inalterabilidad nativas, servicio `AuditLogger` con enmascaramiento automático de credenciales. | ✅ **Cerrada (100%)** |
| **RF-001 (Auditoría)** | Consulta de auditoría de solo lectura para ADMINISTRADOR y evento `CONSULTA_AUDITORIA`. | Controlador `AuditoriaController` básico sin auto-auditoría ni restricción de métodos HTTP 405. | `GET /api/v1/auditoria` paginado y filtrable; verbos POST/PUT/DELETE retornan HTTP 405; auto-registro del evento. | ✅ **Cerrada (100%)** |
| **PROV-SEC-005 (Job)** | Verificación criptográfica continua de la integridad de la cadena SHA-256. | Inexistente. | Comando `php artisan auditoria:verificar-cadena` con detección de eslabones rotos. | ✅ **Cerrada (100%)** |
| **RNF-002** (NOM-004) | Retención documental de 5 años (1825 días) y prohibición de DELETE físico en registros médicos. | Inexistente en tablas clínicas. | Tablas `paciente` y `expediente_c` con UUIDs, borrado lógico y triggers anti-DELETE físicos. | ✅ **Cerrada (100%)** |
| **PROV-RN-003** (NOM-004) | Tracking de `fecha_ultimo_acto_medico` y reporte de expedientes con plazo concluido. | Inexistente. | Campo y método `registrarActoMedico()`, más comando `expedientes:reporte-retencion`. | ✅ **Cerrada (100%)** |
| **PROV-SEC-010** (LFPDPPP)| Datos de prueba sintéticos sin PII real en entornos de desarrollo y pruebas. | Inexistente. | Seeder `ClinicoSinteticoSeeder` con CURPs y pacientes sintéticos para desarrollo y testing. | ✅ **Cerrada (100%)** |
| **PROV-RNF-001** (ISO-27001) | Infraestructura Docker endurecida: versión fija, prohibición de `:latest`, usuario no root (`www-data`), hardening Apache/PHP y healthcheck `/up`. | Dockerfile con imagen base genérica `:latest` y ejecución default como root. | Imagen fija `php:8.2.26-apache-bookworm`, `composer:2.8.2`, `USER www-data`, puerto no privilegiado 8080/10000 y healthcheck `/up`. | ✅ **Cerrada (100%)** |
| **PROV-SEC-007** (CENAM/NTP) | Sincronización horaria estricta con fuentes CENAM/NTP (deriva <= 1 s) y almacenamiento forzado en UTC. | Marcas de tiempo sin validación de offset ni enforcement de UTC en conexión PostgreSQL. | `NtpService` con medición SNTP UDP 123 y fallback HTTP, comando `ntp:verificar-sincronizacion`, configuración UTC y `timezone => UTC` en PostgreSQL. | ✅ **Cerrada (100%)** |
| **RNF-003** (ISO-27001) | Seguridad de la información en red y reposo: TLS >= 1.2 forzado hacia BD (`sslmode=require`), HSTS >= 1 año y cifrado AES-256-GCM para adjuntos. | Conexión BD con sslmode permisivo (`prefer`), sin HSTS/CSP ni utilidad para cifrado de adjuntos clínicos. | `sslmode=require` en PostgreSQL, `SecurityHeadersMiddleware` con HSTS (31.5M s), CSP y servicio `CifradoAdjuntosService` con AES-256-GCM. | ✅ **Cerrada (100%)** |
| **PROV-SEC-006** (ISO-27001) | Estrategia de respaldos 3-2-1 cifrados AES-256, retención 30 d / 12 m y monitor de lag WAL <= 600 s. | Inexistencia de comando de dump cifrado, sin copias redundantes ni alertas de lag de replicación. | `BackupService`, comandos `db:backup-seguro` y `db:verificar-lag-wal`, cifrado AES-256, copias offsite y alerta crítica si lag > 600s. | ✅ **Cerrada (100%)** |
| **RNF-004** (ISO-25010) | Alta disponibilidad y aislamiento de fallos: orquestación con réplicas >= 2, reinicio automático y health check activo `/salud` con RTO <= 300 s. | Servicio en instancia única sin endpoint de salud diagnóstico de subsistemas. | `docker-compose.yml` con 2 réplicas, límites de recursos, reinicio `unless-stopped` y endpoint `GET /api/v1/salud` verificando BD y Storage. | ✅ **Cerrada (100%)** |

---

### Sprint 3: Triage Manchester, Urgencias y Admisión (Auditoría AI-03)
> *Normativas: NOM-004-SSA3-2012, NOM-206-SSA1-2002, ISO/IEC-25010, WCAG 2.1 AA, LFPDPPP.*  
> *Estado del Sprint: ✅ **CERRADO (100% Cumplido - Mergeado en `dev` PR #2)**.*

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Actual | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RF-004** (NOM-004) | Captura obligatoria de signos vitales (presión arterial sistólica/diastólica, FC, FR, temp, EVA 0-10, AVDI). | Inexistente en base de datos y API. | Implementado en `TriageController`, validaciones fisiológicas y modelo `Triage`. | ✅ **Cerrada (100%)** |
| **RF-005** (NOM-206) | Asignación automática de nivel de gravedad (Algoritmo Manchester de 5 niveles: Rojo a Azul). | Inexistente. | Implementado en `TriageManchesterService` con regla crítica de paciente inconsciente. | ✅ **Cerrada (100%)** |
| **RF-006** (NOM-206) | Gestión y distribución dinámica de colas de espera según prioridad clínica y tiempo transcurrido (SLA ponderado). | Inexistente. | Implementado en `ColaUrgenciasService`, 3 salas de emergencia y liberación de consultorios. | ✅ **Cerrada (100%)** |
| **RF-007** (NOM-206) | Reclasificación manual de triage con justificación clínica obligatoria firmada por médico y auto-auditoría. | Inexistente. | Implementado en endpoint `/triages/{id}/reclasificaciones` con auditoría inalterable. | ✅ **Cerrada (100%)** |
| **PROV-RN-004** (NOM-206) | Matriz Manchester y SLAs de atención (Rojo: 0m, Naranja: 15m, Amarillo: 60m, Verde: 120m, Azul: 240m). | Inexistente. | Implementado determinísticamente con timestamps UTC auditados. | ✅ **Cerrada (100%)** |
| **PROV-RN-005** (NOM-206) | Gestión de tiempos excedidos y alertas escalables a jefatura de guardia cuando el SLA se supere. | Inexistente. | Comando `triage:verificar-sla` con cálculo de desviación y logs `SLA_VENCIDO`. | ✅ **Cerrada (100%)** |
| **PROV-UX-001** (ISO-25010) | Actualización de colas de triage en tiempo real mediante WebSockets / SSE. | Inexistente. | Canal SSE `GET /api/v1/triage/stream` y servicio reactivo frontend. | ✅ **Cerrada (100%)** |
| **PROV-BR-001** (WCAG 2.1 AA) | Codificación semántica universal de colores de triage y soporte para daltonismo (iconografía + texto). | Inexistente. | Integrado con iconografía SVG redundante y nombres semánticos. | ✅ **Cerrada (100%)** |
| **PROV-RN-006** (ISO-25010) | Tolerancia a fallos de red en admisión y captura retroactiva de folios offline. | Inexistente. | Parámetro `captura_retroactiva` con registro de fecha física y auditoría real. | ✅ **Cerrada (100%)** |
| **PROV-RN-007** (NOM-004) | Admisión de paciente no identificado (desconocido/N.N.) y posterior proceso de fusión de expediente. | Inexistente. | Soporte de admisión N.N. con folio temporal y endpoint transaccional de fusión. | ✅ **Cerrada (100%)** |
| **PROV-UX-002** (LFPDPPP) | Pantalla pública de sala de espera disociada (solo iniciales y turno, sin PII). | Inexistente. | Endpoint público `GET /api/v1/sala-espera` con disociación estricta (0 PII). | ✅ **Cerrada (100%)** |
| **PROV-UX-003** (WCAG 2.1 AA) | Accesibilidad web y ergonomía clínica WCAG 2.1 AA (operable 100% por teclado). | Inexistente. | Cumplimiento validado y tokens sin dependencia exclusiva de color. | ✅ **Cerrada (100%)** |
| **PROV-UI-001** (ERS Anexo G.1) | Implementación de pantallas de captura de triage según especificación de UI/UX. | Inexistente. | Vistas de triage clínico en frontend (`vitae-front`). | ✅ **Cerrada (100%)** |

---

### Sprint 4: ECE, Recetas Médicas Controladas e Interoperabilidad (Auditoría AI-04)
> *Normativas: NOM-004-SSA3-2012, COFEPRIS / SAT, LFPDPPP, HL7 FHIR R4, ISO/IEC-27001.*  
> *Estado del Sprint: ✅ **CERRADO (100% Cumplido - Certificado con 81 pruebas PHPUnit y 27 peticiones Bruno CLI)**.*

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Actual | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RF-008.1** (NOM-004) | Ficha de Identificación del ECE, antecedentes heredofamiliares, patológicos, no patológicos y alergias. | Tablas `paciente` y `expediente_c` creadas en Sprint 2 sin antecedentes estructurados. | Migración `antecedente_clinico`, modelo Eloquent con tipos validados (`HEREDOFAMILIAR`, `PATOLOGICO`, `NO_PATOLOGICO`, `ALERGIA`) y endpoints REST. | ✅ **Cerrada (100%)** |
| **RF-008.2** (NOM-004) | Notas médicas de evolución, historia clínica e interconsulta. Sellado inmutable post-firma. | Inexistente. | Modelo `NotaMedica`, migración con triggers nativos PostgreSQL/SQLite anti-UPDATE/DELETE tras cierre (`cerrada=true`), notas aclaratorias enlazadas. | ✅ **Cerrada (100%)** |
| **RF-009** (COFEPRIS/SAT) | Receta médica electrónica para medicamentos controlados (Fracc. I, II, III), cédula verificada, hash y código QR de un solo uso. | Inexistente. | Servicio `RecetaMedicaService`, folios únicos, validación e.firma y cédula, hash SHA-256, payload QR de un solo uso y detección de incidentes de seguridad al reuso. | ✅ **Cerrada (100%)** |
| **RF-010** (NOM-004) | Solicitud y vinculación de resultados de laboratorio e imagenología médica al ECE. | Vistas UI en front agregadas en dev; backend pendiente. | Endpoints `/api/v1/expedientes-clinicos/{id}/laboratorio/solicitudes` y `/resultados`, modelos `SolicitudEstudio` y `ResultadoEstudio` con cifrado AES-256-GCM. | ✅ **Cerrada (100%)** |
| **RNF-007** (HL7 FHIR R4) | Interoperabilidad semántica en salud basada en estándar HL7 FHIR R4 (`/fhir/Patient`, `/fhir/Encounter`, `/fhir/Observation`, `/fhir/MedicationRequest`). | Inexistente. | `FhirSerializerService` y `FhirController` exponiendo recursos R4 conformes a especificación HL7 con codificación LOINC para signos vitales. | ✅ **Cerrada (100%)** |
| **PROV-RN-008** (LFPDPPP) | Consentimiento informado del paciente con hash SHA-256 inmutable y retención estricta. | Inexistente. | `ConsentimientoService`, modelo `Consentimiento`, hash SHA-256 inmutable, revocación enlazada y excepción de urgencia por 2 médicos. | ✅ **Cerrada (100%)** |
| **PROV-RN-009** (LFPDPPP) | Atención y trazabilidad de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición). | Inexistente. | `SolicitudArcoController`, modelo `SolicitudArco`, cálculo de plazos de 20 y 15 días hábiles, dictamen fundado de improcedencia por retención legal de 5 años NOM-004. | ✅ **Cerrada (100%)** |
| **PROV-RN-010** (NOM-004) | Protocolo "Break-the-Glass" de acceso excepcional de emergencia al ECE con auto-auditoría. | Inexistente. | `BreakTheGlassService`, modelo `AccesoEmergencia`, verificación de relación asistencial, pase temporal de emergencia de 120 min y auto-auditoría reforzada. | ✅ **Cerrada (100%)** |
| **PROV-SEC-008** (LFPDPPP) | Exportación de expediente en PDF con marcas de agua dinámicas de auditoría y datos disociados. | Inexistente. | `PdfExpedienteService` con marca de agua dinámica `"COPIA CLÍNICA AUDITADA - USO INTERNO"`, trazabilidad de solicitante y enmascaramiento de PII. | ✅ **Cerrada (100%)** |
| **PROV-UI-002** (ERS Anexo G.2) | Pantallas de consulta de ECE, emisión de recetas médicas y panel de Break-the-Glass. | Componentes previos en front. | Backend totalmente acoplado con respuestas JSON estructuradas, cabeceras seguras y soporte integral para frontend y Bruno CLI. | ✅ **Cerrada (100%)** |

---

### Sprint 5: Portal Móvil, Estadística NOM-035 y QA Integral (Auditoría AI-05)
> *Normativas: NOM-024-SSA3-2012, LFPDPPP, NOM-035-STPS-2018, ISO/IEC-25010, OWASP ASVS L2.*  
> *Estado del Sprint: ✅ **CERRADO EN BACKEND (100% Cumplido - Certificado con 92 pruebas PHPUnit y 13 peticiones Bruno CLI)**.*

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Actual | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RF-011** (NOM-024 / LFPDPPP) | API móvil de solo lectura para consulta de paciente (perfil, citas, recetas, resultados) y rechazo HTTP 403 a escrituras clínicas. | Inexistente en API. | Rutas `/api/v1/mobile/*` con middleware `MobilePatientIsolation`, lectura permitida y bloqueo estricto a mutaciones clínicas (HTTP 403). | ✅ **Cerrada (100%)** |
| **RF-002.1** (OWASP ASVS L2 / LFPDPPP) | Aislamiento horizontal estricto de registros clínicos por claim `sub` del token del paciente (Anti-IDOR). | No había validación horizontal en móvil. | Middleware `MobilePatientIsolation` valida que cualquier `id_paciente` coincida con el token; rechazo inmediato con HTTP 403 y auto-auditoría. | ✅ **Cerrada (100%)** |
| **RNF-005** (ISO/IEC-25010) | Rendimiento y tiempos de respuesta P95 <= 2.0 s en visualización de expediente clínico con relaciones completas. | Sin benchmarking formal. | Consultas indexadas y optimizadas; prueba de estrés con 15 consultas completas certifica P95 <= 2.0 s (promedio < 150 ms). | ✅ **Cerrada (100%)** |
| **RNF-006** (ISO/IEC-25010) | Compatibilidad de cliente móvil: validación de `X-Client-Version` >= Android 10 (API 29); rechazo con HTTP 426 o 400. | Inexistente. | Middleware `CheckClientVersion` con rechazo HTTP 426 ante API level < 29 y HTTP 400 ante cabecera faltante o malformada. | ✅ **Cerrada (100%)** |
| **PROV-RF-001** (NOM-035 / LFPDPPP) | Tableros de control estadístico y reportes de salud laboral disociados (CLUES obligatorio, k-anonimato $n \ge 3$, cero PII). | Inexistente. | `EstadisticaController` con tableros agregados, exportación CSV disociada, inclusión de CLUES y auto-auditoría en `AUDITORIA_I`. | ✅ **Cerrada (100%)** |
| **PROV-RF-002** (NOM-024) | Módulo de agenda y confirmación de citas hospitalarias para pacientes y recepción. | Inexistente en base de datos y API. | Migración `cita`, modelo Eloquent `Cita`, endpoints `/citas` (creación, confirmación presencial y rechazo HTTP 422 ante citas expiradas). | ✅ **Cerrada (100%)** |
| **PROV-RNF-002** (ISO/IEC-25010) | Estrategia integral de aseguramiento de calidad (QA: unitarias, integración, e2e, estrés). | Cobertura en desarrollo. | Suite PHPUnit alcanzando 92 pruebas pasando al 100% (492 aserciones) y colección Bruno `09-Portal-Movil-QA` (13 peticiones). | ✅ **Cerrada (100%)** |
| **PROV-SEC-009** (OWASP ASVS L2 / ISO-27001) | Gestión de vulnerabilidades, inventario de componentes SBOM y evaluación de dependencias. | Inexistente. | Comando Artisan `secops:sbom` en formato CycloneDX v1.5, verificación estricta de 0 dependencias críticas (MS-03 = 0). | ✅ **Cerrada (100%)** |

---

## 2. Matriz Detallada de Trazabilidad Técnica vs Constraints Reales

### Tareas Certificadas — Sprint 2 (Auditoría AI-02)
> *Todas las tareas (TASK-W-020 a TASK-W-034) están 100% completadas y auditadas.*

### Tareas Certificadas — Sprint 3 (Auditoría AI-03)
> *Todas las tareas (TASK-W-035 a TASK-W-042) están 100% completadas y auditadas en PR #2 mergeado a `dev`.*

### Tareas Certificadas — Sprint 4 (Auditoría AI-04)

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-043` | RF-008.1, NOM-004 | Migración y modelo para antecedentes clínicos (heredofamiliares, patológicos, no patológicos, alergias) vinculados a `expediente_c`. | `database/migrations`, `app/Models/AntecedenteClinico.php` | Migración ejecutada en Supabase y seed clínico | ✅ **Cumplida (100%)** |
| `TASK-W-044` | RF-008.2, NOM-004 | Migración de notas médicas (`nota_medica`) con triggers anti-UPDATE/DELETE tras cambio a estado `cerrada = true`. | `database/migrations`, `app/Models/NotaMedica.php` | Test de bloqueo físico y eloquent de notas cerradas (HTTP 405) | ✅ **Cumplida (100%)** |
| `TASK-W-045` | RF-009, COFEPRIS | Servicio de emisión de recetas médicas controladas con validación de cédula, folios únicos, hash SHA-256 y payload QR. | `app/Services/RecetaMedicaService.php`, `app/Http/Controllers/Api/V1/RecetaController.php` | Tests de emisión, surtido de un solo uso y detección de reuso con HTTP 409 | ✅ **Cumplida (100%)** |
| `TASK-W-046` | RF-010, NOM-004 | Endpoints para solicitud y registro de estudios de laboratorio clínico vinculados al expediente del paciente. | `app/Http/Controllers/Api/V1/LaboratorioController.php` | Tests de creación y consulta de resultados con cifrado AES-256-GCM | ✅ **Cumplida (100%)** |
| `TASK-W-047` | RNF-007, FHIR R4 | Controlador de interoperabilidad HL7 FHIR R4 (`Patient`, `Encounter`, `Observation`, `MedicationRequest`) conforme a HL7. | `app/Http/Controllers/Api/V1/FhirController.php`, `app/Services/FhirSerializerService.php` | Validación contra schemas JSON FHIR R4 y códigos LOINC | ✅ **Cumplida (100%)** |
| `TASK-W-048` | PROV-RN-008, LFPDPPP | Registro de consentimiento informado con cálculo de hash SHA-256 inmutable de la voluntad del paciente. | `app/Models/Consentimiento.php`, `app/Services/ConsentimientoService.php` | Tests de inmutabilidad, verificación de hash y revocación enlazada | ✅ **Cumplida (100%)** |
| `TASK-W-049` | PROV-RN-010, NOM-004 | Protocolo de emergencia "Break-the-Glass" para consulta no autorizada de expediente con justificación y log reforzado en `AUDITORIA_I`. | `app/Services/BreakTheGlassService.php`, `app/Http/Controllers/Api/V1/AccesoEmergenciaController.php` | Test de bloqueo inicial (HTTP 403), pase temporal 120 min y consulta autorizada | ✅ **Cumplida (100%)** |
| `TASK-W-050` | PROV-SEC-008, LFPDPPP | Generación de PDF de expediente clínico con marca de agua disociada ("COPIA CLÍNICA AUDITADA - USO INTERNO"). | `app/Services/PdfExpedienteService.php` | Test de generación sin PII expuesta en texto plano y marcas de agua | ✅ **Cumplida (100%)** |
| `TASK-W-051` | RF-008 a RF-010, RNF-007 | Colección Bruno `08-ECE-Recetas-FHIR` cubriendo notas médicas, recetas con QR, FHIR R4 y Break-the-Glass. | `vitae-bruno/08-ECE-Recetas-FHIR` | Ejecución 100% exitosa con Bruno CLI (27/27 requests aprobados) | ✅ **Cumplida (100%)** |
| `TASK-W-052` | RF-008 a RF-010, RNF-007 | Suite completa de pruebas automatizadas PHPUnit para ECE, notas inmutables, recetas y FHIR R4. | `tests/Feature/Sprint4EceRecetasTest.php` | Suite de pruebas PHPUnit pasando al 100% (13/13 Sprint 4, 81/81 global) | ✅ **Cumplida (100%)** |

### Tareas Certificadas — Sprint 5 (Auditoría AI-05)

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-053` | RF-011, NOM-024 | Endpoints de solo lectura bajo `/api/v1/mobile/*` (perfil, citas, recetas, resultados) y bloqueo a mutaciones clínicas. | `app/Http/Controllers/Api/V1/MobileApiController.php`, `routes/api.php` | Test de consulta 200 y rechazo HTTP 403 a escrituras clínicas con log en `AUDITORIA_I` | ✅ **Cumplida (100%)** |
| `TASK-W-054` | RF-002.1, OWASP | Aislamiento horizontal anti-IDOR forzado por `sub` del token (HTTP 403 si difiere). | `app/Http/Middleware/MobilePatientIsolation.php` | Test de inyección en query, body y cancelación ajena retornando HTTP 403 | ✅ **Cumplida (100%)** |
| `TASK-W-055` | RNF-006, Android | Middleware de compatibilidad de cliente móvil: exige `X-Client-Version` >= Android 10 (API 29). | `app/Http/Middleware/CheckClientVersion.php` | Test de rechazo HTTP 400 (ausente) y 426 (API 28), aceptación API 29 y 34 | ✅ **Cumplida (100%)** |
| `TASK-W-056` | PROV-RF-002, NOM-024 | Modelo de agenda y citas médicas con agendamiento, consulta y confirmación por recepción. | `database/migrations`, `app/Models/Cita.php`, `app/Http/Controllers/Api/V1/CitaController.php` | Test de creación 201, confirmación 200 y rechazo HTTP 422 ante citas expiradas | ✅ **Cumplida (100%)** |
| `TASK-W-057` | PROV-RF-001, NOM-035 | Tableros estadísticos NOM-035 con inclusión obligatoria de CLUES, k-anonimidad $n \ge 3$ y cero PII. | `app/Http/Controllers/Api/V1/EstadisticaController.php` | Test de rechazo a personal no admin (403), verificación de 0 PII y exportación CSV | ✅ **Cumplida (100%)** |
| `TASK-W-058` | RNF-005, ISO-25010 | Benchmarking de rendimiento clínico en ECE garantizando P95 <= 2.0 s bajo carga. | `tests/Feature/Sprint5PortalMovilQaTest.php` | Prueba automatizada con 15 lecturas sucesivas con eager loading certificando P95 <= 2.0 s | ✅ **Cumplida (100%)** |
| `TASK-W-059` | PROV-SEC-009, SecOps | Comando Artisan `secops:sbom` generador de inventario SBOM CycloneDX y evaluación de CVEs críticas. | `app/Console/Commands/GenerarSbomCommand.php` | Test de generación de `sbom.json` con 0 vulnerabilidades críticas (MS-03 = 0) | ✅ **Cumplida (100%)** |
| `TASK-W-060` | PROV-RNF-002, QA | Suite de pruebas PHPUnit Sprint 5 (10 tests) y colección Bruno `09-Portal-Movil-QA` (13 peticiones). | `tests/Feature/Sprint5PortalMovilQaTest.php`, `vitae-bruno/09-Portal-Movil-QA` | 92/92 tests PHPUnit aprobados y suite Bruno homologada | ✅ **Cumplida (100%)** |

---

## 3. Matriz de Integración Frontend vs Backend (Endpoints Reales vs Mocks)
*Fecha de Diagnóstico: 2026-11-02 • Rama: `dev`*

| Módulo / Vista Frontend | Componente / Acción | Estado de Conexión | Mock Actual | Endpoint Backend Objetivo / TODO |
| :--- | :--- | :---: | :--- | :--- |
| **Administración** (`AdminWorkspaceView.vue`) | Catálogo de usuarios | 🟡 Mock | `usuarios` (6 usuarios simulados) | `GET /api/v1/admin/usuarios` |
| **Administración** (`AdminWorkspaceView.vue`) | Cuentas bloqueadas | 🟡 Mock | `cuentasBloqueadas` (2 simuladas) | `GET /api/v1/admin/bloqueos` / `POST /.../{id}/desbloquear` |
| **Administración** (`AdminWorkspaceView.vue`) | Bitácora de auditoría | 🟡 Mock | `trazas` (registros estáticos) | `GET /api/v1/auditoria` (NOM-024 / `AUDITORIA_I`) |
| **Administración** (`AdminWorkspaceView.vue`) | Contenedores y telemetría | 🟡 Mock | `contenedores`, `telemetria` | `GET /api/v1/admin/infraestructura/contenedores` / `telemetria` |
| **Laboratorio** (`LabWorkspaceView.vue`) | Banco de estudios pendientes | 🟡 Mock | `orders` (banco de pruebas local) | `GET /api/v1/laboratorio/estudios` |
| **Laboratorio** (`LabWorkspaceView.vue`) | Cargar y validar resultado | 🟡 Mock | `handleGuardarResultado` (alerta local) | `POST /api/v1/laboratorio/estudios/{folio}/resultado` |
| **Laboratorio** (`LabWorkspaceView.vue`) | Rechazo de muestra | 🟡 Mock | `handleRechazarMuestra` (alerta local) | `POST /api/v1/laboratorio/estudios/{folio}/rechazo` |
| **Médico** (`DoctorWorkspaceView.vue`) | Cola de pacientes | 🟡 Mock | `pacientesCola` (DEMO-0248..50) | `GET /api/v1/triage/cola` (`triageService.obtenerColaUrgencias`) |
| **Médico** (`NuevaNotaEceSection.vue`) | Guardar y firmar nota ECE | 🟡 Mock | `onGuardarFirmar` (alerta local) | `POST /api/v1/ece/notas` (NOM-004-SSA3-2012) |
| **Médico** (`EmitirRecetaSection.vue`) | Guardar y firmar receta | 🟡 Mock | `firmarYEmitir` (alerta local) | `POST /api/v1/recetas` (COFEPRIS / Cédula verificada) |
| **Médico** (`SolicitarLaboratorioSection.vue`) | Enviar solicitud lab | 🟡 Mock | `enviarSolicitud` (alerta local) | `POST /api/v1/laboratorio/solicitudes` (HL7 / FHIR R4) |
| **Enfermería** (`NurseWorkspaceView.vue`) | Pacientes en espera | 🟡 Mock | `pacientesEnEspera` (2 locales) | `GET /api/v1/triage/cola` (`triageService.obtenerColaUrgencias`) |
| **Enfermería** (`NurseWorkspaceView.vue`) | Registrar clasificación triage | 🟡 Mock | `registrarTriage` (in-memory) | `POST /api/v1/triage` (`triageService.clasificarTriage`) |
| **Enfermería** (`NurseWorkspaceView.vue`) | Reclasificar triage | 🟡 Mock | `actualizarReclasificacion` (in-mem) | `POST /api/v1/triage/reclasificar` (`triageService.reclasificarTriage`) |
| **Enfermería** (`NotaEnfermeriaSection.vue`) | Firmar nota enfermería | 🟡 Mock | `guardarNotaEnfermeria` (alerta local) | `POST /api/v1/enfermeria/notas` (NOM-004-SSA3-2012) |
| **Recepción** (`CurpSearchSection.vue`) | Búsqueda por CURP | 🟡 Mock | `ejecutarBusquedaCurp` (carga demo) | `GET /api/v1/pacientes?curp=...` (Validación RENAPO) |
| **Recepción** (`PatientRegisterSection.vue`) | Completar registro expediente | 🟡 Mock | `completarRegistroExpediente` (alerta) | `POST /api/v1/pacientes` (NOM-024 / Admisión) |
| **Recepción** (`AppointmentBookingSection.vue`) | Confirmar cita médica | 🟡 Mock | `confirmarCitaExpedirComprobante` (alerta) | `POST /api/v1/citas` (Agenda y Consultorios) |
| **Recepción** (`BedCensusSection.vue`) | Asignación de cama | 🟡 Mock | `confirmarAsignacionCama` (local) | `POST /api/v1/camas/{id}/asignar` (Censo Hospitalario) |
| **Recepción** (`WaitingRoomMonitorSection.vue`) | Monitor sala de espera | 🟡 Mock | `turnosPresenciales` (estático) | `GET /api/v1/triage/sala-espera` (`triageService.obtenerMonitorSalaEspera`) |

---

## 4. Matriz de Cumplimiento — Aplicación Móvil Android (`vitae-mobile` vs TASK-M & Requerimientos ERS)
*Fecha de Diagnóstico: 2026-11-08 • Auditoría: AI-05 (Sprint 5) • Estado Global del Cliente Móvil: ❌ **INCOMPLETA (0% de desarrollo funcional)**.*

### 4.1 Resumen del Diagnóstico
1. **Disparidad Backend vs Frontend Móvil:**
   - El **Backend (`vitae-backend`)** tiene su porción del Sprint 5 al **100% terminada y probada**: endpoints `/api/v1/mobile/*` (perfil, citas, recetas, resultados, expediente, consentimientos, ARCO), middleware de aislamiento anti-IDOR (`MobilePatientIsolation`), compatibilidad Android (`CheckClientVersion` >= API 29) y bloqueo estricto a mutaciones clínicas (HTTP 403).
   - La **Aplicación Móvil (`vitae-mobile`)** se encuentra en estado de **plantilla base vacía (scaffold inicial generado por Android Studio)**. `MainActivity.kt` contiene únicamente un `Greeting("Android")`. No cuenta con pantallas, navegación, modelos, repositorios, cliente de red ni integración con la API.
2. **Incumplimiento de Constraint RNF-006 (Android 10+ / API 29):**
   - En `vitae-mobile/app/build.gradle.kts` se tiene configurado `minSdk = 34` (Android 14), lo cual excluye dispositivos con Android 10 (API 29), 11, 12 y 13, violando directamente la especificación normativa **RNF-006** que exige `minSdk = 29`.
3. **Ausencia de Dependencias Clave:**
   - No se encuentran integradas librerías de inyección de dependencias (Hilt), cliente HTTP (Retrofit/OkHttp con Certificate Pinning), almacenamiento seguro (`androidx.security:security-crypto`), navegación Compose (`androidx.navigation:navigation-compose`), ni detección de root (RootBeer).

### 4.2 Tabla de Cumplimiento de Tareas Móviles (`TASK-M`)

| ID Tarea | Requerimiento / Norma | Descripción Técnica | Estado Actual en `vitae-mobile` | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| `TASK-M-001` | RNF-006, ISO-25010 | Proyecto Kotlin con MVVM, minSdk 29, Compose y pipeline CI | Proyecto inicial generado con minSdk=34; sin arquitectura MVVM estructurada. | ❌ **Pendiente (0%)** |
| `TASK-M-002` | PROV-UX-003, WCAG 2.1 AA | Tema Material 3 accesible (contraste >= 4.5:1, touch targets >= 48dp) | Tema default `PacientemovilTheme` sin tokens de accesibilidad ni estados de error/vacío. | ❌ **Pendiente (0%)** |
| `TASK-M-003` | PROV-SEC-001, ISO-27001 | Login con correo y contraseña + verificación OTP de dispositivo nuevo | Inexistente. `MainActivity` no implementa pantalla de login ni captura de credenciales. | ❌ **Pendiente (0%)** |
| `TASK-M-004` | PROV-SEC-002, OWASP ASVS | PKCE, almacenamiento Keystore/EncryptedSharedPreferences y refresh de sesión | Sin implementación de Keystore ni almacenamiento seguro de tokens. | ❌ **Pendiente (0%)** |
| `TASK-M-005` | PROV-SEC-001, NOM-024 | Cierre de sesión automático por inactividad de 5 minutos | Inexistente en el cliente móvil. | ❌ **Pendiente (0%)** |
| `TASK-M-006` | RNF-003, ISO-27001 | Capa de red OkHttp con TLS >= 1.2, certificate pinning e interceptor 401/403 | No existe capa de red ni Retrofit/OkHttp configurado. | ❌ **Pendiente (0%)** |
| `TASK-M-007` | PROV-SEC-011, OWASP | Detección de Root/Magisk, `FLAG_SECURE` y ofuscación R8/ProGuard | `isMinifyEnabled = false`, sin `FLAG_SECURE` ni validaciones de integridad. | ❌ **Pendiente (0%)** |
| `TASK-M-008` | RF-002, PROV-UI-003 | Pantallas de Inicio, Perfil de paciente y datos de aseguradora | Inexistentes. | ❌ **Pendiente (0%)** |
| `TASK-M-009` | PROV-RF-002, RF-011 | Pantalla de citas (consulta de agenda de solo lectura y solicitud/cancelación) | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-010` | RF-009, NOM-004 | Recetas médicas: listado y vista de detalle (MedicationRequest) con visor de QR | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-011` | RF-010, NOM-004 | Resultados de laboratorio: listado y visualizador seguro de PDF/imagen | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-012` | PROV-RN-008, LFPDPPP | Pantalla de Aviso de Privacidad y gestión de consentimientos | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-013` | PROV-RN-009, LFPDPPP | Flujo de solicitud de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición) | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-014` | PROV-UI-003, ISO-25010 | Pantallas de estado: sin conexión, sesión expirada, dispositivo incompatible | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-015` | RF-011, NOM-024 | Garantía de solo lectura en UI clínica (cero botones de edición/mutación) | Inexistente (no hay UI clínica implementada). | ❌ **Pendiente (0%)** |
| `TASK-M-016` | RNF-006, ISO-25010 | Matriz de dispositivos Android >= 10 en múltiples resoluciones | Sin pruebas de compatibilidad ni emuladores probados. | ❌ **Pendiente (0%)** |
| `TASK-M-017` | PROV-UX-003, WCAG 2.1 AA | Accesibilidad móvil: compatibilidad TalkBack y contraste validado | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-018` | RNF-007, FHIR R4 | Pruebas de contrato y consumo de recursos FHIR R4 desde el cliente | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-019` | RF-002.1, Anti-IDOR | Cobertura >= 80% y verificación de que la app jamás envíe `id_paciente` explícito | Inexistente (solo tests boilerplate vacíos). | ❌ **Pendiente (0%)** |
| `TASK-M-020` | AI-05, Auditoría | Paquete de evidencias de pruebas instrumentadas y APK compilado para auditoría | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-021` - `023` | PROV-RNF-005, AI-06 | Firma de APK v2+, SemVer, SBOM móvil y Quality Gate de distribución | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-024` | RF-003 | Manejo cliente de códigos HTTP 401 (refresh/logout) vs 403 (genérico) | Inexistente. | ❌ **Pendiente (0%)** |
| `TASK-M-025` | PROV-UI-003 | Configuración de NavHost y flujo base de navegación Compose | Inexistente. | ❌ **Pendiente (0%)** |
