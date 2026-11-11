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

## `id_paciente` (RF-002.1): se cumple otra vez desde el 10 de noviembre de 2026

La especificación pide que la app nunca envíe `id_paciente`. Entre el 8 y el 10 de noviembre no se cumplió al pie de la letra, porque la app usaba los endpoints de personal (PLAN.md, decisión 16). Con el portal móvil del Sprint 5 (etapa 10, decisión 19) la identidad sale del token del paciente y la app ya no envía el identificador:

| Control | Prueba |
|---|---|
| `id_paciente` no aparece en **ningún** archivo de `src/main`, `src/debug` ni `src/release` | `SoloLecturaTest.idPacienteNoApareceEnNingunArchivo` |
| Las escrituras de Retrofit se limitan a `auth`, `mobile/citas` (pedir una cita, etapa 11), `mobile/consentimientos` y `mobile/solicitudes-arco`; recetas, resultados y perfil siguen siendo de solo lectura | `SoloLecturaTest.ningunaEscrituraDeRetrofitSaleDeLaListaBlanca` |
| Las lecturas salen de `/mobile`, salvo el contenido de un resultado (`solicitudes-estudio/{id}/resultados`, decisión 19) | `SoloLecturaTest.lasLecturasSalenDelPortalMovilSalvoElContenidoDeUnResultado` |
| Una petición real a través de `ApiClient` no lleva `id_paciente` en ruta, query, cabeceras ni cuerpo | `ApiClientTest.respuestaExitosaLlevaBearerYNoEnviaIdPaciente`, `ClinicoRemotoTest.elPerfilSaleDeMobilePerfilSinMandarNingunId` |
| El consentimiento y la solicitud ARCO no llevan `id_paciente` en el cuerpo | `PrivacidadArcoRemotoTest` |

**Del lado del servidor**, un `id_paciente` ajeno en `/mobile` responde 403 (`IDOR_VIOLATION_DETECTED`, comprobado a mano contra el backend local). Fuera de `/mobile` el backend sí deja a un paciente leer a otros: ver `07-conexion-backend.md`.

**No verificado**: la inspección de tráfico con proxy que pide TASK-M-019 (se probó el flujo contra el backend local, pero sin capturar con un proxy).

## Contrato FHIR (RNF-007)

`ContratoFhirTest` lee todo `.json` de `app/src/test/resources/contrato/fhir/`: **7 válidos** (Patient completo y mínimo; MedicationRequest activa, dispensada y cancelada; Bundle con 2 recetas y vacío) deben leerse, y **8 inválidos** (sin `resourceType`, de otro tipo, JSON truncado, tipos erróneos, receta sin `id`, Bundle con una entrada inválida, `OperationOutcome` en lugar de Bundle) deben dar `ApiError.Desconocido` sin lanzar excepciones. Las pruebas `FhirParserTest` (9) y `FhirRecetasTest` (12) cubren los casos de detalle.

Los ejemplos son sintéticos. Además, `app/src/test/resources/backend` guarda **respuestas reales del portal móvil** (login de paciente y de personal, perfil, citas, recetas, resultados, consentimiento, ARCO, renovación y error de login), capturadas del Laravel local de `dev` con datos sintéticos y sin tokens; las pruebas de `data/remoto` las leen. **Desde la etapa 10 el perfil llega como JSON propio (`GET /mobile/perfil`), no como FHIR**: `FhirParser` sigue probado con los ejemplos de arriba, pero la app con el backend real ya no lo usa (RNF-007 queda cubierto solo en el analizador; el portal móvil no entrega FHIR).

**No verificado**: la colección compartida (TASK-W-091) ni Newman contra Staging; no existen. Render ya sirve el portal móvil y los endpoints del Sprint 4 (responden 401 sin token), pero no se probó contra él.
