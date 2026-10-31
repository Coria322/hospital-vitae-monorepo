<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { LabUser, EstudioDiagnostico, LabKPIs, ArchivoResultado } from './types'

import LabHeader from './components/LabHeader.vue'
import LabSidebar from './components/LabSidebar.vue'
import LabSlaBanner from './components/LabSlaBanner.vue'
import LabPendientesSection from './components/LabPendientesSection.vue'
import LabCargarResultadosSection from './components/LabCargarResultadosSection.vue'
import LabBusquedaFolioSection from './components/LabBusquedaFolioSection.vue'
import LabHistorialBitacoraSection from './components/LabHistorialBitacoraSection.vue'

// --- ESTADO PRINCIPAL DEL USUARIO ---
const user = ref<LabUser>({
  nombre: 'Q.F.B. Mariana Téllez Morales',
  cedula: 'LAB-449102',
  rol: 'LABORATORIO CLÍNICO',
  turno: 'Matutino (07:00 - 15:00)',
  area: 'Laboratorio Central e Imagenología - Planta Baja'
})

// --- RELOJ INSTITUCIONAL EN VIVO ---
const currentDateTimeStr = ref('17 oct 2026 - 10:48 h')
let clockInterval: number | null = null

const updateClock = () => {
  const now = new Date()
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
  const d = now.getDate()
  const m = meses[now.getMonth()]
  const y = now.getFullYear()
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  currentDateTimeStr.value = `${d} ${m} ${y} • ${h}:${min} h`
}

// --- PESTAÑA ACTIVA ---
const activeTab = ref<'pendientes' | 'cargar' | 'busqueda' | 'historial'>('pendientes')

// --- BANCO DE ESTUDIOS EN TIEMPO REAL ---
const orders = ref<EstudioDiagnostico[]>([
  {
    id: 'est-1',
    folio: 'EST-2026-9041',
    codigo_tubo: 'BC-HEP-88190',
    paciente_nombre: 'Carmen Mendoza Ramírez',
    paciente_iniciales: 'C.M.R.',
    paciente_edad: 42,
    paciente_sexo: 'F',
    prioridad: 'STAT',
    codigo_color: 'ROJO',
    estado: 'EN_PROCESO',
    categoria: 'LABORATORIO',
    estudio_requerido: 'Gasometría Arterial + Troponina I Alta Sensibilidad',
    servicio_solicitante: 'Urgencias • Box 01 (Choque)',
    medico_prescriptor: 'Dr. Carlos Mendoza (Urgenciólogo)',
    sla_restante_min: 8,
    hora_solicitud: '10:41',
    analizador_asignado: 'Cobas b221 & e411'
  },
  {
    id: 'est-2',
    folio: 'EST-2026-9045',
    codigo_tubo: 'BC-EDTA-10294',
    paciente_nombre: 'Miguel Romero Paredes',
    paciente_iniciales: 'M.R.P.',
    paciente_edad: 35,
    paciente_sexo: 'M',
    prioridad: 'URGENTE',
    codigo_color: 'NARANJA',
    estado: 'PENDIENTE',
    categoria: 'LABORATORIO',
    estudio_requerido: 'Biometría Hemática + Química 6 Elementos',
    servicio_solicitante: 'Urgencias • Box 04 (Observación)',
    medico_prescriptor: 'Dra. Elena Ríos (Medicina Interna)',
    sla_restante_min: 18,
    hora_solicitud: '10:32',
    analizador_asignado: 'Sysmex XN-1000'
  },
  {
    id: 'est-3',
    folio: 'EST-2026-9038',
    paciente_nombre: 'Alejandro Salinas Garza',
    paciente_iniciales: 'A.S.G.',
    paciente_edad: 28,
    paciente_sexo: 'M',
    prioridad: 'ORDINARIA',
    codigo_color: 'GRIS',
    estado: 'PENDIENTE',
    categoria: 'IMAGENOLOGIA',
    estudio_requerido: 'Radiografía de Tórax AP y Lateral',
    servicio_solicitante: 'Consulta Externa • Consultorio 08',
    medico_prescriptor: 'Dr. Alberto Morales (Neumología)',
    sla_restante_min: 45,
    hora_solicitud: '10:15'
  },
  {
    id: 'est-4',
    folio: 'EST-2026-9047',
    paciente_nombre: 'Jorge Luis Morales',
    paciente_iniciales: 'J.L.M.',
    paciente_edad: 58,
    paciente_sexo: 'M',
    prioridad: 'STAT',
    codigo_color: 'ROJO',
    estado: 'EN_PROCESO',
    categoria: 'LABORATORIO',
    estudio_requerido: 'Perfil Cardíaco Enzimático + Dímero D',
    servicio_solicitante: 'Urgencias • Box 02 (Choque)',
    medico_prescriptor: 'Dra. Verónica Peña',
    sla_restante_min: 12,
    hora_solicitud: '10:38',
    analizador_asignado: 'Roche Cobas e411'
  },
  {
    id: 'est-5',
    folio: 'EST-2026-9049',
    paciente_nombre: 'Sofía Fuentes Castro',
    paciente_iniciales: 'S.F.C.',
    paciente_edad: 23,
    paciente_sexo: 'F',
    prioridad: 'STAT',
    codigo_color: 'ROJO',
    estado: 'PENDIENTE',
    categoria: 'LABORATORIO',
    estudio_requerido: 'Prueba de Compatibilidad Cruzada Urgente (Banco Sangre)',
    servicio_solicitante: 'Quirófano Central • QX-2',
    medico_prescriptor: 'Dr. Sergio Navarro (Cirugía)',
    sla_restante_min: 14,
    hora_solicitud: '10:45'
  },
  {
    id: 'est-6',
    folio: 'EST-2026-9032',
    paciente_nombre: 'Gloria Hernández Luna',
    paciente_iniciales: 'G.H.L.',
    paciente_edad: 51,
    paciente_sexo: 'F',
    prioridad: 'ORDINARIA',
    codigo_color: 'GRIS',
    estado: 'PENDIENTE',
    categoria: 'LABORATORIO',
    estudio_requerido: 'Perfil Lipídico + Química 32 Elementos',
    servicio_solicitante: 'Hospitalización • Piso 2 Cama 214',
    medico_prescriptor: 'Dra. Claudia Domínguez',
    sla_restante_min: 60,
    hora_solicitud: '09:40'
  }
])

// --- ORDEN SELECCIONADA PARA CARGA O DETALLE ---
const selectedOrder = ref<EstudioDiagnostico>(orders.value[0] as EstudioDiagnostico)

// --- KPIS GLOBALES DEL TURNO ---
const kpis = ref<LabKPIs>({
  validados_hoy: 68,
  sla_promedio_min: 14.2,
  cumplimiento_pct: 98.2,
  tasa_rechazo_pct: 0.4
})

// --- COMPUTED MÉTRICAS ---
const statCount = computed(() => orders.value.filter(o => o.prioridad === 'STAT').length)
const urgenteCount = computed(() => orders.value.filter(o => o.prioridad === 'URGENTE').length)
const totalPendientes = computed(() => orders.value.length)

// --- ACCIONES Y NAVEGACIÓN ---
const handleSelectTab = (tab: 'pendientes' | 'cargar' | 'busqueda' | 'historial') => {
  activeTab.value = tab
}

const handleCargarEstudio = (order: EstudioDiagnostico) => {
  selectedOrder.value = order
  activeTab.value = 'cargar'
}

const handleGuardarResultado = (payload: { folio: string; archivo: ArchivoResultado; observaciones: string }) => {
  const index = orders.value.findIndex(o => o.folio === payload.folio)
  if (index !== -1) {
    orders.value.splice(index, 1)
  }
  kpis.value.validados_hoy += 1
}

const handleRechazarMuestra = (payload: { folio: string; motivo: string }) => {
  const index = orders.value.findIndex(o => o.folio === payload.folio)
  if (index !== -1) {
    orders.value.splice(index, 1)
  }
  alert(`La muestra del estudio ${payload.folio} ha sido registrada con causal técnica de rechazo: "${payload.motivo}". Se emitió notificación prioritaria al servicio solicitante.`)
}

// --- ATAJOS DE TECLADO GLOBALES (Alt+P, Alt+C, Alt+B, Alt+H) ---
const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.altKey) {
    const key = e.key.toLowerCase()
    if (key === 'p') {
      e.preventDefault()
      activeTab.value = 'pendientes'
    } else if (key === 'c') {
      e.preventDefault()
      activeTab.value = 'cargar'
    } else if (key === 'b') {
      e.preventDefault()
      activeTab.value = 'busqueda'
    } else if (key === 'h') {
      e.preventDefault()
      activeTab.value = 'historial'
    }
  }
}

onMounted(() => {
  updateClock()
  clockInterval = window.setInterval(updateClock, 30000)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  if (clockInterval) clearInterval(clockInterval)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="lab-app-container">
    <!-- 1. ENCABEZADO PERSISTENTE -->
    <LabHeader
      :user="user"
      :current-date-time-str="currentDateTimeStr"
      :total-pendientes="totalPendientes"
      :stat-count="statCount"
    />

    <!-- 2. LAYOUT PRINCIPAL: SIDEBAR + WORKSPACE -->
    <div class="lab-main-layout">
      <!-- SIDEBAR -->
      <LabSidebar
        :active-tab="activeTab"
        :stat-count="statCount"
        :total-count="totalPendientes"
        @select-tab="handleSelectTab"
      />

      <!-- ÁREA DE CONTENIDO -->
      <main class="lab-workspace-content">
        <!-- BANNER DE ALERTA UNIFICADO -->
        <LabSlaBanner
          v-if="statCount > 0"
          :stat-count="statCount"
          :urgente-count="urgenteCount"
          tiempo-promedio-stat="11.4 min"
          vencimiento-proximo-min="08:00 min"
        />

        <!-- PANTALLA 1: BANDEJA DE PENDIENTES (Alt+P) -->
        <LabPendientesSection
          v-if="activeTab === 'pendientes'"
          :orders="orders"
          @cargar-estudio="handleCargarEstudio"
        />

        <!-- PANTALLA 2: CARGA Y VALIDACIÓN DE RESULTADOS (Alt+C) -->
        <LabCargarResultadosSection
          v-else-if="activeTab === 'cargar'"
          :selected-order="selectedOrder"
          @guardar-resultado="handleGuardarResultado"
          @rechazar-muestra="handleRechazarMuestra"
        />

        <!-- PANTALLA 3: BÚSQUEDA Y LOCALIZACIÓN DE ESTUDIOS (Alt+B) -->
        <LabBusquedaFolioSection
          v-else-if="activeTab === 'busqueda'"
          :active-study="selectedOrder"
          @cargar-estudio="handleCargarEstudio"
        />

        <!-- PANTALLA 4: HISTORIAL Y BITÁCORA DE CARGAS VALIDADAS (Alt+H) -->
        <LabHistorialBitacoraSection
          v-else-if="activeTab === 'historial'"
          :kpis="kpis"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
.lab-app-container {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #0f172a;
}

.lab-main-layout {
  display: flex;
  flex: 1;
}

.lab-workspace-content {
  flex: 1;
  padding: 1.25rem 1.5rem;
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
