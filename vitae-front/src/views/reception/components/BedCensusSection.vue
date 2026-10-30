<script setup lang="ts">
import { ref } from 'vue'

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
// PANTALLA 6: CENSO HOSPITALARIO Y GESTIÓN DE CAMAS (IMAGEN 6)
// =========================================================================
interface CamaHospitalaria {
  id: string
  nombre: string
  piso: 'P1' | 'P2' | 'P3' | 'QX'
  subtitulo: string
  estatus: 'disponible' | 'ocupada' | 'reservada' | 'higienizacion'
  pacienteInfo?: string
  servicio?: string
}

interface SolicitudCamaPendiente {
  folio: string
  paciente: string
  prioridad: 'Triage 2 - Naranja' | 'Programado' | 'Triage 3 - Amarillo' | 'Gineco Urgente'
  servicio: string
  tiempoEspera: string
}

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
const fichaCamaSeleccionada = ref<CamaHospitalaria>({
  id: 'HAB-202-A',
  nombre: 'HABITACIÓN 202-A',
  piso: 'P2',
  subtitulo: 'Pabellón Adultos Poniente',
  estatus: 'disponible',
  pacienteInfo: 'Libre para Asignación Inmediata',
  servicio: 'Medicina Interna'
})

// Solicitudes en espera de cama
const solicitudesCamaPendientes = ref<SolicitudCamaPendiente[]>([
  { folio: 'PAC-9920', paciente: 'Gómez Téllez, Arturo · Masc. 45a', prioridad: 'Triage 2 - Naranja', servicio: 'Medicina Interna', tiempoEspera: '18 min' },
  { folio: 'PAC-9924', paciente: 'Paredes R., Claudia · Fem. 52a', prioridad: 'Programado', servicio: 'Cirugía General', tiempoEspera: '24 min' },
  { folio: 'PAC-9931', paciente: 'Vega Morales, Juan · Masc. 29a', prioridad: 'Triage 3 - Amarillo', servicio: 'Traumatología', tiempoEspera: '42 min' },
  { folio: 'PAC-9935', paciente: 'Herrera S., Diana · Fem. 28a', prioridad: 'Gineco Urgente', servicio: 'Ginecoobstetricia', tiempoEspera: '12 min' }
])

// Cuadrícula maestra de camas
const matrizCamasHospital = ref<CamaHospitalaria[]>([
  // Piso 1: Urgencias Médicas y Choque
  { id: 'BOX-U01', nombre: 'BOX U-01', piso: 'P1', subtitulo: 'Choque 1 (Crítico)', estatus: 'ocupada', pacienteInfo: 'Choque Rojo · Politraumatizado', servicio: 'Urgencias' },
  { id: 'BOX-U02', nombre: 'BOX U-02', piso: 'P1', subtitulo: 'Choque 2 (Crítico)', estatus: 'ocupada', pacienteInfo: 'Choque Naranja · IAM en curso', servicio: 'Urgencias' },
  { id: 'BOX-U03', nombre: 'BOX U-03', piso: 'P1', subtitulo: 'Observación Urgencias', estatus: 'disponible', pacienteInfo: 'Listo para ingreso', servicio: 'Urgencias' },
  { id: 'BOX-U04', nombre: 'BOX U-04', piso: 'P1', subtitulo: 'Adultos Observación', estatus: 'ocupada', pacienteInfo: 'Crisis asmática', servicio: 'Urgencias' },
  { id: 'BOX-U05', nombre: 'BOX U-05', piso: 'P1', subtitulo: 'Recepción Ambulancia', estatus: 'ocupada', pacienteInfo: 'Trauma craneal leve', servicio: 'Urgencias' },
  { id: 'BOX-U06', nombre: 'BOX U-06', piso: 'P1', subtitulo: 'Aislamiento Respiratorio', estatus: 'higienizacion', pacienteInfo: 'Limpieza profunda terminal', servicio: 'Urgencias' },
  { id: 'BOX-U07', nombre: 'BOX U-07', piso: 'P1', subtitulo: 'Observación Corta', estatus: 'disponible', pacienteInfo: 'Libre para derivación', servicio: 'Urgencias' },
  { id: 'BOX-U08', nombre: 'BOX U-08', piso: 'P1', subtitulo: 'Observación Corta', estatus: 'disponible', pacienteInfo: 'Libre para derivación', servicio: 'Urgencias' },
  { id: 'BOX-U09', nombre: 'BOX U-09', piso: 'P1', subtitulo: 'Triage 3 Amarillo', estatus: 'ocupada', pacienteInfo: 'Apendicitis en estudio', servicio: 'Urgencias' },
  { id: 'BOX-U10', nombre: 'BOX U-10', piso: 'P1', subtitulo: 'Triage 3 Amarillo', estatus: 'ocupada', pacienteInfo: 'Deshidratación severa', servicio: 'Urgencias' },
  { id: 'BOX-U11', nombre: 'BOX U-11', piso: 'P1', subtitulo: 'Reserva Ambulancia', estatus: 'reservada', pacienteInfo: 'Traslado CRUM en camino', servicio: 'Urgencias' },
  { id: 'BOX-U12', nombre: 'BOX U-12', piso: 'P1', subtitulo: 'Observación Rápida', estatus: 'disponible', pacienteInfo: 'Libre para ingreso', servicio: 'Urgencias' },

  // Piso 2: Hospitalización Adultos
  { id: 'HAB-201-A', nombre: 'HAB 201-A', piso: 'P2', subtitulo: 'Medicina Interna', estatus: 'ocupada', pacienteInfo: 'Gómez R., Juan · Neumonía', servicio: 'Medicina Interna' },
  { id: 'HAB-201-B', nombre: 'HAB 201-B', piso: 'P2', subtitulo: 'Medicina Interna', estatus: 'ocupada', pacienteInfo: 'López M., Ana · DM2 descompensada', servicio: 'Medicina Interna' },
  { id: 'HAB-202-A', nombre: 'HAB 202-A', piso: 'P2', subtitulo: 'Pabellón Adultos Poniente', estatus: 'disponible', pacienteInfo: 'Desinfección aprobada 10:15 h', servicio: 'Medicina Interna' },
  { id: 'HAB-202-B', nombre: 'HAB 202-B', piso: 'P2', subtitulo: 'Cirugía General', estatus: 'ocupada', pacienteInfo: 'Salazar P., Carlos · Post-laparo', servicio: 'Cirugía General' },
  { id: 'HAB-203', nombre: 'HAB 203', piso: 'P2', subtitulo: 'Aislamiento Presión Neg.', estatus: 'ocupada', pacienteInfo: 'Mendoza T., Luis · TB sospecha', servicio: 'Infectología' },
  { id: 'HAB-204-A', nombre: 'HAB 204-A', piso: 'P2', subtitulo: 'Medicina Interna', estatus: 'disponible', pacienteInfo: 'Libre e inspeccionada', servicio: 'Medicina Interna' },
  { id: 'HAB-204-B', nombre: 'HAB 204-B', piso: 'P2', subtitulo: 'Medicina Interna', estatus: 'disponible', pacienteInfo: 'Libre e inspeccionada', servicio: 'Medicina Interna' },
  { id: 'HAB-205-A', nombre: 'HAB 205-A', piso: 'P2', subtitulo: 'Cirugía General', estatus: 'higienizacion', pacienteInfo: 'En proceso de sanitización', servicio: 'Cirugía General' },
  { id: 'HAB-205-B', nombre: 'HAB 205-B', piso: 'P2', subtitulo: 'Cirugía General', estatus: 'ocupada', pacienteInfo: 'Pérez K., Mario · Colecistectomía', servicio: 'Cirugía General' },
  { id: 'HAB-206-A', nombre: 'HAB 206-A', piso: 'P2', subtitulo: 'Post-Quirúrgico', estatus: 'reservada', pacienteInfo: 'Reservada para PAC-9924', servicio: 'Cirugía General' },
  { id: 'HAB-206-B', nombre: 'HAB 206-B', piso: 'P2', subtitulo: 'Post-Quirúrgico', estatus: 'disponible', pacienteInfo: 'Libre con toma O2 y aspiración', servicio: 'Cirugía General' },
  { id: 'HAB-207-A', nombre: 'HAB 207-A', piso: 'P2', subtitulo: 'Neurología', estatus: 'ocupada', pacienteInfo: 'Ríos E., Sandra · EVC en remisión', servicio: 'Neurología' },

  // Piso 3: Pediatría y Ginecoobstetricia
  { id: 'CUNA-01', nombre: 'CUNA-01', piso: 'P3', subtitulo: 'Cuna Térmica', estatus: 'disponible', pacienteInfo: 'Calibrada a 36.8°C · Libre', servicio: 'Pediatría / Neonatos' },
  { id: 'CUNA-02', nombre: 'CUNA-02', piso: 'P3', subtitulo: 'Incubadora Cuidados', estatus: 'ocupada', pacienteInfo: 'RN Pérez · Prematuro 34 SDG', servicio: 'Neonatología' },
  { id: 'CUNA-03', nombre: 'CUNA-03', piso: 'P3', subtitulo: 'Fototerapia Led', estatus: 'disponible', pacienteInfo: 'Lista para hiperbilirrubinemia', servicio: 'Pediatría' },
  { id: 'PED-301', nombre: 'PED-301', piso: 'P3', subtitulo: 'Pediatría Escolar', estatus: 'ocupada', pacienteInfo: 'Dávila M., Mateo (6a) · Bronquiolitis', servicio: 'Pediatría' },
  { id: 'PED-302', nombre: 'PED-302', piso: 'P3', subtitulo: 'Pediatría Escolar', estatus: 'disponible', pacienteInfo: 'Libre con protección anticaídas', servicio: 'Pediatría' },
  { id: 'GIN-305', nombre: 'GIN-305', piso: 'P3', subtitulo: 'Puerperio Fisiológico', estatus: 'ocupada', pacienteInfo: 'Flores H., Carmen · Parto eutócico', servicio: 'Ginecología' },
  { id: 'GIN-306', nombre: 'GIN-306', piso: 'P3', subtitulo: 'Labor y Recuperación', estatus: 'disponible', pacienteInfo: 'Lista para ingreso de parto', servicio: 'Ginecología' },

  // Quirófanos Centrales
  { id: 'QUI-01', nombre: 'QUI-01', piso: 'QX', subtitulo: 'Sala Mayor Cirugía 1', estatus: 'ocupada', pacienteInfo: 'Cirugía activa: Apendicectomía laparoscópica', servicio: 'Quirófano' },
  { id: 'QUI-02', nombre: 'QUI-02', piso: 'QX', subtitulo: 'Sala Mayor Cirugía 2', estatus: 'ocupada', pacienteInfo: 'Cirugía activa: Colecistectomía', servicio: 'Quirófano' },
  { id: 'QUI-03', nombre: 'QUI-03', piso: 'QX', subtitulo: 'Sala Cirugía Ambulatoria', estatus: 'disponible', pacienteInfo: 'Esterilizado · Turno 11:30 h libre', servicio: 'Quirófano' },
  { id: 'QUI-04', nombre: 'QUI-04', piso: 'QX', subtitulo: 'Trauma y Ortopedia', estatus: 'disponible', pacienteInfo: 'Esterilizado con arco en C', servicio: 'Quirófano' },
  { id: 'QUI-05', nombre: 'QUI-05', piso: 'QX', subtitulo: 'Urgencias QX', estatus: 'higienizacion', pacienteInfo: 'Sanitización terminal en curso', servicio: 'Quirófano' }
])

const camasFiltradasPorPiso = (piso: 'P1' | 'P2' | 'P3' | 'QX') => {
  return matrizCamasHospital.value.filter(c => c.piso === piso)
}

const seleccionarCamaTablero = (cama: CamaHospitalaria) => {
  fichaCamaSeleccionada.value = cama
  preasignacionCamaForm.value.camaSeleccionada = cama.nombre
}

const alternarDemoCenso = () => {
  if (preasignacionCamaForm.value.solicitudSeleccionada) {
    preasignacionCamaForm.value.solicitudSeleccionada = ''
    preasignacionCamaForm.value.camaSeleccionada = ''
    preasignacionCamaForm.value.medicoTratante = ''
    preasignacionCamaForm.value.servicio = ''
  } else {
    preasignacionCamaForm.value.solicitudSeleccionada = 'PAC-9920'
    preasignacionCamaForm.value.camaSeleccionada = 'HABITACIÓN 202-A'
    preasignacionCamaForm.value.medicoTratante = 'Dr. Mauricio Sotomayor'
    preasignacionCamaForm.value.servicio = 'Medicina Interna'
  }
}

const confirmarAsignacionCama = () => {
  if (!preasignacionCamaForm.value.solicitudSeleccionada) {
    mostrarAlerta('Seleccione una solicitud de paciente pendiente de la lista.')
    return
  }
  if (!preasignacionCamaForm.value.camaSeleccionada) {
    mostrarAlerta('Seleccione una cama disponible en el tablero de censos.')
    return
  }
  const cama = matrizCamasHospital.value.find(c => c.nombre === preasignacionCamaForm.value.camaSeleccionada)
  if (cama) {
    cama.estatus = 'reservada'
    cama.pacienteInfo = `Asignada a ${preasignacionCamaForm.value.solicitudSeleccionada}`
  }
  solicitudesCamaPendientes.value = solicitudesCamaPendientes.value.filter(s => s.folio !== preasignacionCamaForm.value.solicitudSeleccionada)
  mostrarAlerta(`<svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Cama ${preasignacionCamaForm.value.camaSeleccionada} asignada con éxito al paciente ${preasignacionCamaForm.value.solicitudSeleccionada}. Notificación enviada a la estación de enfermería de ${preasignacionCamaForm.value.servicio || 'Hospitalización'}.`)
}

const reportarLimpiezaCama = () => {
  if (!fichaCamaSeleccionada.value) return
  fichaCamaSeleccionada.value.estatus = 'higienizacion'
  fichaCamaSeleccionada.value.pacienteInfo = 'Solicitud de sanitización enviada a Intendencia'
  mostrarAlerta(`[INTENDENCIA] Alerta de intendencia despachada para ${fichaCamaSeleccionada.value.nombre}. Prioridad: Higienización estándar.`)
}

const bloquearCama = () => {
  if (!fichaCamaSeleccionada.value) return
  fichaCamaSeleccionada.value.estatus = 'higienizacion'
  fichaCamaSeleccionada.value.pacienteInfo = 'Bloqueada por mantenimiento preventivo'
  mostrarAlerta(`� Cama ${fichaCamaSeleccionada.value.nombre} bloqueada temporalmente en censo hospitalario.`)
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
              <p>Monitoreo sincronizado en tiempo real entre Estaciones de Enfermería, Quirófanos Centrales y Sistema de Admisión Hospitalaria.</p>
            </div>

            <div class="banner-right-kpis-row">
              <button 
                type="button" 
                class="btn-demo-slot"
                @click="alternarDemoCenso"
                title="Cargar datos de pre-asignación demo"
              >
                {{ preasignacionCamaForm.solicitudSeleccionada ? 'Vaciar Datos' : 'Cargar Demo' }}
              </button>
              <button type="button" class="btn-export-censo" @click="mostrarAlerta('Generando censo hospitalario consolidado en formato Excel / PDF...')">
                <svg viewBox="0 0 24 24" class="svg-inline-chart" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg> Exportar Censo Diario
              </button>
              <button type="button" class="btn-alert-janitor" @click="mostrarAlerta('Alerta general despachada a personal de intendencia y camilleros de guardia.')">
                <svg viewBox="0 0 24 24" class="svg-inline-siren" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Alerta Intendencia
              </button>
            </div>
          </div>

          <!-- Filtro de Pisos y Áreas -->
          <div class="censo-floors-filter-bar">
            <div class="floors-tab-chips">
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'todos' }]"
                @click="filtroPisoCenso = 'todos'"
              >
                Todos los Pisos (106 Camas)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P1' }]"
                @click="filtroPisoCenso = 'P1'"
              >
                P1: Urgencias (18 Boxes)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P2' }]"
                @click="filtroPisoCenso = 'P2'"
              >
                P2: Hospitalización Adultos (40 Camas)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'P3' }]"
                @click="filtroPisoCenso = 'P3'"
              >
                P3: Materno / Pediatría (28 Camas)
              </button>
              <button 
                type="button" 
                :class="['floor-chip-btn', { active: filtroPisoCenso === 'QX' }]"
                @click="filtroPisoCenso = 'QX'"
              >
                Quirófanos Centrales (5 Salas)
              </button>
            </div>
          </div>

          <!-- Tarjetas KPI de Resumen Hospitalario (5 Métricas Superiores) -->
          <div class="censo-metrics-grid">
            
            <div class="kpi-card censo-metric">
              <span class="kpi-label">CAMAS CENSABLES</span>
              <strong class="kpi-value text-blue">106 Totales</strong>
              <small class="kpi-sub">82 en estancia activa (77.3% Ocupación)</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">DISPONIBLES INMEDIATAS</span>
              <strong class="kpi-value text-cyan">24 Libres listas</strong>
              <small class="kpi-sub">Pre-asignadas en cola: 5 en proceso</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">EN DESINFECCIÓN</span>
              <strong class="kpi-value text-orange">06 Camas aisladas</strong>
              <small class="kpi-sub">Tiempo est. liberación: ~20 min prom.</small>
            </div>

            <div class="kpi-card censo-metric">
              <span class="kpi-label">QUIRÓFANOS CENTRALES</span>
              <strong class="kpi-value text-purple">3 / 5 Disponibles</strong>
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
                      <small>18 Boxes censables · 6 Libres · 1 En Higienización · 1 Reservado</small>
                    </div>
                  </div>
                  <span class="floor-rate">66.7% Ocupado</span>
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
                      <small>40 Camas censables · 14 Libres · 2 En Higienización · 1 Reservada</small>
                    </div>
                  </div>
                  <span class="floor-rate">72.5% Ocupado</span>
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
                      <small>28 Camas censables · 10 Libres · Cunas Térmicas e Incubadoras activas</small>
                    </div>
                  </div>
                  <span class="floor-rate">64.3% Ocupado</span>
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
                      <h4>Área Quirúrgica Central (Salas Q1 a Q5)</h4>
                      <small>5 Salas de Operación · 3 Disponibles · 2 En Procedimiento Quirúrgico Activo</small>
                    </div>
                  </div>
                  <span class="floor-rate">40% Ocupado</span>
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
                    @click="preasignacionCamaForm.solicitudSeleccionada = sol.folio; preasignacionCamaForm.servicio = sol.servicio"
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
                        ● {{ sol.prioridad }}
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
                    ● {{ fichaCamaSeleccionada.estatus.toUpperCase() }}
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

</style>
