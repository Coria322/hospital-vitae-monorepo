# AGENTS.md — SIH Sistema de Información Hospitalaria · mobile/

> Generado por: ers-to-agentic skill
> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Fecha: 2026-10-09
> Versión: 1.0.0
> Alcance de esta carpeta: mobile (app Android Kotlin)

---

## PRODUCT CONTEXT

### Objetivo del sistema
Establecer lineamientos técnicos, metodológicos y normativos para asegurar calidad, seguridad y cumplimiento legal en el desarrollo y operación del Sistema de Información Hospitalaria (NOM-004, NOM-024, NOM-035, NOM-206, HL7/FHIR, ISO 27001/27002).

### Problema de negocio
Un fallo informático en un entorno de salud es un riesgo clínico. El hospital privado requiere un núcleo clínico y operativo con trazabilidad inalterable, retención de expedientes y clasificación de urgencias conforme a norma.

### Alcance general
- Dentro: expedientes clínicos y recetas; triage y urgencias; módulos de diagnóstico (laboratorio e imagenología); portal del paciente; admisión y agenda; capa de seguridad e inteligencia (accesos, estadística, reportes).
- Fuera: facturación electrónica, tabuladores de aseguradoras, inventarios de farmacia.
- Web interna: navegadores estándar sobre Windows en red privada del hospital. Móvil: app Android nativa para pacientes.

### Stakeholders
| Rol | Responsabilidad | Nivel de acceso |
|---|---|---|
| Product Owner | Requisitos, aprueba despliegues y rollbacks | admin |
| Desarrollo | Arquitectura, BD, peer review | editor |
| Seguridad (Sec) | Controles, incidentes | editor |
| DevOps | Infraestructura, despliegues, backups | editor |
| QA | Pruebas | editor |
| Auditor Interno | Auditoría independiente (solo lectura de evidencia) | viewer |
| Médico / Enfermería / Recepción / Laboratorista | Usuarios clínicos y operativos (web) | por rol (Tabla 44) |
| Paciente | Consulta de solo lectura (móvil) | viewer |

---

## TECHNICAL CONTEXT

### Stack tecnológico
```
Frontend web:  Vue.js (SFC, MVVM) — SPA, PascalCase para componentes
Backend:       Laravel (LTS vigente) — API REST /api/v1, Repository Pattern, Spatie Permission (SUP-011)
Móvil:         Kotlin nativo Android (minSdk 29), Jetpack Compose, MVVM
Base de datos: Supabase (PostgreSQL 15+) AUTOHOSPEDADO en Docker (ver AMB-001); Laravel es el ÚNICO cliente
Auth:          JWT RS256 15 min + refresh rotativo; OAuth 2.0 PKCE en móvil; MFA para roles altos
Hosting:       Docker en servidor del hospital; Rolling Update / Blue-Green
CI/CD:         Pipeline con secret-scan, SAST (SonarQube), SCA, DAST, SBOM
```

### Arquitectura recomendada
Cliente-Servidor de 3 capas (Presentación, Lógica de Negocio, Datos) sobre API RESTful única. La API es compartida por web y móvil; el móvil usa el namespace `/api/v1/mobile/*` (solo lectura para el paciente) y el web usa `/api/v1/*`. Detalle: `ARCHITECTURE.md`.

### Estructura de carpetas del paquete
```
sih-hospitalario/
├── web/     ← API Laravel + migraciones Supabase + SPA Vue (repos hijos: backend/, frontend/)
└── mobile/  ← app Kotlin (repo propio)
```

### Librerías clave sugeridas
```
Auth web:      spatie/laravel-permission — RBAC (citado en RF-003)
Tiempo real:   Laravel Reverb — alertas sin recarga (SUP-006)
Móvil red:     Retrofit + OkHttp (certificate pinning)
Móvil seguro:  AndroidX Security (EncryptedSharedPreferences) + Android Keystore
FHIR:          Validador de esquema FHIR R4 en CI
Pruebas:       PHPUnit, Playwright, axe-core, k6, Newman; JUnit/Espresso/Compose UI tests en móvil
```

---

## CONSTRAINTS

### Técnicas
- Frontend y app NUNCA se conectan a la BD ni usan SDK de Supabase; solo la API.
- Todas las marcas de tiempo en UTC (timestamptz). TLS >= 1.2 en el 100% de las conexiones (MS-01).
- Sin `latest` en imágenes ni dependencias; secretos solo en bóveda; sin hardcoding.
- 401 (sin sesión/token inválido) vs 403 (sin permisos o acceso a otro UUID).

### De negocio
- El sistema es apoyo a la decisión clínica; no sustituye la valoración del personal.
- Ningún rol puede eliminar información clínica; correcciones por nota aclaratoria.
- Administrador gestiona usuarios y auditoría, sin acceso a contenido clínico.

### Operativas
- Uptime >= 99.9% mensual; P95 ECE <= 2.0 s; RTO contenedor <= 5 min; RTO desastre <= 4 h; MTTR <= 30 min.
- Desarrolladores sin acceso a datos productivos; entornos no productivos solo con datos sintéticos.

| Norma | Invariantes obligatorios |
|---|---|
| NOM-004-SSA3-2012 | Retención >= 5 años desde `fecha_ultimo_acto_medico`; sin DELETE físico (`estado_activo`); notas cerradas inmutables |
| NOM-024-SSA3-2012 | AUDITORIA_I INSERT-only; hash SHA-256 encadenado; trazabilidad absoluta de accesos |
| NOM-206-SSA1-2002 | Manchester: Rojo 0 / Naranja 15 / Amarillo 60 / Verde 120 / Azul 240 min; AVDI=Inconsciente → Rojo; SLA vencido alerta a Jefatura |
| NOM-035-SSA3-2012 | Dashboards disociados (sin PII) con identificador CLUES |
| COFEPRIS / SAT | Fracciones I, II, III exigen e.firma X.509 validada; QR único invalidado tras surtido |
| HL7 FHIR R4 | Patient, Encounter, Observation, MedicationRequest en JSON |
| LFPDPPP | Consentimiento inmutable con hash; ARCO 20/15 días; anonimización parcial en salas de espera |
| ISO/IEC 27001 | TLS 1.2+; AES-256; Bcrypt >= 12; sesión inactiva 15 min web / 5 min app; respaldos 3-2-1 con WAL |
| OWASP ASVS L2 | 401 sin sesión vs 403 sin permisos o acceso a otro UUID |
| CENAM / NTP | Deriva <= 1 s; fechas en UTC |
| ISO/IEC 25010 | P95 ECE <= 2.0 s; uptime >= 99.9%; RTO contenedor <= 5 min; MTTR <= 30 min; cobertura >= 80% |
| WCAG 2.1 AA | Contraste 4.5:1; triage nunca solo color; 100% operable por teclado en captura clínica |

---

## CODING STANDARDS

### Nomenclatura
```
Componentes Vue / clases / actividades Kotlin:  PascalCase   (TriageForm.vue, ExpedienteClinicoActivity)
Variables, funciones, métodos (JS/PHP/Kotlin):  camelCase    (pacienteActivo, calcularTriage())
Tablas y columnas PostgreSQL:                   snake_case   (signos_vitales, id_paciente)
Constantes y variables de entorno:              SCREAMING_SNAKE_CASE (MAX_TIEMPO_ESPERA)
Rutas API:                                      kebab-case   (/api/v1/expedientes-clinicos); excepción FHIR (SUP-010)
```

### Estructura de repositorios y ramas (§12.2)
Ver directrices generales en [AGENTS.md general](../../AGENTS.md).
Ramas principales: `main` (producción protegida, prohibido commit directo), `dev` (integración y desarrollo base), ramificaciones de trabajo (`feature/<alcance>-<tarea>`, `bugfix/<alcance>-<tarea>`).
Todo merge se realiza hacia `dev` mediante PR con >= 1 aprobación de revisor distinto al autor. Versionado SemVer `vX.Y.Z`.

### Modularidad
- MVVM en cliente; Repository Pattern en backend; sin dependencias circulares; YAGNI; funciones breves de un solo nivel de abstracción.
- Sin secretos ni datos reales de pacientes en commits, tickets o reportes.

---

## EXECUTION PRIORITIES

### P0 — Crítico (bloquea lanzamiento)
| ID | Etapa | Descripción |
|---|---|---|
| PROV-SEC-001 | 5 | Autenticación del paciente y sesión móvil (porción móvil) |
| PROV-SEC-002 | 5 | OAuth 2.0 con PKCE y manejo de tokens (porción móvil) |
| RF-002 | 5 | Rol PACIENTE: acceso móvil (porción móvil de RF-002) |
| RF-002.1 | 5 | Aislamiento horizontal: la app nunca envía id_paciente (porción móvil) |
| RF-003 | 5 | Manejo cliente de 401 y 403 (porción móvil) |
| RF-011 | 5 | Acceso de solo lectura en aplicación móvil |
| RF-009 | 5 | Consulta de recetas (porción móvil) |
| RF-010 | 5 | Consulta de resultados (porción móvil) |
| RNF-003 | 5 | Cifrado en tránsito y fijación de certificados |
| RNF-007 | 5 | Consumo de recursos HL7 FHIR R4 |

### P1 — Importante (siguiente sprint)
| ID | Etapa | Descripción |
|---|---|---|
| RNF-006 | 5 | Compatibilidad Android 10 (API 29) o superior |
| PROV-SEC-011 | 5 | Endurecimiento de la aplicación móvil |
| PROV-RN-008 | 5 | Aviso de privacidad y consentimiento en la app (porción móvil) |
| PROV-RN-009 | 5 | Solicitud de derechos ARCO desde la app (porción móvil) |
| PROV-RF-002 | 5 | Consulta de citas (porción móvil) |
| PROV-UX-003 | 5 | Accesibilidad móvil WCAG 2.1 AA (porción móvil) |
| PROV-UI-003 | 5 | Portal móvil: pantallas (PB-5.1) |
| PROV-RNF-005 | 6 | Firma, versionado y distribución de la app |

### P2 — Complementario (backlog)
| ID | Etapa | Descripción |
|---|---|---|


---

## DELIVERABLES MAP

| Área | Entregable | Archivo de referencia |
|---|---|---|
| Frontend / UI | Pantallas y estados | `UI_UX_SPEC.md` |
| Backend / API | Módulos, endpoints, seguridad | `ARCHITECTURE.md` |
| Base de datos | Entidades, constraints, enums | `schemas/DOMAIN_SCHEMA.json` |
| Pruebas | GWT por requerimiento y evidencia AI-0x | `REQUIREMENTS_EXECUTION.md` |
| Tareas | Backlog ejecutable con verificación máquina | `tasks/TASK_BACKLOG.json` |

---

## AUDITORÍA (Agentes Auditores)

| Stage | Sprint | Fechas 2026 | Épica | Checkpoint |
|---|---|---|---|---|
| 1 | 1 | 09 sep–22 sep | E1 | AI-01 Documental y requisitos |
| 2 | 2 | 23 sep–08 oct | E2 | AI-02 Infraestructura y trazabilidad |
| 3 | 3 | 09 oct–22 oct | E3 | AI-03 Triage y urgencias |
| 4 | 4 | 23 oct–05 nov | E4 | AI-04 Cumplimiento clínico e interoperabilidad |
| 5 | 5 | 06 nov–19 nov | E5 + E6 | AI-05 Seguridad de la información |
| 6 | Release Final | 20 nov–25 nov | E6 | AI-06 Liberación (Quality Gate) |

Cada tarea de `TASK_BACKLOG.json` lleva `sprint`, `stage`, `epic_id`, `normative_refs`, `risk_mitigated` (R-01..R-11), `quality_metrics`, `audit_checkpoint` y `verification_assertions` (formato `SQL:|HTTP:|SHELL: <acción> => <esperado>`).

Métricas homologadas a Tabla 50: MC-08=MS-01, MC-09=MS-02, MP-07=MS-03, MN-07=MS-04, MN-08=MS-05.

Riesgos: R-01 clasificación errónea de triage · R-02 acceso no autorizado al ECE · R-03 alteración de auditoría · R-04 indisponibilidad · R-05 pérdida/ransomware · R-06 vulnerabilidades de terceros · R-07 intercepción en tránsito · R-08 datos reales en no productivo · R-09 caída del servicio de e.firma · R-10 cambios normativos · R-11 concentración de conocimiento.

---

## INVENTARIO

Requerimientos en esta carpeta: **18** (9 con ID provisional PROV-).

## AMBIGUITY_REPORT

### ⚠️ Ambigüedades (requieren confirmación antes de implementar)
| ID | Sección origen | Descripción del problema | Acción requerida |
|---|---|---|---|
| AMB-001 | §3.4, §9.2, §12.3, RNF-003 | El stack pide Supabase, pero el ERS exige BD PostgreSQL en contenedor Docker aislado, servidor de producción en el hospital y sin conexiones desde internet público. Supabase Cloud contradice esto; Supabase autohospedado en Docker lo cumple. | Confirmar autohospedado (recomendado) vs Cloud. Bloquea TASK-W-021 (Stage 2, AI-02). |
| AMB-002 | RF-008 (Tabla 13) | RF-008 cita sub-requisitos RF-009.1, .2 y .3, pero el documento solo define RF-008.1 y RF-008.2 (2, no 3). RF-009 es Recetas. | Confirmar si existe un tercer sub-requisito (¿consentimiento/cierre?). Se asumió solo RF-008.1 y RF-008.2. |
| AMB-003 | Tabla 44, RF-008.1 | La matriz de permisos por rol llegó desordenada en la fuente. Además RF-008.1 menciona rol 'Administración' con acceso a crear expediente, mientras Tabla 44 niega al Administrador acceso clínico. | Entregar la Tabla 44 legible y aclarar quién crea expedientes. Bloquea cierre de TASK-W-010. |
| AMB-004 | Tabla 26 vs Tabla 46, RNF-001 | (a) AUDITORIA_I.id_usuario es VARCHAR(10) FK, pero Tabla 46 exige también actores de integración y pacientes y fallos de login con usuario inexistente. (b) id_auditoria es VARCHAR(10) en Tabla 26 pero 'consecutivo asignado por la BD' en Tabla 46. (c) Tabla 46 pide rol activo, que no tiene columna entre las 13 definidas. (d) tipo_movimiento VARCHAR(25) solo lista INSERT/UPDATE/SOFT_DELETE; Tabla 45 exige un catálogo de eventos mayor. | Decidir. Adoptado en SUP-009. |
| AMB-005 | Tabla Maestra NOM-004 vs §14.8 | Se prohíbe DELETE físico, pero la disposición final exige supresión segura tras 5 años y bloqueo. | Definir procedimiento de supresión (rol DBA bajo control de cambios). No implementado en este alcance. |
| AMB-006 | §8.B, Tabla 25, TC-TRG-001 | No hay rangos fisiológicos, umbrales críticos de presión (sistólica/diastólica), categorías de 'estimación de sangre perdida', ni tabla de decisión Manchester numérica. TC-TRG-001 usa SpO2, que no se captura. La frecuencia cardíaca ('20 rpm') está ilegible. | El PO debe entregar la tabla de decisión clínica (MC-01 = 100% la requiere). Bloquea cierre de TASK-W-042. |
| AMB-007 | RF-004 / §8.B vs Tabla 26 | EVA: el ERS la define como menú de 5 opciones (sin dolor…insoportable), pero Tabla 26 la define como INTEGER 0 a 10. | Confirmar mapeo opción→entero. Provisional: BD guarda SMALLINT 0–10 y la UI muestra 5 opciones con mapeo configurable. |
| AMB-008 | §8.2, RF-007, PROV-RN-006 | 'Usuario con niveles de privilegio superiores' no está definido (¿MEDICO, jefatura?). Tampoco quién puede captura retroactiva. | Definir roles. Provisional: MEDICO y jefatura de urgencias. |
| AMB-009 | Anexo G, RF-004, RF-009 | El Anexo G (mockups web) es solo imagen; no hay prototipo móvil. | Entregar los mockups legibles o confirmar diseño libre. |
| AMB-010 | Todo el ERS | No existe guía de marca: paleta, tipografía, espaciado, iconografía y motion no están definidos. | El PO debe decidir. BRAND_SYSTEM.md marca NO_DEFINIDO. |
| AMB-011 | RF-009, §17.3.b | No se define el límite de cantidad por evento para Fracciones II y III ni quién consume el QR (surtido; farmacia está fuera de alcance). | Definir límites y actor de surtido (integración externa). |
| AMB-012 | RF-010 | No hay tamaño máximo ni tipos de archivo permitidos para resultados. | Definir límites. |
| AMB-013 | RNF-007 vs convención | FHIR exige rutas /Patient, /Encounter (PascalCase); el ERS exige kebab-case en endpoints. | Adoptado en SUP-010. |
| AMB-014 | §14.6 | No se define calendario de días hábiles. | Definir calendario oficial. |
| AMB-015 | §16.3 | No se define duración del acceso de emergencia. | Definir. Provisional: 60 min. |
| AMB-016 | RF-011 vs §14, §15 | RF-011 exige que la API rechace POST/PUT/DELETE del token del paciente, pero la app también debe hacer login, refresh, logout, OTP, consentimiento y ARCO (todos POST). | Confirmar lista blanca (SUP-002). |
| AMB-017 | PROV-RF-001, MN-09 | No se define frecuencia, formato de exportación ni umbral mínimo de agregación de NOM-035. | Definir con el PO. |
| AMB-018 | §3.1, RF-002, RF-011 | 'Agenda y citas' y 'aseguradora' están en el alcance, pero no hay RF formal ni campos en Tabla 26 (solo seguro_activo). | Confirmar alcance. Campos derivados marcados. |
| AMB-019 | Tabla 31, Tabla 32 | La matriz de calor llegó ilegible; los valores P×I de riesgos se tomaron de Tabla 32 (R-01..R-11). | Verificar con documento original. |
| AMB-020 | IDs en narrativa | Referencias cruzadas inconsistentes además de las 3 corregidas por el usuario: RF-012 (firma de recetas, Tabla 33/40/43) se interpretó RF-009; RF-010 para QR (Tabla 43) como RF-009; RF-014 (MS-05) como RF-002; RF-011 (MN-05, ECE) como RF-008; RF-004/RF-006 (diagramas UML §9.5) como RF-003/RF-005; RF-009.1 (cédula) como RF-008.1. | Confirmar normalización. Ver SUP-003. |

### 💡 Supuestos adoptados (no bloquean implementación)
| ID | Base del supuesto | Decisión adoptada | Riesgo si es incorrecto |
|---|---|---|---|
| SUP-001 | Directiva del usuario: carpetas web y mobile; ERS §12.2 pide Multirepo | web/ contiene la API Laravel + migraciones Supabase y el frontend Vue (dos repos hijos: backend y frontend); mobile/ contiene la app Kotlin. La API es una sola y vive en web/. | Si se prefiere un repo de API independiente, mover TASK-W de backend/BD sin cambiar contratos. |
| SUP-002 | RF-011 + AMB-016 | Escrituras permitidas al token de paciente (lista blanca): /api/v1/mobile/auth/{token,otp-verify,refresh,logout}, /api/v1/mobile/consentimientos, /api/v1/mobile/solicitudes-arco. Todo recurso clínico es solo lectura. | Si RF-011 se interpreta literal, consentimiento y ARCO tendrían que hacerse en admisión. |
| SUP-003 | Normalización pedida por el usuario | Mapeos aplicados: 'RF-006 clasificación automática'→RF-005; 'RF-008 reclasificación manual'→RF-007; 'RF-012 portal móvil'→RF-011. Métricas: MC-08=MS-01, MC-09=MS-02, MP-07=MS-03, MN-07=MS-04, MN-08=MS-05. Mapeos adicionales: AMB-020. | Errores de trazabilidad en auditorías. |
| SUP-004 | Skill: prioridad por defecto | Requerimientos sin ID formal reciben prefijo PROV- y prioridad P1, elevada a P0 solo si la sección fuente los marca bloqueantes de liberación o cierre de riesgo alto (Tabla 41, Quality Gate, RF Alta). 'Alta/Crítica'→P0; 'Media'→P1. | Reordenar sprints. |
| SUP-005 | §16.4 | MFA por TOTP (app autenticadora). El ERS exige MFA, no el método. | Cambiar a OTP por correo o hardware key. |
| SUP-006 | RF-005, RF-006 | Tiempo real vía Laravel Reverb (WebSocket) con fallback a polling de 5 s. Supabase Realtime y SDK de Supabase NO se usan desde Vue/Kotlin (el frontend no se conecta a la BD). | Reemplazar por otro bus de eventos. |
| SUP-007 | §15.2 | Detección de root/Magisk con librería de integridad mantenida (p. ej., Play Integrity API más comprobaciones locales). | Falsos positivos. |
| SUP-008 | RNF-003, ASVS L2 | Sesión web con refresh token en cookie HttpOnly+Secure+SameSite=Strict y access token en memoria (no localStorage). | Ajustar a CSRF/XSS. |
| SUP-009 | AMB-004 | AUDITORIA_I con las 13 columnas definidas por el usuario (Tabla 46): fecha_hora timestamptz UTC; id_auditoria BIGINT identity; id_usuario VARCHAR(36) sin FK estricta (admite usuario, paciente o integración); origen guarda JSON {ip, estacion_id, rol_activo, client}; tipo_movimiento VARCHAR(40) con catálogo de Tabla 45. Huella: SHA-256 de campos canónicos concatenados con la huella previa (génesis: 64 ceros). | Cambia esquema y verificación de cadena. |
| SUP-010 | AMB-013 | Rutas FHIR bajo /api/v1/fhir/{Patient|Encounter|Observation|MedicationRequest} en PascalCase; el resto en kebab-case. | Prueba de convención fallida. |
| SUP-011 | Skill: stack | Backend Laravel (no Spring), versión LTS vigente, Spatie Permission (citado en RF-003), JWT RS256 sobre servicio propio o Passport. Supabase = PostgreSQL 15+ autohospedado; Laravel es su único cliente. | Elección de framework. |
| SUP-012 | Etapa 1 vs 2 | Etapa 1 usa BD local de desarrollo (Docker) porque la infraestructura endurecida llega en Etapa 2. | Rehacer migraciones. |
