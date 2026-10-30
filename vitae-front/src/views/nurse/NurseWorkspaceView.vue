<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import NurseHeader from './components/NurseHeader.vue'
import NurseSidebar from './components/NurseSidebar.vue'
import TriageSection from './components/TriageSection.vue'
import ReclasificarTriageSection from './components/ReclasificarTriageSection.vue'
import SalaEsperaMonitoreoSection from './components/SalaEsperaMonitoreoSection.vue'
import NotaEnfermeriaSection from './components/NotaEnfermeriaSection.vue'
import authService from '@/services/authService'

// --- DATOS DEL PERSONAL DE ENFERMERÍA EN SESIÓN (DESDE API SI EXISTE) ---
const usuarioAuth = authService.getUsuario()
const enfermera = ref({
  nombre: usuarioAuth?.nombre_completo || 'Lic. Gabriela Ramos',
  cedula: usuarioAuth?.cedula ? `CÉD. ${usuarioAuth.cedula}` : 'CÉD. ENF-049821',
  servicio: 'Urgencias · Box de Triage'
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

// --- ESTADO PERSISTENTE DEL PACIENTE EN ATENCIÓN ---
export interface PacienteEnfermeria {
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
    glucosa: string
    avdi: 'A' | 'V' | 'D' | 'I'
    evaDolor: number | string
  }
  motivoConsulta: string
  alergias: string
  enfermedadesPrevias: string
}

const pacienteActivo = ref<PacienteEnfermeria>({
  id: '',
  nombre: '',
  edad: null,
  sexo: '',
  folio: '',
  box: 'Box 03 · Urgencias',
  triage: '',
  triageColor: '',
  signos: {
    paSistolica: '',
    paDiastolica: '',
    fc: '',
    fr: '',
    spo2: '',
    temp: '',
    glucosa: '',
    avdi: 'A',
    evaDolor: 0
  },
  motivoConsulta: '',
  alergias: '',
  enfermedadesPrevias: ''
})

// Lista reactiva de pacientes en sala de espera
const pacientesEnEspera = ref<PacienteEnfermeria[]>([
  {
    id: 1,
    nombre: 'Elena Torres Morales',
    edad: 54,
    sexo: 'Femenino',
    folio: 'FOL-TRI-8492',
    box: 'Sala de Espera A',
    triage: 'Nivel III · Urgente',
    triageColor: '#D97706',
    tiempoEspera: 28,
    signos: {
      paSistolica: '135',
      paDiastolica: '85',
      fc: '88',
      fr: '18',
      spo2: '96',
      temp: '37.1',
      glucosa: '110',
      avdi: 'A',
      evaDolor: 6
    },
    motivoConsulta: 'Dolor abdominal cólico progresivo en cuadrante inferior derecho de 6 hrs.',
    alergias: 'Penicilina',
    enfermedadesPrevias: 'Hipertensión arterial'
  },
  {
    id: 2,
    nombre: 'Carlos Mendoza Vega',
    edad: 68,
    sexo: 'Masculino',
    folio: 'FOL-TRI-7310',
    box: 'Sala de Espera A',
    triage: 'Nivel II · Muy Urgente',
    triageColor: '#EA580C',
    tiempoEspera: 12,
    signos: {
      paSistolica: '150',
      paDiastolica: '95',
      fc: '104',
      fr: '22',
      spo2: '93',
      temp: '36.8',
      glucosa: '145',
      avdi: 'A',
      evaDolor: 8
    },
    motivoConsulta: 'Dolor torácico opresivo con irradiación a brazo izquierdo.',
    alergias: 'Sulfas',
    enfermedadesPrevias: 'Diabetes Mellitus tipo 2'
  }
])

// --- NAVEGACIÓN ENTRE PANTALLAS DE ENFERMERÍA ---
const seccionActiva = ref<'triage' | 'reclasificar' | 'sala' | 'nota'>('triage')

const cambiarSeccion = (sec: 'triage' | 'reclasificar' | 'sala' | 'nota') => {
  seccionActiva.value = sec
}

const registrarTriage = (datos: any) => {
  pacienteActivo.value = {
    ...pacienteActivo.value,
    ...datos
  }
  if (datos.nombre) {
    const yaExiste = pacientesEnEspera.value.find(p => p.folio === datos.folio)
    if (!yaExiste) {
      pacientesEnEspera.value.unshift({ ...pacienteActivo.value })
    }
  }
}

const seleccionarPacienteParaReclasificar = (p: PacienteEnfermeria) => {
  pacienteActivo.value = { ...p }
  seccionActiva.value = 'reclasificar'
}

const seleccionarPacienteParaNota = (p: PacienteEnfermeria) => {
  pacienteActivo.value = { ...p }
  seccionActiva.value = 'nota'
}

const actualizarReclasificacion = (datosReval: any) => {
  if (pacienteActivo.value) {
    pacienteActivo.value.triage = datosReval.nuevoNivel
    pacienteActivo.value.triageColor = datosReval.nuevoColor
    if (datosReval.signos) {
      pacienteActivo.value.signos = {
        ...pacienteActivo.value.signos,
        ...datosReval.signos
      }
    }
    const idx = pacientesEnEspera.value.findIndex(p => p.folio === pacienteActivo.value.folio || p.nombre === pacienteActivo.value.nombre)
    if (idx !== -1) {
      pacientesEnEspera.value[idx] = { ...pacienteActivo.value }
    }
  }
}

const activarChoqueInmediato = () => {
  alert('<svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> ALERTA ROJA: Protocolo de Reanimación / Choque activado. Sala 01 notificada en tiempo real.')
}

const activarCodigoAzul = () => {
  alert('<svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> CÓDIGO AZUL ACTIVADO: Alarma general de paro cardiorrespiratorio en Sala de Espera.')
}

// Atajos de teclado del módulo
const handleKeyboardShortcuts = (e: KeyboardEvent) => {
  if (e.altKey && (e.key === 's' || e.key === 'S')) {
    e.preventDefault()
    cambiarSeccion('triage')
  } else if (e.altKey && (e.key === 'e' || e.key === 'E')) {
    e.preventDefault()
    cambiarSeccion('nota')
  } else if (e.altKey && (e.key === 'c' || e.key === 'C')) {
    e.preventDefault()
    activarChoqueInmediato()
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
  <div class="nurse-workspace-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <NurseHeader 
      :enfermera="enfermera"
      :currentDateTimeStr="currentDateTimeStr"
    />

    <!-- 2. WORKSPACE DE 2 COLUMNAS (SIDEBAR IZQUIERDA + CONTENIDO CENTRAL) -->
    <div class="nurse-grid">
      <!-- SIDEBAR IZQUIERDA -->
      <NurseSidebar 
        :seccionActiva="seccionActiva"
        @cambiarSeccion="cambiarSeccion"
        @activarChoque="activarChoqueInmediato"
      />

      <!-- CONTENIDO CENTRAL DINÁMICO (COMPONENTES MODULARES) -->
      <main class="center-content">
        <!-- PANTALLA 1: CAPTURAR SIGNOS TRIAGE (IMAGEN 1) -->
        <TriageSection 
          v-if="seccionActiva === 'triage'" 
          :pacienteActivo="pacienteActivo"
          @guardarTriage="registrarTriage"
          @activarChoque="activarChoqueInmediato"
        />

        <!-- PANTALLA 2: RECLASIFICACIÓN DINÁMICA (IMAGEN 2) -->
        <ReclasificarTriageSection 
          v-else-if="seccionActiva === 'reclasificar'" 
          :enfermera="enfermera"
          :pacienteActivo="pacienteActivo"
          @actualizarReclasificacion="actualizarReclasificacion"
          @activarChoque="activarChoqueInmediato"
        />

        <!-- PANTALLA 3: SALA DE ESPERA Y MONITOREO (IMAGEN 3) -->
        <SalaEsperaMonitoreoSection 
          v-else-if="seccionActiva === 'sala'" 
          :pacientes="pacientesEnEspera"
          @cambiarSeccion="cambiarSeccion"
          @abrirReclasificar="seleccionarPacienteParaReclasificar"
          @abrirNota="seleccionarPacienteParaNota"
          @activarCodigoAzul="activarCodigoAzul"
        />

        <!-- PANTALLA 4: NOTA DE ENFERMERÍA Y KARDEX (IMAGEN 4) -->
        <NotaEnfermeriaSection 
          v-else-if="seccionActiva === 'nota'" 
          :pacienteActivo="pacienteActivo"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ESTILOS DEL ESPACIO DE ENFERMERÍA Y TRIAGE ADAPTABLE */
.nurse-workspace-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #F8FAFC;
  color: #1E293B;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.nurse-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 1.2rem;
  padding: 1rem clamp(0.75rem, 2vw, 1.5rem);
  flex: 1;
  align-items: start;
}

.center-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

@media (max-width: 800px) {
  .nurse-grid {
    grid-template-columns: 1fr;
  }
}
</style>
