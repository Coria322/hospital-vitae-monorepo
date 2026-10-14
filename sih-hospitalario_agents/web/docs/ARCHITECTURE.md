# ARCHITECTURE.md — SIH · web/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Patrón arquitectónico: Cliente-Servidor 3 capas, monolito modular Laravel + SPA Vue (SUP-011)
> Fecha: 2026-10-09

---

## VISIÓN GENERAL

Una única API RESTful (Laravel) concentra reglas de negocio, auditoría y FHIR. La SPA Vue (red privada del hospital) y la app Android consumen esa API. La BD es Supabase (PostgreSQL) autohospedado en Docker, aislado y accesible solo por el backend (AMB-001). El móvil usa un namespace propio de solo lectura.

## CAPAS

```
┌─────────────────────────────────────────┐
│ PRESENTACIÓN  ← Vue SPA (web) · Kotlin (mobile/)   │
├─────────────────────────────────────────┤
│ APLICACIÓN    ← Controllers → Services (casos de uso) │
├─────────────────────────────────────────┤
│ DOMINIO       ← Entidades + motor Manchester + reglas │
├─────────────────────────────────────────┤
│ INFRA         ← Repositories, Supabase/PostgreSQL, auth, Reverb │
└─────────────────────────────────────────┘
```

## TOPOLOGÍA DOCKER

```
Internet ✗ ── Proxy inverso (TLS1.2+, WAF, rate-limit) ──┬─ SPA Vue (solo red privada)
                                                         └─ API Laravel x>=2 réplicas ── red interna ── Supabase/PostgreSQL (sin puerto público)
                                                                                         └─ Reverb (WebSocket) · Jobs/Scheduler · Vault de secretos
```

## MÓDULOS

| Módulo | Responsabilidad | Requerimientos | Persistencia |
|---|---|---|---|
| auth | Login, MFA, JWT, bloqueo 3/1h, PKCE móvil | RF-002, RNF-008, PROV-SEC-001/002 | usuario, token_revocado |
| rbac / api-gateway | Spatie Permission; middlewares autenticación→permiso→propiedad | RF-002, RF-002.1, RF-003 | roles/permisos |
| auditoria | AuditLogger, esquema `auditoria`, cadena SHA-256, verificación diaria | RNF-001, PROV-SEC-005 | auditoria.auditoria_i |
| usuarios | Alta/baja/reactivación, ciclo de cuentas | RF-001, PROV-SEC-003 | usuario |
| triage | Captura, motor Manchester, SLA, reclasificación | RF-004..RF-007, PROV-RN-004..006 | triage |
| colas | Colas, 3 salas de emergencia, sala de espera | RF-006 | sala_atencion |
| pacientes | Alta, no identificados, fusión | PROV-RN-007 | paciente |
| ece | Expediente, notas inmutables, acceso de emergencia | RF-008.x, PROV-RN-010 | expediente_c, nota_medica |
| recetas | Recetas, e.firma (SAT), QR | RF-009 | receta |
| estudios | Solicitudes y resultados cifrados | RF-010 | solicitud_estudio, resultado_estudio |
| fhir | Mapeo a Patient/Encounter/Observation/MedicationRequest | RNF-007 | — |
| privacidad | Consentimiento, ARCO, aviso | PROV-RN-008/009 | consentimiento, solicitud_arco |
| dashboards | NOM-035 disociado con CLUES | PROV-RF-001 | vistas agregadas |
| agenda | Citas | PROV-RF-002 | cita |
| api-mobile | Namespace del paciente | RF-011 | — |

## SUPERFICIE DE API (fuente de verdad web/móvil)

Convenciones: prefijo `/api/v1`; kebab-case; JWT RS256 en `Authorization: Bearer`; token con claim `client` ∈ {web, mobile}. Token de paciente en `/api/v1/*` (no mobile) → 403. Token de personal en `/api/v1/mobile/*` → 403. M* = FHIR consumido por móvil solo a través de `/mobile/*`.

| Método | Ruta | Módulo | Requerimiento | Descripción | Web | Móvil |
|---|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | `auth` | `RF-002` | Login de personal; 423 al 3er fallo | ✔ | — |
| POST | `/api/v1/auth/mfa/verify` | `auth` | `PROV-SEC-001` | Verifica TOTP | ✔ | — |
| POST | `/api/v1/auth/refresh` | `auth` | `PROV-SEC-002` | Refresh rotativo | ✔ | — |
| POST | `/api/v1/auth/logout` | `auth` | `PROV-SEC-002` | Revoca token (blacklist) | ✔ | — |
| POST | `/api/v1/auth/password/reset-request` | `auth` | `RNF-008` | Token de un solo uso | ✔ | — |
| POST | `/api/v1/auth/password/reset` | `auth` | `RNF-008` | Restablece contraseña | ✔ | — |
| POST | `/api/v1/mobile/auth/token` | `auth` | `PROV-SEC-002` | Código OAuth2 + PKCE S256 | — | ✔ |
| POST | `/api/v1/mobile/auth/otp-verify` | `auth` | `PROV-SEC-001` | OTP de dispositivo nuevo | — | ✔ |
| POST | `/api/v1/mobile/auth/refresh` | `auth` | `PROV-SEC-002` | Refresh rotativo | — | ✔ |
| POST | `/api/v1/mobile/auth/logout` | `auth` | `PROV-SEC-002` | Revocación inmediata | — | ✔ |
| GET/POST | `/api/v1/usuarios` | `usuarios` | `RF-001` | Listar y dar de alta | ✔ | — |
| POST | `/api/v1/usuarios/{id}/baja | /reactivar` | `usuarios` | `RF-001` | Baja <= 24 h; reactivación | ✔ | — |
| GET | `/api/v1/auditoria` | `auditoria` | `RF-001` | Solo lectura (ADMIN, SEG, AUDITOR) | ✔ | — |
| POST | `/api/v1/pacientes | /{id}/fusiones` | `pacientes` | `PROV-RN-007` | Alta y fusión | ✔ | — |
| POST/GET | `/api/v1/triages` | `triage` | `RF-004` | Captura y consulta | ✔ | — |
| POST | `/api/v1/triages/{id}/reclasificaciones` | `triage` | `RF-007` | Motivo obligatorio | ✔ | — |
| GET | `/api/v1/colas-atencion | /sala-espera` | `colas` | `RF-006` | Colas y pantalla pública (folio) | ✔ | — |
| POST | `/api/v1/salas/{id}/liberar` | `colas` | `RF-006` | Libera consultorio/sala | ✔ | — |
| POST/GET | `/api/v1/expedientes-clinicos` | `ece` | `RF-008.1` | Crear/consultar | ✔ | — |
| POST | `/api/v1/expedientes-clinicos/{id}/notas | /notas/{nid}/aclaraciones` | `ece` | `RF-008.2` | Notas inmutables | ✔ | — |
| POST | `/api/v1/expedientes-clinicos/{id}/exportaciones` | `ece` | `PROV-SEC-008` | Exportación auditada | ✔ | — |
| POST | `/api/v1/accesos-emergencia` | `ece` | `PROV-RN-010` | Acceso fuera de relación asistencial | ✔ | — |
| POST/GET | `/api/v1/recetas` | `recetas` | `RF-009` | Emitir y consultar | ✔ | — |
| POST | `/api/v1/recetas/{id}/firma | /{folio}/surtido` | `recetas` | `RF-009` | e.firma y consumo de QR | ✔ | — |
| POST/GET | `/api/v1/solicitudes-estudio | /{id}/resultados` | `estudios` | `RF-010` | Solicitudes y resultados | ✔ | — |
| GET/PATCH | `/api/v1/citas | /{id}/confirmacion` | `agenda` | `PROV-RF-002` | Agenda y confirmación | ✔ | — |
| POST | `/api/v1/consentimientos | /{id}/revocaciones` | `consentimientos` | `PROV-RN-008` | Admisión | ✔ | — |
| POST | `/api/v1/solicitudes-arco` | `arco` | `PROV-RN-009` | Admisión | ✔ | — |
| GET | `/api/v1/dashboards/estadisticas | /reportes/nom-035/exportacion` | `dashboards` | `PROV-RF-001` | Disociados + CLUES | ✔ | — |
| GET | `/api/v1/fhir/{Patient|Encounter|Observation|MedicationRequest}/{id}` | `fhir` | `RNF-007` | FHIR R4 (PascalCase, SUP-010) | ✔ | ✔ |
| GET | `/api/v1/mobile/perfil` | `api-mobile` | `RF-011` | Patient FHIR del paciente | — | ✔ |
| GET | `/api/v1/mobile/citas` | `api-mobile` | `PROV-RF-002` | Citas propias | — | ✔ |
| GET | `/api/v1/mobile/recetas | /{id}` | `api-mobile` | `RF-009` | MedicationRequest propios | — | ✔ |
| GET | `/api/v1/mobile/resultados | /{id}/archivo` | `api-mobile` | `RF-010` | Resultados propios (stream) | — | ✔ |
| GET | `/api/v1/mobile/aviso-privacidad` | `api-mobile` | `PROV-RN-008` | Aviso vigente | — | ✔ |
| POST | `/api/v1/mobile/consentimientos` | `api-mobile` | `PROV-RN-008` | Escritura lista blanca (SUP-002) | — | ✔ |
| GET/POST | `/api/v1/mobile/solicitudes-arco` | `api-mobile` | `PROV-RN-009` | Escritura lista blanca (SUP-002) | — | ✔ |

### Paridad web vs móvil
| Capacidad | Web (personal) | Móvil (paciente) |
|---|---|---|
| Autenticación | credencial+contraseña, MFA | correo+contraseña, OTP dispositivo nuevo, PKCE |
| Sesión inactiva | 15 min | 5 min |
| Triage, ECE, recetas (escritura), estudios, auditoría, dashboards, usuarios | ✔ según rol | ✘ |
| Citas, recetas, resultados, perfil | lectura/gestión por rol | solo lectura, solo propios |
| Consentimiento y ARCO | admisión | escritura de lista blanca (SUP-002) |

## FLUJOS PRINCIPALES

```
Triage:  ENFERMERIA → Vue → POST /triages → [mw 401/403] → Motor Manchester → triage + AUDITORIA_I (misma transacción) → Reverb → pantallas
Receta:  MEDICO → POST /recetas → mw propiedad → validar e.firma X.509 (SAT) → QR único → PDF habilitado
Móvil:   App → /mobile/auth (PKCE) → JWT 15 min → GET /mobile/recetas → scope id_paciente=sub → FHIR → app
```

## MODELO DE PERSISTENCIA

Ver `schemas/DOMAIN_SCHEMA.json` (17 entidades; 6 de Tabla 26 + derivadas marcadas ⚠️).

Auditoría: esquema `auditoria` (propietario `sih_audit_owner`); rol `sih_app` solo INSERT; triggers rechazan UPDATE/DELETE/TRUNCATE; trigger BEFORE INSERT calcula `huella_integridad` con `pg_advisory_xact_lock`; copia externa de solo escritura; verificación diaria `auditoria.verificar_cadena()`.
Retención: triggers + REVOKE bloquean DELETE/TRUNCATE en tablas clínicas; baja lógica por `estado_activo`.

## SEGURIDAD

| Área | Mecanismo | Fuente |
|---|---|---|
| Autenticación | Bcrypt >=12, MFA TOTP (roles altos), bloqueo 3/1h, mensajes genéricos | RNF-008, PROV-SEC-001 |
| Tokens | JWT RS256/ES256 <= 15 min, refresh rotativo, blacklist | PROV-SEC-002 |
| Autorización | RBAC Spatie + middleware de propiedad + scope por paciente | RF-002/003, RF-002.1 |
| Transporte | TLS 1.2+/1.3, HSTS 1 año, cifrado BD↔backend | RNF-003 |
| Reposo | AES-256 volúmenes; AES-256-GCM adjuntos | RNF-003 |
| Secretos | Bóveda, rotación 90 d, escaneo CI | PROV-SEC-004 |
| Tiempo | NTP/CENAM <= 1 s, UTC | PROV-SEC-007 |
| Vulnerabilidades | SAST/SCA/DAST/pentest, CVSS 72 h/7 d, SBOM | PROV-SEC-009 |

## MANEJO DE ERRORES

| Código | Escenario | Comportamiento |
|---|---|---|
| 400/422 | Entrada inválida | Mensaje descriptivo por campo, sin stack |
| 401 | Sin token/expirado/inválido | Redirigir a login; sin consultas SQL |
| 403 | Rol o propiedad no autorizan, UUID ajeno | Error de autorización; auditar `DENEGADO` |
| 405 | PUT/PATCH/DELETE sobre recurso inmutable | Rechazo; sin cambios |
| 409 | Duplicado, QR reusado | Rechazo + auditoría |
| 423 | Cuenta suspendida | `code=ACCOUNT_SUSPENDED` |
| 426 | Cliente móvil no soportado | Mensaje controlado |
| 500 | Error interno | Log (fecha, hora, HTTP, endpoint); mensaje genérico |

## EVENTOS Y SINCRONIZACIÓN

| Evento | Productor | Consumidor | Fuente |
|---|---|---|---|
| `triage.alert` | triage | pantallas médico/Jefatura | RF-005 |
| `cola.actualizada` | colas | sala de espera | RF-006 |
| `sla.vencido` | job SLA | Jefatura | PROV-RN-005 |

## SUPUESTOS ARQUITECTÓNICOS
| ID | Supuesto | Alternativa |
|---|---|---|
| SUP-ARQ-001 | Supabase autohospedado (SUP-001/AMB-001) | PostgreSQL puro en Docker |
| SUP-ARQ-002 | Reverb para tiempo real (SUP-006) | Polling 5 s |
