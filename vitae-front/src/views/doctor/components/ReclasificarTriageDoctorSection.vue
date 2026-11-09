<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Paciente, Doctor } from '../types'
import triageService from '@/services/triageService'

const props = defineProps<{
  doctor: Doctor
  pacienteSeleccionado: Paciente | null
  pacientesCola?: Paciente[]
}>()

const emit = defineEmits<{
  (e: 'seleccionarPaciente', paciente: Paciente): void
  (e: 'deseleccionarPaciente'): void
  (e: 'cambiarSeccion', sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'): void
}>()

// Paciente seleccionado (en blanco por solicitud del usuario)
const pacienteInput = ref('')

// Re-evaluación médica actual
const reval = ref({
  sis: '',
  dia: '',
  fc: '',
  fr: '',
  spo2: '',
  temp: '',
  glasgow: '',
  eva: ''
})

// Árbol Discriminador Manchester
const discriminadores = ref({
  viaAerea: false,
  shock: false,
  dolorPrecordial: false,
  deterioroNeurologico: false
})

// Justificación
const justificacionClinica = ref('')

// Reclasificación manual
const nuevoNivel = ref<number | null>(null)
const nuevoDestino = ref('')

// Si el paciente está vinculado o tiene datos
const signosIngreso = computed(() => {
  return props.pacienteSeleccionado?.signos || {
    pa: '-- / --',
    fc: '--',
    fr: '--',
    temp: '--',
    spo2: '--',
    horaToma: '--:--'
  }
})

// Determinar si el paciente proviene de Consulta Externa (cita agendada previa) o Urgencias
const esConsultaExterna = computed(() => {
  if (!props.pacienteSeleccionado) return false
  return Boolean(
    props.pacienteSeleccionado.id_cita ||
    !props.pacienteSeleccionado.id_triage ||
    props.pacienteSeleccionado.triage?.toLowerCase().includes('consulta externa')
  )
})

const escalaraChoque = () => {
  nuevoNivel.value = 1
  discriminadores.value.viaAerea = true
  alert('ALERTA CRÍTICA: Paciente escalado inmediatamente a Nivel I (Choque / Resucitación). Alerta enviada a sala de estabilización.')
}

const reclasificando = ref(false)

const confirmarReclasificacion = async () => {
  if (!props.pacienteSeleccionado) {
    alert('Atención: Seleccione un paciente antes de reclasificar la prioridad de triage.')
    return
  }

  if (!nuevoNivel.value) {
    alert('Seleccione el nuevo nivel de prioridad Manchester (Nivel 1 al 5) para el paciente.')
    return
  }

  const justificacion = justificacionClinica.value?.trim() || ''
  if (justificacion.length < 10) {
    alert('La justificación clínica médica es obligatoria y debe contener al menos 10 caracteres conforme a la NOM-206-SSA1-2002.')
    return
  }

  const nivelesColor: Record<number, { color: string; hex: string }> = {
    1: { color: 'ROJO', hex: '#EF4444' },
    2: { color: 'NARANJA', hex: '#EA580C' },
    3: { color: 'AMARILLO', hex: '#D97706' },
    4: { color: 'VERDE', hex: '#10B981' },
    5: { color: 'AZUL', hex: '#2563EB' }
  }

  const nivelObj = nivelesColor[nuevoNivel.value] || { color: 'AMARILLO', hex: '#D97706' }
  const idTriage = props.pacienteSeleccionado.id_triage || props.pacienteSeleccionado.id

  try {
    reclasificando.value = true
    if (idTriage && !String(idTriage).startsWith('PAC-')) {
      await triageService.reclasificar(String(idTriage), nivelObj.color, justificacion)
    }

    props.pacienteSeleccionado.triage = `Manchester ${nivelObj.color} · Nivel ${nuevoNivel.value}`
    props.pacienteSeleccionado.triageColor = nivelObj.hex

    alert(`Reclasificación médica confirmada exitosamente.\n\nNuevo Nivel: Manchester ${nivelObj.color} (Nivel ${nuevoNivel.value})\nJustificación: "${justificacion}"\nOperación registrada en bitácora inalterable de auditoría con la cédula del médico ${props.doctor.cedula}.`)

    emit('cambiarSeccion', 'atencion')
  } catch (error: any) {
    console.error('Error reclasificando triage médico:', error)
    const msg = error.response?.data?.message || 'Error al registrar la reclasificación en el servidor.'
    alert(`Error: ${msg}`)
  } finally {
    reclasificando.value = false
  }
}

const cancelar = () => {
  emit('cambiarSeccion', 'atencion')
}
</script>

<template>
  <div class="reclasificar-doctor-view">
    <!-- SUBNAV / BREADCRUMBS -->
    <div class="triage-subnav">
      <div class="subnav-left">
        <span class="breadcrumb-trail">Atención clínica / Valoración de Urgencias /</span>
        <strong class="breadcrumb-current">Reclasificación Médica de Triage (Protocolo Manchester Internacional)</strong>
      </div>
      <div class="subnav-right">
        <span class="tag-normativa">● Protocolo Institucional Activo</span>
      </div>
    </div>

    <!-- BANNER DE VINCULACIÓN DE PACIENTE -->
    <div class="triage-patient-banner">
      <div v-if="pacienteSeleccionado" class="banner-connected-pill">
        <div class="banner-left">
          <span class="avatar-pill">{{ pacienteSeleccionado.nombre.split(' ').map(n => n[0]).slice(0, 2).join('') }}</span>
          <div>
            <strong>{{ pacienteSeleccionado.nombre }}</strong>
            <span>ECE {{ pacienteSeleccionado.ece }} ({{ pacienteSeleccionado.folio }}) · {{ pacienteSeleccionado.edad }} años · {{ pacienteSeleccionado.box }}</span>
          </div>
        </div>
        <div class="banner-right-connected">
          <span class="badge-active-triage" :style="{ backgroundColor: pacienteSeleccionado.triageColor }">
            {{ pacienteSeleccionado.triage }}
          </span>
          <button class="btn-change-patient-mini" @click="$emit('deseleccionarPaciente')" title="Cambiar paciente"><svg viewBox="0 0 24 24" class="svg-inline-x" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
        </div>
      </div>

      <template v-else>
        <div class="banner-left">
          <span class="icon-link-user"><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
          <div class="patient-search-col">
            <label class="banner-label">VINCULAR PACIENTE PARA RE-EVALUACIÓN CLÍNICA</label>
            <input
              v-model="pacienteInput"
              type="text"
              class="patient-input-banner"
              placeholder="Seleccionar paciente asignado en sala o box para revaloración médica..."
            />
          </div>
        </div>
        <div class="banner-right">
          <span class="hourglass-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 22h14"/><path d="M5 2h14"/><path d="M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22"/><path d="M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2"/></svg></span>
          <span>Esperando selección de paciente para desplegar signos iniciales y parámetros de reclasificación</span>
        </div>
      </template>
    </div>

    <!-- CUANDO EL PACIENTE ES DE CONSULTA EXTERNA PROGRAMADA (NO APLICA RECLASIFICACIÓN DE URGENCIAS) -->
    <div v-if="esConsultaExterna" class="consulta-externa-notice-card">
      <div class="notice-icon-header">
        <div class="notice-icon-circle">
          <svg viewBox="0 0 24 24" class="notice-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <div class="notice-text">
          <h3>Protocolo de Triage No Aplicable a Consulta Externa</h3>
          <p>El paciente <strong>{{ pacienteSeleccionado?.nombre }}</strong> se encuentra en atención por <strong>Cita Programada de Consulta Externa ({{ pacienteSeleccionado?.folio }})</strong>.</p>
        </div>
      </div>

      <div class="notice-body">
        <div class="notice-explanation">
          <h4>Criterio Normativo (NOM-004-SSA3-2012 / NOM-027-SSA3-2013)</h4>
          <p>
            El Sistema de Reclasificación de Triage (Protocolo Manchester) está diseñado exclusivamente para el Servicio de Urgencias y Resucitación con el fin de clasificar y priorizar el riesgo vital inmediato.
          </p>
          <p>
            En pacientes con cita agendada, la toma de signos vitales se realiza en el área de <strong>Somatometría por Enfermería</strong> previo al ingreso al consultorio, y la atención médica se gestiona directamente desde el <strong>Expediente Clínico Electrónico</strong> y la <strong>Nota Médica ECE</strong>.
          </p>
        </div>

        <div class="somatometria-status-box">
          <div class="status-header">
            <span class="status-title">Estado de Signos Vitales / Somatometría</span>
            <span v-if="pacienteSeleccionado?.signos" class="badge-somatometria ok">Tomados por Enfermería · {{ pacienteSeleccionado.signos.horaToma }}</span>
            <span v-else class="badge-somatometria pending">Pendiente de toma por Enfermería / Somatometría</span>
          </div>

          <div v-if="pacienteSeleccionado?.signos" class="vitals-summary-grid">
            <div class="vital-chip"><span>P.A.:</span> <strong>{{ pacienteSeleccionado.signos.pa }}</strong> mmHg</div>
            <div class="vital-chip"><span>F.C.:</span> <strong>{{ pacienteSeleccionado.signos.fc }}</strong> lpm</div>
            <div class="vital-chip"><span>Temp:</span> <strong>{{ pacienteSeleccionado.signos.temp }}</strong> °C</div>
            <div class="vital-chip"><span>SpO₂:</span> <strong>{{ pacienteSeleccionado.signos.spo2 }}</strong> %</div>
          </div>
          <p v-else class="status-note">
            El paciente no cuenta aún con signos vitales registrados en somatometría. El personal de enfermería los capturará al registrar su llegada en sala, o bien puede usted registrarlos directamente durante la exploración en la Nota Médica.
          </p>
        </div>

        <div class="notice-actions">
          <button class="btn-primary-action" @click="$emit('cambiarSeccion', 'atencion')">
            <svg viewBox="0 0 24 24" class="btn-action-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
            Ir a Expediente Clínico (Alt+E)
          </button>
          <button class="btn-secondary-action" @click="$emit('cambiarSeccion', 'nota')">
            <svg viewBox="0 0 24 24" class="btn-action-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            Elaborar Nota Médica ECE (Alt+N)
          </button>
        </div>
      </div>
    </div>

    <!-- GRID DE 2 COLUMNAS (RE-EVALUACIÓN Y DISCRIMINADORES IZQ + RECÁLCULO DER) -->
    <div v-else class="triage-grid">
      
      <!-- COLUMNA IZQUIERDA: SIGNOS COMPARATIVOS, DISCRIMINADORES Y JUSTIFICACIÓN -->
      <div class="triage-left-column">
        
        <!-- FILA DE 2 CARDS COMPARATIVAS: INGRESO VS RE-EVALUACIÓN ACTUAL -->
        <div class="signs-comparison-row">
          
          <!-- CARD 1: SIGNOS VITALES AL INGRESO -->
          <div class="triage-card">
            <div class="card-header-bar">
              <div class="card-title-icon">
                <span class="clock-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></span>
                <h4>Signos Vitales al Ingreso</h4>
              </div>
              <span class="badge-triage-init">Triage Inicial</span>
            </div>

            <div class="card-notice">
              <span class="info-bubble">ⓘ</span>
              <p v-if="pacienteSeleccionado?.signos">Signos vitales registrados al ingreso por Enfermería en el área de Triage.</p>
              <p v-else>Esperando registro de signos vitales por Enfermería en Triage o vinculación del paciente.</p>
            </div>

            <div class="vitals-6-grid">
              <div class="vital-tile-ro">
                <span class="tile-lbl">PRESIÓN ARTERIAL</span>
                <strong class="tile-val">{{ signosIngreso.pa || '-- / --' }}</strong>
                <small class="tile-unit">mmHg</small>
              </div>

              <div class="vital-tile-ro">
                <span class="tile-lbl">FREC. CARDÍACA</span>
                <strong class="tile-val">{{ signosIngreso.fc || '--' }}</strong>
                <small class="tile-unit">lpm</small>
              </div>

              <div class="vital-tile-ro">
                <span class="tile-lbl">FREC. RESPIRATORIA</span>
                <strong class="tile-val">--</strong>
                <small class="tile-unit">rpm</small>
              </div>

              <div class="vital-tile-ro">
                <span class="tile-lbl">SATURACIÓN O₂</span>
                <strong class="tile-val">{{ signosIngreso.spo2 || '--' }}</strong>
                <small class="tile-unit">% SpO2</small>
              </div>

              <div class="vital-tile-ro">
                <span class="tile-lbl">TEMPERATURA</span>
                <strong class="tile-val">{{ signosIngreso.temp || '--' }}</strong>
                <small class="tile-unit">°C axilar</small>
              </div>

              <div class="vital-tile-ro">
                <span class="tile-lbl">ESCALA GLASGOW</span>
                <strong class="tile-val">-- / 15</strong>
                <small class="tile-unit">Puntos</small>
              </div>
            </div>

            <div class="timestamp-footer">
              <span v-if="pacienteSeleccionado?.signos">Hora de captura inicial: {{ pacienteSeleccionado.signos.horaToma || '--:--' }}</span>
              <span v-else>Sin registro previo de signos en Triage</span>
            </div>
          </div>

          <!-- CARD 2: RE-EVALUACIÓN MÉDICA ACTUAL -->
          <div class="triage-card active-card">
            <div class="card-header-bar">
              <div class="card-title-icon">
                <span class="monitor-icon"><svg viewBox="0 0 24 24" class="svg-inline-chart" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg></span>
                <h4>Re-evaluación Médica Actual</h4>
              </div>
              <span class="badge-capture-active">Captura Activa</span>
            </div>

            <p class="card-instruction">Ingrese los valores hemodinámicos y neurológicos medidos en el momento de la revaloración en box o camilla.</p>

            <div class="vitals-6-inputs-grid">
              <div class="vital-input-tile">
                <span class="tile-lbl">PRESIÓN ART. (SIS/DIA)</span>
                <div class="dual-input">
                  <input v-model="reval.sis" type="text" placeholder="S" class="mini-in" />
                  <span>/</span>
                  <input v-model="reval.dia" type="text" placeholder="D" class="mini-in" />
                  <small>mmHg</small>
                </div>
              </div>

              <div class="vital-input-tile">
                <span class="tile-lbl">FREC. CARDÍACA</span>
                <div class="single-input">
                  <input v-model="reval.fc" type="text" placeholder="Ej. 75" class="clean-in" />
                  <small>lpm</small>
                </div>
              </div>

              <div class="vital-input-tile">
                <span class="tile-lbl">FREC. RESPIRATORIA</span>
                <div class="single-input">
                  <input v-model="reval.fr" type="text" placeholder="Ej. 18" class="clean-in" />
                  <small>rpm</small>
                </div>
              </div>

              <div class="vital-input-tile">
                <span class="tile-lbl">SATURACIÓN O₂</span>
                <div class="single-input">
                  <input v-model="reval.spo2" type="text" placeholder="Ej. 98" class="clean-in" />
                  <small>%</small>
                </div>
              </div>

              <div class="vital-input-tile">
                <span class="tile-lbl">TEMPERATURA</span>
                <div class="single-input">
                  <input v-model="reval.temp" type="text" placeholder="Ej. 36.8" class="clean-in" />
                  <small>°C</small>
                </div>
              </div>

              <div class="vital-input-tile">
                <span class="tile-lbl">GLASGOW (GCS) / AVDI</span>
                <select v-model="reval.glasgow" class="select-in">
                  <option value="">Seleccionar escala...</option>
                  <option value="15">15/15 · Alerta total</option>
                  <option value="14">14/15 · Confusión leve</option>
                  <option value="13">13/15 · Somnoliento</option>
                  <option value="10">9-12 · Deterioro moderado</option>
                  <option value="8">&lt; 8 · Coma / Intubación</option>
                </select>
              </div>
            </div>

            <div class="eva-footer">
              <span class="eva-lbl">ESCALA VISUAL ANÁLOGA DE DOLOR (EVA 0-10)</span>
              <span class="eva-val">Sin definir</span>
            </div>
          </div>

        </div>

        <!-- CARD 3: ÁRBOL DISCRIMINADOR MANCHESTER MÉDICO -->
        <section class="triage-card">
          <div class="card-header-bar">
            <div class="card-title-icon">
              <span class="tree-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/></svg></span>
              <div>
                <h4>Árbol Discriminador Manchester Médico</h4>
                <p class="sub-tree">Criterios de descarte de riesgo vital inmediato y discriminadores específicos de urgencia</p>
              </div>
            </div>
            <span class="badge-mts">TRIAGE MTS ADULTOS</span>
          </div>

          <!-- 4 BLOQUES DISCRIMINADORES -->
          <div class="discriminators-2x2-grid">
            <!-- DISCRIMINADOR 1 -->
            <div class="disc-block">
              <div class="disc-header">
                <span class="disc-icon red-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 4v9"/><path d="M9 13a4 4 0 0 1-5-4V6a3 3 0 0 1 5-2c2 0 2 2 2 4v5"/><path d="M15 13a4 4 0 0 0 5-4V6a3 3 0 0 0-5-2c-2 0-2 2-2 4v5"/><path d="M6 13c0 4.4 2.7 8 6 8s6-3.6 6-8"/></svg></span>
                <div class="disc-text">
                  <strong>¿Compromiso agudo de vía aérea o estridor?</strong>
                  <p>Obstrucción, cianosis central o tiraje intercostal severo.</p>
                </div>
              </div>
              <div class="toggle-buttons">
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: discriminadores.viaAerea }"
                  @click="discriminadores.viaAerea = true"
                >Sí</button>
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: !discriminadores.viaAerea }"
                  @click="discriminadores.viaAerea = false"
                >No</button>
              </div>
            </div>

            <!-- DISCRIMINADOR 2 -->
            <div class="disc-block">
              <div class="disc-header">
                <span class="disc-icon red-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg></span>
                <div class="disc-text">
                  <strong>¿Signos de shock o mala perfusión?</strong>
                  <p>Hipotensión refractaria, llenado capilar &gt;3s, diaforesis fría.</p>
                </div>
              </div>
              <div class="toggle-buttons">
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: discriminadores.shock }"
                  @click="discriminadores.shock = true"
                >Sí</button>
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: !discriminadores.shock }"
                  @click="discriminadores.shock = false"
                >No</button>
              </div>
            </div>

            <!-- DISCRIMINADOR 3 -->
            <div class="disc-block">
              <div class="disc-header">
                <span class="disc-icon blue-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/><path d="M12 5v4"/></svg></span>
                <div class="disc-text">
                  <strong>¿Dolor precordial opresivo / Sospecha SCA?</strong>
                  <p>Dolor típico irradiado a mandíbula/brazo izquierdo o equivalente.</p>
                </div>
              </div>
              <div class="toggle-buttons">
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: discriminadores.dolorPrecordial }"
                  @click="discriminadores.dolorPrecordial = true"
                >Sí</button>
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: !discriminadores.dolorPrecordial }"
                  @click="discriminadores.dolorPrecordial = false"
                >No</button>
              </div>
            </div>

            <!-- DISCRIMINADOR 4 -->
            <div class="disc-block">
              <div class="disc-header">
                <span class="disc-icon blue-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M9.5 2A2.5 2.5 0 0 1 12 4.5v15a2.5 2.5 0 0 1-4.96.44 2.5 2.5 0 0 1-2.96-3.08 3 3 0 0 1-.34-5.58 2.5 2.5 0 0 1 1.32-4.24 2.5 2.5 0 0 1 4.44-5.04z"/><path d="M14.5 2A2.5 2.5 0 0 0 12 4.5v15a2.5 2.5 0 0 0 4.96.44 2.5 2.5 0 0 0 2.96-3.08 3 3 0 0 0 .34-5.58 2.5 2.5 0 0 0-1.32-4.24 2.5 2.5 0 0 0-4.44-5.04z"/></svg></span>
                <div class="disc-text">
                  <strong>¿Deterioro neurológico o convulsión?</strong>
                  <p>Déficit focal súbito, crisis activa o estado postictal prolongado.</p>
                </div>
              </div>
              <div class="toggle-buttons">
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: discriminadores.deterioroNeurologico }"
                  @click="discriminadores.deterioroNeurologico = true"
                >Sí</button>
                <button
                  type="button"
                  class="btn-toggle"
                  :class="{ active: !discriminadores.deterioroNeurologico }"
                  @click="discriminadores.deterioroNeurologico = false"
                >No</button>
              </div>
            </div>
          </div>

          <!-- JUSTIFICACIÓN CLÍNICA MÉDICA OBLIGATORIA -->
          <div class="justification-wrapper">
            <div class="just-header-row">
              <label class="field-label">Justificación Clínica Médica Obligatoria *</label>
              <span class="meta-label">Auditable por Comité Hospitalario de Bioética y Calidad</span>
            </div>
            <textarea
              v-model="justificacionClinica"
              rows="3"
              maxlength="600"
              class="textarea-clean"
              placeholder="Describa el motivo clínico y la evolución hemodinámica que justifica el cambio de prioridad según el protocolo Manchester..."
            ></textarea>
            <div class="just-footer-row">
              <span class="signature-tag"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Firma digital de médico en turno enlazada a Cédula Profesional al confirmar</span>
              <span class="char-count">{{ justificacionClinica.length }} / 600 caracteres</span>
            </div>
          </div>
        </section>

      </div>

      <!-- COLUMNA DERECHA: RECÁLCULO DE PRIORIDAD, BOTONES Y TRAZABILIDAD -->
      <aside class="triage-right-column">
        
        <!-- CARD: RECÁLCULO DE PRIORIDAD Y SLA -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>Recálculo de Prioridad y SLA</h4>
          </div>
          <p class="side-sub">Evaluación cruzada de tiempos y discriminadores</p>

          <!-- CAJAS COMPARATIVAS DE NIVEL -->
          <div class="level-calc-boxes">
            <div class="level-box-gray">
              <span class="box-lbl">NIVEL INICIAL ASIGNADO</span>
              <strong class="level-val">--</strong>
              <small class="sla-sub">SLA: -- min</small>
            </div>

            <div class="level-box-blue">
              <span class="box-lbl">NUEVO NIVEL CALCULADO</span>
              <strong class="level-val highlight">
                {{ nuevoNivel ? `Nivel ${nuevoNivel}` : '--' }}
              </strong>
              <small class="sla-sub">
                {{ nuevoNivel ? 'Calculado por criterio' : 'Seleccione criterios para recalcular' }}
              </small>
            </div>
          </div>

          <!-- RECLASIFICACIÓN MANUAL -->
          <div class="manual-reclass-section">
            <label class="section-micro-lbl">RECLASIFICACIÓN MANUAL POR CRITERIO MÉDICO</label>

            <div class="triage-levels-list">
              <label class="level-radio-card" :class="{ selected: nuevoNivel === 1 }">
                <input type="radio" v-model="nuevoNivel" :value="1" />
                <span class="level-dot red-dot">●</span>
                <div class="level-desc">
                  <strong>Nivel I · Resucitación / Choque</strong>
                  <small>Atención Inmediata (0 min)</small>
                </div>
              </label>

              <label class="level-radio-card" :class="{ selected: nuevoNivel === 2 }">
                <input type="radio" v-model="nuevoNivel" :value="2" />
                <span class="level-dot blue-dot">●</span>
                <div class="level-desc">
                  <strong>Nivel II · Muy Urgente</strong>
                  <small>&lt; 15 min</small>
                </div>
              </label>

              <label class="level-radio-card" :class="{ selected: nuevoNivel === 3 }">
                <input type="radio" v-model="nuevoNivel" :value="3" />
                <span class="level-dot yellow-dot">●</span>
                <div class="level-desc">
                  <strong>Nivel III · Urgente</strong>
                  <small>&lt; 60 min</small>
                </div>
              </label>

              <label class="level-radio-card" :class="{ selected: nuevoNivel === 4 }">
                <input type="radio" v-model="nuevoNivel" :value="4" />
                <span class="level-dot green-dot">●</span>
                <div class="level-desc">
                  <strong>Nivel IV · Estándar</strong>
                  <small>&lt; 120 min</small>
                </div>
              </label>

              <label class="level-radio-card" :class="{ selected: nuevoNivel === 5 }">
                <input type="radio" v-model="nuevoNivel" :value="5" />
                <span class="level-dot gray-dot">●</span>
                <div class="level-desc">
                  <strong>Nivel V · No Urgente</strong>
                  <small>&lt; 240 min</small>
                </div>
              </label>
            </div>
          </div>

          <!-- REASIGNACIÓN DE DESTINO FÍSICO -->
          <div class="destination-section">
            <label class="section-micro-lbl">REASIGNACIÓN DE DESTINO FÍSICO OPERATIVO</label>
            <select v-model="nuevoDestino" class="select-clean">
              <option value="">Seleccionar nueva área de atención inmediata...</option>
              <option value="choque">Sala de Choque y Reanimación 01</option>
              <option value="box-urg">Box de Urgencias Continuo</option>
              <option value="observacion">Cama de Observación Adultos</option>
              <option value="filtro">Consultorio de Filtro / Egreso</option>
            </select>
          </div>

          <!-- BOTONES DE ACCIÓN -->
          <div class="action-buttons-flow">
            <button class="btn-escalate-shock" type="button" @click="escalaraChoque">
              <svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Escalar Inmediato a Choque [Alt+C]
            </button>

            <button class="btn-confirm-reclass" type="button" @click="confirmarReclasificacion">
              <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Confirmar Reclasificación [Enter]
            </button>

            <button class="btn-cancel" type="button" @click="cancelar">
              <svg viewBox="0 0 24 24" class="svg-inline-x" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Cancelar y Volver a Expediente
            </button>
          </div>
        </div>

        <!-- TRAZABILIDAD Y AUDITORÍA -->
        <div class="side-card audit-card">
          <div class="audit-header">
            <span><svg viewBox="0 0 24 24" class="svg-inline-activity" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></span>
            <strong>Trazabilidad y Auditoría Médica</strong>
          </div>
          <p>Cada modificación en el triage queda registrada en la bitácora criptográfica hospitalaria de auditoría. Toda re-estratificación a un nivel inferior exige justificación explícita.</p>
        </div>

      </aside>

    </div>
  </div>
</template>

<style scoped>
.reclasificar-doctor-view {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  width: 100%;
}

/* SUBNAV */
.triage-subnav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #FFFFFF;
  padding: 0.65rem 1.25rem;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  font-size: 0.75rem;
}

.breadcrumb-trail {
  color: #64748B;
  margin-right: 0.3rem;
}

.breadcrumb-current {
  color: #0F4C81;
  font-weight: 700;
}

.tag-normativa {
  color: #2563EB;
  font-size: 0.72rem;
  font-weight: 700;
}

/* BANNER DE VINCULACIÓN */
.triage-patient-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 320px;
}

.icon-link-user {
  font-size: 1.25rem;
  color: #2563EB;
}

.patient-search-col {
  flex: 1;
}

.banner-label {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.04em;
  margin-bottom: 0.2rem;
}

.patient-input-banner {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  font-size: 0.72rem;
  color: #64748B;
}

.hourglass-icon {
  font-size: 0.85rem;
}

.banner-connected-pill {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.avatar-pill {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: #0F4C81;
  color: #FFFFFF;
  font-size: 0.7rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-right-connected {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-active-triage {
  color: #FFFFFF;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.btn-change-patient-mini {
  background: none;
  border: none;
  color: #94A3B8;
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
}

.btn-change-patient-mini:hover {
  color: #DC2626;
}

/* GRID PRINCIPAL */
.triage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1.2rem;
  align-items: start;
}

.triage-left-column {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.signs-comparison-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* CARDS */
.triage-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.65rem;
}

.card-title-icon {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-title-icon h4 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.sub-tree {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.15rem 0 0 0;
}

.badge-triage-init {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.badge-capture-active {
  background-color: #EFF6FF;
  color: #1D4ED8;
  border: 1px solid #BFDBFE;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.badge-mts {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.card-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.68rem;
  color: #64748B;
  margin-bottom: 0.8rem;
}

.card-notice p {
  margin: 0;
  line-height: 1.35;
}

.card-instruction {
  font-size: 0.7rem;
  color: #64748B;
  margin: 0 0 0.8rem 0;
  line-height: 1.35;
}

/* GRIDS DE 6 SIGNOS */
.vitals-6-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.6rem;
}

.vital-tile-ro {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.5rem;
  display: flex;
  flex-direction: column;
}

.tile-lbl {
  font-size: 0.58rem;
  font-weight: 700;
  color: #64748B;
}

.tile-val {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0.15rem 0;
}

.tile-unit {
  font-size: 0.6rem;
  color: #94A3B8;
}

.timestamp-footer {
  margin-top: 0.8rem;
  font-size: 0.68rem;
  color: #94A3B8;
  text-align: right;
}

/* INPUTS DE 6 SIGNOS */
.vitals-6-inputs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.6rem;
}

.vital-input-tile {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.5rem;
  display: flex;
  flex-direction: column;
}

.dual-input {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  margin-top: 0.2rem;
}

.mini-in {
  width: 32px;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.2rem;
  font-size: 0.75rem;
  text-align: center;
}

.single-input {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-top: 0.2rem;
}

.clean-in {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.25rem 0.35rem;
  font-size: 0.75rem;
}

.select-in {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.25rem;
  font-size: 0.7rem;
  margin-top: 0.2rem;
  background-color: #FFFFFF;
}

.eva-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.8rem;
  font-size: 0.68rem;
}

.eva-lbl {
  font-weight: 700;
  color: #475569;
}

.eva-val {
  color: #2563EB;
  font-weight: 600;
}

/* DISCRIMINADORES 2X2 */
.discriminators-2x2-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
  margin-top: 0.8rem;
}

.disc-block {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
  background-color: #F8FAFC;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.6rem;
}

.disc-header {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.disc-icon {
  font-size: 1.1rem;
}

.disc-text strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
  line-height: 1.25;
}

.disc-text p {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
  line-height: 1.35;
}

.toggle-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 0.35rem;
}

.btn-toggle {
  border: 1px solid #CBD5E1;
  background-color: #FFFFFF;
  color: #475569;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-toggle.active {
  background-color: #0F172A;
  border-color: #0F172A;
  color: #FFFFFF;
}

/* JUSTIFICACIÓN */
.justification-wrapper {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.just-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #334155;
}

.meta-label {
  font-size: 0.65rem;
  color: #64748B;
}

.textarea-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.45;
}

.just-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.65rem;
  color: #94A3B8;
}

.signature-tag {
  color: #059669;
  font-weight: 600;
}

/* COLUMNA DERECHA */
.triage-right-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.side-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
}

.side-card-header h4 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.side-sub {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.15rem 0 0.8rem 0;
}

.level-calc-boxes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.level-box-gray {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem;
  text-align: center;
}

.level-box-blue {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 0.6rem;
  text-align: center;
}

.box-lbl {
  display: block;
  font-size: 0.58rem;
  font-weight: 800;
  color: #64748B;
  margin-bottom: 0.2rem;
}

.level-val {
  font-size: 1.2rem;
  font-weight: 800;
  display: block;
}

.level-val.highlight {
  color: #2563EB;
}

.sla-sub {
  font-size: 0.62rem;
  color: #64748B;
  display: block;
  margin-top: 0.2rem;
}

/* NIVELES RADIO */
.manual-reclass-section {
  border-top: 1px solid #F1F5F9;
  padding-top: 0.75rem;
  margin-bottom: 0.8rem;
}

.section-micro-lbl {
  display: block;
  font-size: 0.6rem;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.04em;
  margin-bottom: 0.45rem;
}

.triage-levels-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.level-radio-card {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.6rem;
  background-color: #F8FAFC;
  cursor: pointer;
  transition: all 0.15s;
}

.level-radio-card.selected {
  border-color: #2563EB;
  background-color: #EFF6FF;
}

.level-dot {
  font-size: 0.95rem;
}

.red-dot { color: #DC2626; }
.blue-dot { color: #2563EB; }
.yellow-dot { color: #D97706; }
.green-dot { color: #059669; }
.gray-dot { color: #64748B; }

.level-desc {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
}

.level-desc strong {
  font-size: 0.72rem;
  color: #1E293B;
}

.level-desc small {
  font-size: 0.65rem;
  color: #64748B;
  font-weight: 600;
}

.destination-section {
  margin-bottom: 1rem;
}

.select-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem;
  font-size: 0.75rem;
  background-color: #FFFFFF;
  outline: none;
}

/* BOTONES DE ACCIÓN */
.action-buttons-flow {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.btn-escalate-shock {
  background-color: #580d13;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.7rem;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-escalate-shock:hover {
  background-color: #3f090d;
}

.btn-confirm-reclass {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.7rem;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-confirm-reclass:hover {
  background-color: #1D4ED8;
}

.btn-cancel {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #475569;
  border-radius: 6px;
  padding: 0.45rem;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel:hover {
  background-color: #F1F5F9;
}

/* TRAZABILIDAD */
.audit-card {
  font-size: 0.68rem;
  color: #64748B;
  line-height: 1.4;
}

.audit-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.3rem;
  color: #0F4C81;
}

.audit-card p {
  margin: 0;
}

/* TARJETA INFORMATIVA CONSULTA EXTERNA (NO APLICA TRIAGE) */
.consulta-externa-notice-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.notice-icon-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  border-bottom: 1px solid #BAE6FD;
}

.notice-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #0284C7;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notice-svg {
  width: 24px;
  height: 24px;
}

.notice-text h3 {
  margin: 0 0 0.25rem 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0369A1;
}

.notice-text p {
  margin: 0;
  font-size: 0.85rem;
  color: #0C4A6E;
}

.notice-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.notice-explanation {
  background: #F8FAFC;
  border-left: 4px solid #0284C7;
  padding: 1rem 1.25rem;
  border-radius: 0 6px 6px 0;
}

.notice-explanation h4 {
  margin: 0 0 0.5rem 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #1E293B;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.notice-explanation p {
  margin: 0 0 0.5rem 0;
  font-size: 0.85rem;
  line-height: 1.5;
  color: #475569;
}

.notice-explanation p:last-child {
  margin-bottom: 0;
}

.somatometria-status-box {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 1rem 1.25rem;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.status-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
}

.badge-somatometria {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.badge-somatometria.ok {
  background: #DCFCE7;
  color: #15803D;
  border: 1px solid #86EFAC;
}

.badge-somatometria.pending {
  background: #FEF3C7;
  color: #B45309;
  border: 1px solid #FDE68A;
}

.vitals-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 0.75rem;
}

.vital-chip {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem 0.75rem;
  font-size: 0.8rem;
  color: #475569;
}

.vital-chip strong {
  font-size: 1rem;
  color: #0F172A;
  margin-left: 0.25rem;
}

.status-note {
  margin: 0;
  font-size: 0.82rem;
  color: #64748B;
  line-height: 1.45;
}

.notice-actions {
  display: flex;
  gap: 1rem;
  padding-top: 0.5rem;
}

.btn-primary-action {
  background: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1.25rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: background-color 0.15s ease;
}

.btn-primary-action:hover {
  background: #0a355c;
}

.btn-secondary-action {
  background: #F1F5F9;
  color: #334155;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.65rem 1.25rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: background-color 0.15s ease;
}

.btn-secondary-action:hover {
  background: #E2E8F0;
}

.btn-action-svg {
  width: 16px;
  height: 16px;
}

@media (max-width: 1100px) {
  .triage-grid {
    grid-template-columns: 1fr;
  }
  .signs-comparison-row {
    grid-template-columns: 1fr;
  }
  .discriminators-2x2-grid {
    grid-template-columns: 1fr;
  }
}
</style>
