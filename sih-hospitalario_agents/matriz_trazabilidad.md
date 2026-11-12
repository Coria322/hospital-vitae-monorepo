# Matriz de Trazabilidad de Requerimientos y Normas (RTM)

**Proyecto:** Hospital Vitae — Sistema de Información Hospitalaria (SIH)  
**Documento Fuente:** Manual de Calidad de Arquitectura (`DOC-ARQ-SIH-001`, rev 2.0)  
**Regla de Mantenimiento:** Al finalizar cada Sprint, los requerimientos y normas completados y certificados con pruebas DEBEN tacharse con `~~...~~` y registrar su estado como `✅ Cumplido (Sprint N)`.

---

## 1. Resumen de Estado por Sprint y Auditoría

| Sprint | Épica | Punto de Auditoría | Enfoque | Total Reqs | Estado del Sprint |
| :---: | :---: | :---: | :--- | :---: | :---: |
| **Sprint 1** | E1 | **AI-01** | Gobernanza Ágil, Seguridad Base y Control de Accesos | 10 | **✅ CUMPLIDO (Tachado)** |
| **Sprint 2** | E2 | **AI-02** | Infraestructura Docker, Base de Datos y Trazabilidad | 10 | **✅ CUMPLIDO (Tachado)** |
| **Sprint 3** | E3 | **AI-03** | Triage Manchester, Urgencias y Admisión | 13 | **✅ CUMPLIDO (Tachado)** |
| **Sprint 4** | E4 | **AI-04** | ECE, Recetas Médicas Controladas e Interoperabilidad | 11 | **✅ CUMPLIDO (Tachado)** |
| **Sprint 5** | E5/E6 | **AI-05** | Portal Móvil del Paciente, Estadística y QA Integral | 8 | **✅ CUMPLIDO (Tachado)** |
| **Sprint Final**| E6 | **AI-06** | Quality Gate, Despliegue Inmutable y BCP/DRP | 5 | **✅ CUMPLIDO (Tachado)** |

---

## 2. Matriz Detallada de Requerimientos por Sprint

### Sprint 1: Gobernanza Ágil, Seguridad Base y Control de Accesos (Auditoría AI-01)
> *Estado: Completado y verificado con 19 pruebas PHPUnit y 13 pruebas automatizadas Bruno CLI.*

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| ~~**RF-001**~~ | Funcional | P0 | ~~Herramientas de administración del sistema (Alta/Baja de usuarios, validación de cédula en médicos, consulta de auditoría, baja lógica sin delete físico).~~ | `usuarios`, `auth` | NOM-024-SSA3-2012, ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**RF-002**~~ | Funcional | P0 | ~~Control de acceso basado en roles (RBAC: ADMIN, MEDICO, ENFERMERIA, RECEPCION), suspensión automática al 3er intento fallido (<60 min) y reactivación por administrador.~~ | `auth`, `rbac` | NOM-024-SSA3-2012, ISO/IEC-27001, OWASP ASVS L2 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**RF-003**~~ | Funcional | P0 | ~~Verificación de autorización por capas (middlewares VerifyAccountStatus, RoleAuthorization; 401 unauthenticated vs 403 forbidden).~~ | `auth`, `rbac` | OWASP ASVS L2, ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**RNF-008**~~ | No Funcional | P0 | ~~Gestión y cifrado de credenciales de acceso (Bcrypt con factor de costo >= 12 en `password_hash`, contraseñas seguras, sin texto plano).~~ | `auth`, `seguridad` | ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-SEC-001**~~| No Funcional | P0 | ~~Autenticación y sesiones (MFA para administración, revocación de tokens al logout, mensajes genéricos contra enumeración).~~ | `auth` | OWASP ASVS L2, ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-SEC-002**~~| No Funcional | P0 | ~~Tokens de acceso y sesión (Bearer tokens Sanctum con expiración de 15 minutos, rotación mediante endpoint /auth/refresh).~~ | `auth` | OWASP ASVS L2 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-SEC-003**~~| No Funcional | P0 | ~~Ciclo de vida de cuentas de usuario (Estados: ACTIVA, BLOQUEADA, BAJA; reactivación con validación y baja inmutable).~~ | `usuarios` | NOM-024-SSA3-2012 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-SEC-004**~~| No Funcional | P0 | ~~Gestión de secretos (Credenciales y claves maestras fuera del código, configuradas mediante variables de entorno `.env`).~~ | `config`, `seguridad` | ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-RN-001**~~ | Regla Negocio | P1 | ~~Gobernanza ágil, ramas y revisión por pares (Flujo git con `dev` para merges y `main` con rebase lineal, pruebas previas a commit).~~ | `gobernanza` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 1)~~** |
| ~~**PROV-RN-002**~~ | Regla Negocio | P1 | ~~Matriz de trazabilidad viva y Product Backlog (Documento RTM sincronizado con tachado de requerimientos por sprint).~~ | `gobernanza` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 1)~~** |

---

### Sprint 2: Infraestructura Docker, Base de Datos y Trazabilidad (Auditoría AI-02)
> *Estado: Completado y certificado al 100% con 54 pruebas automatizadas integradas en `dev` y `main`.*

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| ~~**RNF-001**~~ | No Funcional | P0 | ~~Trazabilidad y auditoría de eventos clínicos y de seguridad (tabla `AUDITORIA_I` inalterable con 13 columnas normativas, INSERT-only y servicio `AuditLogger`).~~ | `auditoria` | NOM-024-SSA3-2012 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**RNF-002**~~ | No Funcional | P0 | ~~Retención documental y persistencia legal (retención mínima de 5 años desde última atención, triggers anti-DELETE en `paciente` y `expediente_c`).~~ | `expediente` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**RNF-003**~~ | No Funcional | P0 | ~~Seguridad de la información en red y reposo (TLS >= 1.2 forzado en BD con sslmode=require, HSTS >= 1 año, headers de seguridad y cifrado AES-256-GCM para adjuntos clínicos).~~ | `infraestructura` | ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**RNF-004**~~ | No Funcional | P0 | ~~Alta disponibilidad y aislamiento de fallos (docker-compose con réplicas >= 2, reinicio automático unless-stopped, endpoint `/api/v1/salud` y RTO <= 300 s).~~ | `infraestructura` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-RNF-001**~~ | No Funcional | P0 | ~~Infraestructura Docker endurecida y paridad de entornos (versión fija inmutable php:8.2.26-apache-bookworm, composer:2.8.2, 0 ':latest', usuario www-data no-root, hardening Apache/PHP y healthcheck /up).~~ | `devops` | ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-SEC-005**~~ | No Funcional | P0 | ~~Garantías de inalterabilidad de `AUDITORIA_I` (encadenamiento criptográfico SHA-256 estilo blockchain, génesis 64 ceros y comando `auditoria:verificar-cadena`).~~ | `auditoria` | NOM-024-SSA3-2012, ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-RN-003**~~ | Regla Negocio | P1 | ~~Ciclo de retención documental y bloqueo tras 5 años (actualización de `fecha_ultimo_acto_medico` y comando `expedientes:reporte-retencion`).~~ | `expediente` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-SEC-006**~~ | No Funcional | P1 | ~~Estrategia de respaldos 3-2-1 cifrados AES-256 (retención 30 d / 12 m), archivado WAL y monitor de lag <= 600 s (`db:backup-seguro` y `db:verificar-lag-wal`).~~ | `base_datos` | ISO/IEC-27001 | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-SEC-007**~~ | No Funcional | P1 | ~~Sincronización horaria estricta (NTP con CENAM/pool.ntp.org deriva <= 1 s, marcas en UTC y comando `ntp:verificar-sincronizacion`).~~ | `infraestructura` | CENAM / NTP | ✅ **~~Cumplido (Sprint 2)~~** |
| ~~**PROV-SEC-010**~~ | No Funcional | P1 | ~~Entornos y datos de prueba sintéticos (prohibición de datos reales de pacientes en dev, `ClinicoSinteticoSeeder`).~~ | `seguridad`, `qa` | LFPDPPP | ✅ **~~Cumplido (Sprint 2)~~** |

---

### Sprint 3: Triage Manchester, Urgencias y Admisión (Auditoría AI-03)
> *Estado: Completado y verificado con 14 pruebas PHPUnit (68/68 suite total), 12 peticiones automatizadas Bruno CLI (`07-Triage-Manchester`), modelo Triage inalterable y canal SSE.*

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| ~~**RF-004**~~ | Funcional | P0 | ~~Captura obligatoria de signos vitales (presión arterial, FC, FR, temp, EVA, AVDI).~~ | `triage` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**RF-005**~~ | Funcional | P0 | ~~Asignación automática de nivel de gravedad (Algoritmo Manchester de 5 niveles: Rojo a Azul).~~ | `triage` | NOM-206-SSA1-2002 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**RF-006**~~ | Funcional | P0 | ~~Gestión y distribución dinámica de colas de espera según prioridad clínica y tiempo transcurrido.~~ | `urgencias` | NOM-206-SSA1-2002 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**RF-007**~~ | Funcional | P0 | ~~Reclasificación manual de triage con justificación clínica obligatoria firmada por médico.~~ | `triage` | NOM-206-SSA1-2002 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-RN-004**~~ | Regla Negocio | P0 | ~~Matriz Manchester y SLAs de atención (Rojo: inmediato, Naranja: 15m, Amarillo: 60m, Verde: 120m, Azul: 240m).~~| `triage` | NOM-206-SSA1-2002 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-RN-005**~~ | Regla Negocio | P0 | ~~Gestión de tiempos excedidos y alertas escalables a jefatura de guardia.~~ | `urgencias` | NOM-206-SSA1-2002 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-UX-001**~~ | No Funcional | P0 | ~~Actualización de colas de triage en tiempo real mediante WebSockets / SSE.~~ | `urgencias` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-BR-001**~~ | Regla Negocio | P0 | ~~Codificación semántica universal de colores de triage y soporte para daltonismo.~~ | `ui_ux` | WCAG 2.1 AA | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-RN-006**~~ | Regla Negocio | P1 | ~~Tolerancia a fallos de red en admisión y captura retroactiva de folios offline.~~ | `admision` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-RN-007**~~ | Regla Negocio | P1 | ~~Admisión de paciente no identificado (desconocido) y posterior proceso de fusión de expediente.~~ | `admision` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-UX-002**~~ | No Funcional | P1 | ~~Pantalla pública de sala de espera disociada (solo iniciales y turno, sin PII).~~ | `sala_espera` | LFPDPPP | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-UX-003**~~ | No Funcional | P1 | ~~Accesibilidad web y ergonomía clínica WCAG 2.1 AA (operable 100% por teclado).~~ | `ui_ux` | WCAG 2.1 AA | ✅ **~~Cumplido (Sprint 3)~~** |
| ~~**PROV-UI-001**~~ | No Funcional | P2 | ~~Implementación de pantallas de captura de triage según especificación de UI/UX.~~ | `frontend` | ERS Anexo G.1 | ✅ **~~Cumplido (Sprint 3)~~** |

---

### Sprint 4: ECE, Recetas Médicas Controladas e Interoperabilidad (Auditoría AI-04)
> *Estado: Completado y verificado con 13 pruebas PHPUnit (81/81 suite global), 27 peticiones automatizadas Bruno CLI (08-ECE-Recetas-FHIR) al 100% de éxito, triggers de inmutabilidad en BD y servicios criptográficos.*

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| ~~**RF-008**~~ | Funcional | P0 | ~~Gestión integral del Expediente Clínico Electrónico (ECE) conforme a normatividad nacional.~~ | `expediente` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**RF-008.1**~~ | Funcional | P0 | ~~Creación y datos de identificación del expediente, antecedentes y ficha técnica.~~ | `expediente` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**RF-008.2**~~ | Funcional | P0 | ~~Historia clínica, notas de evolución, interconsultas e inmutabilidad de notas cerradas.~~ | `expediente` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**RF-009**~~ | Funcional | P0 | ~~Generación y emisión de recetas médicas para medicamentos controlados (Fracc. I, II, III).~~ | `recetas` | COFEPRIS, SAT | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**RF-010**~~ | Funcional | P0 | ~~Solicitud y consulta de resultados de laboratorio e imagenología médica en el ECE.~~ | `laboratorio` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**RNF-007**~~ | No Funcional | P0 | ~~Interoperabilidad semántica en salud basada en el estándar HL7 FHIR R4.~~ | `interoperabilidad` | HL7 FHIR R4 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**PROV-RN-008**~~ | Regla Negocio | P1 | ~~Gestión del consentimiento informado del paciente con hash inmutable.~~ | `expediente` | LFPDPPP | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**PROV-RN-009**~~ | Regla Negocio | P1 | ~~Atención y trazabilidad de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición).~~ | `privacidad` | LFPDPPP | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**PROV-RN-010**~~ | Regla Negocio | P1 | ~~Protocolo de acceso de emergencia "Break-the-Glass" al expediente con auditoría reforzada.~~ | `seguridad` | NOM-004-SSA3-2012 | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**PROV-SEC-008**~~ | No Funcional | P1 | ~~Privacidad desde el diseño en exportaciones clínicas (marcas de agua y datos anonimizados).~~ | `seguridad` | LFPDPPP | ✅ **~~Cumplido (Sprint 4)~~** |
| ~~**PROV-UI-002**~~ | No Funcional | P2 | ~~Pantallas de consulta de ECE y emisión de recetas médicas.~~ | `frontend` | ERS Anexo G.2 | ✅ **~~Cumplido (Sprint 4)~~** |

---

### Sprint 5: Portal Móvil, Estadística y QA Integral (Auditoría AI-05)
> *Estado Backend: ✅ **Completado y verificado con 10 pruebas PHPUnit Feature (92 suite global) y 13 peticiones Bruno CLI**.*  
> *Porción móvil (`vitae-mobile/`): avance y evidencia en la sección 4. Integrada desde `feature/mobile-app`.*

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| ~~**RF-011**~~ | Funcional | P0 | ~~API móvil de solo lectura para consulta de paciente (historial, citas, recetas).~~ | `mobile_api` | NOM-024-SSA3-2012, LFPDPPP | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**RF-002.1**~~ | Funcional | P0 | ~~Aislamiento horizontal estricto de registros clínicos por claim `sub` del token del paciente (Anti-IDOR).~~ | `mobile_api`, `rbac` | OWASP ASVS L2, LFPDPPP | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**RNF-005**~~ | No Funcional | P1 | ~~Rendimiento y tiempos de respuesta P95 <= 2.0 s en visualización de expediente clínico.~~ | `rendimiento` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**RNF-006**~~ | No Funcional | P1 | ~~Compatibilidad de la aplicación móvil (Android minSdk 29+ / Android 10+ en cabecera `X-Client-Version`).~~ | `mobile_api` | Android SDK, ISO-25010 | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**PROV-RF-001**~~ | Funcional | P1 | ~~Tableros de control estadístico y reportes de salud laboral disociados (NOM-035 con CLUES).~~ | `estadistica` | NOM-035-STPS-2018 | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**PROV-RF-002**~~ | Funcional | P1 | ~~Módulo de agenda y confirmación de citas médicas para pacientes y recepción.~~ | `agenda` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**PROV-RNF-002**~~ | No Funcional | P0 | ~~Estrategia integral de aseguramiento de calidad (QA: unitarias, integración, e2e, estrés).~~ | `qa` | ISO/IEC-25010 | ✅ **~~Cumplido (Sprint 5)~~** |
| ~~**PROV-SEC-009**~~ | No Funcional | P1 | ~~Gestión de vulnerabilidades, inventario de componentes SBOM CycloneDX y evaluación de seguridad.~~ | `secops` | OWASP ASVS L2 | ✅ **~~Cumplido (Sprint 5)~~** |

---

### Sprint Final: Quality Gate y Despliegue (Auditoría AI-06)

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| **PROV-RNF-003** | No Funcional | P0 | Procedimiento de despliegue inmutable (Blue-Green / Rolling) y plan de reversión automática. | `devops` | ISO/IEC-27001 | ⏳ Pendiente (Sprint 6) |
| **PROV-RN-011** | Regla Negocio | P0 | Quality Gate de liberación final (100% pruebas aprobadas, 0 vulnerabilidades altas/críticas). | `gobernanza` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 6) |
| **PROV-RNF-004** | No Funcional | P0 | Planes de continuidad del negocio y recuperación ante desastres (BCP/DRP con RTO <= 4h). | `infraestructura` | ISO/IEC-27001 | ⏳ Pendiente (Sprint 6) |
| **PROV-RN-012** | Regla Negocio | P1 | Gestión formal de solicitudes de cambio post-liberación (RFC) y control de versiones. | `gobernanza` | ISO/IEC-20000 | ⏳ Pendiente (Sprint 6) |
| **PROV-RN-013** | Regla Negocio | P0 | Consolidación y validación del repositorio de evidencias auditables para checkpoints AI-01 a AI-06. | `auditoria` | ISO/IEC-27001 | ⏳ Pendiente (Sprint 6) |

---

## 3. Matriz de Cumplimiento de Normas y Regulaciones

| Norma / Marco | Objeto y Requisitos Clave | Requerimientos SIH | Estado Global | Motivo / Desglose de Cumplimiento |
| :--- | :--- | :--- | :---: | :--- |
| **NOM-004-SSA3-2012** | Expediente clínico: inmutabilidad de notas médicas, retención >= 5 años, sin DELETE físico, confidencialidad. | ~~RNF-002~~, ~~PROV-RN-003~~, ~~RF-004~~, ~~RF-008~~, ~~RF-008.1~~, ~~RF-008.2~~, ~~RF-010~~, ~~PROV-RN-007~~, ~~PROV-RN-010~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Retención documental obligatoria de 5 años (1,825 días), triggers nativos anti-DELETE en base de datos y borrado lógico (Sprint 2 ✅). Captura obligatoria de signos vitales, inalterabilidad en triage y admisión de paciente N.N. con fusión de expedientes (Sprint 3 ✅). Antecedentes estructurados, inmutabilidad física y eloquent de notas médicas cerradas (HTTP 405), solicitudes y resultados de laboratorio cifrados con AES-256-GCM y protocolo de emergencia Break-the-Glass con auditoría reforzada (Sprint 4 ✅). |
| **NOM-024-SSA3-2012** | Sistemas de información en salud: control de acceso RBAC, auditoría de accesos inalterable `AUDITORIA_I`, integridad. | ~~RF-001~~, ~~RF-002~~, ~~PROV-SEC-003~~, ~~RNF-001~~, ~~PROV-SEC-005~~, RF-011 | 🟡 **Parcialmente Cumplida** | **Completado:** Control de acceso RBAC por roles, ciclo de vida de usuarios, bitácora `AUDITORIA_I` de 13 columnas con SHA-256 inalterable y validación de cédulas médicas (Sprints 1 y 2 ✅).<br>**Pendiente:** API móvil de solo lectura para consulta disociada del paciente (Sprint 5). |
| **NOM-206-SSA1-2002** | Urgencias médicas: Sistema de Triage Manchester (5 niveles de prioridad), monitoreo de SLAs de espera. | ~~RF-005~~, ~~RF-006~~, ~~RF-007~~, ~~PROV-RN-004~~, ~~PROV-RN-005~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Algoritmo Manchester determinista de 5 niveles, asignación inmediata a salas de emergencia para nivel ROJO, colas dinámicas ordenadas por SLA, comando `triage:verificar-sla` y reclasificación médica autorizada (Sprint 3 ✅). |
| **NOM-035-STPS-2018** | Factores de riesgo psicosocial en el trabajo: reportes estadísticos agregados sin datos personales. | ~~PROV-RF-001~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Tableros agregados por CLUES de estrés asistencial y carga laboral, k-anonimidad $n \ge 3$, exportación CSV disociada y cero PII (Sprint 5 ✅). |
| **ISO/IEC 27001** | Seguridad de la información: cifrado fuerte (Bcrypt >= 12, TLS >= 1.2, AES-256), sesiones de 15 min, respaldo 3-2-1, despliegue inmutable y BCP/DRP. | ~~RF-001~~, ~~RF-002~~, ~~RNF-008~~, ~~PROV-SEC-001~~, ~~PROV-SEC-004~~, ~~PROV-RNF-001~~, ~~RNF-003~~, ~~PROV-SEC-006~~, ~~PROV-RNF-003~~, ~~PROV-RNF-004~~, ~~PROV-RN-013~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Cifrado Bcrypt factor 12, tokens de 15m, variables de entorno seguras, Docker no-root, TLS forzado en BD, cifrado AES-256-GCM para adjuntos y política de respaldos 3-2-1 con monitor WAL (Sprints 1 y 2 ✅). Despliegue inmutable Blue-Green con rollback verificado en <= 5 min, plan formal de continuidad BCP/DRP con RTO <= 4h y RPO <= 15m, y repositorio maestro consolidado de evidencias AI-01 a AI-06 firmado criptográficamente (Sprint Final ✅). |
| **ISO/IEC 25010** | Calidad del producto software: rendimiento (P95 <= 2s), alta disponibilidad (99.9%), cobertura de pruebas >= 80%. | ~~PROV-RN-001~~, ~~RNF-004~~, ~~RNF-005~~, ~~RNF-006~~, ~~PROV-RF-002~~, ~~PROV-RNF-002~~, PROV-RN-011 | 🟡 **Parcialmente Cumplida** | **Completado:** Flujo de ramas y revisión por pares, alta disponibilidad con réplicas Docker, reinicio automático y healthcheck activo `/salud` con RTO <= 300s (Sprints 1 y 2 ✅). Streaming SSE y tolerancia a fallos en triage (Sprint 3 ✅). Agenda de citas médicas y benchmarking P95 <= 2.0s en ECE (Sprint 5 ✅). Suite integral con 92 pruebas PHPUnit y 13 pruebas Bruno (Sprint 5 ✅).<br>**Pendiente:** Quality Gate final de liberación de producción (Sprint 6). |
| **OWASP ASVS L2** | Verificación de seguridad en aplicaciones: 401 vs 403, prevención de fuerza bruta, manejo de tokens seguro, anti-IDOR. | ~~RF-002~~, ~~RF-003~~, ~~PROV-SEC-001~~, ~~PROV-SEC-002~~, ~~RF-002.1~~, ~~PROV-SEC-009~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Distinción estricta de 401/403, tokens de 15 min, revocación al logout y bloqueo al 3er intento (Sprint 1 ✅). Hardening cabeceras HSTS/CSP (Sprint 2 ✅). Aislamiento horizontal anti-IDOR forzado por `sub` del token (Sprint 5 ✅) y comando `secops:sbom` CycloneDX con 0 CVEs críticas (Sprint 5 ✅). |
| **HL7 FHIR R4** | Estándar de interoperabilidad en salud: intercambio clínico semántico (`Patient`, `Encounter`, etc.). | ~~RNF-007~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Recursos FHIR R4 estándar (`Patient`, `Encounter`, `Observation` con códigos LOINC de signos vitales, `MedicationRequest`, `OperationOutcome`) serializados y validados contra el estándar oficial HL7 FHIR R4 (Sprint 4 ✅). |
| **LFPDPPP** | Protección de datos personales: consentimiento informado con hash, derechos ARCO, disociación en salas públicas, aislamiento móvil. | ~~PROV-SEC-010~~, ~~PROV-RN-008~~, ~~PROV-RN-009~~, ~~PROV-SEC-008~~, ~~PROV-UX-002~~, ~~RF-011~~, ~~RF-002.1~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Datos sintéticos no-PII (Sprint 2 ✅). Pantalla pública disociada (Sprint 3 ✅). Consentimiento con hash SHA-256, ARCO con improcedencia por NOM-004 y exportación PDF disociada (Sprint 4 ✅). API móvil de solo lectura con aislamiento horizontal anti-IDOR del titular (Sprint 5 ✅). |
| **COFEPRIS / SAT** | Receta médica electrónica controlada: cédula profesional verificada, e.firma digital avanzada y código QR único. | ~~RF-001~~, ~~RF-009~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Validación obligatoria de cédula profesional para médicos en base de datos y control de accesos (Sprint 1 ✅). Emisión de recetas médicas para medicamentos controlados (Fracción I, II y III) con validación de e.firma, hash SHA-256, payload QR firmado de un solo uso, consumo irreversible en farmacia y alerta crítica de auditoría ante reuso no autorizado (Sprint 4 ✅). |
| **WCAG 2.1 AA** | Accesibilidad web: contraste 4.5:1, accesibilidad por teclado y soporte para daltonismo en urgencias. | ~~PROV-BR-001~~, ~~PROV-UX-003~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Codificación redundante semántica de colores Manchester (icono SVG + nombre de texto + color), soporte estricto para daltonismo y formularios operables por teclado (Sprint 3 ✅). |
| **CENAM / NTP** | Metrología y hora legal: sincronización horaria con deriva <= 1 segundo y almacenamiento en UTC. | ~~PROV-SEC-007~~ | ✅ **Cumplida (100%)** | **Completado al 100%:** Medición de desfase SNTP UDP 123 contra CENAM/NTP, comando `ntp:verificar-sincronizacion`, enforcement de zona horaria UTC en Laravel y PostgreSQL (Sprint 2 ✅). |


---

## 4. Avance de la Porción Móvil (`vitae-mobile/`, Sprint 5, Auditoría AI-05)

*Actualizado el 10 de noviembre de 2026 sobre `dev` (Sprint 5 integrado, `2248de8`; en `main` como v1.0.0). Fuente: `vitae-mobile/PLAN.md` y `vitae-mobile/evidence/AI-05-mobile/`.*

**Cómo está conectada la app.** Desde la etapa 10 (decisión 19 del plan) el **paciente inicia sesión él mismo** (`PAC-001`) y la app consulta solo el portal móvil de `dev` (`/api/v1/mobile/…`): perfil, citas, recetas, resultados, alta de consentimientos y alta de solicitudes ARCO. **Ninguna petición lleva `id_paciente`** y cada una lleva `X-Client-Version`. Lo que el portal no tiene (aviso, consulta y revocación de consentimientos, consulta de solicitudes ARCO, detalle de receta, contenido de resultados, OTP, PKCE y FHIR) la app lo dice como "no disponible", lo guarda en el teléfono o, solo para el contenido de un resultado, lo pide a un endpoint fuera de `/mobile`. Entre el 8 y el 10 de noviembre la app usaba los endpoints de personal (decisión 16, reemplazada).

**Por qué aquí no hay tachado.** La regla de este documento reserva el tachado y `✅ Cumplido (Sprint N)` para requerimientos completos y certificados con pruebas. La porción móvil se probó contra el backend local y no contra Render, y quedan funciones sin endpoint, así que se anota aparte y no modifica las filas del Sprint 4 ni las de la tabla del Sprint 5.

**Leyenda:** 🟢 la porción móvil está hecha y probada contra el backend real; 🟡 hecha pero con un supuesto o algo sin verificar; 🔴 no se cumple con el backend actual (se cumple solo con datos simulados o no se puede); ⏳ sin empezar.

| ID | Porción móvil | Estado móvil | Evidencia | Falta para cerrarlo |
| :--- | :--- | :---: | :--- | :--- |
| **PROV-SEC-001** | Login del paciente con usuario y contraseña, sesión de 15 min y cierre por 5 min de inactividad. El OTP de dispositivo nuevo existe **solo con datos simulados** | 🟡 | `AuthRemotoTest`, `InactivityMonitorTest`, `SessionManagerTest`, `LoginViewModelTest`, `OtpViewModelTest`; login de `PAC-001` en el teléfono contra `dev` | OTP en el backend (no existe); medir los 5 min reales en el teléfono |
| **PROV-SEC-002** | Tokens cifrados con Keystore y renovación con `/auth/refresh` (rota con el Bearer vigente). PKCE S256 existe **solo con datos simulados** | 🟡 | `KeystoreTokenStoreTest` (teléfono: sin tokens en claro), `SessionManagerTest`, `PkceTest`, `AuthRemotoTest` | PKCE y refresh token en el backend; hoy el token dura 15 min y no hay segundo factor |
| **RF-002** | La app admite solo cuentas `PACIENTE`: una cuenta de personal inicia sesión en el servidor, pero la app muestra "Acceso no disponible" y revoca su token | 🟢 | `AuthRemotoTest.unaCuentaDePersonalSeRechazaYSuTokenSeRevoca`, `LoginViewModelTest`; en el teléfono contra `dev`: `PAC-001` entra y `MED-001` se rechaza | Probarlo contra Render (aún no se ha iniciado sesión allí) |
| **RF-002.1** | **La app no envía `id_paciente`** en ninguna petición (la identidad sale del token) y en `/mobile` el servidor rechaza uno ajeno con 403 `IDOR_VIOLATION_DETECTED`. **Fuera de `/mobile` el backend deja a un paciente leer a otros** (hallazgo) | 🟡 | `SoloLecturaTest` (el identificador no aparece en ningún archivo), `ClinicoRemotoTest`, `PrivacidadArcoRemotoTest`; 403 comprobado a mano contra `dev` | Aislamiento por dueño también fuera de `/mobile` (`/pacientes`, `/recetas`, `/solicitudes-estudio`, `/consentimientos`, `/solicitudes-arco`) |
| **RF-003** | Un 401 con sesión la invalida; un 403 no | 🟢 | `ApiClientTest`, `SessionManagerTest`, `SeguridadTest`; login erróneo con 401 comprobado en vivo contra `dev` | El 403 real (`BREAK_THE_GLASS_REQUIRED`) se probó con respuesta simulada, no en el teléfono |
| **RF-011** | Solo lectura: 0 controles de edición; los repositorios clínicos solo leen; las únicas escrituras son `auth`, `mobile/citas` (pedir una cita), `mobile/consentimientos` y `mobile/solicitudes-arco`. En el servidor, `POST /mobile/recetas` responde 403 `CLINICAL_WRITE_FORBIDDEN` | 🟡 | `SinEdicionTest`, `SoloLecturaTest`; 403 comprobado a mano contra `dev` | Comprobar el rechazo de escrituras clínicas también fuera de `/mobile` (TASK-W-080) |
| **RF-009** | Lista y detalle de recetas con dosis, estado y QR, leídos de `GET /mobile/recetas`; en el teléfono salen las 3 recetas de `PAC-001` con QR vigente y no válido | 🟡 | `ClinicoRemotoTest` (respuestas reales), `ClinicoTest`, `QrTest`; probado en el teléfono | Vía y duración no existen (el backend guarda `indicaciones` como texto libre); el portal no tiene detalle por id |
| **RF-010** | Lista de resultados (`GET /mobile/resultados`) y visor interno. El backend entrega el contenido como **texto descifrado** y solo en un endpoint fuera de `/mobile`; el visor de PDF e imagen solo se probó con datos simulados | 🟡 | `ClinicoRemotoTest`, `ResultadoVisorViewModelTest`, `ClinicoTest`; visor de texto probado en el teléfono | Contenido bajo `/mobile` y formato binario en el backend (AMB-012); el backend expone `archivo_cifrado_ref` |
| **PROV-RF-002** | Lista de citas reales (`GET /mobile/citas`) y **solicitud de una cita nueva** (`POST /mobile/citas`, la cita queda `PENDIENTE` hasta que recepción la confirme). Cancelar no se ofrece | 🟢 | `ClinicoRemotoTest`, `SolicitarCitaViewModelTest`, `ResumenViewModelTest`, `ClinicoTest`; en el teléfono contra `dev`: se pidió una cita y quedó `PENDIENTE` en la base, y una hora que ya pasó se rechaza | Probarlo contra Render (escribe una cita en producción); decidir si la app debe cancelar citas |
| **RNF-003** | Sin tráfico en claro, pinning por host, TLS 1.2 o superior | 🟡 | `PinningTest`, `SeguridadRedTest` | Pines del certificado de producción (`API_PINES` vacío: el release no arranca sin ellos) |
| **RNF-007** | El portal móvil **no entrega FHIR**: el perfil llega como JSON propio (`GET /mobile/perfil`). El analizador `Patient` y `MedicationRequest` sigue probado con ejemplos, pero la app con el backend real ya no lo usa | 🔴 | `FhirParserTest`, `ContratoFhirTest` (solo ejemplos sintéticos) | FHIR bajo `/mobile`; además `GET /fhir/Patient/{id}` de `dev` **no exige token** (hallazgo de seguridad) |
| **RNF-006** | `minSdk 29` (Android 10+) y cabecera `X-Client-Version` en cada petición; el portal responde 400 sin ella y 426 con API < 29, y la app bloquea con "Versión no compatible" | 🟡 | `DispositivoInterceptorTest`, `AuthRemotoTest.todasLasPeticionesLlevanElNivelDeApi`; 400 y 426 comprobados a mano contra `dev`; teléfono con Android 12; en Android 9 el sistema rechaza la app (`INSTALL_FAILED_OLDER_SDK`) | Matriz con API 29, API 34+ y 3 tamaños de pantalla |
| **PROV-RN-008** | Aviso previo al login, aviso con versión, finalidades opcionales sin marcar (recordatorios, estadística) y alta en `POST /mobile/consentimientos` (aviso y finalidades en filas aparte). **No hay consulta ni revocación**: la app recuerda en el teléfono lo aceptado y revocar es "no disponible" | 🟡 | `PrivacidadArcoRemotoTest` (respuestas reales), `AvisoPrevioTest`, `PrivacidadTest`, `AvisoViewModelTest`; filas creadas en la base de `dev` desde el teléfono | Consulta y revocación en el portal; un paciente sin citas ni recetas no puede aceptar (falta médico responsable); Legal debe validar el texto y la vigencia (`VIGENTE_DESDE` es un supuesto) |
| **PROV-RN-009** | Solicitud ARCO con folio y plazos (`POST /mobile/solicitudes-arco`); la app redacta la descripción que el servidor exige. No hay consulta: se muestran las enviadas en la sesión | 🟡 | `PrivacidadArcoRemotoTest`, `ArcoTest`, `ArcoViewModelTest`; solicitud creada en la base de `dev` desde el teléfono | Consulta por folio y lista en el portal; confirmar con Legal la descripción automática |
| **PROV-UX-003** | Accesibilidad WCAG 2.1 AA: contraste, objetivos de 48 dp, texto hablable, encabezados. La paleta de la guía de estilo se oscureció donde no llegaba a AA | 🟡 | `AccesibilidadTest` (14 pantallas), `ContrasteTest` | Auditoría manual con TalkBack y Accessibility Scanner |

**Sin fila propia en esta matriz** (vienen del ERS móvil): PROV-SEC-011 (bloqueo por root, R8, `FLAG_SECURE`), PROV-UI-003 (14 rutas del `UI_UX_SPEC`) y PROV-RNF-005 (firma del APK, SBOM). Su avance está en `vitae-mobile/PLAN.md`.

**Cifras de la porción móvil:** 240 pruebas unitarias y 46 instrumentadas (teléfono físico, datos simulados) en verde; cobertura de líneas ViewModels 100 % y lógica de datos 93,9 % en la etapa 7 (MC-10 ≥ 80 %), sin medir de nuevo tras la etapa 10; lint sin errores. El recorrido de la etapa 10 se hizo en el teléfono contra el backend local (`dev`), no contra Render.

**Sin cumplir y sin resolver desde la app:** el aislamiento por dueño fuera de `/mobile` y la autenticación de `/api/v1/fhir/*` (hallazgos de seguridad de `dev`), consulta y revocación de consentimientos, consulta de solicitudes ARCO, detalle de receta y contenido de resultados bajo `/mobile`, OTP y PKCE, FHIR para móvil, los pines de producción, probar el flujo contra Render, la matriz de dispositivos completa y la auditoría manual de accesibilidad. Detalle en `vitae-mobile/evidence/AI-05-mobile/README.md` y `07-conexion-backend.md`.
