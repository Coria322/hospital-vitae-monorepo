# Matriz de Cumplimiento — Hospital Vitae SIH
**Sprint:** Sprint 2 (Auditoría AI-02 — Infraestructura, Base de Datos y Trazabilidad Inalterable)  
**Fecha de Actualización:** 2026-10-22  
**Rama:** `feature/sprint-2-auditoria-inalterable`

---

## 1. Resumen Ejecutivo de Cumplimiento

Esta matriz contrasta el estado actual del sistema frente a los requerimientos funcionales, no funcionales y normativos (NOM-024-SSA3-2012, NOM-004-SSA3-2012, ISO/IEC-27001) para la auditoría **AI-02**.

| Requerimiento / Norma | Descripción Normativa | Estado Inicial | Estado Objetivo (Sprint 2) | Nivel de Brecha |
| :--- | :--- | :--- | :--- | :---: |
| **RNF-001** (NOM-024) | Bitácora inalterable `AUDITORIA_I` de 13 columnas con encadenamiento SHA-256 en UTC. | Tabla `auditorias` básica sin encadenamiento ni campos normativos. | Migración `auditoria_i` con 13 columnas canónicas, SHA-256 con bloque génesis y verificación de cadena. | **Alta** (En progreso) |
| **PROV-SEC-005** (ISO-27001) | Garantía INSERT-only; bloqueo de UPDATE, DELETE y TRUNCATE; sanitización de credenciales. | La tabla previa admitía DELETE y UPDATE. No había sanitización de secretos. | Reglas de inalterabilidad y servicio `AuditLogger` que enmascara/ignora tokens y passwords. | **Alta** (En progreso) |
| **RF-001 (Auditoría)** | Consulta de auditoría de solo lectura para ADMINISTRADOR y evento `CONSULTA_AUDITORIA`. | Controlador `AuditoriaController` básico sin auto-auditoría ni restricción de métodos HTTP 405. | `GET /api/v1/auditoria` paginado y filtrable; verbos POST/PUT/DELETE retornan HTTP 405; auto-registro del evento. | **Media** (Por implementar) |
| **PROV-SEC-005 (Job)** | Verificación criptográfica continua de la integridad de la cadena SHA-256. | Inexistente. | Comando `php artisan auditoria:verificar-cadena` con detección de eslabones rotos. | **Alta** (Por implementar) |
| **RNF-002** (NOM-004) | Retención documental de 5 años (1825 días) y prohibición de DELETE físico en registros médicos. | Inexistente en tablas clínicas. | Soporte de borrado lógico y trigger/regla anti-DELETE. | **Media** (Por implementar) |
| **PROV-RN-003** (NOM-004) | Tracking de `fecha_ultimo_acto_medico` para cómputo de retención legal. | Inexistente. | Campo y cálculo de antigüedad legal en expedientes. | **Baja** (Por implementar) |

---

## 2. Matriz Detallada de Trazabilidad Técnica vs Constraints Reales

| ID Tarea | Requisito | Constraint / Criterio de Aceptación Real | Componente Afectado | Evidencia Requerida | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `TASK-W-023` | RNF-001, PROV-SEC-005 | Tabla `auditoria_i` con 13 columnas: `id_auditoria`, `fecha_hora` (UTC), `tipo_movimiento`, `id_usuario`, `tabla_afectada`, `registro_id`, `valor_anterior`, `valor_nuevo`, `origen`, `resultado`, `motivo`, `es_necesario_auditoria_interna`, `huella_integridad`. Génesis: 64 ceros. | `vitae-backend/database/migrations` | Migración ejecutada, schema verificado | ⏳ En desarrollo |
| `TASK-W-024` | RNF-001, PROV-SEC-005 | `AuditLogger` inyectado en eventos del sistema (login, logout, 403, cambios de estado). Exclusión de `password`, `token`, `otp`. | `vitae-backend/app/Services/AuditLogger.php` | Logs generados sin datos sensibles | ⏳ Pendiente |
| `TASK-W-025` | RF-001 | `GET /api/v1/auditoria` protegido con middleware `role:ADMINISTRADOR`. Registra evento `CONSULTA_AUDITORIA`. `POST/PUT/DELETE` devuelven `405 Method Not Allowed`. | `vitae-backend/app/Http/Controllers/Api/AuditoriaIController.php` | Bruno: `05-Auditoria-I` y PHPUnit tests | ⏳ Pendiente |
| `TASK-W-026` | PROV-SEC-005 | Comando `auditoria:verificar-cadena` que valida `hash[n] == sha256(canonico[n] + hash[n-1])`. Alerta si hay alteración. | `vitae-backend/app/Console/Commands` | Test de detección de alteración artificial | ⏳ Pendiente |
| `TASK-W-034` | RNF-001, RNF-002 | Suite completa de pruebas para inalterabilidad, denegación de DELETE, verificación de hashes y Bruno endpoints. | `vitae-backend/tests/Feature`, `vitae-bruno` | 100% pruebas aprobadas | ⏳ Pendiente |
