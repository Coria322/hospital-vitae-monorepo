# REQUIREMENTS_EXECUTION.md — SIH Sistema de Información Hospitalaria · mobile/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Total requerimientos: 18
> Fecha: 2026-10-09
> Organización: por Stage/Sprint/Épica (AI-01..AI-06); dentro de cada Stage, por prioridad P0→P2. Criterios GWT deterministas (códigos HTTP, SQL, valores).
> IDs `PROV-` = sin ID formal en el ERS (⚠️ AMBIGUOUS_ID).

---

# STAGE 5 · Sprint 5 · Portal Móvil, Estadística y QA Integral · Épica E5/E6 · Auditoría AI-05 · 06 Nov – 19 Nov 2026

## RF-002 — Rol PACIENTE: acceso móvil (porción móvil de RF-002)

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `auth-ui`, `rbac-client`
- **Dependencias:** `web:RF-002`
- **Referencias normativas:** NOM-024-SSA3-2012, ISO/IEC-27001
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  La app DEBE operar exclusivamente con el rol PACIENTE: consultar información básica, aseguradora y resultados. NO DEBE mostrar acciones de personal. Ante HTTP 403 DEBE mostrar un error de autorización genérico.

### Criterios de aceptación

**Escenario 1: Inicio de sesión**
- **Dado** un paciente con cuenta ACTIVA
- **Cuando** inicia sesión
- **Entonces** navega a Inicio con citas, recetas y resultados

**Escenario 2: Token de personal**
- **Dado** credenciales de un MEDICO
- **Cuando** intenta iniciar sesión en la app
- **Entonces** la API responde HTTP 403 y la app muestra 'Acceso no disponible en la app'

### Casos borde
- Cuenta suspendida: mensaje de contactar a administración

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> No hay selector de rol.

---

## RF-011 — Acceso de solo lectura en aplicación móvil

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `app-readonly`
- **Dependencias:** `RF-002`, `web:RF-011`
- **Referencias normativas:** NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05, MC-05
- **Descripción implementable:**
  La app Android DEBE permitir al paciente iniciar sesión y consultar exclusivamente sus citas, recetas y resultados. La UI NO DEBE incluir botones de guardado ni modificación de registros clínicos. Escrituras no clínicas permitidas (lista blanca SUP-002): login/refresh/logout/OTP, aceptación-revocación de consentimiento y solicitud ARCO.

### Criterios de aceptación

**Escenario 1: Sin controles de edición**
- **Dado** pantallas de citas, recetas y resultados
- **Cuando** se recorren con pruebas de UI
- **Entonces** 0 botones de guardar/editar/eliminar

**Escenario 2: API rechaza escrituras**
- **Dado** el token del paciente
- **Cuando** POST /api/v1/mobile/recetas
- **Entonces** HTTP 403 o 405 (verificado en pruebas de integración)

### Casos borde
- Sin internet: mensaje controlado y reintento; sin datos en caché sin cifrar

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Ver AMB-016 (lista blanca vs 'rechaza POST/PUT/DELETE').

---

## RF-009 — Consulta de recetas (porción móvil)

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `recetas-ui`
- **Dependencias:** `RF-011`, `web:RF-009`
- **Referencias normativas:** NOM-004-SSA3-2012, HL7-FHIR-R4
- **Riesgos mitigados:** R-02 · **Métricas:** MC-04
- **Descripción implementable:**
  La app DEBE listar y mostrar el detalle de recetas del paciente (MedicationRequest FHIR) con estado (activa, dispensada, cancelada) y código QR cuando exista. NO DEBE permitir imprimir ni modificar.

### Criterios de aceptación

**Escenario 1: Detalle**
- **Dado** una receta propia
- **Cuando** el paciente abre el detalle
- **Entonces** muestra medicamento, dosis, vía, duración y estado

**Escenario 2: QR consumido**
- **Dado** receta dispensada
- **Cuando** se abre
- **Entonces** el QR se muestra como inválido y estado 'dispensada'

### Casos borde
- Receta controlada sin e.firma validada: no aparece en la app

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Parsear FHIR con modelos Kotlin tipados.

---

## RF-010 — Consulta de resultados (porción móvil)

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `resultados-ui`
- **Dependencias:** `RF-011`, `web:RF-010`
- **Referencias normativas:** NOM-004-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-05
- **Descripción implementable:**
  La app DEBE listar resultados de laboratorio e imagen y abrir el archivo (PDF o imagen) en visor interno sin guardado en almacenamiento compartido.

### Criterios de aceptación

**Escenario 1: Abrir PDF**
- **Dado** un resultado propio
- **Cuando** el paciente lo abre
- **Entonces** el archivo se muestra en visor interno sin errores

**Escenario 2: Resultado ajeno**
- **Dado** id de otro paciente forzado
- **Cuando** la API responde 403
- **Entonces** la app no muestra datos

### Casos borde
- Archivo grande: muestra progreso y no bloquea la UI

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Descifrado lo hace el backend; el cliente recibe stream sobre TLS.

---

## RNF-003 — Cifrado en tránsito y fijación de certificados

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `network`
- **Dependencias:** `web:RNF-003`
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-07 · **Métricas:** MS-01
- **Descripción implementable:**
  La app DEBE comunicarse solo por TLS >= 1.2, validar certificados estrictamente y aplicar certificate pinning; DEBE rechazar HTTP plano (cleartextTrafficPermitted=false).

### Criterios de aceptación

**Escenario 1: Cleartext**
- **Dado** la configuración de red
- **Cuando** se inspecciona
- **Entonces** cleartextTrafficPermitted=false

**Escenario 2: MITM**
- **Dado** un proxy con certificado no fijado
- **Cuando** la app intenta conectar
- **Entonces** la conexión se rechaza

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- MS-01 = 100%

### Notas de agente
> Sin notas adicionales.

---

## RNF-007 — Consumo de recursos HL7 FHIR R4

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `fhir-client`
- **Dependencias:** `web:RNF-007`
- **Referencias normativas:** HL7-FHIR-R4
- **Riesgos mitigados:** R-04 · **Métricas:** MC-04
- **Descripción implementable:**
  La app DEBE consumir Patient y MedicationRequest como JSON FHIR R4 y rechazar con error controlado recursos que no validen contra el esquema.

### Criterios de aceptación

**Escenario 1: Patient válido**
- **Dado** respuesta FHIR Patient
- **Cuando** la app la procesa
- **Entonces** muestra CURP, nombre y fecha de nacimiento

**Escenario 2: Recurso inválido**
- **Dado** respuesta sin resourceType
- **Cuando** la app la procesa
- **Entonces** muestra error controlado y no se cierra

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-SEC-001 — Autenticación del paciente y sesión móvil (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `auth-ui`, `session`
- **Dependencias:** `web:PROV-SEC-001`
- **Referencias normativas:** ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-04
- **Descripción implementable:**
  La app DEBE autenticar al paciente con correo electrónico verificado y contraseña, solicitar código de un solo uso al iniciar sesión en un dispositivo nuevo, bloquear tras 3 intentos fallidos en < 1 h y cerrar la sesión tras 5 min de inactividad.

### Criterios de aceptación

**Escenario 1: Dispositivo nuevo**
- **Dado** un paciente con credenciales correctas en un dispositivo no registrado
- **Cuando** envía el login
- **Entonces** la API responde otp_required=true y no entrega tokens hasta POST /api/v1/mobile/auth/otp-verify válido

**Escenario 2: Inactividad**
- **Dado** sesión abierta sin interacción
- **Cuando** pasan 300 s
- **Entonces** la app navega a la pantalla de login, borra datos locales y la API revoca el token

### Casos borde
- Tercer intento fallido: la app muestra alerta de contactar a administración (HTTP 423)

### Validaciones
- inactividad: 300 s
- password: sin límite superior < 64

### Notas de agente
> Mensajes de error genéricos. Mismas reglas de bloqueo que RF-002 (web).

---

## PROV-SEC-002 — OAuth 2.0 con PKCE y manejo de tokens (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `auth-client`
- **Dependencias:** `web:PROV-SEC-002`
- **Referencias normativas:** ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02, R-07 · **Métricas:** MS-04
- **Descripción implementable:**
  La app DEBE usar el flujo Authorization Code + PKCE (S256), guardar tokens solo en EncryptedSharedPreferences respaldado por Android Keystore, renovar con refresh token rotativo y, al cerrar sesión, borrar datos locales e invocar la revocación inmediata (blacklisting) en el servidor.

### Criterios de aceptación

**Escenario 1: Almacenamiento seguro**
- **Dado** un login exitoso
- **Cuando** se inspecciona el almacenamiento de la app en un dispositivo de prueba
- **Entonces** no existen tokens en texto plano; se usan EncryptedSharedPreferences

**Escenario 2: Logout**
- **Dado** sesión activa
- **Cuando** el paciente cierra sesión
- **Entonces** se borran datos locales y el access_token anterior produce HTTP 401

### Casos borde
- Refresh token reutilizado: la app fuerza nuevo login

### Validaciones
- code_challenge_method=S256

### Notas de agente
> Access token <= 15 min.

---

## RF-002.1 — Aislamiento horizontal: la app nunca envía id_paciente (porción móvil)

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** security
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `api-client`
- **Dependencias:** `web:RF-002.1`
- **Referencias normativas:** ISO/IEC-27001, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  El cliente NO DEBE enviar id_paciente en ninguna petición; la identidad se deriva del token. Ante HTTP 403 por aislamiento DEBE descartar el estado y mostrar error genérico.

### Criterios de aceptación

**Escenario 1: Revisión de red**
- **Dado** tráfico de la app capturado
- **Cuando** se inspeccionan las peticiones
- **Entonces** 0 peticiones incluyen id_paciente en query, path o cuerpo

**Escenario 2: Respuesta 403**
- **Dado** la API devuelve 403
- **Cuando** la app procesa la respuesta
- **Entonces** no muestra datos y registra el evento localmente sin PII

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Verificar con interceptor de pruebas.

---

## RF-003 — Manejo cliente de 401 y 403 (porción móvil)

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** security
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `api-client`
- **Dependencias:** `web:RF-003`
- **Referencias normativas:** OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  La app DEBE tratar HTTP 401 como sesión inválida (ir a login y limpiar datos) y HTTP 403 como falta de permisos (mensaje sin cerrar sesión). NO DEBE ocultar botones como único control.

### Criterios de aceptación

**Escenario 1: 401**
- **Dado** cualquier petición que retorne 401
- **Cuando** se procesa
- **Entonces** la app navega a login y limpia el estado

**Escenario 2: 403**
- **Dado** una petición que retorne 403
- **Cuando** se procesa
- **Entonces** la app permanece en sesión y muestra error de permisos

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-RF-002 — Consulta de citas (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** functional
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `citas-ui`
- **Dependencias:** `web:PROV-RF-002`, `RF-011`
- **Referencias normativas:** NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-05
- **Descripción implementable:**
  La app DEBE listar las citas del paciente en solo lectura.

### Criterios de aceptación

**Escenario 1: Listado**
- **Dado** un paciente con citas
- **Cuando** abre Citas
- **Entonces** ve fecha, hora y estado de cada cita propia

**Escenario 2: Sin citas**
- **Dado** paciente sin citas
- **Cuando** abre Citas
- **Entonces** estado vacío con mensaje

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Estado vacío obligatorio.

---

## RNF-006 — Compatibilidad Android 10 (API 29) o superior

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** non_functional
- **Prioridad:** P1 _(base: ERS: Media)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `app-core`
- **Dependencias:** `web:RNF-006`
- **Referencias normativas:** ISO/IEC-25010
- **Riesgos mitigados:** R-04 · **Métricas:** MC-05
- **Descripción implementable:**
  La app DEBE instalarse y ejecutarse sin cierres forzados en Android 10 (API 29) o superior, en al menos 3 tamaños de pantalla (gama baja, media y alta), sin deformaciones que impidan leer resultados. En versiones anteriores DEBE mostrar mensaje controlado de incompatibilidad.

### Criterios de aceptación

**Escenario 1: Matriz de dispositivos**
- **Dado** 3 dispositivos o emuladores con API >= 29
- **Cuando** se ejecutan las pruebas funcionales
- **Entonces** 0 crashes

**Escenario 2: API 28**
- **Dado** un dispositivo con Android 9
- **Cuando** se abre la app
- **Entonces** mensaje de incompatibilidad controlado, sin datos técnicos

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- minSdk=29

### Notas de agente
> Sin notas adicionales.

---

## PROV-RN-008 — Aviso de privacidad y consentimiento en la app (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `privacy-ui`
- **Dependencias:** `web:PROV-RN-008`
- **Referencias normativas:** LFPDPPP
- **Riesgos mitigados:** R-10 · **Métricas:** MN-05
- **Descripción implementable:**
  La app DEBE mostrar el aviso de privacidad simplificado con enlace al integral antes de recabar datos, registrar versión mostrada y permitir aceptar o revocar consentimientos (finalidades secundarias en opción independiente no preseleccionada).

### Criterios de aceptación

**Escenario 1: Primera apertura**
- **Dado** un paciente nuevo
- **Cuando** abre la app
- **Entonces** ve el aviso antes de cualquier formulario

**Escenario 2: Revocación**
- **Dado** consentimiento secundario vigente
- **Cuando** el paciente lo revoca
- **Entonces** POST /api/v1/mobile/consentimientos con accion='REVOCAR' responde 201

### Casos borde
- Cambio sustancial del aviso: se solicita nueva aceptación

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Escritura permitida por lista blanca SUP-002.

---

## PROV-RN-009 — Solicitud de derechos ARCO desde la app (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `arco-ui`
- **Dependencias:** `web:PROV-RN-009`
- **Referencias normativas:** LFPDPPP
- **Riesgos mitigados:** R-10 · **Métricas:** MN-05
- **Descripción implementable:**
  La app DEBE permitir enviar una solicitud ARCO y consultar su estado y plazos.

### Criterios de aceptación

**Escenario 1: Envío**
- **Dado** un paciente autenticado
- **Cuando** POST /api/v1/mobile/solicitudes-arco
- **Entonces** HTTP 201 con folio y fechas límite

**Escenario 2: Consulta**
- **Dado** una solicitud existente
- **Cuando** GET /api/v1/mobile/solicitudes-arco/{folio}
- **Entonces** HTTP 200 con estado

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-SEC-011 — Endurecimiento de la aplicación móvil

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `app-security`
- **Dependencias:** `PROV-SEC-002`
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-07, R-06 · **Métricas:** MS-03
- **Descripción implementable:**
  La app DEBE detectar Root y Magisk y bloquear su ejecución, ofuscar el código en release, evitar capturas de pantalla en pantallas clínicas (FLAG_SECURE), no persistir PII en claro y firmar con APK Signature Scheme v2 o superior.

### Criterios de aceptación

**Escenario 1: Dispositivo con root**
- **Dado** un dispositivo rooteado de pruebas
- **Cuando** se abre la app
- **Entonces** la app muestra bloqueo y no ejecuta ninguna petición

**Escenario 2: Captura de pantalla**
- **Dado** pantalla de resultados
- **Cuando** se intenta capturar
- **Entonces** la captura está bloqueada

### Casos borde
- Detección falsa positiva: documentar y reportar a Sec

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Librería de detección: SUP-007.

---

## PROV-UX-003 — Accesibilidad móvil WCAG 2.1 AA (porción móvil)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** ux
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `ui-global`
- **Dependencias:** `web:PROV-UX-003`
- **Referencias normativas:** WCAG-2.1-AA
- **Riesgos mitigados:** R-04 · **Métricas:** MC-05
- **Descripción implementable:**
  La app DEBE cumplir contraste >= 4.5:1, soportar lector de pantalla (TalkBack) con contentDescription en controles, objetivos táctiles >= 48 dp y no depender solo del color.

### Criterios de aceptación

**Escenario 1: Contraste**
- **Dado** cualquier texto normal
- **Cuando** se mide con Accessibility Scanner
- **Entonces** ratio >= 4.5:1

**Escenario 2: TalkBack**
- **Dado** pantalla de resultados
- **Cuando** se navega con TalkBack
- **Entonces** todos los controles tienen descripción

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-UI-003 — Portal móvil: pantallas (PB-5.1)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** ui
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `app-ui`
- **Dependencias:** `RF-011`
- **Referencias normativas:** WCAG-2.1-AA
- **Riesgos mitigados:** R-04 · **Métricas:** MC-05
- **Descripción implementable:**
  La app DEBE ofrecer pantallas: Login (+OTP), Aviso de privacidad, Inicio, Citas, Recetas (lista/detalle), Resultados (lista/visor), Perfil y aseguradora, Solicitud ARCO, Sesión expirada, Incompatible, Bloqueada por root. Patrón MVVM con Jetpack ViewModel.

### Criterios de aceptación

**Escenario 1: Navegación**
- **Dado** un paciente autenticado
- **Cuando** recorre las pantallas
- **Entonces** todas existen y los estados cargando, vacío y error están implementados

**Escenario 2: Estados**
- **Dado** sin conexión
- **Cuando** se abre cualquier lista
- **Entonces** muestra estado de error con reintento

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin prototipo móvil en el ERS: AMB-009.

---

# STAGE 6 · Sprint RELEASE · Quality Gate y Despliegue (Release Final) · Épica E6 · Auditoría AI-06 · 20 Nov – 25 Nov 2026

## PROV-RNF-005 — Firma, versionado y distribución de la app

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

_Alcance: porción móvil del requerimiento (la contraparte API vive en `web/`)._

- **Tipo:** non_functional
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `release`
- **Dependencias:** `PROV-SEC-011`
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-06 · **Métricas:** MC-11, MP-06
- **Descripción implementable:**
  La app DEBE firmarse con APK Signature v2 o superior, versionarse con SemVer, generar SBOM por release y publicarse en la plataforma de distribución con cuentas con MFA. Un rollback DEBE reactivar la versión estable anterior; compatibilidad API: la app NO DEBE publicarse si falla la prueba de contrato contra la API desplegada.

### Criterios de aceptación

**Escenario 1: Firma**
- **Dado** el APK de release
- **Cuando** se verifica con apksigner
- **Entonces** esquema v2 o superior válido

**Escenario 2: Contrato**
- **Dado** la API desplegada vX
- **Cuando** se ejecutan las pruebas de contrato de la app
- **Entonces** 100% de endpoints móviles conformes

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> El plan de rollback del ERS cita incompatibilidad app–API como ejemplo.

---
