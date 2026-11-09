# 6. Accesibilidad móvil, WCAG 2.1 AA (TASK-M-017, PROV-UX-003)

## Qué se comprobó

`AccesibilidadTest` (4 pruebas, teléfono) recorre **14 pantallas** y revisa su árbol semántico, el mismo que usa TalkBack:

| Criterio | Cómo |
|---|---|
| Todo control tiene texto o descripción que TalkBack pueda leer (WCAG 4.1.2) | cada nodo tocable, interruptor o campo editable debe tener `Text` o `ContentDescription` no vacío |
| Objetivos táctiles ≥ 48 × 48 dp (WCAG 2.5.8, guía de Material) | se mide `touchBoundsInRoot` de cada control |
| Imágenes informativas con descripción (WCAG 1.1.1) | todo nodo con rol `Image` debe tener descripción (el QR de la receta, las páginas del visor) |
| Cada pantalla tiene un encabezado (WCAG 2.4.6) | al menos un nodo con la propiedad `heading` |
| Contraste de texto ≥ 4,5 : 1 y de controles ≥ 3 : 1 (WCAG 1.4.3 y 1.4.11) | `ContrasteTest` (3) sobre los colores del tema, en la capa de unidad |
| El estado nunca depende solo del color | `ClinicoTest.recetasListaLasTresRecetasConSuEstadoEnTexto` (el estado de la receta va siempre como texto) |

Pantallas revisadas: Aviso previo, Login, Aviso de privacidad, Preferencias de privacidad, Solicitud ARCO, Inicio, Citas, Recetas, Detalle de receta, Resultados, Visor de resultado, Perfil, Sesión expirada y Dispositivo no seguro. **Resultado: 0 incidencias.**

**Prueba de que la revisión detecta**: se inyectó en Inicio un cuadro tocable de 24 dp sin etiqueta y `AccesibilidadTest` falló con `Inicio: control sin texto hablable`; se retiró la mutación y vuelve a pasar.

## Qué NO se hizo (y por qué)

- **Accessibility Scanner y TalkBack con voz real**: son auditorías manuales sobre el teléfono (activar TalkBack es un cambio de ajustes del sistema que no se hizo desde aquí). La tarea las pide: *quedan pendientes y las debe hacer una persona*.
- **Accessibility Test Framework (ATF) automático**: se intentó (`androidx.compose.ui:ui-test-junit4-accessibility`). Exigía forzar versiones de Guava y `concurrent-futures` en el APK de pruebas y, con una violación inyectada a propósito (control sin etiqueta de 24 dp), **no falló nunca**, ni al revisar la raíz ni al hacer acciones reales; una prueba que no puede fallar no es evidencia. Se descartó y se revirtió; no queda en el proyecto.
- **OTP y "Dispositivo no compatible"**: no están entre las 14 pantallas (la de OTP comparte componentes con Login; la de incompatibilidad con la de root).
- **Texto grande del sistema (fuente al 200 %) y modo oscuro**: no se probaron.
- **Orden de foco y lectura** que TalkBack sigue en cada pantalla: no se verificó más allá de que existan encabezados.
