# Matriz de Trazabilidad de Requerimientos y Normas (RTM)

**Proyecto:** Hospital Vitae — Sistema de Información Hospitalaria (SIH)  
**Documento Fuente:** Manual de Calidad de Arquitectura (`DOC-ARQ-SIH-001`, rev 2.0)  
**Regla de Mantenimiento:** Al finalizar cada Sprint, los requerimientos y normas completados y certificados con pruebas DEBEN tacharse con `~~...~~` y registrar su estado como `✅ Cumplido (Sprint N)`.

---

## 1. Resumen de Estado por Sprint y Auditoría

| Sprint | Épica | Punto de Auditoría | Enfoque | Total Reqs | Estado del Sprint |
| :---: | :---: | :---: | :--- | :---: | :---: |
| **Sprint 1** | E1 | **AI-01** | Gobernanza Ágil, Seguridad Base y Control de Accesos | 10 | **✅ CUMPLIDO (Tachado)** |
| **Sprint 2** | E2 | **AI-02** | Infraestructura Docker, Base de Datos y Trazabilidad | 10 | 🔄 En progreso (5/10 cumplidos) |
| **Sprint 3** | E3 | **AI-03** | Triage Manchester, Urgencias y Admisión | 13 | ⏳ Pendiente |
| **Sprint 4** | E4 | **AI-04** | ECE, Recetas Médicas Controladas e Interoperabilidad | 11 | ⏳ Pendiente |
| **Sprint 5** | E5/E6 | **AI-05** | Portal Móvil del Paciente, Estadística y QA Integral | 7 | ⏳ Pendiente |
| **Sprint Final**| E6 | **AI-06** | Quality Gate, Despliegue Inmutable y BCP/DRP | 6 | ⏳ Pendiente |

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
> *Estado: En progreso. Implementada bitácora inalterable AUDITORIA_I, esquema clínico PACIENTE y EXPEDIENTE_C con triggers anti-DELETE, servicio AuditLogger con SHA-256 encadenado, reporte de retención legal NOM-004 (1,825 días), seeders sintéticos no-PII y 35 pruebas automatizadas.*

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

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| **RF-004** | Funcional | P0 | Captura obligatoria de signos vitales (presión arterial, FC, FR, temp, EVA, Glasgow). | `triage` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 3) |
| **RF-005** | Funcional | P0 | Asignación automática de nivel de gravedad (Algoritmo Manchester de 5 niveles: Rojo a Azul). | `triage` | NOM-206-SSA1-2002 | ⏳ Pendiente (Sprint 3) |
| **RF-006** | Funcional | P0 | Gestión y distribución dinámica de colas de espera según prioridad clínica y tiempo transcurrido. | `urgencias` | NOM-206-SSA1-2002 | ⏳ Pendiente (Sprint 3) |
| **RF-007** | Funcional | P0 | Reclasificación manual de triage con justificación clínica obligatoria firmada por médico. | `triage` | NOM-206-SSA1-2002 | ⏳ Pendiente (Sprint 3) |
| **PROV-RN-004** | Regla Negocio | P0 | Matriz Manchester y SLAs de atención (Rojo: inmediato, Naranja: 10m, Amarillo: 60m, Verde: 120m, Azul: 240m).| `triage` | NOM-206-SSA1-2002 | ⏳ Pendiente (Sprint 3) |
| **PROV-RN-005** | Regla Negocio | P0 | Gestión de tiempos excedidos y alertas escalables a jefatura de guardia. | `urgencias` | NOM-206-SSA1-2002 | ⏳ Pendiente (Sprint 3) |
| **PROV-UX-001** | No Funcional | P0 | Actualización de colas de triage en tiempo real mediante WebSockets / SSE. | `urgencias` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 3) |
| **PROV-BR-001** | Regla Negocio | P0 | Codificación semántica universal de colores de triage y soporte para daltonismo. | `ui_ux` | WCAG 2.1 AA | ⏳ Pendiente (Sprint 3) |
| **PROV-RN-006** | Regla Negocio | P1 | Tolerancia a fallos de red en admisión y captura retroactiva de folios offline. | `admision` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 3) |
| **PROV-RN-007** | Regla Negocio | P1 | Admisión de paciente no identificado (desconocido) y posterior proceso de fusión de expediente. | `admision` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 3) |
| **PROV-UX-002** | No Funcional | P1 | Pantalla pública de sala de espera disociada (solo iniciales y turno, sin PII). | `sala_espera` | LFPDPPP | ⏳ Pendiente (Sprint 3) |
| **PROV-UX-003** | No Funcional | P1 | Accesibilidad web y ergonomía clínica WCAG 2.1 AA (operable 100% por teclado). | `ui_ux` | WCAG 2.1 AA | ⏳ Pendiente (Sprint 3) |
| **PROV-UI-001** | No Funcional | P2 | Implementación de pantallas de captura de triage según especificación de UI/UX. | `frontend` | ERS Anexo G.1 | ⏳ Pendiente (Sprint 3) |

---

### Sprint 4: ECE, Recetas Médicas Controladas e Interoperabilidad (Auditoría AI-04)

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| **RF-008** | Funcional | P0 | Gestión integral del Expediente Clínico Electrónico (ECE) conforme a normatividad nacional. | `expediente` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 4) |
| **RF-008.1** | Funcional | P0 | Creación y datos de identificación del expediente, antecedentes y ficha técnica. | `expediente` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 4) |
| **RF-008.2** | Funcional | P0 | Historia clínica, notas de evolución, interconsultas e inmutabilidad de notas cerradas. | `expediente` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 4) |
| **RF-009** | Funcional | P0 | Generación y emisión de recetas médicas para medicamentos controlados (Fracc. I, II, III). | `recetas` | COFEPRIS, SAT | ⏳ Pendiente (Sprint 4) |
| **RF-010** | Funcional | P0 | Solicitud y consulta de resultados de laboratorio e imagenología médica en el ECE. | `laboratorio` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 4) |
| **RNF-007** | No Funcional | P0 | Interoperabilidad semántica en salud basada en el estándar HL7 FHIR R4. | `interoperabilidad` | HL7 FHIR R4 | ⏳ Pendiente (Sprint 4) |
| **PROV-RN-008** | Regla Negocio | P1 | Gestión del consentimiento informado del paciente con hash inmutable. | `expediente` | LFPDPPP | ⏳ Pendiente (Sprint 4) |
| **PROV-RN-009** | Regla Negocio | P1 | Atención y trazabilidad de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición). | `privacidad` | LFPDPPP | ⏳ Pendiente (Sprint 4) |
| **PROV-RN-010** | Regla Negocio | P1 | Protocolo de acceso de emergencia "Break-the-Glass" al expediente con auditoría reforzada. | `seguridad` | NOM-004-SSA3-2012 | ⏳ Pendiente (Sprint 4) |
| **PROV-SEC-008** | No Funcional | P1 | Privacidad desde el diseño en exportaciones clínicas (marcas de agua y datos anonimizados). | `seguridad` | LFPDPPP | ⏳ Pendiente (Sprint 4) |
| **PROV-UI-002** | No Funcional | P2 | Pantallas de consulta de ECE y emisión de recetas médicas. | `frontend` | ERS Anexo G.2 | ⏳ Pendiente (Sprint 4) |

---

### Sprint 5: Portal Móvil, Estadística y QA Integral (Auditoría AI-05)

| ID | Tipo | Prioridad | Requerimiento / Descripción | Módulo | Normas Asociadas | Estado |
| :--- | :---: | :---: | :--- | :--- | :--- | :---: |
| **RF-011** | Funcional | P0 | API móvil de solo lectura para consulta de paciente (historial, citas, recetas). | `mobile_api` | NOM-024-SSA3-2012, LFPDPPP | ⏳ Pendiente (Sprint 5) |
| **RNF-005** | No Funcional | P1 | Rendimiento y tiempos de respuesta P95 <= 2.0 s en visualización de expediente clínico. | `rendimiento` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 5) |
| **RNF-006** | No Funcional | P1 | Compatibilidad de la aplicación móvil nativa (Android minSdk 29+ / Android 10+). | `mobile` | Android SDK | ⏳ Pendiente (Sprint 5) |
| **PROV-RF-001** | Funcional | P1 | Tableros de control estadístico y reportes de salud laboral disociados (NOM-035). | `estadistica` | NOM-035-STPS-2018 | ⏳ Pendiente (Sprint 5) |
| **PROV-RF-002** | Funcional | P1 | Módulo de agenda y confirmación de citas médicas para pacientes. | `agenda` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 5) |
| **PROV-RNF-002** | No Funcional | P0 | Estrategia integral de aseguramiento de calidad (QA: unitarias, integración, e2e, estrés). | `qa` | ISO/IEC-25010 | ⏳ Pendiente (Sprint 5) |
| **PROV-SEC-009** | No Funcional | P1 | Gestión de vulnerabilidades, inventario de componentes SBOM y escaneo SAST/DAST. | `secops` | OWASP ASVS L2 | ⏳ Pendiente (Sprint 5) |

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

| Norma / Marco | Objeto y Requisitos Clave | Requerimientos SIH | Estado Global |
| :--- | :--- | :--- | :---: |
| **NOM-004-SSA3-2012** | Expediente clínico: inmutabilidad de notas médicas, retención >= 5 años, sin DELETE físico, confidencialidad. | RF-004, RF-008, RF-008.1, RF-008.2, RF-010, RNF-002, PROV-RN-003, PROV-RN-007, PROV-RN-010 | ⏳ En Progreso (Sprint 2/3/4) |
| **NOM-024-SSA3-2012** | Sistemas de información en salud: control de acceso RBAC, auditoría de accesos inalterable `AUDITORIA_I`, integridad. | ~~RF-001~~, ~~RF-002~~, ~~PROV-SEC-003~~, RNF-001, PROV-SEC-005 | 🟡 **Parcialmente Cumplida** (Base Sprint 1 ✅ / Trazabilidad Sprint 2 ⏳) |
| **NOM-206-SSA1-2002** | Urgencias médicas: Sistema de Triage Manchester (5 niveles de prioridad), monitoreo de SLAs de espera. | RF-005, RF-006, RF-007, PROV-RN-004, PROV-RN-005 | ⏳ Pendiente (Sprint 3) |
| **NOM-035-STPS-2018** | Factores de riesgo psicosocial en el trabajo: reportes estadísticos agregados sin datos personales. | PROV-RF-001 | ⏳ Pendiente (Sprint 5) |
| **ISO/IEC 27001** | Seguridad de la información: cifrado fuerte (Bcrypt >= 12, TLS >= 1.2, AES-256), sesiones de 15 min, respaldo 3-2-1. | ~~RF-001~~, ~~RF-002~~, ~~RNF-008~~, ~~PROV-SEC-001~~, ~~PROV-SEC-004~~, ~~PROV-RNF-001~~, ~~RNF-003~~, ~~PROV-SEC-006~~, PROV-RNF-004 | 🟡 **Parcialmente Cumplida** (Auth/Docker/TLS/Cifrado Reposo/Respaldos 3-2-1 ✅) |
| **ISO/IEC 25010** | Calidad del producto software: rendimiento (P95 <= 2s), alta disponibilidad (99.9%), cobertura de pruebas >= 80%. | ~~PROV-RN-001~~, ~~RNF-004~~, RNF-005, PROV-RNF-002, PROV-RN-011 | 🟡 **Parcialmente Cumplida** (Pruebas 100% Sprint 1 ✅ / HA y Resiliencia Sprint 2 ✅) |
| **OWASP ASVS L2** | Verificación de seguridad en aplicaciones: 401 vs 403, prevención de fuerza bruta, manejo de tokens seguro. | ~~RF-002~~, ~~RF-003~~, ~~PROV-SEC-001~~, ~~PROV-SEC-002~~, PROV-SEC-009 | 🟡 **Parcialmente Cumplida** (Auth & RBAC ✅ / DAST Sprint 5 ⏳) |
| **HL7 FHIR R4** | Estándar de interoperabilidad en salud: intercambio clínico semántico (`Patient`, `Encounter`, etc.). | RNF-007 | ⏳ Pendiente (Sprint 4) |
| **LFPDPPP** | Protección de datos personales: consentimiento informado con hash, derechos ARCO, disociación en salas públicas. | PROV-RN-008, PROV-RN-009, PROV-SEC-008, PROV-UX-002, PROV-SEC-010 | ⏳ Pendiente (Sprint 3/4) |
| **COFEPRIS / SAT** | Receta médica electrónica controlada: cédula profesional verificada, e.firma digital avanzada y código QR único. | ~~RF-001~~ (Cédula médica ✅), RF-009 (e.firma/QR ⏳) | 🟡 **Parcialmente Cumplida** |
| **WCAG 2.1 AA** | Accesibilidad web: contraste 4.5:1, accesibilidad por teclado y soporte para daltonismo en urgencias. | PROV-BR-001, PROV-UX-003 | ⏳ Pendiente (Sprint 3) |
| **CENAM / NTP** | Metrología y hora legal: sincronización horaria con deriva <= 1 segundo y almacenamiento en UTC. | ~~PROV-SEC-007~~ | ✅ **Cumplida (100%)** |
