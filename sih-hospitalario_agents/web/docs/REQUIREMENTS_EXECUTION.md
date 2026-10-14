# REQUIREMENTS_EXECUTION.md — SIH Sistema de Información Hospitalaria · web/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Total requerimientos: 57
> Fecha: 2026-10-09
> Organización: por Stage/Sprint/Épica (AI-01..AI-06); dentro de cada Stage, por prioridad P0→P2. Criterios GWT deterministas (códigos HTTP, SQL, valores).
> IDs `PROV-` = sin ID formal en el ERS (⚠️ AMBIGUOUS_ID).

---

# STAGE 1 · Sprint 1 · Gobernanza Ágil, Seguridad Base y Control de Accesos · Épica E1 · Auditoría AI-01 · 09 Sep – 22 Sep 2026

## RF-001 — Herramientas de administración del sistema

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `usuarios`, `auditoria-consulta`
- **Dependencias:** `RF-002`, `RNF-008`
- **Referencias normativas:** ISO/IEC-27001, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05, MS-02
- **Descripción implementable:**
  El sistema DEBE permitir al rol ADMINISTRADOR dar de alta y de baja usuarios del hospital y consultar, en modo solo lectura, los registros de AUDITORIA_I de todas las áreas. El alta de un MEDICO DEBE verificar la cédula profesional (VARCHAR 10). La baja DEBE ejecutarse en un plazo máximo de 24 h desde la conclusión de la relación laboral, sin reutilizar el id_usuario y conservando íntegros los registros de auditoría asociados. Toda consulta a AUDITORIA_I DEBE generar un evento de auditoría.

### Criterios de aceptación

**Escenario 1: Alta de usuario médico**
- **Dado** un ADMINISTRADOR autenticado con MFA verificado
- **Cuando** envía POST /api/v1/usuarios con rol=MEDICO, cedula de 10 caracteres e id_usuario no existente
- **Entonces** responde HTTP 201; existe 1 fila en usuario con estado_cuenta='ACTIVA' y password_hash con prefijo '$2y$12$' o superior; existe 1 fila en auditoria.auditoria_i con tipo_movimiento='INSERT' y tabla_afectada='usuario'

**Escenario 2: Baja de usuario**
- **Dado** un usuario activo con registros previos en AUDITORIA_I
- **Cuando** el ADMINISTRADOR envía POST /api/v1/usuarios/{id_usuario}/baja
- **Entonces** responde HTTP 200; usuario.estado_cuenta='BAJA'; sus tokens quedan revocados; COUNT de filas de AUDITORIA_I del usuario no disminuye; un POST /api/v1/usuarios con el mismo id_usuario responde HTTP 409

**Escenario 3: Rol sin privilegio administrativo**
- **Dado** un token válido con rol=ENFERMERIA
- **Cuando** envía POST /api/v1/usuarios
- **Entonces** responde HTTP 403 y se inserta 1 fila en AUDITORIA_I con resultado='DENEGADO'

**Escenario 4: Consulta de auditoría**
- **Dado** un ADMINISTRADOR autenticado
- **Cuando** envía GET /api/v1/auditoria?desde=&hasta=
- **Entonces** responde HTTP 200 con registros paginados y se inserta 1 fila de auditoría con tipo_movimiento='CONSULTA_AUDITORIA'; PUT/PATCH/DELETE sobre /api/v1/auditoria responden HTTP 405

### Casos borde
- Alta de MEDICO sin cédula: HTTP 422 con error de validación en campo cedula
- ADMINISTRADOR consulta GET /api/v1/expedientes-clinicos/{id}: HTTP 403 (separación de funciones, sin acceso a contenido clínico)

### Validaciones
- id_usuario: VARCHAR(10), único, no reutilizable
- cedula: obligatoria si rol=MEDICO, VARCHAR(10)
- rol: valor del enum RolUsuario

### Notas de agente
> Implementar primero RF-002 (roles) y RNF-008 (hash). El administrador gestiona usuarios y lee auditoría, nunca contenido clínico (Tabla 44).

---

## RF-002 — Control de acceso basado en roles (macro-requerimiento)

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `auth`, `rbac`
- **Dependencias:** `RNF-008`, `PROV-SEC-001`
- **Referencias normativas:** NOM-024-SSA3-2012, ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-04, MS-05
- **Descripción implementable:**
  El sistema DEBE habilitar interfaces y permisos según el rol: MEDICO (consultar paciente, solicitar estudios, generar recetas, gestionar cola), ENFERMERIA (capturar signos vitales, ver colas, registrar notas; PROHIBIDA la emisión de recetas y diagnósticos), RECEPCION (ver sala de espera, confirmar citas), PACIENTE (solo móvil: información básica, aseguradora y resultados). El sistema DEBE suspender la cuenta al registrar 3 intentos fallidos de inicio de sesión en menos de 1 hora; solo ADMINISTRADOR reactiva, con verificación de identidad y cambio obligatorio de contraseña.

### Criterios de aceptación

**Escenario 1: Acceso a módulo no autorizado**
- **Dado** un token válido con rol=ENFERMERIA
- **Cuando** envía POST /api/v1/recetas
- **Entonces** responde HTTP 403 sin crear fila en receta

**Escenario 2: Recepción consulta sala de espera**
- **Dado** un token válido con rol=RECEPCION
- **Cuando** envía GET /api/v1/sala-espera
- **Entonces** responde HTTP 200 con folio e iniciales, sin nombre completo ni datos clínicos

**Escenario 3: Bloqueo al tercer intento**
- **Dado** una cuenta ACTIVA sin intentos previos
- **Cuando** se envían 3 POST /api/v1/auth/login con contraseña incorrecta dentro de 60 minutos
- **Entonces** el tercer intento responde HTTP 423 con code='ACCOUNT_SUSPENDED'; usuario.estado_cuenta='SUSPENDIDA'; un cuarto intento con la contraseña correcta responde HTTP 423

**Escenario 4: Reactivación**
- **Dado** una cuenta SUSPENDIDA
- **Cuando** un ADMINISTRADOR envía POST /api/v1/usuarios/{id}/reactivar con verificacion_identidad=true
- **Entonces** estado_cuenta='ACTIVA', debe_cambiar_password=true y 1 fila de auditoría tipo_movimiento='REACTIVACION_CUENTA'; un rol distinto de ADMINISTRADOR recibe HTTP 403

### Casos borde
- Credencial inexistente con 3 intentos: se aplica el mismo contador y la misma respuesta HTTP 423 que a una cuenta existente (sin revelar existencia)
- Intentos 1 y 2 fallidos: HTTP 401 con mensaje genérico idéntico para usuario inexistente y contraseña errónea

### Validaciones
- ventana de bloqueo: 3 fallos en < 60 min
- rol activo único por sesión

### Notas de agente
> Permisos asignados a roles, no a usuarios. La matriz Tabla 44 llegó desordenada en el documento fuente: ver AMB-003 antes de codificar la matriz definitiva.

---

## RNF-008 — Gestión y cifrado de credenciales de acceso

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `auth`
- **Dependencias:** ninguna
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-02 · **Métricas:** MS-02
- **Descripción implementable:**
  El sistema DEBE almacenar contraseñas únicamente con Bcrypt (sal única automática, factor de costo >= 12) en usuario.password_hash VARCHAR(60). El sistema NO DEBE permitir recuperar contraseñas en texto plano; el restablecimiento DEBE usar tokens de un solo uso. Longitud mínima 12 caracteres con verificación contra listas de contraseñas comunes o comprometidas.

### Criterios de aceptación

**Escenario 1: Hash seguro**
- **Dado** un alta de usuario con contraseña válida
- **Cuando** se inspecciona usuario.password_hash
- **Entonces** coincide con regex ^\$2[aby]\$(1[2-9]|[2-3][0-9])\$ y longitud = 60; no contiene la contraseña tecleada

**Escenario 2: Token de reinicio de un solo uso**
- **Dado** un token de reinicio ya consumido
- **Cuando** se reenvía POST /api/v1/auth/password/reset
- **Entonces** HTTP 410 o 422 y la contraseña no cambia

### Casos borde
- Contraseña de 11 caracteres: HTTP 422
- Contraseña en lista de comprometidas: HTTP 422

### Validaciones
- password >= 12 caracteres
- bcrypt cost >= 12

### Notas de agente
> Prohibido MD5, SHA-1, DES, 3DES, RC4 y criptografía propia.

---

## RF-002.1 — Aislamiento horizontal de registros clínicos (derivado)

- **Tipo:** security
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `rbac`, `pacientes`
- **Dependencias:** `RF-002`, `RF-003`
- **Referencias normativas:** ISO/IEC-27001, NOM-024-SSA3-2012, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  El sistema DEBE restringir toda consulta del rol PACIENTE a registros cuyo id_paciente coincida con el claim 'sub' del token. El sistema DEBE ignorar cualquier id_paciente enviado por el cliente y abortar con HTTP 403 si difiere del token.

### Criterios de aceptación

**Escenario 1: Inyección de otro UUID**
- **Dado** un token de paciente A con sesión activa
- **Cuando** envía GET /api/v1/mobile/recetas?id_paciente=<UUID de B>
- **Entonces** responde HTTP 403; el cuerpo no contiene datos de B; se inserta 1 fila de auditoría resultado='DENEGADO'

**Escenario 2: Consulta propia**
- **Dado** un token de paciente A
- **Cuando** envía GET /api/v1/mobile/recetas
- **Entonces** responde HTTP 200 y todas las filas devueltas cumplen receta.id_expediente → expediente.id_paciente = A

### Casos borde
- Endpoint con path param de otro paciente: HTTP 403, no 404, para no habilitar enumeración de recursos

### Validaciones
- id_paciente: UUID v4 validado antes de consultar

### Notas de agente
> Implementar como global scope/policy en el repositorio, no solo en controladores.

---

## RF-003 — Verificación de autorización por capas (middlewares)

- **Tipo:** security
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `rbac`, `api-gateway`
- **Dependencias:** `PROV-SEC-002`
- **Referencias normativas:** OWASP-ASVS-L2, ISO/IEC-25010, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  El sistema DEBE tratar la ocultación de menús como mejora UX, no como control de seguridad (renderizado por rol con Laravel Spatie Permission). El middleware de la API DEBE validar token y permisos antes de cualquier transacción: HTTP 401 si falta token, es inválido o expiró; HTTP 403 si el token es válido y el rol o la propiedad del registro no autorizan la acción. Un segundo middleware DEBE validar la propiedad del registro (ej. receta solo modificable por su médico emisor) antes de llegar a la base de datos.

### Criterios de aceptación

**Escenario 1: Sin token**
- **Dado** una petición sin cabecera Authorization
- **Cuando** GET /api/v1/expedientes-clinicos/{id}
- **Entonces** HTTP 401 y cero consultas SQL ejecutadas sobre expediente

**Escenario 2: Token expirado**
- **Dado** un JWT con exp < now()
- **Cuando** cualquier endpoint protegido
- **Entonces** HTTP 401

**Escenario 3: Receta de otro médico**
- **Dado** un MEDICO válido distinto del emisor
- **Cuando** PATCH /api/v1/recetas/{id}
- **Entonces** HTTP 403 emitido por el middleware de propiedad; 0 filas actualizadas; 1 fila de auditoría resultado='DENEGADO'

### Casos borde
- Cliente envía rol en el cuerpo: el valor se ignora; el rol sale solo del token

### Validaciones
- orden de middlewares: autenticación → rol/permiso → propiedad → controlador

### Notas de agente
> No confiar en el frontend. Pruebas de contrato obligatorias para 401 vs 403 (ASVS L2).

---

## PROV-SEC-001 — Autenticación y sesiones

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `auth`
- **Dependencias:** `RF-002`, `RNF-008`
- **Referencias normativas:** ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-04, MS-05
- **Descripción implementable:**
  El sistema DEBE autenticar al personal con número de credencial y contraseña. El sistema DEBE exigir MFA a ADMINISTRADOR, Seguridad de la Información, DevOps y toda cuenta con acceso a producción. Los mensajes de fallo DEBEN ser genéricos. La sesión web DEBE cerrarse a los 15 min de inactividad. Se PROHÍBEN cuentas genéricas o compartidas.

### Criterios de aceptación

**Escenario 1: MFA obligatorio**
- **Dado** un ADMINISTRADOR con credenciales correctas y MFA sin verificar
- **Cuando** POST /api/v1/auth/login
- **Entonces** responde HTTP 200 con mfa_required=true y sin access_token; solo POST /api/v1/auth/mfa/verify válido emite access_token

**Escenario 2: Inactividad web**
- **Dado** una sesión web sin peticiones durante 15 min
- **Cuando** se envía cualquier petición protegida
- **Entonces** HTTP 401 y el refresh_token queda revocado

### Casos borde
- Error de autenticación: mismo cuerpo y mismo tiempo de respuesta ±100 ms para usuario inexistente y contraseña errónea

### Validaciones
- inactividad web: 900 s

### Notas de agente
> Los códigos de error no deben distinguir 'usuario no existe' de 'contraseña incorrecta'.

---

## PROV-SEC-002 — Tokens de acceso y sesión (JWT)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `auth`
- **Dependencias:** `PROV-SEC-001`
- **Referencias normativas:** ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02, R-07 · **Métricas:** MS-04
- **Descripción implementable:**
  El sistema DEBE emitir JWT firmados RS256 (o ES256) con vigencia máxima de 15 min (exp - iat <= 900) y refresh tokens rotativos. El sistema DEBE invalidar (blacklist) el token al cerrar sesión, cambiar contraseña o suspender la cuenta. La app móvil DEBE usar OAuth 2.0 con PKCE (ver carpeta mobile).

### Criterios de aceptación

**Escenario 1: Vigencia del JWT**
- **Dado** un login exitoso
- **Cuando** se decodifica el access_token
- **Entonces** alg ∈ {RS256, ES256} y exp - iat <= 900

**Escenario 2: Revocación al logout**
- **Dado** un access_token vigente
- **Cuando** POST /api/v1/auth/logout y luego una petición protegida con ese token
- **Entonces** la segunda petición responde HTTP 401

### Casos borde
- Refresh token reutilizado: HTTP 401 y se revocan todos los tokens de la familia

### Validaciones
- alg: RS256|ES256
- exp-iat <= 900

### Notas de agente
> Llaves de firma en bóveda de secretos, rotación semestral.

---

## PROV-SEC-004 — Gestión de secretos

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `infra`, `ci-cd`
- **Dependencias:** ninguna
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-06, R-08 · **Métricas:** MS-03
- **Descripción implementable:**
  El sistema NO DEBE contener contraseñas, llaves o certificados en código, imágenes de contenedor ni archivos versionados. El pipeline CI DEBE ejecutar escaneo de secretos en cada integración y bloquear las que expongan credenciales. Las credenciales de cuentas de servicio DEBEN rotarse como máximo cada 90 días.

### Criterios de aceptación

**Escenario 1: Secreto expuesto**
- **Dado** un commit que contiene una llave de API
- **Cuando** corre el pipeline CI
- **Entonces** el job 'secret-scan' falla y la integración queda bloqueada

**Escenario 2: Repositorio limpio**
- **Dado** la rama main
- **Cuando** se ejecuta el escáner completo
- **Entonces** 0 hallazgos

### Casos borde
- Archivo .env versionado: el pipeline falla

### Validaciones
- rotación de credenciales de servicio <= 90 días

### Notas de agente
> Separación de funciones: Sec administra llaves maestras; DevOps las usa; nadie con acceso admin a la BD accede a llaves de cifrado de datos.

---

## PROV-RN-001 — Gobernanza ágil, ramas y revisión por pares

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `gobernanza`
- **Dependencias:** ninguna
- **Referencias normativas:** ISO/IEC/IEEE-29148, ISO/IEC-27001
- **Riesgos mitigados:** R-11 · **Métricas:** MP-08, MP-05
- **Descripción implementable:**
  El equipo DEBE operar Scrum (PO, Scrum Master, Dev), estimar con Planning Poker (Fibonacci 1,2,3,5,8,13,21) y aplicar la Definición de Terminado (criterios de aceptación, pruebas unitarias e integración en Staging, Pull Request con >= 1 revisor distinto del autor, integración sin conflictos, 0 defectos críticos/altos, documentación actualizada). Las ramas main y develop DEBEN estar protegidas; el trabajo ocurre en feature/<nombre>. Versionado SemVer vX.Y.Z.

### Criterios de aceptación

**Escenario 1: Merge sin revisión**
- **Dado** una PR sin aprobación de un segundo miembro
- **Cuando** se intenta fusionar a develop
- **Entonces** la plataforma de código rechaza el merge

**Escenario 2: Push directo**
- **Dado** un push a main
- **Cuando** se ejecuta
- **Entonces** es rechazado por regla de protección de rama

### Casos borde
- Hardcoding de secretos detectado en revisión: la PR se rechaza

### Validaciones
- MP-08 = 100% de PR con revisor distinto del autor

### Notas de agente
> Repos: backend (Laravel + migraciones), frontend web (Vue), móvil (Kotlin). Ver SUP-001 sobre carpetas web/mobile.

---

## PROV-RN-002 — Matriz de trazabilidad y Product Backlog

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `gobernanza`
- **Dependencias:** `PROV-RN-001`
- **Referencias normativas:** ISO/IEC/IEEE-29148, ISO/IEC/IEEE-29119
- **Riesgos mitigados:** R-11 · **Métricas:** MC-02
- **Descripción implementable:**
  El equipo DEBE mantener el Product Backlog (PB-1.1 a PB-6.3) y una matriz Requisito → Historia → commit → PR → caso de prueba → resultado, muestreable por el Auditor (control Tabla 54).

### Criterios de aceptación

**Escenario 1: Rastreo completo**
- **Dado** una muestra de 5 requisitos
- **Cuando** el Auditor sigue la cadena
- **Entonces** cada uno tiene historia, commit, PR, caso de prueba y resultado

**Escenario 2: Requisito sin caso de prueba**
- **Dado** un RF-xxx sin caso asociado
- **Cuando** se genera la matriz
- **Entonces** la matriz marca la fila como 'SIN_CASO' y bloquea el AI correspondiente

### Casos borde
- Requisito con ID provisional: la matriz lo marca como PROV

### Validaciones
- MC-02 = requisitos con criterio verificado / comprometidos

### Notas de agente
> Generar la matriz desde TASK_BACKLOG.json.

---

## PROV-SEC-003 — Ciclo de vida de cuentas de usuario

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 1 / 1 / E1 / AI-01
- **Módulos afectados:** `usuarios`, `auth`
- **Dependencias:** `RF-001`
- **Referencias normativas:** ISO/IEC-27001, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  El sistema DEBE suspender automáticamente cuentas sin uso durante 90 días, ejecutar bajas en <= 24 h, revocar permisos anteriores al cambiar de área o función y generar la lista de cuentas para revisión trimestral (privilegiadas mensual).

### Criterios de aceptación

**Escenario 1: Suspensión por inactividad**
- **Dado** una cuenta con ultimo_acceso_en < now() - 90 días
- **Cuando** corre el job diario de cuentas
- **Entonces** estado_cuenta='SUSPENDIDA' y 1 fila de auditoría de tipo 'SUSPENSION_INACTIVIDAD'

**Escenario 2: Cambio de rol**
- **Dado** un usuario con rol MEDICO
- **Cuando** un ADMINISTRADOR lo cambia a RECEPCION
- **Entonces** los permisos MEDICO quedan revocados antes de asignar los nuevos y los tokens vigentes se invalidan

### Casos borde
- Cuenta de baja: su id_usuario no puede reasignarse

### Validaciones
- inactividad: 90 días
- baja: <= 24 h

### Notas de agente
> Job programado; no ejecutar bajas físicas.

---

# STAGE 2 · Sprint 2 · Infraestructura Docker, Base de Datos y Trazabilidad · Épica E2 · Auditoría AI-02 · 23 Sep – 08 Oct 2026

## RNF-001 — Trazabilidad y auditoría (logs inalterables)

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `auditoria`, `db`
- **Dependencias:** `PROV-SEC-005`
- **Referencias normativas:** NOM-024-SSA3-2012
- **Riesgos mitigados:** R-03 · **Métricas:** MN-04
- **Descripción implementable:**
  El sistema DEBE registrar en AUDITORIA_I toda operación de escritura, actualización o cambio de estado de paciente, y todo movimiento de entrada/salida, con encadenamiento SHA-256 (cada registro incorpora huella_integridad del anterior). Cada registro DEBE incluir fecha_hora UTC, tipo_movimiento, id_usuario y motivo (justificación clínica o administrativa).

### Criterios de aceptación

**Escenario 1: Registro encadenado**
- **Dado** dos escrituras consecutivas sobre triage
- **Cuando** se leen las dos últimas filas de AUDITORIA_I
- **Entonces** fila[n].huella_integridad = SHA256(campos canónicos de fila[n] || fila[n-1].huella_integridad); ambas con fecha_hora en UTC

**Escenario 2: Alteración detectada**
- **Dado** una fila de AUDITORIA_I modificada fuera de la aplicación
- **Cuando** corre la verificación de integridad
- **Entonces** la función auditoria.verificar_cadena() retorna ok=false con id_auditoria del primer registro roto y se dispara alerta

### Casos borde
- Primer registro: usa prev = 64 ceros hexadecimales (génesis)

### Validaciones
- huella_integridad: CHAR(64) hexadecimal

### Notas de agente
> Ver DOMAIN_SCHEMA.json (AUDITORIA_I, 13 columnas). Ver AMB-004 sobre el actor id_usuario.

---

## RNF-002 — Retención documental y persistencia

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `db`, `retencion`
- **Dependencias:** `RNF-001`
- **Referencias normativas:** NOM-004-SSA3-2012
- **Riesgos mitigados:** R-05, R-03 · **Métricas:** MN-05
- **Descripción implementable:**
  El motor de BD DEBE rechazar toda eliminación física (DELETE/TRUNCATE) de expediente_c, notas, resultados y recetas por cualquier rol de aplicación, incluso administrador. La baja DEBE ser lógica con estado_activo=false. La retención mínima es 5 años (1 825 días) desde fecha_ultimo_acto_medico.

### Criterios de aceptación

**Escenario 1: DELETE bloqueado**
- **Dado** un expediente con antigüedad < 1 825 días
- **Cuando** se ejecuta DELETE FROM expediente_c WHERE id_expediente=… desde el contenedor PostgreSQL
- **Entonces** el 100% de los intentos fallan con error de trigger/constraint (SQLSTATE P0001 o 23xxx)

**Escenario 2: Baja lógica**
- **Dado** un expediente activo
- **Cuando** POST /api/v1/expedientes-clinicos/{id}/baja-logica
- **Entonces** estado_activo=false, el registro sigue existiendo y 1 fila de auditoría tipo_movimiento='SOFT_DELETE'

### Casos borde
- TRUNCATE: también rechazado (REVOKE + trigger)

### Validaciones
- retención: fecha_ultimo_acto_medico + 1825 días

### Notas de agente
> Ver AMB-005 (conflicto con la supresión segura posterior a 5 años).

---

## RNF-003 — Seguridad de la información en red y reposo

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`, `db`
- **Dependencias:** `PROV-RNF-001`
- **Referencias normativas:** ISO/IEC-27001, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-07, R-05 · **Métricas:** MS-01
- **Descripción implementable:**
  El sistema DEBE cifrar datos en tránsito con TLS >= 1.2 (preferente 1.3, HSTS >= 1 año) y en reposo con AES-256 en volúmenes; adjuntos con AES-256-GCM a nivel de aplicación. El contenedor de BD DEBE operar aislado, sin conexiones desde internet público.

### Criterios de aceptación

**Escenario 1: Puerto de BD no expuesto**
- **Dado** el entorno desplegado
- **Cuando** se intenta conectar al puerto de PostgreSQL desde fuera de la red privada
- **Entonces** la conexión es rechazada/timeout

**Escenario 2: Solo TLS**
- **Dado** la plataforma web y la API
- **Cuando** se intenta HTTP plano y TLS 1.1
- **Entonces** HTTP plano redirige a HTTPS o se rechaza; TLS <= 1.1 se rechaza en el handshake

### Casos borde
- Conexión backend–BD: usa TLS con verificación completa de certificado

### Validaciones
- MS-01 = 100% de conexiones >= TLS 1.2

### Notas de agente
> Ver AMB-001: Supabase DEBE desplegarse en Docker dentro de la red del hospital.

---

## RNF-004 — Alta disponibilidad y aislamiento de fallos

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`
- **Dependencias:** `PROV-RNF-001`
- **Referencias normativas:** ISO/IEC-25010
- **Riesgos mitigados:** R-04 · **Métricas:** MC-06, MC-07
- **Descripción implementable:**
  Ante falla del contenedor web, el contenedor de BD DEBE permanecer intacto y el orquestador DEBE restablecer el servicio web en < 5 min (RTO contenedor <= 5 min). La API DEBE correr en >= 2 réplicas detrás de balanceador interno con health-checks.

### Criterios de aceptación

**Escenario 1: Caída del contenedor web**
- **Dado** producción con 2 réplicas de API
- **Cuando** se detiene manualmente un contenedor de API
- **Entonces** las peticiones se atienden por la réplica restante con HTTP 200 y la réplica caída reinicia en < 300 s

**Escenario 2: BD intacta**
- **Dado** el contenedor web apagado
- **Cuando** se reinicia el servicio web
- **Entonces** la BD no presenta corrupción (pg_amcheck sin errores) y el servicio vuelve en < 300 s

### Casos borde
- Pico de carga: se levantan réplicas adicionales sin degradar la BD

### Validaciones
- RTO contenedor <= 300 s
- MTTR <= 30 min

### Notas de agente
> Medir con prueba de recuperación en 3 ejecuciones independientes.

---

## PROV-RNF-001 — Infraestructura Docker endurecida y paridad de entornos

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`, `ci-cd`
- **Dependencias:** ninguna
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-04, R-06 · **Métricas:** MC-11
- **Descripción implementable:**
  El sistema DEBE desplegar backend, frontend y BD en contenedores Docker con imágenes oficiales con versión fija (prohibido 'latest'), usuarios no privilegiados, límites de CPU/RAM, secretos inyectados en runtime y red privada aislada. Los entornos Desarrollo, Pruebas y Producción DEBEN usar la misma imagen.

### Criterios de aceptación

**Escenario 1: Imágenes fijadas**
- **Dado** los Dockerfile y docker-compose
- **Cuando** se analizan estáticamente
- **Entonces** 0 ocurrencias de ':latest'; todos los servicios con user no root

**Escenario 2: Paridad**
- **Dado** la misma imagen etiquetada vX.Y.Z
- **Cuando** se despliega en los 3 entornos
- **Entonces** MC-11 = 100% de despliegues homogéneos

### Casos borde
- Imagen con CVE crítico: el pipeline bloquea el despliegue

### Validaciones
- CPU/RAM con límites definidos

### Notas de agente
> Docker Compose por entorno. Producción en el servidor del hospital (acceso directo de desarrolladores prohibido).

---

## PROV-SEC-005 — Garantías de inalterabilidad de AUDITORIA_I

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `auditoria`, `db`
- **Dependencias:** `RNF-001`
- **Referencias normativas:** NOM-024-SSA3-2012, ISO/IEC-27001
- **Riesgos mitigados:** R-03 · **Métricas:** MN-04
- **Descripción implementable:**
  AUDITORIA_I DEBE residir en un esquema independiente cuyo propietario no sea el usuario de la aplicación; el usuario de aplicación DEBE tener solo INSERT (UPDATE, DELETE, TRUNCATE revocados) y triggers DEBEN rechazar modificaciones. Los registros DEBEN replicarse periódicamente a almacenamiento externo de solo escritura; la cadena DEBE verificarse diariamente; el registro NO DEBE contener contraseñas, tokens, llaves ni OTP.

### Criterios de aceptación

**Escenario 1: UPDATE rechazado**
- **Dado** el rol sih_app
- **Cuando** ejecuta UPDATE auditoria.auditoria_i SET motivo='x'
- **Entonces** error permission denied o excepción de trigger; 0 filas afectadas

**Escenario 2: Superusuario altera la cadena**
- **Dado** una fila modificada por superusuario
- **Cuando** corre la verificación diaria
- **Entonces** MN-04 < 100% y alerta crítica a Seguridad en < 5 min

### Casos borde
- Consulta a auditoría: genera su propio evento

### Validaciones
- GRANT sih_app: INSERT únicamente

### Notas de agente
> Probar con usuario de privilegios elevados en Staging (Tabla 54, control 'Logs inalterables').

---

## PROV-RN-003 — Ciclo de retención y disposición final

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `retencion`, `db`
- **Dependencias:** `RNF-002`
- **Referencias normativas:** NOM-004-SSA3-2012, LFPDPPP
- **Riesgos mitigados:** R-10 · **Métricas:** MN-05
- **Descripción implementable:**
  El sistema DEBE calcular la vigencia de retención de cada expediente a partir de fecha_ultimo_acto_medico (mínimo 5 años; triage y consentimientos igual al expediente; auditoría igual al expediente; ARCO 5 años desde resolución; logs de infraestructura 1 año). Mensualmente DEBE generar la relación de expedientes con plazo concluido sin ejecutar acciones automáticas; la supresión requiere validación jurídica, bloqueo y acta.

### Criterios de aceptación

**Escenario 1: Relación mensual**
- **Dado** expedientes con fecha_ultimo_acto_medico + 1825 d < hoy
- **Cuando** corre el job mensual
- **Entonces** genera un reporte y no modifica ni borra ninguna fila

**Escenario 2: Actualización del último acto médico**
- **Dado** un expediente activo
- **Cuando** se inserta una nota nueva
- **Entonces** fecha_ultimo_acto_medico se actualiza al timestamp de la nota

### Casos borde
- Expediente bloqueado: no puede consultarse ni tratarse

### Validaciones
- plazo mínimo 1825 días

### Notas de agente
> La supresión física queda fuera de este sprint: ver AMB-005.

---

## PROV-SEC-006 — Respaldos 3-2-1 y archivado continuo WAL

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`, `db`
- **Dependencias:** `PROV-RNF-001`
- **Referencias normativas:** ISO/IEC-27001
- **Riesgos mitigados:** R-05 · **Métricas:** MC-06, MC-07
- **Descripción implementable:**
  El sistema DEBE ejecutar respaldo completo diario cifrado AES-256, enviar WAL en tiempo real a bucket externo y alertar si el lag excede 10 min. Retención: diarios 30 días, mensuales 12 meses. Regla 3-2-1 con copia inmutable fuera de sitio. DevOps DEBE ejecutar simulacros mensuales de restauración en Staging (RTO desastre <= 4 h).

### Criterios de aceptación

**Escenario 1: Alerta de lag**
- **Dado** el lag de copia WAL > 600 s
- **Cuando** corre el monitor
- **Entonces** se emite alerta crítica a DevOps

**Escenario 2: Restauración mensual**
- **Dado** un respaldo + WAL
- **Cuando** se restaura en Staging
- **Entonces** la BD arranca sin archivos corruptos y el tiempo total <= 4 h

### Casos borde
- Llaves de respaldo: independientes de producción y fuera del sitio

### Validaciones
- retención 30 d / 12 m

### Notas de agente
> Ver R-05.

---

## PROV-SEC-007 — Sincronización de tiempo y UTC

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`, `db`
- **Dependencias:** ninguna
- **Referencias normativas:** CENAM/NTP, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-01 · **Métricas:** MN-01
- **Descripción implementable:**
  Todos los servidores y contenedores DEBEN sincronizarse por NTP con fuentes referidas a CENAM con deriva <= 1 s. Todas las marcas de tiempo DEBEN almacenarse en UTC (timestamptz) y mostrarse en hora local del hospital.

### Criterios de aceptación

**Escenario 1: Deriva**
- **Dado** un contenedor del backend
- **Cuando** se mide offset contra la fuente NTP
- **Entonces** |offset| <= 1 s

**Escenario 2: Almacenamiento UTC**
- **Dado** cualquier columna de fecha/hora
- **Cuando** se consulta information_schema
- **Entonces** tipo = timestamp with time zone y el valor se guarda en UTC

### Casos borde
- Deriva > 1 s: alerta de monitoreo

### Validaciones
- offset <= 1 s

### Notas de agente
> Los temporizadores de triage y la validez de e.firma dependen de esto.

---

## PROV-SEC-010 — Entornos y datos de prueba sintéticos

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 2 / 2 / E2 / AI-02
- **Módulos afectados:** `infra`, `db`
- **Dependencias:** `PROV-RNF-001`
- **Referencias normativas:** ISO/IEC-27001, LFPDPPP
- **Riesgos mitigados:** R-08 · **Métricas:** MS-02
- **Descripción implementable:**
  Los entornos de Desarrollo y Pruebas DEBEN operar solo con datos sintéticos o disociados; está PROHIBIDO copiar datos reales de pacientes fuera de producción. Los desarrolladores NO DEBEN tener acceso a datos de pacientes en producción.

### Criterios de aceptación

**Escenario 1: Datos reales en Staging**
- **Dado** la BD de Staging
- **Cuando** se ejecuta el script de verificación de PII real
- **Entonces** 0 coincidencias con CURP válidas de producción

**Escenario 2: Acceso de desarrollador a producción**
- **Dado** un usuario de rol DEV
- **Cuando** se conecta a la BD de producción
- **Entonces** la conexión es denegada y queda auditada

### Casos borde
- Respaldo de producción: no se restaura en Staging sin disociación

### Validaciones
- datos sintéticos únicamente fuera de producción

### Notas de agente
> Seeders con Faker en es_MX.

---

# STAGE 3 · Sprint 3 · Triage, Urgencias y Admisión · Épica E3 · Auditoría AI-03 · 09 Oct – 22 Oct 2026

## RF-004 — Captura obligatoria de signos vitales

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`, `pacientes`
- **Dependencias:** `RF-002`, `RNF-001`
- **Referencias normativas:** NOM-206-SSA1-2002, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-01 · **Métricas:** MC-01
- **Descripción implementable:**
  El sistema DEBE registrar presión arterial (mmHg), frecuencia cardíaca (lpm), glucosa (mg/dL), estimación de sangre perdida (categórica), nivel de consciencia (AVDI) y escala EVA de dolor, bloqueando el avance del registro hasta capturar los campos obligatorios. Solo el rol ENFERMERIA con sesión activa PUEDE crear un registro TRIAGE. Cada captura DEBE conservarse en historial para evaluar la evolución.

### Criterios de aceptación

**Escenario 1: Captura completa**
- **Dado** un ENFERMERIA autenticado con paciente en camilla
- **Cuando** POST /api/v1/triages con presion_art='120/80', frecuencia_card=80, nivel_glucosa=95, nivel_conciencia='ALERTA', escala_eva=0
- **Entonces** HTTP 201; 1 fila en triage con color_asig calculado y fecha_hora_ingreso en UTC; 1 fila de auditoría tipo_movimiento='INSERT' tabla_afectada='triage'

**Escenario 2: Campo obligatorio faltante**
- **Dado** un ENFERMERIA autenticado
- **Cuando** POST /api/v1/triages sin nivel_conciencia
- **Entonces** HTTP 422 con error en nivel_conciencia y 0 filas insertadas

**Escenario 3: Rol no permitido**
- **Dado** un token con rol=RECEPCION
- **Cuando** POST /api/v1/triages
- **Entonces** HTTP 403

### Casos borde
- frecuencia_card=999 (fuera de rango fisiológico): HTTP 422 'valor fuera de rango' (caso TC-TRG-005)
- nivel_glucosa omitido: permitido (columna NULL) por Tabla 26

### Validaciones
- presion_art: VARCHAR(10) formato 'sis/dia'
- frecuencia_card: INTEGER
- nivel_glucosa: INTEGER nullable
- nivel_conciencia ∈ {ALERTA,VERBAL,DOLOR,INCONSCIENTE}
- escala_eva: ver AMB-007

### Notas de agente
> Rangos fisiológicos, categorías de sangre perdida y umbrales críticos NO están definidos: ver AMB-006. Prototipo: Anexo G.1.

---

## RF-005 — Asignación automática de gravedad (triage)

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`
- **Dependencias:** `RF-004`, `PROV-RN-004`
- **Referencias normativas:** NOM-206-SSA1-2002
- **Riesgos mitigados:** R-01 · **Métricas:** MC-01
- **Descripción implementable:**
  El sistema DEBE calcular el color_asig con el motor de reglas Manchester. Si nivel_conciencia='INCONSCIENTE', el sistema DEBE asignar ROJO ignorando los demás parámetros. Ningún usuario PUEDE asignar un nivel inferior al calculado por el motor.

### Criterios de aceptación

**Escenario 1: Inconsciente fuerza Rojo**
- **Dado** un ENFERMERIA procesando triage
- **Cuando** POST /api/v1/triages con nivel_conciencia='INCONSCIENTE' y signos normales
- **Entonces** triage.color_asig='ROJO'; la interfaz emite alerta visual sin recargar (evento recibido en <= 2 s)

**Escenario 2: Degradación manual bloqueada**
- **Dado** un triage en ROJO
- **Cuando** se intenta establecer color_asig='AMARILLO' sin ser reclasificación autorizada
- **Entonces** la acción se bloquea con HTTP 403/422 y se registra en AUDITORIA_I (caso TC-TRG-004)

### Casos borde
- Alerta y notificación: médico de guardia notificado (caso TC-TRG-001)

### Validaciones
- color_asig ∈ {ROJO,NARANJA,AMARILLO,VERDE,AZUL}

### Notas de agente
> Matriz clínica completa Tabla 25 pendiente de reglas numéricas: AMB-006. El sistema es apoyo a decisión, no sustituye valoración clínica.

---

## RF-006 — Gestión y distribución de colas de espera

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`, `colas`, `salas`
- **Dependencias:** `RF-005`
- **Referencias normativas:** NOM-206-SSA1-2002
- **Riesgos mitigados:** R-01, R-04 · **Métricas:** MN-01
- **Descripción implementable:**
  El sistema DEBE gestionar colas separadas para emergencias y consultorios. Para casos ROJO DEBE saltar la cola y asignar automáticamente una de las 3 salas de emergencia (apartando cama). Para pacientes regulares DEBE dirigir al siguiente consultorio liberado y actualizar la pantalla de sala de espera sin recarga.

### Criterios de aceptación

**Escenario 1: Caso crítico salta cola**
- **Dado** 3 salas de emergencia con >= 1 libre
- **Cuando** se registra un triage ROJO
- **Entonces** se asigna una sala libre y el paciente aparece en cola de emergencia con posición 0

**Escenario 2: Liberación de consultorio**
- **Dado** un paciente regular en espera
- **Cuando** el médico libera el consultorio (POST /api/v1/salas/{id}/liberar)
- **Entonces** la pantalla de sala de espera muestra el siguiente paciente en <= 2 s sin recarga

### Casos borde
- Sin sala de emergencia libre: alerta crítica a Jefatura y el paciente queda primero en cola de emergencia

### Validaciones
- salas de emergencia = 3

### Notas de agente
> Orden de cola: nivel (1→5) y luego hora de arribo.

---

## RF-007 — Reclasificación manual con justificación clínica

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`, `auditoria`
- **Dependencias:** `RF-005`, `RNF-001`
- **Referencias normativas:** NOM-206-SSA1-2002, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-01, R-03 · **Métricas:** MN-03
- **Descripción implementable:**
  El sistema DEBE permitir modificar manualmente el nivel de urgencia solo a usuarios con privilegios superiores. Al solicitar el cambio DEBE exigir texto de justificación y iniciar un temporizador de 30 min en el BACKEND (no solo en la interfaz). Si vence sin justificación, DEBE marcar es_necesario_auditoria_interna=true. DEBE registrar fecha, hora, credencial, valor anterior y valor nuevo.

### Criterios de aceptación

**Escenario 1: Justificación obligatoria**
- **Dado** un usuario autorizado con triage en AMARILLO
- **Cuando** POST /api/v1/triages/{id}/reclasificaciones con motivo vacío
- **Entonces** HTTP 422 y color_asig sin cambio

**Escenario 2: Reclasificación válida**
- **Dado** un usuario autorizado
- **Cuando** POST con nuevo_color='NARANJA' y motivo='deterioro hemodinámico'
- **Entonces** HTTP 201; 1 fila de auditoría con valor_anterior='AMARILLO', valor_nuevo='NARANJA', motivo no vacío

**Escenario 3: Vencimiento de 30 min**
- **Dado** una reclasificación sin justificación capturada
- **Cuando** transcurren 1 800 s
- **Entonces** el job backend marca es_necesario_auditoria_interna=true con id_usuario y fecha_hora (MN-03 cuenta como no conforme)

### Casos borde
- Degradar ROJO: bloqueado (TC-TRG-004)

### Validaciones
- motivo: obligatorio, VARCHAR(500)

### Notas de agente
> NC-001 del Anexo C: el temporizador DEBE vivir en backend. Quién tiene 'privilegios superiores': AMB-008.

---

## PROV-RN-004 — Matriz Manchester y SLA por nivel

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`
- **Dependencias:** ninguna
- **Referencias normativas:** NOM-206-SSA1-2002
- **Riesgos mitigados:** R-01 · **Métricas:** MN-01, MC-01
- **Descripción implementable:**
  El sistema DEBE aplicar tiempos máximos de espera: ROJO atención inmediata (sin espera programada, apartar cama), NARANJA 15 min, AMARILLO 60 min, VERDE 120 min, AZUL 240 min, y mostrar temporizador por paciente.

### Criterios de aceptación

**Escenario 1: Temporizadores**
- **Dado** triage NARANJA registrado a las 10:00:00Z
- **Cuando** se consulta sla_vencimiento_en
- **Entonces** = 10:15:00Z

**Escenario 2: Valores por nivel**
- **Dado** los 5 niveles
- **Cuando** se consulta la tabla de configuración
- **Entonces** ROJO=0, NARANJA=15, AMARILLO=60, VERDE=120, AZUL=240 minutos

### Casos borde
- ROJO: estado 'ATENCION_INMEDIATA'; sla_vencimiento_en = fecha_hora_ingreso

### Validaciones
- minutos por nivel configurables, versionados y auditados

### Notas de agente
> Cambios a las reglas de triage son evento de auditoría administrativo (Tabla 45).

---

## PROV-RN-005 — Gestión de tiempos excedidos

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`, `alertas`
- **Dependencias:** `PROV-RN-004`
- **Referencias normativas:** NOM-206-SSA1-2002, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-01 · **Métricas:** MN-02
- **Descripción implementable:**
  Al llegar a 0 un temporizador sin atención registrada, el sistema NO DEBE bloquearse; DEBE escalar una alerta visual crítica a Jefatura de Urgencias y registrar la desviación en BD (triage.sla_vencido=true, desviación en minutos) para métricas y auditorías.

### Criterios de aceptación

**Escenario 1: Vencimiento de SLA**
- **Dado** triage VERDE sin atención a los 120 min
- **Cuando** vence el temporizador
- **Entonces** alerta visible en pantalla de Jefatura en <= 5 s y 1 fila de auditoría evento 'SLA_VENCIDO'

**Escenario 2: Sistema operativo tras vencimiento**
- **Dado** varios SLA vencidos
- **Cuando** se consulta GET /api/v1/colas-atencion
- **Entonces** HTTP 200 (el sistema no se bloquea)

### Casos borde
- Recarga de pantalla: la alerta persiste

### Validaciones
- MN-02 = 100% vencidos escalados

### Notas de agente
> Job de backend, no timer de UI.

---

## PROV-UX-001 — Captura de triage y alertas en tiempo real

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** ux
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage-ui`
- **Dependencias:** `RF-004`, `RF-005`
- **Referencias normativas:** WCAG-2.1-AA, NOM-206-SSA1-2002
- **Riesgos mitigados:** R-01 · **Métricas:** MC-05
- **Descripción implementable:**
  La UI de triage DEBE bloquear el botón de avance hasta completar los campos obligatorios, validar rangos en cliente y servidor, y mostrar alertas de cambio de color sin recargar la página (WebSocket con fallback a polling, ver SUP-006).

### Criterios de aceptación

**Escenario 1: Botón bloqueado**
- **Dado** formulario incompleto
- **Cuando** el usuario intenta enviar
- **Entonces** el botón está deshabilitado y los campos faltantes marcados con aria-invalid=true

**Escenario 2: Alerta en vivo**
- **Dado** otra estación registra un ROJO
- **Cuando** pasan <= 2 s
- **Entonces** la pantalla de médico muestra la alerta sin recarga

### Casos borde
- Pérdida de conexión: banner persistente 'sin conexión' y reintento

### Validaciones
- validación en cliente y servidor

### Notas de agente
> Operable 100% por teclado.

---

## PROV-BR-001 — Codificación semántica de color de triage

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** branding
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `design-tokens`
- **Dependencias:** `PROV-RN-004`
- **Referencias normativas:** WCAG-2.1-AA, NOM-206-SSA1-2002
- **Riesgos mitigados:** R-01 · **Métricas:** MC-05
- **Descripción implementable:**
  Los niveles DEBEN representarse con nombre de color semántico (Rojo, Naranja, Amarillo, Verde, Azul) más icono y texto; nunca solo color. Los valores hexadecimales no están definidos en el ERS.

### Criterios de aceptación

**Escenario 1: Redundancia de codificación**
- **Dado** cualquier badge de nivel
- **Cuando** se inspecciona el DOM
- **Entonces** contiene texto del nivel y un icono además del color

**Escenario 2: Valor hex**
- **Dado** el archivo de tokens
- **Cuando** se revisa
- **Entonces** los hex figuran como NO_DEFINIDO hasta decisión del PO (AMB-010)

### Casos borde
- Modo alto contraste: se mantiene icono+texto

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> No inventar paleta.

---

## PROV-RN-006 — Tolerancia a fallos de red y captura retroactiva de folios

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage`, `infra`
- **Dependencias:** `RF-004`
- **Referencias normativas:** NOM-206-SSA1-2002, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-04 · **Métricas:** MN-04
- **Descripción implementable:**
  En apagón total, el sistema DEBE permitir la captura retroactiva de folios manuales (papel) conservando cronológicamente el timestamp del arribo físico original, sin alterar la integridad de los logs; las transacciones activas DEBEN aislarse ante caídas o picos.

### Criterios de aceptación

**Escenario 1: Captura retroactiva**
- **Dado** folio en papel con arribo 08:15 y captura a las 09:40
- **Cuando** POST /api/v1/triages con captura_retroactiva=true y fecha_hora_ingreso='08:15'
- **Entonces** fecha_hora_ingreso='08:15'; AUDITORIA_I conserva fecha_hora real de captura 09:40 y motivo 'CAPTURA_RETROACTIVA'

**Escenario 2: Sin captura retroactiva**
- **Dado** un usuario sin permiso
- **Cuando** envía captura_retroactiva=true
- **Entonces** HTTP 403

### Casos borde
- Timestamp futuro: HTTP 422

### Validaciones
- fecha_hora_ingreso <= now()

### Notas de agente
> Permiso de captura retroactiva: AMB-008.

---

## PROV-RN-007 — Paciente no identificado y fusión autorizada

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `pacientes`
- **Dependencias:** `RF-004`
- **Referencias normativas:** NOM-004-SSA3-2012, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-01 · **Métricas:** MN-05
- **Descripción implementable:**
  Ante paciente inconsciente o no identificado, el sistema DEBE generar un UUID temporal y un folio de urgencias, con estado 'DESCONOCIDO'. Al confirmar la identidad, DEBE permitir la fusión autorizada y registrada sin pérdida de información.

### Criterios de aceptación

**Escenario 1: Alta temporal**
- **Dado** un paciente sin CURP en estado INCONSCIENTE
- **Cuando** POST /api/v1/pacientes con identificado=false
- **Entonces** se crea paciente.id_paciente (UUID) y folio_urgencias; el triage se registra

**Escenario 2: Fusión**
- **Dado** un registro temporal y uno definitivo
- **Cuando** POST /api/v1/pacientes/{id}/fusiones con motivo
- **Entonces** los datos clínicos se enlazan al definitivo, el temporal queda inactivo y 1 fila de auditoría 'FUSION_PACIENTES' con motivo

### Casos borde
- CURP duplicada: HTTP 409 y propuesta de fusión

### Validaciones
- CURP: VARCHAR(18) válida por formato

### Notas de agente
> Fusión: evento de auditoría con justificación obligatoria.

---

## PROV-UX-002 — Pantalla de sala de espera con privacidad

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** ux
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `sala-espera-ui`
- **Dependencias:** `RF-006`
- **Referencias normativas:** LFPDPPP, WCAG-2.1-AA
- **Riesgos mitigados:** R-02 · **Métricas:** MN-05
- **Descripción implementable:**
  Las pantallas de sala de espera y colas DEBEN mostrar folio o iniciales, sin nombre completo ni datos clínicos (anonimización parcial).

### Criterios de aceptación

**Escenario 1: Contenido permitido**
- **Dado** la pantalla de sala de espera
- **Cuando** se inspecciona el payload de GET /api/v1/sala-espera
- **Entonces** contiene solo folio, iniciales, color+icono+texto y posición; 0 campos nombre_completo/diagnostico/curp

**Escenario 2: Redirección**
- **Dado** un consultorio liberado
- **Cuando** se actualiza la cola
- **Entonces** el siguiente folio se destaca sin recarga

### Casos borde
- Iniciales repetidas: se desambigua con folio

### Validaciones
- sin PII en pantalla pública

### Notas de agente
> Sin notas adicionales.

---

## PROV-UX-003 — Accesibilidad WCAG 2.1 AA

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** ux
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `ui-global`
- **Dependencias:** ninguna
- **Referencias normativas:** WCAG-2.1-AA, ISO/IEC-25010
- **Riesgos mitigados:** R-01 · **Métricas:** MC-05
- **Descripción implementable:**
  Las interfaces DEBEN cumplir WCAG 2.1 AA: contraste >= 4.5:1, operabilidad 100% por teclado en captura clínica (Triage y Recetas), etiquetado semántico de formularios y nunca comunicar el nivel de urgencia solo con color.

### Criterios de aceptación

**Escenario 1: Contraste**
- **Dado** cualquier texto normal
- **Cuando** se mide con axe-core
- **Entonces** ratio >= 4.5:1

**Escenario 2: Teclado**
- **Dado** pantallas de Triage y Recetas
- **Cuando** se completa el flujo solo con teclado
- **Entonces** tarea completada sin ratón

### Casos borde
- Simulación de daltonismo: el nivel sigue siendo perceptible por icono y texto

### Validaciones
- 0 incidencias bloqueantes/críticas AA

### Notas de agente
> Verificación formal en Stage 5.

---

## PROV-UI-001 — Pantalla de captura de triage (Anexo G.1)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** ui
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 3 / 3 / E3 / AI-03
- **Módulos afectados:** `triage-ui`
- **Dependencias:** `PROV-UX-001`
- **Referencias normativas:** WCAG-2.1-AA
- **Riesgos mitigados:** R-01 · **Métricas:** MC-05
- **Descripción implementable:**
  La UI web DEBE implementar el prototipo del Anexo G.1: captura de signos vitales, selector AVDI, selector EVA, indicador de color con icono+texto y temporizador. (El Anexo G contiene solo imágenes no legibles como texto: ver AMB-009.)

### Criterios de aceptación

**Escenario 1: Render del formulario**
- **Dado** un ENFERMERIA
- **Cuando** abre /triage/nuevo
- **Entonces** muestra los 6 campos obligatorios y el indicador de nivel

**Escenario 2: Estados vacío y error**
- **Dado** sin pacientes o con error
- **Cuando** se carga la pantalla
- **Entonces** muestra estado vacío o error con mensaje descriptivo

### Casos borde
- Pantalla < 1280 px: layout se adapta sin scroll horizontal en el formulario

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Vue.js con MVVM; componentes PascalCase.

---

# STAGE 4 · Sprint 4 · ECE, Recetas Controladas e Interoperabilidad · Épica E4 · Auditoría AI-04 · 23 Oct – 05 Nov 2026

## RF-008 — Gestión integral del expediente clínico electrónico (macro-requerimiento)

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece`
- **Dependencias:** `RF-008.1`, `RF-008.2`, `RNF-002`
- **Referencias normativas:** NOM-004-SSA3-2012, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-03, MN-05
- **Descripción implementable:**
  El sistema DEBE administrar de forma integral el historial médico del paciente (datos de identificación, notas de evolución, diagnósticos e interoperabilidad), con confidencialidad e integridad. Solo el rol MEDICO PUEDE crear/consultar contenido clínico; el acceso exige relación asistencial vigente o acceso de emergencia justificado.

### Criterios de aceptación

**Escenario 1: Médico con relación asistencial**
- **Dado** un MEDICO con el paciente en su cola
- **Cuando** GET /api/v1/expedientes-clinicos/{id}
- **Entonces** HTTP 200 y 1 fila de auditoría 'CONSULTA_EXPEDIENTE'

**Escenario 2: Sin relación asistencial**
- **Dado** un MEDICO sin relación con el paciente
- **Cuando** GET /api/v1/expedientes-clinicos/{id}
- **Entonces** HTTP 403 y sugerencia de acceso de emergencia (PROV-RN-010)

### Casos borde
- Sub-requisitos: se aprueba solo si RF-008.1 y RF-008.2 pasan (AMB-002 corrige la referencia RF-009.1/.2/.3)

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> El texto fuente cita RF-009.1, .2 y .3 pero define RF-008.1 y RF-008.2 (2 sub-requisitos): ver AMB-002.

---

## RF-008.1 — Creación y datos de identificación del expediente

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece`, `pacientes`
- **Dependencias:** `RF-008`
- **Referencias normativas:** NOM-004-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MN-05
- **Descripción implementable:**
  El sistema DEBE permitir crear el expediente solo con institución, médico tratante (con cédula) y datos del paciente (nombre, edad, representante legal). DEBE bloquear la creación si faltan campos obligatorios NOM-004 y si el paciente ya tiene expediente abierto (relación 1:1).

### Criterios de aceptación

**Escenario 1: Creación válida**
- **Dado** paciente existente sin expediente y MEDICO autenticado
- **Cuando** POST /api/v1/expedientes-clinicos con campos obligatorios
- **Entonces** HTTP 201; expediente_c.estado_activo=true y fecha_creacion en UTC

**Escenario 2: Campos faltantes**
- **Dado** la misma petición sin médico tratante
- **Cuando** POST
- **Entonces** HTTP 422 y 0 filas insertadas

**Escenario 3: Expediente duplicado**
- **Dado** paciente con expediente abierto
- **Cuando** POST
- **Entonces** HTTP 409

### Casos borde
- Rol ADMINISTRADOR: permitido solo con rol 'Médico' o 'Administración' según precondición; ver AMB-003

### Validaciones
- id_expediente: VARCHAR(10), PK
- UNIQUE(id_paciente)

### Notas de agente
> La precondición menciona 'Administración' pero Tabla 44 niega acceso clínico al Administrador: AMB-003.

---

## RF-008.2 — Historia clínica, interrogatorio y notas de evolución inmutables

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece`
- **Dependencias:** `RF-008.1`, `RF-009`
- **Referencias normativas:** NOM-004-SSA3-2012, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-03 · **Métricas:** MN-05
- **Descripción implementable:**
  El sistema DEBE capturar interrogatorio (AHF, APP, APNP, AGO), exploración física y diagnósticos, y notas de evolución/ingreso/egreso con fecha, hora y firma electrónica automáticas. Una nota guardada NO PUEDE editarse ni eliminarse; las correcciones se hacen con nota aclaratoria que conserva la original. La fecha_creacion es inmutable.

### Criterios de aceptación

**Escenario 1: Inmutabilidad**
- **Dado** una nota de evolución guardada
- **Cuando** PUT/PATCH/DELETE /api/v1/expedientes-clinicos/{id}/notas/{nid}
- **Entonces** HTTP 405 o 403; la fila no cambia (hash de contenido idéntico)

**Escenario 2: Nota aclaratoria**
- **Dado** una nota guardada
- **Cuando** POST .../notas/{nid}/aclaraciones
- **Entonces** se crea nueva nota vinculada y la original permanece intacta

**Escenario 3: Cierre con campos incompletos**
- **Dado** un expediente sin diagnóstico
- **Cuando** se intenta cerrar
- **Entonces** HTTP 422 con campos NOM-004 faltantes

### Casos borde
- Edición vía SQL: trigger rechaza UPDATE sobre notas cerradas

### Validaciones
- firma electrónica de la nota: TEXT

### Notas de agente
> Triggers de BD además de la API.

---

## RF-009 — Generación y emisión de recetas médicas

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `recetas`, `firma-electronica`
- **Dependencias:** `RF-008.1`, `PROV-RN-010`
- **Referencias normativas:** COFEPRIS/SAT, NOM-004-SSA3-2012, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-09 · **Métricas:** MN-06
- **Descripción implementable:**
  El sistema DEBE generar recetas vinculadas al expediente con datos del médico, del paciente, denominación genérica, dosis, vía, presentación y duración. Para Fracción I, II y III DEBE validar la e.firma (certificado X.509 vigente, revocación verificada contra el SAT) y generar un código QR único que se invalida tras el surtido. Fracción I: solo formato QR de un solo uso. Fracciones II/III: límite de cantidad por evento. DEBE bloquear la impresión del PDF si la e.firma no valida.

### Criterios de aceptación

**Escenario 1: Controlada con e.firma válida**
- **Dado** un MEDICO con certificado vinculado
- **Cuando** POST /api/v1/recetas fraccion='I' y POST /api/v1/recetas/{id}/firma válida
- **Entonces** receta.qr_estado='VIGENTE' con código único y PDF disponible

**Escenario 2: e.firma inválida**
- **Dado** un certificado revocado o no validable
- **Cuando** POST .../firma
- **Entonces** HTTP 422; GET .../pdf responde HTTP 409; 1 fila de auditoría

**Escenario 3: Reuso de QR**
- **Dado** un QR ya consumido
- **Cuando** POST /api/v1/recetas/{folio}/surtido
- **Entonces** HTTP 409, 0 cambios y evento 'INCIDENTE_SEGURIDAD_QR_REUSADO'

**Escenario 4: Rol no permitido**
- **Dado** ENFERMERIA
- **Cuando** POST /api/v1/recetas
- **Entonces** HTTP 403

### Casos borde
- Servicio SAT no disponible: no se emite receta controlada; procedimiento alterno con firma autógrafa (R-09)
- Segunda receta sobre el mismo folio: HTTP 409

### Validaciones
- folio único irrepetible
- firma_electronica: TEXT
- denominación genérica obligatoria

### Notas de agente
> Límites de cantidad por fracción y quién consume el QR no están definidos: AMB-011.

---

## RF-010 — Solicitudes y resultados de laboratorio e imagen

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `estudios`
- **Dependencias:** `RF-008.1`
- **Referencias normativas:** NOM-004-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-03
- **Descripción implementable:**
  El sistema DEBE permitir a MEDICO emitir solicitudes de estudio vinculadas al expediente, mostrarlas de inmediato en la bandeja del LABORATORISTA y permitir cargar y visualizar resultados (imágenes o PDF) desde el historial; adjuntos cifrados AES-256-GCM.

### Criterios de aceptación

**Escenario 1: Solicitud visible**
- **Dado** un MEDICO
- **Cuando** POST /api/v1/solicitudes-estudio
- **Entonces** HTTP 201 y aparece en GET /api/v1/solicitudes-estudio?estado=PENDIENTE del LABORATORISTA en <= 2 s

**Escenario 2: Carga de resultado**
- **Dado** un LABORATORISTA con solicitud pendiente
- **Cuando** POST .../resultados con PDF
- **Entonces** HTTP 201 y el MEDICO ve el archivo en el historial sin error

### Casos borde
- Archivo > límite: HTTP 413 (límite en AMB-012)
- Tipo no permitido: HTTP 415

### Validaciones
- tipos: PDF, imagen

### Notas de agente
> Tamaño máximo no definido: AMB-012.

---

## RNF-007 — Interoperabilidad semántica HL7 FHIR

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `fhir`
- **Dependencias:** `RF-008`, `RF-009`, `RF-004`
- **Referencias normativas:** HL7-FHIR-R4, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-04 · **Métricas:** MC-04
- **Descripción implementable:**
  La API DEBE exponer Patient (desde paciente), Encounter (desde triage), Observation (signos vitales) y MedicationRequest (desde receta) como JSON FHIR R4 válido sobre HTTPS, con estado de receta (activa, dispensada, cancelada) consistente con el módulo de recetas.

### Criterios de aceptación

**Escenario 1: Patient válido**
- **Dado** un paciente existente
- **Cuando** GET /api/v1/fhir/Patient/{id}
- **Entonces** JSON con resourceType='Patient', valida contra el esquema FHIR R4 y solo expone CURP, nombre y fecha de nacimiento

**Escenario 2: MedicationRequest**
- **Dado** una receta dispensada
- **Cuando** GET /api/v1/fhir/MedicationRequest/{id}
- **Entonces** status='completed'

### Casos borde
- Recurso inválido recibido: rechazo con OperationOutcome y evento de auditoría

### Validaciones
- validador de esquema FHIR en CI

### Notas de agente
> Excepción de nomenclatura: rutas FHIR en PascalCase (AMB-013).

---

## PROV-RN-008 — Gestión del consentimiento del paciente

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `consentimientos`
- **Dependencias:** `RF-008.1`
- **Referencias normativas:** LFPDPPP, NOM-004-SSA3-2012
- **Riesgos mitigados:** R-10 · **Métricas:** MN-05
- **Descripción implementable:**
  El sistema DEBE registrar el consentimiento como registro inmutable (id paciente, tipo, versión, finalidades, fecha/hora, medio, firmantes y testigos, hash del documento, estado). La revocación DEBE crear un registro nuevo vinculado sin eliminar el original y surtir efecto inmediato sin retroactividad. En urgencia sin consentimiento DEBE exigir justificación y firma de >= 2 médicos.

### Criterios de aceptación

**Escenario 1: Registro inmutable**
- **Dado** un consentimiento otorgado
- **Cuando** se intenta UPDATE/DELETE
- **Entonces** rechazado; hash_documento intacto

**Escenario 2: Revocación**
- **Dado** un consentimiento vigente
- **Cuando** POST /api/v1/consentimientos/{id}/revocaciones
- **Entonces** nuevo registro con estado='REVOCADO' y el original permanece

**Escenario 3: Urgencia**
- **Dado** paciente inconsciente sin representante
- **Cuando** se registra la excepción con 1 médico
- **Entonces** HTTP 422 (se exigen 2 médicos autorizados)

### Casos borde
- Menor de edad: se registra el representante legal con documento acreditante

### Validaciones
- hash_documento: SHA-256

### Notas de agente
> Finalidades secundarias nunca preseleccionadas.

---

## PROV-RN-009 — Derechos ARCO

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `arco`
- **Dependencias:** `PROV-RN-008`
- **Referencias normativas:** LFPDPPP, NOM-004-SSA3-2012
- **Riesgos mitigados:** R-10 · **Métricas:** MN-05
- **Descripción implementable:**
  El sistema DEBE registrar solicitudes ARCO con folio, fecha y derecho; acreditar identidad; responder en <= 20 días hábiles y ejecutar en <= 15 días hábiles posteriores (ampliables una vez). Rectificación de notas solo con nota aclaratoria. Cada etapa se audita.

### Criterios de aceptación

**Escenario 1: Plazos**
- **Dado** una solicitud ARCO recibida el día D
- **Cuando** se calcula el vencimiento
- **Entonces** respuesta_limite = D + 20 días hábiles; ejecucion_limite = respuesta + 15 días hábiles

**Escenario 2: Cancelación improcedente**
- **Dado** un expediente dentro de retención
- **Cuando** solicitud de cancelación
- **Entonces** se resuelve como improcedente con fundamento y queda auditada

### Casos borde
- Solicitud sin identidad acreditada: no avanza de estado

### Validaciones
- plazos en días hábiles

### Notas de agente
> Calendario de días hábiles: AMB-014.

---

## PROV-RN-010 — Acceso de emergencia al expediente

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece`, `auditoria`
- **Dependencias:** `RF-008`
- **Referencias normativas:** NOM-024-SSA3-2012, ISO/IEC-27001
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  Fuera de la relación asistencial, el sistema DEBE habilitar acceso de emergencia con justificación clínica obligatoria, tiempo limitado, es_necesario_auditoria_interna=true y notificación a Seguridad.

### Criterios de aceptación

**Escenario 1: Acceso con justificación**
- **Dado** un MEDICO sin relación
- **Cuando** POST /api/v1/accesos-emergencia con motivo
- **Entonces** HTTP 201, token de acceso temporal y 1 fila de auditoría con es_necesario_auditoria_interna=true

**Escenario 2: Sin justificación**
- **Dado** petición sin motivo
- **Cuando** POST
- **Entonces** HTTP 422

### Casos borde
- Expiración: al vencer el tiempo el acceso vuelve a 403

### Validaciones
- duración: ver AMB-015

### Notas de agente
> Alerta en tiempo real a Seguridad.

---

## PROV-SEC-008 — Privacidad por diseño y exportaciones

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece`, `exportaciones`
- **Dependencias:** `RF-008`
- **Referencias normativas:** LFPDPPP, ISO/IEC-27001
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  El sistema DEBE minimizar campos, enmascarar identificación en roles que no la requieren, limitar impresión/exportación a roles autorizados y registrar cada impresión/exportación.

### Criterios de aceptación

**Escenario 1: Enmascaramiento**
- **Dado** un rol sin necesidad de identificación completa
- **Cuando** GET paciente
- **Entonces** CURP y domicilio parcialmente enmascarados

**Escenario 2: Exportación auditada**
- **Dado** un MEDICO exporta un PDF
- **Cuando** POST /api/v1/expedientes-clinicos/{id}/exportaciones
- **Entonces** 1 fila de auditoría 'EXPORTACION_DOCUMENTO'

### Casos borde
- Rol no autorizado: HTTP 403

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-UI-002 — Pantallas de ECE y emisión de recetas (Anexo G.2)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** ui
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 4 / 4 / E4 / AI-04
- **Módulos afectados:** `ece-ui`, `recetas-ui`
- **Dependencias:** `RF-008`, `RF-009`
- **Referencias normativas:** WCAG-2.1-AA, NOM-004-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-05
- **Descripción implementable:**
  La UI web DEBE implementar el prototipo del Anexo G.2 (consulta del expediente, nota médica, receta con indicador de validación de e.firma y vista previa de PDF). Prototipo solo en imagen: AMB-009.

### Criterios de aceptación

**Escenario 1: Emisión controlada**
- **Dado** un MEDICO
- **Cuando** abre /recetas/nueva y elige Fracción I
- **Entonces** muestra paso de e.firma obligatorio y bloquea 'Imprimir' hasta validación

**Escenario 2: Nota inmutable**
- **Dado** una nota guardada
- **Cuando** se abre en la UI
- **Entonces** los botones editar/eliminar no existen; solo 'Agregar aclaración'

### Casos borde
- Teclado: 100% operable por teclado

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Vue.js; ver UI_UX_SPEC.md.

---

# STAGE 5 · Sprint 5 · Portal Móvil, Estadística y QA Integral · Épica E5/E6 · Auditoría AI-05 · 06 Nov – 19 Nov 2026

## RF-011 — Acceso de solo lectura para la aplicación móvil (lado API)

- **Tipo:** functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `api-mobile`, `rbac`
- **Dependencias:** `RF-002.1`, `RF-003`, `RF-009`, `RF-010`, `PROV-RF-002`
- **Referencias normativas:** NOM-024-SSA3-2012, OWASP-ASVS-L2
- **Riesgos mitigados:** R-02 · **Métricas:** MS-05
- **Descripción implementable:**
  La API DEBE exponer bajo /api/v1/mobile/* únicamente endpoints de lectura de citas, recetas y resultados para el rol PACIENTE y rechazar con HTTP 403 todo POST/PUT/PATCH/DELETE originado de un token de paciente sobre recursos clínicos. Solo se admiten las escrituras no clínicas de la lista blanca definida en SUP-002. Los tokens de personal NO PUEDEN usar /api/v1/mobile/*.

### Criterios de aceptación

**Escenario 1: Escritura clínica rechazada**
- **Dado** un token de PACIENTE
- **Cuando** POST/PUT/DELETE /api/v1/mobile/recetas
- **Entonces** HTTP 403 (o 405) y 0 filas modificadas

**Escenario 2: Lectura permitida**
- **Dado** un token de PACIENTE
- **Cuando** GET /api/v1/mobile/citas, /recetas, /resultados
- **Entonces** HTTP 200 solo con registros propios

**Escenario 3: Token de personal en móvil**
- **Dado** un token de rol MEDICO
- **Cuando** GET /api/v1/mobile/recetas
- **Entonces** HTTP 403

### Casos borde
- Escritura no clínica: POST /api/v1/mobile/consentimientos y /solicitudes-arco permitidos (lista blanca SUP-002)

### Validaciones
- lista blanca de escrituras: 5 endpoints

### Notas de agente
> Conflicto RF-011 vs consentimiento/ARCO/logout: AMB-016.

---

## RNF-005 — Eficiencia y tiempo de respuesta del ECE

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E6 / AI-05
- **Módulos afectados:** `ece`, `db`, `pruebas`
- **Dependencias:** `RF-008`, `RNF-004`
- **Referencias normativas:** ISO/IEC-25010
- **Riesgos mitigados:** R-04 · **Métricas:** MC-03
- **Descripción implementable:**
  Las lecturas y escrituras del ECE y las consultas críticas (búsqueda de expediente, registro de triage, recetas activas) DEBEN responder con P95 <= 2.0 s medido en servidor bajo carga pico; en soak de 8 h el P95 no degrada más de 20%.

### Criterios de aceptación

**Escenario 1: Carga pico**
- **Dado** perfil de carga pico simulado
- **Cuando** se ejecuta la prueba
- **Entonces** P95 <= 2.0 s

**Escenario 2: Resistencia**
- **Dado** 8 h de carga moderada continua
- **Cuando** se compara P95 final vs inicial
- **Entonces** degradación <= 20% y sin fugas de memoria

### Casos borde
- Estrés: degradación controlada, no caída total

### Validaciones
- P95 <= 2.0 s

### Notas de agente
> Este requisito atraviesa etapas; su verificación formal ocurre en PB-6.2.

---

## PROV-RNF-002 — Estrategia integral de pruebas (QA)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E6 / AI-05
- **Módulos afectados:** `pruebas`
- **Dependencias:** `RNF-005`, `RF-005`
- **Referencias normativas:** ISO/IEC/IEEE-29119, ISO/IEC-25010, WCAG-2.1-AA
- **Riesgos mitigados:** R-01, R-04 · **Métricas:** MC-01, MC-10, MC-05
- **Descripción implementable:**
  QA DEBE ejecutar pruebas funcionales, de integración/API (contrato OpenAPI, FHIR, concurrencia, aislamiento de red), rendimiento/estrés/soak, compatibilidad (Chrome, Firefox, Edge en Windows, versión vigente y anterior) y accesibilidad WCAG 2.1 AA, con cobertura >= 80% de código en módulos clínicos.

### Criterios de aceptación

**Escenario 1: Cobertura**
- **Dado** el módulo clínico
- **Cuando** se mide con análisis estático
- **Entonces** MC-10 >= 80%

**Escenario 2: Triage 100%**
- **Dado** la matriz AVDI × 5 niveles
- **Cuando** se ejecuta
- **Entonces** 100% de casos aprobados (MC-01)

### Casos borde
- Fallo de Triage o QR reusable: bloqueante; suspende el paso de fase

### Validaciones
- MC-01 = 100%
- MC-10 >= 80%

### Notas de agente
> Evidencias según ISO 29119-3.

---

## PROV-SEC-009 — Gestión de vulnerabilidades, SBOM y pruebas de seguridad

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** security
- **Prioridad:** P0 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E6 / AI-05
- **Módulos afectados:** `ci-cd`, `pruebas`
- **Dependencias:** `PROV-SEC-004`
- **Referencias normativas:** ISO/IEC-27001, OWASP-ASVS-L2
- **Riesgos mitigados:** R-06 · **Métricas:** MS-03, MS-04, MS-01
- **Descripción implementable:**
  El pipeline DEBE ejecutar SAST en cada integración, SCA y escaneo de imágenes en cada build, DAST previo a cada liberación y pentest (PTES) previo a producción; generar SBOM por release. CVSS crítica (9.0-10) se remedia en 72 h (mitigación en 24 h); alta (7.0-8.9) en 7 días; ambas bloquean la liberación.

### Criterios de aceptación

**Escenario 1: Bloqueo por CVE crítica**
- **Dado** una dependencia con CVSS >= 9.0 sin remediar
- **Cuando** corre el pipeline
- **Entonces** el job falla y la liberación se bloquea (MS-03 > 0)

**Escenario 2: SBOM**
- **Dado** un release vX.Y.Z
- **Cuando** se revisa el artefacto
- **Entonces** existe SBOM con librerías, versión y licencia

### Casos borde
- CVSS media: permitida con plan de remediación en 30 días

### Validaciones
- MS-03 = 0 al liberar

### Notas de agente
> Pruebas de bloqueo 3er intento: MS-04 = 100%.

---

## PROV-RF-001 — Tableros estadísticos y exportación (NOM-035)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** functional
- **Prioridad:** P1 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E6 / AI-05
- **Módulos afectados:** `dashboards`, `reportes`
- **Dependencias:** `RF-004`, `RF-008`
- **Referencias normativas:** NOM-035-SSA3-2012, LFPDPPP
- **Riesgos mitigados:** R-10 · **Métricas:** MN-09
- **Descripción implementable:**
  El sistema DEBE generar tableros y exportaciones estadísticas con datos disociados y agregados (sin identificadores directos), usando el identificador CLUES del establecimiento. Solo ADMINISTRADOR (y roles autorizados por Tabla 44) PUEDEN verlos.

### Criterios de aceptación

**Escenario 1: Sin PII**
- **Dado** el reporte NOM-035 generado
- **Cuando** se inspecciona la exportación
- **Entonces** 0 columnas de nombre_completo, curp, domicilio; incluye clues

**Escenario 2: Generación exitosa**
- **Dado** un reporte programado
- **Cuando** corre la generación
- **Entonces** MN-09: terminó sin error y quedó 1 fila de auditoría

### Casos borde
- Celda con n < umbral: se suprime/agrupa (umbral en AMB-017)

### Validaciones
- clues: obligatorio

### Notas de agente
> Frecuencia de Dashboards y formato de exportación: AMB-017.

---

## PROV-RF-002 — Agenda y citas (consulta y confirmación)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** functional
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E6 / AI-05
- **Módulos afectados:** `agenda`
- **Dependencias:** `RF-002`
- **Referencias normativas:** NOM-024-SSA3-2012
- **Riesgos mitigados:** R-02 · **Métricas:** MC-02
- **Descripción implementable:**
  El sistema DEBE permitir a RECEPCION confirmar citas y a PACIENTE consultarlas. El ERS incluye 'Admisión y agenda hospitalaria' en el alcance pero no define un RF formal para agenda ni su modelo de datos.

### Criterios de aceptación

**Escenario 1: Confirmación**
- **Dado** un RECEPCION
- **Cuando** PATCH /api/v1/citas/{id}/confirmacion
- **Entonces** cita.estado='CONFIRMADA' y 1 fila de auditoría

**Escenario 2: Consulta del paciente**
- **Dado** un PACIENTE
- **Cuando** GET /api/v1/mobile/citas
- **Entonces** solo sus citas

### Casos borde
- Cita pasada: no confirmable (HTTP 422)

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Campos de cita derivados, marcar ⚠️ en DOMAIN_SCHEMA.json: AMB-018.

---

## RNF-006 — Compatibilidad de la aplicación móvil (lado API)

- **Tipo:** non_functional
- **Prioridad:** P1 _(base: ERS: Media)_
- **Stage / Sprint / Épica / Auditoría:** 5 / 5 / E5 / AI-05
- **Módulos afectados:** `api-mobile`
- **Dependencias:** `RF-011`
- **Referencias normativas:** ISO/IEC-25010
- **Riesgos mitigados:** R-04 · **Métricas:** MC-05
- **Descripción implementable:**
  La API DEBE aceptar el cliente móvil solo con User-Agent/cabecera X-Client-Version >= soporte Android 10 (API 29) y responder HTTP 426 con mensaje controlado a versiones no soportadas, sin exponer datos técnicos ni clínicos.

### Criterios de aceptación

**Escenario 1: Versión soportada**
- **Dado** cliente declarando api_level=29
- **Cuando** GET /api/v1/mobile/perfil
- **Entonces** HTTP 200

**Escenario 2: Versión no soportada**
- **Dado** cliente declarando api_level=28
- **Cuando** GET /api/v1/mobile/perfil
- **Entonces** HTTP 426 con mensaje genérico de incompatibilidad

### Casos borde
- Cabecera ausente: HTTP 400

### Validaciones
- api_level >= 29

### Notas de agente
> La compatibilidad en dispositivos se verifica en la carpeta mobile.

---

# STAGE 6 · Sprint RELEASE · Quality Gate y Despliegue (Release Final) · Épica E6 · Auditoría AI-06 · 20 Nov – 25 Nov 2026

## PROV-RNF-003 — Despliegue inmutable y plan de reversión

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** non_functional
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `deploy`
- **Dependencias:** `PROV-RN-011`, `RNF-004`
- **Referencias normativas:** ISO/IEC-25010, ISO/IEC-27001
- **Riesgos mitigados:** R-04 · **Métricas:** MC-06, MC-07, MP-06, MC-11
- **Descripción implementable:**
  El despliegue DEBE ser inmutable (prohibido modificar código en el servidor), con tag SemVer, imágenes versionadas, Rolling Update o Blue-Green con draining y cero tiempo de inactividad perceptible. El rollback a la versión estable anterior DEBE restablecer el servicio en <= 5 min; las migraciones DEBEN ser retrocompatibles con down-migrations.

### Criterios de aceptación

**Escenario 1: Rolling update**
- **Dado** versión v1.0.1 en producción
- **Cuando** se despliega v1.1.0
- **Entonces** 0 peticiones fallidas durante el despliegue y disponibilidad >= 99.9%

**Escenario 2: Rollback**
- **Dado** una falla crítica post-liberación
- **Cuando** se reactiva la imagen v1.0.1
- **Entonces** servicio restablecido en <= 300 s

### Casos borde
- Migración sin retrocompatibilidad: el pipeline la rechaza

### Validaciones
- rollback <= 300 s

### Notas de agente
> Solo el PO aprueba producción y rollback (RACI).

---

## PROV-RN-011 — Criterios de liberación (Quality Gate)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P0 _(base: ERS)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `quality-gate`
- **Dependencias:** `PROV-RNF-002`, `PROV-SEC-009`
- **Referencias normativas:** ISO/IEC-25010, ISO/IEC-27001
- **Riesgos mitigados:** R-01, R-03, R-06 · **Métricas:** MC-01, MC-02, MP-01, MS-01, MS-03, MS-05, MC-10, MP-08, MN-04
- **Descripción implementable:**
  Ningún incremento (incluido el release 20-25 nov) DEBE liberarse si no cumple: MC-01=100% y MC-02=100%; MP-01=0 y 0 defectos críticos/altos abiertos; MS-03=0, MS-01=100% y MS-05=0; MC-10>=80% y MP-08=100%; MN-04=100% en Pruebas; 0 no conformidades críticas/mayores abiertas; acta de liberación firmada por QA, Seguridad y PO. (Homologación: MC-08=MS-01, MC-09=MS-02, MP-07=MS-03, MN-07=MS-04, MN-08=MS-05.)

### Criterios de aceptación

**Escenario 1: Gate verde**
- **Dado** todas las métricas del gate en meta
- **Cuando** se evalúa el gate
- **Entonces** gate.estado='APROBADO' y se habilita el despliegue

**Escenario 2: Gate rojo**
- **Dado** MS-03 = 1
- **Cuando** se evalúa el gate
- **Entonces** gate.estado='RECHAZADO' y el pipeline de despliegue falla

### Casos borde
- Métrica de tolerancia cero en rojo: cualquier desviación = estado rojo (Tabla 52)

### Validaciones
- 7 grupos de criterios

### Notas de agente
> Implementar como job del pipeline que consulta las métricas.

---

## PROV-RNF-004 — Operación, monitoreo y continuidad (BCP/DRP)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** non_functional
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `monitoreo`
- **Dependencias:** `PROV-RNF-003`
- **Referencias normativas:** ISO/IEC-25010, NOM-024-SSA3-2012
- **Riesgos mitigados:** R-04 · **Métricas:** MC-06, MC-07
- **Descripción implementable:**
  El sistema DEBE monitorear CPU, RAM y latencia de la API; al superar 85% de capacidad de un contenedor DEBE escalar. La API y la BD DEBEN generar error logs con fecha, hora, código HTTP y endpoint. Existe plan BCP/DRP con servidor secundario.

### Criterios de aceptación

**Escenario 1: Escalamiento**
- **Dado** un contenedor > 85% CPU
- **Cuando** corre el monitor
- **Entonces** se levanta una réplica adicional y se registra el evento

**Escenario 2: Error log**
- **Dado** un HTTP 500
- **Cuando** se revisa el log
- **Entonces** contiene fecha, hora, código HTTP y endpoint

### Casos borde
- Caída total del servidor: BCP levanta contenedores en servidor secundario

### Validaciones
- umbral 85%

### Notas de agente
> Sin notas adicionales.

---

## PROV-RN-013 — Evidencia auditable por punto de control AI-01 a AI-06

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P1 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `auditoria-interna`
- **Dependencias:** `PROV-RN-002`
- **Referencias normativas:** ISO/IEC/IEEE-29148
- **Riesgos mitigados:** R-03, R-11 · **Métricas:** MN-04, MC-02
- **Descripción implementable:**
  Al cierre de cada sprint (22 sep, 08 oct, 22 oct, 05 nov, 19 nov) y pre-release (20-24 nov) el sistema DEBE producir el paquete de evidencia del Auditor Interno: AI-01 documental; AI-02 infraestructura y trazabilidad; AI-03 triage y urgencias; AI-04 ECE, recetas e interoperabilidad; AI-05 seguridad; AI-06 liberación. Evidencia solo en Staging.

### Criterios de aceptación

**Escenario 1: Paquete AI-02**
- **Dado** fin del Sprint 2
- **Cuando** se genera el paquete
- **Entonces** incluye scripts de BD, bitácora del pipeline y resultado de prueba de inalterabilidad de logs

**Escenario 2: Hallazgo**
- **Dado** un incumplimiento
- **Cuando** se registra
- **Entonces** se crea una no conformidad con folio NC-xxx y clasificación

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---

## PROV-RN-012 — Gestión de defectos y cambios post-liberación (RFC)

⚠️ AMBIGUOUS_ID — ID provisional; ver AMBIGUITY_REPORT en AGENTS.md

- **Tipo:** business_rule
- **Prioridad:** P2 _(base: SUP-004)_
- **Stage / Sprint / Épica / Auditoría:** 6 / RELEASE / E6 / AI-06
- **Módulos afectados:** `soporte`
- **Dependencias:** `PROV-RNF-003`
- **Referencias normativas:** ISO/IEC-27001, ISO/IEC/IEEE-29148
- **Riesgos mitigados:** R-11 · **Métricas:** MP-06, MP-01
- **Descripción implementable:**
  Los defectos DEBEN clasificarse: Crítico P1 (caída total, BD inaccesible, fallo de cálculo de triage), Alto P2 (recetas, comunicación móvil), Medio/Bajo P3. Todo cambio post-liberación DEBE entrar por RFC (Anexo F) con análisis de impacto y aprobación del PO.

### Criterios de aceptación

**Escenario 1: Clasificación**
- **Dado** un fallo de cálculo de triage
- **Cuando** se registra
- **Entonces** severidad='CRITICO' y prioridad 1

**Escenario 2: Cambio informal**
- **Dado** una petición verbal
- **Cuando** se intenta implementar
- **Entonces** se rechaza; solo procede con RFC aprobada

### Casos borde
- Sin casos borde adicionales derivables del ERS.

### Validaciones
- Ver criterios y reglas de dominio en DOMAIN_SCHEMA.json.

### Notas de agente
> Sin notas adicionales.

---
