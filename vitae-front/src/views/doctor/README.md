# 🩺 Módulo Médico (Doctor Workspace) · Hospital Vitae

## 1. Descripción General
El módulo **Doctor Workspace** (`src/views/doctor/`) es el entorno clínico digital diseñado para el médico de urgencias y consulta externa. Cumple rigurosamente con las normas oficiales mexicanas **NOM-004-SSA3-2012** (Expediente Clínico Electrónico) y **NOM-024-SSA3-2012** (Sistemas de Información de Registro Electrónico para la Salud).

Permite gestionar la consulta en curso, revisar notas previas y antecedentes, emitir notas de evolución ECE, prescribir recetas médicas con validación de medicamentos controlados mediante e.firma/FIEL, solicitar estudios paraclínicos (laboratorio e imagenología), y reclasificar el nivel de Triage Manchester del paciente.

---

## 2. Arquitectura de Componentes
El módulo está completamente desacoplado y modularizado dentro de `src/views/doctor/`:

```
src/views/doctor/
├── DoctorWorkspaceView.vue            # Orquestador principal y estado global del paciente
├── types.ts                          # Interfaces TypeScript (Doctor, Paciente, Receta, etc.)
├── README.md                         # Esta documentación técnica
└── components/
    ├── DoctorHeader.vue              # Encabezado clínico persistente, reloj y menú de sesión
    ├── DoctorSlaBanner.vue           # Barra de contexto del paciente activo, SLA y alerta de alergias
    ├── DoctorSidebar.vue             # Menú lateral rápido, atajos de teclado y cola de espera
    ├── ClinicalRecordSection.vue     # Vista integral del expediente clínico histórico
    ├── PrescriptionPanel.vue         # Panel lateral desacoplado de prescripción rápida (Alt+R)
    ├── NuevaNotaEceSection.vue       # Formulario estructurado SOAP (NOM-004) y CIE-10
    ├── EmitirRecetaSection.vue       # Prescripción formal, posología y e.firma criptográfica
    ├── SolicitarLaboratorioSection.vue # Catálogo y solicitud de estudios de laboratorio/gabinete
    ├── ReclasificarTriageDoctorSection.vue # Revaloración médica de severidad Manchester
    ├── ColaEsperaDoctorSection.vue   # Tablero de pacientes asignados al Box
    └── DoctorPlaceholderSection.vue  # Vista base para secciones auxiliares en desarrollo
```

---

## 3. Estado Centralizado del Paciente (`DoctorWorkspaceView.vue`)
Para evitar recapturar información al cambiar entre pestañas (ej. de *Expediente* a *Nueva Nota* o *Receta*), el paciente activo se mantiene reactivamente en `DoctorWorkspaceView.vue`:

```typescript
const pacienteSeleccionado = ref<Paciente>({
  id: 1,
  nombre: 'Vargas Soto, Manuel',
  edad: 58,
  genero: 'M',
  curp: 'VASM660814HDFRNT08',
  folio: 'EXP-2026-08492',
  alergias: 'Penicilina / Sulfamidas',
  diagnosticoActual: 'I20.0 Angina Inestable',
  cie10: 'I20.0',
  triage: 'Nivel II · Muy Urgente',
  triageColor: '#EA580C',
  signosVitales: {
    pa: '130/88',
    fc: 88,
    fr: 18,
    temp: 36.8,
    spo2: 97
  }
})
```

Al seleccionar cualquier paciente desde la barra lateral (`DoctorSidebar.vue`) o la cola de espera (`ColaEsperaDoctorSection.vue`), todos los componentes hijos heredan automáticamente los datos, antecedentes y constantes vitales.

---

## 4. Integración con el Backend (Laravel API v1)

### 4.1 Autenticación e Identidad
El encabezado (`DoctorHeader.vue`) recupera el médico en sesión a través de `authService.getUsuario()`:
* **Headers requeridos:** `Authorization: Bearer <token_sanctum>`
* **Cierre de sesión:** Botón `⋮` en la esquina superior derecha $\rightarrow$ llama a `POST /api/v1/auth/logout` y borra `localStorage`.

### 4.2 Endpoints a Conectar

| Acción Clínica | Método | Endpoint Backend | Payload Esperado |
| :--- | :--- | :--- | :--- |
| **Listar pacientes asignados** | `GET` | `/api/v1/doctor/pacientes` | Filtro por Box o Médico |
| **Obtener expediente clínico** | `GET` | `/api/v1/pacientes/{id}/expediente` | Retorna historial, notas previas y alergias |
| **Guardar Nota Médica (SOAP)** | `POST` | `/api/v1/doctor/notas-evolucion` | `{ paciente_id, subjetivo, objetivo, analisis, plan, cie10_codigo }` |
| **Emitir Receta Médica** | `POST` | `/api/v1/doctor/recetas` | `{ paciente_id, medicamentos: [...], efirma_hash, requiere_controlado }` |
| **Solicitar Laboratorio / Imagen** | `POST` | `/api/v1/doctor/ordenes-estudios` | `{ paciente_id, estudios: [...], prioridad, justificacion }` |
| **Reclasificar Triage** | `POST` | `/api/v1/doctor/reclasificar-triage` | `{ paciente_id, nuevo_nivel, motivo_clinico, signos_actuales }` |

---

## 5. Atajos de Teclado del Módulo Médico
Para agilizar el flujo de trabajo en urgencias sin depender exclusivamente del ratón:
* <kbd>Alt</kbd> + <kbd>E</kbd> : Abrir **Expediente Clínico**
* <kbd>Alt</kbd> + <kbd>N</kbd> : Redactar **Nueva Nota Médica ECE (SOAP)**
* <kbd>Alt</kbd> + <kbd>R</kbd> : Desplegar **Prescripción / Receta Rápida**
* <kbd>Alt</kbd> + <kbd>L</kbd> : Abrir **Solicitud de Laboratorio y Gabinete**
* <kbd>Alt</kbd> + <kbd>T</kbd> : **Reclasificar Triage Manchester**
* <kbd>Alt</kbd> + <kbd>C</kbd> : Consultar **Cola de Espera de Urgencias**
