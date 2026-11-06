# 4. Matriz de dispositivos (TASK-M-016, RNF-006)

La tarea pide 3 dispositivos con API ≥ 29 de distintos tamaños y 1 con API 28, con 0 caídas. **Está cumplida solo en parte**: no hay forma de completar la matriz con lo que hay instalado.

| # | Dispositivo | Android | Pantalla | Resultado |
|---|---|---|---|---|
| 1 | Xiaomi M2007J20CG (físico, MIUI), `release-keys` | 12 (API 31) | 1080 × 2400, 440 dpi | **44 pruebas instrumentadas OK, 0 caídas, 0 denegaciones del sistema** (`resultados-pruebas.md`) |
| 2 | Emulador `Medium_Phone` x86 con Google Play | 9 (API 28) | 1080 × 2400 | **El sistema rechaza la app**: `INSTALL_FAILED_OLDER_SDK … Requires newer sdk version #29 (current version is #28)` |
| 3 | — | 10 (API 29) | — | **No ejecutado**: no hay imagen de sistema |
| 4 | — | 14 o superior (API 34+) | — | **No ejecutado** |
| 5 | — | cualquiera | un segundo y tercer tamaño | **No ejecutado** |

## Lectura del renglón 2 (API 28)

Con `minSdk = 29` (lo que pide la especificación) la app **no se puede instalar** en Android 9, así que el "mensaje de incompatibilidad" de RNF-006, escenario 2, no lo puede mostrar la app: lo muestra el instalador del sistema. La app solo muestra su pantalla "Dispositivo no compatible" ante un HTTP 426. Para que la app mostrara el mensaje en Android 9 habría que bajar `minSdk`; la decisión 3 del plan sigue abierta.

## Qué falta y quién lo puede hacer

- **Imágenes de emulador** de API 29 y de API 34 o superior, con 2 tamaños de pantalla distintos del actual (por ejemplo, un teléfono pequeño y una tableta). Se instalan desde *Android Studio → SDK Manager*; ocupan 1–2 GB cada una. En este equipo hay aceleración WHPX, así que los emuladores x86_64 funcionan.
- El detector de root de debug ignora `test-keys` justamente para poder usar esos emuladores sin que se bloqueen.
- Alternativa que pide la tarea: Firebase Test Lab (necesita cuenta y es un servicio externo; no se usó).
