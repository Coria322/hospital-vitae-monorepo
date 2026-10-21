<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

const props = defineProps<{
  curpCompartida?: string
  pacienteSeleccionado?: any
}>()

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
// PANTALLA 3: PROGRAMACIÓN Y ASIGNACIÓN DE CITAS MÉDICAS (IMAGEN 3)
// =========================================================================
const agendamientoForm = ref({
  especialidad: 'Medicina Interna — Consultorio 104 (Pabellón Central)',
  medico: 'Dr. Guillermo Ramírez Vega · Céd. Prof. 8492011 (Medicina Interna)',
  curpBusqueda: props.curpCompartida || '',
  pacienteValidado: null as {
    nombre: string
    edad: number
    folio: string
    curp: string
  } | null,
  tipoIntervencion: 'control' as 'primera' | 'control' | 'preoperatoria',
  diaSeleccionado: 'sab17',
  horaSeleccionada: '11:00',
  folioTurno: 'CITA-2026-1049'
})

const sincronizarPacienteCita = () => {
  if (props.pacienteSeleccionado) {
    const p = props.pacienteSeleccionado
    agendamientoForm.value.curpBusqueda = p.curp || props.curpCompartida || ''
    agendamientoForm.value.pacienteValidado = {
      nombre: p.nombreCompleto || p.nombre || 'Paciente Validado',
      edad: p.edad || 36,
      folio: p.cupe || p.folio || '#EXP-2026-9042',
      curp: p.curp || agendamientoForm.value.curpBusqueda
    }
  } else if (props.curpCompartida) {
    agendamientoForm.value.curpBusqueda = props.curpCompartida
  }
}

watch(() => props.pacienteSeleccionado, () => sincronizarPacienteCita())
watch(() => props.curpCompartida, () => sincronizarPacienteCita())
onMounted(() => sincronizarPacienteCita())

const diasSemanaCitas = ref([
  { id: 'jue15', nombre: 'JUE', num: '15', estado: 'Concluido', disabled: true, cupos: 0 },
  { id: 'vie16', nombre: 'VIE', num: '16', estado: 'Concluido', disabled: true, cupos: 0 },
  { id: 'sab17', nombre: 'SÁB', num: '17', estado: '6 Cupos', hoy: true, cupos: 6 },
  { id: 'dom18', nombre: 'DOM', num: '18', estado: 'Inactivo', disabled: true, cupos: 0 },
  { id: 'lun19', nombre: 'LUN', num: '19', estado: '14 Cupos', cupos: 14 },
  { id: 'mar20', nombre: 'MAR', num: '20', estado: '11 Cupos', cupos: 11 }
])

const horariosSabado = ref([
  { hora: '08:00', estado: 'ocupado', label: 'Ocupado' },
  { hora: '08:30', estado: 'ocupado', label: 'Ocupado' },
  { hora: '09:00', estado: 'ocupado', label: 'Ocupado' },
  { hora: '09:30', estado: 'ocupado', label: 'Ocupado' },
  { hora: '10:00', estado: 'disponible', label: 'Disponible' },
  { hora: '10:30', estado: 'disponible', label: 'Disponible' },
  { hora: '11:00', estado: 'seleccionado', label: 'Seleccionado' },
  { hora: '11:30', estado: 'disponible', label: 'Disponible' },
  { hora: '12:00', estado: 'ocupado', label: 'Ocupado' },
  { hora: '12:30', estado: 'disponible', label: 'Disponible' },
  { hora: '13:00', estado: 'disponible', label: 'Disponible' },
  { hora: '13:30', estado: 'urgencia', label: 'Interconsulta' }
])

const agendaConsultorio104 = ref([
  {
    horario: '08:00 - 08:30',
    folio: 'VTE-8102',
    iniciales: 'M. H. G.',
    curp: 'HEMG710928HDF...',
    modalidad: 'Subsecuente',
    estado: 'Atendida',
    confirmacion: 'Entregado'
  },
  {
    horario: '08:30 - 09:00',
    folio: 'VTE-8219',
    iniciales: 'S. R. L.',
    curp: 'ROLS650112MDF...',
    modalidad: 'Primera Vez',
    estado: 'Atendida',
    confirmacion: 'Entregado'
  },
  {
    horario: '09:30 - 10:00',
    folio: 'VTE-8409',
    iniciales: 'L. C. T.',
    curp: 'COTL790310HDF...',
    modalidad: 'Subsecuente',
    estado: 'En Sala de Espera',
    confirmacion: 'Reenviar Aviso'
  },
  {
    horario: '10:00 - 10:30',
    folio: 'VTE-8755',
    iniciales: 'R. D. B.',
    curp: 'DIBR881104HDF...',
    modalidad: 'Preoperatoria',
    estado: 'Confirmada',
    confirmacion: 'WhatsApp'
  }
])

const seleccionarHorarioCita = (slot: typeof horariosSabado.value[0]) => {
  if (slot.estado === 'ocupado') {
    mostrarAlerta('El horario de las ' + slot.hora + ' se encuentra ocupado por otro paciente.')
    return
  }
  horariosSabado.value.forEach(s => {
    if (s.estado === 'seleccionado') s.estado = 'disponible'
  })
  slot.estado = 'seleccionado'
  agendamientoForm.value.horaSeleccionada = slot.hora
}

const buscarPacienteParaCita = () => {
  if (!agendamientoForm.value.curpBusqueda) {
    mostrarAlerta('Ingrese una CURP o Folio de paciente para agendar cita.')
    return
  }
  agendamientoForm.value.pacienteValidado = {
    nombre: 'Álvarez Morales, Antonio',
    edad: 42,
    folio: 'VTE-8812',
    curp: agendamientoForm.value.curpBusqueda.toUpperCase()
  }
}

const cargarDemoCita = () => {
  if (agendamientoForm.value.pacienteValidado) {
    agendamientoForm.value.curpBusqueda = ''
    agendamientoForm.value.pacienteValidado = null
  } else {
    agendamientoForm.value.curpBusqueda = 'ALMA820419HDFRNR02'
    agendamientoForm.value.pacienteValidado = {
      nombre: 'Álvarez Morales, Antonio',
      edad: 42,
      folio: 'VTE-8812',
      curp: 'ALMA820419HDFRNR02'
    }
  }
}

const confirmarCitaExpedirComprobante = () => {
  if (!agendamientoForm.value.pacienteValidado) {
    mostrarAlerta('Seleccione o valide un paciente antes de confirmar la cita médica.')
    return
  }
  mostrarAlerta('✓ Cita ' + agendamientoForm.value.folioTurno + ' confirmada con éxito a las ' + agendamientoForm.value.horaSeleccionada + ' hrs para ' + agendamientoForm.value.pacienteValidado.nombre + '.')
}
</script>

<template>
<div class="view-agendar-container">
          
          <!-- Encabezado de Agendamiento -->
          <div class="section-top-banner">
            <div class="banner-with-icon-col">
              <div class="blue-icon-square">
                <span>📅</span>
              </div>
              <div class="banner-text-col">
                <div class="badges-row">
                  <span class="cert-pill blue">MÓDULO DE ADMISIÓN · CONSULTA EXTERNA</span>
                  <span class="cert-pill cyan">● Turno Matutino (07:00 - 15:00)</span>
                </div>
                <h2>Programación y Asignación de Citas Médicas</h2>
              </div>
            </div>

            <div class="banner-right-kpis-row">
              <div class="mini-kpi-white">
                <span class="kpi-symbol">🩺</span>
                <div>
                  <small>ESPECIALIDADES ACTIVAS</small>
                  <strong>5 Servicios · 14 Consultorios</strong>
                </div>
              </div>

              <div class="mini-kpi-white">
                <span class="kpi-symbol blue">📅</span>
                <div>
                  <small>DISPONIBILIDAD HOY</small>
                  <strong class="text-blue">38 Cupos Libres</strong>
                </div>
              </div>

              <button type="button" class="btn-sync-catalogs" @click="mostrarAlerta('Sincronizando agendas médicas y disponibilidad con PostgreSQL...')">
                🔄 Sincronizar Catálogos
              </button>
            </div>
          </div>

          <!-- Cuadrícula Principal de Agendamiento (Izquierda Formulario/Calendario + Derecha Resumen) -->
          <div class="curp-layout-grid">
            
            <!-- SUBCOLUMNA IZQUIERDA -->
            <div class="curp-left-col">
              
              <!-- 1. Parámetros del Agendamiento -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="step-num-title">
                    <span class="step-circle">1-4</span>
                    <h4>Parámetros del Agendamiento</h4>
                  </div>
                  <span class="live-dot-tag">● Validación de Cédula y Cobertura Activa</span>
                </div>

                <div class="step-body">
                  <!-- Fila 1: Especialidad y Médico -->
                  <div class="grid-2-cols">
                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span>🏥</span>
                        <strong>Paso 1: Especialidad y Consultorio</strong>
                      </div>
                      <select v-model="agendamientoForm.especialidad" class="select-clean full">
                        <option>Medicina Interna — Consultorio 104 (Pabellón Central)</option>
                        <option>Cardiología Clínica — Consultorio 108</option>
                        <option>Cirugía General — Consultorio 112</option>
                        <option>Pediatría Médica — Consultorio 201</option>
                        <option>Ginecología y Obstetricia — Consultorio 205</option>
                      </select>
                      <small class="field-hint">Ubicación asignada en tiempo real según roles del día</small>
                    </div>

                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span>👨‍⚕️</span>
                        <strong>Paso 2: Médico Tratante Asignado</strong>
                      </div>
                      <select v-model="agendamientoForm.medico" class="select-clean full">
                        <option>Dr. Guillermo Ramírez Vega · Céd. Prof. 8492011 (Medicina Interna)</option>
                        <option>Dra. Sofía Alarcón Castillo · Céd. Prof. 9410291 (Cardiología)</option>
                        <option>Dr. Carlos Morales Peña · Céd. Prof. 4029103 (Cirugía)</option>
                      </select>
                      <small class="field-hint text-blue">Estado: En funciones · Capacidad máxima 16 citas/día</small>
                    </div>
                  </div>

                  <!-- Fila 2: Paciente y Tipo de Consulta -->
                  <div class="grid-2-cols margin-top-sm">
                    <!-- Paso 3: Paciente -->
                    <div class="field-subcard">
                      <div class="subcard-header-row">
                        <div class="subcard-header-icon">
                          <span>👤</span>
                          <strong>Paso 3: Paciente (CURP o Folio Único)</strong>
                        </div>
                        <span class="nom-micro">Validación NOM-024</span>
                      </div>

                      <div class="search-patient-slot-row">
                        <input 
                          type="text" 
                          v-model="agendamientoForm.curpBusqueda" 
                          placeholder="ALMA820419HDFRNR02"
                          class="input-clean flex-1 monospace"
                          @keyup.enter="buscarPacienteParaCita"
                        />
                        <button type="button" class="btn-search-slot" @click="buscarPacienteParaCita">
                          🔍 Buscar
                        </button>
                        <button 
                          type="button" 
                          class="btn-demo-slot"
                          @click="cargarDemoCita"
                        >
                          {{ agendamientoForm.pacienteValidado ? '🧹' : '🧪' }}
                        </button>
                      </div>

                      <div v-if="agendamientoForm.pacienteValidado" class="patient-validated-pill">
                        <span class="check-blue">✓</span>
                        <span>Paciente validado: <strong>{{ agendamientoForm.pacienteValidado.nombre }} ({{ agendamientoForm.pacienteValidado.edad }} años)</strong> · Folio {{ agendamientoForm.pacienteValidado.folio }}</span>
                      </div>
                      <div v-else class="patient-validated-pill empty">
                        <span>Esperando selección o ingreso de paciente...</span>
                      </div>
                    </div>

                    <!-- Paso 4: Tipo de Intervención -->
                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span>🩺</span>
                        <strong>Tipo de Intervención / Consulta</strong>
                      </div>

                      <div class="intervention-types-row">
                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'primera' }"
                          @click="agendamientoForm.tipoIntervencion = 'primera'"
                        >
                          <strong>Primera Vez</strong>
                          <small>45 min</small>
                        </button>

                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'control' }"
                          @click="agendamientoForm.tipoIntervencion = 'control'"
                        >
                          <strong>Control / Subs.</strong>
                          <small>30 min</small>
                        </button>

                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'preoperatoria' }"
                          @click="agendamientoForm.tipoIntervencion = 'preoperatoria'"
                        >
                          <strong>Preoperatoria</strong>
                          <small>30 min</small>
                        </button>
                      </div>
                      <small class="field-hint">Protocolo de seguimiento con análisis de laboratorio requeridos</small>
                    </div>
                  </div>

                </div>
              </div>

              <!-- 2. Calendario de Turnos (Paso 4) -->
              <div class="step-card">
                <div class="step-card-header">
                  <div>
                    <span class="step-label-blue">PASO 4: CALENDARIO DE TURNOS</span>
                    <h4>Octubre 2026 · Turno Matutino Consultorio 104</h4>
                  </div>
                  
                  <div class="calendar-legend-row">
                    <span class="legend-item"><span class="box-legend free"></span> Libre</span>
                    <span class="legend-item"><span class="box-legend selected"></span> Seleccionado</span>
                    <span class="legend-item"><span class="box-legend occupied"></span> Ocupado</span>
                    <span class="legend-item"><span class="box-legend urgency"></span> Urgencia</span>
                  </div>
                </div>

                <div class="step-body">
                  <!-- Selector de Días de la Semana -->
                  <div class="days-tabs-grid">
                    <button 
                      v-for="dia in diasSemanaCitas" 
                      :key="dia.id"
                      type="button"
                      class="day-tab-card"
                      :class="{ 
                        disabled: dia.disabled, 
                        active: agendamientoForm.diaSeleccionado === dia.id,
                        today: dia.hoy 
                      }"
                      @click="!dia.disabled && (agendamientoForm.diaSeleccionado = dia.id)"
                    >
                      <span class="day-name">{{ dia.nombre }} {{ dia.num }} {{ dia.hoy ? '(HOY)' : '' }}</span>
                      <strong class="day-number">{{ dia.num }}</strong>
                      <span class="day-status" :class="{ highlight: dia.cupos > 0 }">{{ dia.estado }}</span>
                    </button>
                  </div>

                  <!-- Horarios Disponibles para Sábado 17 -->
                  <div class="available-hours-container">
                    <span class="hours-header-title">Horarios Disponibles para Sábado 17 de Octubre:</span>

                    <div class="slots-hours-grid">
                      <button 
                        v-for="slot in horariosSabado" 
                        :key="slot.hora"
                        type="button"
                        class="slot-btn"
                        :class="slot.estado"
                        @click="seleccionarHorarioCita(slot)"
                      >
                        <span class="slot-time">{{ slot.hora }}</span>
                        <small class="slot-label">{{ slot.label }}</small>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 3. Agenda del Día · Consultorio 104 -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="title-with-icon-sm">
                    <span>📋</span>
                    <div>
                      <h4>Agenda del Día · Consultorio 104</h4>
                      <small class="text-subtle">Pacientes asignados al Dr. Guillermo Ramírez Vega para hoy</small>
                    </div>
                  </div>
                  <span class="badge-neutral-sm">8 Citas Registradas</span>
                </div>

                <div class="step-body">
                  <div class="table-responsive">
                    <table class="history-table">
                      <thead>
                        <tr>
                          <th>HORARIO</th>
                          <th>FOLIO / INICIALES</th>
                          <th>MODALIDAD</th>
                          <th>ESTADO</th>
                          <th class="text-right">CONFIRMACIÓN OFICIAL</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(cita, idx) in agendaConsultorio104" :key="idx">
                          <td class="col-time font-bold">{{ cita.horario }}</td>
                          <td class="col-patient">
                            <strong>{{ cita.folio }} · {{ cita.iniciales }}</strong>
                            <small class="block-curp">CURP: {{ cita.curp }}</small>
                          </td>
                          <td><span class="pill-modalidad">{{ cita.modalidad }}</span></td>
                          <td>
                            <span class="status-pill" :class="{ attended: cita.estado === 'Atendida', waiting: cita.estado.includes('Espera'), confirmed: cita.estado === 'Confirmada' }">
                              ● {{ cita.estado }}
                            </span>
                          </td>
                          <td class="text-right">
                            <span v-if="cita.confirmacion === 'Entregado'" class="text-green-check">✓ Entregado</span>
                            <button v-else-if="cita.confirmacion === 'Reenviar Aviso'" class="btn-resend" @click="mostrarAlerta('Aviso reenviado a sala de espera.')">
                              ▷ Reenviar Aviso
                            </button>
                            <span v-else class="text-whatsapp">💬 WhatsApp</span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

            </div>

            <!-- SUBCOLUMNA DERECHA: Resumen de Cita y SLA -->
            <aside class="curp-right-col">
              
              <!-- Tarjeta Resumen de Cita -->
              <div class="side-widget-card appointment-summary-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span>☑️</span>
                    <strong>Resumen de Cita</strong>
                  </div>
                  <span class="badge-confirm-pending">POR CONFIRMAR</span>
                </div>

                <!-- Bloque Destacado de Turno y Hora -->
                <div class="appointment-highlight-box">
                  <div class="highlight-top">
                    <small>FOLIO DE TURNO ASIGNADO</small>
                    <strong class="highlight-folio">{{ agendamientoForm.folioTurno }}</strong>
                  </div>
                  <div class="highlight-time-row">
                    <span class="highlight-big-hour">{{ agendamientoForm.horaSeleccionada }}</span>
                    <span class="highlight-date-badge">HRS · SÁB 17 OCT</span>
                  </div>
                  <small class="highlight-sub">Duración estimada: 30 minutos (Espacio reservado en agenda del médico).</small>
                </div>

                <!-- Desglose de Parámetros -->
                <div class="appointment-details-stack">
                  <div class="detail-row-item">
                    <span class="detail-icon">👤</span>
                    <div>
                      <small>PACIENTE REGISTRADO</small>
                      <strong>{{ agendamientoForm.pacienteValidado?.nombre || 'Sin paciente seleccionado' }}</strong>
                      <span class="detail-meta">CURP: {{ agendamientoForm.pacienteValidado?.curp || 'ALMA820419HDFRNR02' }}</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon">🏥</span>
                    <div>
                      <small>ESPECIALIDAD & MÉDICO</small>
                      <strong>Medicina Interna</strong>
                      <span class="detail-meta">Dr. Guillermo Ramírez Vega (Céd. 8492011)</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon">📍</span>
                    <div>
                      <small>UBICACIÓN FÍSICA</small>
                      <strong>Consultorio 104 · Planta Baja</strong>
                      <span class="detail-meta">Pabellón Central de Consulta Externa</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon">📋</span>
                    <div>
                      <small>TIPO DE CITA</small>
                      <strong class="text-blue">Control Subsecuente</strong>
                      <span class="detail-meta">Seguimiento de Cuadro Hipertensivo</span>
                    </div>
                  </div>
                </div>

                <!-- Requerimientos de Presentación -->
                <div class="requirements-box">
                  <strong>REQUERIMIENTOS DE PRESENTACIÓN</strong>
                  <ul>
                    <li>✓ Presentarse 15 min antes con identificación oficial</li>
                    <li>✓ Traer carnet de citas y receta anterior</li>
                    <li>✓ Estudios de química sanguínea recientes</li>
                  </ul>
                </div>

                <!-- Botones de Acción -->
                <div class="appointment-actions-stack">
                  <button type="button" class="btn-action-primary" @click="confirmarCitaExpedirComprobante">
                    Confirmar y Expedir Comprobante [Enter]
                  </button>
                  <button type="button" class="btn-action-secondary" @click="mostrarAlerta('Imprimiendo comprobante térmico con código QR...')">
                    🖨️ Imprimir Recordatorio Térmico
                  </button>
                  <div class="grid-2-cols">
                    <button type="button" class="btn-subtle-msg" @click="mostrarAlerta('SMS enviado al teléfono móvil del paciente.')">
                      💬 SMS Paciente
                    </button>
                    <button type="button" class="btn-subtle-msg" @click="mostrarAlerta('Notificación enviada a WhatsApp con enlace de confirmación.')">
                      💬 WhatsApp
                    </button>
                  </div>
                </div>

                <!-- Nota NOM-024 -->
                <div class="summary-audit-foot">
                  <span>🛡️</span>
                  <small>Registro auditado de acuerdo con la <strong>NOM-024-SSA3-2012</strong> de Sistemas de Información de Registro Electrónico para la Salud.</small>
                </div>
              </div>

              <!-- Widget de SLA de Espera -->
              <div class="side-widget-card sla-widget-card">
                <div class="sla-row">
                  <span class="sla-clock-icon">⏱️</span>
                  <div>
                    <small>TIEMPO PROMEDIO ESPERA</small>
                    <strong>08 min · Óptimo Clínico</strong>
                  </div>
                  <span class="sla-badge-success">SLA CUMPLIDO</span>
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
   PANTALLA 3: PROGRAMACIÓN Y ASIGNACIÓN DE CITAS MÉDICAS (IMAGEN 3)
   ========================================================================= */
.view-agendar-container, .view-consentimiento-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.banner-with-icon-col {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.blue-icon-square {
  width: 44px;
  height: 44px;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
}

.banner-right-kpis-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.mini-kpi-white {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.45rem 0.8rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.kpi-symbol {
  font-size: 1.15rem;
}

.mini-kpi-white small {
  display: block;
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748B;
}

.mini-kpi-white strong {
  font-size: 0.75rem;
  color: #1E293B;
}

.btn-sync-catalogs {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.55rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-sync-catalogs:hover {
  background-color: #F8FAFC;
}

.grid-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
}

.margin-top-sm {
  margin-top: 0.8rem;
}

.field-subcard {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.subcard-header-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: #1E293B;
}

.subcard-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nom-micro {
  font-size: 0.65rem;
  color: #0284C7;
  font-weight: 700;
}

.field-hint {
  font-size: 0.68rem;
  color: #64748B;
}

.search-patient-slot-row {
  display: flex;
  gap: 0.4rem;
}

.btn-search-slot {
  background-color: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0 0.85rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-demo-slot {
  background-color: #F0FDF4;
  border: 1px solid #86EFAC;
  color: #166534;
  border-radius: 6px;
  padding: 0 0.65rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.patient-validated-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #F0FDF4;
  border: 1px solid #BBF7D0;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.7rem;
  color: #166534;
}

.patient-validated-pill.empty {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  color: #94A3B8;
}

.check-blue {
  color: #0284C7;
  font-weight: 800;
}

.intervention-types-row {
  display: flex;
  gap: 0.4rem;
}

.btn-intervention {
  flex: 1;
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.25rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-intervention strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.btn-intervention small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.btn-intervention.active {
  background-color: #0284C7;
  border-color: #0284C7;
}

.btn-intervention.active strong, .btn-intervention.active small {
  color: #FFFFFF;
}

.step-label-blue {
  font-size: 0.68rem;
  font-weight: 700;
  color: #0284C7;
}

.calendar-legend-row {
  display: flex;
  gap: 0.6rem;
  font-size: 0.68rem;
  color: #475569;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.box-legend {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

.box-legend.free { background-color: #EFF6FF; border: 1px solid #BFDBFE; }
.box-legend.selected { background-color: #2563EB; }
.box-legend.occupied { background-color: #F1F5F9; border: 1px solid #E2E8F0; }
.box-legend.urgency { background-color: #FEE2E2; border: 1px solid #FECACA; }

.days-tabs-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.day-tab-card {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.6rem 0.35rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  cursor: pointer;
  transition: all 0.15s;
}

.day-tab-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.day-tab-card.today {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border-color: #0F2D4A;
}

.day-tab-card.today .day-name, .day-tab-card.today .day-number, .day-tab-card.today .day-status {
  color: #FFFFFF;
}

.day-name {
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748B;
}

.day-number {
  font-size: 1.1rem;
  font-weight: 800;
  color: #1E293B;
}

.day-status {
  font-size: 0.65rem;
  color: #64748B;
}

.day-status.highlight {
  color: #0284C7;
  font-weight: 700;
}

.available-hours-container {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.hours-header-title {
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E293B;
}

.slots-hours-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.5rem;
}

.slot-btn {
  padding: 0.5rem 0.25rem;
  border-radius: 6px;
  text-align: center;
  border: 1px solid #E2E8F0;
  cursor: pointer;
  transition: all 0.15s;
}

.slot-btn.ocupado {
  background-color: #F1F5F9;
  color: #94A3B8;
  cursor: not-allowed;
}

.slot-btn.disponible {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: #1D4ED8;
}

.slot-btn.seleccionado {
  background-color: #2563EB;
  border-color: #2563EB;
  color: #FFFFFF;
}

.slot-btn.seleccionado .slot-time, .slot-btn.seleccionado .slot-label {
  color: #FFFFFF;
}

.slot-btn.urgencia {
  background-color: #FEF2F2;
  border-color: #FECACA;
  color: #DC2626;
}

.slot-time {
  display: block;
  font-size: 0.75rem;
  font-weight: 800;
}

.slot-label {
  display: block;
  font-size: 0.6rem;
}

.pill-modalidad {
  font-size: 0.68rem;
  background-color: #F1F5F9;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-pill.attended { background-color: #F1F5F9; color: #64748B; }
.status-pill.waiting { background-color: #E0F2FE; color: #0284C7; }
.status-pill.confirmed { background-color: #DCFCE7; color: #166534; }

.text-green-check { color: #059669; font-weight: 700; font-size: 0.72rem; }
.btn-resend { background: none; border: 1px solid #0284C7; color: #0284C7; font-size: 0.68rem; font-weight: 700; border-radius: 4px; padding: 0.2rem 0.5rem; cursor: pointer; }
.text-whatsapp { color: #166534; font-size: 0.72rem; font-weight: 600; }

/* RESUMEN DE CITA DERECHA */
.appointment-summary-card {
  gap: 0.85rem;
}

.badge-confirm-pending {
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.appointment-highlight-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.highlight-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.68rem;
  color: #64748B;
}

.highlight-folio {
  color: #0F4C81;
  font-family: monospace;
  font-weight: 800;
  font-size: 0.95rem;
}

.highlight-time-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.highlight-big-hour {
  font-size: 1.8rem;
  font-weight: 800;
  color: #0F2D4A;
}

.highlight-date-badge {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748B;
  background-color: #E2E8F0;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.highlight-sub {
  font-size: 0.68rem;
  color: #64748B;
}

.appointment-details-stack {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  border-top: 1px solid #F1F5F9;
  padding-top: 0.8rem;
}

.detail-row-item {
  display: flex;
  gap: 0.6rem;
}

.detail-icon {
  font-size: 1rem;
}

.detail-row-item small {
  display: block;
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748B;
}

.detail-row-item strong {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
}

.detail-meta {
  font-size: 0.68rem;
  color: #64748B;
}

.requirements-box {
  background-color: #F0F9FF;
  border: 1px solid #BAE6FD;
  border-radius: 8px;
  padding: 0.75rem;
}

.requirements-box strong {
  font-size: 0.68rem;
  color: #0369A1;
  display: block;
  margin-bottom: 0.35rem;
}

.requirements-box ul {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 0.68rem;
  color: #0C4A6E;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.appointment-actions-stack {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.btn-subtle-msg {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-subtle-msg:hover {
  background-color: #F1F5F9;
}

.summary-audit-foot {
  display: flex;
  gap: 0.4rem;
  font-size: 0.65rem;
  color: #64748B;
  line-height: 1.3;
}

.sla-widget-card {
  padding: 0.75rem 1rem;
}

.sla-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sla-clock-icon {
  font-size: 1.2rem;
}

.sla-row small {
  display: block;
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748B;
}

.sla-row strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.sla-badge-success {
  background-color: #DCFCE7;
  color: #15803D;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

</style>
