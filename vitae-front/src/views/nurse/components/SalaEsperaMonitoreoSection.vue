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
  alert('📢 Sistema de voceo general activado en altavoces de sala de espera.')
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
        <span class="telemetria-pill">📡 Telemetría Activa</span>
      </div>

      <div class="sala-top-actions">
        <button type="button" class="btn-voceo" @click="emitirVoceo">
          📢 Voceo General <kbd>Alt+V</kbd>
        </button>
        <button type="button" class="btn-codigo-azul" @click="dispararCodigoAzul">
          ⚡ Código Azul / Alarma Sala
        </button>
        <button type="button" class="btn-nuevo-triage" @click="emit('cambiarSeccion', 'triage')">
          ➕ Nuevo Triage <kbd>Alt+S</kbd>
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
        <div class="kpi-icon-box">👥</div>
      </div>

      <!-- KPI 2 -->
      <div class="kpi-card">
        <div class="kpi-info">
          <small>TIEMPO PROMEDIO ESPERA</small>
          <div class="kpi-value-row">
            <strong>{{ tiempoPromedioEspera }}</strong>
            <span class="kpi-subtag">min</span>
          </div>
          <span class="kpi-footer-text green">✓ SLA global en norma</span>
        </div>
        <div class="kpi-icon-box">⏱️</div>
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
        <div class="kpi-icon-box alert">🚨</div>
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
        <div class="kpi-icon-box">🏥</div>
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
        🔄 Actualización automática activa
      </div>
    </div>

    <!-- Lista de Tarjetas de Pacientes en Sala -->
    <div class="sala-patients-cards-container">
      <div v-if="listaPacientes.length === 0" class="empty-sala-card">
        <div class="empty-sala-icon">🛋️</div>
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
              ⏱️ Espera: <strong>{{ p.tiempoEspera || 10 }} min</strong>
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
            <span v-if="p.alergias" class="w-allergy">⚠️ Alergia: {{ p.alergias }}</span>
          </div>

          <div class="w-actions-row">
            <button 
              type="button" 
              class="btn-w-reval" 
              @click="emit('abrirReclasificar', p)"
              title="Abrir este paciente en Reclasificación Manchester"
            >
              🔄 Reclasificar Manchester
            </button>
            <button 
              type="button" 
              class="btn-w-note" 
              @click="emit('abrirNota', p)"
              title="Abrir Nota de Enfermería para este paciente"
            >
              📝 Nota de Enfermería
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
