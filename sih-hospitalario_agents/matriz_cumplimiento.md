# Matriz de Cumplimiento — Hospital Vitae SIH
**Sprint:** Sprint 2 (Auditoría AI-02 — Infraestructura, Base de Datos y Trazabilidad Inalterable)  
**Fecha de Actualización:** 2026-10-26  
**Rama:** `feature/sprint-2-auditoria-inalterable`

---

## 1. Resumen Ejecutivo de Cumplimiento

Esta matriz contrasta el estado del sistema frente a los requerimientos normativos (NOM-024-SSA3-2012, NOM-004-SSA3-2012, ISO/IEC-27001, LFPDPPP) para la auditoría **AI-02**.

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

---

## 2. Matriz Detallada de Trazabilidad Técnica vs Constraints Reales

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-020` | PROV-RNF-001, ISO-27001 | Dockerfile con versiones fijas inmutables, 0 ocurrencias de `:latest`, usuario no-root (`USER www-data`), hardening Apache/PHP y HEALTHCHECK `/up`. | `Dockerfile`, `docker-entrypoint.sh`, `render.yaml`, `.dockerignore` | Dockerfile auditado sin `:latest`, healthcheck `/up` probado | ✅ Cumplido |
| `TASK-W-022` | RNF-002, NOM-004 | Tablas `paciente` (UUID) y `expediente_c` con relaciones estrictas y triggers nativos que abortan DELETE/TRUNCATE. | `database/migrations`, `app/Models/Paciente.php`, `app/Models/ExpedienteClinico.php` | Migración ejecutada, tests anti-DELETE aprobados | ✅ Cumplido |
| `TASK-W-023` | RNF-001, PROV-SEC-005 | Tabla `auditoria_i` con 13 columnas normativas, INSERT-only y huella SHA-256 (génesis 64 ceros). | `database/migrations`, `app/Models/AuditoriaI.php` | Migración ejecutada, schema verificado | ✅ Cumplido |
| `TASK-W-024` | RNF-001, PROV-SEC-005 | `AuditLogger` inyectado en eventos del sistema (login, logout, 403, cambios de estado). Exclusión de `password`, `token`, `otp`. | `app/Services/AuditLogger.php` | Logs generados sin datos sensibles | ✅ Cumplido |
| `TASK-W-025` | RF-001 | `GET /api/v1/auditoria` protegido con middleware `role:ADMINISTRADOR`. Registra evento `CONSULTA_AUDITORIA`. `POST/PUT/DELETE` devuelven `405 Method Not Allowed`. | `app/Http/Controllers/Api/V1/AuditoriaIController.php` | Bruno: `05-Auditoria-I` y PHPUnit tests | ✅ Cumplido |
| `TASK-W-026` | PROV-SEC-005 | Comando `auditoria:verificar-cadena` que valida `hash[n] == sha256(canonico[n] + hash[n-1])`. Alerta si hay alteración. | `app/Console/Commands/VerificarCadenaAuditoriaCommand.php` | Test de detección de alteración artificial | ✅ Cumplido |
| `TASK-W-032` | PROV-SEC-010 | Seeders con datos ficticios para Dev/Pruebas y verificación automática de ausencia de CURP reales. | `database/seeders/ClinicoSinteticoSeeder.php` | Seed ejecutado en Supabase sin PII | ✅ Cumplido |
| `TASK-W-033` | PROV-RN-003, NOM-004 | Actualización de `fecha_ultimo_acto_medico` y comando `expedientes:reporte-retencion` para expedientes >= 1,825 días. | `app/Console/Commands/ReporteRetencionExpedientesCommand.php` | Reporte informativo ejecutado y testeado | ✅ Cumplido |
| `TASK-W-034` | RNF-001, RNF-002 | Suite completa de pruebas para inalterabilidad, denegación de DELETE, verificación de hashes y Bruno endpoints. | `tests/Feature/Sprint2AuditoriaTest.php`, `Sprint2PersistenciaClinicaTest.php` | 35/35 pruebas aprobadas (100%) | ✅ Cumplido |
