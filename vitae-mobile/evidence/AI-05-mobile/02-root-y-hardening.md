# 2. Root, Magisk y endurecimiento de la app (PROV-SEC-011, TASK-M-007 y M-014)

## Detección de root (local, de mejor esfuerzo)

`DetectorRoot` busca: binarios `su` (12 rutas), rastros de Magisk (4 rutas), gestores de root instalados (8 paquetes, declarados en `<queries>` del manifiesto), `Build.TAGS` con `test-keys` y `/system` montado en lectura y escritura. Las señales no se muestran al usuario.

| Control | Prueba | Dónde |
|---|---|---|
| Cada señal detecta root; un teléfono oficial no | `DetectorRootTest` (12) | JVM |
| El manifiesto declara los mismos paquetes que el detector | `DetectorRootTest.elManifiestoDeclaraLosMismosPaquetesQueElDetector` | JVM |
| Android < 10 es "incompatible" aunque tenga root; root bloquea | `DetectorRootTest`, `GuardiaDispositivoTest` (7) | JVM |
| Un 426 bloquea y no se levanta al re-verificar; root gana al 426 | `GuardiaDispositivoTest` | JVM |
| Con el dispositivo bloqueado no sale ninguna petición (`requestCount == 0`) | `DispositivoInterceptorTest` (4) | JVM, MockWebServer |
| Debug ignora `test-keys` (emuladores) pero detecta lo demás | `DetectorRootDebugTest` (3) | JVM |
| Con root: pantalla genérica con "Salir", sin formulario de login | `SeguridadTest.conRootLaAppSeBloqueaAntesDelLogin`, `salirCierraLaApp` | teléfono |
| La pantalla no revela señales: solo título, mensaje y "Salir" | `SeguridadTest.laPantallaDeRootNoRevelaDetallesTecnicos` | teléfono |
| Con root y sesión viva, la sesión se borra en local | `SeguridadTest.conRootYSesionVivaLaSesionSeBorraEnLocal` | teléfono |
| Si el root desaparece, se vuelve a Login | `SeguridadTest.alQuitarseElRootSeVuelveAlLogin` | teléfono |
| Un 426 en vivo lleva a "Dispositivo no compatible" | `SeguridadTest.un426MuestraLaPantallaIncompatible` | teléfono |

El root se simula en debug con `SimulacionRoot` (solo existe en `src/debug`; el release usa el detector sin ganchos).

## Otros controles de endurecimiento

| Control | Evidencia |
|---|---|
| `allowBackup="false"` y reglas que excluyen todo (respaldo en la nube y transferencia) | `DetectorRootTest.noHayRespaldoNiTransferenciaDeDatos` lee el manifiesto y los dos XML |
| R8 activo en release | `assembleRelease` genera `mapping.txt` (11,8 MB); la APK de release pesa 1,4 MB frente a 33 MB la de debug |
| Las reglas keep bastan | Humo manual con el debug minificado (`-Pvitae.minificarDebug=true`) en el teléfono, etapa 6 (`3e8b446`): aviso previo → login → OTP → aviso → Inicio → receta con QR → visor de PDF, 0 caídas, 0 excepciones de clases o métodos faltantes |
| Capturas bloqueadas en pantallas clínicas (`FLAG_SECURE`) | `ClinicoTest.lasPantallasClinicasBloqueanLasCapturas` y `alCerrarSesionSeLiberaLaProteccion` (teléfono); `ContadorProteccionTest` (3) |
| Sin `Log` ni `println` en el código de la app | búsqueda en `app/src/main/java`: 0 coincidencias |

## Qué NO se verificó

- **Root real**: no hay teléfono con Magisk. La lógica se prueba con un entorno falso y con una ruta `su` simulada.
- **Falsos negativos**: un root oculto (Zygisk, Shamiko) no se detecta desde la app. Play Integrity necesita verificación en el servidor, que no existe (SUP-007).
- **Falsos positivos** en firmware personalizado o de desarrollo (`test-keys`): hay que reportarlos a Seguridad cuando ocurran.
- **Release en ejecución**: sus repositorios remotos no existen y fallan a propósito; el humo de R8 se hizo con el debug minificado.
- **Revocación del token** tras un bloqueo por root: la sesión se borra en local sin llamar al servidor (no sale ninguna petición); el token vive hasta que vence.
