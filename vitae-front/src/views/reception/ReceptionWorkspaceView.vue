<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import ReceptionHeader from './components/ReceptionHeader.vue'
import ReceptionSidebar from './components/ReceptionSidebar.vue'
import CurpSearchSection from './components/CurpSearchSection.vue'
import PatientRegisterSection from './components/PatientRegisterSection.vue'
import AppointmentBookingSection from './components/AppointmentBookingSection.vue'
import InformedConsentSection from './components/InformedConsentSection.vue'
import WaitingRoomMonitorSection from './components/WaitingRoomMonitorSection.vue'
import BedCensusSection from './components/BedCensusSection.vue'
import authService from '@/services/authService'

// --- DATOS DEL PERSONAL DE ADMISIÓN / RECEPCIÓN EN SESIÓN (DESDE API SI EXISTE) ---
const usuarioAuth = authService.getUsuario()
const recepcionista = ref({
  nombre: usuarioAuth?.nombre_completo || 'Lic. Andrea Morales',
  cedula: usuarioAuth?.cedula ? `CÉD. ${usuarioAuth.cedula}` : 'CÉD. ADM-102941',
  cargo: 'Admisión y Archivo Clínico',
  sede: 'Sede Principal Norte'
})

// --- RELOJ Y FECHA EN TIEMPO REAL ---
const currentDateTimeStr = ref('')
const horaActual = ref('')
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
  horaActual.value = `${horas}:${minutos}:${segundos}`
}

// --- NAVEGACIÓN ENTRE SECCIONES DE RECEPCIÓN ---
type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'
const seccionActiva = ref<SeccionRecepcion>('curp')

// Estado compartido de paciente y CURP entre módulos de admisión
const curpCompartida = ref('')
const pacienteSeleccionado = ref<any | null>(null)

const cambiarSeccion = (sec: SeccionRecepcion) => {
  seccionActiva.value = sec
}

const onPacienteSeleccionado = (paciente: any) => {
  pacienteSeleccionado.value = paciente
}

const onCurpSeleccionada = (curp: string) => {
  curpCompartida.value = curp
}

// Atajos de teclado del módulo
const handleKeyboardShortcuts = (e: KeyboardEvent) => {
  if (e.altKey && (e.key === 'b' || e.key === 'B')) {
    e.preventDefault()
    cambiarSeccion('curp')
  } else if (e.altKey && (e.key === 'p' || e.key === 'P')) {
    e.preventDefault()
    cambiarSeccion('registrar')
  } else if (e.altKey && (e.key === 'c' || e.key === 'C')) {
    e.preventDefault()
    cambiarSeccion('agendar')
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
  <div class="reception-workspace-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <ReceptionHeader 
      :recepcionista="recepcionista"
      :currentDateTimeStr="currentDateTimeStr"
      :horaActual="horaActual"
    />

    <!-- 2. CONTENIDO PRINCIPAL: SIDEBAR + ESPACIO CENTRAL -->
    <div class="reception-main-layout">
      <!-- SIDEBAR LATERAL IZQUIERDA -->
      <ReceptionSidebar 
        :seccionActiva="seccionActiva"
        :recepcionista="recepcionista"
        @cambiarSeccion="cambiarSeccion"
      />

      <!-- ÁREA CENTRAL DE TRABAJO -->
      <main class="reception-content-area">
        <!-- PANTALLA 1: BÚSQUEDA POR CURP (IMAGEN 1) -->
        <CurpSearchSection 
          v-if="seccionActiva === 'curp'" 
          @cambiarSeccion="cambiarSeccion"
          @seleccionarPaciente="onPacienteSeleccionado"
          @curpSeleccionada="onCurpSeleccionada"
        />

        <!-- PANTALLA 2: REGISTRAR NUEVO PACIENTE (IMAGEN 2) -->
        <PatientRegisterSection 
          v-else-if="seccionActiva === 'registrar'" 
          :curpCompartida="curpCompartida"
          :pacienteSeleccionado="pacienteSeleccionado"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 3: PROGRAMACIÓN Y ASIGNACIÓN DE CITAS MÉDICAS (IMAGEN 3) -->
        <AppointmentBookingSection 
          v-else-if="seccionActiva === 'agendar'" 
          :curpCompartida="curpCompartida"
          :pacienteSeleccionado="pacienteSeleccionado"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 4: GESTIÓN DE CONSENTIMIENTOS INFORMADOS (IMAGEN 4) -->
        <InformedConsentSection 
          v-else-if="seccionActiva === 'consentimiento'" 
          :recepcionista="recepcionista"
          :pacienteSeleccionado="pacienteSeleccionado"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 5: MONITOR GENERAL DE SALA DE ESPERA Y TURNOS (IMAGEN 5) -->
        <WaitingRoomMonitorSection 
          v-else-if="seccionActiva === 'monitor'" 
          :horaActual="horaActual"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 6: CENSO HOSPITALARIO Y GESTIÓN DE CAMAS (IMAGEN 6) -->
        <BedCensusSection 
          v-else-if="seccionActiva === 'censo'" 
          :horaActual="horaActual"
          @cambiarSeccion="cambiarSeccion"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
.reception-workspace-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #F8FAFC;
  color: #1E293B;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.reception-main-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  flex: 1;
  min-height: calc(100vh - 60px);
}

.reception-content-area {
  padding: 1.25rem;
  overflow-y: auto;
}

@media (max-width: 1024px) {
  .reception-main-layout {
    grid-template-columns: 1fr;
  }
}
</style>
