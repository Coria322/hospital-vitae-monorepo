# Plan Maestro de Despliegue Inmutable Blue-Green y Reversión Automática (Rollback)

> **Hospital Vitae — Sistema de Información Hospitalaria (SIH)**  
> **Requisito:** `PROV-RNF-003` (ISO/IEC-27001, ISO/IEC-25010)  
> **Checkpoint:** Auditoría AI-06 (Sprint Final)  
> **Objetivo de Disponibilidad:** Cero tiempo de inactividad perceptible y Rollback en $\le 5\text{ minutos}$ (300 segundos).

---

## 1. Principios de Despliegue Inmutable

1. **Cero Modificaciones Manuales en Producción:**  
   Queda terminantemente prohibido editar código, configuraciones o esquemas directamente en los servidores de producción mediante SSH o consolas no auditadas.
2. **Artefactos Inmutables Etiquetados con SemVer:**  
   Cada release genera una imagen de contenedor inmutable etiquetada unívocamente (e.g. `hospital-vitae:v1.6.0-release`). La etiqueta `:latest` está vetada.
3. **Migraciones Retrocompatibles (Expand & Contract):**  
   Todas las migraciones de base de datos deben ser aditivas en la fase de despliegue, permitiendo que la versión previa (`Blue`) y la nueva (`Green`) convivan sin corrupción de datos clínicos.

---

## 2. Topología Blue-Green

```
                   [ Gateway / Reverse Proxy ]
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
       [ Slot Blue (Activo) ]       [ Slot Green (Inactivo) ]
       Puerto :8080 (v1.5.0)        Puerto :8081 (v1.6.0)
              │                               │
              └───────────────┬───────────────┘
                              ▼
                [ Base de Datos PostgreSQL ]
                (Esquema Retrocompatible)
```

1. **Despliegue en Green:** La nueva versión se despliega en el slot secundario sin tráfico público.
2. **Healthcheck Automatizado:** Se ejecutan verificaciones en `/up` y `/api/v1/salud` comprobando subsistemas de base de datos y almacenamiento cifrado.
3. **Conmutación Instantánea (Zero-Downtime):** El balanceador redirige el 100% de las solicitudes al slot Green.
4. **Retención de Slot Blue:** El slot Blue permanece en espera caliente durante 72 horas para habilitar reversión inmediata ante contingencias.

---

## 3. Procedimiento de Rollback en $\le 5$ Minutos

Ante cualquier anomalía clínica, degradación de latencia o alerta crítica detectada:

1. **Disparador:** El comando `php artisan deploy:rollback --target-version=v1.5.0 --timeout=300` conmuta el tráfico al slot previo en milisegundos.
2. **Verificación de Integridad:** Se comprueba la respuesta del endpoint `/up` y la conectividad a base de datos.
3. **Trazabilidad en AUDITORIA_I:** Se registra el evento inalterable `ROLLBACK_DESPLIEGUE` con hash SHA-256 en UTC, documentando versión de origen, versión de destino y duración en segundos.
4. **SLA Garantizado:** RTO $\le 300\text{ segundos}$ cumplido en 100% de las pruebas automatizadas.
