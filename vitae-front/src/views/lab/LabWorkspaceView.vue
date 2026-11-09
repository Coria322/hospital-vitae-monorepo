<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { LabUser, EstudioDiagnostico, LabKPIs, ArchivoResultado, PrioridadEstudio, CodigoColor, EstadoEstudio, CategoriaEstudio } from './types'
import { laboratorioService, type SolicitudEstudioBackend } from '@/services/laboratorioService'
import { authService } from '@/services/authService'

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
const initialOrders: EstudioDiagnostico[] = [
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
]

const orders = ref<EstudioDiagnostico[]>([...initialOrders])
const historialEstudios = ref<any[]>([])
const todosLosEstudios = computed(() => {
  const histComoEstudios: EstudioDiagnostico[] = historialEstudios.value.map(h => ({
    id: h.id || h.folio,
    folio: h.folio,
    codigo_tubo: `BC-${h.folio.replace(/[^a-zA-Z0-9]/g, '').substring(0, 8)}`,
    paciente_nombre: h.paciente_nombre || h.paciente,
    paciente_iniciales: h.paciente || 'P.N.',
    paciente_edad: parseInt(h.edad) || 35,
    paciente_sexo: h.sexo === 'Femenino' ? 'F' : 'M',
    prioridad: 'ORDINARIA',
    codigo_color: 'GRIS',
    estado: 'VALIDADO',
    categoria: h.formato === 'DICOM' ? 'IMAGENOLOGIA' : 'LABORATORIO',
    estudio_requerido: h.estudio,
    servicio_solicitante: h.servicio,
    medico_prescriptor: h.medico,
    sla_restante_min: 0,
    hora_solicitud: h.hora,
    analizador_asignado: 'Cobas / Sysmex LIS'
  }))
  return [...orders.value, ...histComoEstudios]
})

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

// --- CARGA DESDE BACKEND ---
const cargarSolicitudesBackend = async () => {
  try {
    const data = await laboratorioService.obtenerSolicitudes({ per_page: 100 })
    const items = data.data || data
    if (Array.isArray(items) && items.length > 0) {
      const transformados: EstudioDiagnostico[] = items.map((s: SolicitudEstudioBackend) => {
        const pacNombre = s.expediente?.paciente?.nombre_completo || 'Paciente Hospitalizado'
        const iniciales = pacNombre.split(' ').map(n => n[0]).join('').substring(0, 4).toUpperCase() || 'P.N.'
        let edad = 35
        if (s.expediente?.paciente?.fecha_nacimiento) {
          const birth = new Date(s.expediente.paciente.fecha_nacimiento)
          const diff = Date.now() - birth.getTime()
          edad = Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25)) || 35
        }

        const tipoLower = (s.tipo_estudio || '').toLowerCase()
        const esStat = s.prioridad === 'STAT' || (s.prioridad === 'URGENTE' && (tipoLower.includes('choque') || tipoLower.includes('stat') || tipoLower.includes('gasometría')))
        const esUrgente = s.prioridad === 'URGENTE'
        const prioridad: PrioridadEstudio = esStat ? 'STAT' : (esUrgente ? 'URGENTE' : 'ORDINARIA')
        const codigo_color: CodigoColor = esStat ? 'ROJO' : (esUrgente ? 'NARANJA' : 'GRIS')
        const esImg = tipoLower.includes('radio') || tipoLower.includes('rx') || tipoLower.includes('tac') || tipoLower.includes('resonancia') || tipoLower.includes('ultrasonido') || tipoLower.includes('ecg')
        const categoria: CategoriaEstudio = esImg ? 'IMAGENOLOGIA' : 'LABORATORIO'

        let estado: EstadoEstudio = 'PENDIENTE'
        if (s.estado === 'EN_PROCESO') estado = 'EN_PROCESO'
        else if (s.estado === 'COMPLETADA') estado = 'VALIDADO'
        else if (s.estado === 'RECHAZADA') estado = 'RECHAZADO'

        const hora = s.fecha_hora_solicitud ? new Date(s.fecha_hora_solicitud).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '10:00'

        return {
          id: s.id_solicitud,
          folio: `EST-${s.id_solicitud.substring(0, 8).toUpperCase()}`,
          codigo_tubo: `BC-${s.id_solicitud.substring(0, 8).toUpperCase()}`,
          paciente_nombre: pacNombre,
          paciente_iniciales: iniciales,
          paciente_edad: edad,
          paciente_sexo: (s.expediente?.paciente?.sexo === 'F' ? 'F' : 'M') as 'M' | 'F',
          prioridad,
          codigo_color,
          estado,
          categoria,
          estudio_requerido: s.tipo_estudio,
          servicio_solicitante: s.indicaciones_clinicas || 'Urgencias / Hospitalización Central',
          medico_prescriptor: s.medico_solicitante?.nombre_completo || 'Dr. Médico Adscrito',
          sla_restante_min: esStat ? 10 : (esUrgente ? 25 : 55),
          hora_solicitud: hora,
          analizador_asignado: estado === 'EN_PROCESO' ? 'Roche Cobas / Sysmex' : undefined,
          observaciones_tecnicas: s.indicaciones_clinicas || undefined
        }
      })

      // Pendientes
      const pendientes = transformados.filter(t => t.estado === 'PENDIENTE' || t.estado === 'EN_PROCESO')
      if (pendientes.length > 0) {
        orders.value = pendientes
        selectedOrder.value = orders.value[0] as EstudioDiagnostico
      }

      // Validadas / Completadas para historial
      const validadas = transformados.filter(t => t.estado === 'VALIDADO' || t.estado === 'RECHAZADO')
      if (validadas.length > 0) {
        historialEstudios.value = validadas.map(v => ({
          folio: v.folio,
          id: v.id,
          hora: `${v.hora_solicitud} h`,
          paciente: v.paciente_iniciales,
          paciente_nombre: v.paciente_nombre,
          edad: `${v.paciente_edad}a`,
          sexo: v.paciente_sexo === 'F' ? 'Femenino' : 'Masculino',
          estudio: v.estudio_requerido,
          metodo: v.categoria === 'IMAGENOLOGIA' ? 'Radiología Digital Directa' : 'Analizador Roche Cobas e411',
          servicio: v.servicio_solicitante,
          medico: v.medico_prescriptor,
          formato: v.categoria === 'IMAGENOLOGIA' ? 'DICOM' : 'PDF',
          tamano: '1.4 MB',
          shaPreview: `${v.id.substring(0, 4)}...${v.id.substring(v.id.length - 4)}`,
          shaFull: `e83d${v.id.replace(/-/g, '')}44a02b`,
          estado: v.estado
        }))
      }
    }
  } catch (err) {
    console.warn('Uso de banco local ante modo autónomo de LIS:', err)
  }
}

const cargarKpisBackend = async () => {
  try {
    const res = await laboratorioService.obtenerKpis()
    if (res) {
      kpis.value = {
        validados_hoy: res.validados_hoy || kpis.value.validados_hoy,
        sla_promedio_min: res.sla_promedio_min || kpis.value.sla_promedio_min,
        cumplimiento_pct: res.cumplimiento_pct || kpis.value.cumplimiento_pct,
        tasa_rechazo_pct: res.tasa_rechazo_pct ?? kpis.value.tasa_rechazo_pct
      }
    }
  } catch (_) {}
}

const handleSelectTab = (tab: 'pendientes' | 'cargar' | 'busqueda' | 'historial') => {
  activeTab.value = tab
}

const handleCargarEstudio = async (order: EstudioDiagnostico) => {
  selectedOrder.value = order
  activeTab.value = 'cargar'
  if (order.id && !order.analizador_asignado && order.estado === 'PENDIENTE') {
    try {
      await laboratorioService.marcarEnProceso(order.id)
      order.estado = 'EN_PROCESO'
      order.analizador_asignado = 'Roche Cobas / Sysmex Online'
    } catch (_) {}
  }
}

const handleGuardarResultado = async (payload: { folio: string; archivo: ArchivoResultado; observaciones: string; rawFile?: File }) => {
  const targetOrder = orders.value.find(o => o.folio === payload.folio || o.id === payload.folio)
  const targetId = targetOrder?.id || payload.folio

  try {
    const res = await laboratorioService.cargarResultado(targetId, {
      archivo: payload.rawFile,
      contenido_texto: `INFORME ANALÍTICO DE LABORATORIO VALIDADO: ${targetOrder?.estudio_requerido || 'Estudio Clínico'}.\nObservaciones técnicas: ${payload.observaciones || 'Resultados conformes dentro de rangos normales de referencia.'}`,
      observaciones: payload.observaciones,
      mime: payload.archivo?.formato === 'PDF' ? 'application/pdf' : 'text/plain'
    })

    // Agregar a la bitácora en vivo
    historialEstudios.value.unshift({
      folio: targetOrder?.folio || payload.folio,
      id: targetId,
      hora: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' h',
      paciente: targetOrder?.paciente_iniciales || 'P.N.',
      paciente_nombre: targetOrder?.paciente_nombre || 'Paciente Validado',
      edad: targetOrder ? `${targetOrder.paciente_edad}a` : '35a',
      sexo: targetOrder?.paciente_sexo === 'F' ? 'Femenino' : 'Masculino',
      estudio: targetOrder?.estudio_requerido || 'Estudio de Laboratorio',
      metodo: targetOrder?.categoria === 'IMAGENOLOGIA' ? 'Radiología Digital / DICOM' : 'Analizador Cobas / Sysmex LIS',
      servicio: targetOrder?.servicio_solicitante || 'Urgencias',
      medico: targetOrder?.medico_prescriptor || 'Dr. Médico Adscrito',
      formato: payload.archivo?.formato || 'PDF',
      tamano: payload.archivo?.tamano || '1.4 MB',
      shaPreview: res?.hash_sha256 ? `${res.hash_sha256.substring(0, 4)}...${res.hash_sha256.substring(res.hash_sha256.length - 4)}` : '4fa9...8c2b',
      shaFull: res?.hash_sha256 || payload.archivo?.sha256 || '4fa9c182b8a93e198df4920bce1101ab648821ec39062fa883908c2b',
      estado: 'VALIDADO'
    })
  } catch (err) {
    console.warn('Error al transmitir a backend de laboratorio:', err)
  }

  const index = orders.value.findIndex(o => o.folio === payload.folio || o.id === payload.folio)
  if (index !== -1) {
    orders.value.splice(index, 1)
  }
  kpis.value.validados_hoy += 1
  await cargarKpisBackend()
  activeTab.value = 'historial'
}

const handleRechazarMuestra = async (payload: { folio: string; motivo: string }) => {
  const targetOrder = orders.value.find(o => o.folio === payload.folio || o.id === payload.folio)
  const targetId = targetOrder?.id || payload.folio

  try {
    await laboratorioService.rechazarMuestra(targetId, payload.motivo)
  } catch (err) {
    console.warn('Error al enviar rechazo a backend:', err)
  }

  const index = orders.value.findIndex(o => o.folio === payload.folio || o.id === payload.folio)
  if (index !== -1) {
    orders.value.splice(index, 1)
  }
  alert(`La muestra del estudio ${payload.folio} ha sido registrada con causal técnica de rechazo: "${payload.motivo}". Se emitió notificación prioritaria al servicio solicitante.`)
  await cargarKpisBackend()
  activeTab.value = 'pendientes'
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
  const usuarioAuth = authService.getUsuario()
  if (usuarioAuth) {
    user.value = {
      nombre: usuarioAuth.nombre_completo || 'Q.F.B. Mariana Téllez Morales',
      cedula: usuarioAuth.cedula || 'LAB-449102',
      rol: usuarioAuth.rol === 'LABORATORISTA' ? 'LABORATORIO CLÍNICO' : usuarioAuth.rol,
      turno: 'Matutino (07:00 - 15:00)',
      area: 'Laboratorio Central e Imagenología - Planta Baja'
    }
  }

  updateClock()
  clockInterval = window.setInterval(updateClock, 30000)
  window.addEventListener('keydown', handleGlobalKeydown)

  cargarSolicitudesBackend()
  cargarKpisBackend()
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
          @refrescar="cargarSolicitudesBackend"
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
          :todos-estudios="todosLosEstudios"
          @cargar-estudio="handleCargarEstudio"
        />

        <!-- PANTALLA 4: HISTORIAL Y BITÁCORA DE CARGAS VALIDADAS (Alt+H) -->
        <LabHistorialBitacoraSection
          v-else-if="activeTab === 'historial'"
          :kpis="kpis"
          :historial-estudios="historialEstudios"
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
