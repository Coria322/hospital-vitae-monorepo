# 7. Conexión de la app a los endpoints normales del backend

Decisión del Product Owner del 8 de noviembre de 2026: la app no usa `/api/v1/mobile/*` (no existe); se conecta a los endpoints normales de `vitae-backend` (ver PLAN.md, etapa 8 y decisiones 16 y 17).

## Cómo se probó

- **Backend:** la rama `dev` (Sprint 4) corriendo en el equipo con `php artisan serve` (PHP 8.2.12, SQLite), migraciones y seeders sintéticos. Se añadieron por API datos sintéticos: 3 recetas (una vigente con QR, una dispensada y una sin firmar), 2 solicitudes de estudio (una con resultado de texto y otra pendiente), consentimientos y solicitudes ARCO.
- **App:** compilación de debug con el backend real (por omisión) en un Xiaomi M2007J20CG con Android 12, usando `adb reverse tcp:8080 tcp:8080` y `-Pvitae.apiUrl=http://localhost:8080/api/v1/`.
- **Acciones con las que se manejó el teléfono:** entradas de texto y toques por `adb`, leyendo la pantalla con `uiautomator`. Las credenciales son las del seeder del repositorio (usuarios sintéticos del backend local).

## Resultado, pantalla por pantalla

| Pantalla | Qué se comprobó | Resultado |
|---|---|---|
| Aviso previo → Login | El campo dice "Usuario" (modo backend real) | OK |
| Login | `POST /auth/login` con el usuario `MED-001`; sesión de 15 min | OK |
| Elegir paciente | `GET /pacientes` muestra los 3 pacientes sintéticos (el de urgencias, por su folio) | OK |
| Aviso de privacidad | Estado leído de `GET /consentimientos`; aceptar con una finalidad opcional creó **2 filas** en el backend (aviso y finalidades) con `medio = APP_MOVIL` y el responsable que inició sesión | OK |
| Inicio | Nombre del Patient FHIR real, 2 recetas activas, última biometría; el aviso de privacidad ya aceptado se salta solo | OK |
| Citas | "Esta sección aún no está disponible en el servidor." sin botón de reintento (no hay endpoint de agenda) | OK |
| Recetas | 3 recetas reales con su estado; el detalle de la vigente muestra el QR y el de la dispensada "Código QR no válido" | OK |
| Resultados | Solo aparece la solicitud con resultado; el visor muestra el texto descifrado del backend | OK |
| Perfil | Nombre, CURP y fecha de nacimiento del Patient FHIR y el seguro "Activo" del detalle del paciente | OK |
| ARCO | Se creó una solicitud de "Oposición"; el backend la guardó con folio y `acreditacion_identidad = true`; la lista muestra la anterior | OK |
| Privacidad | Revocar la finalidad opcional creó una fila `REVOCADO` enlazada a la original | OK |

Sin caídas (`FATAL EXCEPTION`: 0). La misma ruta se repitió con la compilación minificada por R8 (`-Pvitae.minificarDebug=true`): login, elección de paciente, Inicio y Recetas sin excepciones de clases ni de serialización.

## Pruebas automáticas

- 36 pruebas unitarias nuevas (227 en total, todas en verde): 30 en `data/remoto` sobre **13 respuestas reales del backend** guardadas como fixtures (sin tokens), 4 de `PacienteViewModel` y 2 del resumen de Inicio.
- Las 44 instrumentadas siguen en verde con `-Pvitae.backend=simulado` (exigido por `AppTest`, que falla con un mensaje claro si se corre contra el backend real).
- `SoloLecturaTest` limita `id_paciente` a un archivo y las escrituras a `auth`, `consentimientos` y `solicitudes-arco`.

## Qué NO se verificó

- **Producción:** el servidor de Render (`auditoria-backend-u47a.onrender.com`) **no tiene** FHIR, recetas, consentimientos, ARCO ni laboratorio: sus rutas responden 404. Corre lo de `main`, hasta el Sprint 3. El release no puede funcionar allí hasta que se despliegue `dev`.
- **Pines de certificado** del servidor de producción (`API_PINES` sigue vacío; el release se niega a arrancar sin ellos).
- **OTP y PKCE:** el backend actual no los tiene; solo existen con datos simulados.
- **Recetas y resultados de otro médico:** el backend solo entrega el expediente al médico dueño o con acceso de emergencia; con otro usuario la app mostrará "sin permiso" (probado en unitarias con un 403 simulado, no en el teléfono).
- **Resultados en PDF o imagen:** el backend los devuelve como texto dentro del JSON; un archivo binario no cabe en ese formato y no se probó.
- **Pantalla "Elegir paciente"** con TalkBack y con una prueba instrumentada propia: las instrumentadas corren con datos simulados, donde esa pantalla no existe.

## Hallazgo de seguridad del backend

Las rutas `GET /api/v1/fhir/Patient/{id}`, `Encounter`, `Observation` y `MedicationRequest` de `dev` **no exigen token**: `Patient` responde 200 con nombre, CURP, fecha de nacimiento y domicilio al consultarlo por id **o por CURP** sin autenticar (confirmado en el backend local; `pacientes` y `recetas` sin token sí dan 401). No están en producción. Hay que ponerlas detrás de `auth:sanctum` antes de desplegar el Sprint 4. No se tocó: está fuera de `vitae-mobile/`.
