# Contexto del proyecto: Hospital Vitae (monorepo SIH)

> Resumen de contexto para desarrolladores y agentes. Fuente principal: [README.md](README.md) (rama `dev`, commit `197fa40`, 2026-11-03). Las secciones marcadas **[verificado en código]** se contrastaron con el repo; el resto es lo que afirma el README.
> Reglas de Git y commits: [AGENTS.md](AGENTS.md). Especificación completa: [sih-hospitalario_agents/](sih-hospitalario_agents/).

## Qué es

Sistema de Información Hospitalaria (SIH) del **Hospital Vitae**. Proyecto académico de **Auditoría de Software (9no semestre)**, diseñado con foco en auditoría, trazabilidad forense e interoperabilidad clínica. Integra:

- API REST (backend)
- Plataforma web clínica y administrativa (frontend)
- App móvil nativa Android para pacientes
- Colección de pruebas E2E (Bruno)
- Matrices de cumplimiento normativo

Repositorio: https://github.com/Coria322/hospital-vitae-monorepo

## Componentes

| Carpeta | Qué es | Stack | Despliegue |
|---|---|---|---|
| `vitae-backend/` | API REST, lógica de negocio, triage, bitácora forense | PHP 8.2+, Laravel 12, Sanctum, PostgreSQL 16 / SQLite, Docker endurecido (usuario `www-data`, puertos 8080/10000) | Render (Docker). Prod: `https://auditoria-backend-u47a.onrender.com/api/v1`, health: `/up` |
| `vitae-front/` | Web clínica y administrativa, navegación por roles, WCAG 2.1 AA | Vue 3 (Composition API, `<script setup>`), Vite, TypeScript, Vue Router, Axios | Vercel |
| `vitae-mobile/` | App Android del paciente (citas y recetas) | Kotlin, Jetpack Compose, Material 3, Gradle KTS, SDK 35 | Local (Android Studio) |
| `vitae-bruno/` | Pruebas E2E de la API | Bruno / Bruno CLI | `test-prod-suite.ps1` |
| `sih-hospitalario_agents/` | Especificación, matrices de cumplimiento y trazabilidad (`matriz_trazabilidad.md`) | Markdown / JSON | n/a |
| `matrices-web/` | Portal estático para ver las matrices normativas | HTML | n/a |
| `.agents/rules/` | Reglas de agentes y matriz de cumplimiento | Markdown | n/a |

Los tres clientes (web, móvil, Bruno) consumen el mismo backend por HTTPS/JSON.

## Marco normativo (qué exige y cómo se implementa)

| Norma | Alcance | Implementación |
|---|---|---|
| NOM-024-SSA3-2012 | Registro electrónico en salud | Bitácora inalterable `AUDITORIA_I` de 13 columnas, SHA-256 encadenado en UTC, triggers que bloquean UPDATE, DELETE y TRUNCATE |
| NOM-004-SSA3-2012 | Expediente clínico electrónico | Retención 5 años (1825 días), sin borrado físico, fecha del último acto médico, notas selladas tras la firma |
| NOM-206-SSA1-2002 | Urgencias y triage | Manchester de 5 niveles determinista, paciente inconsciente fuerza Rojo, SLAs, distribución de salas |
| ISO/IEC 27001 | Seguridad | AES-256-GCM en adjuntos, TLS >= 1.2 a la BD, HSTS (31,5 M s), CSP estricto, JWT de 15 min, respaldos 3-2-1 |
| ISO/IEC 25010 | Calidad y alta disponibilidad | Docker con límites de recursos, réplicas >= 2, salud en `/up` y `/api/v1/salud` |
| LFPDPPP | Datos personales | Monitores de sala de espera sin PII, datos sintéticos, accesos auditados |
| HL7 FHIR R4 | Interoperabilidad | Recursos `Patient`, `Encounter`, `Observation`, `MedicationRequest` |
| COFEPRIS / SAT | Receta electrónica | Folio único, validación de cédula, sello SHA-256, QR verificable |

## Arquitectura (resumen)

- **Cliente → API Gateway (Laravel 12) → módulos:** autenticación y seguridad (Sanctum, MFA, rotación de token a 15 min), triage y urgencias (Manchester), bitácora inalterable (`AuditLogger`), ECE y recetas.
- **Persistencia:** BD transaccional (PostgreSQL 16, cifrada) y tabla `AUDITORIA_I` de solo inserción.
- **Flujo de auditoría:** el usuario actúa en el front, la API valida token, rol y sesión, ejecuta la operación y pide al `AuditLogger` un registro con hash SHA-256 encadenado al eslabón previo. Se hace INSERT en `AUDITORIA_I` y los triggers impiden modificarlo.

## Roles de la web (`vitae-front`)

- **Médico:** espacio ECE, notas clínicas, recetas, solicitud de estudios, cola.
- **Enfermería:** box de triage, evaluación Manchester, reclasificación, monitoreo de sala.
- **Recepción:** búsqueda fonética / RENAPO de CURP, expedientes, citas, censo de camas.
- **Administración:** usuarios, desbloqueo de cuentas, monitoreo Docker, consulta de la bitácora.
- **Laboratorista:** banco de estudios, captura y validación de resultados, rechazo de muestras.

## API actual **[verificado en código]** (`vitae-backend/routes/api.php`, prefijo `/api/v1`)

- **Públicas:** `POST /auth/login`, `GET /salud`, `GET /sistema/tiempo`, `GET /sala-espera`, `GET /triage/stream`.
- **Con `auth:sanctum` + `account.active`:** `/auth/logout`, `/auth/refresh`, `/auth/me`, `/pacientes` (alta, listado, detalle), `/triages` y `/triages/cola`, `/triages/{id}`, `/salas`.
- **Por rol:**
  - `POST /pacientes/{id}/fusiones`: MEDICO, ADMINISTRADOR.
  - `POST /triages`: ENFERMERIA, ADMINISTRADOR.
  - `POST /triages/{id}/reclasificaciones`: MEDICO, ADMINISTRADOR.
  - `POST /salas/{id}/liberar`: MEDICO, ADMINISTRADOR, ENFERMERIA.
  - `/usuarios` (alta, baja, reactivar) y `GET /auditoria`: solo ADMINISTRADOR. `/auditoria` rechaza escrituras con 405.
- **Aún no hay endpoints** de ECE, recetas, FHIR ni nada exclusivo de móvil (`/api/v1/mobile`).
- Quedan rutas de prueba fuera de `/v1`: `/api/ping`, `/api/hello` y `apiResource('auditorias')`.

## Pruebas E2E (`vitae-bruno`)

Suites `01-Auth`, `02-Usuarios`, `03-Seguridad-RBAC`, `04-Logout`, `05-Auditoria-I`, `06-Infraestructura-y-Tiempo`, `07-Triage-Manchester`. `test-prod-suite.ps1` autentica cada rol contra producción, corre las 7 suites y escribe `bruno_prod_results.log`.

## Cómo correr

```bash
# Backend
cd vitae-backend && composer install && cp .env.example .env && php artisan key:generate && php artisan migrate --seed && php artisan serve
php artisan test

# Frontend
cd vitae-front && npm install
npm run dev            # desarrollo
npm run type-check     # vue-tsc
npm run test           # sesión y autenticación
npm run build-only     # producción

# Móvil: abrir vitae-mobile en Android Studio Ladybug o superior, sincronizar Gradle y ejecutar

# Pruebas en producción (PowerShell)
cd vitae-bruno && powershell -ExecutionPolicy Bypass -File ./test-prod-suite.ps1
```

## Estado de sprints

| Sprint | Tema | Estado |
|---|---|---|
| 1 | Base y autenticación (monorepo, Sanctum, modelos) | Completado |
| 2 | Infraestructura y auditoría inalterable (Docker, NTP, `AUDITORIA_I`, respaldos 3-2-1) | Completado |
| 3 | Triage Manchester y urgencias (colas, sala de espera disociada, vistas Admin y Laboratorista) | Completado |
| 4 | ECE, recetas controladas y FHIR R4 | **En desarrollo** (rama remota `feature/sprint-4-ece-recetas-fhir`) |

## Estado de la app móvil **[verificado en código]**

*Actualizado el 8 de noviembre de 2026 (rama `feature/mobile-app`, etapas 0 a 8 del plan en [`vitae-mobile/PLAN.md`](vitae-mobile/PLAN.md)).*

- `applicationId = com.example.vitae_mobile`, `compileSdk` 37, `targetSdk` 36, **`minSdk` 29** (como pide la especificación).
- Implementado: navegación y tema accesible; capa de red (Retrofit y OkHttp, pinning por `BuildConfig`, manejo de 401, 403, 423 y 426); sesión con OTP, PKCE y cierre a los 5 min de inactividad; Inicio y perfil (Patient FHIR); citas, recetas con QR y visor de resultados; aviso de privacidad (antes y después del login), consentimientos y ARCO; bloqueo por root o Magisk, pantalla de incompatibilidad, R8 en release y sin respaldo de datos.
- **Conectada a los endpoints normales del backend** (decisión del PO del 8 de noviembre: no se necesita `/api/v1/mobile/*`): login con `id_usuario`, elección de un paciente, perfil FHIR, recetas y resultados del expediente, consentimientos y ARCO. Probada en un teléfono contra el Laravel local con la rama `dev`. Consecuencias aceptadas: entra personal (no el paciente), se envía `id_paciente`, no hay OTP ni PKCE ni citas. `-Pvitae.backend=simulado` vuelve a los datos simulados. **El servidor de producción (Render) corre `main` y no tiene los endpoints del Sprint 4, así que el release aún no puede funcionar allí.**
- Estilo visual según la guía "Aetna Health App" (lienzo gris perla, tarjetas cálidas, azul cobalto, negro para la acción principal), sin cambiar opciones ni funciones; donde un par de colores de la guía no llega a WCAG AA se oscureció lo mínimo (`vitae-mobile/PLAN.md`, etapa 9).
- Pruebas: 227 unitarias y 44 instrumentadas (en un teléfono físico, con datos simulados) en verde. Cobertura de líneas: ViewModels 100 %, lógica de datos 94 %. Paquete de evidencia AI-05 en `vitae-mobile/evidence/AI-05-mobile/`.
- Falta: desplegar `dev` en producción (y exigir token en `/api/v1/fhir/*`, hoy abierto), la matriz de dispositivos completa (hay un teléfono con API 31 y un emulador con API 28), la auditoría manual con TalkBack, los pines de certificado reales y la colección de contrato compartida.

## Flujo de trabajo Git (de [AGENTS.md](AGENTS.md))

1. **Prohibido commitear a `main`.** Solo recibe releases auditados desde `dev`.
2. **Toda rama sale de `dev` y vuelve a `dev` por PR** (1 aprobación de otra persona y CI en verde). Prefijos: `feature/`, `bugfix/`, `refactor/`, `docs/`, `test/`, `chore/`, con el patrón `<tipo>/<alcance>-<descripcion>`.
3. **Commits:** `<tipo>(<alcance>): <descripción en imperativo>` con `Refs: #TASK-ID, Req: RF-XXX, Audit: AI-0X`. Alcances válidos: `backend`, `front`, `mobile`, `bruno`, `db`, `infra`, `auth`, `triage`, `expediente`, `recetas`, `auditoria`, `fhir`. Sin mensajes genéricos ("wip", "cambios").
4. **Cero secretos y cero PII real.** Jamás permitir UPDATE ni DELETE en `AUDITORIA_I`.

## Discrepancias detectadas **[verificado en código]**

- El README lista una carpeta `docs/` que **no existe** en el repo.
- `vitae-backend/render.yaml` (líneas 22-26) sigue con `DB_USERNAME` y `DB_PASSWORD` en texto plano dentro del repo. Contradice "cero secretos" y PROV-SEC-004. Hay que pasarlos a variables de entorno en Render (`sync: false`) y rotar la contraseña, porque ya está en el historial de git.
