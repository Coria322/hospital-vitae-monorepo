# 3. Sesión y almacenamiento seguro (PROV-SEC-001, PROV-SEC-002, RF-010)

| Control | Prueba | Dónde |
|---|---|---|
| La sesión se guarda cifrada (AES-GCM con una clave del Android Keystore que no sale del dispositivo) y se lee entre instancias | `KeystoreTokenStoreTest.guardaYLeeLaSesionEntreInstancias` | teléfono (Keystore real) |
| **No quedan tokens en texto plano** en el archivo de preferencias | `KeystoreTokenStoreTest.noDejaTokensEnClaro` | teléfono |
| Al borrar, se elimina la sesión pero se conserva el id de instalación (no es secreto) | `KeystoreTokenStoreTest.borrarEliminaLaSesionPeroConservaElIdDeInstalacion` | teléfono |
| Cierre por 5 minutos de inactividad, exacto a los 300 s; la actividad reinicia el plazo; comprobar al volver de segundo plano | `InactivityMonitorTest` (4, reloj virtual) | JVM |
| Renovación del access token y expiración ante un refresh inválido; cierre voluntario revoca el token | `SessionManagerTest` (7) | JVM |
| PKCE S256 | `PkceTest` (3) | JVM |
| Un 401 con sesión invalida la sesión; un 403 no | `ApiClientTest` | JVM, MockWebServer |
| Arranque en frío: no se restaura la sesión anterior, se descarta y se revoca | `SessionManagerTest` | JVM |
| Un resultado clínico se descarga a `cacheDir` (nunca a almacenamiento compartido) y se borra al salir del visor | `ClinicoTest.elArchivoDelVisorSeBorraAlSalir` | teléfono |
| Sin sesión no queda ningún resultado en disco (también tras un proceso muerto) | `AppContainer.init` borra `cacheDir/resultados` si no hay sesión | revisado en código |
| Ni respaldo ni transferencia de datos de la app | ver [02](02-root-y-hardening.md) | JVM |
| Sin `Log` ni `println` con datos | 0 coincidencias en `main`, `debug` y `release` | búsqueda en el código |

## Qué NO se verificó

- **Los 5 minutos reales de inactividad en el teléfono**: la lógica se prueba con reloj virtual, no esperando 5 minutos.
- **Cifrado a nivel de archivo del sistema**: se confía en el Keystore de la plataforma; no se auditó el contenido del almacenamiento de la app con herramientas forenses.
- **Almacenamiento del `id_instalacion`** en preferencias simples: es un UUID de instalación, no un secreto, pero si el servidor lo usa para decidir si pide OTP, su sensibilidad depende de esa regla del backend (decisión abierta 6 del plan).
