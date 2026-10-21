<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import DoctorHeader from './components/DoctorHeader.vue'
import DoctorSlaBanner from './components/DoctorSlaBanner.vue'
import DoctorSidebar from './components/DoctorSidebar.vue'
import ClinicalRecordSection from './components/ClinicalRecordSection.vue'
import PrescriptionPanel from './components/PrescriptionPanel.vue'
import NuevaNotaEceSection from './components/NuevaNotaEceSection.vue'
import EmitirRecetaSection from './components/EmitirRecetaSection.vue'
import SolicitarLaboratorioSection from './components/SolicitarLaboratorioSection.vue'
import ReclasificarTriageDoctorSection from './components/ReclasificarTriageDoctorSection.vue'
import ColaEsperaDoctorSection from './components/ColaEsperaDoctorSection.vue'
import DoctorPlaceholderSection from './components/DoctorPlaceholderSection.vue'
import type { Paciente, Doctor, Receta } from './types'
import authService from '@/services/authService'

// --- DATOS DEL MÉDICO EN SESIÓN (DESDE API SI EXISTE) ---
const usuarioAuth = authService.getUsuario()
const doctor = ref<Doctor>({
  nombre: usuarioAuth?.nombre_completo || 'Dr. Carlos Mendoza',
  cedula: usuarioAuth?.cedula ? `CÉD. ${usuarioAuth.cedula}` : 'CED. DEMO-000001',
  box: 'Box 04'
})

// --- RELOJ Y FECHA EN TIEMPO REAL ---
const currentDateTimeStr = ref('')
let timerId: number | undefined

const updateClock = () => {
  const now = new Date()
  const opcionesFecha: Intl.DateTimeFormatOptions = { 
    day: 'numeric', 
    month: 'short', 
    year: 'numeric' 
  }
  const fecha = now.toLocaleDateString('es-MX', opcionesFecha).replace('.', '')
  const horas = String(now.getHours()).padStart(2, '0')
  const minutos = String(now.getMinutes()).padStart(2, '0')
  const segundos = String(now.getSeconds()).padStart(2, '0')
  currentDateTimeStr.value = `${fecha} · ${horas}:${minutos}:${segundos} h`
}

// --- LISTA DE PACIENTES EN COLA ---
const pacientesCola = ref<Paciente[]>([
  {
    id: 'PAC-1',
    nombre: 'Carlos Mendoza Ruiz',
    folio: 'PAC-94821',
    ece: 'DEMO-0248',
    edad: 42,
    sexo: 'Hombre',
    box: 'Box 04',
    motivo: 'Fiebre de 24 h y opresión precordial',
    triage: 'Manchester II · Emergencia / Valoración prioritaria',
    triageColor: '#EF4444',
    limiteSla: 'Límite < 15 min desde triage',
    transcurridosMin: 48,
    restantesMin: 12,
    signos: {
      pa: '118/76',
      fc: '88',
      temp: '37.8',
      spo2: '97',
      horaToma: '10:40 h'
    },
    antecedentes: {
      motivo: 'Fiebre de 24 h de evolución, cefalea y sensación de opresión precordial leve sin disnea referida. Valoración y estabilización en Box de Urgencias.',
      alergias: 'Penicilina (confirmada - Riesgo anafilaxia)',
      cronicos: 'Hipertensión arterial en control.',
      medicacion: 'Losartán 50mg / 24h.'
    },
    ultimaNota: {
      titulo: 'Nota de evolución · Consulta externa',
      medico: 'Dra. Elena Ríos',
      folio: 'NOTA-DEMO-018',
      fecha: '15 oct 2026 · 09:20 h',
      contenido: 'Paciente en seguimiento por cefalea tensional, con mejoría referida. Sin fiebre ni síntomas respiratorios en esa consulta. Se indicaron medidas generales y vigilancia.'
    }
  },
  {
    id: 'PAC-2',
    nombre: 'Mateo Ruiz',
    folio: 'PAC-94822',
    ece: 'DEMO-0249',
    edad: 35,
    sexo: 'Hombre',
    box: 'Box 02',
    motivo: 'Dolor abdominal · 35 años',
    triage: 'III · Amarillo',
    triageColor: '#F59E0B',
    limiteSla: 'Límite < 60 min desde triage',
    transcurridosMin: 32,
    restantesMin: 28,
    signos: {
      pa: '125/80',
      fc: '92',
      temp: '36.9',
      spo2: '98',
      horaToma: '11:10 h'
    },
    antecedentes: {
      motivo: 'Dolor abdominal en fosa ilíaca derecha de inicio súbito.',
      alergias: 'Ninguna conocida (NKDA)',
      cronicos: 'Sin antecedentes crónicos de relevancia.',
      medicacion: 'Ninguna habitual.'
    }
  },
  {
    id: 'PAC-3',
    nombre: 'Ana Solís',
    folio: 'PAC-94823',
    ece: 'DEMO-0250',
    edad: 28,
    sexo: 'Mujer',
    box: 'Box 01',
    motivo: 'Esguince de tobillo derecho',
    triage: 'IV · Verde',
    triageColor: '#10B981',
    limiteSla: 'Límite < 120 min desde triage',
    transcurridosMin: 21,
    restantesMin: 99
  }
])

// Paciente seleccionado (inicia en null para no tener datos fijos)
const pacienteSeleccionado = ref<Paciente | null>(null)

// Diagnóstico clínico unificado y compartido entre módulos
const diagnosticoGlobal = ref('')

const seleccionarPaciente = (p: Paciente) => {
  pacienteSeleccionado.value = p
  if (p.motivo && !diagnosticoGlobal.value) {
    diagnosticoGlobal.value = p.motivo
  }
}

const ingresarConsultaDesdeCola = (p: Paciente) => {
  pacienteSeleccionado.value = p
  seccionActiva.value = 'atencion'
}

const deseleccionarPaciente = () => {
  pacienteSeleccionado.value = null
}

// Control de secciones activas de la barra lateral
const seccionActiva = ref<'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'>('nota')

const cambiarSeccion = (sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola') => {
  seccionActiva.value = sec
}

// Formulario de emisión de receta en panel lateral
const receta = ref<Receta>({
  medicamento: '',
  dosis: '',
  via: 'Oral',
  frecuencia: '',
  duracion: '',
  indicaciones: ''
})

const emitirReceta = () => {
  alert('Prescripción emitida con éxito.')
}

// Atajos de teclado del módulo médico
const handleKeyboardShortcuts = (e: KeyboardEvent) => {
  if (e.altKey && (e.key === 'e' || e.key === 'E')) {
    e.preventDefault()
    cambiarSeccion('atencion')
  } else if (e.altKey && (e.key === 'n' || e.key === 'N')) {
    e.preventDefault()
    cambiarSeccion('nota')
  } else if (e.altKey && (e.key === 'r' || e.key === 'R')) {
    e.preventDefault()
    cambiarSeccion('receta')
  } else if (e.altKey && (e.key === 'l' || e.key === 'L')) {
    e.preventDefault()
    cambiarSeccion('lab')
  } else if (e.altKey && (e.key === 't' || e.key === 'T')) {
    e.preventDefault()
    cambiarSeccion('triage')
  }
}

onMounted(() => {
  updateClock()
  timerId = window.setInterval(updateClock, 1000)
  window.addEventListener('keydown', handleKeyboardShortcuts)
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
  window.removeEventListener('keydown', handleKeyboardShortcuts)
})
</script>

<template>
  <div class="doctor-workspace-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <DoctorHeader 
      :doctor="doctor"
      :currentDateTimeStr="currentDateTimeStr"
    />

    <!-- 2. BANNER DE PRIORIDAD SLA (SOLO EN EXPEDIENTE CLÍNICO) -->
    <DoctorSlaBanner 
      v-if="seccionActiva === 'atencion'"
      :pacienteSeleccionado="pacienteSeleccionado"
      @deseleccionarPaciente="deseleccionarPaciente"
    />

    <!-- 3. WORKSPACE: GRID ADAPTABLE (3 COLUMNAS EN EXPEDIENTE, 2 COLUMNAS EN PANTALLAS DE TRABAJO) -->
    <div class="workspace-grid" :class="{ 'two-columns-layout': seccionActiva !== 'atencion' }">
      
      <!-- ================= COLUMNA 1: SIDEBAR HERRAMIENTAS Y COLA ================= -->
      <DoctorSidebar 
        :seccionActiva="seccionActiva"
        :pacientesCola="pacientesCola"
        :pacienteSeleccionado="pacienteSeleccionado"
        @cambiarSeccion="cambiarSeccion"
        @seleccionarPaciente="seleccionarPaciente"
      />

      <!-- ================= CONTENIDO CENTRAL DINÁMICO ================= -->
      <main class="center-workspace">
        <!-- PANTALLA 1: EXPEDIENTE CLÍNICO -->
        <ClinicalRecordSection 
          v-if="seccionActiva === 'atencion'"
          :pacienteSeleccionado="pacienteSeleccionado"
        />

        <!-- PANTALLA 2: NUEVA NOTA MÉDICA ECE (CAPTURA 1) -->
        <NuevaNotaEceSection
          v-else-if="seccionActiva === 'nota'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
        />

        <!-- PANTALLA 3: MÓDULO DE PRESCRIPCIÓN ELECTRÓNICA (CAPTURA 2) -->
        <EmitirRecetaSection
          v-else-if="seccionActiva === 'receta'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @emitirReceta="emitirReceta"
        />

        <!-- PANTALLA 4: SOLICITUD DE LABORATORIO E IMAGEN (CAPTURA 3) -->
        <SolicitarLaboratorioSection
          v-else-if="seccionActiva === 'lab'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @enviarSolicitud="emitirReceta"
        />

        <!-- PANTALLA 5: RECLASIFICACIÓN DE TRIAGE (CAPTURA 4) -->
        <ReclasificarTriageDoctorSection
          v-else-if="seccionActiva === 'triage'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 6: COLA DE ESPERA Y FLUJO DE DEMANDA (CAPTURA 5) -->
        <ColaEsperaDoctorSection 
          v-else-if="seccionActiva === 'cola'"
          :doctor="doctor"
          :pacientesCola="pacientesCola"
          :pacienteSeleccionado="pacienteSeleccionado"
          @seleccionarPaciente="seleccionarPaciente"
          @ingresarConsulta="ingresarConsultaDesdeCola"
        />

        <DoctorPlaceholderSection 
          v-else
          :seccionActiva="seccionActiva"
          :pacientesCola="pacientesCola"
          @cambiarSeccion="cambiarSeccion"
          @seleccionarPaciente="seleccionarPaciente"
        />
      </main>

      <!-- ================= COLUMNA 3: RESUMEN TERAPÉUTICO (SOLO EN EXPEDIENTE CLÍNICO) ================= -->
      <PrescriptionPanel 
        v-if="seccionActiva === 'atencion'"
        :pacienteSeleccionado="pacienteSeleccionado"
        :doctor="doctor"
        @cambiarSeccion="cambiarSeccion"
      />

    </div>
  </div>
</template>

<style scoped>
.doctor-workspace-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #F8FAFC;
  color: #1E293B;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 340px;
  gap: 1.2rem;
  padding: 1rem clamp(0.75rem, 2vw, 1.5rem);
  flex: 1;
  align-items: start;
}

.workspace-grid.two-columns-layout {
  grid-template-columns: 240px minmax(0, 1fr);
}

.center-workspace {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

@media (max-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 220px 1fr 300px;
    gap: 0.75rem;
  }
}

@media (max-width: 1000px) {
  .workspace-grid {
    grid-template-columns: 220px 1fr;
  }
}

@media (max-width: 768px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
