<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { Paciente, Doctor } from '../types'

const props = defineProps<{
  doctor: Doctor
  pacienteSeleccionado: Paciente | null
  pacientesCola?: Paciente[]
  diagnosticoGlobal?: string
}>()

const emit = defineEmits<{
  (e: 'seleccionarPaciente', paciente: Paciente): void
  (e: 'deseleccionarPaciente'): void
  (e: 'actualizarDiagnostico', d: string): void
  (e: 'enviarSolicitud'): void
}>()

// Filtros y pestaña activa
const pestanaActiva = ref<'lab' | 'imagen' | 'gabinete'>('lab')
const busquedaFiltro = ref('')

// Datos de paciente y orden (heredados automáticamente si están en sesión)
const pacienteInput = ref('')
const prioridadOperativa = ref<'ordinaria' | 'urgente' | 'critica'>('urgente')
const diagnosticoCie = ref(props.diagnosticoGlobal || props.pacienteSeleccionado?.motivo || '')
const justificacionTecnica = ref('')
const modalidadToma = ref<'box' | 'gabinete'>('box')

watch(() => props.diagnosticoGlobal, (nuevo) => {
  if (nuevo && !diagnosticoCie.value) {
    diagnosticoCie.value = nuevo
  }
})

watch(diagnosticoCie, (val) => {
  if (val) emit('actualizarDiagnostico', val)
})

onMounted(() => {
  if (props.diagnosticoGlobal && !diagnosticoCie.value) {
    diagnosticoCie.value = props.diagnosticoGlobal
  } else if (props.pacienteSeleccionado?.motivo && !diagnosticoCie.value) {
    diagnosticoCie.value = props.pacienteSeleccionado.motivo
  }
})

// Catálogo de estudios
interface Estudio {
  id: string
  nombre: string
  subtexto: string
  tiempo: string
  categoria: 'lab' | 'imagen' | 'gabinete'
  stat?: boolean
}

const catalogoEstudios = ref<Estudio[]>([
  {
    id: 'bhc',
    nombre: 'Biometría Hemática Completa (BHC)',
    subtexto: 'Eritrocitos, Leucocitos, Fórmula diferencial y Plaquetas',
    tiempo: '30 min',
    categoria: 'lab'
  },
  {
    id: 'qs6',
    nombre: 'Química Sanguínea (6 elementos)',
    subtexto: 'Glucosa, Urea, BUN, Creatinina, Ácido Úrico, Colesterol',
    tiempo: '45 min',
    categoria: 'lab'
  },
  {
    id: 'qs24',
    nombre: 'Química Sanguínea Extendida (24 elem)',
    subtexto: 'Perfil metabólico amplio y electrolitos integrados',
    tiempo: '60 min',
    categoria: 'lab'
  },
  {
    id: 'lipidos',
    nombre: 'Perfil de Lípidos Completo',
    subtexto: 'Colesterol Total, HDL, LDL, VLDL y Triglicéridos',
    tiempo: '60 min',
    categoria: 'lab'
  },
  {
    id: 'coag',
    nombre: 'Tiempos Coagulación (TP, TTP, INR)',
    subtexto: 'Evaluación hemostática prequirúrgica y anticoagulación',
    tiempo: '35 min',
    categoria: 'lab'
  },
  {
    id: 'enzimas',
    nombre: 'Enzimas Cardíacas (Troponina I, CPK)',
    subtexto: 'Troponina I Ultrasensible, CPK-MB masa, Dímero D',
    tiempo: '25 min STAT',
    categoria: 'lab',
    stat: true
  },
  {
    id: 'ego',
    nombre: 'Examen General de Orina (EGO)',
    subtexto: 'Físico-químico y examen microscópico de sedimento',
    tiempo: '40 min',
    categoria: 'lab'
  },
  {
    id: 'gaso',
    nombre: 'Gasometría Arterial (pH, pO2, pCO2)',
    subtexto: 'Equilibrio ácido-base, gradiente alvéolo-arterial y lactato',
    tiempo: '15 min STAT',
    categoria: 'lab',
    stat: true
  },
  {
    id: 'pfh',
    nombre: 'Pruebas de Función Hepática (PFH)',
    subtexto: 'Bilirrubinas (T, D, I), TGO/AST, TGP/ALT, FA, Albúmina',
    tiempo: '50 min',
    categoria: 'lab'
  },
  {
    id: 'hemo',
    nombre: 'Hemocultivo x2 Frascos (Aerobio/Anaerobio)',
    subtexto: 'Detección bacteriológica con reporte preliminar a 24h',
    tiempo: 'Incubación',
    categoria: 'lab'
  },
  // Imagenología
  {
    id: 'rx-torax',
    nombre: 'Radiografía de Tórax PA y Lateral',
    subtexto: 'Silueta cardíaca, parénquima pulmonar y ángulos costofrénicos',
    tiempo: '20 min',
    categoria: 'imagen'
  },
  {
    id: 'tc-craneo',
    nombre: 'Tomografía Axial Computarizada de Cráneo Simple',
    subtexto: 'Descarte de EVC isquémico / hemorrágico o traumatismo agudo',
    tiempo: '25 min STAT',
    categoria: 'imagen',
    stat: true
  },
  // Gabinete
  {
    id: 'ecg',
    nombre: 'Electrocardiograma de 12 Derivaciones',
    subtexto: 'Trazado con ritmo, eje, intervalos y segmento ST en reposo',
    tiempo: '10 min STAT',
    categoria: 'gabinete',
    stat: true
  }
])

// Estudios seleccionados
const seleccionadosIds = ref<string[]>([])

const toggleEstudio = (id: string) => {
  if (seleccionadosIds.value.includes(id)) {
    seleccionadosIds.value = seleccionadosIds.value.filter(i => i !== id)
  } else {
    seleccionadosIds.value.push(id)
  }
}

const estudiosFiltrados = computed(() => {
  return catalogoEstudios.value.filter(e => {
    const matchCat = e.categoria === pestanaActiva.value
    const matchTxt = !busquedaFiltro.value || 
      e.nombre.toLowerCase().includes(busquedaFiltro.value.toLowerCase()) ||
      e.subtexto.toLowerCase().includes(busquedaFiltro.value.toLowerCase())
    return matchCat && matchTxt
  })
})

const totalEstudiosCat = (cat: 'lab' | 'imagen' | 'gabinete') => {
  return seleccionadosIds.value.filter(id => {
    const est = catalogoEstudios.value.find(e => e.id === id)
    return est?.categoria === cat
  }).length
}

const limpiarFormulario = () => {
  if (confirm('¿Desea limpiar toda la orden diagnóstica?')) {
    seleccionadosIds.value = []
    pacienteInput.value = ''
    prioridadOperativa.value = 'ordinaria'
    diagnosticoCie.value = ''
    justificacionTecnica.value = ''
    modalidadToma.value = 'box'
  }
}

const enviarSolicitud = () => {
  if (seleccionadosIds.value.length === 0) {
    alert('Seleccione al menos un estudio del catálogo antes de enviar.')
    return
  }
  emit('enviarSolicitud')
  alert(`Solicitud enviada a Laboratorio/Imagen con ${seleccionadosIds.value.length} estudios programados (Sincronización HL7/FHIR v4).`)
}
</script>

<template>
  <div class="solicitar-lab-view">
    <!-- SUB-ENCABEZADO / BREADCRUMBS & ACCIONES -->
    <div class="lab-subnav">
      <div class="subnav-left">
        <span class="breadcrumb-trail">ATENCIÓN CLÍNICA / AUXILIARES DE DIAGNÓSTICO /</span>
        <strong class="breadcrumb-current">SOLICITUD ELECTRÓNICA DE ESTUDIOS CLÍNICOS</strong>
        <div class="title-row">
          <h2>Nueva Orden Diagnóstica</h2>
          <span class="badge-format">● Formato ECE-LAB-2026</span>
        </div>
      </div>
      <div class="subnav-right">
        <span class="tag-shortcut">⚡ Atajo directo: <kbd>Alt + L</kbd></span>
        <button class="btn-clear" type="button" @click="limpiarFormulario">
          🔄 Limpiar Formulario
        </button>
      </div>
    </div>

    <!-- GRID DE 2 COLUMNAS (FORMULARIO PRINCIPAL + RESUMEN DERECHO) -->
    <div class="lab-grid">
      
      <!-- COLUMNA IZQUIERDA: PARÁMETROS Y CATÁLOGO -->
      <div class="lab-left-column">
        
        <!-- CARD 1: PARÁMETROS DE LA ORDEN Y PACIENTE -->
        <section class="lab-card">
          <div class="card-header-row">
            <div class="title-with-num">
              <span class="step-num">1.</span>
              <h3>Parámetros de la Orden y Paciente</h3>
            </div>
            <span class="badge-required">OBLIGATORIO</span>
          </div>

          <div class="card-body-flow">
            <!-- VINCULACIÓN DE PACIENTE: Muestra paciente conectado si existe -->
            <div v-if="pacienteSeleccionado" class="patient-connected-banner-clean">
              <div class="patient-id-group">
                <span class="avatar-pill">{{ pacienteSeleccionado.nombre.split(' ').map(n => n[0]).slice(0, 2).join('') }}</span>
                <div>
                  <strong>{{ pacienteSeleccionado.nombre }}</strong>
                  <span>ECE {{ pacienteSeleccionado.ece }} ({{ pacienteSeleccionado.folio }}) · {{ pacienteSeleccionado.edad }} años · {{ pacienteSeleccionado.box }}</span>
                </div>
              </div>
              <button class="btn-change-patient-mini" @click="$emit('deseleccionarPaciente')" title="Cambiar paciente">✕</button>
            </div>

            <div v-else class="field-group">
              <div class="label-row">
                <label class="field-label">Vinculación de Paciente Receptor</label>
                <span class="meta-label">Búsqueda en padrón activo</span>
              </div>
              <div class="input-with-button">
                <div class="input-wrapper">
                  <span class="input-icon">👤</span>
                  <input
                    v-model="pacienteInput"
                    type="text"
                    class="input-clean"
                    placeholder="Seleccione un paciente de la lista o ingrese Folio/CURP para vincular..."
                  />
                </div>
                <button class="btn-explore" type="button">Explorar cola</button>
              </div>
              <small class="help-text">Sin paciente preseleccionado. Ingrese folio institucional de 8 dígitos, CURP o nombre completo.</small>
            </div>

            <!-- FILA DE PRIORIDAD Y DIAGNÓSTICO -->
            <div class="two-cols-row">
              <div class="field-group">
                <label class="field-label">Prioridad Operativa de Toma y Procesamiento</label>
                <div class="priority-cards-grid">
                  <div
                    class="priority-card"
                    :class="{ active: prioridadOperativa === 'ordinaria' }"
                    @click="prioridadOperativa = 'ordinaria'"
                  >
                    <div class="priority-title">Ordinaria 🕒</div>
                    <small>Consulta Externa</small>
                  </div>

                  <div
                    class="priority-card"
                    :class="{ active: prioridadOperativa === 'urgente' }"
                    @click="prioridadOperativa = 'urgente'"
                  >
                    <div class="priority-title">Urgente ⏱️</div>
                    <small>Box Urgencias</small>
                  </div>

                  <div
                    class="priority-card alert"
                    :class="{ active: prioridadOperativa === 'critica' }"
                    @click="prioridadOperativa = 'critica'"
                  >
                    <div class="priority-title">Crítica ⚠️</div>
                    <small>Choque / Reanim.</small>
                  </div>
                </div>
              </div>

              <div class="field-group">
                <label class="field-label">Diagnóstico Presuntivo Orientativo (CIE-10)</label>
                <div class="input-wrapper">
                  <span class="input-icon">📋</span>
                  <input
                    v-model="diagnosticoCie"
                    type="text"
                    class="input-clean"
                    placeholder="Código CIE-10 o descripción clínica..."
                  />
                </div>
                <small class="help-text">Requerido para justificación médica de acuerdo a NOM-004-SSA3-2012.</small>
              </div>
            </div>
          </div>
        </section>

        <!-- CARD 2: CATÁLOGO DE ESTUDIOS CLÍNICOS -->
        <section class="lab-card">
          <div class="card-header-row">
            <div class="title-with-num">
              <span class="step-num">2.</span>
              <h3>Catálogo de Estudios Clínicos</h3>
            </div>
            <div class="search-box">
              <span class="search-icon">🔍</span>
              <input
                v-model="busquedaFiltro"
                type="text"
                placeholder="Filtrar pruebas por nombre..."
                class="search-input"
              />
            </div>
          </div>

          <!-- PESTAÑAS DEL CATÁLOGO -->
          <div class="catalog-tabs">
            <button
              class="tab-btn"
              :class="{ active: pestanaActiva === 'lab' }"
              @click="pestanaActiva = 'lab'"
              type="button"
            >
              <span class="tab-icon">🧪</span>
              <span>Laboratorio Clínico</span>
              <span class="tab-badge">{{ totalEstudiosCat('lab') }}</span>
            </button>

            <button
              class="tab-btn"
              :class="{ active: pestanaActiva === 'imagen' }"
              @click="pestanaActiva = 'imagen'"
              type="button"
            >
              <span class="tab-icon">📅</span>
              <span>Imagenología y Rayos X</span>
              <span class="tab-badge">{{ totalEstudiosCat('imagen') }}</span>
            </button>

            <button
              class="tab-btn"
              :class="{ active: pestanaActiva === 'gabinete' }"
              @click="pestanaActiva = 'gabinete'"
              type="button"
            >
              <span class="tab-icon">〽️</span>
              <span>Gabinete y Fisiología</span>
              <span class="tab-badge">{{ totalEstudiosCat('gabinete') }}</span>
            </button>
          </div>

          <div class="catalog-header-info">
            <span class="cat-subtitle">Pruebas hemáticas, urinarias y perfiles metabólicos estandarizados</span>
            <span class="analyzer-badge">● Analizador Automatizado Roche Cobas en línea</span>
          </div>

          <!-- GRID DE ESTUDIOS -->
          <div class="studies-selection-grid">
            <div
              v-for="estudio in estudiosFiltrados"
              :key="estudio.id"
              class="study-check-card"
              :class="{ selected: seleccionadosIds.includes(estudio.id) }"
              @click="toggleEstudio(estudio.id)"
            >
              <div class="check-box-wrapper">
                <span class="custom-checkbox" :class="{ checked: seleccionadosIds.includes(estudio.id) }">
                  <span v-if="seleccionadosIds.includes(estudio.id)">✓</span>
                </span>
              </div>
              <div class="study-info">
                <div class="study-title-row">
                  <strong>{{ estudio.nombre }}</strong>
                  <span class="time-tag" :class="{ 'stat-pill': estudio.stat }">
                    {{ estudio.tiempo }}
                  </span>
                </div>
                <p class="study-subtext">{{ estudio.subtexto }}</p>
              </div>
            </div>
          </div>

          <!-- JUSTIFICACIÓN E INSTRUCCIONES AL TÉCNICO -->
          <div class="justification-section">
            <div class="label-row">
              <label class="field-label">Justificación Clínica e Instrucciones al Personal Técnico</label>
              <span class="meta-label">Notas orientativas y alertas</span>
            </div>
            <textarea
              v-model="justificacionTecnica"
              rows="3"
              maxlength="500"
              class="textarea-clean"
              placeholder="Especifique la sospecha clínica relevante, condiciones especiales del paciente (ej. ayuno, alergia a medio de contraste, movilidad reducida)..."
            ></textarea>
            <div class="textarea-footer">
              <small>Máximo 500 caracteres para impresión de marbete en contenedor de muestra.</small>
              <span class="legal-badge">🛡️ Confidencialidad bajo NOM-024-SSA3-2012</span>
            </div>
          </div>
        </section>

      </div>

      <!-- COLUMNA DERECHA: RESUMEN Y ENVÍO -->
      <aside class="lab-right-column">
        
        <!-- RESUMEN DE ORDEN -->
        <div class="side-card">
          <div class="side-card-header">
            <div class="header-title-row">
              <span class="icon-summary">📋</span>
              <h4>Resumen de Orden</h4>
            </div>
            <span class="count-pill">{{ seleccionadosIds.length }} pruebas</span>
          </div>

          <div class="order-metric-strip">
            <span>📄 {{ seleccionadosIds.length }} estudios seleccionados en esta orden</span>
            <span class="order-time">-- min</span>
          </div>

          <!-- ESTADO VACÍO -->
          <div v-if="seleccionadosIds.length === 0" class="empty-studies-box">
            <div class="empty-flask-icon">⚗️</div>
            <strong>Sin estudios agregados</strong>
            <p>No hay estudios agregados a la solicitud. Seleccione las pruebas requeridas del catálogo para confeccionar la orden clínica.</p>
          </div>

          <!-- LISTA SI HAY ESTUDIOS SELECCIONADOS -->
          <div v-else class="selected-studies-list">
            <div
              v-for="id in seleccionadosIds"
              :key="id"
              class="selected-item-row"
            >
              <span>{{ catalogoEstudios.find(e => e.id === id)?.nombre }}</span>
              <button class="btn-remove" @click.stop="toggleEstudio(id)">✕</button>
            </div>
          </div>
        </div>

        <!-- MODALIDAD DE TOMA DE MUESTRA -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>MODALIDAD DE TOMA DE MUESTRA</h4>
          </div>
          <div class="modalities-grid">
            <label class="modality-option" :class="{ selected: modalidadToma === 'box' }">
              <input type="radio" v-model="modalidadToma" value="box" />
              <div class="modality-text">
                <strong>Flebotomía en Box</strong>
                <p>Técnico acude</p>
              </div>
            </label>

            <label class="modality-option" :class="{ selected: modalidadToma === 'gabinete' }">
              <input type="radio" v-model="modalidadToma" value="gabinete" />
              <div class="modality-text">
                <strong>Traslado a Gabinete</strong>
                <p>Camillería</p>
              </div>
            </label>
          </div>
        </div>

        <!-- FIRMA Y ENVÍO COMPACTO -->
        <div class="side-card">
          <div class="doctor-signature-badge-compact">
            <span>🔒 e.firma Activa · Cédula {{ doctor.cedula }}</span>
          </div>

          <button class="btn-send-order" @click="enviarSolicitud">
            ➤ Enviar Solicitud a Laboratorio / Imagen
          </button>

          <button class="btn-print-voucher" type="button">
            🖨️ Imprimir Volante Foleado para el Paciente
          </button>

          <div class="hl7-cert-row">
            <span>🔒 Sincronización HL7 / FHIR v4 certificada</span>
          </div>
        </div>

        <!-- TIEMPOS DE RESPUESTA DE EMERGENCIA -->
        <div class="emergency-sla-card">
          <div class="sla-title">
            <span>ⓘ</span>
            <strong>Tiempos de Respuesta de Emergencia</strong>
          </div>
          <p>Las muestras categorizadas como <strong>STAT / Emergencia</strong> tienen un SLA de 15 a 30 minutos desde la recepción de tubos con código de barras en el analizador de urgencias.</p>
        </div>

      </aside>

    </div>
  </div>
</template>

<style scoped>
.solicitar-lab-view {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  width: 100%;
}

/* SUBNAV */
.lab-subnav {
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

.title-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 0.2rem;
}

.title-row h2 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-format {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.subnav-right {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.tag-shortcut {
  font-size: 0.72rem;
  color: #0F4C81;
  font-weight: 600;
}

kbd {
  font-family: monospace;
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  padding: 0.15rem 0.35rem;
  border-radius: 4px;
}

.patient-connected-banner-clean {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
}

.patient-id-group {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.avatar-pill {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: #0F4C81;
  color: #FFFFFF;
  font-size: 0.7rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.patient-id-group strong {
  display: block;
  font-size: 0.8rem;
  color: #1E293B;
  line-height: 1.1;
}

.patient-id-group span {
  font-size: 0.68rem;
  color: #64748B;
}

.btn-change-patient-mini {
  background: none;
  border: none;
  color: #94A3B8;
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
}

.btn-change-patient-mini:hover {
  color: #DC2626;
}

.doctor-signature-badge-compact {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E40AF;
  text-align: center;
  margin-bottom: 0.75rem;
}

.btn-clear {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-clear:hover {
  background-color: #F1F5F9;
}

/* GRID PRINCIPAL */
.lab-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

.lab-left-column {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.lab-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem 1.25rem;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #F8FAFC;
  padding-bottom: 0.5rem;
}

.title-with-num {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.step-num {
  color: #2563EB;
  font-weight: 800;
  font-size: 1rem;
}

.title-with-num h3 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-required {
  font-size: 0.65rem;
  font-weight: 700;
  color: #475569;
  letter-spacing: 0.04em;
}

.card-body-flow {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #334155;
}

.meta-label {
  font-size: 0.68rem;
  color: #64748B;
}

.input-with-button {
  display: flex;
  gap: 0.5rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}

.input-icon {
  position: absolute;
  left: 0.65rem;
  font-size: 0.85rem;
  color: #94A3B8;
}

.input-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem 0.45rem 2rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
}

.btn-explore {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  color: #334155;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.help-text {
  font-size: 0.68rem;
  color: #64748B;
}

.two-cols-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* PRIORIDADES */
.priority-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.4rem;
}

.priority-card {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.5rem;
  background-color: #FFFFFF;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
}

.priority-title {
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E293B;
}

.priority-card small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.priority-card.active {
  background-color: #0F172A;
  border-color: #0F172A;
  color: #FFFFFF;
}

.priority-card.active .priority-title,
.priority-card.active small {
  color: #FFFFFF;
}

.priority-card.alert.active {
  background-color: #DC2626;
  border-color: #DC2626;
}

/* BUSCADOR DE CATÁLOGO */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.5rem;
  font-size: 0.75rem;
  color: #94A3B8;
}

.search-input {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.35rem 0.5rem 0.35rem 1.6rem;
  font-size: 0.75rem;
  outline: none;
}

/* TABS */
.catalog-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.tab-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn.active {
  background-color: #0F4C81;
  color: #FFFFFF;
  border-color: #0F4C81;
}

.tab-badge {
  background-color: rgba(255, 255, 255, 0.25);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  font-size: 0.68rem;
}

.tab-btn:not(.active) .tab-badge {
  background-color: #E2E8F0;
  color: #0F4C81;
}

.catalog-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.7rem;
  margin-bottom: 0.65rem;
}

.cat-subtitle {
  color: #64748B;
}

.analyzer-badge {
  color: #059669;
  font-weight: 600;
}

/* GRID DE ESTUDIOS (CHECK CARDS) */
.studies-selection-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.study-check-card {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem 0.75rem;
  background-color: #F8FAFC;
  cursor: pointer;
  transition: all 0.15s;
}

.study-check-card:hover {
  border-color: #93C5FD;
}

.study-check-card.selected {
  background-color: #EFF6FF;
  border-color: #2563EB;
}

.check-box-wrapper {
  margin-top: 0.1rem;
}

.custom-checkbox {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1.5px solid #94A3B8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  color: #FFFFFF;
  background-color: #FFFFFF;
}

.custom-checkbox.checked {
  background-color: #2563EB;
  border-color: #2563EB;
}

.study-info {
  flex: 1;
}

.study-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.15rem;
}

.study-title-row strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.time-tag {
  font-size: 0.65rem;
  color: #64748B;
  background-color: #E2E8F0;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
}

.time-tag.stat-pill {
  background-color: #FEE2E2;
  color: #DC2626;
  font-weight: 700;
}

.study-subtext {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0;
  line-height: 1.3;
}

/* JUSTIFICACIÓN */
.justification-section {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.textarea-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  resize: vertical;
  font-family: inherit;
}

.textarea-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.65rem;
  color: #94A3B8;
}

.legal-badge {
  color: #0F4C81;
  font-weight: 600;
}

/* COLUMNA DERECHA */
.lab-right-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

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

.header-title-row {
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

.count-pill {
  background-color: #EFF6FF;
  color: #2563EB;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 9999px;
}

.order-metric-strip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.72rem;
  color: #334155;
  margin-bottom: 0.8rem;
}

.order-time {
  font-weight: 700;
}

/* CAJA VACÍA */
.empty-studies-box {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 1.5rem 1rem;
  text-align: center;
}

.empty-flask-icon {
  font-size: 1.8rem;
  margin-bottom: 0.3rem;
  color: #64748B;
}

.empty-studies-box strong {
  display: block;
  font-size: 0.82rem;
  color: #334155;
  margin-bottom: 0.2rem;
}

.empty-studies-box p {
  font-size: 0.7rem;
  color: #64748B;
  margin: 0;
  line-height: 1.35;
}

.selected-studies-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.selected-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  font-size: 0.72rem;
  color: #1E293B;
}

.btn-remove {
  background: none;
  border: none;
  color: #DC2626;
  font-weight: 700;
  cursor: pointer;
}

/* MODALIDADES DE TOMA */
.modalities-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.modality-option {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  padding: 0.5rem;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  cursor: pointer;
  background-color: #F8FAFC;
}

.modality-option.selected {
  border-color: #2563EB;
  background-color: #EFF6FF;
}

.modality-text strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.modality-text p {
  font-size: 0.65rem;
  color: #64748B;
  margin: 0.1rem 0 0 0;
}

/* FIRMA MÉDICA */
.doctor-signature-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 0.6rem 0.75rem;
  margin-bottom: 0.8rem;
}

.seal-icon, .key-icon {
  font-size: 1.1rem;
}

.doc-name {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
}

.doc-ced {
  font-size: 0.65rem;
  color: #64748B;
  margin: 0;
}

.btn-send-order {
  width: 100%;
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  margin-bottom: 0.5rem;
  transition: background-color 0.15s;
}

.btn-send-order:hover {
  background-color: #1D4ED8;
}

.btn-print-voucher {
  width: 100%;
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #475569;
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-print-voucher:hover {
  background-color: #F1F5F9;
}

.hl7-cert-row {
  text-align: center;
  font-size: 0.65rem;
  color: #64748B;
  margin-top: 0.5rem;
}

.emergency-sla-card {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  padding: 0.85rem;
  font-size: 0.72rem;
  color: #1E3A8A;
  line-height: 1.4;
}

.sla-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
  font-weight: 700;
}

.emergency-sla-card p {
  margin: 0;
}

@media (max-width: 1100px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }
  .studies-selection-grid {
    grid-template-columns: 1fr;
  }
  .two-cols-row {
    grid-template-columns: 1fr;
  }
}
</style>
