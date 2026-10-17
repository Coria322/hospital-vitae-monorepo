# AGENTS.md — SIH Hospital Vitae (Gobernanza General del Proyecto)

> Documento Maestro de Lineamientos, Reglas de Agentes y Control de Cambios.
> Aplica para todo el ecosistema: `vitae-backend`, `vitae-front`, `vitae-mobile` y `vitae-bruno`.

---

## 1. REGLAS GLOBALES Y OBLIGATORIAS DE AGENTES

### 📌 Regla de Fin de Sprint / Feature y Commits
1. **Verificación de Pruebas Obligatoria:** NO realizar commit si no se ha pasado previamente por el 100% de las pruebas automatizadas (PHPUnit, Jest/Vitest, Bruno CLI, según aplique).
2. **Autorización Previa:** SIEMPRE solicitar autorización expresa del usuario antes de realizar cualquier commit.
3. **Mapa de Trabajo:** Al solicitar autorización, generar y presentar un mapa detallado y estructurado de todo lo construido o modificado.
4. **Recordatorio de Commit:** Al terminar un sprint o feature, recordar inmediatamente al usuario realizar el commit antes de iniciar cualquier nueva tarea o feature.

### 📌 Regla de Mantenimiento de la Matriz de Trazabilidad
- Al concluir cada Sprint o Feature, el agente DEBE actualizar obligatoriamente el archivo [`matriz_trazabilidad.md`](./matriz_trazabilidad.md).
- **Indicación de Tachado:** Todo requerimiento funcional (RF), no funcional (RNF) o norma cumplida DEBE tacharse visualmente mediante sintaxis markdown (`~~RF-XXX~~: ~~Descripción~~~`) y marcarse con el indicador `✅ Cumplido (Sprint N)`.
- Ningún Sprint puede declararse cerrado sin haber aplicado el tachado y validado la correspondencia con los criterios de aceptación y normas del ERS.

---

## 2. ESTRATEGIA Y FLUJO DE TRABAJO GIT

### 🔀 Modelo de Ramas y Políticas de Integración
```
feature/TASK-XXX  ──(Pull Request / Merge)──>  dev (develop)  ──(Git Rebase)──>  main (producción)
```

1. **Ramas Feature:**
   - Todo desarrollo parte de `dev` con el formato `feature/<nombre-feature>` o `fix/<nombre-fix>`.
2. **Integración en Dev (`merge`):**
   - El **`merge`** va **ÚNICAMENTE hacia la rama `dev`** (mediante Pull Request y revisión por pares).
   - En `dev` se integra, valida y despliegan los ambientes de pruebas locales y staging.
3. **Integración en Main (`rebase`):**
   - **`main` SE REBASARÁ (`git rebase`), NUNCA SE HARÁ `merge` DIRECTO A `main`.**
   - Para llevar cambios a producción:
     ```bash
     git checkout main
     git fetch origin
     git rebase dev
     git push origin main
     ```
   - Esto garantiza un historial completamente lineal, sin commits de merge huérfanos en la rama productiva, facilitando auditorías forenses y bisecciones (`git bisect`).

### 📝 Estándar de Commits (Conventional Commits)
Formato estricto:
```
<tipo>(<alcance>): <descripción imperativa en español>

[cuerpo explicativo opcional]

Refs: #TASK-W-XXX, Req: RF-XXX, Audit: AI-XX
```
- **Tipos permitidos:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- **Ejemplo:**
  ```text
  feat(auth): implementar endpoints y rbac para sprint 1 (AI-01)

  - Creada tabla usuario con id_usuario VARCHAR(10) y soporte Bcrypt cost 12
  - Configurados middlewares VerifyAccountStatus y RoleAuthorization
  - Creada suite de pruebas en Bruno CLI (13 requests) y PHPUnit (8 tests)

  Refs: #TASK-W-004, #TASK-W-005, Req: RF-001, RF-002, Audit: AI-01
  ```

---

## 3. ARQUITECTURA Y ESTÁNDARES TÉCNICOS

### Stack Oficial
- **Backend:** Laravel 11 (LTS), API REST bajo prefijo `/api/v1`, Laravel Sanctum (Tokens Bearer).
- **Base de Datos:** PostgreSQL 15+ (Supabase) con esquema `public` endurecido.
- **Frontend Web:** Vue.js (SFC, MVVM), Vite, Pinia.
- **App Móvil:** Android Nativo (Kotlin, Jetpack Compose, minSdk 29).
- **Pruebas de API:** Bruno CLI (`bru run`) y Bruno Desktop v4 (formato OpenCollection `.yml` y legacy `.bru`).

### Invariantes de Seguridad y Normatividad
- **NOM-004-SSA3-2012:** Prohibido el `DELETE` físico en registros clínicos y usuarios (`baja` lógica con `estado_cuenta = 'BAJA'`). Retención mínima de 5 años.
- **NOM-024-SSA3-2012:** Trazabilidad absoluta en `AUDITORIA_I` inalterable con encadenamiento SHA-256.
- **NOM-206-SSA1-2002:** Triage Manchester de 5 niveles con alertas de SLA estrictas.
- **ISO/IEC 27001 & OWASP ASVS L2:**
  - Contraseñas con factor de costo Bcrypt >= 12.
  - Respuestas HTTP estándar: `401 Unauthorized` (falta sesión o token inválido) vs `403 Forbidden` (rol sin privilegios suficientes).
  - Bloqueo de cuenta tras 3 intentos fallidos de inicio de sesión en ventana de 60 minutos.
  - Secretos y variables maestras exclusivamente en `.env`.
