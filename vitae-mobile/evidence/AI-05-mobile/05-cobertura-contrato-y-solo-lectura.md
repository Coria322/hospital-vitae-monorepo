# 5. Cobertura, contrato FHIR, solo lectura e `id_paciente` (TASK-M-015, M-018, M-019)

## Cobertura de líneas (MC-10 ≥ 80 %)

Medida con JaCoCo integrado en AGP: `./gradlew createDebugUnitTestCoverageReport` y `node cobertura.mjs` (en esta carpeta). El script sale con código 1 si ViewModels o la lógica de datos bajan de 80 %.

| Grupo | Líneas cubiertas | % |
|---|---|---|
| **ViewModels** | 280 / 280 | **100,0 %** |
| **Lógica de datos de producción** (`data/**` sin clases de Android, cableado ni dobles de prueba) | 520 / 554 | **93,9 %** |
| Dobles de prueba de debug (`Mock*`, informativo) | 139 / 175 | 79,4 % |
| Todo el código (informativo) | 1029 / 2194 | 46,9 % |

**Cómo leer estas cifras**

- El 46,9 % global es bajo porque las pantallas Compose (`ui/**/*Screen.kt`, navegación, tema) no se miden en las pruebas unitarias: se ejercitan en las 44 pruebas instrumentadas del teléfono, y esa cobertura **no** se incluye aquí.
- La "lógica de datos" excluye a propósito: `KeystoreTokenStore` y `AvisoPrevioLocal` (necesitan el runtime de Android; las prueba `KeystoreTokenStoreTest` y `AvisoPrevioTest` en el teléfono), `EntornoAndroid`, `ApiConfig`, las `Fabrica` (cableado) y los `Mock*`. Quien audite puede cambiar los filtros en `cobertura.mjs`.
- **Los repositorios remotos de producción aún no existen** (los de release fallan a propósito). Cuando se escriban, MC-10 se mide de nuevo con ellos. Hoy lo que hay bajo "repositorios" son los dobles de debug.

## Solo lectura sobre recursos clínicos (RF-011)

| Control | Prueba |
|---|---|
| Ningún control de edición en las pantallas clínicas: sin campos de texto y sin botones cuyo texto sea guardar, editar, eliminar, borrar, modificar, agendar, cancelar, crear, enviar, etc. Recorre Inicio, Citas, Recetas, detalle, Resultados, visor y Perfil. | `SinEdicionTest` (teléfono) |
| Los repositorios clínicos solo exponen lecturas: `citas`; `perfil`; `recetas`, `receta`; `resultados`, `archivo`. Una función nueva rompe la prueba. | `SoloLecturaTest.losRepositoriosClinicosSoloExponenLecturas` |
| Las únicas escrituras son las de la lista blanca SUP-002 (auth, consentimientos, solicitudes ARCO) | `SoloLecturaTest.lasEscriturasSonSoloLasDeLaListaBlanca` |
| Ninguna anotación Retrofit `@POST/@PUT/@DELETE/@PATCH` (ni `@HTTP`) apunta fuera de esa lista | `SoloLecturaTest.ningunaEscrituraDeRetrofitSaleDeLaListaBlanca` |

**Prueba de que las pruebas detectan**: se inyectó en Inicio un botón "Guardar" y `SinEdicionTest` falló con `Controles de edición: [Inicio: 'Guardar']`; se inyectó `@retrofit2.http.DELETE("mobile/recetas/1")` y `SoloLecturaTest` falló. (La primera versión de la regex no detectaba la anotación calificada; se corrigió.) Los cambios se revirtieron.

**No verificado**: el rechazo `403/405` de `POST/PUT/DELETE` clínicos **del lado del servidor** (TASK-W-080); la app solo garantiza que no los envía.

## `id_paciente` (RF-002.1): excepción decidida el 8 de noviembre de 2026

La especificación pide que la app nunca envíe `id_paciente`. Los endpoints actuales del backend son de personal e identifican al paciente con un id en la ruta o el cuerpo, y el Product Owner decidió conectar la app a ellos (PLAN.md, etapa 8 y decisión 16). **RF-002.1 ya no se cumple al pie de la letra.** Lo que se conserva es el control:

| Control | Prueba |
|---|---|
| `id_paciente` aparece en **un solo archivo**, `data/remoto/ApiBackend.kt` (el contrato del backend); en cualquier otro, la prueba falla | `SoloLecturaTest.idPacienteSoloApareceEnElContratoDelBackend` |
| Las escrituras de Retrofit se limitan a `auth`, `consentimientos` y `solicitudes-arco`; recetas, expedientes, estudios y pacientes siguen siendo de solo lectura | `SoloLecturaTest.ningunaEscrituraDeRetrofitSaleDeLaListaBlanca` |
| Una petición real a través de `ApiClient` no lleva `id_paciente` en ruta, query, cabeceras ni cuerpo (para las llamadas que no lo necesitan) | `ApiClientTest.respuestaExitosaLlevaBearerYNoEnviaIdPaciente` |

**Consecuencias aceptadas**: entra personal, no el paciente; la app pide elegir un paciente tras el login; sin OTP ni PKCE.

**No verificado**: la inspección de tráfico con proxy que pide TASK-M-019 (se probó el flujo contra el backend local, pero sin capturar con un proxy).

## Contrato FHIR (RNF-007)

`ContratoFhirTest` lee todo `.json` de `app/src/test/resources/contrato/fhir/`: **7 válidos** (Patient completo y mínimo; MedicationRequest activa, dispensada y cancelada; Bundle con 2 recetas y vacío) deben leerse, y **8 inválidos** (sin `resourceType`, de otro tipo, JSON truncado, tipos erróneos, receta sin `id`, Bundle con una entrada inválida, `OperationOutcome` en lugar de Bundle) deben dar `ApiError.Desconocido` sin lanzar excepciones. Las pruebas `FhirParserTest` (9) y `FhirRecetasTest` (12) cubren los casos de detalle.

Los ejemplos son sintéticos. Además, `app/src/test/resources/backend` guarda **13 respuestas reales del backend** (login, pacientes, Patient FHIR, expediente, recetas, resultado, consentimientos y ARCO, capturadas del Laravel local de `dev` con datos sintéticos y sin tokens) y los 34 tests de `data/remoto` y `PacienteViewModelTest` las usan: así el contrato que asume la app es el que el backend emite hoy.

**No verificado**: la colección compartida (TASK-W-091) ni Newman contra Staging; no existen. El servidor de producción (Render) tampoco tiene todavía FHIR ni los endpoints del Sprint 4.
