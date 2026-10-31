# Matriz de Cumplimiento — Hospital Vitae SIH
**Sprint Activo:** Sprint 4 (Auditoría AI-04 — ECE, Recetas Médicas Controladas e Interoperabilidad HL7 FHIR R4)  
**Fecha de Actualización:** 2026-10-30  
**Rama:** `feature/sprint-4-ece-recetas-fhir`

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
> *Estado del Sprint: 🟡 **ACTIVO (Rama: `feature/sprint-4-ece-recetas-fhir`)**.*

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Actual | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RF-008.1** (NOM-004) | Ficha de Identificación del ECE, antecedentes heredofamiliares, patológicos, no patológicos y alergias. | Tablas `paciente` y `expediente_c` creadas en Sprint 2 sin antecedentes estructurados. | En diseño de migración y modelos relacionales. | 🟡 **En Progreso** |
| **RF-008.2** (NOM-004) | Notas médicas de evolución, historia clínica e interconsulta. Sellado inmutable post-firma. | Inexistente. | En diseño de migración `nota_medica` con triggers de sellado inmutable. | 🟡 **En Progreso** |
| **RF-009** (COFEPRIS/SAT) | Receta médica electrónica para medicamentos controlados (Fracc. I, II, III), cédula verificada, hash y código QR. | Inexistente. | En diseño de servicio `RecetaControladaService` con hash SHA-256 y payload QR. | 🟡 **En Progreso** |
| **RF-010** (NOM-004) | Solicitud y vinculación de resultados de laboratorio e imagenología médica al ECE. | Vistas UI en front agregadas en dev; backend pendiente. | En integración de endpoints `/api/v1/laboratorio/ordenes`. | 🟡 **En Progreso** |
| **RNF-007** (HL7 FHIR R4) | Interoperabilidad semántica en salud basada en estándar HL7 FHIR R4 (`/fhir/Patient`, `/fhir/Encounter`). | Inexistente. | En diseño de serializadores FHIR R4 y controlador de interoperabilidad. | 🟡 **En Progreso** |
| **PROV-RN-008** (LFPDPPP) | Consentimiento informado del paciente con hash SHA-256 inmutable y retención estricta. | Inexistente. | En diseño de migración `consentimiento_informado`. | 🟡 **En Progreso** |
| **PROV-RN-009** (LFPDPPP) | Atención y trazabilidad de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición). | Inexistente. | En diseño de módulo de solicitudes ARCO auditadas. | 🟡 **En Progreso** |
| **PROV-RN-010** (NOM-004) | Protocolo "Break-the-Glass" de acceso excepcional de emergencia al ECE con auto-auditoría. | Inexistente. | En diseño de middleware y evento `ACCESO_BREAK_THE_GLASS`. | 🟡 **En Progreso** |
| **PROV-SEC-008** (LFPDPPP) | Exportación de expediente en PDF con marcas de agua dinámicas de auditoría y datos disociados. | Inexistente. | En diseño de servicio de renderizado seguro de ECE en PDF. | 🟡 **En Progreso** |
| **PROV-UI-002** (ERS Anexo G.2) | Pantallas de consulta de ECE, emisión de recetas médicas y panel de Break-the-Glass. | Componentes previos en front. | En integración con nuevos endpoints de Sprint 4. | 🟡 **En Progreso** |

---

## 2. Matriz Detallada de Trazabilidad Técnica vs Constraints Reales

### Tareas Certificadas — Sprint 2 (Auditoría AI-02)
> *Todas las tareas (TASK-W-020 a TASK-W-034) están 100% completadas y auditadas.*

### Tareas Certificadas — Sprint 3 (Auditoría AI-03)
> *Todas las tareas (TASK-W-035 a TASK-W-042) están 100% completadas y auditadas en PR #2 mergeado a `dev`.*

### Tareas Planificadas — Sprint 4 (Auditoría AI-04)

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-043` | RF-008.1, NOM-004 | Migración y modelo para antecedentes clínicos (heredofamiliares, patológicos, no patológicos, alergias) vinculados a `expediente_c`. | `database/migrations`, `app/Models/AntecedenteClinico.php` | Migración ejecutada y seed clínico | ⏳ Pendiente |
| `TASK-W-044` | RF-008.2, NOM-004 | Migración de notas médicas (`nota_medica`) con triggers anti-UPDATE/DELETE tras cambio a estado `CERRADA`/`FIRMADA`. | `database/migrations`, `app/Models/NotaMedica.php` | Test de bloqueo físico/eloquent de notas cerradas | ⏳ Pendiente |
| `TASK-W-045` | RF-009, COFEPRIS | Servicio de emisión de recetas médicas controladas con validación de cédula, folios únicos, hash SHA-256 y payload QR. | `app/Services/RecetaMedicaService.php`, `app/Http/Controllers/Api/V1/RecetaController.php` | Tests de emisión, validación de fracción y QR | ⏳ Pendiente |
| `TASK-W-046` | RF-010, NOM-004 | Endpoints para solicitud y registro de estudios de laboratorio clínico vinculados al expediente del paciente. | `app/Http/Controllers/Api/V1/LaboratorioController.php` | Tests de creación y consulta de resultados | ⏳ Pendiente |
| `TASK-W-047` | RNF-007, FHIR R4 | Controlador de interoperabilidad HL7 FHIR R4 (`Patient`, `Encounter`, `MedicationRequest`) conforme a especificación HL7. | `app/Http/Controllers/Api/V1/FhirController.php`, `app/Services/FhirSerializerService.php` | Validación contra schemas JSON FHIR R4 | ⏳ Pendiente |
| `TASK-W-048` | PROV-RN-008, LFPDPPP | Registro de consentimiento informado con cálculo de hash SHA-256 inmutable de la voluntad del paciente. | `app/Models/ConsentimientoInformado.php`, `app/Services/ConsentimientoService.php` | Tests de inmutabilidad y verificación de hash | ⏳ Pendiente |
| `TASK-W-049` | PROV-RN-010, NOM-004 | Protocolo de emergencia "Break-the-Glass" para consulta no autorizada de expediente con justificación y log reforzado en `AUDITORIA_I`. | `app/Http/Middleware/BreakTheGlassMiddleware.php`, `app/Services/BreakTheGlassService.php` | Test de registro de evento crítico y acceso concedido | ⏳ Pendiente |
| `TASK-W-050` | PROV-SEC-008, LFPDPPP | Generación de PDF de expediente clínico con marca de agua disociada ("COPIA CLÍNICA AUDITADA - USO INTERNO"). | `app/Services/PdfExpedienteService.php` | Test de generación sin PII expuesta en texto plano | ⏳ Pendiente |
| `TASK-W-051` | RF-008 a RF-010, RNF-007 | Colección Bruno `08-ECE-Recetas-FHIR` cubriendo notas médicas, recetas con QR, FHIR R4 y Break-the-Glass. | `vitae-bruno/08-ECE-Recetas-FHIR` | Ejecución 100% exitosa con Bruno CLI | ⏳ Pendiente |
| `TASK-W-052` | RF-008 a RF-010, RNF-007 | Suite completa de pruebas automatizadas PHPUnit para ECE, notas inmutables, recetas y FHIR R4. | `tests/Feature/Sprint4EceRecetasTest.php` | Suite de pruebas PHPUnit pasando al 100% | ⏳ Pendiente |
