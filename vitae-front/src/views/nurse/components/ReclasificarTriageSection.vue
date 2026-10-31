<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  enfermera: {
    nombre: string
    cedula: string
    servicio: string
  }
  pacienteActivo?: {
    nombre: string
    edad: number | null
    sexo?: string
    folio?: string
    box?: string
    triage?: string
    triageColor?: string
    tiempoEspera?: number
    signos?: {
      paSistolica: string
      paDiastolica: string
      fc: string
      fr: string
      spo2: string
      temp: string
      evaDolor: number | string
    }
  }
}>()

const emit = defineEmits<{
  (e: 'activarChoque'): void
  (e: 'actualizarReclasificacion', datos: any): void
}>()

// --- ESTADO PARA RECLASIFICACIÓN DE TRIAGE ---
const reevaluacionForm = ref({
  paSistolica: '',
  paDiastolica: '',
  fc: '',
  fr: '',
  spo2: '',
  temp: '',
  evaDolor: '',
  motivoDeterioro: 'hemodinamico' as 'hemodinamico' | 'dolor' | 'respiratorio',
  viaAereaComprometida: false,
  signosHipoperfusion: false,
  dolorToracicoIsquemico: false,
  alteracionConciencia: false,
  justificacionClinica: ''
})

// Trazabilidad de reclasificaciones
const reclasificacionesHistoricas = ref<Array<{
  hora: string
  paciente: string
  transicion: string
  motivo: string
  destino: string
  enfermero: string
}>>([])

const copiarSignosPrevios = () => {
  if (props.pacienteActivo?.signos) {
    const s = props.pacienteActivo.signos
    reevaluacionForm.value.paSistolica = s.paSistolica || ''
    reevaluacionForm.value.paDiastolica = s.paDiastolica || ''
    reevaluacionForm.value.fc = s.fc || ''
    reevaluacionForm.value.fr = s.fr || ''
    reevaluacionForm.value.spo2 = s.spo2 || ''
    reevaluacionForm.value.temp = s.temp || ''
    reevaluacionForm.value.evaDolor = String(s.evaDolor || '')
  }
}

// Cálculo del nuevo nivel
const nuevoNivelCalculado = computed(() => {
  if (reevaluacionForm.value.alteracionConciencia || reevaluacionForm.value.viaAereaComprometida) {
    return {
      titulo: '● NIVEL I · Reanimación',
      color: '#EF4444',
      badge: 'red',
      tiempo: 'INMEDIATO (0 min)',
      destino: 'Sala de Choque Inmediato (Box 01)'
    }
  }
  if (reevaluacionForm.value.signosHipoperfusion || reevaluacionForm.value.dolorToracicoIsquemico || Number(reevaluacionForm.value.evaDolor) >= 8) {
    return {
      titulo: '● NIVEL II · Muy Urgente',
      color: '#EA580C',
      badge: 'orange',
      tiempo: '≤ 10 - 15 MINUTOS',
      destino: 'Box de Urgencias Críticas 01'
    }
  }
  return {
    titulo: props.pacienteActivo?.triage ? `● ${props.pacienteActivo.triage}` : '● NIVEL III · Urgente',
    color: '#D97706',
    badge: 'yellow',
    tiempo: '≤ 60 MINUTOS',
    destino: props.pacienteActivo?.box || 'Consultorio Filtro 02'
  }
})

const confirmarReclasificacion = () => {
  if (!reevaluacionForm.value.justificacionClinica) {
    alert('NORMA MÉDICA NOM-027 / NOM-024: La justificación clínica es obligatoria para registrar la reclasificación inalterable.')
    return
  }

  const now = new Date()
  const hora = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')} hrs`
  const nombrePaciente = props.pacienteActivo?.nombre || 'Paciente en Espera'

  reclasificacionesHistoricas.value.unshift({
    hora,
    paciente: nombrePaciente,
    transicion: `${props.pacienteActivo?.triage || 'Nivel III'} <svg viewBox="0 0 24 24" class="svg-inline-arrow" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg> ${nuevoNivelCalculado.value.titulo}`,
    motivo: reevaluacionForm.value.justificacionClinica.slice(0, 45) + '...',
    destino: nuevoNivelCalculado.value.destino,
    enfermero: props.enfermera.nombre
  })

  emit('actualizarReclasificacion', {
    nuevoNivel: nuevoNivelCalculado.value.titulo.replace('● ', ''),
    nuevoColor: nuevoNivelCalculado.value.color,
    box: nuevoNivelCalculado.value.destino,
    signos: {
      paSistolica: reevaluacionForm.value.paSistolica,
      paDiastolica: reevaluacionForm.value.paDiastolica,
      fc: reevaluacionForm.value.fc,
      fr: reevaluacionForm.value.fr,
      spo2: reevaluacionForm.value.spo2,
      temp: reevaluacionForm.value.temp,
      evaDolor: reevaluacionForm.value.evaDolor
    }
  })

  alert(`<svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Reclasificación confirmada para: ${nombrePaciente}.\nNueva prioridad: ${nuevoNivelCalculado.value.titulo}.\nDestino reasignado: ${nuevoNivelCalculado.value.destino}.`)
}
</script>

<template>
  <div class="reclasificar-layout-grid">
    <!-- SUBCOLUMNA IZQUIERDA: COMPARATIVA Y PREGUNTAS DISCRIMINADORAS -->
    <div class="reclasificar-form-col">
      <!-- Header de Reclasificación -->
      <div class="reval-header-card">
        <div class="reval-header-top">
          <div>
            <span class="module-tag">RECLASIFICACIÓN DINÁMICA DE TRIAGE</span>
            <span class="protocol-badge">PROTOCOLO MANCHESTER V4.2</span>
            <p class="reval-subtitle">Revaloración de prioridad por deterioro agudo o temporizador SLA de espera excedido.</p>
          </div>
        </div>
        <div class="reval-tags-row">
          <span class="tag-status green">● Sala Choque: <strong>DISPONIBLE (BOX 1)</strong></span>
          <span class="tag-status blue"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> Auditoría Clínico-Legal Activa</span>
          <span class="tag-status gray">Operador: <strong>{{ enfermera.nombre }} [{{ enfermera.cedula }}]</strong></span>
        </div>
      </div>

      <!-- Paciente Seleccionado / Selector de Paciente -->
      <div class="reval-patient-bar">
        <div class="reval-patient-left">
          <small>PACIENTE SELECCIONADO EN SALA DE ESPERA PARA REEVALUACIÓN</small>
          <strong v-if="pacienteActivo?.nombre">
            <svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> {{ pacienteActivo.nombre }} {{ pacienteActivo.edad ? `(${pacienteActivo.edad} años)` : '' }} · {{ pacienteActivo.folio || 'FOL-URG' }}
          </strong>
          <strong v-else class="text-unselected">[ Seleccione paciente de la lista de espera ]</strong>
        </div>
        <div class="reval-sla-alert-box">
          <div class="sla-alert-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></div>
          <div class="sla-alert-text">
            <span class="badge-danger">ALERTA SLA MANCHESTER</span>
            <p>Próximo a tolerancia clínica máxima.</p>
          </div>
          <div class="sla-countdown">
            <strong>{{ pacienteActivo?.tiempoEspera ? `${pacienteActivo.tiempoEspera} min` : '--:--' }}</strong>
            <small>Transcurrido</small>
          </div>
        </div>
      </div>

      <!-- Bloque 1: Comparativa de Telemetría Dinámica -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-activity" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></span>
            <h4>Comparativa de Telemetría Dinámica</h4>
          </div>
          <span class="badge-neutral">Delta Fisiológico en Espera</span>
        </div>

        <div class="telemetry-comparison-grid">
          <!-- Columna: Signos al Ingreso (Histórico) -->
          <div class="historic-signs-card">
            <div class="card-inner-header">
              <strong>SIGNOS AL INGRESO</strong>
              <span class="time-tag">Línea Base</span>
            </div>
            <div class="historic-grid">
              <div class="h-sign">
                <small>P.A.</small>
                <strong>{{ pacienteActivo?.signos?.paSistolica || '--' }}/{{ pacienteActivo?.signos?.paDiastolica || '--' }}</strong>
                <span>mmHg</span>
              </div>
              <div class="h-sign">
                <small>F.C.</small>
                <strong>{{ pacienteActivo?.signos?.fc || '--' }}</strong>
                <span>lpm</span>
              </div>
              <div class="h-sign">
                <small>F.R.</small>
                <strong>{{ pacienteActivo?.signos?.fr || '--' }}</strong>
                <span>rpm</span>
              </div>
              <div class="h-sign">
                <small>SPO2</small>
                <strong>{{ pacienteActivo?.signos?.spo2 || '--' }}%</strong>
                <span>Aire Amb.</span>
              </div>
              <div class="h-sign">
                <small>TEMP</small>
                <strong>{{ pacienteActivo?.signos?.temp || '--' }}</strong>
                <span>°C Axilar</span>
              </div>
              <div class="h-sign">
                <small>EVA DOLOR</small>
                <strong>{{ pacienteActivo?.signos?.evaDolor || '0' }}/10</strong>
                <span>Escala</span>
              </div>
            </div>
          </div>

          <!-- Columna: Captura Actual de Reevaluación -->
          <div class="current-reval-card">
            <div class="card-inner-header blue">
              <div class="reval-card-title-row">
                <strong>● CAPTURA ACTUAL DE REEVALUACIÓN</strong>
                <button type="button" class="btn-copy-history" @click="copiarSignosPrevios" title="Copiar constantes previas como punto de partida">
                  <svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg> Copiar previos
                </button>
              </div>
              <span class="time-tag">Tabulación Rápida</span>
            </div>
            <div class="reval-inputs-grid">
              <div class="input-subgroup">
                <label>PA (mmHg)</label>
                <div class="pa-mini-row">
                  <input v-model="reevaluacionForm.paSistolica" type="text" placeholder="ej. 88" class="input-mini" />
                  <span>/</span>
                  <input v-model="reevaluacionForm.paDiastolica" type="text" placeholder="ej. 54" class="input-mini" />
                </div>
              </div>
              <div class="input-subgroup">
                <label>FC (lpm)</label>
                <input v-model="reevaluacionForm.fc" type="text" placeholder="ej. 118" class="input-mini full" />
              </div>
              <div class="input-subgroup">
                <label>FR (rpm)</label>
                <input v-model="reevaluacionForm.fr" type="text" placeholder="ej. 26" class="input-mini full" />
              </div>
              <div class="input-subgroup">
                <label>SpO2 (%)</label>
                <input v-model="reevaluacionForm.spo2" type="text" placeholder="ej. 93" class="input-mini full" />
              </div>
              <div class="input-subgroup">
                <label>Temp (°C)</label>
                <input v-model="reevaluacionForm.temp" type="text" placeholder="ej. 38.9" class="input-mini full" />
              </div>
              <div class="input-subgroup">
                <label>EVA (0-10)</label>
                <input v-model="reevaluacionForm.evaDolor" type="text" placeholder="ej. 8" class="input-mini full" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Bloque 2: Discriminadores Manchester & Causa de Reevaluación -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-steth" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4"/><circle cx="20" cy="10" r="2"/></svg></span>
            <h4>Discriminadores Manchester & Causa de Reevaluación</h4>
          </div>
          <span class="protocol-link">Protocolo: Cefalea / Dolor Abdominal / Sepsis Potencial</span>
        </div>

        <!-- Motivo principal del deterioro clínico observado -->
        <div class="deterioro-radio-group">
          <span class="group-label">MOTIVO PRINCIPAL DEL DETERIORO CLÍNICO OBSERVADO</span>
          <div class="radio-options-row">
            <label class="radio-card" :class="{ active: reevaluacionForm.motivoDeterioro === 'hemodinamico' }">
              <input type="radio" value="hemodinamico" v-model="reevaluacionForm.motivoDeterioro" />
              <span>Deterioro hemodinámico</span>
            </label>
            <label class="radio-card" :class="{ active: reevaluacionForm.motivoDeterioro === 'dolor' }">
              <input type="radio" value="dolor" v-model="reevaluacionForm.motivoDeterioro" />
              <span>Aumento severo dolor (EVA > 7)</span>
            </label>
            <label class="radio-card" :class="{ active: reevaluacionForm.motivoDeterioro === 'respiratorio' }">
              <input type="radio" value="respiratorio" v-model="reevaluacionForm.motivoDeterioro" />
              <span>Desaturación / Dificultad resp.</span>
            </label>
          </div>
        </div>

        <!-- Preguntas discriminadoras obligatorias -->
        <div class="discriminator-questions">
          <span class="group-label">PREGUNTAS DISCRIMINADORAS MANCHESTER OBLIGATORIAS</span>

          <!-- Pregunta 1 -->
          <div class="question-row">
            <div class="q-text">
              <span class="q-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 4v9"/><path d="M9 13a4 4 0 0 1-5-4V6a3 3 0 0 1 5-2c2 0 2 2 2 4v5"/><path d="M15 13a4 4 0 0 0 5-4V6a3 3 0 0 0-5-2c-2 0-2 2-2 4v5"/><path d="M6 13c0 4.4 2.7 8 6 8s6-3.6 6-8"/></svg></span>
              <span>¿Vía aérea comprometida, estridor laríngeo o sibilancias audibles?</span>
            </div>
            <div class="q-actions">
              <button type="button" class="btn-toggle" :class="{ active: reevaluacionForm.viaAereaComprometida }" @click="reevaluacionForm.viaAereaComprometida = true">Sí</button>
              <button type="button" class="btn-toggle" :class="{ active: !reevaluacionForm.viaAereaComprometida }" @click="reevaluacionForm.viaAereaComprometida = false">No</button>
            </div>
          </div>

          <!-- Pregunta 2 -->
          <div class="question-row highlight-urgent">
            <div class="q-text">
              <span class="q-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/><polyline points="17 18 23 18 23 12"/></svg></span>
              <div>
                <span>¿Signos evidentes de hipoperfusión, palidez extrema o shock compensado?</span>
                <small class="q-hint">PA &lt; 90/60 + Taquicardia &gt; 110 lpm activa umbral de discriminación Nivel II</small>
              </div>
            </div>
            <div class="q-actions">
              <button type="button" class="btn-toggle btn-danger-toggle" :class="{ active: reevaluacionForm.signosHipoperfusion }" @click="reevaluacionForm.signosHipoperfusion = true">Sí</button>
              <button type="button" class="btn-toggle" :class="{ active: !reevaluacionForm.signosHipoperfusion }" @click="reevaluacionForm.signosHipoperfusion = false">No</button>
            </div>
          </div>

          <!-- Pregunta 3 -->
          <div class="question-row">
            <div class="q-text">
              <span class="q-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/><path d="M12 5v4"/></svg></span>
              <span>¿Dolor torácico opresivo de características isquémicas agudas?</span>
            </div>
            <div class="q-actions">
              <button type="button" class="btn-toggle" :class="{ active: reevaluacionForm.dolorToracicoIsquemico }" @click="reevaluacionForm.dolorToracicoIsquemico = true">Sí</button>
              <button type="button" class="btn-toggle" :class="{ active: !reevaluacionForm.dolorToracicoIsquemico }" @click="reevaluacionForm.dolorToracicoIsquemico = false">No</button>
            </div>
          </div>

          <!-- Pregunta 4 -->
          <div class="question-row">
            <div class="q-text">
              <span class="q-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M9.5 2A2.5 2.5 0 0 1 12 4.5v15a2.5 2.5 0 0 1-4.96.44 2.5 2.5 0 0 1-2.96-3.08 3 3 0 0 1-.34-5.58 2.5 2.5 0 0 1 1.32-4.24 2.5 2.5 0 0 1 4.44-5.04z"/><path d="M14.5 2A2.5 2.5 0 0 0 12 4.5v15a2.5 2.5 0 0 0 4.96.44 2.5 2.5 0 0 0 2.96-3.08 3 3 0 0 0 .34-5.58 2.5 2.5 0 0 0-1.32-4.24 2.5 2.5 0 0 0-4.44-5.04z"/></svg></span>
              <span>¿Alteración aguda del estado de conciencia (AVDI / Glasgow &lt; 14) o crisis presenciada?</span>
            </div>
            <div class="q-actions">
              <button type="button" class="btn-toggle" :class="{ active: reevaluacionForm.alteracionConciencia }" @click="reevaluacionForm.alteracionConciencia = true">Sí</button>
              <button type="button" class="btn-toggle" :class="{ active: !reevaluacionForm.alteracionConciencia }" @click="reevaluacionForm.alteracionConciencia = false">No</button>
            </div>
          </div>
        </div>

        <!-- Justificación Clínica Obligatoria -->
        <div class="justificacion-box">
          <div class="just-header">
            <label>JUSTIFICACIÓN CLÍNICA OBLIGATORIA DE RECLASIFICACIÓN (NORMA AUDITORÍA MÉDICA NOM-027 / NOM-024)</label>
            <span class="badge-required">* Campo Requerido</span>
          </div>
          <textarea 
            v-model="reevaluacionForm.justificacionClinica" 
            rows="3" 
            class="input-clinical full" 
            placeholder="Redacte la justificación clínica detallada del deterioro que sustenta el escalamiento o cambio de nivel..."
          ></textarea>
        </div>
      </section>

      <!-- Bloque 3: Trazabilidad de Reclasificaciones del Turno -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
            <h4>Trazabilidad de Reclasificaciones del Turno en Curso</h4>
          </div>
          <div class="box-actions-right">
            <span class="shift-tag">Guardia A · Turno Diurno</span>
            <button type="button" class="btn-clean"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg> Exportar Bitácora FHIR</button>
          </div>
        </div>

        <div class="table-container">
          <table class="triage-records-table">
            <thead>
              <tr>
                <th>HORA REEVAL.</th>
                <th>PACIENTE / HC</th>
                <th>TRANSICIÓN PRIORIDAD</th>
                <th>MOTIVO CLÍNICO PRINCIPAL</th>
                <th>DESTINO ACTUALIZADO</th>
                <th>ENFERMERO RESPONSABLE</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="reclasificacionesHistoricas.length === 0" class="empty-table-row">
                <td colspan="6">
                  <div class="empty-table-msg">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-folder" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg> Sin eventos de reclasificación registrados en este turno</span>
                    <small>Cada escalamiento aprobado generará un registro sellado en la bitácora clínica.</small>
                  </div>
                </td>
              </tr>
              <tr v-for="(rec, idx) in reclasificacionesHistoricas" :key="idx" class="classified-row">
                <td><strong class="time-text">{{ rec.hora }}</strong></td>
                <td><span class="patient-name-cell">{{ rec.paciente }}</span></td>
                <td><span class="manchester-pill orange">{{ rec.transicion }}</span></td>
                <td><small>{{ rec.motivo }}</small></td>
                <td><span class="dest-badge">{{ rec.destino }}</span></td>
                <td><small>{{ rec.enfermero }}</small></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- SUBCOLUMNA DERECHA: RECÁLCULO MANCHESTER Y ACCIÓN -->
    <aside class="reval-decision-col">
      <div class="decision-card">
        <div class="decision-header">
          <span class="decision-icon"><svg viewBox="0 0 24 24" class="svg-inline-chart" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg></span>
          <h4>Recálculo Manchester</h4>
        </div>

        <div class="prior-priority-badge">
          <small>PRIORIDAD ANTERIOR ASIGNADA</small>
          <div class="badge-yellow">
            ● {{ pacienteActivo?.triage || 'NIVEL III - Urgente' }} 
            <small>SLA &lt; 60 min</small>
          </div>
          <span>Destino actual: {{ pacienteActivo?.box || 'Sala de Espera' }}</span>
        </div>

        <div class="down-arrow-indicator"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg></div>

        <!-- Nuevo Nivel Calculado -->
        <div class="new-level-card" :class="nuevoNivelCalculado.badge">
          <div class="new-level-tag">
            <small>NUEVO NIVEL CALCULADO</small>
            <span class="badge-escalate">EVALUACIÓN REACTIVA</span>
          </div>
          <h3 :class="`level-title-${nuevoNivelCalculado.badge}`">{{ nuevoNivelCalculado.titulo }}</h3>
          <div class="sla-reval-footer">
            <span>Tiempo Máximo Atención:</span>
            <strong>{{ nuevoNivelCalculado.tiempo }}</strong>
          </div>
        </div>

        <!-- Reasignación Física Inmediata -->
        <div class="reassignment-card">
          <div class="reassign-title">
            <span class="bed-icon"><svg viewBox="0 0 24 24" class="svg-inline-hospital" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><path d="M9 22v-4h6v4"/><line x1="12" y1="6" x2="12" y2="10"/><line x1="10" y1="8" x2="14" y2="8"/></svg></span>
            <strong>DESTINO SUGERIDO:</strong>
          </div>
          <h4>{{ nuevoNivelCalculado.destino }}</h4>
          <small>Notificación automatizada a Médico de Guardia y camillero en curso.</small>
        </div>

        <!-- Botones de Acción -->
        <button type="button" class="btn-save-triage" @click="confirmarReclasificacion">
          <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Confirmar Reclasificación y Reordenar Turno [Enter]
        </button>

        <button type="button" class="btn-direct-choque" @click="emit('activarChoque')">
          <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Escalar Directo a Sala de Choque (Nivel I) [Alt+C]
        </button>

        <button type="button" class="btn-subtle full-w">
          <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg> Mantener Clasificación y Registrar Control de Espera
        </button>

        <!-- Estado de Turno Actual -->
        <div class="turno-summary-grid">
          <div class="summary-stat-box">
            <small>Pacientes Sala</small>
            <strong>0</strong>
          </div>
          <div class="summary-stat-box">
            <small>Reclasificados</small>
            <strong>0</strong>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.reclasificar-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

.reclasificar-form-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.reval-header-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.module-tag {
  font-size: 0.65rem;
  font-weight: 800;
  color: #2563EB;
  letter-spacing: 0.05em;
  display: inline-block;
}

.protocol-badge {
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  margin-left: 0.5rem;
}

.reval-subtitle {
  font-size: 0.78rem;
  color: #64748B;
  margin: 0.3rem 0 0.6rem 0;
}

.reval-tags-row {
  display: flex;
  gap: 0.8rem;
  font-size: 0.72rem;
  flex-wrap: wrap;
}

.tag-status {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
}

.tag-status.green { border-color: #A7F3D0; background-color: #ECFDF5; color: #065F46; }
.tag-status.blue { border-color: #BFDBFE; background-color: #EFF6FF; color: #1E40AF; }
.tag-status.gray { color: #64748B; }

.reval-patient-bar {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem 1.1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.reval-patient-left small {
  display: block;
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748B;
}

.reval-patient-left strong {
  font-size: 0.9rem;
  color: #1E293B;
}

.reval-sla-alert-box {
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 8px;
  padding: 0.4rem 0.8rem;
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.sla-alert-icon {
  font-size: 1.4rem;
}

.sla-alert-text p {
  margin: 0;
  font-size: 0.65rem;
  color: #991B1B;
}

.badge-danger {
  background-color: #DC2626;
  color: #FFFFFF;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.sla-countdown strong {
  font-size: 1.1rem;
  font-weight: 900;
  color: #DC2626;
  font-family: monospace;
  display: block;
}

.sla-countdown small {
  font-size: 0.62rem;
  color: #991B1B;
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

.badge-neutral {
  background-color: #E2E8F0;
  color: #475569;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.protocol-link {
  font-size: 0.68rem;
  color: #2563EB;
  font-weight: 600;
}

.telemetry-comparison-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.historic-signs-card, .current-reval-card {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
  background-color: #F8FAFC;
}

.current-reval-card {
  background-color: #FFFFFF;
  border-color: #BFDBFE;
}

.card-inner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.68rem;
  font-weight: 700;
  color: #64748B;
  margin-bottom: 0.6rem;
  border-bottom: 1px solid #E2E8F0;
  padding-bottom: 0.3rem;
}

.reval-card-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-copy-history {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-copy-history:hover {
  background-color: #DBEAFE;
}

.card-inner-header.blue {
  color: #2563EB;
  border-color: #DBEAFE;
}

.historic-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
}

.h-sign {
  display: flex;
  flex-direction: column;
}

.h-sign small {
  font-size: 0.6rem;
  color: #94A3B8;
  font-weight: 700;
}

.h-sign strong {
  font-size: 0.95rem;
  color: #1E293B;
}

.h-sign span {
  font-size: 0.6rem;
  color: #64748B;
}

.reval-inputs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
}

.input-subgroup {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.input-subgroup label {
  font-size: 0.65rem;
  color: #475569;
  font-weight: 600;
}

.pa-mini-row {
  display: flex;
  align-items: center;
  gap: 0.15rem;
}

.input-mini {
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.35rem 0.4rem;
  font-size: 0.85rem;
  font-weight: 700;
  text-align: center;
  width: 50%;
  outline: none;
}

.input-mini.full {
  width: 100%;
}

.input-mini:focus {
  border-color: #2563EB;
}

.deterioro-radio-group {
  margin-bottom: 1rem;
}

.group-label {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748B;
  margin-bottom: 0.4rem;
  letter-spacing: 0.05em;
}

.radio-options-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
}

.radio-card {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.72rem;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
}

.radio-card.active {
  background-color: #EFF6FF;
  border-color: #2563EB;
  color: #1E40AF;
  font-weight: 700;
}

.discriminator-questions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.question-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.55rem 0.75rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
}

.question-row.highlight-urgent {
  background-color: #FFFDFD;
  border-color: #FECACA;
}

.q-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: #1E293B;
  flex: 1;
}

.q-hint {
  display: block;
  font-size: 0.65rem;
  color: #DC2626;
}

.q-actions {
  display: flex;
  gap: 0.3rem;
}

.btn-toggle {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.25rem 0.6rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: #64748B;
  cursor: pointer;
}

.btn-toggle.active {
  background-color: #0F4C81;
  border-color: #0F4C81;
  color: #FFFFFF;
}

.btn-danger-toggle.active {
  background-color: #DC2626;
  border-color: #DC2626;
  color: #FFFFFF;
}

.justificacion-box {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.just-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.just-header label {
  font-size: 0.68rem;
  font-weight: 800;
  color: #475569;
}

.badge-required {
  color: #DC2626;
  font-size: 0.65rem;
  font-weight: 700;
}

.input-clinical {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.7rem;
  font-size: 0.82rem;
  color: #1E293B;
  outline: none;
  background-color: #FFFFFF;
}

.input-clinical.full {
  width: 100%;
}

.input-clinical:focus {
  border-color: #2563EB;
}

.box-actions-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.shift-tag {
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 600;
}

.btn-clean {
  background: none;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  font-size: 0.7rem;
  color: #475569;
  cursor: pointer;
}

.table-container {
  overflow-x: auto;
}

.triage-records-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  text-align: left;
}

.triage-records-table th {
  background-color: #F8FAFC;
  color: #64748B;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #E2E8F0;
}

.empty-table-msg {
  padding: 1.5rem;
  color: #64748B;
  text-align: center;
}

.empty-table-msg span {
  display: block;
  font-weight: 700;
  margin-bottom: 0.2rem;
}

.empty-table-msg small {
  font-size: 0.72rem;
  color: #94A3B8;
}

.reval-decision-col {
  display: flex;
  flex-direction: column;
}

.decision-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.decision-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.5rem;
}

.decision-header h4 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.prior-priority-badge {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.6rem;
  text-align: center;
}

.prior-priority-badge small {
  font-size: 0.6rem;
  color: #64748B;
  display: block;
}

.badge-yellow {
  color: #D97706;
  font-weight: 800;
  font-size: 0.85rem;
  margin: 0.2rem 0;
}

.badge-yellow small {
  display: inline;
  color: #92400E;
}

.prior-priority-badge span {
  font-size: 0.68rem;
  color: #64748B;
}

.down-arrow-indicator {
  text-align: center;
  font-size: 1.2rem;
  color: #EA580C;
}

.new-level-card.orange {
  background-color: #FFF7ED;
  border: 1.5px solid #FDBA74;
  border-radius: 8px;
  padding: 0.75rem;
  text-align: center;
}

.new-level-tag {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.3rem;
}

.new-level-tag small {
  font-size: 0.62rem;
  font-weight: 800;
  color: #9A3412;
}

.badge-escalate {
  background-color: #EA580C;
  color: #FFFFFF;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.level-title-orange {
  font-size: 1.1rem;
  font-weight: 900;
  color: #C2410C;
  margin: 0.2rem 0 0.5rem 0;
}

.sla-reval-footer {
  font-size: 0.7rem;
  color: #7C2D12;
  border-top: 1px dashed #FDBA74;
  padding-top: 0.4rem;
  display: flex;
  justify-content: space-between;
}

.reassignment-card {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  padding: 0.65rem;
}

.reassign-title {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.65rem;
  color: #1E40AF;
  font-weight: 800;
}

.reassignment-card h4 {
  font-size: 0.95rem;
  color: #0F4C81;
  margin: 0.2rem 0;
}

.reassignment-card small {
  font-size: 0.65rem;
  color: #64748B;
  display: block;
}

.btn-save-triage {
  background: linear-gradient(135deg, #0F4C81, #1E40AF);
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(15, 76, 129, 0.2);
}

.btn-save-triage:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-direct-choque {
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
  border-radius: 8px;
  padding: 0.65rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-direct-choque:hover {
  background-color: #FEE2E2;
}

.btn-subtle {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-subtle.full-w {
  width: 100%;
}

.turno-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.summary-stat-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.4rem;
  text-align: center;
}

.summary-stat-box small {
  font-size: 0.62rem;
  color: #64748B;
  display: block;
}

.summary-stat-box strong {
  font-size: 1.1rem;
  color: #1E293B;
}

@media (max-width: 1100px) {
  .reclasificar-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .telemetry-comparison-grid, .radio-options-row {
    grid-template-columns: 1fr;
  }
}
</style>
