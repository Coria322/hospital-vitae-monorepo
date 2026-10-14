# UI_UX_SPEC.md — SIH · web/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Requerimientos UX: 3 | UI: 2 | Branding: 1 | Motion: 0
> Fecha: 2026-10-09

---

## NAVIGATION FLOW

```
/login ── /mfa
└── / (según rol)
    ├── /admin/usuarios · /admin/auditoria · /admin/dashboards   ← ADMINISTRADOR (auditoría: SEG/AUDITOR)
    ├── /triage/nuevo · /triage/cola · /sala-espera              ← ENFERMERIA / RECEPCION / MEDICO
    ├── /pacientes · /expedientes/{id} · /notas                 ← MEDICO
    ├── /recetas/nueva · /recetas/{id}                          ← MEDICO
    ├── /estudios/solicitudes · /estudios/bandeja                ← MEDICO / LABORATORISTA
    ├── /agenda                                                  ← RECEPCION
    └── /privacidad/consentimientos · /privacidad/arco           ← admisión
```

| Pantalla | Ruta | Acceso | Fuente |
|---|---|---|---|
| Captura de triage | `/triage/nuevo` | ENFERMERIA | PROV-UI-001, PROV-UX-001 |
| Cola / sala de espera | `/triage/cola`, `/sala-espera` | MEDICO, RECEPCION | PROV-UX-002 |
| Expediente y notas | `/expedientes/{id}` | MEDICO | PROV-UI-002 |
| Emisión de recetas | `/recetas/nueva` | MEDICO | PROV-UI-002 |
| Dashboards NOM-035 | `/admin/dashboards` | ADMINISTRADOR | PROV-RF-001 |

> Prototipos del Anexo G (G.1 triage, G.2 expediente/recetas) existen solo como imagen: AMB-009. Ocultar menús por rol es mejora UX, NO control de seguridad (RF-003).

## COMPONENTS

### Formularios (triage)
- **Fuente:** `PROV-UI-001`, `PROV-UX-001`
- **Campos:** presión arterial (sis/dia), FC (número), glucosa (número, opcional), sangre perdida (categórico, AMB-006), AVDI (4 opciones), EVA (5 opciones, AMB-007)
- **Validación:** cliente y servidor; botón de avance deshabilitado hasta completar obligatorios
- **Submit:** POST /api/v1/triages; muestra nivel con icono+texto+color
- **Error inline:** sí, junto al campo con `aria-invalid`
- **Reset:** no (evitar pérdida accidental en urgencias)

### Colas / sala de espera
- **Fuente:** `PROV-UX-002`
- **Columnas:** folio, iniciales, nivel (icono+texto+color), posición, tiempo restante
- **Actualización:** tiempo real (Reverb) con polling 5 s de contingencia
- **Estado vacío:** "Sin pacientes en espera" · **Cargando:** skeleton
- **Prohibido:** nombre completo, CURP o datos clínicos

### Modales
- **Reclasificación (RF-007):** trigger botón en paciente; contenido nivel nuevo + motivo obligatorio + cuenta regresiva 30 min; acciones Confirmar/Cancelar; cierre solo por botón; el vencimiento lo decide el backend.
- **Acción destructiva:** no existen eliminaciones clínicas; baja de usuario pide confirmación.

### Dashboards
- **Fuente:** `PROV-RF-001` · **Métricas:** indicadores agregados NOM-035 con CLUES · **Actualización:** manual + exportación · **Layout:** grid responsive (frecuencia en AMB-017)

### Alerts / Toasts
- **Tipos:** success | error | warning | info; alertas críticas (ROJO, SLA vencido) son persistentes y con icono+texto
- **Posición:** top-right; críticas: banner superior fijo · **Dismissible:** no en críticas

### Loaders
- Skeleton en listas; spinner inline en acciones; duración mínima visible 300 ms.

## UX RULES

| Regla | Valor | Fuente |
|---|---|---|
| Alertas sin recargar | <= 2 s | PROV-UX-001 |
| Pantalla pública sin PII | folio/iniciales | PROV-UX-002 |
| Operabilidad por teclado | 100% en triage y recetas | PROV-UX-003 |
| Contraste mínimo | 4.5:1 | PROV-UX-003 |
| Nivel de urgencia | nunca solo color (icono+texto) | PROV-BR-001 |
| Estado vacío | obligatorio en todas las listas | PROV-UI-001 |
| Mensajes de autenticación | genéricos | PROV-SEC-001 |
| Inactividad | 15 min | PROV-SEC-001 |
| Notas guardadas | sin editar/eliminar; solo "Agregar aclaración" | RF-008.2 |
| Impresión de receta controlada | bloqueada hasta validar e.firma | RF-009 |

## MOTION / ANIMATIONS

Sin especificaciones de motion en el ERS fuente. Regla heredada de accesibilidad: respetar `prefers-reduced-motion`; las alertas críticas no dependen de animación.

| Token | Valor | Uso |
|---|---|---|
| `--duration-fast` | ⚠️ NO_DEFINIDO | — |
| `--duration-normal` | ⚠️ NO_DEFINIDO | — |
| `--duration-slow` | ⚠️ NO_DEFINIDO | — |
