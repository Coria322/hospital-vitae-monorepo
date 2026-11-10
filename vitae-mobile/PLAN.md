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
| S5 | La ruta inicial la decide el estado del dispositivo y la sesión (etapa 6): bloqueo, aviso previo, Login o la puerta del aviso. No hay Splash: la comprobación es local e instantánea. | n/a |
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
| 6 | Estados y hardening | 014, 007 | RNF-006, PROV-SEC-011 | L | 426 | **Hecha** (con mock) |
| 7 | Calidad y evidencia | 015–020 | RF-011, PROV-UX-003, RF-002.1, RNF-007 | M | colección de contrato | **Hecha en lo local** (ver pendientes) |
| 8 | Conexión al backend real | — | RF-003, RNF-003, RNF-007, PROV-RN-008/009 | L | endpoints normales de `dev` | **Hecha**, probada con el backend local (ver etapa 8) |
| 9 | Estilo visual (guía "Aetna Health App") | — | PROV-UX-003, PROV-UI-003 | M | no | **Hecha** (ver etapa 9) |
| 10 | Portal móvil del Sprint 5 | — | RF-002, RF-002.1, PROV-RF-002, RF-011, RNF-006 | M | `/api/v1/mobile/…` de `dev` | **Hecha**, probada con el backend local (ver etapa 10) |

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
- **Discrepancia (resuelta en la etapa 6):** el flujo de `UI_UX_SPEC` pone el aviso después del login, pero el escenario 1 de PROV-RN-008 pide verlo "antes de cualquier formulario" (el login lo es). En esta etapa se siguió el flujo de pantallas porque el aviso se pide al servidor y eso exige sesión; en la etapa 6 se añadió un aviso simplificado empaquetado antes del login (ver abajo).
- **Supuestos del contrato** (a confirmar con el backend): `GET /mobile/aviso-privacidad` devuelve versión, vigencia, texto simplificado, enlace al integral, las finalidades opcionales con su estado y si la versión está aceptada por el paciente; `POST /mobile/consentimientos` recibe `tipo` (AVISO_PRIVACIDAD o FINALIDADES_SECUNDARIAS), `version_documento`, `finalidades` y `accion` (ACEPTAR o REVOCAR) y responde 201; los derechos ARCO viajan como ACCESO, RECTIFICACION, CANCELACION y OPOSICION; la solicitud ARCO solo lleva el derecho (el esquema no tiene campo de descripción); `GET .../solicitudes-arco` lista las propias y `GET .../{folio}` devuelve una, con 403 si es ajena.
- **Accesibilidad no textual (hallazgo):** el borde de los controles (interruptores apagados, campos de texto, botón de contorno) usaba el color decorativo de la web (#E2E8F0, 1,2:1) y casi no se veía. Ahora el borde de controles es #74839A (3,1:1 o más, WCAG 1.4.11) y el decorativo (tarjetas, esqueletos) queda aparte; un test lo comprueba.
- **Plan original:**
- **Aviso:** se muestra antes de cualquier formulario en la primera apertura y cuando cambia la versión; se registra la versión mostrada.
- **Consentimientos:** aceptar y revocar con `POST /mobile/consentimientos` (`accion='REVOCAR'` → 201). Finalidades secundarias en opción independiente y **no preseleccionada**. Los registros son inmutables: la revocación crea uno nuevo (CON-016).
- **ARCO:** formulario → `POST /mobile/solicitudes-arco` → 201 con folio y fechas límite; consulta de estado por folio.
- **Pruebas:** orden de pantallas en primera apertura, finalidades sin marcar por defecto, `PrivacidadTest` y `ArcoTest` instrumentadas.

### Etapa 6 — Estados y hardening (TASK-M-014, M-007) — hecha, con mock
- **Resultado:** `data/seguridad` (`DetectorRoot`, `EntornoDispositivo`, `EstadoDispositivo`, `GuardiaDispositivo`), `DispositivoInterceptor` en la cadena de red, `PantallaBloqueo` (Root bloqueado e Incompatible), `AvisoPrevioScreen` y `AvisoPrevioLocal`; manifiesto y reglas de respaldo endurecidos; R8 activado en release. La ruta inicial y los cambios en vivo los decide `VitaeNavHost` (sin Splash). Se eliminó `PantallaBase`: ya todas las rutas tienen pantalla real.
- **Verificado:** 174 pruebas unitarias en verde (26 nuevas); 39 instrumentadas en verde en el teléfono (10 nuevas: `AvisoPrevioTest` 3 y `SeguridadTest` 7); lint sin errores; `assembleRelease` con R8 (APK de 1,4 MB con `mapping.txt`); humo manual en el teléfono con el debug minificado (`-Pvitae.minificarDebug=true`): aviso previo → login → OTP → aviso → Inicio → recetas con QR → visor de PDF, sin caídas ni excepciones de clases faltantes.
- **No verificado:** root real (se simula en debug con `SimulacionRoot`; la lógica del detector se prueba con un entorno falso); Android < 10 (no hay dispositivo y con `minSdk = 29` no se instala); un 426 de un servidor real (el interceptor se prueba con MockWebServer); el release en ejecución (sus repositorios remotos aún no existen y fallan a propósito); exactitud y falsos positivos de la detección en teléfonos reales con Magisk.
- **Reglas aplicadas:** con root, 426 o Android no soportado la sesión se borra solo en local y ninguna petición sale (`DispositivoInterceptor` va primero en la cadena); el 426 no se levanta al volver a comprobar; el root se vuelve a comprobar en cada `onStart`; los mensajes son genéricos, sin la señal detectada (un test compara los textos de la pantalla); `allowBackup=false` y reglas que excluyen todo del respaldo y de la transferencia (también evita que el id de instalación, copiado a otro equipo, se salte el OTP); sin `Log` ni `println` en el código de la app.
- **Detección de root:** binarios `su`, rutas y paquetes de Magisk y de gestores de root (declarados en `<queries>`, un test los compara con el detector), `test-keys` y `/system` montado en lectura y escritura. Es de mejor esfuerzo: un root oculto (Zygisk, Shamiko) no se ve. Falsos positivos posibles en release: firmware personalizado o de desarrollo (`test-keys`); hay que reportarlos a Seguridad (caso borde de PROV-SEC-011). En debug se ignora `test-keys` para poder usar emuladores (decisión 14).
- **Aviso previo al login (PROV-RN-008, escenario 1):** primera apertura y cada vez que cambia `AvisoPrevio.VERSION` (caso borde "cambio sustancial"). El texto (`aviso_previo_texto`) y la URL del aviso integral son **marcadores** que Legal debe validar. No recaba nada; la aceptación formal y su registro siguen siendo la puerta posterior al login, que sí tiene sesión. La versión vista va en preferencias simples: no es dato sensible.
- **Sin conexión:** no hay pantalla aparte; cada pantalla muestra su estado de error con "Reintentar" (y el login su mensaje de red). Si se quiere una pantalla propia, es un cambio pequeño.
- **Firma de la APK (v2+):** corresponde a la etapa de liberación (TASK-M-021), no a esta.
- **Plan original:**
- **Pantallas de estado:** incompatible (HTTP 426 y versión de Android), sesión expirada, root bloqueado y sin conexión.
- **Hardening:** bloqueo en root/Magisk sin enviar peticiones, R8 en release con reglas para serialization y Retrofit, `allowBackup=false`, sin PII en claro.
- **Detección de root:** comprobaciones locales (binarios `su`, rutas de Magisk, build `test-keys`). Play Integrity necesita verificación en servidor y no es viable sin backend (SUP-007).
- **Pruebas:** `RootBlockTest` con un detector falso, `EstadosTest`, inspección del APK de release (clases ofuscadas).

### Etapa 7 — Calidad y evidencia (TASK-M-015 a M-020) — hecha en lo local; faltan pasos que requieren a otra persona o al backend
- **Resultado:** `SinEdicionTest` y `AccesibilidadTest` (teléfono), `SoloLecturaTest`, `ContratoFhirTest` con 15 ejemplos JSON y `PinningTest` (JVM), cobertura con JaCoCo y el paquete `evidence/AI-05-mobile/` (6 documentos, resultados de pruebas y 2 scripts). Cambios de código: `ApiClient.certificatePinner` (extraído para poder probar el pinning) y `enableUnitTestCoverage` en debug.
- **Verificado:** 191 pruebas unitarias y 44 instrumentadas en verde en el teléfono; lint sin errores; cobertura de líneas: ViewModels 100 % (252/252) y lógica de datos de producción 94,2 % (311/330). Las pruebas nuevas se validaron con mutaciones (se inyectó un botón "Guardar", un control sin etiqueta de 24 dp, un `DELETE` de Retrofit y `id_paciente`, y fallaron como debían); una de esas mutaciones destapó un hueco real en la regex de `SoloLecturaTest` (no veía la anotación calificada), ya corregido. En un emulador de Android 9 (API 28) el sistema rechaza la app con `INSTALL_FAILED_OLDER_SDK`.
- **No hecho o no verificable:** matriz de dispositivos completa (faltan 2 tamaños de pantalla y APIs 29 y 34 o superior: no hay imágenes de emulador); Accessibility Scanner y TalkBack con voz real; colección compartida de contrato (TASK-W-091) y Newman contra Staging; inspección de tráfico con proxy y pines contra el servidor real; los repositorios remotos de producción (la cobertura MC-10 de repositorios se vuelve a medir cuando existan); rechazo 403/405 de escrituras clínicas del lado del servidor (TASK-W-080).
- **Cambios respecto al plan:** `espresso-accessibility` no se añadió. Se probó `ui-test-junit4-accessibility` (ATF para Compose): exigía forzar versiones de Guava y `concurrent-futures` y no detectó una violación inyectada a propósito, así que se descartó y se revirtió; la accesibilidad se revisa con comprobaciones propias sobre el árbol semántico (texto hablable, objetivos de 48 dp, imágenes con descripción, encabezado), 14 pantallas. No se añadió ninguna dependencia nueva.
- **Plan original:**
- **Sin controles de edición:** prueba de UI que recorre las pantallas clínicas y exige 0 botones guardar/editar/eliminar.
- **Accesibilidad:** Espresso con `AccessibilityChecks`, TalkBack con `contentDescription` en todos los controles, objetivos >= 48 dp.
- **Cobertura >= 80%** en ViewModels y repositorios con JaCoCo, e inspección de tráfico: 0 `id_paciente`.
- **Contrato:** pruebas contra la colección compartida (TASK-W-091) cuando exista; mientras tanto, contrato sobre fixtures JSON.
- **Matriz de dispositivos:** 3 tamaños con API >= 29 y 1 con API 28. Hoy hay un teléfono físico con Android 12 (API 31) y solo la imagen de sistema de API 28 instalada; faltan imágenes de API >= 29 para los emuladores.
- **Evidencia AI-05:** paquete con resultados de pruebas, cobertura y capturas de accesibilidad.

### Etapa 8 — Conexión a los endpoints normales del backend — hecha, probada contra el backend local
- **Decisión del PO (8 de noviembre de 2026):** la app no necesita `/api/v1/mobile/*`; se conecta a los endpoints normales de `vitae-backend`. Se verificó en `dev` (Sprint 4) y no hay ningún endpoint móvil ni cuentas de paciente.
- **Resultado:** `data/remoto` (contrato Retrofit `ApiBackend`, `SesionBackend`, `AuthRemoto`, `PerfilRemoto`, `CitasRemoto`, `RecetasRemoto`, `ResultadosRemoto`, `PrivacidadRemoto`, `ArcoRemoto`), pantalla y ViewModel para elegir paciente, visor de texto para resultados y `ApiError.NoDisponible`. `AppContainer` usa los repositorios remotos si `BuildConfig.BACKEND_REAL` es verdadero: **por omisión en debug y siempre en release**. `-Pvitae.backend=simulado` vuelve a los datos simulados (las pruebas instrumentadas lo exigen y fallan con un mensaje si no se usa). La URL de debug sale de `-Pvitae.apiUrl` (en teléfono: `adb reverse tcp:8080 tcp:8080` y `http://localhost:8080/api/v1/`).
- **Mapeo contra el backend real** (formas capturadas del backend y guardadas como fixtures en `app/src/test/resources/backend`):
  - **Sesión:** `POST /auth/login` con `id_usuario` y `password` (el campo "Correo" pasa a "Usuario"); token de 15 min. No hay OTP ni PKCE ni refresh token: se renueva con `POST /auth/refresh` usando el Bearer vigente, y `POST /auth/logout` lo revoca.
  - **Quién entra:** el personal del hospital (`MED-001` etc.). Tras el login se **elige un paciente** (`GET /pacientes`); sus datos salen de `GET /pacientes/{id}`.
  - **Perfil:** `GET /fhir/Patient/{id}` (FHIR R4); el seguro viene de `seguro_activo` del paciente porque el Patient no lo trae.
  - **Recetas:** no hay lista; salen de `GET /expedientes-clinicos/{id}` (solo el médico dueño del expediente; otro usuario recibe 403) y el detalle de `GET /recetas/{id}`. Estado y QR salen de `qr_estado` y `qr_codigo` (el QR existe solo tras firmar). Dosis = `indicaciones` (texto libre); vía y duración no existen.
  - **Resultados:** solicitudes de estudio del expediente que ya tienen resultado; el contenido llega descifrado como **texto** en `GET /solicitudes-estudio/{id}/resultados` (un PDF o imagen binario no cabe en ese JSON).
  - **Citas:** el backend no tiene agenda: la sección dice "no disponible".
  - **Aviso y consentimientos:** el backend no sirve el aviso; texto y finalidades van en la app. El estado sale de `GET /consentimientos`; aceptar guarda una fila de aviso y otra de finalidades opcionales; revocar crea una fila REVOCADO que apunta a la original (los registros son inmutables).
  - **ARCO:** `POST/GET /solicitudes-arco`; el backend exige `acreditacion_identidad = true` y la app lo envía siempre. No hay consulta por folio.
- **Verificado:** 227 pruebas unitarias en verde (36 nuevas; las del backend sobre respuestas reales capturadas) y 44 instrumentadas en el teléfono con datos simulados. **Flujo real en el teléfono contra Laravel local** (`dev`, SQLite, datos sintéticos): login, elegir paciente, aviso, Inicio, Perfil, Recetas con QR vigente y no válido, Resultados con visor de texto, Citas "no disponible", ARCO (solicitud creada en la base) y revocación de una finalidad (fila REVOCADO creada en la base). También con la compilación minificada por R8: sin caídas ni excepciones de serialización.
- **No verificado:** el servidor de producción de Render **no tiene estos endpoints** (corre lo de `main`, hasta el Sprint 3: sin FHIR, recetas, consentimientos, ARCO ni laboratorio), así que el release no puede funcionar allí todavía; pines de certificado de producción; las pantallas nuevas con el lector de pantalla y con una prueba instrumentada propia (las instrumentadas corren con datos simulados).
- **Seguridad:** las rutas `/api/v1/fhir/*` de `dev` **no exigen token** (Patient por id o por CURP responde 200 sin autenticar); no están en producción. Corregirlas en el backend antes de desplegar el Sprint 4.
- **Plan original:** ninguno; la etapa nace de la decisión del PO.

### Etapa 9 — Estilo visual según la guía "Aetna Health App" — hecha
- **Pedido del PO (8 de noviembre de 2026):** cambiar solo el estilo y la organización con las reglas de esa guía, **sin cambiar las opciones ni las funciones**. No se tocó ningún repositorio, ViewModel ni flujo; los textos, botones y secciones son los mismos.
- **Resultado:**
  - **Tema:** paleta (lienzo gris perla, tarjetas cálidas, azul cobalto, negro para la acción principal), formas (28 dp en tarjetas, 32 dp en la destacada, píldoras y círculos completos, nada por debajo de 14 dp) y tipografía con pesos 400 y 500 (`ui/theme`).
  - **Componentes:** `Tarjeta` (cálida con borde de 1 dp), `TarjetaDestacada` (azul, texto blanco), `BotonSecundario` (píldora blanca), `Campo`, `Encabezado` (título de 24 sp y botón circular de volver), `BotonCircular`, `CirculoIcono` e `IconoFlecha` (← y ↗ dibujadas a mano, sin dependencias).
  - **Organización:** Inicio con la tarjeta azul del resumen (valor arriba y etiqueta abajo) y las secciones en una cuadrícula de dos columnas con su círculo ↗; Perfil y detalle de receta agrupados en tarjetas; la elección del derecho en ARCO como tarjetas de opción (cálida, y azul al elegir, con el botón de radio visible); el enlace al aviso integral pasa de texto suelto a botón.
  - **Botón de volver:** aparece en cada cabecera solo si hay pantalla anterior (lo provee `VitaeNavHost`) y hace lo mismo que el gesto de atrás.
- **Adaptaciones por accesibilidad (WCAG AA, PROV-UX-003):** la guía tiene pares que no llegan a AA y `ContrasteTest` los exige; se oscureció lo mínimo y está anotado en `Color.kt`:
  - blanco sobre Cobalt Blue `#5584D9` da 3,7:1 → el fondo de la tarjeta azul usa el "Deep Cobalt" de la propia guía, `#4A74C4` (4,6:1); el `#5584D9` queda solo para bordes sin texto;
  - el gris `#8A8A8A` da 3,1:1 → el texto secundario usa `#6B6B6B` (4,8:1);
  - el rojo `#E5484D` de la guía (3,5:1) es solo para puntos de notificación; los mensajes de error usan `#C62F34` (4,9:1);
  - el borde de los controles (campos de texto, interruptores) usa `#767676` (≥ 3:1) en lugar del hairline, que es decorativo;
  - el texto de la tarjeta azul va en blanco puro: con transparencia deja de llegar a 4,5:1.
- **Lo que no se aplicó y por qué:**
  - **Fuente:** la guía pide Manrope, Plus Jakarta Sans o Inter Tight; no vienen con Android y no se descargó ninguna. Se usa la sans del sistema y cambiarla es poner la `FontFamily` en `Type.kt`.
  - **Tamaño mínimo 12 sp** en lugar de los 11 px de las etiquetas (ilegible con fuente grande).
  - **Acción principal como círculo negro con ↗ o "+":** se mantienen botones con texto (píldora negra); las pruebas y TalkBack dependen de la etiqueta, y quitarla cambiaría la accesibilidad.
  - **No existen en la app y no se inventaron:** pestañas o píldoras de filtro, conmutador lista/mapa, tarjeta de doctor, chips de fecha, gráficas de barras ni hoja inferior.
  - **Modo oscuro:** la guía es solo tema claro.
- **Verificado:** 227 pruebas unitarias en verde (`ContrasteTest` ampliado con los pares nuevos) y las 44 instrumentadas en el teléfono con datos simulados (incluyen objetivos de 48 dp, etiquetas y encabezados en 14 pantallas); lint sin errores; release con R8; y recorrido a ojo en el teléfono, conectado al backend local, de aviso previo, login, elegir paciente, Inicio, Recetas (lista y detalle), Citas, Resultados y su visor, Perfil, Privacidad y ARCO.
- **No verificado:** OTP, sesión expirada y las pantallas de bloqueo no se revisaron a ojo con el estilo nuevo (sí las cubren las pruebas automáticas); fuente grande del sistema; modo oscuro del teléfono.

### Etapa 10 — Portal móvil del Sprint 5 (`/api/v1/mobile/…`) — hecha, probada contra el backend local
- **Origen:** `dev` (commit `2248de8`, ya en `main` como v1.0.0) trae el portal móvil: rol `PACIENTE`, aislamiento por token y `/api/v1/mobile/…`. Render ya sirve esas rutas (responden 401 sin token). Esto reemplaza la decisión 16: la app deja de usar los endpoints de personal.
- **Resultado:** el paciente inicia sesión él mismo (`PAC-001`) y la app consulta solo `/mobile`: `perfil`, `citas`, `recetas`, `resultados`, `POST consentimientos` y `POST solicitudes-arco`. **Ninguna petición lleva `id_paciente`**: se quitó la pantalla de elegir paciente (la app tiene otra vez 14 rutas) y `SoloLecturaTest` exige que el identificador no aparezca en ningún archivo. Cada petición lleva `X-Client-Version: API-<nivel>` (`VersionInterceptor`); sin ella el portal responde 400 y con un nivel menor que 29, 426. Una cuenta de personal se rechaza en la app ("Acceso no disponible") y su token se revoca en el acto.
- **Mapeo contra el backend real** (respuestas capturadas con datos sintéticos y guardadas como fixtures `mobile-*.json`):
  - **Perfil:** `GET /mobile/perfil` (JSON propio, no FHIR): nombre, CURP, fecha de nacimiento y seguro.
  - **Citas:** `GET /mobile/citas`. Ya hay datos; el estado es texto libre (`CONFIRMADA`, `PENDIENTE`, `CANCELADA`). Pedir o cancelar cita existe en el backend (`POST /mobile/citas`, `PATCH …/cancelar`) pero la app sigue siendo de solo lectura y no lo usa.
  - **Recetas:** `GET /mobile/recetas`: la lista trae todo lo que se muestra, así que el detalle sale de ella. Estado y QR, igual que antes.
  - **Resultados:** `GET /mobile/resultados` lista tipo, fecha y tipo de archivo, pero **no entrega el contenido**. El contenido sigue pidiéndose a `GET /solicitudes-estudio/{id}/resultados` con el id de un resultado de la propia lista (decisión 19).
  - **Consentimientos:** solo existe `POST /mobile/consentimientos`; exige `firmante_nombre` y `medico_responsable_id`. La app firma con el nombre del perfil y toma como responsable al médico de la cita (o, si no hay, de la receta) más reciente: un paciente sin citas ni recetas no puede registrar el aviso. Como no hay consulta, la app **recuerda en el teléfono** qué aceptó (por usuario, con hash, sin datos personales). **Revocar no existe** en el portal: devuelve `NoDisponible`.
  - **ARCO:** solo existe `POST /mobile/solicitudes-arco`; exige `descripcion_peticion` de 15 caracteres o más y la app, que solo pide elegir el derecho, la redacta. Ya no se envía `acreditacion_identidad`. No hay consulta: "Mis solicitudes" muestra las enviadas en esta sesión y "Actualizar estado" no puede cambiarlas.
- **Verificado:** 228 pruebas unitarias y las 44 instrumentadas (teléfono, datos simulados) en verde, lint sin errores, release con R8 compilado y recorrido en el teléfono contra Laravel local actualizado a `dev`: login de paciente, aviso (con finalidad opcional), Inicio con nombre, cita, recetas y último resultado, Citas, Recetas, Resultados con su visor, Perfil y ARCO (folio devuelto); en la base quedaron las filas de consentimiento (`DIGITAL_MOVIL`, aviso y finalidad) y de ARCO, y el login de `MED-001` se rechaza.
- **Hallazgos de seguridad del backend (no se corrigen desde la app):**
  - Un token de **paciente** lee fuera de `/mobile`: `GET /pacientes` lista a todos, `GET /pacientes/{otro}`, `GET /recetas/{id}` y `GET /solicitudes-estudio/{id}/resultados` responden 200 y `GET /consentimientos|solicitudes-arco?id_paciente=…` también. El aislamiento (`mobile.isolation`) solo cubre el prefijo `/mobile`.
  - `GET /api/v1/fhir/Patient/{id}` responde 200 sin token.
  - `GET /mobile/resultados` expone `archivo_cifrado_ref` (ruta interna del archivo cifrado).
  - `GET /fhir/Patient/x` responde 500 en Render en lugar de 404.
- **Render (10 de noviembre de 2026):** por pedido del PO, debug y release apuntan por omisión a `https://auditoria-backend-u47a.onrender.com/api/v1/` (el backend local pasa a ser `-Pvitae.apiUrl`). Render duerme el servidor tras un rato sin tráfico y la primera respuesta tardó 24 s, así que `readTimeout` subió de 10 a 60 s. Responden 401 sin token `mobile/perfil|citas|recetas|resultados|expediente`; no se sabe si Render tiene cuentas `PACIENTE` sembradas.
- **No verificado:** el flujo con sesión contra Render (hace falta una cuenta de paciente de allí); los pines de producción; `Citas` con muchas filas o sin ninguna a ojo en el teléfono.

## 5. Dependencias nuevas (se añaden en la etapa que las usa)

| Etapa | Dependencia | Motivo | Alternativa más ligera |
|---|---|---|---|
| 1 | `okhttp` 5.5.1 (instalada) | Pinning e interceptores (RNF-003) | `HttpURLConnection`: sin pinning cómodo |
| 1 | `retrofit` 3.0.0 + `converter-kotlinx-serialization` | Lo nombra la especificación | OkHttp a mano |
| 1 | `kotlinx-serialization-json` 1.9.0 + plugin (instalada) | Modelos FHIR tipados, sin reflexión, compatible con R8 | Moshi o Gson |
| 1 | `kotlinx-coroutines-test` 1.10.2, `mockwebserver3` 5.5.1 (solo pruebas, instaladas) | Pruebas de red y de tiempo | Fakes manuales |
| 2 | Ninguna para el cifrado de tokens: Keystore de la plataforma (AES-GCM) | Evitar `security-crypto` (ver decisión 4) | `androidx.security:security-crypto` 1.1.0 |
| 4 | `com.google.zxing:core` 3.5.4 (instalada) | Dibujar el QR de la receta | Escribir un codificador QR |
| 7 | JaCoCo (ya integrado en AGP). `espresso-accessibility` y `ui-test-junit4-accessibility` **no se añadieron** (ver etapa 7) | Cobertura | n/a |

Hilt, Coil, Room y Material Icons extendidos quedan fuera mientras no hagan falta.

Versiones limitadas por el compilador: el proyecto usa Kotlin 2.2.10, que lee metadatos hasta 2.3. `kotlinx-serialization` 1.12.0 y `kotlinx-coroutines-test` 1.11.0 están compiladas con Kotlin 2.4 y arrastran `kotlin-stdlib` 2.4.10, por eso no compilan. Se subirán junto con el plugin de Kotlin (2.4.x), que lint ya recomienda.

## 6. Contratos que se reutilizan de la API actual

- Envoltorio `{ "status": "success" | "error", "message"?, "data"? }`.
- Bearer: `POST /auth/login` → `{ token, token_type, expires_in, usuario }`; `POST /auth/refresh`; `POST /auth/logout`; `GET /auth/me`.
- Códigos: 401 sin sesión, 403 sin permisos, 423 bloqueo tras 3 intentos.
- El móvil recibe FHIR **solo a través de `/mobile/*`** (web/ARCHITECTURE.md, convenciones). Eso resuelve la contradicción con la tabla de `mobile/ARCHITECTURE.md`, que lista también `/fhir/*`.

## 7. Cambios fuera de esta carpeta (no se hacen desde la app)

**Estado el 10 de noviembre de 2026 (etapa 10):** `dev` ya trae el rol `PACIENTE` (1), `GET perfil|citas|recetas|resultados`, `POST consentimientos|solicitudes-arco` (3, parcial), las tablas de citas y de lo demás (4), el 403 en `/mobile` para tokens de personal (5, parcial) y el 426 (6). Sigue faltando: OTP, PKCE y refresh token (2); aviso de privacidad, consulta de consentimientos, revocación, consulta de solicitudes ARCO, detalle de receta y contenido de resultados bajo `/mobile` (3); el 403 para tokens de paciente fuera de `/mobile` (5; hoy un paciente lee a otros, ver los hallazgos de la etapa 10); FHIR bajo `/mobile` (el perfil llega como JSON propio); y actualizar `matriz_trazabilidad.md` (7).

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
3. **`minSdk` 29 contra el mensaje en Android 9:** con `minSdk` 29 la app no se instala en Android 9 y el mensaje de RNF-006 no se vería (hoy solo se ve con un 426). Se mantuvo `minSdk = 29` como pide la especificación y la comprobación de versión existe (`verificarDispositivo`), pero solo se ejerce en pruebas unitarias. Para mostrar el mensaje en Android 9 habría que bajar `minSdk`. **Sigue abierta.**
4. **Almacenamiento de tokens** (adoptado en la etapa 2): Keystore directo con AES-GCM. La especificación nombra `EncryptedSharedPreferences`, que está marcado como obsoleto; el criterio de aceptación ("sin tokens en claro") se cumple igual.
5. **Cómo se obtiene el código PKCE** (adoptado): el repositorio expone `iniciarSesion(correo, contraseña)` y esconde el intercambio. Hay que definirlo con el backend; la especificación lista `POST /mobile/auth/token` pero ningún endpoint que entregue el código.
6. **Identificación del dispositivo para el OTP** (adoptado): UUID de instalación guardado en la app. No se define cómo sabe el servidor que el dispositivo es nuevo.
7. **Datos sin definir:** valores de `CITA.estado` y de `derecho` en ARCO, fuente del texto del aviso integral. Longitud del OTP: se asumió 6 (constante `LONGITUD_OTP`).
8. **Pines de certificado** del servidor real.
9. **Aseguradora (AMB-018):** solo existe `seguro_activo`. Cómo viaja en el Patient FHIR y en qué sistema va la CURP (supuestos de la etapa 3, ver arriba).
10. **Contrato de citas y resultados:** la especificación no los define (formato, valores de `CITA.estado`, tipos de archivo y tamaño máximo, AMB-012).
11. **Aviso antes del login** (adoptado en la etapa 6): aviso simplificado empaquetado antes de Login; el vigente, con sus finalidades, sigue después del OTP. Pendiente que Legal valide el texto y la URL.
12. **Contrato de privacidad y ARCO:** ver los supuestos de la etapa 5; la especificación no define los endpoints de lectura ni los valores de los derechos.
13. **Commits:** `AGENTS.md` pide tu autorización previa y el 100% de las pruebas.
14. **Root y emuladores** (adoptado): los emuladores sin Play Store traen `test-keys` y no son root. El detector de debug ignora esa señal (`SimulacionRoot.kt`, un test lo comprueba) para poder usar emuladores en la matriz de dispositivos de la etapa 7; el release la mantiene.
15. **Token tras un bloqueo por root:** la sesión se borra en local sin revocarla en el servidor (no se envían peticiones desde un dispositivo bloqueado); el token vive hasta que vence. Si Seguridad quiere revocación inmediata, tiene que hacerse del lado del servidor.
16. **Conexión a los endpoints normales** (adoptada el 8 de noviembre de 2026; **reemplazada por la decisión 19**, etapa 10): la app consulta el backend actual. Consecuencias que el PO aceptó: (a) entra **personal**, no el paciente, y elige de quién ve los datos; (b) **se envía `id_paciente`** (RF-002.1 ya no se cumple al pie de la letra: `SoloLecturaTest` lo limita al único archivo `ApiBackend.kt`); (c) sin OTP ni PKCE (lo de PROV-SEC-001 y PROV-SEC-002 sobre OTP y PKCE solo existe con datos simulados); (d) no hay citas; (e) las recetas y resultados solo los ve el médico dueño del expediente; (f) la app marca `acreditacion_identidad = true` en ARCO (supuesto a confirmar con Legal). Si más adelante se crea `/mobile`, el cambio queda acotado a `data/remoto` y a la pantalla de paciente.
17. **Producción sin los endpoints del Sprint 4:** Render sirve `main`. Hay que desplegar `dev` (y corregir la autenticación de `/fhir/*`) para que el release funcione.
18. **Estilo:** la guía "Aetna Health App" manda sobre el estilo anterior (basado en la web), salvo donde chocaría con WCAG AA; ahí se oscurece lo mínimo (etapa 9). Si el PO quiere los colores exactos de la guía, hay que relajar `ContrasteTest` y asumir el 3,7:1 del blanco sobre el azul.
19. **Portal móvil del Sprint 5** (adoptada el 10 de noviembre de 2026, etapa 10): la app usa solo `/api/v1/mobile/…` y el login; ninguna petición lleva `id_paciente`. Dos decisiones propias, porque el portal no cubre todo, y que el PO debe confirmar: (a) **contenido de los resultados:** el portal lista los resultados pero no los entrega, así que la app sigue leyendo `GET /solicitudes-estudio/{id}/resultados`, la única llamada fuera de `/mobile`, y solo con el id de un resultado de la propia lista; si el backend la restringe al personal, el visor deja de funcionar hasta que el portal tenga su endpoint (`SoloLecturaTest` la deja como única excepción); (b) **lo que no existe en el portal** (consulta de consentimientos, revocación, consulta de solicitudes ARCO) **no se completa con endpoints de personal**, porque piden `id_paciente` y hoy no filtran por dueño: la app guarda en el teléfono lo aceptado, no puede revocar y muestra solo las solicitudes ARCO de la sesión. Si el PO prefiere los endpoints de personal para esas tres funciones, el cambio queda en `data/remoto`.

## 9. Verificación en cada etapa

```bash
cd vitae-mobile
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # JDK 21 de Android Studio
./gradlew assembleDebug testDebugUnitTest lintDebug
# con teléfono o emulador conectado:
./gradlew connectedDebugAndroidTest
```

Qué no se puede verificar: pinning contra el servidor de producción y todo lo que dependa de los endpoints del Sprint 4 desplegados (hoy solo existen en el backend local). El tráfico real se probó contra Laravel local (etapa 8); OTP y PKCE no existen en el backend actual.

Pruebas instrumentadas (datos simulados) y conexión real:

```bash
./gradlew assembleDebug assembleDebugAndroidTest -Pvitae.backend=simulado   # las instrumentadas exigen esto
# conectado al backend local: php artisan serve --port=8080, y en el teléfono
adb reverse tcp:8080 tcp:8080
./gradlew installDebug -Pvitae.apiUrl=http://localhost:8080/api/v1/
```
