<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import triageService from '@/services/triageService'
import guardiasService, { type PisoHospital, type SolicitudCamaPendiente } from '@/services/guardiasService'

defineProps<{
  horaActual?: string
}>()

type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: SeccionRecepcion): void
}>()

const cambiarSeccion = (sec: SeccionRecepcion) => {
  emit('cambiarSeccion', sec)
}

const mostrarAlerta = (mensaje: string) => {
  alert(mensaje)
}

// =========================================================================
// PANTALLA 6: CENSO HOSPITALARIO Y GESTIÓN DE CAMAS
// =========================================================================
export interface CamaHospitalaria {
  id: string
  nombre: string
  piso: 'P1' | 'P2' | 'P3' | 'QX'
  subtitulo: string
  estatus: 'disponible' | 'ocupada' | 'reservada' | 'higienizacion'
  pacienteInfo?: string
  servicio?: string
}

// SolicitudCamaPendiente importada de guardiasService

const filtroPisoCenso = ref<'todos' | 'P1' | 'P2' | 'P3' | 'QX'>('todos')

// Formulario de Pre-asignación rápida (inicia en blanco para conexión backend)
const preasignacionCamaForm = ref({
  solicitudSeleccionada: '',
  camaSeleccionada: '',
  tomaO2: true,
  monitorSignos: true,
  aislamiento: false,
  barandales: true,
  medicoTratante: '',
  servicio: ''
})

// Ficha de Cama actualmente seleccionada para inspección
const fichaCamaSeleccionada = ref<CamaHospitalaria | null>(null)

// Solicitudes en espera de cama (inicia vacía sin datos precargados)
const solicitudesCamaPendientes = ref<SolicitudCamaPendiente[]>([])

const cargarSolicitudesCama = () => {
  solicitudesCamaPendientes.value = guardiasService.obtenerSolicitudesCama()
}

const seleccionarSolicitud = (sol: SolicitudCamaPendiente) => {
  preasignacionCamaForm.value.solicitudSeleccionada = sol.folio
  if (sol.servicio) preasignacionCamaForm.value.servicio = sol.servicio
  if (sol.medicoTratante) preasignacionCamaForm.value.medicoTratante = sol.medicoTratante
}

watch(() => preasignacionCamaForm.value.solicitudSeleccionada, (folio) => {
  if (folio) {
    const sol = solicitudesCamaPendientes.value.find(s => s.folio === folio)
    if (sol) {
      if (sol.servicio) preasignacionCamaForm.value.servicio = sol.servicio
      if (sol.medicoTratante) preasignacionCamaForm.value.medicoTratante = sol.medicoTratante
    }
  }
})

// Matriz reactiva de camas
const matrizCamasHospital = ref<CamaHospitalaria[]>([])

const cargarMatrizCamas = () => {
  const maestras = guardiasService.obtenerCamasMaestras()
  matrizCamasHospital.value = maestras.map(m => ({
    id: m.id,
    nombre: m.nombre,
    piso: m.piso,
    subtitulo: m.subtitulo,
    estatus: m.estatusCenso || 'disponible',
    pacienteInfo: m.pacienteInfo || 'Libre para derivación inmediata',
    servicio: m.servicio
  }))
  if (!fichaCamaSeleccionada.value && matrizCamasHospital.value.length > 0) {
    const primera = matrizCamasHospital.value[0]
    if (primera) fichaCamaSeleccionada.value = primera
  }
}

// Modal para registrar nueva cama / box
const modalRegistrarCamaAbierto = ref(false)
const formNuevaCama = ref<{
  piso: PisoHospital
  id: string
  nombre: string
  subtitulo: string
  servicio: string
}>({
  piso: 'P1',
  id: '',
  nombre: '',
  subtitulo: '',
  servicio: 'Urgencias'
})

const abrirModalRegistroCama = (pisoSugerido?: PisoHospital) => {
  formNuevaCama.value = {
    piso: pisoSugerido || (filtroPisoCenso.value === 'todos' ? 'P1' : filtroPisoCenso.value),
    id: '',
    nombre: '',
    subtitulo: '',
    servicio: pisoSugerido === 'P2' ? 'Medicina Interna' : pisoSugerido === 'P3' ? 'Pediatría' : pisoSugerido === 'QX' ? 'Quirófano' : 'Urgencias'
  }
  modalRegistrarCamaAbierto.value = true
}

const cerrarModalRegistroCama = () => {
  modalRegistrarCamaAbierto.value = false
}

const guardarNuevaCamaDesdeModal = () => {
  if (!formNuevaCama.value.id.trim() || !formNuevaCama.value.nombre.trim()) {
    mostrarAlerta('El código identificador y el nombre de la cama o box son obligatorios.')
    return
  }

  const res = guardiasService.registrarNuevaCama({
    id: formNuevaCama.value.id,
    nombre: formNuevaCama.value.nombre,
    piso: formNuevaCama.value.piso,
    subtitulo: formNuevaCama.value.subtitulo || 'Servicio Hospitalario',
    servicio: formNuevaCama.value.servicio || 'Servicio Hospitalario'
  })

  if (!res.ok) {
    mostrarAlerta(res.mensaje)
    return
  }

  cargarMatrizCamas()
  cerrarModalRegistroCama()
  mostrarAlerta(res.mensaje)
}

const camasFiltradasPorPiso = (piso: 'P1' | 'P2' | 'P3' | 'QX') => {
  return matrizCamasHospital.value.filter(c => c.piso === piso)
}

// Estadísticas de censo reactivas
const totalCamas = computed(() => matrizCamasHospital.value.length)
const camasDisponibles = computed(() => matrizCamasHospital.value.filter(c => c.estatus === 'disponible'))
const camasOcupadas = computed(() => matrizCamasHospital.value.filter(c => c.estatus === 'ocupada'))
const camasHigienizacion = computed(() => matrizCamasHospital.value.filter(c => c.estatus === 'higienizacion'))
const camasReservadas = computed(() => matrizCamasHospital.value.filter(c => c.estatus === 'reservada'))

const quirofanosTotal = computed(() => camasFiltradasPorPiso('QX').length)
const quirofanosDisponibles = computed(() => camasFiltradasPorPiso('QX').filter(c => c.estatus === 'disponible').length)

const tasaOcupacionPiso = (p: 'P1' | 'P2' | 'P3' | 'QX') => {
  const pisoItems = camasFiltradasPorPiso(p)
  if (pisoItems.length === 0) return '0%'
  const ocupadas = pisoItems.filter(c => c.estatus === 'ocupada' || c.estatus === 'reservada').length
  return `${Math.round((ocupadas / pisoItems.length) * 100)}% Ocupado`
}

const resumenTextoPiso = (p: 'P1' | 'P2' | 'P3' | 'QX') => {
  const pisoItems = camasFiltradasPorPiso(p)
  const libres = pisoItems.filter(c => c.estatus === 'disponible').length
  const ocup = pisoItems.filter(c => c.estatus === 'ocupada').length
  const hig = pisoItems.filter(c => c.estatus === 'higienizacion').length
  const res = pisoItems.filter(c => c.estatus === 'reservada').length
  const tipoPalabra = p === 'P1' ? 'Boxes' : p === 'QX' ? 'Salas' : 'Camas'
  return `${pisoItems.length} ${tipoPalabra} censables · ${libres} Libres · ${ocup} Ocupadas · ${hig} En Higienización · ${res} Reservadas`
}

const seleccionarCamaTablero = (cama: CamaHospitalaria) => {
  fichaCamaSeleccionada.value = cama
  preasignacionCamaForm.value.camaSeleccionada = cama.nombre
  preasignacionCamaForm.value.servicio = cama.servicio || ''
}

const liberarTodasLasCamas = () => {
  if (confirm('¿Desea restablecer todas las camas a estado DISPONIBLE (libres de pacientes)?')) {
    guardiasService.restablecerTodoLibre()
    cargarMatrizCamas()
    solicitudesCamaPendientes.value = []; guardiasService.guardarSolicitudesCama([]);
    preasignacionCamaForm.value.camaSeleccionada = ''
    preasignacionCamaForm.value.solicitudSeleccionada = ''
    mostrarAlerta('Todas las zonas y camas han sido restablecidas a estado DISPONIBLE.')
  }
}

const cargarSalasReales = async () => {
  try {
    const salas = await triageService.obtenerSalas()
    if (Array.isArray(salas) && salas.length > 0) {
      salas.forEach(s => {
        const encontrada = matrizCamasHospital.value.find(c => c.nombre.toUpperCase().includes(s.nombre.toUpperCase()) || c.id === s.id_sala)
        if (encontrada) {
          encontrada.estatus = s.ocupada ? 'ocupada' : 'disponible'
          encontrada.pacienteInfo = s.ocupada ? 'Ocupada en atención activa' : 'Libre para derivación inmediata'
        }
      })
    }
  } catch (error) {
    console.warn('Aviso sincronizando salas con censo:', error)
  }
}

onMounted(() => {
  cargarMatrizCamas()
  cargarSalasReales()
  cargarSolicitudesCama()
  window.addEventListener('vitae-camas-actualizadas', cargarMatrizCamas)
  window.addEventListener('vitae-solicitudes-cama-actualizadas', cargarSolicitudesCama)
})

onUnmounted(() => {
  window.removeEventListener('vitae-camas-actualizadas', cargarMatrizCamas)
  window.removeEventListener('vitae-solicitudes-cama-actualizadas', cargarSolicitudesCama)
})

const confirmarAsignacionCama = () => {
  if (!preasignacionCamaForm.value.camaSeleccionada) {
    mostrarAlerta('Seleccione una cama disponible en el tablero de censos.')
    return
  }
  const cama = matrizCamasHospital.value.find(c => c.nombre === preasignacionCamaForm.value.camaSeleccionada)
  if (!cama) {
    mostrarAlerta('No se encontró la cama seleccionada en el censo hospitalario.')
    return
  }
  if (cama.estatus === 'ocupada') {
    mostrarAlerta('La cama ' + cama.nombre + ' ya se encuentra ocupada por otro paciente.')
    return
  }

  const sol = solicitudesCamaPendientes.value.find(s => s.folio === preasignacionCamaForm.value.solicitudSeleccionada)
  const nombrePaciente = sol ? `${sol.paciente} (${sol.folio})` : (preasignacionCamaForm.value.solicitudSeleccionada || 'Paciente Asignado')

  cama.estatus = 'ocupada'
  cama.pacienteInfo = `Ocupada por ${nombrePaciente} · Dr. ${preasignacionCamaForm.value.medicoTratante || 'Tratante'}`
  guardiasService.actualizarEstatusCama(cama.id, 'ocupada', cama.pacienteInfo, preasignacionCamaForm.value.servicio || cama.servicio)

  if (preasignacionCamaForm.value.solicitudSeleccionada) {
    guardiasService.removerSolicitudCama(preasignacionCamaForm.value.solicitudSeleccionada)
    cargarSolicitudesCama()
  }

  mostrarAlerta(`Cama ${cama.nombre} asignada con éxito a ${nombrePaciente}. Notificación enviada a la estación de enfermería de ${preasignacionCamaForm.value.servicio || cama.servicio || 'Hospitalización'}.`)
  preasignacionCamaForm.value.solicitudSeleccionada = ''
}

const reportarLimpiezaCama = () => {
  if (!fichaCamaSeleccionada.value) return
  fichaCamaSeleccionada.value.estatus = 'higienizacion'
  fichaCamaSeleccionada.value.pacienteInfo = 'Solicitud de sanitización enviada a Intendencia'
  guardiasService.actualizarEstatusCama(fichaCamaSeleccionada.value.id, 'higienizacion', fichaCamaSeleccionada.value.pacienteInfo)
  mostrarAlerta(`[INTENDENCIA] Alerta de intendencia despachada para ${fichaCamaSeleccionada.value.nombre}. Prioridad: Higienización estándar.`)
}

const bloquearCama = () => {
  if (!fichaCamaSeleccionada.value) return
  fichaCamaSeleccionada.value.estatus = 'higienizacion'
  fichaCamaSeleccionada.value.pacienteInfo = 'Bloqueada por mantenimiento preventivo'
  guardiasService.actualizarEstatusCama(fichaCamaSeleccionada.value.id, 'higienizacion', fichaCamaSeleccionada.value.pacienteInfo)
  mostrarAlerta(`Cama ${fichaCamaSeleccionada.value.nombre} bloqueada temporalmente en censo hospitalario.`)
}
</script>

<template>
<div class="view-censo-container">
          
          <!-- Encabezado del Censo -->
          <div class="section-top-banner">
            <div class="banner-text-col">
              <div class="badges-row">
                <span class="cert-pill blue">TELEMETRÍA EN VIVO: {{ horaActual }} h</span>
                <span class="cert-pill green">REFRESCO CADA 15s</span>
                <span class="cert-pill cyan">NOM-024 / NOM-016-SSA3-2012</span>
              </div>
              <h2>Censo Hospitalario y Tablero de Gestión de Camas</h2>
              <p>Monitoreo sincronizado en tiempo Áreal entre Estaciones de Enfermería, Quirófanos Centrales y Sistema de Admisión Hospitalaria.</p>
            </div>

            <div class="banner-right-kpis-row">
              <button 
                type="button" 
                class="btn-action-primary"
                @click="abrirModalRegistroCama()"
                title="Registrar una nueva cama o box en el tablero"
              >
                + Registrar Cama / Box
              </button>
              <button 
                type="button" 
                class="btn-demo-slot"
                @click="liberarTodasLasCamas"
                title="Restablecer todas las camas a estado disponible"
              >
                Liberar Todas
              </button>
              <button type="button" class="btn-export-censo" @click="mostrarAlerta('Generando censo hospitalario consolidado en formato Excel / PDF...')">
                <svg viewBox="0 0 24 24" class="svg-inline-chart" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg> Exportar Censo Diario
              </button>
              <button type="button" class="btn-alert-janitor" @click="mostrarAlerta('Alerta general despachada a personal de intendencia y camilleros de guardia.')">
                <svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Alerta Intendencia
              </button>
            </div>
          </div>

          <!-- Filtro de Pisos y ÁÁreas -->
          <div class="censo-floors-filter-bar">
            <div class="floors-tab-chips">
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'todos' }]"
                @click="filtroPisoCenso = 'todos'"
              >
                Todos los Pisos ({{ totalCamas }} Posiciones)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P1' }]"
                @click="filtroPisoCenso = 'P1'"
              >
                P1: Urgencias ({{ camasFiltradasPorPiso('P1').length }} Boxes)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P2' }]"
                @click="filtroPisoCenso = 'P2'"
              >
                P2: Hospitalización ({{ camasFiltradasPorPiso('P2').length }} Camas)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P3' }]"
                @click="filtroPisoCenso = 'P3'"
              >
                P3: Pediatría y Gineco ({{ camasFiltradasPorPiso('P3').length }} Camas)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'QX' }]"
                @click="filtroPisoCenso = 'QX'"
              >
                Quirófanos Centrales ({{ camasFiltradasPorPiso('QX').length }} Salas)
              </button>
            </div>
          </div>

          <!-- Tarjetas KPI de Resumen Hospitalario (5 Métricas Superiores) -->
          <div class="censo-metrics-grid">
            
            <div class="kpi-card censo-metric">
              <span class="kpi-label">CAMAS CENSABLES</span>
              <strong class="kpi-value text-blue">{{ totalCamas }} Totales</strong>
              <small class="kpi-sub">{{ camasOcupadas.length }} en estancia activa ({{ totalCamas > 0 ? Math.round((camasOcupadas.length / totalCamas) * 100) : 0 }}% Ocupación)</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">DISPONIBLES INMEDIATAS</span>
              <strong class="kpi-value text-cyan">{{ camasDisponibles.length }} Libres listas</strong>
              <small class="kpi-sub">Pre-asignadas en cola: {{ camasReservadas.length }} en proceso</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">EN DESINFECCIÓN</span>
              <strong class="kpi-value text-orange">{{ camasHigienizacion.length }} En higienización</strong>
              <small class="kpi-sub">Tiempo est. liberación: ~20 min prom.</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">QUIRÓFANOS CENTRALES</span>
              <strong class="kpi-value text-purple">{{ quirofanosDisponibles }} / {{ quirofanosTotal }} Disponibles</strong>
              <small class="kpi-sub">Salas en cirugía: 2 en curso</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">URGENCIAS / CHOQUE</span>
              <strong class="kpi-value text-red">6 / 18 Boxes libres</strong>
              <small class="kpi-sub">Boxes críticos ocupados: 12 pacientes</small>
            </div>

          </div>

          <!-- Barra de Leyenda de Códigos de Color -->
          <div class="censo-legend-bar">
            <span class="legend-title">CÓDIGOS DE ESTADO:</span>
            <div class="legend-items-row">
              <span class="legend-item"><span class="legend-dot disponible"></span> Disponible Inmediata</span>
              <span class="legend-item"><span class="legend-dot ocupada"></span> Ocupada (Estancia Activa)</span>
              <span class="legend-item"><span class="legend-dot reservada"></span> Reservada para Ingreso</span>
              <span class="legend-item"><span class="legend-dot higienizacion"></span> En Higienización / Bloqueo</span>
            </div>
          </div>

          <!-- Cuadrícula Principal (Matriz de Pisos a la Izquierda + Panel de Asignación y Ficha Técnica a la Derecha) -->
          <div class="curp-layout-grid">
            
            <!-- SUBCOLUMNA IZQUIERDA: Matriz de Camas por Piso -->
            <div class="curp-left-col">
              
              <!-- PISO 1: Urgencias Médicas y Choque -->
              <div v-if="filtroPisoCenso === 'todos' || filtroPisoCenso === 'P1'" class="floor-block-card">
                <div class="floor-header">
                  <div class="floor-title-info">
                    <span class="floor-badge p1">P1</span>
                    <div>
                      <h4>Piso 1 · Urgencias Médicas, Triage y Sala de Choque</h4>
                      <small>{{ resumenTextoPiso('P1') }}</small>
                    </div>
                  </div>
                  <span class="floor-rate">{{ tasaOcupacionPiso('P1') }}</span>
                </div>

                <div class="beds-matrix-grid">
                  <div 
                    v-for="cama in camasFiltradasPorPiso('P1')" 
                    :key="cama.id"
                    :class="['bed-cell-card', cama.estatus, { selected: fichaCamaSeleccionada?.id === cama.id }]"
                    @click="seleccionarCamaTablero(cama)"
                  >
                    <div class="bed-cell-top">
                      <strong>{{ cama.nombre }}</strong>
                      <span class="bed-status-indicator" :class="cama.estatus"></span>
                    </div>
                    <span class="bed-cell-sub">{{ cama.subtitulo }}</span>
                    <small class="bed-cell-info">{{ cama.pacienteInfo }}</small>
                  </div>
                </div>
              </div>

              <!-- PISO 2: Hospitalización Adultos -->
              <div v-if="filtroPisoCenso === 'todos' || filtroPisoCenso === 'P2'" class="floor-block-card">
                <div class="floor-header">
                  <div class="floor-title-info">
                    <span class="floor-badge p2">P2</span>
                    <div>
                      <h4>Piso 2 · Hospitalización Adultos (Cirugía y Medicina Interna)</h4>
                      <small>{{ resumenTextoPiso('P2') }}</small>
                    </div>
                  </div>
                  <span class="floor-rate">{{ tasaOcupacionPiso('P2') }}</span>
                </div>

                <div class="beds-matrix-grid">
                  <div 
                    v-for="cama in camasFiltradasPorPiso('P2')" 
                    :key="cama.id"
                    :class="['bed-cell-card', cama.estatus, { selected: fichaCamaSeleccionada?.id === cama.id }]"
                    @click="seleccionarCamaTablero(cama)"
                  >
                    <div class="bed-cell-top">
                      <strong>{{ cama.nombre }}</strong>
                      <span class="bed-status-indicator" :class="cama.estatus"></span>
                    </div>
                    <span class="bed-cell-sub">{{ cama.subtitulo }}</span>
                    <small class="bed-cell-info">{{ cama.pacienteInfo }}</small>
                  </div>
                </div>
              </div>

              <!-- PISO 3: Pediatría y Ginecoobstetricia -->
              <div v-if="filtroPisoCenso === 'todos' || filtroPisoCenso === 'P3'" class="floor-block-card">
                <div class="floor-header">
                  <div class="floor-title-info">
                    <span class="floor-badge p3">P3</span>
                    <div>
                      <h4>Piso 3 · Pediatría, Cunas y Ginecoobstetricia</h4>
                      <small>{{ resumenTextoPiso('P3') }}</small>
                    </div>
                  </div>
                  <span class="floor-rate">{{ tasaOcupacionPiso('P3') }}</span>
                </div>

                <div class="beds-matrix-grid">
                  <div 
                    v-for="cama in camasFiltradasPorPiso('P3')" 
                    :key="cama.id"
                    :class="['bed-cell-card', cama.estatus, { selected: fichaCamaSeleccionada?.id === cama.id }]"
                    @click="seleccionarCamaTablero(cama)"
                  >
                    <div class="bed-cell-top">
                      <strong>{{ cama.nombre }}</strong>
                      <span class="bed-status-indicator" :class="cama.estatus"></span>
                    </div>
                    <span class="bed-cell-sub">{{ cama.subtitulo }}</span>
                    <small class="bed-cell-info">{{ cama.pacienteInfo }}</small>
                  </div>
                </div>
              </div>

              <!-- ÁREA QUIRÚRGICA: Quirófanos Centrales -->
              <div v-if="filtroPisoCenso === 'todos' || filtroPisoCenso === 'QX'" class="floor-block-card">
                <div class="floor-header">
                  <div class="floor-title-info">
                    <span class="floor-badge qx">QX</span>
                    <div>
                      <h4>Área Quirúrgica Central (Salas QUI)</h4>
                      <small>{{ resumenTextoPiso('QX') }}</small>
                    </div>
                  </div>
                  <span class="floor-rate">{{ tasaOcupacionPiso('QX') }}</span>
                </div>

                <div class="beds-matrix-grid">
                  <div 
                    v-for="cama in camasFiltradasPorPiso('QX')" 
                    :key="cama.id"
                    :class="['bed-cell-card', cama.estatus, { selected: fichaCamaSeleccionada?.id === cama.id }]"
                    @click="seleccionarCamaTablero(cama)"
                  >
                    <div class="bed-cell-top">
                      <strong>{{ cama.nombre }}</strong>
                      <span class="bed-status-indicator" :class="cama.estatus"></span>
                    </div>
                    <span class="bed-cell-sub">{{ cama.subtitulo }}</span>
                    <small class="bed-cell-info">{{ cama.pacienteInfo }}</small>
                  </div>
                </div>
              </div>

            </div>

            <!-- SUBCOLUMNA DERECHA: Asignación Rápida, Solicitudes y Ficha de Cama -->
            <aside class="curp-right-col">
              
              <!-- 1. Formulario de Pre-asignación Rápida de Cama -->
              <div class="side-widget-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-bed" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 4v16"/><path d="M2 8h18a2 2 0 0 1 2 2v10" /><path d="M2 17h20"/><path d="M6 8v9"/></svg></span>
                    <strong>Pre-asignación Rápida de Cama</strong>
                  </div>
                  <span class="badge-confirm-pending">NOM-024</span>
                </div>

                <div class="assignment-form-body">
                  
                  <div class="form-group-item">
                    <label class="field-label-sm">Paciente en Espera de Cama</label>
                    <select v-model="preasignacionCamaForm.solicitudSeleccionada" class="form-select">
                      <option value="">-- Seleccionar Solicitud en Cola --</option>
                      <option 
                        v-for="(sol, idx) in solicitudesCamaPendientes" 
                        :key="idx" 
                        :value="sol.folio"
                      >
                        {{ sol.folio }} · {{ sol.paciente }} ({{ sol.servicio }})
                      </option>
                    </select>
                  </div>

                  <div class="form-group-item">
                    <label class="field-label-sm">Cama Seleccionada en Tablero</label>
                    <div class="curp-field-wrapper">
                      <span class="field-prefix"><svg viewBox="0 0 24 24" class="svg-inline-bed" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 4v16"/><path d="M2 8h18a2 2 0 0 1 2 2v10" /><path d="M2 17h20"/><path d="M6 8v9"/></svg></span>
                      <input 
                        type="text" 
                        v-model="preasignacionCamaForm.camaSeleccionada" 
                        placeholder="Haga clic en una cama del tablero" 
                        class="form-input font-bold" 
                      />
                    </div>
                  </div>

                  <!-- Requisitos de Soporte / Aislamiento -->
                  <div class="support-checks-grid">
                    <label class="check-inline-label">
                      <input type="checkbox" v-model="preasignacionCamaForm.tomaO2" />
                      <span>Toma O2 Central</span>
                    </label>
                    <label class="check-inline-label">
                      <input type="checkbox" v-model="preasignacionCamaForm.monitorSignos" />
                      <span>Monitor Signos</span>
                    </label>
                    <label class="check-inline-label">
                      <input type="checkbox" v-model="preasignacionCamaForm.aislamiento" />
                      <span>Aislamiento</span>
                    </label>
                    <label class="check-inline-label">
                      <input type="checkbox" v-model="preasignacionCamaForm.barandales" />
                      <span>Barandales</span>
                    </label>
                  </div>

                  <div class="grid-2-cols mt-2">
                    <div class="form-group-item">
                      <label class="field-label-sm">Médico Tratante</label>
                      <input 
                        type="text" 
                        v-model="preasignacionCamaForm.medicoTratante" 
                        placeholder="Dr. M. Sotomayor" 
                        class="form-input" 
                      />
                    </div>
                    <div class="form-group-item">
                      <label class="field-label-sm">Servicio Asignado</label>
                      <input 
                        type="text" 
                        v-model="preasignacionCamaForm.servicio" 
                        placeholder="Medicina Interna" 
                        class="form-input" 
                      />
                    </div>
                  </div>

                  <button 
                    type="button" 
                    class="btn-action-primary mt-3"
                    @click="confirmarAsignacionCama"
                  >
                    Confirmar Asignación de Cama [Enter]
                  </button>

                </div>
              </div>

              <!-- 2. Solicitudes de Cama Pendientes -->
              <div class="side-widget-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
                    <strong>Solicitudes Pendientes ({{ solicitudesCamaPendientes.length }})</strong>
                  </div>
                  <span class="badge-queue-count">En Cola</span>
                </div>

                <div class="bed-requests-list">
                  <div 
                    v-for="(sol, idx) in solicitudesCamaPendientes" 
                    :key="idx"
                    class="bed-request-item"
                    :class="{ selected: preasignacionCamaForm.solicitudSeleccionada === sol.folio }"
                    @click="seleccionarSolicitud(sol)"
                  >
                    <div class="request-top">
                      <strong>{{ sol.folio }}</strong>
                      <span 
                        class="priority-tag"
                        :class="{
                          orange: sol.prioridad.includes('Naranja'),
                          yellow: sol.prioridad.includes('Amarillo'),
                          purple: sol.prioridad.includes('Gineco'),
                          blue: sol.prioridad.includes('Programado')
                        }"
                      >
                        • {{ sol.prioridad }}
                      </span>
                    </div>
                    <div class="request-patient">{{ sol.paciente }}</div>
                    <div class="request-meta">
                      <span>Servicio: <strong>{{ sol.servicio }}</strong></span>
                      <span>Espera: <strong>{{ sol.tiempoEspera }}</strong></span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 3. Ficha Técnica de Cama Seleccionada -->
              <div v-if="fichaCamaSeleccionada" class="side-widget-card bed-spec-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
                    <strong>Ficha de Cama: {{ fichaCamaSeleccionada.nombre }}</strong>
                  </div>
                  <span class="bed-status-chip" :class="fichaCamaSeleccionada.estatus">
                    • {{ fichaCamaSeleccionada.estatus.toUpperCase() }}
                  </span>
                </div>

                <div class="bed-specs-body">
                  <div class="spec-row">
                    <span>Ubicación:</span>
                    <strong>{{ fichaCamaSeleccionada.subtitulo }} ({{ fichaCamaSeleccionada.piso }})</strong>
                  </div>
                  <div class="spec-row">
                    <span>Estado / Paciente:</span>
                    <strong>{{ fichaCamaSeleccionada.pacienteInfo }}</strong>
                  </div>
                  <div class="spec-row">
                    <span>Tomas de Gases:</span>
                    <span class="text-green"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Oxígeno, Aire y Vacío Operativos</span>
                  </div>
                  <div class="spec-row">
                    <span>Seguridad Eléctrica:</span>
                    <span>Grado Médico · UPS Aislada</span>
                  </div>

                  <div class="bed-spec-actions-row">
                    <button type="button" class="btn-subtle-clean" @click="reportarLimpiezaCama">
                      <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m18 3-6.5 6.5"/><path d="M14.5 12.5 11 16l-3-1-3 3 5 5 3-3-1-3 3.5-3.5"/></svg> Solicitar Higienización Especial
                    </button>
                    <button type="button" class="btn-subtle-block" @click="bloquearCama">
                      <svg viewBox="0 0 24 24" class="svg-inline-ban" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg> Bloquear Cama
                    </button>
                  </div>
                </div>
              </div>

            </aside>
          </div>
        </div>

    <!-- MODAL REGISTRO DE NUEVA CAMA / BOX -->
    <div v-if="modalRegistrarCamaAbierto" class="modal-backdrop-custom">
      <div class="modal-card-custom">
        <div class="modal-header-custom">
          <h3>Registrar Nueva Cama o Box en Tablero</h3>
          <button type="button" class="btn-close-modal" @click="cerrarModalRegistroCama">✕</button>
        </div>
        <div class="modal-body-custom">
          <div class="form-group-item">
            <label class="field-label-sm">Piso / Zona Hospitalaria</label>
            <select v-model="formNuevaCama.piso" class="form-select">
              <option value="P1">Piso 1 · Urgencias y Triage</option>
              <option value="P2">Piso 2 · Hospitalización Adultos</option>
              <option value="P3">Piso 3 · Pediatría y Ginecoobstetricia</option>
              <option value="QX">Bloque Quirúrgico Central QX</option>
            </select>
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Código / Identificador (ej. BOX-U09, HAB-204-A, QUI-06)</label>
            <input type="text" v-model="formNuevaCama.id" placeholder="BOX-U09" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Nombre Mostrado (ej. BOX U-09, HABITACIÓN 204-A)</label>
            <input type="text" v-model="formNuevaCama.nombre" placeholder="BOX U-09" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Subtítulo / Especialidad (ej. Observación Pediátrica, Cirugía)</label>
            <input type="text" v-model="formNuevaCama.subtitulo" placeholder="Observación Urgencias" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Servicio Clínico</label>
            <input type="text" v-model="formNuevaCama.servicio" placeholder="Urgencias" class="form-input" />
          </div>
        </div>
        <div class="modal-footer-custom">
          <button type="button" class="btn-secondary-custom" @click="cerrarModalRegistroCama">Cancelar</button>
          <button type="button" class="btn-primary-custom" @click="guardarNuevaCamaDesdeModal">Habilitar Cama en Tablero</button>
        </div>
      </div>
    </div>

</template>

<style scoped>

/* TOP BANNER COMÚN */
.section-top-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}

.banner-text-col {
  flex: 1;
  min-width: 300px;
}

.badges-row {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-bottom: 0.4rem;
}

.cert-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.cert-pill.blue { background-color: #EFF6FF; color: #1D4ED8; border: 1px solid #BFDBFE; }
.cert-pill.cyan { background-color: #ECFEFF; color: #0E7490; border: 1px solid #A5F3FC; }
.cert-pill.green { background-color: #F0FDF4; color: #15803D; border: 1px solid #BBF7D0; }

.section-top-banner h2 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0.2rem 0;
}

.section-top-banner p {
  font-size: 0.75rem;
  color: #64748B;
  margin: 0;
  line-height: 1.35;
  max-width: 750px;
}

.curp-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1.2rem;
  align-items: start;
}

.curp-left-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  min-width: 0;
}

.curp-right-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.step-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.side-widget-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-row-sm {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #0F2D4A;
}

.title-row-sm span {
  font-size: 1rem;
}

.title-row-sm strong {
  display: block;
  font-size: 0.8rem;
  color: #1E293B;
}

.block-sub {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
}

.time-stamp-sm {
  font-size: 0.7rem;
  font-weight: 700;
  color: #64748B;
  background-color: #F1F5F9;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}


/* =========================================================================
   ESTILOS PANTALLA 6: CENSO HOSPITALARIO Y GESTIÓN DE CAMAS
   ========================================================================= */
.btn-export-censo {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.45rem 0.85rem;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-alert-janitor {
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
  border-radius: 6px;
  padding: 0.45rem 0.85rem;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

.censo-floors-filter-bar {
  margin-bottom: 0.85rem;
}

.floors-tab-chips {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.floor-chip-btn {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.4rem 0.8rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.floor-chip-btn:hover {
  background-color: #F1F5F9;
}

.floor-chip-btn.active {
  background-color: #0F2D4A;
  border-color: #0F2D4A;
  color: #FFFFFF;
}

.censo-metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.censo-metric {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem 0.85rem;
  display: flex;
  flex-direction: column;
}

.text-purple {
  color: #7C3AED;
}

.text-red {
  color: #DC2626;
}

.censo-legend-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.5rem 0.85rem;
  margin-bottom: 1.25rem;
  font-size: 0.72rem;
  flex-wrap: wrap;
}

.legend-title {
  font-weight: 700;
  color: #334155;
}

.legend-items-row {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #475569;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.legend-dot.disponible {
  background-color: #10B981;
}

.legend-dot.ocupada {
  background-color: #0F2D4A;
}

.legend-dot.reservada {
  background-color: #0284C7;
}

.legend-dot.higienizacion {
  background-color: #F59E0B;
}

.floor-block-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  margin-bottom: 1rem;
  padding: 0.85rem;
}

.floor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.5rem;
}

.floor-title-info {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.floor-badge {
  font-weight: 900;
  font-size: 0.75rem;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
}

.floor-badge.p1 {
  background-color: #FEE2E2;
  color: #991B1B;
}

.floor-badge.p2 {
  background-color: #E0F2FE;
  color: #0369A1;
}

.floor-badge.p3 {
  background-color: #EDE9FE;
  color: #6D28D9;
}

.floor-badge.qx {
  background-color: #FCE7F3;
  color: #9D174D;
}

.floor-rate {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748B;
}

.beds-matrix-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(135px, 1fr));
  gap: 0.55rem;
}

.bed-cell-card {
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.55rem;
  cursor: pointer;
  transition: all 0.15s ease;
  background-color: #F8FAFC;
}

.bed-cell-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.08);
}

.bed-cell-card.disponible {
  border-left: 4px solid #10B981;
  background-color: #F0FDF4;
}

.bed-cell-card.ocupada {
  border-left: 4px solid #0F2D4A;
  background-color: #F8FAFC;
}

.bed-cell-card.reservada {
  border-left: 4px solid #0284C7;
  background-color: #EFF6FF;
}

.bed-cell-card.higienizacion {
  border-left: 4px solid #F59E0B;
  background-color: #FFFBEB;
}

.bed-cell-card.selected {
  outline: 2px solid #0284C7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.3);
}

.bed-cell-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
}

.bed-cell-top strong {
  color: #0F2D4A;
}

.bed-status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.bed-status-indicator.disponible {
  background-color: #10B981;
}

.bed-status-indicator.ocupada {
  background-color: #0F2D4A;
}

.bed-status-indicator.reservada {
  background-color: #0284C7;
}

.bed-status-indicator.higienizacion {
  background-color: #F59E0B;
}

.bed-cell-sub {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
  margin-top: 0.15rem;
}

.bed-cell-info {
  display: block;
  font-size: 0.62rem;
  color: #475569;
  margin-top: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.support-checks-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.check-inline-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  color: #334155;
  cursor: pointer;
}

.badge-queue-count {
  background-color: #FEF3C7;
  color: #92400E;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.bed-requests-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.bed-request-item {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.55rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.bed-request-item:hover {
  border-color: #0284C7;
}

.bed-request-item.selected {
  border-color: #0284C7;
  background-color: #EFF6FF;
}

.request-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.72rem;
}

.priority-tag {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 3px;
}

.priority-tag.orange {
  background-color: #FFEDD5;
  color: #C2410C;
}

.priority-tag.yellow {
  background-color: #FEF9C3;
  color: #A16207;
}

.priority-tag.purple {
  background-color: #F3E8FF;
  color: #7E22CE;
}

.priority-tag.blue {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.request-patient {
  font-size: 0.68rem;
  color: #334155;
  font-weight: 600;
  margin-top: 0.2rem;
}

.request-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.62rem;
  color: #64748B;
  margin-top: 0.2rem;
}

.bed-status-chip {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.bed-status-chip.disponible {
  background-color: #DCFCE7;
  color: #166534;
}

.bed-status-chip.ocupada {
  background-color: #E2E8F0;
  color: #1E293B;
}

.bed-status-chip.reservada {
  background-color: #DBEAFE;
  color: #1E40AF;
}

.bed-status-chip.higienizacion {
  background-color: #FEF3C7;
  color: #92400E;
}

.bed-specs-body {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-size: 0.7rem;
}

.bed-spec-actions-row {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.65rem;
}

.btn-subtle-clean {
  background-color: #F0FDF4;
  border: 1px solid #BBF7D0;
  color: #166534;
  border-radius: 4px;
  padding: 0.35rem 0.55rem;
  font-size: 0.65rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-subtle-block {
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
  border-radius: 4px;
  padding: 0.35rem 0.55rem;
  font-size: 0.65rem;
  font-weight: 600;
  cursor: pointer;
}


/* FORMULARIO DE PRE-ASIGNACION RAPIDA */
.assignment-form-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  width: 100%;
}

.field-label-sm {
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.form-select,
.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  background-color: #FFFFFF;
  color: #1E293B;
  font-size: 0.8rem;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;
}

.form-select:focus,
.form-input:focus {
  border-color: #0284C7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.curp-field-wrapper {
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
}

.field-prefix {
  position: absolute;
  left: 0.65rem;
  display: flex;
  align-items: center;
  color: #64748B;
  pointer-events: none;
}

.curp-field-wrapper .form-input {
  padding-left: 2.2rem;
}

.support-checks-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.45rem;
  background-color: #F8FAFC;
  padding: 0.6rem;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.check-inline-label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.73rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.check-inline-label input[type="checkbox"] {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

.grid-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.btn-action-primary {
  width: 100%;
  background-color: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.03em;
  transition: background-color 0.15s ease, transform 0.05s ease;
  text-align: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.btn-action-primary:hover {
  background-color: #0369A1;
}

.btn-action-primary:active {
  transform: scale(0.99);
}

.mt-2 {
  margin-top: 0.5rem;
}

.mt-3 {
  margin-top: 0.75rem;
}

.font-bold {
  font-weight: 700;
}


/* ESTILOS DEL MODAL DE REGISTRO DE CAMA */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.modal-card-custom {
  background: #ffffff;
  border-radius: 10px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
  border: 1px solid #e2e8f0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header-custom h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #0f172a;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: #64748b;
}

.modal-body-custom {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.modal-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 0.85rem 1.25rem;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.btn-secondary-custom {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-weight: 600;
  font-size: 0.8125rem;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary-custom {
  background: #0284c7;
  border: none;
  color: #ffffff;
  font-weight: 600;
  font-size: 0.8125rem;
  padding: 0.5rem 1.1rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary-custom:hover {
  background: #0369a1;
}

</style>
