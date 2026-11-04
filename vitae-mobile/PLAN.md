# Plan de implementación de la app móvil (vitae-mobile)

Fuentes: [CONTEXT.md](../CONTEXT.md) y [sih-hospitalario_agents/mobile/](../sih-hospitalario_agents/mobile/) (`REQUIREMENTS_EXECUTION`, `ARCHITECTURE`, `UI_UX_SPEC`, `DOMAIN_SCHEMA.json`, `TASK_BACKLOG.json`), más [web/docs/ARCHITECTURE.md](../sih-hospitalario_agents/web/docs/ARCHITECTURE.md) (superficie de API), `vitae-backend/routes/api.php` y `vitae-front/src/services/`.
Regla de trabajo: todo el código vive en `vitae-mobile/`. Lo que exija tocar otra carpeta está en la sección 7 y no se hace sin tu decisión.

## 1. Alcance y supuestos (por confirmar)

| # | Supuesto | Alternativa |
|---|---|---|
| S1 | La app es el **portal del paciente de solo lectura** (RF-011): citas, recetas, resultados, perfil, aviso/consentimientos y ARCO. Reutiliza los contratos de la API actual (envoltorio, Bearer, 401/403/423). | Port de los workspaces del personal |
| S2 | DI manual (un contenedor simple) en lugar de Hilt. SUP-ARQ-M-01 es un supuesto, no un requisito. | Hilt si el grafo crece |
| S3 | Marca = tokens de `vitae-front/src/assets/main.css` (AMB-010 deja la marca sin definir), con una excepción: el texto suave de la web (`#64748B`) da 4,40:1 sobre el fondo y se oscurece a `#5F6F86` (4,73:1) para cumplir PROV-UX-003. Solo tema claro y sin color dinámico. | Marca propia / tema oscuro |
| S4 | Repositorios como interfaz. El `Mock*` vive en el source set debug (hasta que exista el backend móvil) y la implementación remota se escribe cuando haya contrato. Un solo punto de conmutación (`crearAuthRepository`). | n/a |
| S5 | La app arranca en Login (etapa 2). Splash con las comprobaciones de compatibilidad y root llega en la etapa 6. | n/a |
| S6 | Estado de receta: VIGENTE = activa, CONSUMIDO = dispensada, CANCELADO = cancelada (`RECETA.qr_estado`). La especificación no da el mapeo. | Confirmar con el PO |

## 2. Arquitectura y convenciones

`View (Compose) → ViewModel (StateFlow de UiState) → Repository (interfaz) → fuente (Retrofit | mock)`.

```
com.example.vitae_mobile/
  core/        Resultado, ApiError, reloj (Clock) inyectable
  data/
    network/   ApiClient, AuthInterceptor, ApiEnvelope, errores HTTP
    fhir/      Patient, MedicationRequest (kotlinx-serialization)
    auth/      TokenStore, SessionManager, PKCE, AuthRepository
    citas|recetas|resultados|privacidad/   Repository + Mock + Remote
  ui/
    theme/ navigation/ components/ (cargando, vacío, error con reintento)
    <pantalla>/ Screen + ViewModel + UiState
```

- **Estados de lista** (UI_UX_SPEC): cargando con skeleton, vacío con mensaje, error con reintento; cada ViewModel expone un `UiState` sellado.
- **Errores** (ARCHITECTURE): 401 → limpiar estado y Login · 403 → mensaje de permisos sin cerrar sesión · 423 → contactar a administración · 426 → incompatibilidad · red/5xx → error con reintento. Mensajes genéricos en autenticación.
- **Nombres:** PascalCase en clases, camelCase en funciones, SCREAMING_SNAKE_CASE en constantes, kebab-case en rutas. Textos en `strings.xml`, en español.
- **Fechas** en UTC con `java.time` (CON-015). **Sin PII en logs.**
- **Solo lectura:** ninguna pantalla clínica lleva guardar/editar/eliminar. Escrituras permitidas (SUP-002): login, OTP, refresh, logout, consentimiento y ARCO.
- **Nunca se envía `id_paciente`** en query, ruta ni cuerpo (RF-002.1); la identidad sale del token.

## 3. Mapa de etapas

| # | Etapa | TASK-M | Requisitos | Tamaño | Backend requerido | Estado |
|---|---|---|---|---|---|---|
| 0 | Cimientos | 001, 002, 025 | RNF-006, PROV-UX-003, PROV-UI-003 | M | no | **Hecha** |
| 1 | Red y errores | 006, 024 | RNF-003, RF-003, RF-002.1 | M | no (MockWebServer) | **Hecha** |
| 2 | Sesión | 003, 004, 005 | PROV-SEC-001/002, RF-002 | L | `/mobile/auth/*` | **Hecha** (con mock) |
| 3 | Inicio y perfil | 008 | RF-011, RNF-007 | M | `/mobile/perfil` | **Hecha** (con mock) |
| 4 | Citas, recetas, resultados | 009, 010, 011 | PROV-RF-002, RF-009, RF-010 | L | `/mobile/{citas,recetas,resultados}` | **Hecha** (con mock) |
| 5 | Privacidad y ARCO | 012, 013 | PROV-RN-008/009 | M | `/mobile/{aviso,consentimientos,solicitudes-arco}` | **Hecha** (con mock) |
| 6 | Estados y hardening | 014, 007 | RNF-006, PROV-SEC-011 | L | 426 | Pendiente |
| 7 | Calidad y evidencia | 015–020 | RF-011, PROV-UX-003, RF-002.1, RNF-007 | M | colección de contrato | Pendiente |

Fuera de alcance por ahora: release (TASK-M-021 a 023).
Cada etapa termina con: build + tests unitarios + lint en verde, tu autorización y un commit `feat(mobile): …` con `Refs: #TASK-M-xxx, Req: …, Audit: AI-05`.

## 4. Detalle por etapa

### Etapa 0 — Cimientos (hecha)
Compose + Navigation, 12 rutas, `InicioScreen`, `PantallaBase`, tema con contraste verificado por test, `minSdk` 29, `compileSdk` 37.

### Etapa 1 — Red y errores (TASK-M-006, M-024) — hecha
- **Resultado:** `ApiClient`, `AuthInterceptor`, `SesionInterceptor`, `llamar { }` y `ApiError` en `data/network` y `core`; `ApiConfig` toma URL y pines de `BuildConfig`. El release se niega a crear el cliente sin pines. 15 tests unitarios en verde; el recurso empaquetado del release no admite tráfico en claro.
- **Qué se construye:** cliente OkHttp/Retrofit con `AuthInterceptor` (Bearer), mapeo uniforme de errores (`ApiError`), `Resultado<T>`, envoltorio `{status, message, data}`, `network_security_config.xml` sin tráfico en claro, y permiso `INTERNET`.
- **Pinning:** `CertificatePinner` con pines por `BuildConfig`. Debug sin pines y con excepción de HTTP solo para `10.0.2.2` (backend local). Release exige pines reales (pendientes: certificado del servidor del hospital).
- **Respaldo existente:** `vitae-front/src/services/api.ts` (interceptor 401 con refresh y cola de peticiones).
- **Pruebas:** MockWebServer → 401, 403, 423, 426, timeout y 5xx devuelven el `ApiError` correcto; el 403 no limpia sesión; ninguna petición contiene `id_paciente`; `grep 'cleartextTrafficPermitted="false"'` sobre el XML.
- **Aceptación:** conexión con proxy no fijado falla (verificación manual con mitmproxy, fuera de CI).

### Etapa 2 — Sesión (TASK-M-003, M-004, M-005) — hecha, con mock
- **Resultado:** `data/auth` (`AuthRepository`, `SessionManager`, `KeystoreTokenStore`, `Pkce`, `InactivityMonitor`), pantallas de Login, OTP y Sesión expirada, y cierre de sesión desde Inicio. La navegación la dicta el estado de la sesión. 52 tests unitarios en verde.
- **Mock solo en debug:** `MockAuthRepository` vive en `src/debug` (trae credenciales de demostración); el release no puede referenciarlo y `crearAuthRepository` falla de forma explícita hasta que exista la implementación remota. Usuario de demo `paciente.demo@vitae.test`, OTP `123456`, personal `medico.demo@vitae.test` (403).
- **Arranque en frío:** la sesión no se restaura; un token que quede de un proceso anterior se borra y se revoca.
- **Verificado en teléfono real** (M2007J20CG, Android 12, API 31, app de debug instalada desde Android Studio): login con error genérico, OTP incorrecto y correcto, Inicio, navegación y cierre de sesión, segundo login sin OTP y arranque en frío que descarta la sesión. Con `run-as`: antes del OTP no hay sesión, después hay un valor cifrado sin tokens en claro, y al cerrar sesión se borra conservando el `id_instalacion`. Sin cierres en logcat.
- **Sin verificar en dispositivo:** `KeystoreTokenStoreTest` (el teléfono bloquea instalar el APK de pruebas por USB; la instalación manual desde Studio sí funcionó) y el cierre real a los 5 min de inactividad (solo con tests de tiempo virtual).
- **Plan original:**
- **Pantallas:** Login (correo y contraseña), OTP, Sesión expirada. Errores genéricos; 423 → alerta de contactar a administración.
- **Módulos:** `AuthRepository` (mock y remoto), `SessionManager` (Autenticada | SinSesion), `TokenStore` cifrado con Android Keystore, `Pkce` (verifier y challenge S256), `InactivityMonitor` (300 s con reloj inyectable; se reinicia desde `Activity.onUserInteraction`).
- **Flujo:** credenciales correctas en dispositivo nuevo → `otp_required=true`, sin tokens hasta `otp-verify`. Refresh rotativo; token reutilizado → forzar login. Logout: revocación en servidor + borrado local.
- **Cuenta de personal:** HTTP 403 → "Acceso no disponible en la app" (RF-002, escenario 2).
- **Pruebas:** vector de RFC 7636 (apéndice B) para PKCE; inactividad a 300 s con tiempo virtual; ViewModels de Login y OTP con repositorio falso; instrumentadas `LoginOtpTest` e `InactivityTest`; `adb run-as … ls shared_prefs` sin tokens en claro.
- **Respaldo existente:** `authService.ts` (login, refresh 2 min antes de expirar, logout), `AuthController` (423 al tercer fallo).
- **Datos del mock:** solo sintéticos (`paciente.demo@vitae.test`).

### Etapa 3 — Inicio y perfil (TASK-M-008) — hecha, con mock
- **Resultado:** `data/fhir` (`FhirPatient`, `FhirParser`), `data/perfil` (`Perfil`, `PerfilRepository`, mock solo en debug), `PerfilViewModel`, `PerfilScreen`, componentes reutilizables de estado (`Esqueleto`, `EstadoError`) y saludo en Inicio. 71 tests unitarios en verde. Cada pantalla tiene su propia instancia del ViewModel, ligada a su entrada de navegación, para que los datos del paciente no sobrevivan al cierre de sesión.
- **Cambio de alcance:** el resumen de Inicio (próxima cita, recetas activas, últimos resultados) pasa a la etapa 4, porque necesita los repositorios de citas, recetas y resultados. Inicio muestra por ahora el saludo con el nombre.
- **Supuestos del contrato FHIR** (a confirmar con el backend): la CURP viaja en `identifier` con `system = urn:oid:2.16.840.1.113883.4.629` y `seguro_activo` como extensión `urn:vitae:seguro-activo` (`valueBoolean`); la especificación no define ninguno de los dos.
- **Verificado en teléfono real** (M2007J20CG, Android 12): Inicio con el saludo, Perfil con nombre, CURP, fecha y seguro, y el esqueleto de carga. Las 9 pruebas instrumentadas pasan en el dispositivo: `HomeTest` (5), `KeystoreTokenStoreTest` (3, con el Keystore real) y `ExampleInstrumentedTest`.
- **Corrección que salió de las pruebas:** si la actividad se cerraba con "atrás" y se reabría con el proceso vivo, la navegación arrancaba en Login aunque la sesión estuviera activa; ahora arranca en Inicio. `unaActividadNuevaConLaSesionVivaMuestraInicio` lo cubre y se comprobó que falla sin la corrección.
- **Nota para MIUI (Xiaomi):** bloquea que una app abra su propia pantalla desde segundo plano, y `am instrument` reinicia el proceso, así que `ActivityScenario` se cuelga. `HomeTest` abre la actividad con `am start --activity-clear-task` a través de `UiAutomation` (identidad de shell, que MIUI permite). Además la instalación por USB exige activar "Instalar mediante USB" y aceptar el aviso en el teléfono.
- **Plan original:**
- **Inicio:** resumen de próxima cita, recetas activas y últimos resultados, solo datos propios, con estados de carga, vacío y error.
- **Perfil:** `Patient` FHIR → CURP, nombre y fecha de nacimiento; aseguradora como `seguro_activo` (sí/no), porque no hay más campos (AMB-018).
- **FHIR:** modelos tipados; un recurso sin `resourceType` o que no valida produce error controlado y la app no se cierra (RNF-007).
- **Pruebas:** JSON FHIR válido e inválido; `HomeTest` instrumentada.

### Etapa 4 — Citas, recetas, resultados (TASK-M-009, M-010, M-011) — hecha, con mock
- **Resultado:** repositorios y pantallas de citas, recetas (lista y detalle con QR) y resultados (lista y visor interno de PDF e imagen); resumen de Inicio (próxima cita, recetas activas, último resultado), que pasó aquí desde la etapa 3; `FLAG_SECURE` en Inicio, Citas, Perfil, Recetas y Resultados con un contador para no quitarlo al navegar entre pantallas protegidas; componentes reutilizables (`CargaViewModel`, `ContenidoCarga`, `Tarjeta`). 116 tests unitarios y 19 instrumentados en verde.
- **Verificado en teléfono real:** las 18 pruebas instrumentadas pasan (`ClinicoTest` 8, `HomeTest` 6, `KeystoreTokenStoreTest` 3 y el ejemplo). Revisión visual de cada pantalla con un build de debug que permite capturas (`-Pvitae.permitirCapturas=true`; el build normal las bloquea). El QR, tal como se pinta en el teléfono, se decodifica con ZXing como el texto original.
- **Hallazgos corregidos:** los módulos del QR con tamaño fraccionario dejaban costuras; ahora el tamaño y el desfase son enteros. ZXing codifica en ISO-8859-1 sin declararlo y un texto con acentos se leía de vuelta como katakana; ahora se declara UTF-8 solo si el texto no es ASCII.
- **Supuestos del contrato** (a confirmar con el backend, la especificación no los define): la lista de recetas es un `Bundle` FHIR y el detalle un `MedicationRequest`; el QR viaja en las extensiones `urn:vitae:qr-codigo` y `urn:vitae:qr-estado` (VIGENTE, CONSUMIDO, CANCELADO); el estado de la receta sale de `status` (active, completed, cancelled o stopped) y de `qr-estado`; la duración sale de `dispenseRequest.expectedSupplyDuration`; citas y resultados llegan como JSON propio (fecha en UTC) y el archivo del resultado como `GET .../resultados/{id}/archivo`.
- **Protección de capturas:** Inicio, Citas, Perfil, Recetas y Resultados (el plan solo pedía las tres últimas; se añadieron Inicio, por su resumen clínico, y Citas por decisión del PO). Login, OTP y Sesión expirada no la llevan.
- **Plan original:**
- **Citas:** lista con fecha, hora y estado; vacío con mensaje. Los valores de `CITA.estado` no están definidos: se muestra el texto del servidor.
- **Recetas:** lista y detalle con medicamento, dosis, vía, duración, estado y QR. Con QR consumido el QR se muestra inválido y el estado "dispensada". Sin imprimir ni modificar.
- **Resultados:** lista y visor interno. El archivo se transmite a `cacheDir` (nunca a almacenamiento compartido), se abre con `PdfRenderer` (PDF) o decodificación de imagen, muestra progreso y se borra al salir. Un 403 no muestra datos.
- **Seguridad de pantalla:** `FLAG_SECURE` en recetas, resultados y perfil mediante un contenedor `PantallaSegura`.
- **Pruebas:** ViewModels con repositorios falsos (vacío, error, éxito), mapeo de estados de receta, `RecetasTest` y `ResultadosTest` instrumentadas.

### Etapa 5 — Privacidad y ARCO (TASK-M-012, M-013) — hecha, con mock
- **Resultado:** `data/privacidad` y `data/arco` (modelos, repositorios y servidores simulados solo en debug), `AvisoViewModel` con dos modos (`Aceptacion` y `Gestion`), `AvisoScreen`, `ArcoViewModel`, `ArcoScreen`. Flujo Login → OTP → Aviso → Inicio; la puerta del aviso reenvía sola a Inicio si la versión ya está aceptada y vuelve a aparecer si cambia. Desde Inicio, "Privacidad y consentimientos" permite aceptar y revocar las finalidades opcionales. 148 tests unitarios y 29 instrumentados en verde.
- **Verificado en teléfono real:** las 29 pruebas instrumentadas pasan (`PrivacidadTest` 6, `ArcoTest` 4, además de las anteriores), y revisión visual del aviso, ARCO y preferencias.
- **Reglas aplicadas:** las finalidades opcionales arrancan siempre desmarcadas en la aceptación, aunque se hubieran aceptado antes; cada cambio crea un registro nuevo (CON-016: revocar no modifica el anterior); "No acepto" cierra la sesión; no hay `id_paciente` en ninguna petición.
- **Discrepancia a resolver:** el flujo de `UI_UX_SPEC` pone el aviso después del login, pero el escenario 1 de PROV-RN-008 pide verlo "antes de cualquier formulario" (el login lo es). Se siguió el flujo de pantallas porque el aviso se pide al servidor y eso exige sesión. Alternativa: mostrar un aviso empaquetado en la app antes del login y el vigente después.
- **Supuestos del contrato** (a confirmar con el backend): `GET /mobile/aviso-privacidad` devuelve versión, vigencia, texto simplificado, enlace al integral, las finalidades opcionales con su estado y si la versión está aceptada por el paciente; `POST /mobile/consentimientos` recibe `tipo` (AVISO_PRIVACIDAD o FINALIDADES_SECUNDARIAS), `version_documento`, `finalidades` y `accion` (ACEPTAR o REVOCAR) y responde 201; los derechos ARCO viajan como ACCESO, RECTIFICACION, CANCELACION y OPOSICION; la solicitud ARCO solo lleva el derecho (el esquema no tiene campo de descripción); `GET .../solicitudes-arco` lista las propias y `GET .../{folio}` devuelve una, con 403 si es ajena.
- **Accesibilidad no textual (hallazgo):** el borde de los controles (interruptores apagados, campos de texto, botón de contorno) usaba el color decorativo de la web (#E2E8F0, 1,2:1) y casi no se veía. Ahora el borde de controles es #74839A (3,1:1 o más, WCAG 1.4.11) y el decorativo (tarjetas, esqueletos) queda aparte; un test lo comprueba.
- **Plan original:**
- **Aviso:** se muestra antes de cualquier formulario en la primera apertura y cuando cambia la versión; se registra la versión mostrada.
- **Consentimientos:** aceptar y revocar con `POST /mobile/consentimientos` (`accion='REVOCAR'` → 201). Finalidades secundarias en opción independiente y **no preseleccionada**. Los registros son inmutables: la revocación crea uno nuevo (CON-016).
- **ARCO:** formulario → `POST /mobile/solicitudes-arco` → 201 con folio y fechas límite; consulta de estado por folio.
- **Pruebas:** orden de pantallas en primera apertura, finalidades sin marcar por defecto, `PrivacidadTest` y `ArcoTest` instrumentadas.

### Etapa 6 — Estados y hardening (TASK-M-014, M-007)
- **Pantallas de estado:** incompatible (HTTP 426 y versión de Android), sesión expirada, root bloqueado y sin conexión.
- **Hardening:** bloqueo en root/Magisk sin enviar peticiones, R8 en release con reglas para serialization y Retrofit, `allowBackup=false`, sin PII en claro.
- **Detección de root:** comprobaciones locales (binarios `su`, rutas de Magisk, build `test-keys`). Play Integrity necesita verificación en servidor y no es viable sin backend (SUP-007).
- **Pruebas:** `RootBlockTest` con un detector falso, `EstadosTest`, inspección del APK de release (clases ofuscadas).

### Etapa 7 — Calidad y evidencia (TASK-M-015 a M-020)
- **Sin controles de edición:** prueba de UI que recorre las pantallas clínicas y exige 0 botones guardar/editar/eliminar.
- **Accesibilidad:** Espresso con `AccessibilityChecks`, TalkBack con `contentDescription` en todos los controles, objetivos >= 48 dp.
- **Cobertura >= 80%** en ViewModels y repositorios con JaCoCo, e inspección de tráfico: 0 `id_paciente`.
- **Contrato:** pruebas contra la colección compartida (TASK-W-091) cuando exista; mientras tanto, contrato sobre fixtures JSON.
- **Matriz de dispositivos:** 3 tamaños con API >= 29 y 1 con API 28. Hoy hay un teléfono físico con Android 12 (API 31) y solo la imagen de sistema de API 28 instalada; faltan imágenes de API >= 29 para los emuladores.
- **Evidencia AI-05:** paquete con resultados de pruebas, cobertura y capturas de accesibilidad.

## 5. Dependencias nuevas (se añaden en la etapa que las usa)

| Etapa | Dependencia | Motivo | Alternativa más ligera |
|---|---|---|---|
| 1 | `okhttp` 5.5.1 (instalada) | Pinning e interceptores (RNF-003) | `HttpURLConnection`: sin pinning cómodo |
| 1 | `retrofit` 3.0.0 + `converter-kotlinx-serialization` | Lo nombra la especificación | OkHttp a mano |
| 1 | `kotlinx-serialization-json` 1.9.0 + plugin (instalada) | Modelos FHIR tipados, sin reflexión, compatible con R8 | Moshi o Gson |
| 1 | `kotlinx-coroutines-test` 1.10.2, `mockwebserver3` 5.5.1 (solo pruebas, instaladas) | Pruebas de red y de tiempo | Fakes manuales |
| 2 | Ninguna para el cifrado de tokens: Keystore de la plataforma (AES-GCM) | Evitar `security-crypto` (ver decisión 4) | `androidx.security:security-crypto` 1.1.0 |
| 4 | `com.google.zxing:core` 3.5.4 (instalada) | Dibujar el QR de la receta | Escribir un codificador QR |
| 7 | `espresso-accessibility`, JaCoCo (ya integrado en AGP) | Accesibilidad y cobertura | n/a |

Hilt, Coil, Room y Material Icons extendidos quedan fuera mientras no hagan falta.

Versiones limitadas por el compilador: el proyecto usa Kotlin 2.2.10, que lee metadatos hasta 2.3. `kotlinx-serialization` 1.12.0 y `kotlinx-coroutines-test` 1.11.0 están compiladas con Kotlin 2.4 y arrastran `kotlin-stdlib` 2.4.10, por eso no compilan. Se subirán junto con el plugin de Kotlin (2.4.x), que lint ya recomienda.

## 6. Contratos que se reutilizan de la API actual

- Envoltorio `{ "status": "success" | "error", "message"?, "data"? }`.
- Bearer: `POST /auth/login` → `{ token, token_type, expires_in, usuario }`; `POST /auth/refresh`; `POST /auth/logout`; `GET /auth/me`.
- Códigos: 401 sin sesión, 403 sin permisos, 423 bloqueo tras 3 intentos.
- El móvil recibe FHIR **solo a través de `/mobile/*`** (web/ARCHITECTURE.md, convenciones). Eso resuelve la contradicción con la tabla de `mobile/ARCHITECTURE.md`, que lista también `/fhir/*`.

## 7. Cambios fuera de esta carpeta (no se hacen)

1. Rol `PACIENTE` y cuentas con correo y contraseña (hoy `id_usuario` más contraseña, solo roles de personal).
2. `POST /api/v1/mobile/auth/{token,otp-verify,refresh,logout}`, un endpoint que entregue el código PKCE y el claim `client ∈ {web, mobile}` en el token.
3. `GET /api/v1/mobile/{perfil,citas,recetas,resultados,resultados/{id}/archivo,aviso-privacidad}`, `POST …/consentimientos`, `GET|POST …/solicitudes-arco` y `GET …/solicitudes-arco/{folio}`.
4. Tablas de citas, consentimientos, aviso de privacidad, solicitudes ARCO y resultados.
5. 403 en `/mobile/*` para tokens de personal y 403 para tokens de paciente fuera de `/mobile/*`.
6. HTTP 426 para versiones de app incompatibles.
7. Actualizar `matriz_trazabilidad.md` al cerrar el sprint (lo exige `sih-hospitalario_agents/AGENTS.md`).

## 8. Decisiones abiertas

1. Confirmar S1 a S6.
2. **Paquete definitivo:** hoy `com.example.vitae_mobile`. Cambiarlo después cuesta más.
3. **`minSdk` 29 contra el mensaje en Android 9:** con `minSdk` 29 la app no se instala en Android 9 y el mensaje de RNF-006 no se vería. Para mostrarlo habría que bajar `minSdk` y comprobar la versión en ejecución.
4. **Almacenamiento de tokens** (adoptado en la etapa 2): Keystore directo con AES-GCM. La especificación nombra `EncryptedSharedPreferences`, que está marcado como obsoleto; el criterio de aceptación ("sin tokens en claro") se cumple igual.
5. **Cómo se obtiene el código PKCE** (adoptado): el repositorio expone `iniciarSesion(correo, contraseña)` y esconde el intercambio. Hay que definirlo con el backend; la especificación lista `POST /mobile/auth/token` pero ningún endpoint que entregue el código.
6. **Identificación del dispositivo para el OTP** (adoptado): UUID de instalación guardado en la app. No se define cómo sabe el servidor que el dispositivo es nuevo.
7. **Datos sin definir:** valores de `CITA.estado` y de `derecho` en ARCO, fuente del texto del aviso integral. Longitud del OTP: se asumió 6 (constante `LONGITUD_OTP`).
8. **Pines de certificado** del servidor real.
9. **Aseguradora (AMB-018):** solo existe `seguro_activo`. Cómo viaja en el Patient FHIR y en qué sistema va la CURP (supuestos de la etapa 3, ver arriba).
10. **Contrato de citas y resultados:** la especificación no los define (formato, valores de `CITA.estado`, tipos de archivo y tamaño máximo, AMB-012).
11. **Aviso antes del login:** ¿mostrar un aviso empaquetado en la app antes de Login para cumplir el escenario 1 de PROV-RN-008 al pie de la letra? Hoy el aviso va después de Login y OTP.
12. **Contrato de privacidad y ARCO:** ver los supuestos de la etapa 5; la especificación no define los endpoints de lectura ni los valores de los derechos.
13. **Commits:** `AGENTS.md` pide tu autorización previa y el 100% de las pruebas.

## 9. Verificación en cada etapa

```bash
cd vitae-mobile
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # JDK 21 de Android Studio
./gradlew assembleDebug testDebugUnitTest lintDebug
# con teléfono o emulador conectado:
./gradlew connectedDebugAndroidTest
```

Qué no se puede verificar sin backend: tráfico real, OTP, PKCE y pinning contra el servidor. Se prueban contra mocks y se marcan como pendientes en el reporte de cada etapa.
