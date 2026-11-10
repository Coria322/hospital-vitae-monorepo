# 7. Conexión de la app al portal móvil del backend

Desde el 10 de noviembre de 2026 la app usa el portal móvil de `vitae-backend` (`/api/v1/mobile/…`, Sprint 5 de `dev`, en `main` como v1.0.0) y el login. Antes (8 de noviembre) se conectó a los endpoints de personal porque el portal no existía; esa decisión quedó reemplazada (PLAN.md, etapa 10 y decisión 19).

## Cómo se probó

- **Backend:** la rama `dev` (commit `2248de8`) corriendo en el equipo con `php artisan serve` (PHP 8.2.12, SQLite), con las migraciones del Sprint 5 aplicadas y los seeders sintéticos (`PAC-001`, `PAC-002` y sus citas, recetas y resultados).
- **App:** compilación de debug con el backend real (por omisión) en un Xiaomi M2007J20CG con Android 12, usando `adb reverse tcp:8080 tcp:8080` y `-Pvitae.apiUrl=http://localhost:8080/api/v1/`.
- **Acciones con las que se manejó el teléfono:** entradas de texto y toques por `adb`, leyendo la pantalla con `uiautomator`. Las credenciales son las del seeder del repositorio (usuarios sintéticos del backend local).
- **Contrato:** las respuestas guardadas como fixtures (`app/src/test/resources/backend/mobile-*.json`) son capturas reales del backend local con datos sintéticos; el token se sustituyó por un valor de prueba.

## Resultado, pantalla por pantalla

| Pantalla | Qué se comprobó | Resultado |
|---|---|---|
| Login | `POST /auth/login` con `PAC-001`; sesión de 15 min; sin pantalla de elegir paciente | OK |
| Login con personal | `MED-001` inicia sesión en el servidor, pero la app muestra "Acceso no disponible" y revoca el token | OK |
| Aviso de privacidad | Aceptar con una finalidad opcional creó **2 filas** en el backend (aviso y finalidades) con `medio = DIGITAL_MOVIL`, el nombre del perfil como firmante y `MED-001` (médico de la cita) como responsable | OK |
| Inicio | Nombre real del perfil, próxima cita (13/11/2026 04:00 hora local), 2 recetas activas y última biometría | OK |
| Citas | 2 citas reales del portal, con su estado (`CONFIRMADA`, `CANCELADA`) | OK |
| Recetas | 3 recetas reales con su estado; la lista trae todo lo que muestra el detalle | OK |
| Resultados | La biometría de texto; el visor muestra el contenido descifrado | OK |
| Perfil | Nombre, CURP, fecha de nacimiento y seguro "Activo" de `GET /mobile/perfil` | OK |
| ARCO | Una solicitud de "Rectificación" creó la fila `RECTIFICACION` con folio y plazos que la app mostró | OK |
| Privacidad | Aceptar la finalidad opcional desde Inicio creó la fila `FINALIDADES_SECUNDARIAS` | OK |

Pruebas negativas del portal contra el backend local (sin la app, con `PAC-001`): sin `X-Client-Version` responde 400 (`CLIENT_VERSION_REQUIRED`); con `API-28`, 426 (`UPGRADE_REQUIRED`); con el `id_paciente` de otro, 403 (`IDOR_VIOLATION_DETECTED`); un token de personal, 403 (`FORBIDDEN_ROLE`); `POST /mobile/recetas`, 403 (`CLINICAL_WRITE_FORBIDDEN`); sin token, 401; un derecho ARCO en minúsculas, 422.

## Pruebas automáticas

- 228 pruebas unitarias en verde. Las de `data/remoto` usan las respuestas reales guardadas como fixtures: login de paciente y de personal, perfil, citas, recetas, resultados, consentimiento y ARCO. `SoloLecturaTest` exige que `id_paciente` no aparezca en ningún archivo, que las escrituras se limiten a `auth`, `mobile/consentimientos` y `mobile/solicitudes-arco`, y que la única lectura fuera de `/mobile` sea el contenido de un resultado.
- Las instrumentadas siguen con `-Pvitae.backend=simulado` (exigido por `AppTest`, que falla con un mensaje claro si se corre contra el backend real).

## Qué NO se verificó

- **Render:** responde 401 en `mobile/perfil`, `citas`, `recetas`, `resultados` y `expediente`, así que ya sirve el portal; **no se inició sesión allí** y el flujo no se probó contra producción.
- **Pines de certificado** del servidor de producción (`API_PINES` sigue vacío; el release se niega a arrancar sin ellos).
- **OTP y PKCE:** el backend no los tiene; solo existen con datos simulados.
- **Resultados en PDF o imagen:** el backend los devuelve como texto dentro del JSON; un archivo binario no cabe en ese formato y no se probó.
- **Paciente sin citas ni recetas:** no puede registrar el aviso (el servidor exige un médico responsable y la app no tiene a quién nombrar); se cubre en unitarias, no en el teléfono.
- **Revocar finalidades, consultar solicitudes ARCO anteriores y actualizar su estado:** el portal no tiene esos endpoints; la app lo dice como "no disponible" y se probó en unitarias.

## Hallazgos de seguridad del backend

Confirmados en el backend local con el token de `PAC-001` (paciente) y datos sintéticos; no se corrigen desde la app.

| Hallazgo | Evidencia |
|---|---|
| El aislamiento solo cubre `/mobile`: un paciente lee a otros fuera de ese prefijo | `GET /pacientes` (lista de todos), `GET /pacientes/{otro}`, `GET /recetas/{id}` y `GET /solicitudes-estudio/{id}/resultados` responden 200 |
| Consentimientos y ARCO se consultan por `id_paciente` sin comprobar el dueño | `GET /consentimientos?id_paciente=…` y `GET /solicitudes-arco?id_paciente=…` responden 200 |
| Las rutas FHIR no exigen token | `GET /fhir/Patient/{id}` responde 200 sin `Authorization` (confirmado en local; en Render, `fhir/Patient/x` responde 500 en lugar de 404) |
| El portal expone una ruta interna | `GET /mobile/resultados` incluye `archivo_cifrado_ref` (p. ej. `laboratorio/….enc`) |
| El consentimiento exige un médico | `POST /mobile/consentimientos` pide `medico_responsable_id` existente: un paciente sin médico no puede aceptar el aviso |
