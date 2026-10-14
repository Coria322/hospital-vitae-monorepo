# BRAND_SYSTEM.md — SIH · web/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Fecha: 2026-10-09
> El ERS NO define guía de marca (AMB-010). Todo valor de diseño no mencionado se marca `⚠️ NO_DEFINIDO`; no inventar.

---

## IDENTIDAD
- **Logotipo:** ⚠️ NO_DEFINIDO (todas las variantes, zona de protección, tamaño mínimo y usos prohibidos)
- **Estilo guía mencionado (§11.1):** interfaz "funcional y óptima más allá de algo más estilizado" (fuente: Vue)

## DESIGN TOKENS — COLORES
```json
{
  "colors": {
    "primary": "⚠️ NO_DEFINIDO", "secondary": "⚠️ NO_DEFINIDO", "accent": "⚠️ NO_DEFINIDO",
    "background": "⚠️ NO_DEFINIDO", "surface": "⚠️ NO_DEFINIDO", "on_primary": "⚠️ NO_DEFINIDO",
    "on_surface": "⚠️ NO_DEFINIDO", "error": "⚠️ NO_DEFINIDO", "success": "⚠️ NO_DEFINIDO",
    "warning": "⚠️ NO_DEFINIDO", "info": "⚠️ NO_DEFINIDO"
  },
  "triage_levels": {
    "ROJO":     {"nivel": 1, "sla_min": 0,   "hex": "⚠️ NO_DEFINIDO", "icono_y_texto_obligatorios": true},
    "NARANJA":  {"nivel": 2, "sla_min": 15,  "hex": "⚠️ NO_DEFINIDO", "icono_y_texto_obligatorios": true},
    "AMARILLO": {"nivel": 3, "sla_min": 60,  "hex": "⚠️ NO_DEFINIDO", "icono_y_texto_obligatorios": true},
    "VERDE":    {"nivel": 4, "sla_min": 120, "hex": "⚠️ NO_DEFINIDO", "icono_y_texto_obligatorios": true},
    "AZUL":     {"nivel": 5, "sla_min": 240, "hex": "⚠️ NO_DEFINIDO", "icono_y_texto_obligatorios": true}
  }
}
```
**Fuente:** `PROV-BR-001`, `PROV-RN-004`. Restricción WCAG 2.1 AA: contraste texto normal >= 4.5:1.

## DESIGN TOKENS — TIPOGRAFÍA
```json
{ "typography": { "font_family_primary": "⚠️ NO_DEFINIDO", "font_family_secondary": "⚠️ NO_DEFINIDO", "font_family_mono": "⚠️ NO_DEFINIDO",
  "heading_scale": { "h1": "⚠️ NO_DEFINIDO", "h2": "⚠️ NO_DEFINIDO", "h3": "⚠️ NO_DEFINIDO" },
  "body_scale": { "large": "⚠️ NO_DEFINIDO", "default": "⚠️ NO_DEFINIDO", "small": "⚠️ NO_DEFINIDO", "caption": "⚠️ NO_DEFINIDO" } } }
```

## DESIGN TOKENS — ESPACIADO
```json
{ "spacing": { "base_unit": "⚠️ NO_DEFINIDO", "scale": "⚠️ NO_DEFINIDO" }, "touch_target_min": "⚠️ NO_DEFINIDO" }
```

## DESIGN TOKENS — MOTION
Sin especificaciones de motion en el ERS (todos `⚠️ NO_DEFINIDO`). Respetar `prefers-reduced-motion`.

## ICONOGRAFÍA
- **Librería / estilo / tamaños:** ⚠️ NO_DEFINIDO
- **Regla definida:** cada nivel de triage lleva un icono distinto junto al texto (nunca solo color).

## TONO DE MARCA
- **Personalidad / voz:** ⚠️ NO_DEFINIDO. Regla de contenido: mensajes de autenticación fallida genéricos; mensajes de error sin datos técnicos ni clínicos.

## ESTILO VISUAL GENERAL
⚠️ NO_DEFINIDO (estilo, radios, sombras, densidad).

## REGLAS DE USO
| Regla | Descripción | Fuente |
|---|---|---|
| Color + icono + texto | Nivel de urgencia nunca solo por color | PROV-BR-001 |
| Contraste | >= 4.5:1 | PROV-UX-003 |
| Pantallas públicas | Sin PII | PROV-UX-002 |
