<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Paciente, Doctor } from '../types'

const props = defineProps<{
  doctor: Doctor
  pacientesCola: Paciente[]
  pacienteSeleccionado: Paciente | null
}>()

const emit = defineEmits<{
  (e: 'seleccionarPaciente', paciente: Paciente): void
  (e: 'ingresarConsulta', paciente: Paciente): void
}>()

// Filtros y búsqueda
const tabActivo = ref<'todos' | 'asignados' | 'sla' | 'interconsultas'>('todos')
const busquedaTexto = ref('')
const recepcionPausada = ref(false)
const pacienteLlamadoEnMonitor = ref<string>('Carlos Mendoza Ruiz')

// Modelo de pacientes en cola ampliado con los datos visuales de la captura
interface PacienteColaItem {
  id: string
  nombre: string
  edad: number
  sexo: string
  folioTriage: string
  triageNivel: 'I' | 'II' | 'III' | 'IV' | 'V'
  triageNombre: string
  triageColor: string
  esperaMin: number
  metaMin: number
  excedidoMin?: number
  motivo: string
  constantes: string
  destinoAsignado?: string
  alergias?: string
  observacionTriage?: string
  tiempoGuardia?: string
  asignadoAMiBox: boolean
}

const listaCola = ref<PacienteColaItem[]>([
  {
    id: 'PAC-1',
    nombre: 'Carlos Mendoza Ruiz',
    edad: 42,
    sexo: 'Masculino',
    folioTriage: 'TRI-2026-0891',
    triageNivel: 'II',
    triageNombre: 'NIVEL II · MUY URGENTE',
    triageColor: '#DC2626',
    esperaMin: 48,
    metaMin: 15,
    excedidoMin: 33,
    motivo: 'Dolor torácico opresivo precordial',
    constantes: 'TA: 145/95 · FC: 98 lpm · SpO2: 95%',
    destinoAsignado: 'Box 04 (Dr. Carlos Mendoza)',
    asignadoAMiBox: true
  },
  {
    id: 'PAC-2',
    nombre: 'Mateo Ruiz',
    edad: 35,
    sexo: 'Masculino',
    folioTriage: 'TRI-2026-0892',
    triageNivel: 'III',
    triageNombre: 'NIVEL III · URGENTE (AMARILLO)',
    triageColor: '#D97706',
    esperaMin: 32,
    metaMin: 60,
    motivo: 'Dolor abdominal agudo en fosa ilíaca derecha',
    constantes: 'TA: 120/80 · FC: 82 lpm · EVA: 6/10',
    alergias: 'Sin alergias medicamentosas declaradas',
    asignadoAMiBox: true
  },
  {
    id: 'PAC-3',
    nombre: 'Ana Solís',
    edad: 28,
    sexo: 'Femenino',
    folioTriage: 'TRI-2026-0893',
    triageNivel: 'IV',
    triageNombre: 'NIVEL IV · ESTÁNDAR (VERDE)',
    triageColor: '#059669',
    esperaMin: 21,
    metaMin: 120,
    motivo: 'Esguince de tobillo derecho tras caída mecánica',
    constantes: '',
    observacionTriage: 'Edema moderado, marcha claudicante, sensibilidad distal conservada',
    asignadoAMiBox: false
  },
  {
    id: 'PAC-4',
    nombre: 'Pablo Vega',
    edad: 54,
    sexo: 'Masculino',
    folioTriage: 'TRI-2026-0894',
    triageNivel: 'V',
    triageNombre: 'NIVEL V · NO URGENTE (AZUL)',
    triageColor: '#2563EB',
    esperaMin: 14,
    metaMin: 240,
    motivo: 'Control post-quirúrgico menor y retiro de material de sutura',
    constantes: '',
    tiempoGuardia: 'Ingreso por recepción general - Documentación completa',
    asignadoAMiBox: false
  }
])

// Filtrado de pacientes
const pacientesFiltrados = computed(() => {
  return listaCola.value.filter(p => {
    // Filtro por pestaña
    if (tabActivo.value === 'asignados' && !p.asignadoAMiBox) return false
    if (tabActivo.value === 'sla' && (!p.excedidoMin || p.excedidoMin <= 0)) return false
    // Filtro por texto
    if (busquedaTexto.value) {
      const q = busquedaTexto.value.toLowerCase()
      const matchNom = p.nombre.toLowerCase().includes(q)
      const matchFol = p.folioTriage.toLowerCase().includes(q)
      const matchMot = p.motivo.toLowerCase().includes(q)
      if (!matchNom && !matchFol && !matchMot) return false
    }
    return true
  })
})

const llamarSiguiente = () => {
  const primero = listaCola.value[0]
  if (primero) {
    pacienteLlamadoEnMonitor.value = primero.nombre
    alert(`📢 LLAMANDO EN ALTAVOZ Y MONITOR: ${primero.nombre} a Box 04 · Urgencia Adultos`)
  }
}

const reLlamarAltavoz = (p: PacienteColaItem) => {
  pacienteLlamadoEnMonitor.value = p.nombre
  alert(`📢 Re-llamando en pantalla y altavoz a ${p.nombre} a Box 04`)
}

const ingresarConsulta = (p: PacienteColaItem) => {
  // Convertir a estructura Paciente
  const pacienteObj: Paciente = {
    id: p.id,
    nombre: p.nombre,
    folio: p.folioTriage,
    ece: 'DEMO-0248',
    edad: p.edad,
    sexo: p.sexo,
    box: 'Box 04',
    motivo: p.motivo,
    triage: p.triageNombre,
    triageColor: p.triageColor,
    limiteSla: `Límite < ${p.metaMin} min desde triage`,
    transcurridosMin: p.esperaMin,
    restantesMin: Math.max(0, p.metaMin - p.esperaMin),
    signos: {
      pa: '145/95',
      fc: '98',
      temp: '37.2',
      spo2: '95',
      horaToma: '10:40 h'
    }
  }
  emit('ingresarConsulta', pacienteObj)
}
</script>

<template>
  <div class="cola-espera-view">
    <!-- SUBNAV / BREADCRUMBS -->
    <div class="cola-subnav">
      <div class="subnav-left">
        <span class="breadcrumb-trail">ATENCIÓN CLÍNICA &gt; GESTIÓN DE PACIENTES &gt;</span>
        <strong class="breadcrumb-current">COLA DE ESPERA Y FLUJO DE ATENCIÓN MÉDICA</strong>
        <div class="title-row">
          <h2>Gestor de Flujo y Demanda de {{ doctor.box }}</h2>
        </div>
        <p class="service-subtitle">Servicio Activo: Urgencias Adultos · Censo de pacientes clasificados y tiempos de compromiso asistencial (SLA).</p>
      </div>

      <div class="subnav-right">
        <button class="btn-call-next" type="button" @click="llamarSiguiente">
          <span class="btn-speaker-icon">📢</span>
          <span>Llamar Siguiente Turno [Espacio]</span>
        </button>

        <button
          class="btn-pause-reception"
          :class="{ paused: recepcionPausada }"
          type="button"
          @click="recepcionPausada = !recepcionPausada"
        >
          <span>⏸</span>
          <span>{{ recepcionPausada ? 'Reanudar' : 'Pausar Recepción' }}</span>
        </button>
      </div>
    </div>

    <!-- TARJETAS DE MÉTRICAS / KPIS (4 EN FILA) -->
    <div class="kpi-metrics-grid">
      <!-- KPI 1 -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-label">PACIENTES EN COLA</span>
          <span class="blue-dot">●</span>
        </div>
        <div class="kpi-body">
          <strong class="kpi-number">{{ String(listaCola.length).padStart(2, '0') }}</strong>
          <span class="kpi-unit">en sala física</span>
        </div>
        <div class="kpi-footer text-green">
          <span>✓ Aforo controlado (Box 01 a 06)</span>
        </div>
      </div>

      <!-- KPI 2 -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-label">TIEMPO PROMEDIO ESPERA</span>
          <span class="kpi-icon">⏱️</span>
        </div>
        <div class="kpi-body">
          <strong class="kpi-number">22</strong>
          <span class="kpi-unit">minutos</span>
        </div>
        <div class="kpi-footer text-blue">
          <span>● Meta institucional: &lt; 30 min</span>
        </div>
      </div>

      <!-- KPI 3 -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-label">ALERTAS DE SLA MANCHESTER</span>
          <span class="badge-kpi-red">Crítico</span>
        </div>
        <div class="kpi-body">
          <strong class="kpi-number text-red">01</strong>
          <span class="kpi-unit">urgencia superada</span>
        </div>
        <div class="kpi-footer text-red">
          <span>⚠️ Manchester Nivel II en espera</span>
        </div>
      </div>

      <!-- KPI 4 -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-label">ESTADO DE MI BOX (BOX 04)</span>
          <span class="badge-kpi-green">DISPONIBLE</span>
        </div>
        <div class="kpi-body">
          <strong class="kpi-text-status">Listo</strong>
          <span class="kpi-unit">para recepción</span>
        </div>
        <div class="kpi-footer text-gray">
          <span>📄 En espera de ingreso de turno</span>
        </div>
      </div>
    </div>

    <!-- PESTAÑAS Y BUSCADOR -->
    <div class="filters-card">
      <div class="tabs-and-order-row">
        <div class="queue-tabs">
          <button
            class="q-tab"
            :class="{ active: tabActivo === 'todos' }"
            @click="tabActivo = 'todos'"
            type="button"
          >
            Todos los Pacientes ({{ listaCola.length }})
          </button>
          <button
            class="q-tab"
            :class="{ active: tabActivo === 'asignados' }"
            @click="tabActivo = 'asignados'"
            type="button"
          >
            Asignados a mi Box ({{ doctor.box }})
          </button>
          <button
            class="q-tab"
            :class="{ active: tabActivo === 'sla' }"
            @click="tabActivo = 'sla'"
            type="button"
          >
            Por Vencer SLA
          </button>
          <button
            class="q-tab"
            :class="{ active: tabActivo === 'interconsultas' }"
            @click="tabActivo = 'interconsultas'"
            type="button"
          >
            Interconsultas
          </button>
        </div>

        <div class="order-label">
          <span>Orden: <strong>Prioridad Clínica (Manchester)</strong></span>
        </div>
      </div>

      <div class="search-queue-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="busquedaTexto"
          type="text"
          class="search-queue-input"
          placeholder="Buscar en la cola por nombre, folio TRIAGE o CURP..."
        />
      </div>
    </div>

    <!-- GRID DE 2 COLUMNAS (LISTA DE TARJETAS IZQ + PANELES LATERALES DER) -->
    <div class="queue-main-grid">
      
      <!-- COLUMNA IZQUIERDA: LISTA DE TARJETAS DE PACIENTES -->
      <div class="patients-cards-column">
        
        <div
          v-for="paciente in pacientesFiltrados"
          :key="paciente.id"
          class="patient-demand-card"
          :class="`border-level-${paciente.triageNivel}`"
        >
          <!-- HEADER DE LA TARJETA -->
          <div class="demand-card-header">
            <div class="badge-and-folio">
              <span class="triage-level-tag" :class="`tag-level-${paciente.triageNivel}`">
                {{ paciente.triageNombre }}
              </span>
              <span class="triage-folio">{{ paciente.folioTriage }}</span>
              <span class="dot-sep">•</span>
            </div>

            <div class="sla-timer-badge" :class="{ 'sla-exceeded': paciente.excedidoMin }">
              <span class="timer-icon">⏱️</span>
              <span v-if="paciente.excedidoMin">
                Espera: {{ paciente.esperaMin }} min / Meta: {{ paciente.metaMin }} min <strong>(+{{ paciente.excedidoMin }} min excedido)</strong>
              </span>
              <span v-else>
                Espera: {{ paciente.esperaMin }} min / Meta: {{ paciente.metaMin }} min ({{ paciente.metaMin - paciente.esperaMin }} min restantes)
              </span>
            </div>
          </div>

          <!-- NOMBRE DEL PACIENTE -->
          <div class="demand-patient-name-row">
            <h3>{{ paciente.nombre }}</h3>
            <span class="patient-age-sex">{{ paciente.edad }} años · {{ paciente.sexo }}</span>
          </div>

          <!-- CONTENIDO ESPECÍFICO SEGÚN NIVEL -->
          <div class="demand-details-grid">
            <div class="detail-block">
              <span class="block-lbl">{{ paciente.triageNivel === 'IV' ? 'MOTIVO DE CONSULTA' : 'MOTIVO DE TRIAGE' }}</span>
              <p class="block-text">{{ paciente.motivo }}</p>
            </div>

            <div v-if="paciente.constantes" class="detail-block">
              <span class="block-lbl">CONSTANTES EN RECEPCIÓN</span>
              <p class="block-text">{{ paciente.constantes }}</p>
            </div>

            <div v-if="paciente.destinoAsignado" class="detail-block">
              <span class="block-lbl">DESTINO ASIGNADO</span>
              <p class="block-text highlight-box">{{ paciente.destinoAsignado }}</p>
            </div>

            <div v-if="paciente.alergias" class="detail-block">
              <span class="block-lbl">ALERGIAS CONOCIDAS</span>
              <p class="block-text bold-text">{{ paciente.alergias }}</p>
            </div>

            <div v-if="paciente.observacionTriage" class="detail-block">
              <span class="block-lbl">TRIAGE OBSERVACIÓN</span>
              <p class="block-text">{{ paciente.observacionTriage }}</p>
            </div>

            <div v-if="paciente.tiempoGuardia" class="detail-block">
              <span class="block-lbl">TIEMPO DE GUARDIA</span>
              <p class="block-text">{{ paciente.tiempoGuardia }}</p>
            </div>
          </div>

          <!-- BOTONES DE ACCIÓN DE LA TARJETA -->
          <div class="demand-actions-bar">
            <!-- Caso Nivel II (Botón grande azul de ingreso inmediato) -->
            <template v-if="paciente.triageNivel === 'II'">
              <div class="sub-actions-row">
                <button class="btn-subtle-action" @click="reLlamarAltavoz(paciente)" type="button">
                  📢 Re-llamar Altavoz
                </button>
                <button class="btn-subtle-action" type="button">
                  📄 Ver Expediente Previo
                </button>
              </div>
              <button class="btn-primary-enter-box" @click="ingresarConsulta(paciente)" type="button">
                ➔ Ingresar a Consulta / {{ doctor.box }} [Enter]
              </button>
            </template>

            <!-- Caso Nivel III -->
            <template v-else-if="paciente.triageNivel === 'III'">
              <div class="actions-right-aligned">
                <button class="btn-subtle-action" type="button">
                  👁️ Ver Triage Completo
                </button>
                <button class="btn-dark-call" @click="ingresarConsulta(paciente)" type="button">
                  ➔ Llamar a Consulta
                </button>
              </div>
            </template>

            <!-- Caso Niveles IV y V -->
            <template v-else>
              <div class="actions-right-aligned">
                <button class="btn-dark-call" @click="ingresarConsulta(paciente)" type="button">
                  ➔ Llamar a Consulta
                </button>
              </div>
            </template>
          </div>
        </div>

      </div>

      <!-- COLUMNA DERECHA: MONITOR DE PASILLO, ESTADO OPERATIVO Y ATENDIDOS -->
      <aside class="queue-right-column">
        
        <!-- MONITOR PÚBLICO DE PASILLO -->
        <div class="hallway-monitor-card">
          <div class="monitor-header">
            <span class="monitor-screen-icon">📺</span>
            <h4>MONITOR PÚBLICO DE PASILLO</h4>
            <span class="badge-live">● En vivo</span>
          </div>

          <div class="monitor-screen-body">
            <span class="screen-calling-tag">LLAMANDO A CONSULTA</span>
            <strong class="screen-patient-name">{{ pacienteLlamadoEnMonitor }}</strong>
            <div class="screen-box-route">
              ➔ {{ doctor.box.toUpperCase() }} · URGENCIA ADULTOS
            </div>
          </div>

          <div class="monitor-footer-sync">
            <small>Sincronizado con sistema de audio de pasillo y pantallas del módulo B.</small>
          </div>
        </div>

        <!-- ESTADO OPERATIVO BOX 04 -->
        <div class="side-card">
          <div class="side-card-header">
            <div class="header-with-icon">
              <span>🏥</span>
              <h4>Estado Operativo {{ doctor.box }}</h4>
            </div>
            <span class="badge-operativo">100% Operativo</span>
          </div>

          <ul class="equipment-checklist">
            <li class="equipment-item">
              <span class="eq-icon">🫀</span>
              <span class="eq-name">Electrocardiógrafo 12 derivaciones</span>
              <span class="eq-status tag-blue">Listo</span>
            </li>
            <li class="equipment-item">
              <span class="eq-icon">🚑</span>
              <span class="eq-name">Carro de Paro / Reanimación</span>
              <span class="eq-status tag-blue">Verificado</span>
            </li>
            <li class="equipment-item">
              <span class="eq-icon">🛏️</span>
              <span class="eq-name">Camilla de exploración clínica</span>
              <span class="eq-status tag-blue">Desinfectada</span>
            </li>
          </ul>

          <button class="btn-block-box" type="button">
            🔒 Bloquear Box por Desinfección / Procedimiento
          </button>
        </div>

        <!-- ATENDIDOS EN GUARDIA -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>Atendidos en Guardia</h4>
            <span class="sub-counter">Hoy: 8 pacientes</span>
          </div>

          <ul class="attended-list">
            <li class="attended-item">
              <div class="attended-meta">
                <strong>19:15 · Sofía Delgado</strong>
                <small>(Cefalea)</small>
              </div>
              <span class="attended-outcome tag-alta">Alta médica</span>
            </li>
            <li class="attended-item">
              <div class="attended-meta">
                <strong>18:40 · Javier Morales</strong>
                <small>(Crisis HTA)</small>
              </div>
              <span class="attended-outcome tag-obs">A Observación</span>
            </li>
            <li class="attended-item">
              <div class="attended-meta">
                <strong>18:02 · Elena Campos</strong>
                <small>(Laceración)</small>
              </div>
              <span class="attended-outcome tag-sutura">Sutura / Alta</span>
            </li>
          </ul>
        </div>

      </aside>

    </div>
  </div>
</template>

<style scoped>
.cola-espera-view {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  width: 100%;
}

/* SUBNAV */
.cola-subnav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #FFFFFF;
  padding: 0.85rem 1.25rem;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  flex-wrap: wrap;
  gap: 1rem;
}

.breadcrumb-trail {
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 700;
  letter-spacing: 0.04em;
  margin-right: 0.3rem;
}

.breadcrumb-current {
  font-size: 0.68rem;
  color: #0F4C81;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.title-row h2 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0.2rem 0 0 0;
}

.service-subtitle {
  font-size: 0.72rem;
  color: #64748B;
  margin: 0.15rem 0 0 0;
}

.subnav-right {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.btn-call-next {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1rem;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-call-next:hover {
  background-color: #0c3b64;
}

.btn-pause-reception {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  color: #334155;
  border-radius: 6px;
  padding: 0.65rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-pause-reception.paused {
  background-color: #FEE2E2;
  color: #DC2626;
  border-color: #FCA5A5;
}

/* KPIS */
.kpi-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.4rem;
}

.kpi-label {
  font-size: 0.62rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.04em;
}

.blue-dot {
  color: #2563EB;
}

.badge-kpi-red {
  background-color: #7F1D1D;
  color: #FFFFFF;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.badge-kpi-green {
  background-color: #E2E8F0;
  color: #334155;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.kpi-body {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  margin-bottom: 0.4rem;
}

.kpi-number {
  font-size: 1.7rem;
  font-weight: 800;
  line-height: 1;
  color: #1E293B;
}

.kpi-text-status {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
  color: #1E293B;
}

.kpi-number.text-red {
  color: #DC2626;
}

.kpi-unit {
  font-size: 0.72rem;
  color: #64748B;
}

.kpi-footer {
  font-size: 0.68rem;
  font-weight: 600;
}

.text-green { color: #059669; }
.text-blue { color: #2563EB; }
.text-red { color: #DC2626; }
.text-gray { color: #64748B; }

/* FILTROS Y BUSCADOR */
.filters-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.tabs-and-order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.queue-tabs {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.q-tab {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.q-tab.active {
  background-color: #FFFFFF;
  border-color: #CBD5E1;
  color: #0F4C81;
  font-weight: 800;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.order-label {
  font-size: 0.72rem;
  color: #64748B;
}

.search-queue-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  font-size: 0.85rem;
  color: #94A3B8;
}

.search-queue-input {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem 0.75rem 0.55rem 2.2rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  background-color: #F8FAFC;
}

.search-queue-input:focus {
  background-color: #FFFFFF;
  border-color: #3B82F6;
}

/* GRID PRINCIPAL */
.queue-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

.patients-cards-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* TARJETA DE PACIENTE */
.patient-demand-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-left-width: 5px;
  border-radius: 10px;
  padding: 1.1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  transition: box-shadow 0.15s;
}

.patient-demand-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.border-level-II { border-left-color: #7F1D1D; }
.border-level-III { border-left-color: #D97706; }
.border-level-IV { border-left-color: #059669; }
.border-level-V { border-left-color: #2563EB; }

.demand-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.badge-and-folio {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.triage-level-tag {
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  color: #FFFFFF;
}

.tag-level-II { background-color: #7F1D1D; }
.tag-level-III { background-color: #D97706; }
.tag-level-IV { background-color: #059669; }
.tag-level-V { background-color: #2563EB; }

.triage-folio {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748B;
  font-family: monospace;
}

.sla-timer-badge {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
  font-size: 0.7rem;
  color: #475569;
}

.sla-timer-badge.sla-exceeded {
  background-color: #FFF1F2;
  border-color: #FECDD3;
  color: #BE123C;
}

.demand-patient-name-row {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
}

.demand-patient-name-row h3 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.patient-age-sex {
  font-size: 0.78rem;
  color: #64748B;
}

.demand-details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.85rem;
  background-color: #F8FAFC;
  border-radius: 8px;
  padding: 0.75rem 1rem;
}

.detail-block {
  display: flex;
  flex-direction: column;
}

.block-lbl {
  font-size: 0.6rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.04em;
  margin-bottom: 0.2rem;
}

.block-text {
  font-size: 0.8rem;
  color: #1E293B;
  margin: 0;
  line-height: 1.35;
}

.block-text.highlight-box {
  color: #0284C7;
  font-weight: 700;
}

.block-text.bold-text {
  font-weight: 700;
}

/* ACCIONES DE TARJETA */
.demand-actions-bar {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sub-actions-row {
  display: flex;
  gap: 0.5rem;
}

.btn-subtle-action {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  color: #334155;
  border-radius: 6px;
  padding: 0.45rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-subtle-action:hover {
  background-color: #E2E8F0;
}

.btn-primary-enter-box {
  width: 100%;
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.75rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-primary-enter-box:hover {
  background-color: #1D4ED8;
}

.actions-right-aligned {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-dark-call {
  background-color: #0F172A;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-dark-call:hover {
  background-color: #1E293B;
}

/* COLUMNA DERECHA */
.queue-right-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* MONITOR DE PASILLO */
.hallway-monitor-card {
  background-color: #0B132B;
  color: #FFFFFF;
  border-radius: 10px;
  padding: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 4px 12px rgba(11, 19, 43, 0.2);
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monitor-header h4 {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  margin: 0;
  color: #94A3B8;
}

.badge-live {
  color: #38BDF8;
  font-size: 0.68rem;
  font-weight: 700;
}

.monitor-screen-body {
  text-align: center;
  padding: 0.6rem 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.screen-calling-tag {
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  color: #94A3B8;
  display: block;
  margin-bottom: 0.35rem;
}

.screen-patient-name {
  font-size: 1.25rem;
  font-weight: 800;
  color: #FFFFFF;
  display: block;
  margin-bottom: 0.35rem;
}

.screen-box-route {
  color: #38BDF8;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.monitor-footer-sync {
  font-size: 0.65rem;
  color: #64748B;
  text-align: center;
}

/* SIDE CARDS */
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
  margin-bottom: 0.75rem;
}

.header-with-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.side-card-header h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-operativo {
  color: #0284C7;
  font-size: 0.68rem;
  font-weight: 700;
}

.equipment-checklist {
  list-style: none;
  padding: 0;
  margin: 0 0 0.85rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.equipment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
  color: #334155;
}

.eq-name {
  flex: 1;
  margin-left: 0.4rem;
}

.eq-status {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.tag-blue {
  background-color: #EFF6FF;
  color: #2563EB;
}

.btn-block-box {
  width: 100%;
  background-color: #FEF2F2;
  border: 1px solid #FECDD3;
  color: #991B1B;
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-block-box:hover {
  background-color: #FEE2E2;
}

/* ATENDIDOS */
.sub-counter {
  font-size: 0.68rem;
  color: #64748B;
}

.attended-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.attended-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid #F1F5F9;
  font-size: 0.72rem;
}

.attended-meta strong {
  display: block;
  color: #1E293B;
}

.attended-meta small {
  color: #64748B;
}

.attended-outcome {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.tag-alta {
  background-color: #EFF6FF;
  color: #2563EB;
}

.tag-obs {
  background-color: #FEF3C7;
  color: #D97706;
}

.tag-sutura {
  background-color: #ECFDF5;
  color: #059669;
}

@media (max-width: 1100px) {
  .queue-main-grid {
    grid-template-columns: 1fr;
  }
  .kpi-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
