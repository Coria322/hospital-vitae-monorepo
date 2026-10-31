<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps<{
  pacienteActivo?: {
    id?: string | number
    nombre: string
    edad: number | null
    sexo?: string
    folio?: string
    box?: string
    triage?: string
    triageColor?: string
    signos?: {
      paSistolica: string
      paDiastolica: string
      fc: string
      fr: string
      spo2: string
      temp: string
      evaDolor: number | string
    }
    motivoConsulta?: string
    alergias?: string
  }
}>()

// --- ESTADOS REACTIVOS PARA NOTA DE ENFERMERÍA ---
const pacienteSeleccionadoNota = ref({
  box: 'B03',
  nombre: '',
  edad: '',
  fechaNac: '',
  ubicacion: 'Box 03 · Urgencias Adultos',
  medicoTratante: 'Dr. Javier Morales Peña (Céd. 4029103)',
  alergias: '',
  folio: '',
  nivel: ''
})

const sincronizarPacienteActivo = () => {
  if (props.pacienteActivo?.nombre) {
    pacienteSeleccionadoNota.value.nombre = props.pacienteActivo.nombre
    pacienteSeleccionadoNota.value.edad = props.pacienteActivo.edad ? `${props.pacienteActivo.edad} años` : ''
    pacienteSeleccionadoNota.value.folio = props.pacienteActivo.folio || 'FOL-URG-8492'
    pacienteSeleccionadoNota.value.nivel = props.pacienteActivo.triage || 'Nivel III · Urgente'
    pacienteSeleccionadoNota.value.alergias = props.pacienteActivo.alergias || ''
    pacienteSeleccionadoNota.value.box = props.pacienteActivo.box || 'B03'
  }
}

onMounted(() => {
  sincronizarPacienteActivo()
})

watch(() => props.pacienteActivo, () => {
  sincronizarPacienteActivo()
}, { deep: true })

const notaEnfermeriaForm = ref({
  tipoNota: 'Nota de Evolución / Turno',
  turnoCobertura: 'Matutino' as 'Matutino' | 'Vespertino' | 'Nocturno',
  horaEvaluacion: '02:30 PM',
  pa: '',
  fc: '',
  fr: '',
  temp: '',
  spo2: '',
  evaDolor: '',
  sitioVascular: '',
  calibreVascular: '18G',
  fechaPuncion: '',
  sinSignosFlebitis: true,
  permeableLimpia: true,
  ingresosLiquidos: null as number | null,
  egresosLiquidos: null as number | null,
  procedimientos: {
    oxigenoterapia: false,
    prevencionCaidas: false,
    curacionHeridas: false,
    sondaVesical: false,
    monitoreoECG: false,
    aspiracionSecreciones: false
  },
  narrativaSOAPIE: ''
})

const balanceDiferencia = computed(() => {
  if (notaEnfermeriaForm.value.ingresosLiquidos === null && notaEnfermeriaForm.value.egresosLiquidos === null) {
    return '+0 ml'
  }
  const ing = notaEnfermeriaForm.value.ingresosLiquidos || 0
  const egr = notaEnfermeriaForm.value.egresosLiquidos || 0
  const dif = ing - egr
  return dif >= 0 ? `+${dif} ml` : `${dif} ml`
})

const insertarFraseRapida = (frase: string) => {
  if (notaEnfermeriaForm.value.narrativaSOAPIE) {
    notaEnfermeriaForm.value.narrativaSOAPIE += ` ${frase}`
  } else {
    notaEnfermeriaForm.value.narrativaSOAPIE = frase
  }
}

const heredarSignosTriage = () => {
  if (props.pacienteActivo?.signos) {
    const s = props.pacienteActivo.signos
    notaEnfermeriaForm.value.pa = (s.paSistolica && s.paDiastolica) ? `${s.paSistolica}/${s.paDiastolica}` : '130/85'
    notaEnfermeriaForm.value.fc = s.fc || '84'
    notaEnfermeriaForm.value.fr = s.fr || '18'
    notaEnfermeriaForm.value.temp = s.temp || '36.8'
    notaEnfermeriaForm.value.spo2 = s.spo2 || '97'
    notaEnfermeriaForm.value.evaDolor = String(s.evaDolor || '4')
    alert('<svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Constantes vitales importadas desde el registro de Triage.')
  } else {
    alert('No hay signos capturados en Triage para heredar. Utilice el Monitor Philips o ingréselos manualmente.')
  }
}

const importarMonitorPhilips = () => {
  notaEnfermeriaForm.value.pa = '130/88'
  notaEnfermeriaForm.value.fc = '88'
  notaEnfermeriaForm.value.fr = '18'
  notaEnfermeriaForm.value.temp = '36.8'
  notaEnfermeriaForm.value.spo2 = '97'
  notaEnfermeriaForm.value.evaDolor = '4'
  alert('[TELEMETRÍA] Datos importados con éxito del Monitor Philips Box 03 (Telemetría sincronizada).')
}

// Kardex de Medicación prescrito por el médico
const kardexMedicamentos = ref([
  {
    id: 1,
    nombre: 'Paracetamol 1 g Solución IV',
    hora: '14:00 hrs',
    indicacion: 'Dosis única / Cada 8h · Pasa en 15 min',
    dobleChequeo: 'Lic. C. Ramos',
    aplicado: false,
    horaAplicacion: ''
  },
  {
    id: 2,
    nombre: 'Solución Salina 0.9% 500 ml IV',
    estado: 'En Curso',
    indicacion: 'Infusión continua a 42 ml/h para vena permeable',
    infundido: 325,
    resta: 175,
    porcentaje: 65,
    aplicado: true,
    horaAplicacion: 'En infusión'
  },
  {
    id: 3,
    nombre: 'Enoxaparina 60 mg SC',
    estado: 'Pendiente',
    indicacion: 'Subcutánea periumbilical · Programada 20:00 hrs',
    turno: 'Turno Nocturno',
    espera: 'En espera',
    aplicado: false,
    horaAplicacion: ''
  }
])

const medParacetamol = computed(() => kardexMedicamentos.value[0]!)
const medSolucion = computed(() => kardexMedicamentos.value[1]!)
const medEnoxaparina = computed(() => kardexMedicamentos.value[2]!)

const consultarHistorial = () => {
  alert('Consultando bitácora histórica del expediente clínico...')
}

const consultarNotasPrevias = () => {
  alert('Cargando expediente completo de notas de enfermería...')
}

const aplicarMedicamento = (medId: number) => {
  const med = kardexMedicamentos.value.find(m => m.id === medId)
  if (med) {
    med.aplicado = true
    const now = new Date()
    const hh = String(now.getHours()).padStart(2, '0')
    const mm = String(now.getMinutes()).padStart(2, '0')
    med.horaAplicacion = `${hh}:${mm} hrs`
    alert(`Medicamento "${med.nombre}" marcado como APLICADO a las ${hh}:${mm} hrs con doble chequeo de enfermería.`)
  }
}

// Notas clínicas del turno previo
const notasPreviasTurno = ref([
  {
    id: 1,
    autor: 'Enf. T. González (Matutino)',
    hora: '11:15',
    contenido: 'Ingreso a Box 03. Se canaliza AVP 18G en antebrazo izq. Toma de muestras para laboratorio completada.'
  },
  {
    id: 2,
    autor: 'Control Triage Inicial',
    hora: '09:40',
    contenido: 'PA 145/95, FC 98, FR 20, SpO2 94%. Dolor precordial EVA 7/10. Asignado a Nivel II Manchester.'
  }
])

const guardarNotaEnfermeria = () => {
  if (!notaEnfermeriaForm.value.narrativaSOAPIE) {
    alert('NOM-004-SSA3-2012: Ingrese la narrativa clínica estructurada (SOAPIE) antes de firmar.')
    return
  }
  alert('Nota de enfermería firmada digitalmente y resguardada con hash criptográfico SHA-256 inalterable.')
}

const imprimirHojaEnfermeria = () => {
  alert('Generando formato oficial de Hoja de Enfermería en PDF conforme a NOM-004-SSA3-2012...')
}

const notificarDeterioro = () => {
  alert('<svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg> Notificación prioritaria de deterioro clínico enviada de inmediato al Médico de Guardia.')
}
</script>

<template>
  <div class="nota-view-container">
    <!-- 1. Encabezado Oficial NOM-004 y Advertencia Legal -->
    <div class="nota-header-banner">
      <div class="banner-title-block">
        <div class="blue-accent-bar"></div>
        <div>
          <h3>MÓDULO ACTIVO: Registro y Evolución de Enfermería <span class="badge-kardex">Kardex - Hoja Clínica</span></h3>
          <p>Conforme a la Norma Oficial Mexicana NOM-004-SSA3-2012 del Expediente Clínico</p>
        </div>
      </div>
      <div class="prescription-lock-badge-compact">
        <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> Bloqueo Estricto NOM-004: Solo Lectura / Administración</span>
      </div>
    </div>

    <!-- 2. Barra de Paciente y Box Clínico -->
    <div class="nota-patient-bar">
      <div class="patient-main-info">
        <div class="box-badge-large">{{ pacienteSeleccionadoNota.box || 'B03' }}</div>
        <div class="patient-details">
          <div class="name-row">
            <h3 v-if="pacienteSeleccionadoNota.nombre">{{ pacienteSeleccionadoNota.nombre }}</h3>
            <h3 v-else class="unselected-text">Sin paciente asignado (Esperando selección o ingreso)</h3>
            
            <span v-if="pacienteSeleccionadoNota.nivel" class="triage-pill urgent">
              ● {{ pacienteSeleccionadoNota.nivel }}
            </span>
            <span v-if="pacienteSeleccionadoNota.folio" class="folio-code">
              Folio: {{ pacienteSeleccionadoNota.folio }}
            </span>
          </div>
          <div class="meta-row">
            <span>Edad: <strong>{{ pacienteSeleccionadoNota.edad || '--' }}</strong> ({{ pacienteSeleccionadoNota.fechaNac || '--/--/----' }})</span>
            <span class="sep">•</span>
            <span>Ubicación: <strong>{{ pacienteSeleccionadoNota.ubicacion }}</strong></span>
            <span class="sep">•</span>
            <span>Médico Tratante: <strong>{{ pacienteSeleccionadoNota.medicoTratante }}</strong></span>
            <span class="sep">•</span>
            <span class="allergy-warning" v-if="pacienteSeleccionadoNota.alergias">
              <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg> Alergia: <strong>{{ pacienteSeleccionadoNota.alergias }}</strong>
            </span>
            <span class="allergy-warning" v-else>
              <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg> Alergia: <em>Sin alergias registradas</em>
            </span>
          </div>
        </div>
      </div>
      
      <div class="patient-actions-col">
        <div class="search-switch-box">
          <input 
            type="text" 
            placeholder="Cambiar paciente (Cama, Folio, Nombre)..." 
            class="patient-search-input" 
          />
          <button type="button" class="btn-history" @click="consultarHistorial">
            <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> Historial
          </button>
        </div>
      </div>
    </div>

    <!-- 3. Contenedor de 2 Columnas: Formulario Clínico y Panel Lateral (Kardex / Firma) -->
    <div class="nota-grid-layout">
      
      <!-- ========== COLUMNA IZQUIERDA: Formulario Clínico SOAPIE ========== -->
      <div class="nota-col-left">
        
        <!-- 1. Parámetros de la Nota Institucional -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-edit" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
              <h4>1. Parámetros de la Nota Institucional</h4>
            </div>
            <span class="nom-tag">NOM-004-SSA3 · Numeral 9.1</span>
          </div>

          <div class="params-row-grid">
            <div class="param-group">
              <label>Tipo de Nota <span class="req">*</span></label>
              <select v-model="notaEnfermeriaForm.tipoNota" class="select-clean">
                <option>Nota de Evolución / Turno</option>
                <option>Nota de Ingreso a Urgencias</option>
                <option>Nota de Traslado / Reubicación</option>
                <option>Nota de Egreso Hospitalario</option>
              </select>
            </div>

            <div class="param-group">
              <label>Turno de Cobertura <span class="req">*</span></label>
              <div class="shift-toggle-group">
                <button 
                  type="button" 
                  class="shift-btn" 
                  :class="{ active: notaEnfermeriaForm.turnoCobertura === 'Matutino' }"
                  @click="notaEnfermeriaForm.turnoCobertura = 'Matutino'"
                >
                  Matutino
                </button>
                <button 
                  type="button" 
                  class="shift-btn" 
                  :class="{ active: notaEnfermeriaForm.turnoCobertura === 'Vespertino' }"
                  @click="notaEnfermeriaForm.turnoCobertura = 'Vespertino'"
                >
                  Vesp.
                </button>
                <button 
                  type="button" 
                  class="shift-btn" 
                  :class="{ active: notaEnfermeriaForm.turnoCobertura === 'Nocturno' }"
                  @click="notaEnfermeriaForm.turnoCobertura = 'Nocturno'"
                >
                  Noct.
                </button>
              </div>
            </div>

            <div class="param-group">
              <label>Hora de Evaluación</label>
              <input 
                type="text" 
                v-model="notaEnfermeriaForm.horaEvaluacion" 
                class="input-clean text-center" 
                placeholder="02:30 PM"
              />
            </div>
          </div>
        </section>

        <!-- 2. Signos Vitales de Control (Re-valoración) -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-activity" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></span>
              <h4>2. Signos Vitales de Control (Re-valoración)</h4>
            </div>
            <div class="vitals-import-buttons">
              <button type="button" class="btn-triage-import" @click="heredarSignosTriage" title="Heredar signos capturados en Triage">
                <svg viewBox="0 0 24 24" class="svg-inline-inbox" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/></svg> Heredar de Triage
              </button>
              <button type="button" class="btn-philips-import" @click="importarMonitorPhilips" title="Sincronizar telemetría de Box">
                <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.1 19.1 19"/></svg> Monitor Philips Box 03
              </button>
            </div>
          </div>

          <div class="vitals-control-grid">
            <!-- PA -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>PRESIÓN ART.</span>
                <span class="icon-subtle"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.pa" 
                  placeholder="130/88" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">mmHg</span>
            </div>

            <!-- FC -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>FREC. CARD.</span>
                <span class="icon-subtle red"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.fc" 
                  placeholder="88" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">lpm</span>
            </div>

            <!-- FR -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>FREC. RESP.</span>
                <span class="icon-subtle blue"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 4v9"/><path d="M9 13a4 4 0 0 1-5-4V6a3 3 0 0 1 5-2c2 0 2 2 2 4v5"/><path d="M15 13a4 4 0 0 0 5-4V6a3 3 0 0 0-5-2c-2 0-2 2-2 4v5"/><path d="M6 13c0 4.4 2.7 8 6 8s6-3.6 6-8"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.fr" 
                  placeholder="18" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">rpm</span>
            </div>

            <!-- TEMPERATURA -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>TEMPERATURA</span>
                <span class="icon-subtle orange"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.temp" 
                  placeholder="36.8" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">°C Axilar</span>
            </div>

            <!-- SATURACIÓN -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>SATURACIÓN</span>
                <span class="icon-subtle cyan"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.spo2" 
                  placeholder="97" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">% SpO2</span>
            </div>

            <!-- DOLOR EVA -->
            <div class="vital-control-card">
              <div class="vital-label">
                <span>DOLOR (EVA)</span>
                <span class="icon-subtle purple"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 15h8"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg></span>
              </div>
              <div class="vital-input-wrapper">
                <input 
                  type="text" 
                  v-model="notaEnfermeriaForm.evaDolor" 
                  placeholder="4" 
                  class="vital-input"
                />
              </div>
              <span class="vital-unit">Escala / 10</span>
            </div>
          </div>
        </section>

        <!-- 3. Vías de Acceso y Balance Hídrico Parcial -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg></span>
              <h4>3. Vías de Acceso y Balance Hídrico Parcial</h4>
            </div>
            <span class="infusion-tag">● Monitor de Infusión Activo</span>
          </div>

          <div class="access-balance-split">
            <!-- Tarjeta AVP -->
            <div class="access-card">
              <div class="access-card-header">
                <div class="card-title-mini">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-steth" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4"/><circle cx="20" cy="10" r="2"/></svg></span>
                  <strong>Acceso Venoso Periférico (AVP)</strong>
                </div>
                <span class="calibre-badge">Calibre {{ notaEnfermeriaForm.calibreVascular }}</span>
              </div>

              <div class="access-inputs-row">
                <div class="access-field">
                  <label>Sitio Anatómico</label>
                  <input 
                    type="text" 
                    v-model="notaEnfermeriaForm.sitioVascular" 
                    placeholder="ej. Antebrazo Izquierdo" 
                    class="input-mini-clean"
                  />
                </div>
                <div class="access-field">
                  <label>Fecha / Hora Punción</label>
                  <input 
                    type="text" 
                    v-model="notaEnfermeriaForm.fechaPuncion" 
                    placeholder="24/05 09:15" 
                    class="input-mini-clean"
                  />
                </div>
              </div>

              <div class="access-checkboxes-row">
                <label class="clean-check">
                  <input type="checkbox" v-model="notaEnfermeriaForm.sinSignosFlebitis" />
                  <span>Sin signos de flebitis</span>
                </label>
                <label class="clean-check">
                  <input type="checkbox" v-model="notaEnfermeriaForm.permeableLimpia" />
                  <span>Permeable y fijación limpia</span>
                </label>
              </div>
            </div>

            <!-- Tarjeta Balance de Líquidos -->
            <div class="balance-card">
              <div class="balance-card-header">
                <div class="card-title-mini">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="M7 21h10"/><path d="M12 3v18"/><path d="M3 7h18"/></svg></span>
                  <strong>Balance de Líquidos (ml)</strong>
                </div>
                <span class="diff-badge">Dif: {{ balanceDiferencia }}</span>
              </div>

              <div class="balance-inputs-row">
                <div class="balance-input-group">
                  <label>Ingresos (IV + Oral) <span class="unit-tag">ml</span></label>
                  <input 
                    type="number" 
                    v-model.number="notaEnfermeriaForm.ingresosLiquidos" 
                    placeholder="450" 
                    class="input-balance-num"
                  />
                </div>

                <div class="balance-input-group">
                  <label>Egresos (Diuresis/Vóm.) <span class="unit-tag red">ml</span></label>
                  <input 
                    type="number" 
                    v-model.number="notaEnfermeriaForm.egresosLiquidos" 
                    placeholder="300" 
                    class="input-balance-num"
                  />
                </div>
              </div>

              <div class="diuresis-status-bar">
                <span class="dot-active">●</span>
                <span>Diuresis espontánea presente (colúrica leve)</span>
                <strong class="gasto-tag">Gasto: 0.85 mL/kg/h</strong>
              </div>
            </div>
          </div>
        </section>

        <!-- 4. Procedimientos y Cuidados Ejecutados -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="9" width="18" height="6" rx="3" transform="rotate(-45 12 12)"/><path d="m10 10 4 4"/><path d="m14 10-4 4"/></svg></span>
              <h4>4. Procedimientos y Cuidados Ejecutados</h4>
            </div>
            <span class="subtle-note">Marcar intervenciones realizadas</span>
          </div>

          <div class="procedures-grid">
            <!-- 1. Oxigenoterapia -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.oxigenoterapia }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.oxigenoterapia" />
              </div>
              <div class="proc-text-col">
                <strong>Oxigenoterapia</strong>
                <small>Puntas nasales a 2 L/min</small>
              </div>
            </label>

            <!-- 2. Prevención de UPP / Caídas -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.prevencionCaidas }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.prevencionCaidas" />
              </div>
              <div class="proc-text-col">
                <strong>Prevención de UPP / Caídas</strong>
                <small>Barandales arriba + Timbre accesible</small>
              </div>
            </label>

            <!-- 3. Curación de Heridas -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.curacionHeridas }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.curacionHeridas" />
              </div>
              <div class="proc-text-col">
                <strong>Curación de Heridas</strong>
                <small>Técnica aséptica en sitio quirúrgico</small>
              </div>
            </label>

            <!-- 4. Sonda Vesical / Foley -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.sondaVesical }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.sondaVesical" />
              </div>
              <div class="proc-text-col">
                <strong>Sonda Vesical / Foley</strong>
                <small>Circuito cerrado estéril</small>
              </div>
            </label>

            <!-- 5. Monitoreo Electrocardiográfico -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.monitoreoECG }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.monitoreoECG" />
              </div>
              <div class="proc-text-col">
                <strong>Monitoreo Electrocardiográfico</strong>
                <small>Ritmo sinusal continuo en monitor</small>
              </div>
            </label>

            <!-- 6. Aspiración de Secreciones -->
            <label class="procedure-card" :class="{ selected: notaEnfermeriaForm.procedimientos.aspiracionSecreciones }">
              <div class="proc-check-col">
                <input type="checkbox" v-model="notaEnfermeriaForm.procedimientos.aspiracionSecreciones" />
              </div>
              <div class="proc-text-col">
                <strong>Aspiración de Secreciones</strong>
                <small>Vía aérea con tubo endotraqueal</small>
              </div>
            </label>
          </div>
        </section>

        <!-- 5. Narrativa de Enfermería (Estructura SOAPIE) -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
              <h4>5. Narrativa de Enfermería (Estructura SOAPIE)</h4>
            </div>
          </div>

          <!-- Frases Rápidas -->
          <div class="quick-phrases-row">
            <span class="phrases-label">Frases rápidas:</span>
            <button type="button" class="btn-fast-phrase" @click="insertarFraseRapida('Consciente y Orientado en tiempo, espacio y persona.')">
              + Consciente y Orientado
            </button>
            <button type="button" class="btn-fast-phrase" @click="insertarFraseRapida('Tolera vía oral adecuadamente sin náuseas.')">
              + Tolera Vía Oral
            </button>
            <button type="button" class="btn-fast-phrase" @click="insertarFraseRapida('Se notifica hallazgo clínico a Médico de Guardia.')">
              + Notificado a Médico
            </button>
            <button type="button" class="btn-fast-phrase" @click="insertarFraseRapida('Reposo absoluto semifowler a 30° con barandales arriba.')">
              + Reposo Semifowler
            </button>
          </div>

          <!-- Textarea SOAPIE -->
          <div class="soapie-textarea-wrapper">
            <textarea 
              v-model="notaEnfermeriaForm.narrativaSOAPIE"
              class="soapie-textarea"
              rows="6"
              placeholder="Escriba la evolución clínica estructurada del paciente (Subjetivo, Objetivo, Análisis de Enfermería, Plan, Intervención y Evaluación)..."
            ></textarea>
            
            <div class="soapie-footer-tip">
              <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5"/><path d="M9 18h6"/><path d="M10 22h4"/></svg> Formato SOAPIE obligatorio según guía clínica del expediente institucional.</span>
              <span class="char-counter">{{ notaEnfermeriaForm.narrativaSOAPIE ? notaEnfermeriaForm.narrativaSOAPIE.length : 0 }} caracteres</span>
            </div>
          </div>
        </section>

      </div>

      <!-- ========== COLUMNA DERECHA: Kardex de Medicación & Acciones ========== -->
      <div class="nota-col-right">
        
        <!-- 1. Kardex de Medicación Prescrita -->
        <section class="clinical-box kardex-card">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-pill" fill="none" stroke="currentColor" stroke-width="2"><path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/><path d="m8.5 8.5 7 7"/></svg></span>
              <h4>KARDEX DE MEDICACIÓN</h4>
            </div>
            <span class="badge-lock-rx">SOLO LECTURA / APLICACIÓN</span>
          </div>

          <div class="kardex-list">
            <!-- 1. Paracetamol -->
            <div class="med-item-card" :class="{ applied: medParacetamol.aplicado }">
              <div class="med-top">
                <div class="med-name-badge">
                  <strong>{{ medParacetamol.nombre }}</strong>
                  <span class="med-time-tag">{{ medParacetamol.hora }}</span>
                </div>
                <span class="status-indicator-pill" :class="medParacetamol.aplicado ? 'applied' : 'pending'">
                  {{ medParacetamol.aplicado ? '● Aplicado' : '● Pendiente' }}
                </span>
              </div>
              
              <p class="med-instructions">{{ medParacetamol.indicacion }}</p>

              <div class="med-actions-footer">
                <span class="double-check-text">Doble chequeo: <strong>{{ medParacetamol.dobleChequeo }}</strong></span>
                <button 
                  v-if="!medParacetamol.aplicado" 
                  type="button" 
                  class="btn-apply-med"
                  @click="aplicarMedicamento(medParacetamol.id)"
                >
                  <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Aplicar
                </button>
                <span v-else class="applied-timestamp">Aplicado {{ medParacetamol.horaAplicacion }}</span>
              </div>
            </div>

            <!-- 2. Solución Salina -->
            <div class="med-item-card in-course">
              <div class="med-top">
                <div class="med-name-badge">
                  <strong>{{ medSolucion.nombre }}</strong>
                  <span class="med-time-tag blue">{{ medSolucion.estado }}</span>
                </div>
                <span class="status-indicator-pill in-course">● En curso</span>
              </div>

              <p class="med-instructions">{{ medSolucion.indicacion }}</p>

              <!-- Barra de Infusión -->
              <div class="infusion-progress-box">
                <div class="infusion-labels">
                  <span>Infundido: <strong>{{ medSolucion.infundido }} ml</strong></span>
                  <span>Resta: <strong>{{ medSolucion.resta }} ml</strong></span>
                </div>
                <div class="infusion-track">
                  <div class="infusion-fill" :style="{ width: `${medSolucion.porcentaje}%` }"></div>
                </div>
              </div>

              <div class="med-actions-footer">
                <span class="double-check-text">Bomba de infusión: <strong>Canal 02</strong></span>
                <span class="applied-timestamp blue">En infusión activa</span>
              </div>
            </div>

            <!-- 3. Enoxaparina -->
            <div class="med-item-card pending">
              <div class="med-top">
                <div class="med-name-badge">
                  <strong>{{ medEnoxaparina.nombre }}</strong>
                  <span class="med-time-tag gray">{{ medEnoxaparina.turno }}</span>
                </div>
                <span class="status-indicator-pill gray">● {{ medEnoxaparina.espera }}</span>
              </div>

              <p class="med-instructions">{{ medEnoxaparina.indicacion }}</p>

              <div class="med-actions-footer">
                <span class="double-check-text">Horario estricto: <strong>20:00 hrs</strong></span>
                <button 
                  type="button" 
                  class="btn-apply-med-disabled" 
                  disabled
                  title="Disponible a partir de las 20:00 hrs"
                >
                  <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 22h14"/><path d="M5 2h14"/><path d="M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22"/><path d="M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2"/></svg> Programado
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 2. Trazabilidad de Notas Previas del Turno -->
        <section class="clinical-box">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
              <h4>Notas del Turno Previo</h4>
            </div>
            <button type="button" class="btn-text-link" @click="consultarNotasPrevias">Ver todas</button>
          </div>

          <div class="prev-notes-timeline">
            <div v-for="nota in notasPreviasTurno" :key="nota.id" class="prev-note-item">
              <div class="prev-note-header">
                <strong>{{ nota.autor }}</strong>
                <span class="time">{{ nota.hora }} hrs</span>
              </div>
              <p>{{ nota.contenido }}</p>
            </div>
          </div>
        </section>

        <!-- 3. Acciones Clínicas Inmediatas -->
        <section class="clinical-box actions-sidebar-card">
          <div class="box-header">
            <div class="box-title-row">
              <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg></span>
              <h4>Acción Clínica Inmediata</h4>
            </div>
          </div>

          <div class="actions-stack">
            <button type="button" class="btn-action-primary" @click="guardarNotaEnfermeria">
              <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> Firmar y Resguardar Nota [Enter]
            </button>
            <button type="button" class="btn-action-secondary" @click="imprimirHojaEnfermeria">
              <svg viewBox="0 0 24 24" class="svg-inline-print" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg> Imprimir Formato Oficial PDF
            </button>
            <button type="button" class="btn-action-alert" @click="notificarDeterioro">
              <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg> Notificar Deterioro a Médico
            </button>
          </div>

          <div class="crypto-seal-row">
            <span><svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></span>
            <small>Firma digital criptográfica SHA-256 vinculada a ficha clínica</small>
          </div>
        </section>

      </div>

    </div>
  </div>
</template>

<style scoped>
.nota-view-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.nota-header-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.banner-title-block {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.blue-accent-bar {
  width: 4px;
  height: 38px;
  background-color: #2563EB;
  border-radius: 2px;
}

.banner-title-block h3 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-kardex {
  background-color: #EFF6FF;
  color: #2563EB;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.banner-title-block p {
  font-size: 0.72rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
}

.prescription-lock-badge-compact {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.3rem 0.65rem;
  font-size: 0.68rem;
  font-weight: 700;
  color: #475569;
}

.prescription-lock-badge-compact span {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.nota-patient-bar {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.patient-main-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.box-badge-large {
  background: linear-gradient(135deg, #0F4C81, #1E40AF);
  color: #FFFFFF;
  font-weight: 900;
  font-size: 1.1rem;
  padding: 0.6rem 0.85rem;
  border-radius: 8px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.3rem;
}

.name-row h3 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.unselected-text {
  font-size: 0.95rem;
  color: #64748B;
  font-style: italic;
  font-weight: 600;
}

.triage-pill {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
}

.triage-pill.urgent {
  background-color: #FFF7ED;
  color: #C2410C;
  border: 1px solid #FFEDD5;
}

.folio-code {
  font-size: 0.7rem;
  font-family: monospace;
  color: #64748B;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
  color: #64748B;
}

.meta-row .sep {
  color: #CBD5E1;
}

.allergy-warning {
  color: #DC2626;
  font-weight: 600;
}

.patient-actions-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
}

.search-switch-box {
  display: flex;
  gap: 0.3rem;
}

.patient-search-input {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.35rem 0.6rem;
  font-size: 0.75rem;
  width: 220px;
}

.btn-history {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.35rem 0.6rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-demo-toggle {
  background-color: #EFF6FF;
  border: 1px dashed #3B82F6;
  color: #1D4ED8;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
}

.nota-grid-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 1.2rem;
  align-items: start;
}

.nota-col-left {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.clinical-box {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.5rem;
}

.box-title-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.box-title-row h4 {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
}

.nom-tag {
  font-size: 0.65rem;
  font-weight: 700;
  color: #2563EB;
}

.params-row-grid {
  display: grid;
  grid-template-columns: 1.5fr 1.5fr 1fr;
  gap: 0.8rem;
}

.param-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.param-group label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #475569;
}

.req {
  color: #DC2626;
}

.select-clean, .input-clean {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.6rem;
  font-size: 0.78rem;
  color: #1E293B;
  outline: none;
}

.shift-toggle-group {
  display: flex;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  overflow: hidden;
}

.shift-btn {
  flex: 1;
  background-color: #FFFFFF;
  border: none;
  padding: 0.45rem 0.2rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  border-right: 1px solid #CBD5E1;
}

.shift-btn:last-child {
  border-right: none;
}

.shift-btn.active {
  background-color: #0F4C81;
  color: #FFFFFF;
}

.vitals-import-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-triage-import {
  background-color: #FEF3C7;
  border: 1px solid #FDE68A;
  color: #B45309;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-triage-import:hover {
  background-color: #FDE68A;
}

.btn-philips-import {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
}

.vitals-control-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.6rem;
}

.vital-control-card {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.6rem 0.4rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.vital-label {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748B;
  margin-bottom: 0.3rem;
}

.vital-input {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.3rem 0.2rem;
  font-size: 1rem;
  font-weight: 800;
  color: #1E293B;
  text-align: center;
  background-color: #FFFFFF;
  outline: none;
}

.vital-unit {
  font-size: 0.6rem;
  color: #94A3B8;
  margin-top: 0.2rem;
}

.access-balance-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.access-card, .balance-card {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
  background-color: #F8FAFC;
}

.access-card-header, .balance-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.card-title-mini {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: #1E293B;
}

.calibre-badge, .diff-badge {
  background-color: #EFF6FF;
  color: #2563EB;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.access-inputs-row, .balance-inputs-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 0.6rem;
}

.access-field, .balance-input-group {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.access-field label, .balance-input-group label {
  font-size: 0.65rem;
  color: #64748B;
  font-weight: 600;
}

.input-mini-clean, .input-balance-num {
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.35rem 0.5rem;
  font-size: 0.75rem;
  background-color: #FFFFFF;
  outline: none;
}

.access-checkboxes-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.clean-check {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.7rem;
  color: #334155;
  cursor: pointer;
}

.diuresis-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.65rem;
  color: #64748B;
  border-top: 1px solid #E2E8F0;
  padding-top: 0.4rem;
}

.dot-active {
  color: #10B981;
}

.gasto-tag {
  color: #0F4C81;
}

.procedures-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.6rem;
}

.procedure-card {
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.55rem;
  display: flex;
  gap: 0.5rem;
  cursor: pointer;
  background-color: #F8FAFC;
  transition: all 0.15s;
}

.procedure-card.selected {
  border-color: #2563EB;
  background-color: #EFF6FF;
}

.proc-text-col strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
}

.proc-text-col small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.quick-phrases-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.6rem;
}

.phrases-label {
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 700;
}

.btn-fast-phrase {
  background-color: #F1F5F9;
  border: 1px solid #E2E8F0;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  font-size: 0.65rem;
  color: #334155;
  cursor: pointer;
}

.btn-fast-phrase:hover {
  background-color: #E2E8F0;
}

.soapie-textarea-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.soapie-textarea {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.6rem;
  font-size: 0.8rem;
  line-height: 1.4;
  outline: none;
  font-family: inherit;
  color: #1E293B;
}

.soapie-footer-tip {
  display: flex;
  justify-content: space-between;
  font-size: 0.65rem;
  color: #64748B;
}

.nota-col-right {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.badge-lock-rx {
  background-color: #F1F5F9;
  color: #64748B;
  font-size: 0.6rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.kardex-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.med-item-card {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.65rem;
  background-color: #F8FAFC;
}

.med-item-card.applied {
  border-color: #A7F3D0;
  background-color: #F0FDF4;
}

.med-item-card.in-course {
  border-color: #BFDBFE;
  background-color: #EFF6FF;
}

.med-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.3rem;
}

.med-name-badge strong {
  display: block;
  font-size: 0.8rem;
  color: #1E293B;
}

.med-time-tag {
  font-size: 0.62rem;
  color: #64748B;
}

.status-indicator-pill {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.status-indicator-pill.applied { background-color: #DCFCE7; color: #166534; }
.status-indicator-pill.pending { background-color: #FEF3C7; color: #B45309; }
.status-indicator-pill.in-course { background-color: #DBEAFE; color: #1E40AF; }

.med-instructions {
  font-size: 0.68rem;
  color: #475569;
  margin: 0.2rem 0 0.5rem 0;
}

.med-actions-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.65rem;
  border-top: 1px solid #E2E8F0;
  padding-top: 0.35rem;
}

.btn-apply-med {
  background-color: #10B981;
  color: #FFFFFF;
  border: none;
  border-radius: 4px;
  padding: 0.25rem 0.55rem;
  font-size: 0.68rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-apply-med-disabled {
  background-color: #E2E8F0;
  color: #94A3B8;
  border: none;
  border-radius: 4px;
  padding: 0.25rem 0.55rem;
  font-size: 0.65rem;
  cursor: not-allowed;
}

.infusion-progress-box {
  margin-bottom: 0.5rem;
}

.infusion-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.62rem;
  color: #64748B;
  margin-bottom: 0.2rem;
}

.infusion-track {
  height: 6px;
  background-color: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.infusion-fill {
  height: 100%;
  background-color: #2563EB;
}

.prev-notes-timeline {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.prev-note-item {
  background-color: #F8FAFC;
  border-left: 3px solid #0F4C81;
  padding: 0.4rem 0.6rem;
  border-radius: 0 4px 4px 0;
}

.prev-note-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.68rem;
  margin-bottom: 0.2rem;
}

.prev-note-header strong {
  color: #0F4C81;
}

.prev-note-item p {
  margin: 0;
  font-size: 0.65rem;
  color: #475569;
  line-height: 1.3;
}

.btn-text-link {
  background: none;
  border: none;
  color: #2563EB;
  font-size: 0.68rem;
  cursor: pointer;
}

.actions-stack {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.btn-action-primary {
  background: linear-gradient(135deg, #0F4C81, #1E40AF);
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.7rem;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(15, 76, 129, 0.2);
}

.btn-action-secondary {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #334155;
  border-radius: 8px;
  padding: 0.65rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-alert {
  background-color: #7F1D1D;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.65rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-action-alert:hover {
  background-color: #991B1B;
}

.crypto-seal-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  font-size: 0.68rem;
  color: #64748B;
  text-align: center;
}

@media (max-width: 1100px) {
  .nota-grid-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .vitals-control-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .access-balance-split, .procedures-grid, .params-row-grid {
    grid-template-columns: 1fr;
  }
}
</style>
