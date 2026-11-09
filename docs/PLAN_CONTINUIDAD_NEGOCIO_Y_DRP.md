# Plan de Continuidad del Negocio (BCP) y Recuperación ante Desastres (DRP)

> **Hospital Vitae — Sistema de Información Hospitalaria (SIH)**  
> **Requisito:** `PROV-RNF-004` (ISO/IEC-27001, NOM-024-SSA3-2012)  
> **Checkpoint:** Auditoría AI-06 (Sprint Final)  
> **Métricas Clave:** RTO $\le 4\text{ horas}$ | RPO $\le 15\text{ minutos}$

---

## 1. Alcance y Objetivos de Recuperación

El presente plan establece los protocolos para garantizar la continuidad ininterrumpida de los servicios críticos del hospital (Urgencias, Triage Manchester, Expediente Clínico Electrónico NOM-004 y Farmacia de Medicamentos Controlados):

| Métrica | Parámetro Normativo | Implementación en Hospital Vitae |
| :--- | :--- | :--- |
| **RTO (Recovery Time Objective)** | $\le 4\text{ horas}$ | **$\le 45\text{ minutos}$** mediante réplica Standby y orquestación automatizada. |
| **RPO (Recovery Point Objective)** | $\le 15\text{ minutos}$ | **$\le 5\text{ minutos}$** mediante streaming de logs WAL en PostgreSQL. |
| **Monitoreo de Capacidad** | Alerta al superar el $85\%$ | Comando `ops:capacity-monitor` con alertas de CPU, RAM, disco y pool BD. |

---

## 2. Estrategia de Respaldo 3-2-1 y Conmutación (Failover)

1. **3 Copias de Información Clínica:**
   - Copia principal activa en nodo primario (Supabase PostgreSQL con TLS `sslmode=require`).
   - Copia en réplica secundaria Standby sincronizada en tiempo real mediante WAL streaming.
   - Respaldo cifrado diario offsite con retención 30 días y 12 meses (`php artisan db:backup-seguro`).
2. **2 Medios Diferentes:**
   - Almacenamiento en bloque transaccional SSD NVMe.
   - Almacenamiento de objetos cifrado AES-256 en nube secundaria redundante.
3. **1 Copia Fuera de Sitio (Offsite):**
   - Repositorio geográficamente aislado para contingencias de desastre natural o pérdida de centro de datos.

---

## 3. Protocolo de Conmutación por Falla (Failover)

```
[ Incidente Crítico Primario ] 
             │
             ▼
[ Detección: Healthcheck / Capacidad ] ──(Fallo > 3 min)──┐
             │                                            │
             ▼                                            ▼
[ Promoción de Nodo Secundario Standby ] ◄────────────────┘
             │
             ▼  (< 15 minutos)
[ Actualización de CNAME / DNS Gateway ]
             │
             ▼
[ Sistema Operativo en Contingencia (RTO <= 4h) ]
```

1. **Monitoreo Continuo:** `php artisan ops:capacity-monitor` y healthcheck `/salud`.
2. **Declaración de Contingencia:** El Comité de Operaciones y Seguridad autoriza el failover.
3. **Promoción del Standby:** La réplica secundaria asume como primario en modo lectura/escritura.
4. **Registro Forense:** El evento `FAILOVER_DRP` se consolida en la bitácora inalterable `AUDITORIA_I`.
