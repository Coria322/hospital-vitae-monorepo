# 5. Cobertura, contrato FHIR, solo lectura e `id_paciente` (TASK-M-015, M-018, M-019)

## Cobertura de líneas (MC-10 ≥ 80 %)

Medida con JaCoCo integrado en AGP: `./gradlew createDebugUnitTestCoverageReport` y `node cobertura.mjs` (en esta carpeta). El script sale con código 1 si ViewModels o la lógica de datos bajan de 80 %.

| Grupo | Líneas cubiertas | % |
|---|---|---|
| **ViewModels** | 252 / 252 | **100,0 %** |
| **Lógica de datos de producción** (`data/**` sin clases de Android, cableado ni dobles de prueba) | 311 / 330 | **94,2 %** |
| Dobles de prueba de debug (`Mock*`, informativo) | 139 / 175 | 79,4 % |
| Todo el código (informativo) | 782 / 1852 | 42,2 % |

**Cómo leer estas cifras**

- El 42,2 % global es bajo porque las pantallas Compose (`ui/**/*Screen.kt`, navegación, tema) no se miden en las pruebas unitarias: se ejercitan en las 44 pruebas instrumentadas del teléfono, y esa cobertura **no** se incluye aquí.
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

## `id_paciente` (RF-002.1)

| Control | Prueba |
|---|---|
| El código fuente (`main`, `debug`, `release`) nunca menciona `id_paciente` | `SoloLecturaTest.elCodigoNuncaMencionaIdPaciente` |
| Una petición real a través de `ApiClient` no lleva `id_paciente` en ruta, query, cabeceras ni cuerpo | `ApiClientTest.respuestaExitosaLlevaBearerYNoEnviaIdPaciente` |

**No verificado**: la "inspección de tráfico con proxy" que pide la tarea. La app todavía no habla con ningún servidor real, así que no hay tráfico que capturar. Cuando existan los repositorios remotos, hay que añadir una prueba que ejecute cada operación contra MockWebServer y aplique `assertSinIdPaciente` a cada petición.

## Contrato FHIR (RNF-007)

`ContratoFhirTest` lee todo `.json` de `app/src/test/resources/contrato/fhir/`: **7 válidos** (Patient completo y mínimo; MedicationRequest activa, dispensada y cancelada; Bundle con 2 recetas y vacío) deben leerse, y **8 inválidos** (sin `resourceType`, de otro tipo, JSON truncado, tipos erróneos, receta sin `id`, Bundle con una entrada inválida, `OperationOutcome` en lugar de Bundle) deben dar `ApiError.Desconocido` sin lanzar excepciones. Las pruebas `FhirParserTest` (9) y `FhirRecetasTest` (12) cubren los casos de detalle.

Los ejemplos son sintéticos y los **supuestos** de forma (extensiones `urn:vitae:*`, sistema de la CURP) están en `PLAN.md`. **No verificado**: la colección compartida (TASK-W-091) ni Newman contra Staging; no existen. Cuando haya ejemplos reales del backend se copian a esas carpetas sin tocar el código.
