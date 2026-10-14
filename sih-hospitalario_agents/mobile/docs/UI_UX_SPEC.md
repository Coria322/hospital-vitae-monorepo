# UI_UX_SPEC.md — SIH · mobile/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Requerimientos UX: 1 | UI: 1 | Motion: 0
> Fecha: 2026-10-09

---

## NAVIGATION FLOW
```
Splash ── Incompatible (API<29) | Bloqueada por root
└── Login ── OTP (dispositivo nuevo)
    └── Aviso de privacidad (1ª vez / versión nueva)
        └── Inicio
            ├── Citas (lista)
            ├── Recetas (lista → detalle + QR)
            ├── Resultados (lista → visor PDF/imagen)
            ├── Perfil y aseguradora
            └── Solicitud ARCO (crear / estado)
Sesión expirada (5 min) → Login
```

| Pantalla | Acceso | Fuente |
|---|---|---|
| Login / OTP | público | PROV-SEC-001 |
| Aviso de privacidad | autenticado | PROV-RN-008 |
| Inicio, Citas, Recetas, Resultados, Perfil | PACIENTE | RF-011 |
| ARCO | PACIENTE | PROV-RN-009 |
| Incompatible / Root / Sesión expirada | sistema | RNF-006, PROV-SEC-011 |

> Sin prototipo móvil en el ERS (AMB-009). La app NO tiene botones de guardar/editar/eliminar sobre registros clínicos (RF-011).

## COMPONENTS
- **Listas (citas, recetas, resultados):** tarjetas; estado vacío con mensaje; cargando con skeleton; error con reintento; sin acciones por fila salvo "Abrir".
- **Visor de resultados:** interno; sin guardar a almacenamiento compartido; `FLAG_SECURE`.
- **Detalle de receta:** medicamento, dosis, vía, duración, estado (activa/dispensada/cancelada), QR (inválido si dispensada).
- **Formularios:** solo login, OTP, ARCO y consentimiento (escrituras de lista blanca).
- **Alerts:** diálogos para 401/403/423/426; no persisten datos clínicos.

## UX RULES
| Regla | Valor | Fuente |
|---|---|---|
| Inactividad | 5 min → login y borrado local | PROV-SEC-001 |
| Contraste | >= 4.5:1 | PROV-UX-003 |
| Objetivo táctil | >= 48 dp | PROV-UX-003 |
| TalkBack | contentDescription en todos los controles | PROV-UX-003 |
| Color | nunca único indicador | WCAG 2.1 AA |
| Android | >= 10 (API 29); anterior → mensaje controlado | RNF-006 |
| Mensajes de error | genéricos en autenticación | PROV-SEC-001 |

## MOTION / ANIMATIONS
Sin especificaciones de motion en el ERS fuente.
