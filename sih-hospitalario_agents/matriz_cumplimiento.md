# Matriz de Cumplimiento — Hospital Vitae SIH
**Sprint Activo:** Sprint 3 (Auditoría AI-03 — Triage Manchester, Urgencias y Admisión)  
**Fecha de Actualización:** 2026-10-27  
**Rama:** `feature/sprint-3-triage-urgencias`

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
> *Estado del Sprint: ✅ **CONCLUIDO Y VERIFICADO (Rama: `feature/sprint-3-triage-urgencias`)**.*

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

## 2. Matriz Detallada de Trazabilidad Técnica vs Constraints Reales

### Tareas Certificadas — Sprint 2 (Auditoría AI-02)

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-020` | PROV-RNF-001, ISO-27001 | Dockerfile con versiones fijas inmutables, 0 `:latest`, `USER www-data`, hardening Apache/PHP y healthcheck `/up`. | `Dockerfile`, `docker-entrypoint.sh`, `render.yaml` | Dockerfile auditado, healthcheck `/up` probado | ✅ Cumplido |
| `TASK-W-021` | RNF-003, ISO-27001 | Conexión segura hacia PostgreSQL forzando TLS mediante `sslmode=require` y aislamiento en red. | `config/database.php` | Configuración verificada, conexión TLS forzada | ✅ Cumplido |
| `TASK-W-022` | RNF-002, NOM-004 | Tablas `paciente` (UUID) y `expediente_c` con relaciones estrictas y triggers nativos anti-DELETE/TRUNCATE. | `database/migrations`, `app/Models/Paciente.php`, `app/Models/ExpedienteClinico.php` | Migración ejecutada, tests anti-DELETE aprobados | ✅ Cumplido |
| `TASK-W-023` | RNF-001, PROV-SEC-005 | Tabla `auditoria_i` con 13 columnas normativas, INSERT-only y huella SHA-256 (génesis 64 ceros). | `database/migrations`, `app/Models/AuditoriaI.php` | Migración ejecutada, schema verificado | ✅ Cumplido |
| `TASK-W-024` | RNF-001, PROV-SEC-005 | `AuditLogger` inyectado en eventos del sistema (login, logout, 403, cambios de estado). Exclusión de `password`, `token`, `otp`. | `app/Services/AuditLogger.php` | Logs generados sin datos sensibles | ✅ Cumplido |
| `TASK-W-025` | RF-001 | `GET /api/v1/auditoria` protegido con middleware `role:ADMINISTRADOR`. Registra `CONSULTA_AUDITORIA`. `POST/PUT/DELETE` -> 405. | `app/Http/Controllers/Api/V1/AuditoriaIController.php` | Bruno: `05-Auditoria-I` y PHPUnit tests | ✅ Cumplido |
| `TASK-W-026` | PROV-SEC-005 | Comando `auditoria:verificar-cadena` que valida `hash[n] == sha256(canonico[n] + hash[n-1])`. Alerta si hay alteración. | `app/Console/Commands/VerificarCadenaAuditoriaCommand.php` | Test de detección de alteración artificial | ✅ Cumplido |
| `TASK-W-027` | RNF-003, ISO-27001 | Cifrado AES-256-GCM para adjuntos clínicos con verificación de tag y almacenamiento opaco (cero cadenas `%PDF` o PII en claro). | `app/Services/CifradoAdjuntosService.php` | Tests de cifrado, descifrado y anti-tampering aprobados | ✅ Cumplido |
| `TASK-W-028` | RNF-003, ASVS L2 | Cabeceras de seguridad HTTP: HSTS >= 1 año (`max-age=31536000; includeSubDomains; preload`), `nosniff`, `DENY` y CSP restrictiva. | `app/Http/Middleware/SecurityHeadersMiddleware.php`, `bootstrap/app.php` | Headers verificados en suite de pruebas | ✅ Cumplido |
| `TASK-W-029` | RNF-004, ISO-25010 | Réplicas >= 2, límites de CPU/RAM, reinicio automático y endpoint `GET /api/v1/salud` para monitoreo activo de salud y RTO <= 300 s. | `routes/api.php`, `docker-compose.yml`, `vitae-bruno` | Tests automatizados (3/3), Bruno request `03-Salud` | ✅ Cumplido |
| `TASK-W-030` | PROV-SEC-006, ISO-27001 | Respaldos diarios cifrados AES-256, regla 3-2-1 con copias offsite, retención 30d/12m y monitor de lag WAL (`db:verificar-lag-wal`). | `app/Services/BackupService.php`, `app/Console/Commands/*Backup*.php` | Dump cifrado, test de simulación de lag > 600s | ✅ Cumplido |
| `TASK-W-031` | PROV-SEC-007, CENAM | Comando `ntp:verificar-sincronizacion` que mide deriva contra CENAM/pool.ntp.org; alerta y audita si \|offset\| > 1s; enforcement UTC. | `app/Services/NtpService.php`, `app/Console/Commands/VerificarSincronizacionHorariaCommand.php`, `config/database.php` | Tests automatizados (5/5) aprobados | ✅ Cumplido |
| `TASK-W-032` | PROV-SEC-010 | Seeders con datos ficticios para Dev/Pruebas y verificación automática de ausencia de CURP reales. | `database/seeders/ClinicoSinteticoSeeder.php` | Seed ejecutado en Supabase sin PII | ✅ Cumplido |
| `TASK-W-033` | PROV-RN-003, NOM-004 | Actualización de `fecha_ultimo_acto_medico` y comando `expedientes:reporte-retencion` para expedientes >= 1,825 días. | `app/Console/Commands/ReporteRetencionExpedientesCommand.php` | Reporte informativo ejecutado y testeado | ✅ Cumplido |
| `TASK-W-034` | RNF-001, RNF-002, RNF-003, RNF-004, PROV-SEC-006, PROV-SEC-007 | Suite completa de pruebas para inalterabilidad, denegación de DELETE, verificación de hashes, Bruno, NTP, cifrado, respaldos y HA. | `tests/Feature/Sprint2*.php` | 53/53 pruebas aprobadas (100%) | ✅ Cumplido |

---

### Tareas Certificadas — Sprint 3 (Auditoría AI-03)

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-035` | RF-004, NOM-004 | Migración y modelo para Signos Vitales y Triage (presión arterial, FC, FR, temp, EVA 0-10, AVDI) con triggers anti-DELETE. | `database/migrations`, `app/Models/Triage.php`, `app/Models/SalaAtencion.php` | Migración ejecutada, constraints de rango biológico | ✅ Cumplido |
| `TASK-W-036` | RF-005, PROV-RN-004, NOM-206 | Servicio de clasificación Manchester determinista con regla crítica (inconsciente fuerza ROJO) y SLAs auditados. | `app/Services/TriageManchesterService.php` | Tests unitarios y de integración aprobados | ✅ Cumplido |
| `TASK-W-037` | RF-006, NOM-206 | Cola de urgencias viva con algoritmo de priorización FIFO ponderado, 3 salas de emergencia y liberación de consultorios. | `app/Services/ColaUrgenciasService.php`, `app/Http/Controllers/Api/V1/TriageController.php` | Tests de ordenamiento por SLA y asignación de salas | ✅ Cumplido |
| `TASK-W-038` | RF-007, NOM-206 | Endpoint para reclasificación médica manual de triage, requiriendo justificación obligatoria y registrando evento en `AUDITORIA_I`. | `app/Http/Controllers/Api/V1/TriageController.php` | Tests de denegación sin justificación y bloqueo degradar ROJO | ✅ Cumplido |
| `TASK-W-039` | PROV-RN-007, NOM-004 | Registro de paciente no identificado (N.N.) y endpoint seguro para fusión de expedientes con trazabilidad completa. | `app/Services/AdmisionService.php`, `app/Http/Controllers/Api/V1/PacienteController.php` | Tests de fusión transaccional y reasignación de triages | ✅ Cumplido |
| `TASK-W-040` | PROV-UX-001, PROV-UX-002, ISO-25010 | Mecanismo de notificación en tiempo real (SSE) y pantalla pública de sala de espera disociada (cero PII). | `routes/api.php`, `app/Http/Controllers/Api/V1/SalaEsperaController.php` | Verificación de endpoint SSE y garantía 0 PII en payload | ✅ Cumplido |
| `TASK-W-041` | RF-004, RF-005, RF-006, RF-007 | Colección de peticiones Bruno (`07-Triage-Manchester`) para certificación de endpoints de urgencias y triage. | `vitae-bruno/07-Triage-Manchester` | 12/12 peticiones exitosas con Bruno CLI | ✅ Cumplido |
| `TASK-W-042` | RF-004 a RF-007, PROV-RN-004 a 007 | Suite completa de pruebas automatizadas PHPUnit para Triage Manchester, signos vitales, SLAs y alertas de escalamiento. | `tests/Feature/Sprint3TriageTest.php` | 14/14 pruebas aprobadas (100%), 68/68 suite global | ✅ Cumplido |
