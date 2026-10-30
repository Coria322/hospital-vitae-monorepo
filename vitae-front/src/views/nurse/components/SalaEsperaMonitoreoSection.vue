<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  pacientes?: Array<{
    id?: string | number
    nombre: string
    edad: number | null
    sexo: string
    folio: string
    box: string
    triage: string
    triageColor: string
    tiempoEspera?: number
    signos: {
      paSistolica: string
      paDiastolica: string
      fc: string
      fr: string
      spo2: string
      temp: string
      evaDolor: number | string
    }
    motivoConsulta: string
    alergias?: string
  }>
}>()

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: 'triage'): void
  (e: 'activarCodigoAzul'): void
  (e: 'abrirReclasificar', paciente: any): void
  (e: 'abrirNota', paciente: any): void
}>()

const filtroSala = ref<'todos' | 'naranja' | 'amarillo' | 'verde'>('todos')

const listaPacientes = computed(() => {
  const base = props.pacientes || []
  if (filtroSala.value === 'todos') return base
  if (filtroSala.value === 'naranja') return base.filter(p => p.triage.toLowerCase().includes('naranja') || p.triage.toLowerCase().includes('ii'))
  if (filtroSala.value === 'amarillo') return base.filter(p => p.triage.toLowerCase().includes('amarillo') || p.triage.toLowerCase().includes('iii'))
  if (filtroSala.value === 'verde') return base.filter(p => p.triage.toLowerCase().includes('verde') || p.triage.toLowerCase().includes('iv'))
  return base
})

const pacientesEnEsperaCount = computed(() => (props.pacientes || []).length)
const tiempoPromedioEspera = computed(() => {
  if (!props.pacientes?.length) return 0
  const suma = props.pacientes.reduce((acc, curr) => acc + (curr.tiempoEspera || 0), 0)
  return Math.round(suma / props.pacientes.length)
})

const emitirVoceo = () => {
  alert('<svg viewBox="0 0 24 24" class="svg-inline-speaker" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg> Sistema de voceo general activado en altavoces de sala de espera.')
}

const dispararCodigoAzul = () => {
  emit('activarCodigoAzul')
}
</script>

<template>
  <div class="sala-layout-wrapper">
    <!-- Encabezado de la Sala -->
    <div class="sala-top-banner">
      <div class="sala-top-left">
        <span class="status-dot blue"></span>
        <div>
          <span class="module-tag">MÓDULO ACTIVO</span>
          <h3 class="sala-title">Monitor de Sala de Espera & Censo Dinámico de Urgencias</h3>
        </div>
        <span class="telemetria-pill"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.1 19.1 19"/></svg> Telemetría Activa</span>
      </div>

      <div class="sala-top-actions">
        <button type="button" class="btn-voceo" @click="emitirVoceo">
          <svg viewBox="0 0 24 24" class="svg-inline-speaker" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg> Voceo General <kbd>Alt+V</kbd>
        </button>
        <button type="button" class="btn-codigo-azul" @click="dispararCodigoAzul">
          <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Código Azul / Alarma Sala
        </button>
        <button type="button" class="btn-nuevo-triage" @click="emit('cambiarSeccion', 'triage')">
          <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg> Nuevo Triage <kbd>Alt+S</kbd>
        </button>
      </div>
    </div>

    <!-- Cuadrícula de 4 Indicadores Estadísticos Superiores -->
    <div class="sala-kpis-grid">
      <!-- KPI 1 -->
      <div class="kpi-card">
        <div class="kpi-info">
          <small>PACIENTES EN SALA</small>
          <div class="kpi-value-row">
            <strong>{{ pacientesEnEsperaCount }}</strong>
            <span class="kpi-subtag">espera activa</span>
          </div>
          <span class="kpi-footer-text">Priorizados según Manchester</span>
        </div>
        <div class="kpi-icon-box"><svg viewBox="0 0 24 24" class="svg-inline-users" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg></div>
      </div>

      <!-- KPI 2 -->
      <div class="kpi-card">
        <div class="kpi-info">
          <small>TIEMPO PROMEDIO ESPERA</small>
          <div class="kpi-value-row">
            <strong>{{ tiempoPromedioEspera }}</strong>
            <span class="kpi-subtag">min</span>
          </div>
          <span class="kpi-footer-text green"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> SLA global en norma</span>
        </div>
        <div class="kpi-icon-box"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></div>
      </div>

      <!-- KPI 3 -->
      <div class="kpi-card alert">
        <div class="kpi-info">
          <small>PRÓXIMOS A VENCER SLA</small>
          <div class="kpi-value-row">
            <strong class="text-danger">0</strong>
            <span class="badge-danger-mini">Control SLA</span>
          </div>
          <span class="kpi-footer-text red">Sin alertas críticas de tiempo</span>
        </div>
        <div class="kpi-icon-box alert"><svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg></div>
      </div>

      <!-- KPI 4 -->
      <div class="kpi-card">
        <div class="kpi-info">
          <small>CAPACIDAD BOXES URGENCIAS</small>
          <div class="kpi-value-row">
            <strong>{{ pacientesEnEsperaCount }}/6</strong>
            <span class="kpi-subtag">Ocupados</span>
          </div>
          <div class="capacity-bar-container">
            <div class="capacity-fill" :style="{ width: `${Math.min(100, (pacientesEnEsperaCount / 6) * 100)}%` }"></div>
          </div>
        </div>
        <div class="kpi-icon-box"><svg viewBox="0 0 24 24" class="svg-inline-hospital" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><path d="M9 22v-4h6v4"/><line x1="12" y1="6" x2="12" y2="10"/><line x1="10" y1="8" x2="14" y2="8"/></svg></div>
      </div>
    </div>

    <!-- Filtros de Manchester y Barra de Actualización -->
    <div class="sala-filters-bar">
      <div class="filters-chips">
        <button 
          type="button" 
          class="filter-chip" 
          :class="{ active: filtroSala === 'todos' }"
          @click="filtroSala = 'todos'"
        >
          Todos ({{ (props.pacientes || []).length }})
        </button>
        <button 
          type="button" 
          class="filter-chip orange" 
          :class="{ active: filtroSala === 'naranja' }"
          @click="filtroSala = 'naranja'"
        >
          ● Nivel II - Naranja
        </button>
        <button 
          type="button" 
          class="filter-chip yellow" 
          :class="{ active: filtroSala === 'amarillo' }"
          @click="filtroSala = 'amarillo'"
        >
          ● Nivel III - Amarillo
        </button>
        <button 
          type="button" 
          class="filter-chip green" 
          :class="{ active: filtroSala === 'verde' }"
          @click="filtroSala = 'verde'"
        >
          ● Nivel IV - Verde
        </button>
      </div>

      <div class="auto-refresh-tag">
        <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Actualización automática activa
      </div>
    </div>

    <!-- Lista de Tarjetas de Pacientes en Sala -->
    <div class="sala-patients-cards-container">
      <div v-if="listaPacientes.length === 0" class="empty-sala-card">
        <div class="empty-sala-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="7" rx="2"/><path d="M5 18v2M19 18v2M2 9v5M22 9v5"/></svg></div>
        <h4>Sala de espera despejada</h4>
        <p>Actualmente no hay pacientes en cola de espera en esta sala de urgencias. Los pacientes clasificados en el módulo de Triage aparecerán aquí con sus temporizadores en cuenta regresiva.</p>
      </div>

      <!-- Tarjetas de pacientes activos en espera -->
      <div 
        v-for="p in listaPacientes" 
        :key="p.id || p.folio" 
        class="waiting-patient-card"
      >
        <div class="w-patient-top">
          <div class="w-patient-identity">
            <div class="w-avatar">{{ p.nombre.charAt(0) }}</div>
            <div>
              <div class="w-name-row">
                <h4>{{ p.nombre }}</h4>
                <span class="w-age">({{ p.edad }} años · {{ p.sexo }})</span>
                <span class="w-folio">{{ p.folio }}</span>
              </div>
              <p class="w-motivo">{{ p.motivoConsulta }}</p>
            </div>
          </div>

          <div class="w-patient-meta">
            <span class="w-triage-pill" :style="{ backgroundColor: p.triageColor + '20', color: p.triageColor, borderColor: p.triageColor }">
              ● {{ p.triage }}
            </span>
            <div class="w-wait-timer">
              <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg> Espera: <strong>{{ p.tiempoEspera || 10 }} min</strong>
            </div>
          </div>
        </div>

        <div class="w-patient-bottom">
          <div class="w-vitals-strip">
            <span>PA: <strong>{{ p.signos.paSistolica }}/{{ p.signos.paDiastolica }}</strong></span>
            <span>FC: <strong>{{ p.signos.fc }} lpm</strong></span>
            <span>SpO2: <strong>{{ p.signos.spo2 }}%</strong></span>
            <span>Temp: <strong>{{ p.signos.temp }}°C</strong></span>
            <span>EVA: <strong>{{ p.signos.evaDolor }}/10</strong></span>
            <span v-if="p.alergias" class="w-allergy"><svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg> Alergia: {{ p.alergias }}</span>
          </div>

          <div class="w-actions-row">
            <button 
              type="button" 
              class="btn-w-reval" 
              @click="emit('abrirReclasificar', p)"
              title="Abrir este paciente en Reclasificación Manchester"
            >
              <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Reclasificar Manchester
            </button>
            <button 
              type="button" 
              class="btn-w-note" 
              @click="emit('abrirNota', p)"
              title="Abrir Nota de Enfermería para este paciente"
            >
              <svg viewBox="0 0 24 24" class="svg-inline-edit" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg> Nota de Enfermería
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sala-layout-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.sala-top-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.sala-top-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.blue {
  background-color: #2563EB;
}

.module-tag {
  font-size: 0.65rem;
  font-weight: 800;
  color: #2563EB;
  letter-spacing: 0.05em;
  display: block;
}

.sala-title {
  font-size: 1.05rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
}

.telemetria-pill {
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  margin-left: 0.5rem;
}

.sala-top-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-voceo {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #334155;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.btn-codigo-azul {
  background-color: #7F1D1D;
  border: 1px solid #991B1B;
  color: #FFFFFF;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-nuevo-triage {
  background-color: #2563EB;
  border: none;
  color: #FFFFFF;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

kbd {
  font-family: monospace;
  font-size: 0.68rem;
  padding: 0.15rem 0.35rem;
  background: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  color: #64748B;
}

.sala-kpis-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.kpi-card.alert {
  border-color: #FECACA;
  background-color: #FFFDFD;
}

.kpi-info small {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
  margin-bottom: 0.3rem;
}

.kpi-value-row {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
}

.kpi-value-row strong {
  font-size: 1.6rem;
  font-weight: 800;
  color: #1E293B;
  line-height: 1;
}

.text-danger {
  color: #DC2626 !important;
}

.kpi-subtag {
  font-size: 0.72rem;
  color: #64748B;
}

.badge-danger-mini {
  background-color: #FEE2E2;
  color: #DC2626;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.kpi-footer-text {
  display: block;
  font-size: 0.68rem;
  color: #64748B;
}

.kpi-footer-text.green { color: #10B981; }
.kpi-footer-text.red { color: #DC2626; }

.kpi-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background-color: #F8FAFC;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.kpi-icon-box.alert {
  background-color: #FEE2E2;
}

.capacity-bar-container {
  width: 100px;
  height: 6px;
  background-color: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 0.4rem;
}

.capacity-fill {
  height: 100%;
  background-color: #2563EB;
}

.sala-filters-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.filters-chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-chip {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 9999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-chip.active {
  background-color: #0F4C81;
  color: #FFFFFF;
  border-color: #0F4C81;
}

.filter-chip.orange.active { background-color: #EA580C; border-color: #EA580C; color: #FFFFFF; }
.filter-chip.yellow.active { background-color: #D97706; border-color: #D97706; color: #FFFFFF; }
.filter-chip.green.active { background-color: #059669; border-color: #059669; color: #FFFFFF; }

.auto-refresh-tag {
  font-size: 0.72rem;
  color: #64748B;
}

.sala-patients-cards-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.empty-sala-card {
  background-color: #FFFFFF;
  border: 1px dashed #CBD5E1;
  border-radius: 10px;
  padding: 3rem 1.5rem;
  text-align: center;
}

.empty-sala-icon {
  font-size: 3rem;
  margin-bottom: 0.8rem;
}

.empty-sala-card h4 {
  font-size: 1.15rem;
  color: #1E293B;
  margin-bottom: 0.4rem;
}

.empty-sala-card p {
  font-size: 0.85rem;
  color: #64748B;
  max-width: 500px;
  margin: 0 auto 1.5rem auto;
}

.waiting-patient-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.w-patient-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.w-patient-identity {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.w-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #EFF6FF;
  color: #1E40AF;
  font-weight: 800;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.w-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.w-name-row h4 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
}

.w-age {
  font-size: 0.75rem;
  color: #64748B;
}

.w-folio {
  font-family: monospace;
  font-size: 0.7rem;
  background-color: #F1F5F9;
  color: #475569;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.w-motivo {
  font-size: 0.8rem;
  color: #334155;
  margin: 0.25rem 0 0 0;
}

.w-patient-meta {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.w-triage-pill {
  font-size: 0.72rem;
  font-weight: 800;
  padding: 0.25rem 0.6rem;
  border-radius: 9999px;
  border: 1px solid;
}

.w-wait-timer {
  font-size: 0.75rem;
  color: #475569;
}

.w-wait-timer strong {
  color: #B45309;
}

.w-patient-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.8rem;
  border-top: 1px solid #F1F5F9;
  padding-top: 0.6rem;
}

.w-vitals-strip {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  font-size: 0.72rem;
  color: #64748B;
  flex-wrap: wrap;
}

.w-vitals-strip strong {
  color: #1E293B;
  font-family: monospace;
}

.w-allergy {
  color: #DC2626;
  font-weight: 700;
  background-color: #FEE2E2;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.w-actions-row {
  display: flex;
  gap: 0.5rem;
}

.btn-w-reval {
  background-color: #FFFBEB;
  border: 1px solid #FDE68A;
  color: #B45309;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-w-reval:hover {
  background-color: #FEF3C7;
}

.btn-w-note {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-w-note:hover {
  background-color: #DBEAFE;
}

@media (max-width: 800px) {
  .sala-kpis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
