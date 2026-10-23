<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import type { Paciente, Doctor } from '../types'

const props = defineProps<{
  doctor: Doctor
  pacienteSeleccionado: Paciente | null
  pacientesCola?: Paciente[]
  diagnosticoGlobal?: string
}>()

const emit = defineEmits<{
  (e: 'seleccionarPaciente', paciente: Paciente): void
  (e: 'deseleccionarPaciente'): void
  (e: 'actualizarDiagnostico', diagnostico: string): void
  (e: 'guardarBorrador'): void
  (e: 'guardarYFirmar'): void
}>()

// Modelos reactivos limpios
const tipoNota = ref('Nota de Evolución / Urgencias')
const pacienteVinculadoId = ref(props.pacienteSeleccionado?.id || '')

// SOAP
const anamnesis = ref('')

// Signos Vitales (se pre-pueblan con los signos de triage del paciente si existen)
const signos = ref({
  ta: props.pacienteSeleccionado?.signos?.pa || '',
  fc: props.pacienteSeleccionado?.signos?.fc || '',
  fr: '18',
  temp: props.pacienteSeleccionado?.signos?.temp || '',
  spo2: props.pacienteSeleccionado?.signos?.spo2 || '',
  glucosa: '105',
  eva: '0/10',
  glasgow: '15/15'
})

const exploracionSegmentaria = ref('')
const busquedaCie = ref(props.diagnosticoGlobal || '')
const tipoDiagnostico = ref('')
const razonamientoClinico = ref('')

watch(() => props.diagnosticoGlobal, (nuevo) => {
  if (nuevo && !busquedaCie.value) {
    busquedaCie.value = nuevo
  }
})

watch(busquedaCie, (val) => {
  if (val) emit('actualizarDiagnostico', val)
})

watch(() => props.pacienteSeleccionado, (nuevoP) => {
  if (nuevoP?.signos) {
    signos.value.ta = nuevoP.signos.pa || ''
    signos.value.fc = nuevoP.signos.fc || ''
    signos.value.temp = nuevoP.signos.temp || ''
    signos.value.spo2 = nuevoP.signos.spo2 || ''
  }
  if (nuevoP?.id) {
    pacienteVinculadoId.value = nuevoP.id
  }
})

onMounted(() => {
  if (props.pacienteSeleccionado?.signos) {
    signos.value.ta = props.pacienteSeleccionado.signos.pa || ''
    signos.value.fc = props.pacienteSeleccionado.signos.fc || ''
    signos.value.temp = props.pacienteSeleccionado.signos.temp || ''
    signos.value.spo2 = props.pacienteSeleccionado.signos.spo2 || ''
  }
})

const pronosticoVida = ref<'Bueno' | 'Reservado' | 'Malo' | ''>('')
const pronosticoFuncion = ref<'Bueno' | 'Reservado' | 'Malo' | ''>('')
const indicacionesMedicas = ref('')
const planSeguimiento = ref('')

// Aplicar plantilla rápida
const aplicarPlantilla = (tipo: string) => {
  if (tipo === 'estable') {
    anamnesis.value = 'Paciente consciente, orientado en sus tres esferas neurológicas, refiere mejoría sintomática progresiva con el esquema terapéutico administrado en box.'
    signos.value = { ta: '120/80', fc: '75', fr: '18', temp: '36.6', spo2: '98', glucosa: '105', eva: '1/10', glasgow: '15/15' }
    exploracionSegmentaria.value = 'Habitus exterior con adecuada coloración de tegumentos y mucosas. Tórax simétrico con murmullo vesicular conservado sin estertores. Abdomen blando, no doloroso, peristalsis presente.'
    tipoDiagnostico.value = 'Evolución Favorable'
    pronosticoVida.value = 'Bueno'
    pronosticoFuncion.value = 'Bueno'
  } else if (tipo === 'toracico') {
    anamnesis.value = 'Refiere inicio súbito de dolor precordial opresivo sin irradiación con EVA 7/10 en reposo. Niega diaforesis o cortejo vegetativo en el momento de la revaloración.'
    signos.value = { ta: '135/85', fc: '88', fr: '20', temp: '36.7', spo2: '97', glucosa: '110', eva: '7/10', glasgow: '15/15' }
    exploracionSegmentaria.value = 'Tórax normolíneo, ruidos cardiacos rítmicos sin soplos audibles. Campos pulmonares bien ventilados. Abdomen sin alteraciones.'
    pronosticoVida.value = 'Reservado'
    pronosticoFuncion.value = 'Reservado'
  } else if (tipo === 'abdomen') {
    anamnesis.value = 'Dolor en hemiabdomen inferior de intensidad moderada, acompañado de náuseas sin vómito. Niega fiebre.'
    signos.value = { ta: '115/75', fc: '82', fr: '18', temp: '37.1', spo2: '98', glucosa: '95', eva: '5/10', glasgow: '15/15' }
    exploracionSegmentaria.value = 'Abdomen semigloboso a expensas de panículo adiposo, doloroso a la palpación media y profunda, sin datos francos de irritación peritoneal al momento.'
    pronosticoVida.value = 'Reservado'
    pronosticoFuncion.value = 'Bueno'
  } else if (tipo === 'trauma') {
    anamnesis.value = 'Contusión directa por mecanismo mecánico de baja energía. Dolor localizado y limitación funcional leve.'
    signos.value = { ta: '120/80', fc: '76', fr: '16', temp: '36.5', spo2: '99', glucosa: '90', eva: '4/10', glasgow: '15/15' }
    exploracionSegmentaria.value = 'Edema localizado sin solución de continuidad cutánea, arcos de movilidad conservados con molestia a la carga.'
    pronosticoVida.value = 'Bueno'
    pronosticoFuncion.value = 'Bueno'
  }
}

const vincularMonitor = () => {
  signos.value = {
    ta: '120/80',
    fc: '75',
    fr: '18',
    temp: '36.6',
    spo2: '98',
    glucosa: '105',
    eva: '0-10',
    glasgow: '15/15'
  }
}

const onGuardarFirmar = () => {
  emit('guardarYFirmar')
  alert('Nota Médica ECE guardada y firmada digitalmente con e.firma (NOM-004-SSA3-2012).')
}

const onGuardarBorrador = () => {
  emit('guardarBorrador')
  alert('Borrador de nota médica guardado localmente.')
}

const descartarNota = () => {
  if (confirm('¿Desea limpiar los campos de la nota en borrador?')) {
    anamnesis.value = ''
    signos.value = { ta: '', fc: '', fr: '', temp: '', spo2: '', glucosa: '', eva: '', glasgow: '' }
    exploracionSegmentaria.value = ''
    busquedaCie.value = ''
    tipoDiagnostico.value = ''
    razonamientoClinico.value = ''
    pronosticoVida.value = ''
    pronosticoFuncion.value = ''
    indicacionesMedicas.value = ''
    planSeguimiento.value = ''
  }
}
</script>

<template>
  <div class="nota-ece-view">
    <!-- SUB-ENCABEZADO / BREADCRUMBS -->
    <div class="nota-subnav">
      <div class="subnav-left">
        <span class="breadcrumb-trail">Atención clínica / Expediente Clínico Electrónico /</span>
        <strong class="breadcrumb-current">• Nueva Nota Médica de Evolución</strong>
      </div>
      <div class="subnav-right">
        <span class="tag-legal">Marco Jurídico: NOM-004-SSA3-2012</span>
        <span class="tag-clock">🕒 19 oct 2026 · 19:55 h</span>
      </div>
    </div>

    <!-- BARRA SUPERIOR DE SELECTOR DE PACIENTE Y TIPO DE NOTA -->
    <div class="nota-top-bar">
      <!-- Muestra paciente conectado sin duplicar buscador si ya está en sesión -->
      <div v-if="pacienteSeleccionado" class="patient-connected-pill">
        <span class="avatar-pill">{{ pacienteSeleccionado.nombre.split(' ').map(n => n[0]).slice(0, 2).join('') }}</span>
        <div class="connected-info">
          <strong>{{ pacienteSeleccionado.nombre }}</strong>
          <span>ECE {{ pacienteSeleccionado.ece }} · {{ pacienteSeleccionado.edad }} años · {{ pacienteSeleccionado.box }}</span>
        </div>
        <span class="badge-active-triage" :style="{ backgroundColor: pacienteSeleccionado.triageColor }">
          {{ pacienteSeleccionado.triage.split('·')[0]?.trim() || pacienteSeleccionado.triage }}
        </span>
        <button class="btn-change-patient-mini" @click="$emit('deseleccionarPaciente')" title="Cambiar paciente">✕</button>
      </div>

      <div v-else class="patient-selector-group">
        <button class="btn-qr" title="Escanear pulsera o código QR">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M3 3h8v8H3zm2 2v4h4V5zm8-2h8v8h-8zm2 2v4h4V5zM3 13h8v8H3zm2 2v4h4v-4zm13-2h3v3h-3zm-5 0h3v3h-3zm2 5h3v3h-3zm3 0h3v3h-3z"/>
          </svg>
          <span>QR</span>
        </button>
        <div class="select-wrapper">
          <select v-model="pacienteVinculadoId" class="select-patient">
            <option value="">Seleccione de la cola lateral ({{ pacientesCola?.length || 0 }} asignados)</option>
            <option v-for="p in pacientesCola" :key="p.id" :value="p.id">
              {{ p.nombre }} · ECE {{ p.ece }} ({{ p.folio }}) - {{ p.box }}
            </option>
          </select>
        </div>
      </div>

      <div class="note-type-group">
        <label>TIPO DE NOTA OFICIAL</label>
        <select v-model="tipoNota" class="select-styled">
          <option value="Nota de Evolución / Urgencias">Nota de Evolución / Urgencias</option>
          <option value="Nota de Ingreso a Urgencias">Nota de Ingreso a Urgencias</option>
          <option value="Nota de Interconsulta">Nota de Interconsulta</option>
          <option value="Nota de Egreso / Alta">Nota de Egreso / Alta</option>
        </select>
      </div>

      <div class="doctor-badge-compact">
        <div class="avatar-cm">CM</div>
        <div class="doctor-meta">
          <strong>{{ doctor.nombre }}</strong>
          <span>Médico Urgenciólogo · {{ doctor.cedula }}</span>
        </div>
      </div>
    </div>

    <!-- GRID PRINCIPAL DE 2 COLUMNAS (SOAP IZQUIERDA + ACCIONES / REQUISITOS DERECHA) -->
    <div class="nota-grid">
      
      <!-- COLUMNA IZQUIERDA: FORMULARIO SOAP ESTRUCTURADO -->
      <div class="soap-column">
        
        <!-- S: SUBJETIVO -->
        <section class="soap-card">
          <div class="soap-header">
            <div class="soap-title-group">
              <span class="soap-badge badge-s">S</span>
              <div>
                <h4>Subjetivo · Motivo de Consulta y Padecimiento Actual</h4>
                <p>Manifestaciones y sintomatología relatada por el paciente o familiar responsable</p>
              </div>
            </div>
            <span class="badge-nom">Obligatorio NOM</span>
          </div>

          <div class="soap-body">
            <label class="field-label">Anamnesis, Semiología y Cronología:</label>
            <textarea
              v-model="anamnesis"
              class="textarea-clean"
              rows="4"
              placeholder="Describa el motivo de consulta referido por el paciente, semiología del dolor (inicio, localización, intensidad ALICIA), tiempo de evolución, desencadenantes y síntomas asociados..."
            ></textarea>
          </div>
        </section>

        <!-- O: OBJETIVO -->
        <section class="soap-card">
          <div class="soap-header">
            <div class="soap-title-group">
              <span class="soap-badge badge-o">O</span>
              <div>
                <h4>Objetivo · Signos Vitales y Exploración Física</h4>
                <p>Hallazgos clínicos comprobables, telemetría y examen cefalocaudal</p>
              </div>
            </div>
            <span class="badge-nom">Obligatorio NOM</span>
          </div>

          <div class="soap-body">
            <div class="vitals-header-row">
              <span class="vitals-title">CAPTURA LIMPIA DE SIGNOS VITALES</span>
              <button class="btn-link-telemetry" @click="vincularMonitor" type="button">
                📡 Vincular monitor de cabecera
              </button>
            </div>

            <!-- GRID DE 8 SIGNOS VITALES -->
            <div class="vitals-grid-8">
              <div class="vital-tile">
                <span class="vital-name">T. ARTERIAL</span>
                <input v-model="signos.ta" type="text" placeholder="120/80" class="vital-input" />
                <span class="vital-unit">mmHg</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">FREC. CARDÍACA</span>
                <input v-model="signos.fc" type="text" placeholder="75" class="vital-input" />
                <span class="vital-unit">lpm</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">FREC. RESPIRATORIA</span>
                <input v-model="signos.fr" type="text" placeholder="18" class="vital-input" />
                <span class="vital-unit">rpm</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">TEMPERATURA</span>
                <input v-model="signos.temp" type="text" placeholder="36.6" class="vital-input" />
                <span class="vital-unit">°C</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">SATURACIÓN O₂</span>
                <input v-model="signos.spo2" type="text" placeholder="98" class="vital-input" />
                <span class="vital-unit">% SpO2</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">GLUCOSA CAPILAR</span>
                <input v-model="signos.glucosa" type="text" placeholder="105" class="vital-input" />
                <span class="vital-unit">mg/dL</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">ESCALA EVA</span>
                <input v-model="signos.eva" type="text" placeholder="0-10" class="vital-input" />
                <span class="vital-unit">Escala 0/10</span>
              </div>
              <div class="vital-tile">
                <span class="vital-name">GLASGOW / AVDI</span>
                <input v-model="signos.glasgow" type="text" placeholder="15/15" class="vital-input" />
                <span class="vital-unit">Puntos / Estado</span>
              </div>
            </div>

            <div class="mt-4">
              <label class="field-label">Exploración Física Segmentaria:</label>
              <textarea
                v-model="exploracionSegmentaria"
                class="textarea-clean"
                rows="4"
                placeholder="Describa los hallazgos por segmentos anatómicos: Habitus exterior (facies, biotipo, marcha), Cabeza y cuello (pupilas, mucosa oral, adenomegalias), Tórax y cardiopulmonar (movimientos de amplexión, ruidos respiratorios y cardiacos), Abdomen (blando, depresible, peristalsis, megalias, puntos dolorosos), Extremidades (pulsos periféricos, llenado capilar, edemas), Estado neurológico..."
              ></textarea>
            </div>
          </div>
        </section>

        <!-- A: ANÁLISIS -->
        <section class="soap-card">
          <div class="soap-header">
            <div class="soap-title-group">
              <span class="soap-badge badge-a">A</span>
              <div>
                <h4>Análisis · Diagnóstico Clínico y Codificación CIE</h4>
                <p>Impresión diagnóstica, análisis nosológico y codificación estandarizada</p>
              </div>
            </div>
            <span class="badge-nom">Obligatorio NOM</span>
          </div>

          <div class="soap-body">
            <div class="cie-row">
              <div class="cie-search-group">
                <label class="field-label">Búsqueda de Catálogo CIE-10 / CIE-11:</label>
                <div class="input-with-icon">
                  <span class="icon-search">🔍</span>
                  <input
                    v-model="busquedaCie"
                    type="text"
                    class="input-styled-clean"
                    placeholder="Buscar diagnóstico por código CIE o descripción (ej. J00, I10)..."
                  />
                </div>
              </div>
              <div class="cie-type-group">
                <label class="field-label">Tipo de Diagnóstico:</label>
                <select v-model="tipoDiagnostico" class="select-styled">
                  <option value="">Seleccione categoría...</option>
                  <option value="Principal">Principal / Ingreso</option>
                  <option value="Secundario">Secundario / Comorbilidad</option>
                  <option value="Presuntivo">Presuntivo / Sospecha</option>
                  <option value="Confirmado">Confirmado</option>
                </select>
              </div>
            </div>

            <div class="mt-4">
              <label class="field-label">Razonamiento Clínico y Juicio Médico:</label>
              <textarea
                v-model="razonamientoClinico"
                class="textarea-clean"
                rows="3"
                placeholder="Razonamiento clínico y correlación fisiopatológica, evolución del cuadro respecto al ingreso, interpretación de estudios complementarios y diagnósticos diferenciales considerados..."
              ></textarea>
            </div>
          </div>
        </section>

        <!-- P: PLAN -->
        <section class="soap-card">
          <div class="soap-header">
            <div class="soap-title-group">
              <span class="soap-badge badge-p">P</span>
              <div>
                <h4>Plan · Conducta Terapéutica, Indicaciones y Pronóstico</h4>
                <p>Directivas médicas, plan farmacológico/no farmacológico y pronóstico</p>
              </div>
            </div>
            <span class="badge-nom">Obligatorio NOM</span>
          </div>

          <div class="soap-body">
            <!-- PRONÓSTICO PARA LA VIDA Y LA FUNCIÓN -->
            <div class="prognosis-row">
              <div class="prognosis-item">
                <label class="field-label">Pronóstico para la Vida:</label>
                <div class="radio-group">
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoVida" value="Bueno" />
                    <span>Bueno</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoVida" value="Reservado" />
                    <span>Reservado</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoVida" value="Malo" />
                    <span>Malo</span>
                  </label>
                </div>
              </div>

              <div class="prognosis-item">
                <label class="field-label">Pronóstico para la Función:</label>
                <div class="radio-group">
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoFuncion" value="Bueno" />
                    <span>Bueno</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoFuncion" value="Reservado" />
                    <span>Reservado</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="pronosticoFuncion" value="Malo" />
                    <span>Malo</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="mt-4">
              <label class="field-label">Indicaciones Médicas, Cuidados, Dieta y Reposo:</label>
              <textarea
                v-model="indicacionesMedicas"
                class="textarea-clean"
                rows="3"
                placeholder="Especifique plan general, tipo de dieta autorizada, reposo relativo/absoluto, cuidados generales de enfermería, soluciones intravenosas e indicaciones directas..."
              ></textarea>
            </div>

            <div class="mt-4">
              <label class="field-label">Plan de Seguimiento y Datos de Alarma Explicados:</label>
              <textarea
                v-model="planSeguimiento"
                class="textarea-clean"
                rows="3"
                placeholder="Describa los signos de alarma informados al paciente/familiar (ej. fiebre persistente, disnea, dolor refractario) y cita de revaloración clínica..."
              ></textarea>
            </div>
          </div>
        </section>

      </div>

      <!-- COLUMNA DERECHA: ESTADO, FIRMA Y PLANTILLAS RÁPIDAS -->
      <aside class="right-audit-column">
        
        <!-- ESTADO DEL DOCUMENTO -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>ESTADO DEL DOCUMENTO</h4>
            <span class="pill-draft">● Nota en Borrador</span>
          </div>

          <div class="normative-checklist">
            <div class="checklist-title">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
              <strong>REQUISITOS NORMATIVOS (NOM-004)</strong>
            </div>

            <ul class="checklist-items">
              <li class="check-item">
                <div class="check-left">
                  <span class="circle-check" :class="{ done: !!pacienteVinculadoId }"></span>
                  <span>Identificación de paciente vinculada</span>
                </div>
                <span class="status-tag" :class="pacienteVinculadoId ? 'tag-green' : 'tag-orange'">
                  {{ pacienteVinculadoId ? 'Completado' : 'Pendiente' }}
                </span>
              </li>
              <li class="check-item">
                <div class="check-left">
                  <span class="circle-check" :class="{ done: !!exploracionSegmentaria || !!signos.ta }"></span>
                  <span>Exploración física asentada</span>
                </div>
                <span class="status-tag" :class="(exploracionSegmentaria || signos.ta) ? 'tag-green' : 'tag-gray'">
                  {{ (exploracionSegmentaria || signos.ta) ? 'Asentada' : 'Sin capturar' }}
                </span>
              </li>
              <li class="check-item">
                <div class="check-left">
                  <span class="circle-check" :class="{ done: !!busquedaCie }"></span>
                  <span>Diagnóstico CIE codificado</span>
                </div>
                <span class="status-tag" :class="busquedaCie ? 'tag-green' : 'tag-gray'">
                  {{ busquedaCie ? 'Codificado' : 'Sin capturar' }}
                </span>
              </li>
              <li class="check-item">
                <div class="check-left">
                  <span class="circle-check" :class="{ done: !!pronosticoVida && !!pronosticoFuncion }"></span>
                  <span>Pronóstico para vida y función</span>
                </div>
                <span class="status-tag" :class="(pronosticoVida && pronosticoFuncion) ? 'tag-green' : 'tag-gray'">
                  {{ (pronosticoVida && pronosticoFuncion) ? 'Definido' : 'Sin seleccionar' }}
                </span>
              </li>
            </ul>
          </div>

          <!-- FIRMA ELECTRÓNICA AVANZADA COMPACTA -->
          <div class="efirma-box-compact">
            <div class="badge-cert">
              <span>🔒 e.firma Activa · Cédula {{ doctor.cedula }}</span>
            </div>
          </div>

          <!-- BOTONES DE ACCIÓN -->
          <div class="action-buttons-group">
            <button class="btn-firmar-nota" @click="onGuardarFirmar">
              <span class="btn-icon">✍️</span>
              <span>Guardar y Firmar Nota ECE [Ctrl+Enter]</span>
            </button>

            <div class="row-2-btn">
              <button class="btn-draft" @click="onGuardarBorrador">
                💾 Guardar Borrador
              </button>
              <button class="btn-discard" @click="descartarNota">
                🗑️ Descartar
              </button>
            </div>
          </div>

          <div class="legal-footnote-compact">
            <span>🛡️ NOM-004-SSA3-2012 · Trazabilidad Inalterable</span>
          </div>
        </div>

        <!-- PLANTILLAS RÁPIDAS DE URGENCIAS -->
        <div class="side-card">
          <div class="templates-header">
            <h4>PLANTILLAS RÁPIDAS DE URGENCIAS</h4>
          </div>
          <div class="templates-grid">
            <button class="btn-template" @click="aplicarPlantilla('estable')">
              + Evolución Estable
            </button>
            <button class="btn-template" @click="aplicarPlantilla('toracico')">
              + Dolor Torácico
            </button>
            <button class="btn-template" @click="aplicarPlantilla('abdomen')">
              + Abdomen Agudo
            </button>
            <button class="btn-template" @click="aplicarPlantilla('trauma')">
              + Trauma Menor
            </button>
          </div>
        </div>

      </aside>

    </div>
  </div>
</template>

<style scoped>
.nota-ece-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
}

/* SUBNAV / BREADCRUMBS */
.nota-subnav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #FFFFFF;
  padding: 0.6rem 1rem;
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
}

.subnav-right {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.tag-legal {
  color: #0F4C81;
  font-weight: 700;
  font-size: 0.72rem;
}

.tag-clock {
  color: #2563EB;
  font-family: monospace;
  font-size: 0.72rem;
  font-weight: 600;
}

/* BARRA SUPERIOR DE SELECTORES */
.nota-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.75rem 1rem;
  flex-wrap: wrap;
}

.patient-connected-pill {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.35rem 0.65rem;
}

.avatar-pill {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background-color: #0F4C81;
  color: #FFFFFF;
  font-size: 0.68rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.connected-info strong {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
  line-height: 1.1;
}

.connected-info span {
  font-size: 0.65rem;
  color: #64748B;
}

.badge-active-triage {
  color: #FFFFFF;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.btn-change-patient-mini {
  background: none;
  border: none;
  color: #94A3B8;
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0 0.2rem;
}

.btn-change-patient-mini:hover {
  color: #DC2626;
}

.efirma-box-compact {
  margin-bottom: 0.8rem;
}

.legal-footnote-compact {
  text-align: center;
  font-size: 0.65rem;
  color: #94A3B8;
  margin-top: 0.4rem;
}

.patient-selector-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 280px;
}

.btn-qr {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  color: #1E293B;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-qr:hover {
  background-color: #E2E8F0;
}

.select-wrapper {
  flex: 1;
}

.select-patient {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8rem;
  color: #1E293B;
  background-color: #FFFFFF;
  outline: none;
}

.note-type-group {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.note-type-group label {
  font-size: 0.68rem;
  font-weight: 700;
  color: #64748B;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.select-styled {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8rem;
  color: #1E293B;
  background-color: #FFFFFF;
  outline: none;
}

.doctor-badge-compact {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.avatar-cm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #0F172A;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.doctor-meta strong {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
}

.doctor-meta span {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
}

/* GRID PRINCIPAL */
.nota-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

/* CARDS SOAP */
.soap-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.soap-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.soap-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.8rem;
}

.soap-title-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.soap-badge {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 0.95rem;
  color: #FFFFFF;
}

.badge-s { background-color: #1E293B; }
.badge-o { background-color: #2563EB; }
.badge-a { background-color: #0F172A; }
.badge-p { background-color: #0284C7; }

.soap-title-group h4 {
  font-size: 0.92rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.soap-title-group p {
  font-size: 0.7rem;
  color: #64748B;
  margin: 0.15rem 0 0 0;
}

.badge-nom {
  background-color: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.soap-body {
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  margin-bottom: 0.35rem;
  display: block;
}

.textarea-clean {
  width: 100%;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.65rem 0.75rem;
  font-size: 0.82rem;
  line-height: 1.45;
  color: #1E293B;
  background-color: #F8FAFC;
  outline: none;
  font-family: inherit;
  resize: vertical;
}

.textarea-clean:focus {
  border-color: #3B82F6;
  background-color: #FFFFFF;
}

/* SIGNOS VITALES EN GRID DE 8 */
.vitals-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.vitals-title {
  font-size: 0.68rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.04em;
}

.btn-link-telemetry {
  background: none;
  border: none;
  color: #2563EB;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.btn-link-telemetry:hover {
  text-decoration: underline;
}

.vitals-grid-8 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.6rem;
  background-color: #F8FAFC;
  padding: 0.75rem;
  border-radius: 8px;
  border: 1px solid #EDF2F7;
}

.vital-tile {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.6rem;
  display: flex;
  flex-direction: column;
}

.vital-name {
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748B;
  margin-bottom: 0.15rem;
}

.vital-input {
  border: none;
  font-size: 1.1rem;
  font-weight: 800;
  color: #1E293B;
  outline: none;
  width: 100%;
  padding: 0;
  background: transparent;
}

.vital-unit {
  font-size: 0.65rem;
  color: #94A3B8;
  margin-top: 0.1rem;
}

/* CIE ROW */
.cie-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0.8rem;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.icon-search {
  position: absolute;
  left: 0.6rem;
  font-size: 0.8rem;
  color: #94A3B8;
}

.input-styled-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem 0.45rem 2rem;
  font-size: 0.8rem;
  outline: none;
  color: #1E293B;
}

/* PRONÓSTICO */
.prognosis-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  background-color: #F8FAFC;
  padding: 0.75rem;
  border-radius: 8px;
  border: 1px solid #EDF2F7;
}

.radio-group {
  display: flex;
  gap: 0.8rem;
  margin-top: 0.3rem;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: #334155;
  cursor: pointer;
}

.mt-4 {
  margin-top: 0.8rem;
}

/* COLUMNA DERECHA */
.right-audit-column {
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

.side-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
}

.side-card-header h4 {
  font-size: 0.75rem;
  font-weight: 800;
  color: #475569;
  margin: 0;
}

.pill-draft {
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
}

.normative-checklist {
  border-top: 1px solid #F1F5F9;
  padding-top: 0.75rem;
  margin-bottom: 0.8rem;
}

.checklist-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.68rem;
  color: #0F4C81;
  margin-bottom: 0.6rem;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.check-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.7rem;
  color: #334155;
}

.check-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.circle-check {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1.5px solid #CBD5E1;
}

.circle-check.done {
  background-color: #10B981;
  border-color: #10B981;
}

.status-tag {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.tag-orange {
  background-color: #FFF7ED;
  color: #EA580C;
}

.tag-gray {
  background-color: #F1F5F9;
  color: #64748B;
}

.tag-green {
  background-color: #ECFDF5;
  color: #059669;
}

.efirma-box {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 0.75rem;
  margin-bottom: 0.8rem;
}

.efirma-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.65rem;
  font-weight: 800;
  color: #475569;
  margin-bottom: 0.3rem;
}

.doctor-title {
  display: block;
  font-size: 0.82rem;
  color: #1E293B;
}

.doctor-sub {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.2rem 0 0.5rem 0;
  line-height: 1.3;
}

.badge-cert {
  background-color: #ECFDF5;
  border: 1px solid #A7F3D0;
  color: #047857;
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.25rem 0.45rem;
  border-radius: 4px;
}

.action-buttons-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.btn-firmar-nota {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-firmar-nota:hover {
  background-color: #0d3d66;
}

.row-2-btn {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
}

.btn-draft, .btn-discard {
  border: 1px solid #CBD5E1;
  background-color: #FFFFFF;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.45rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-draft:hover, .btn-discard:hover {
  background-color: #F1F5F9;
}

.legal-footnote {
  display: flex;
  gap: 0.4rem;
  font-size: 0.65rem;
  color: #94A3B8;
  line-height: 1.3;
}

.shield-icon {
  font-size: 0.85rem;
}

.legal-footnote p {
  margin: 0;
}

.templates-header h4 {
  font-size: 0.72rem;
  font-weight: 800;
  color: #475569;
  margin: 0 0 0.6rem 0;
}

.templates-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
}

.btn-template {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.5rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: #1E293B;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
}

.btn-template:hover {
  background-color: #EFF6FF;
  border-color: #3B82F6;
  color: #1D4ED8;
}

@media (max-width: 1100px) {
  .nota-grid {
    grid-template-columns: 1fr;
  }
  .vitals-grid-8 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
