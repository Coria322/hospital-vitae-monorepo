# Módulo de Administrador de Sistemas y Seguridad Hospitalaria

**Hospital Vitae - Sistema de Información Hospitalaria**  
**Ubicación:** `src/views/admin/`  
**Rutas en frontend:** `/administrador` (redirección desde `/admin`)

---

## 1. Propósito y Separación de Privilegios

Este módulo está destinado a los ingenieros de soporte técnico, infraestructura y seguridad informática (**SysAdmin / TI**).

Bajo las normas internacionales y mexicanas:
- **NOM-024-SSA3-2012:** Sistemas de Información de Registro Electrónico para la Salud (Trazabilidad estricta de accesos técnicos, sellado append-only y disociación de expedientes).
- **ISO/IEC 27001:2022:** Sistemas de Gestión de la Seguridad de la Información (Control de acceso RBAC, gestión de incidentes y monitorización de infraestructura).

> **Principio de Menor Privilegio (Cero Acceso Clínico):**  
> El personal de administración de TI tiene acceso irrestricto a la gestión de cuentas, configuración de contenedores, certificados SSL y trazas de telemetría, pero tiene **estrictamente bloqueada por arquitectura la visualización de notas médicas, diagnósticos y datos confidenciales del paciente**. Toda consulta técnica trabaja sobre identificadores clínicos disociados mediante salting hospitalario (`ECE-DEMO-0248`).

---

## 2. Iconografía y Estándar Visual

- **Cero emojis:** Todos los iconos son vectoriales limpios (**inline SVG** `viewBox="0 0 24 24"` con trazos `stroke-width="2"`) basados en **Lucide Icons** y **Heroicons**, en total coherencia con los módulos de `doctor`, `nurse`, `reception` y `lab`.
- **Paleta de Colores Institucional:**
  - **Slate Oscuro / Root TI (`#0f172a` / `#1e293b`):** Botones principales de administración, consola de Docker y avatar root.
  - **Azul Primario Vitae (`#0284c7` / `#0369a1`):** Acciones del sistema y elementos interactivos.
  - **Rojo de Alerta / Bloqueo (`#b91c1c` / `#fee2e2`):** Cuentas con intentos fallidos consecutivos, accesos 403 y cuarentena de seguridad.
  - **Verde Saludable (`#16a34a` / `#f0fdf4`):** Servicios Docker en línea, 100% integridad Block-Tree y sincronización NTP del CENAM.

---

## 3. Estructura de Componentes

```text
src/views/admin/
├── AdminWorkspaceView.vue          # Orquestador del estado de usuarios, bloqueos, trazas y atajos de teclado
├── types.ts                        # Definiciones TypeScript de identidades, bloqueos, auditoría y Docker
├── README.md                       # Documentación técnica y especificaciones de seguridad
└── components/
    ├── AdminHeader.vue             # Cabecera institucional + reloj en vivo + menú flotante cerrar sesión
    ├── AdminSidebar.vue            # Barra lateral con atajos (Alt+U, Alt+B, Alt+A, Alt+S) y balance TI
    ├── AdminUsuariosSection.vue    # Pantalla 1: Directorio RBAC, alta de identidad digital y matriz de permisos
    ├── AdminBloqueosSection.vue    # Pantalla 2: Centro de reactivación de cuentas, protocolo OTP y auditoría
    ├── AdminBitacoraSection.vue    # Pantalla 3: Trazabilidad forense inmutable NOM-024, hashes y comprobante
    └── AdminDockerSection.vue      # Pantalla 4: Telemetría de cluster, matriz de 6 contenedores y consola de logs
```

---

## 4. Flujo Operativo y Atajos de Teclado

| Atajo | Pantalla | Descripción |
| :--- | :--- | :--- |
| `Alt+U` | **Gestión de Usuarios** | Directorio general de colaboradores con búsqueda reactiva, alta de nuevos colaboradores con matriz RBAC dinámica y sincronización con Directorio Activo (LDAP). |
| `Alt+B` | **Cuentas Bloqueadas** | Listado de contención para cuentas suspendidas tras 3 intentos fallidos consecutivos. Permite desbloquear mediante protocolo formal con motivo institucional y verificación presencial / OTP. |
| `Alt+A` | **Bitácora NOM-024** | Auditoría forense en tiempo real (142,890+ registros) con verificación criptográfica de cadenas de bloques SHA-256 e inspección de JWT. |
| `Alt+S` | **Estado del Sistema** | Métricas de uso de CPU (Intel Xeon), RAM DDR4, almacenamiento NVMe, matriz de 6 contenedores Docker (`vite-hospital-ui`, `postgres-ece-db`, `redis-cache`, `hl7-fhir-gateway`, `audit-logger`, `docker-auth-mfa`) y consola de logs en vivo. |

---

## 5. Integración con el Backend (Laravel API)

### Endpoints sugeridos en `vitae-backend`:
1. `GET /api/admin/usuarios`: Listado paginado de usuarios con rol hospitalario y estado.
2. `POST /api/admin/usuarios`: Alta de nuevo colaborador y despacho de credenciales provisionales.
3. `POST /api/admin/cuentas/{id}/desbloquear`:
   ```json
   {
     "motivo": "Olvido de clave en cambio de turno, validado presencialmente.",
     "medida_seguridad": "CAMBIO_CLAVE",
     "admin_id": "ADM-0014"
   }
   ```
4. `GET /api/admin/bitacora`: Listado inmutable de trazas de auditoría (RFC-5424 / Syslog).
5. `GET /api/admin/telemetria`: Métricas de hardware y estado de contenedores Docker.
