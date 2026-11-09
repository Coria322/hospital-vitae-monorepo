<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import NurseHeader from './components/NurseHeader.vue'
import NurseSidebar from './components/NurseSidebar.vue'
import TriageSection from './components/TriageSection.vue'
import ReclasificarTriageSection from './components/ReclasificarTriageSection.vue'
import SalaEsperaMonitoreoSection from './components/SalaEsperaMonitoreoSection.vue'
import NotaEnfermeriaSection from './components/NotaEnfermeriaSection.vue'
import authService from '@/services/authService'
import guardiasService from '@/services/guardiasService'

//import servicio de la cola viva
import triageService from '@/services/triageService'

// --- DATOS DEL PERSONAL DE ENFERMERÍA EN SESIÓN (DESDE API SI EXISTE) ---
const usuarioAuth = authService.getUsuario()
const boxInicialEnfermero = guardiasService.obtenerBoxAsignadoEnfermero(
  usuarioAuth?.id_usuario,
  usuarioAuth?.nombre_completo
)
const tieneBoxAsignado = ref(guardiasService.estaPersonalAsignado(
  usuarioAuth?.id_usuario,
  usuarioAuth?.nombre_completo,
  'ENFERMERIA',
  usuarioAuth?.cedula || undefined
))
const enfermera = ref({
  nombre: usuarioAuth?.nombre_completo || 'Lic. Gabriela Ramos',
  cedula: usuarioAuth?.cedula ? `CÉD. ${usuarioAuth.cedula}` : 'CÉD. ENF-049821',
  servicio: boxInicialEnfermero || 'Sin Box Asignado (Bloqueado)'
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
  fechaIngreso?: string
  slaVencimiento?: string
  slaVencido?: boolean
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

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/triage/cola (triageService.obtenerColaUrgencias)


// Lista reactiva conectada al servidor
const pacientesEnEspera = ref<PacienteEnfermeria[]>([])
const cargandoCola = ref(false)

// Función para obtener la cola viva desde el backend
const cargarColaUrgencias = async () => {
  try {
    cargandoCola.value = true
    const cola = await triageService.obtenerCola()
    
    // Mapear los datos canónicos de la API al modelo visual de la sala
    pacientesEnEspera.value = cola.map(item => {
      const pa = (item.signos?.presion_art || '120/80').split('/')
      const colorHex = 
        item.color_asig === 'ROJO' ? '#EF4444' :
        item.color_asig === 'NARANJA' ? '#EA580C' :
        item.color_asig === 'AMARILLO' ? '#D97706' :
        item.color_asig === 'VERDE' ? '#10B981' : '#3B82F6'

      return {
        id: item.id_triage,
        nombre: item.paciente_nombre,
        edad: (item as any).paciente_edad ?? null,
        sexo: (item as any).paciente_sexo ?? '',
        folio: item.folio_ingreso,
        box: item.sala_nombre || 'Sala de Espera General',
        triage: `Nivel ${item.prioridad_nivel} · ${item.color_asig}`,
        triageColor: colorHex,
        tiempoEspera: item.minutos_espera,
        fechaIngreso: item.fecha_hora_ingreso,
        slaVencimiento: item.sla_vencimiento_en,
        slaVencido: item.sla_vencido,
        signos: {
          paSistolica: pa[0] || '120',
          paDiastolica: pa[1] || '80',
          fc: String(item.signos?.frecuencia_card || 80),
          fr: String(item.signos?.frecuencia_resp || 16),
          spo2: String(item.signos?.spo2 || 98),
          temp: String(item.signos?.temperatura || 36.5),
          glucosa: String(item.signos?.nivel_glucosa || ''),
          avdi: (item.signos?.nivel_conciencia === 'INCONSCIENTE' ? 'I' : 
                 item.signos?.nivel_conciencia === 'DOLOR' ? 'D' : 
                 item.signos?.nivel_conciencia === 'VERBAL' ? 'V' : 'A') as 'A' | 'V' | 'D' | 'I',
          evaDolor: item.signos?.escala_eva || 0
        },
        motivoConsulta: 'Ingreso valorado por triage Manchester',
        alergias: '',
        enfermedadesPrevias: ''
      }
    })
  } catch (error) {
    console.error('Error al cargar la cola viva de urgencias:', error)
  } finally {
    cargandoCola.value = false
  }
}

// Función auxiliar de colores institucionales
const obtenerHexColor = (color: string) => {
  switch (color) {
    case 'ROJO': return '#EF4444'
    case 'NARANJA': return '#EA580C'
    case 'AMARILLO': return '#D97706'
    case 'VERDE': return '#10B981'
    case 'AZUL': return '#2563EB'
    default: return '#64748B'
  }
}

// --- NAVEGACIÓN ENTRE PANTALLAS DE ENFERMERÍA ---
// Al ingresar al sistema debe ubicar en Sala de Espera y Monitoreo
const seccionActiva = ref<'triage' | 'reclasificar' | 'sala' | 'nota'>('sala')

const tienePacienteSeleccionado = computed(() => {
  return Boolean(pacienteActivo.value?.id && pacienteActivo.value?.nombre)
})

const cambiarSeccion = (sec: 'triage' | 'reclasificar' | 'sala' | 'nota') => {
  if ((sec === 'reclasificar' || sec === 'nota') && !tienePacienteSeleccionado.value) {
    alert('Acceso restringido: Debe seleccionar primero a un paciente desde la "Sala de Espera y Monitoreo" para acceder a esta sección.')
    return
  }
  seccionActiva.value = sec
}

// TODO: [PENDIENTE-CONEXION-API] Conectar con endpoint real POST /api/v1/triage (triageService.clasificarTriage)
const registrarTriage = async (datos: any) => {
  pacienteActivo.value = {
    ...pacienteActivo.value,
    ...datos
  }
  // Refrescar inmediatamente la cola real desde el servidor
  await cargarColaUrgencias()
  seccionActiva.value = 'sala'
}

// Egreso hospitalario: Quitar al paciente de Sala de Espera y Monitoreo
const procesarEgresoPaciente = async (idTriageOId: string | number) => {
  const targetId = String(idTriageOId || pacienteActivo.value.id || pacienteActivo.value.folio)

  try {
    if (targetId) {
      await triageService.egresarPaciente(targetId, 'Nota de Egreso Hospitalario de Enfermería').catch(() => null)
    }
  } catch (e) {
    console.warn('Aviso egreso:', e)
  }

  pacientesEnEspera.value = pacientesEnEspera.value.filter(p => p.id !== idTriageOId && p.id !== targetId && p.folio !== pacienteActivo.value.folio)

  pacienteActivo.value = {
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
  }

  await cargarColaUrgencias()
  seccionActiva.value = 'sala'
}

const seleccionarPacienteParaReclasificar = (p: PacienteEnfermeria) => {
  pacienteActivo.value = { ...p }
  seccionActiva.value = 'reclasificar'
}

const seleccionarPacienteParaNota = (p: PacienteEnfermeria) => {
  pacienteActivo.value = { ...p }
  seccionActiva.value = 'nota'
}

// TODO: [PENDIENTE-CONEXION-API] Conectar con endpoint real POST /api/v1/triage/reclasificar (triageService.reclasificarTriage)
const actualizarReclasificacion = async (datosReval: any) => {
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
      pacientesEnEspera.value[idx] = { ...pacientesEnEspera.value[idx], ...pacienteActivo.value }
    }
  }
  await cargarColaUrgencias()
  seccionActiva.value = 'sala'
}

const activarChoqueInmediato = () => {
  alert('ALERTA ROJA: Protocolo de Reanimación / Choque activado. Sala 01 notificada en tiempo real.')
}

const activarCodigoAzul = () => {
  alert('CÓDIGO AZUL ACTIVADO: Alarma general de paro cardiorrespiratorio en Sala de Espera.')
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

let cerrarStreamSse: (() => void) | null = null

onMounted(() => {
  updateClock()
  timerId = window.setInterval(updateClock, 1000)
  window.addEventListener('keydown', handleKeyboardShortcuts)
  // Cargar pacientes de urgencias al abrir la vista
  cargarColaUrgencias()
  try {
    cerrarStreamSse = triageService.conectarStream(() => {
      cargarColaUrgencias()
    })
  } catch (e) {
    console.warn('SSE stream no disponible en enfermeria:', e)
  }
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
  window.removeEventListener('keydown', handleKeyboardShortcuts)
  if (cerrarStreamSse) {
    cerrarStreamSse()
    cerrarStreamSse = null
  }
})
</script>

<template>
  <div class="nurse-workspace-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <NurseHeader 
      :enfermera="enfermera"
      :currentDateTimeStr="currentDateTimeStr"
    />

    <!-- BANNER DE ADVERTENCIA: SIN BOX ASIGNADO -->
    <div v-if="!tieneBoxAsignado" class="session-warning-banner-nurse">
      <div class="warning-icon-box">
        <svg viewBox="0 0 24 24" class="svg-warning" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
      </div>
      <div class="warning-text-block">
        <strong>ESTADO EN ADVERTENCIA: ACCESO CLÍNICO RESTRINGIDO</strong>
        <p>El personal de enfermería {{ enfermera.nombre }} no cuenta con box o puesto asignado en el turno actual. Las acciones asistenciales de triage y evaluación fisiológica se encuentran restringidas hasta que Administración le asigne puesto físico.</p>
      </div>
    </div>


    <!-- 2. WORKSPACE DE 2 COLUMNAS (SIDEBAR IZQUIERDA + CONTENIDO CENTRAL) -->
    <div class="nurse-grid">
      <!-- SIDEBAR IZQUIERDA -->
      <NurseSidebar 
        :seccionActiva="seccionActiva"
        :tienePacienteSeleccionado="tienePacienteSeleccionado"
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
          @darEgreso="procesarEgresoPaciente"
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

.session-warning-banner-nurse {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #fff1f2;
  border-bottom: 2px solid #e11d48;
  padding: 0.85rem 1.5rem;
  box-shadow: 0 2px 4px rgba(225, 29, 72, 0.08);
}

.warning-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e11d48;
  flex-shrink: 0;
}

.warning-icon-box svg {
  width: 20px;
  height: 20px;
}

.warning-text-block strong {
  display: block;
  font-size: 0.85rem;
  color: #9f1239;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.warning-text-block p {
  margin: 0.15rem 0 0 0;
  font-size: 0.78rem;
  color: #be123c;
}

</style>
