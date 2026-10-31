# Módulo de Laboratorista y Auxiliares Diagnósticos (LIS / RIS)

**Hospital Vitae - Sistema de Información Hospitalaria**  
**Ubicación:** `src/views/lab/`  
**Ruta en frontend:** `/laboratorio` (redirección desde `/lab`)

---

## 1. Propósito del Módulo

Este módulo constituye la interfaz operativa para el personal de **Laboratorio Clínico, Gabinete e Imagenología Diagnóstica (LIS / RIS)**. Está diseñado bajo los requerimientos de las normas mexicanas:
- **NOM-024-SSA3-2012:** Sistemas de Información de Registro Electrónico para la Salud (aislamiento estricto y trazabilidad de consulta por token de usuario).
- **NOM-007-SSA3-2011:** Organización y funcionamiento de laboratorios clínicos (aseguramiento de la calidad analítica, controles Bio-Rad y calibraciones diarias).

A diferencia de los módulos de Consulta Externa o Hospitalización, este módulo opera con **acceso restringido a auxiliares diagnósticos**, lo que significa que el personal técnico visualiza únicamente los datos indispensables para procesar la muestra (iniciales del paciente, edad, sexo, servicio de procedencia y médico prescriptor), protegiendo la confidencialidad integral del expediente clínico.

---

## 2. Iconografía y Estándar Visual

- **Cero emojis:** Todos los elementos gráficos, estados y acciones se representan mediante **iconos vectoriales limpios (inline SVG)** basados en el catálogo de código abierto de **Lucide Icons** y **Heroicons**, manteniendo la misma identidad visual sobria y profesional que los módulos de `doctor`, `nurse` y `reception`.
- **Paleta de Colores Institucional:**
  - **Azul Primario LIS (`#0284c7` / `#0369a1`):** Indicadores de sistema, botones principales y estados de procesamiento normal.
  - **Rojo Código Crítico / STAT (`#b91c1c` / `#fee2e2`):** Solicitudes de choque, muestras con menos de 10 min de SLA y banners de alarma diagnóstica.
  - **Naranja Urgencia (`#ea580c` / `#ffedd5`):** Solicitudes urgentes de observación o quirófano.
  - **Verde Validado / Calibrado (`#16a34a` / `#f0fdf4`):** Estudios conciliados, controles de calidad en rango y conexiones telemáticas activas.

---

## 3. Estructura de Componentes

```text
src/views/lab/
├── LabWorkspaceView.vue            # Orquestador del estado de órdenes, estudio activo y atajos de teclado
├── types.ts                        # Definiciones TypeScript de entidades LIS (Estudios, Analitos, Custodia, PKI)
├── README.md                       # Documentación técnica del módulo
└── components/
    ├── LabHeader.vue               # Cabecera institucional, reloj en vivo y menú desplegable para cierre de sesión
    ├── LabSidebar.vue              # Barra lateral con atajos de teclado (Alt+P, Alt+C, Alt+B, Alt+H) y balance operativo
    ├── LabSlaBanner.vue            # Banner unificado de cuenta regresiva de urgencias críticas STAT
    ├── LabPendientesSection.vue    # Pantalla 1: Bandeja de entrada 100% de ancho con priorización STAT
    ├── LabCargarResultadosSection.vue # Pantalla 2: Asociación técnica, dropzone con importador Cobas b221 y firma e.firma
    ├── LabBusquedaFolioSection.vue # Pantalla 3: Cadena de custodia en 4 fases, analizador e intercom telefónico a Urgencias
    └── LabHistorialBitacoraSection.vue # Pantalla 4: 4 tarjetas de KPIs, bitácora de informes y comprobante telemático HL7/FHIR
```

---

## 4. Flujo Operativo y Atajos de Teclado

| Atajo | Pantalla | Descripción |
| :--- | :--- | :--- |
| `Alt+P` | **Bandeja de Pendientes** | Visualiza el flujo de trabajo de estudios pendientes por procesar. Al hacer clic en **"Subir Resultado STAT"** o **"Cargar Resultados"**, el estudio seleccionado se transfiere automáticamente a la pantalla de carga. |
| `Alt+C` | **Carga y Validación** | Asocia la orden a un analizador (ej. *Roche Cobas b221*), permite arrastrar informes en PDF o DICOM, verificar el control de calidad diario y aplicar la firma electrónica PKI. |
| `Alt+B` | **Búsqueda por Folio** | Búsqueda por folio oficial (`EST-2026-9041`), código de tubo (`BC-HEP-88190`) o iniciales. Muestra la cadena de custodia en 4 etapas y las extensiones de intercom directo a Choque (`Ext. 2101`). |
| `Alt+H` | **Historial y Bitácora** | Monitorea los 4 KPIs del turno, permite auditar los hashes SHA-256 de los informes emitidos y verificar el acuse telemático HL7 `ACK_AA 200 OK`. |

---

## 5. Integración con el Backend (Laravel API & LIS)

### Endpoints sugeridos en `vitae-backend`:
1. `GET /api/laboratorio/pendientes`: Retorna la lista de órdenes diagnósticas pendientes con su SLA restante.
2. `GET /api/laboratorio/estudio/{folio}`: Detalle completo de determinaciones analíticas, valores de referencia y cadena de custodia.
3. `POST /api/laboratorio/cargar-resultado`:
   ```json
   {
     "folio": "EST-2026-9041",
     "analizador": "Cobas b221",
     "token_firma": "SHA256_PKI_HASH",
     "archivo": "BASE64_O_MULTIPART_PDF",
     "observaciones": "Valores verificados por duplicado."
   }
   ```
4. `POST /api/laboratorio/rechazar-muestra`: Registra el motivo de rechazo (hemólisis, volumen insuficiente) y alerta a Urgencias.
5. `GET /api/laboratorio/historial`: Bitácora auditable de resultados del turno con sello de tiempo NTP.
