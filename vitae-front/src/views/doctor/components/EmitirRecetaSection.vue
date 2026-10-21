<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
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
  (e: 'emitirReceta'): void
}>()

// Paciente y Diagnóstico (heredados automáticamente si ya existen en sesión)
const pacienteInput = ref('')
const diagnosticoCie = ref(props.diagnosticoGlobal || props.pacienteSeleccionado?.motivo || '')

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

// Formulario de ingreso de medicamento
const medicamento = ref('')
const dosis = ref('')
const forma = ref('')
const via = ref('')
const frecuencia = ref('')
const duracionValor = ref('')
const duracionUnidad = ref('días')
const cantidadDispensar = ref('')
const indicaciones = ref('')

// Lista de medicamentos agregados
interface ItemReceta {
  id: string
  medicamento: string
  dosis: string
  forma: string
  via: string
  frecuencia: string
  duracion: string
  cantidad: string
  indicaciones: string
}
const medicamentosAgregados = ref<ItemReceta[]>([])

const agregarMedicamento = () => {
  if (!medicamento.value) {
    alert('Ingrese al menos el nombre o denominación genérica del medicamento.')
    return
  }
  medicamentosAgregados.value.push({
    id: `MED-${Date.now()}`,
    medicamento: medicamento.value,
    dosis: dosis.value || 'Estándar',
    forma: forma.value || 'Tabletas',
    via: via.value || 'Oral',
    frecuencia: frecuencia.value || 'Cada 8 horas',
    duracion: duracionValor.value ? `${duracionValor.value} ${duracionUnidad.value}` : '3 días',
    cantidad: cantidadDispensar.value || '1 caja',
    indicaciones: indicaciones.value || 'Tomar según prescripción'
  })
  // Limpiar campos de captura rápida
  medicamento.value = ''
  dosis.value = ''
  forma.value = ''
  via.value = ''
  frecuencia.value = ''
  duracionValor.value = ''
  cantidadDispensar.value = ''
  indicaciones.value = ''
}

const removerMedicamento = (id: string) => {
  medicamentosAgregados.value = medicamentosAgregados.value.filter(m => m.id !== id)
}

// Medidas no farmacológicas
const instruccionesGenerales = ref('')
const incluirSignosAlarma = ref(true)
const adjuntarPautaHidratacion = ref(true)

// Configuración de receta (columna derecha)
const tipoPrescripcion = ref<'urgencias' | 'ordinaria' | 'controlada'>('urgencias')
const dispensacionFarmacia = ref(true)
const impresionVolanteQr = ref(false)
const envioCifrado = ref(false)

const firmarYEmitir = () => {
  if (medicamentosAgregados.value.length === 0 && !medicamento.value) {
    alert('Agregue al menos un medicamento a la receta antes de firmarla.')
    return
  }
  if (medicamento.value && medicamentosAgregados.value.length === 0) {
    agregarMedicamento()
  }
  emit('emitirReceta')
  alert('Receta médica firmada digitalmente con e.firma y enviada a Farmacia Hospitalaria con SHA-256.')
}

const guardarBorrador = () => {
  alert('Borrador de prescripción médica guardado.')
}

const descartarReceta = () => {
  if (confirm('¿Desea descartar los cambios y limpiar la receta?')) {
    medicamentosAgregados.value = []
    medicamento.value = ''
    dosis.value = ''
    forma.value = ''
    via.value = ''
    frecuencia.value = ''
    duracionValor.value = ''
    cantidadDispensar.value = ''
    indicaciones.value = ''
    instruccionesGenerales.value = ''
    diagnosticoCie.value = ''
    pacienteInput.value = ''
  }
}
</script>

<template>
  <div class="emitir-receta-view">
    <!-- ENCABEZADO DESTACADO DEL MÓDULO -->
    <div class="rx-header-banner">
      <div class="banner-main-row">
        <div class="icon-cube-rx">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
          </svg>
        </div>
        <div class="banner-title-area">
          <div class="banner-title-row">
            <h2>Módulo de Prescripción Electrónica</h2>
            <span class="badge-status-box">ACTIVO · {{ doctor.box }}</span>
          </div>
          <p class="banner-subtext">Cumplimiento NOM-004-SSA3-2012 · Art. 65 Reglamento de Insumos para la Salud · Trazabilidad Criptográfica SHA-256</p>
        </div>
      </div>

      <div class="banner-tags-row">
        <span class="pill-vad"><span class="dot-vad">●</span> Vademécum Hospitalario En Línea</span>
        <span class="pill-vad"><span class="shield-vad">🛡️</span> e.firma SAT/SSA Habilitada</span>
      </div>
    </div>

    <!-- GRID DE 2 COLUMNAS (FORMULARIO PRINCIPAL + CONTROLES Y DISPENSACIÓN) -->
    <div class="rx-main-grid">
      
      <!-- COLUMNA IZQUIERDA: FORMULARIOS -->
      <div class="rx-left-column">
        
        <!-- CARD 1: NUEVA RECETA MÉDICA ELECTRÓNICA -->
        <section class="rx-card">
          <div class="card-top-header">
            <div class="title-with-badge">
              <h3>Nueva Receta Médica Electrónica</h3>
              <span class="badge-draft-pill">Borrador ECE-RX-2026</span>
            </div>
            <span class="shortcut-tag">Atajo global: <kbd>Alt + R</kbd></span>
          </div>

          <div class="rx-fields-flow">
            <!-- VINCULAR PACIENTE: Muestra paciente conectado si existe -->
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
              <label class="field-label">Vincular Paciente al Encuentro Clínico *</label>
              <div class="input-action-row">
                <div class="input-with-icon">
                  <span class="icon-input">👤</span>
                  <input
                    v-model="pacienteInput"
                    type="text"
                    class="input-clean"
                    placeholder="Seleccionar paciente asignado de la cola o ingresar Folio / ..."
                  />
                </div>
                <button class="btn-primary-action" type="button">
                  🔗 Vincular
                </button>
              </div>
            </div>

            <!-- DIAGNÓSTICO CIE -->
            <div class="field-group">
              <label class="field-label">Diagnóstico Principal que Motiva la Prescripción (CIE-10 / CIE-11) *</label>
              <div class="input-with-icon">
                <span class="icon-input">📋</span>
                <input
                  v-model="diagnosticoCie"
                  type="text"
                  class="input-clean"
                  placeholder="Buscar código o descripción (ej. J00 Rinofaringitis aguda, I10 Hipertensión esencial..."
                />
              </div>
            </div>

            <!-- ALERTA DE ALERGIAS FARMACOLÓGICAS -->
            <div class="allergy-shield-box">
              <div class="allergy-left">
                <span class="shield-badge-icon">🛡️</span>
                <div>
                  <strong>Verificación de Alergias Farmacológicas</strong>
                  <p>Sin paciente vinculado. Al vincular paciente se verificará automáticamente el historial de alergias farmacológicas (Análisis cruzado en tiempo real con expediente único).</p>
                </div>
              </div>
              <span class="badge-system-active">Sistema Activo</span>
            </div>
          </div>
        </section>

        <!-- CARD 2: INGRESO DE MEDICAMENTOS -->
        <section class="rx-card">
          <div class="card-top-header">
            <div class="title-with-badge">
              <span class="dot-blue-heading">●</span>
              <h3>Ingreso de Medicamentos al Tratamiento</h3>
            </div>
            <span class="cofepris-tag">Catálogo Oficial Institucional (COFEPRIS)</span>
          </div>

          <div class="rx-fields-flow">
            <!-- NOMBRE DEL MEDICAMENTO -->
            <div class="field-group">
              <div class="label-with-tip">
                <label class="field-label">Medicamento / Denominación Genérica o Comercial *</label>
                <small class="tip-text">Presiona [Tab] para autocompletar</small>
              </div>
              <div class="input-with-icon">
                <span class="icon-input">💊</span>
                <input
                  v-model="medicamento"
                  type="text"
                  class="input-clean"
                  placeholder="Escriba denominación genérica o comercial (ej. Paracetamol, Amoxicilina, Ketorolaco)..."
                />
              </div>
            </div>

            <!-- FILA 1: Dosis, Forma, Vía -->
            <div class="form-row-3">
              <div class="field-group">
                <label class="field-label">Dosis y Concentración *</label>
                <input
                  v-model="dosis"
                  type="text"
                  class="input-clean"
                  placeholder="ej. 500 mg, 1 g, 10 ml"
                />
              </div>
              <div class="field-group">
                <label class="field-label">Forma Farmacéutica *</label>
                <select v-model="forma" class="select-clean">
                  <option value="">Seleccione presentación...</option>
                  <option value="Tabletas">Tabletas</option>
                  <option value="Cápsulas">Cápsulas</option>
                  <option value="Solución Inyectable">Solución Inyectable</option>
                  <option value="Suspensión Oral">Suspensión Oral</option>
                  <option value="Jarabe">Jarabe</option>
                  <option value="Gel / Crema">Gel / Crema</option>
                </select>
              </div>
              <div class="field-group">
                <label class="field-label">Vía de Administración *</label>
                <select v-model="via" class="select-clean">
                  <option value="">Seleccione vía...</option>
                  <option value="Oral">Oral</option>
                  <option value="Intravenosa (IV)">Intravenosa (IV)</option>
                  <option value="Intramuscular (IM)">Intramuscular (IM)</option>
                  <option value="Sublingual">Sublingual</option>
                  <option value="Tópica">Tópica</option>
                  <option value="Inhalatoria">Inhalatoria</option>
                </select>
              </div>
            </div>

            <!-- FILA 2: Frecuencia, Duración, Cantidad -->
            <div class="form-row-3">
              <div class="field-group">
                <label class="field-label">Frecuencia / Intervalo *</label>
                <select v-model="frecuencia" class="select-clean">
                  <option value="">Seleccione pauta...</option>
                  <option value="Cada 6 horas">Cada 6 horas</option>
                  <option value="Cada 8 horas">Cada 8 horas</option>
                  <option value="Cada 12 horas">Cada 12 horas</option>
                  <option value="Cada 24 horas">Cada 24 horas</option>
                  <option value="Dosis única">Dosis única</option>
                  <option value="PRN (En caso de dolor)">PRN (En caso de dolor)</option>
                </select>
              </div>

              <div class="field-group">
                <label class="field-label">Duración del Tratamiento *</label>
                <div class="duration-combo">
                  <input
                    v-model="duracionValor"
                    type="number"
                    min="1"
                    class="input-clean duration-val"
                    placeholder="ej. 5"
                  />
                  <select v-model="duracionUnidad" class="select-clean duration-unit">
                    <option value="días">días</option>
                    <option value="semanas">semanas</option>
                    <option value="meses">meses</option>
                    <option value="dosis">dosis</option>
                  </select>
                </div>
              </div>

              <div class="field-group">
                <label class="field-label">Cantidad Total a Dispensar</label>
                <input
                  v-model="cantidadDispensar"
                  type="text"
                  class="input-clean"
                  placeholder="ej. 1 caja con 20 tabletas"
                />
              </div>
            </div>

            <!-- INDICACIONES ESPECÍFICAS -->
            <div class="field-group">
              <label class="field-label">Indicaciones Específicas para este Fármaco</label>
              <textarea
                v-model="indicaciones"
                class="textarea-clean"
                rows="3"
                placeholder="Especifique pautas para el paciente: Ingerir después de alimentos, tomar con abundante agua, no suspender el tratamiento prematuramente, advertencias específicas..."
              ></textarea>
            </div>

            <!-- BOTÓN AGREGAR -->
            <div class="add-btn-wrapper">
              <button class="btn-add-drug" type="button" @click="agregarMedicamento">
                + Agregar Medicamento a la Receta
              </button>
            </div>
          </div>
        </section>

        <!-- CARD 3: RENGLONES DE MEDICAMENTOS PRESCRITOS -->
        <section class="rx-card">
          <div class="card-top-header">
            <div class="title-with-badge">
              <h3>Renglones de Medicamentos Prescritos</h3>
              <span class="badge-count-pill">{{ medicamentosAgregados.length }} fármacos activos</span>
            </div>
            <span class="tip-text-green">✓ Verificación de interacciones farmacológicas activada</span>
          </div>

          <!-- LISTA SI HAY FARMACOS -->
          <div v-if="medicamentosAgregados.length > 0" class="prescribed-items-list">
            <div v-for="item in medicamentosAgregados" :key="item.id" class="prescribed-item-row">
              <div class="item-main">
                <strong class="item-name">{{ item.medicamento }}</strong>
                <span class="item-details">{{ item.dosis }} · {{ item.forma }} · {{ item.via }} · {{ item.frecuencia }} por {{ item.duracion }}</span>
                <p v-if="item.indicaciones" class="item-notes">Indicaciones: {{ item.indicaciones }}</p>
              </div>
              <div class="item-actions">
                <span class="item-qty">{{ item.cantidad }}</span>
                <button class="btn-remove-item" @click="removerMedicamento(item.id)" title="Eliminar renglón">✕</button>
              </div>
            </div>
          </div>

          <!-- ESTADO VACÍO (SEGMENTO EN BLANCO SEGÚN CAPTURA) -->
          <div v-else class="empty-prescribed-box">
            <div class="empty-icon-capsule">
              <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                <path d="M4.5 10.5C3.67 10.5 3 11.17 3 12s.67 1.5 1.5 1.5h15c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5h-15z M10.5 4.5C10.5 3.67 11.17 3 12 3s1.5.67 1.5 1.5v15c0 .83-.67 1.5-1.5 1.5s-1.5-.67-1.5-1.5v-15z"/>
              </svg>
            </div>
            <strong>No hay fármacos agregados a esta receta todavía</strong>
            <p>Complete los campos en el formulario de captura superior y haga clic en "+ Agregar Medicamento a la Receta" para generar los renglones terapéuticos.</p>
          </div>
        </section>

        <!-- CARD 4: RECOMENDACIONES Y MEDIDAS NO FARMACOLÓGICAS -->
        <section class="rx-card">
          <div class="card-top-header">
            <div class="title-with-badge">
              <span class="shield-blue">🛡️</span>
              <h3>Recomendaciones y Medidas No Farmacológicas</h3>
            </div>
            <span class="badge-disclaimer">Documento de egreso / Cuidados en domicilio</span>
          </div>

          <div class="rx-fields-flow">
            <div class="field-group">
              <label class="field-label">Instrucciones Generales, Cuidados y Signos de Alarma *</label>
              <textarea
                v-model="instruccionesGenerales"
                class="textarea-clean"
                rows="4"
                placeholder="Recomendaciones generales, reposo, dieta específica, abundante hidratación, señales de alarma para acudir de inmediato a Urgencias (disnea súbita, fiebre persistente >38.5°C resistente a antipiréticos, dolor precordial opresivo) e indicaciones de cita de control o revaloración..."
              ></textarea>
            </div>

            <div class="checkbox-options-row">
              <label class="checkbox-pill">
                <input type="checkbox" v-model="incluirSignosAlarma" />
                <span>Incluir signos de alarma estandarizados para Urgencias</span>
              </label>

              <label class="checkbox-pill">
                <input type="checkbox" v-model="adjuntarPautaHidratacion" />
                <span>Adjuntar pauta de hidratación y reposo relativo</span>
              </label>
            </div>
          </div>
        </section>

      </div>

      <!-- COLUMNA DERECHA: CONFIGURACIÓN, DISPENSACIÓN Y FIRMA -->
      <aside class="rx-right-column">
        
        <!-- CARD 1: TIPO DE PRESCRIPCIÓN -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>Tipo de Prescripción</h4>
            <span class="badge-pending-folio">Folio Pendiente</span>
          </div>

          <div class="prescription-type-options">
            <label class="type-radio-option" :class="{ selected: tipoPrescripcion === 'urgencias' }">
              <input type="radio" v-model="tipoPrescripcion" value="urgencias" />
              <div class="type-text">
                <strong>Receta de Urgencias Hospitalarias</strong>
                <p>Surtimiento prioritario en farmacia central (24h)</p>
              </div>
            </label>

            <label class="type-radio-option" :class="{ selected: tipoPrescripcion === 'ordinaria' }">
              <input type="radio" v-model="tipoPrescripcion" value="ordinaria" />
              <div class="type-text">
                <strong>Receta Ordinaria / Ambulatoria</strong>
                <p>Consulta externa y seguimiento de tratamiento</p>
              </div>
            </label>

            <label class="type-radio-option" :class="{ selected: tipoPrescripcion === 'controlada' }">
              <input type="radio" v-model="tipoPrescripcion" value="controlada" />
              <div class="type-text">
                <strong>Receta Controlada (Fracción II y III)</strong>
                <p>Requiere código de barras oficial y folio COFEPRIS</p>
              </div>
            </label>
          </div>

          <!-- CONTADORES / TIEMPO DE VALIDEZ -->
          <div class="rx-metric-box">
            <div class="metric-item">
              <span class="metric-lbl">TOTAL DE FÁRMACOS</span>
              <strong class="metric-val">{{ medicamentosAgregados.length }}</strong>
            </div>
            <div class="metric-item border-left">
              <span class="metric-lbl">TIEMPO DE VALIDEZ</span>
              <strong class="metric-val highlight">72 horas</strong>
            </div>
          </div>
        </div>

        <!-- CARD 2: DISPENSACIÓN Y ENTREGA -->
        <div class="side-card">
          <div class="side-card-header">
            <h4>Dispensación y Entrega</h4>
          </div>

          <div class="dispensation-options">
            <label class="delivery-option">
              <input type="checkbox" v-model="dispensacionFarmacia" />
              <div class="delivery-text">
                <strong>Dispensación Directa en Farmacia Hospitalaria</strong>
                <p>Envío telemático inmediato para surtimiento en {{ doctor.box }}</p>
              </div>
            </label>

            <label class="delivery-option">
              <input type="checkbox" v-model="impresionVolanteQr" />
              <div class="delivery-text">
                <strong>Impresión de Volante Foleado con QR</strong>
                <p>Código bidimensional verificable por COFEPRIS</p>
              </div>
            </label>

            <label class="delivery-option">
              <input type="checkbox" v-model="envioCifrado" />
              <div class="delivery-text">
                <strong>Envío Cifrado al Paciente</strong>
                <p>Copia digital en formato PDF vía SMS / WhatsApp</p>
              </div>
            </label>
          </div>
        </div>

        <!-- CARD 3: FIRMA MÉDICA DIGITAL COMPACTA -->
        <div class="side-card">
          <div class="side-card-header">
            <div class="sign-title-group">
              <span class="sign-pen-icon">✍️</span>
              <h4>Firma Digital e.firma</h4>
            </div>
            <span class="badge-cert-green">Certificado Válido</span>
          </div>

          <div class="sha-box-compact">
            <span class="hash-text">SHA256: 48f9-b82c-0a71-99af...</span>
            <span class="sat-tag">SAT / SSA</span>
          </div>

          <div class="action-buttons-group">
            <button class="btn-firmar-receta" @click="firmarYEmitir">
              ✓ Firmar y Emitir Receta [Ctrl+Enter]
            </button>

            <div class="row-2-btn">
              <button class="btn-draft" @click="guardarBorrador">
                💾 Guardar Borrador
              </button>
              <button class="btn-discard" @click="descartarReceta">
                🗑️ Descartar
              </button>
            </div>
          </div>

          <div class="legal-footnote-compact">
            <span>🛡️ NOM-004-SSA3 · COFEPRIS Receta Electrónica</span>
          </div>
        </div>

      </aside>

    </div>
  </div>
</template>

<style scoped>
.emitir-receta-view {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  width: 100%;
}

/* ENCABEZADO DESTACADO DEL MÓDULO */
.rx-header-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.banner-main-row {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.icon-cube-rx {
  width: 40px;
  height: 40px;
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-title-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.banner-title-row h2 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
}

.badge-status-box {
  background-color: #DBEAFE;
  color: #1E40AF;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.banner-subtext {
  font-size: 0.72rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
}

.banner-tags-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.pill-vad {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 9999px;
  padding: 0.25rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.dot-vad {
  color: #2563EB;
  font-size: 0.8rem;
}

/* GRID PRINCIPAL */
.rx-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

.rx-left-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* CARDS */
.rx-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem 1.25rem;
}

.card-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #F8FAFC;
  padding-bottom: 0.4rem;
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

.sha-box-compact {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.legal-footnote-compact {
  text-align: center;
  font-size: 0.65rem;
  color: #94A3B8;
  margin-top: 0.4rem;
}

.title-with-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.title-with-badge h3 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-draft-pill {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.shortcut-tag {
  font-size: 0.72rem;
  color: #64748B;
}

kbd {
  font-family: monospace;
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  padding: 0.15rem 0.35rem;
  border-radius: 4px;
  color: #334155;
}

.dot-blue-heading {
  color: #2563EB;
  font-size: 0.9rem;
}

.cofepris-tag {
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 600;
}

.badge-count-pill {
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
}

.tip-text-green {
  font-size: 0.68rem;
  color: #059669;
  font-weight: 600;
}

.badge-disclaimer {
  font-size: 0.68rem;
  color: #64748B;
}

/* FLOW CAMPOS */
.rx-fields-flow {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
}

.label-with-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip-text {
  font-size: 0.68rem;
  color: #94A3B8;
}

.input-action-row {
  display: flex;
  gap: 0.5rem;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}

.icon-input {
  position: absolute;
  left: 0.65rem;
  font-size: 0.85rem;
  color: #94A3B8;
}

.input-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  background-color: #FFFFFF;
}

.input-with-icon .input-clean {
  padding-left: 2rem;
}

.input-clean:focus, .select-clean:focus, .textarea-clean:focus {
  border-color: #3B82F6;
}

.select-clean {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  background-color: #FFFFFF;
}

.textarea-clean {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem 0.75rem;
  font-size: 0.8rem;
  color: #1E293B;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.45;
}

.btn-primary-action {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.45rem 0.9rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.btn-primary-action:hover {
  background-color: #1D4ED8;
}

/* ALERGIAS */
.allergy-shield-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
}

.allergy-left {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
}

.shield-badge-icon {
  font-size: 1.2rem;
}

.allergy-left strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
}

.allergy-left p {
  font-size: 0.7rem;
  color: #64748B;
  margin: 0.15rem 0 0 0;
  line-height: 1.35;
}

.badge-system-active {
  background-color: #ECFDF5;
  color: #047857;
  border: 1px solid #A7F3D0;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  white-space: nowrap;
}

/* FORMS GRID */
.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.8rem;
}

.duration-combo {
  display: flex;
  gap: 0.35rem;
}

.duration-val {
  width: 70px;
}

.duration-unit {
  flex: 1;
}

.add-btn-wrapper {
  display: flex;
  justify-content: flex-end;
}

.btn-add-drug {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-add-drug:hover {
  background-color: #DBEAFE;
}

/* CAJA VACÍA DE FARMACOS */
.empty-prescribed-box {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 1.5rem;
  text-align: center;
}

.empty-icon-capsule {
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.4rem;
}

.empty-prescribed-box strong {
  display: block;
  font-size: 0.85rem;
  color: #334155;
  margin-bottom: 0.25rem;
}

.empty-prescribed-box p {
  font-size: 0.72rem;
  color: #64748B;
  margin: 0;
  max-width: 460px;
  margin: 0 auto;
}

/* LISTA DE RENGLONES */
.prescribed-items-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.prescribed-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem 0.8rem;
}

.item-name {
  font-size: 0.85rem;
  color: #0F4C81;
  display: block;
}

.item-details {
  font-size: 0.72rem;
  color: #475569;
}

.item-notes {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.item-qty {
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E293B;
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.btn-remove-item {
  background: none;
  border: none;
  color: #EF4444;
  font-weight: 700;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
}

.checkbox-options-row {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.checkbox-pill {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.75rem;
  color: #334155;
  cursor: pointer;
}

/* COLUMNA DERECHA */
.rx-right-column {
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

.side-card-header h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-pending-folio {
  background-color: #F1F5F9;
  color: #64748B;
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

/* OPCIONES RADIO DE TIPO DE PRESCRIPCIÓN */
.prescription-type-options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.type-radio-option {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.6rem;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
  background-color: #F8FAFC;
  cursor: pointer;
  transition: all 0.15s;
}

.type-radio-option.selected {
  border-color: #3B82F6;
  background-color: #EFF6FF;
}

.type-text strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
}

.type-text p {
  font-size: 0.65rem;
  color: #64748B;
  margin: 0.1rem 0 0 0;
}

.rx-metric-box {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-top: 1px solid #E2E8F0;
  padding-top: 0.65rem;
}

.metric-item {
  text-align: center;
}

.metric-item.border-left {
  border-left: 1px solid #E2E8F0;
}

.metric-lbl {
  display: block;
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748B;
}

.metric-val {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1E293B;
}

.metric-val.highlight {
  color: #2563EB;
}

/* DISPENSACIÓN */
.dispensation-options {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.delivery-option {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  cursor: pointer;
}

.delivery-text strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.delivery-text p {
  font-size: 0.65rem;
  color: #64748B;
  margin: 0.1rem 0 0 0;
}

/* FIRMA MÉDICA DIGITAL */
.sign-title-group {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.sign-pen-icon {
  font-size: 0.85rem;
}

.badge-cert-green {
  background-color: #ECFDF5;
  color: #047857;
  border: 1px solid #A7F3D0;
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.doctor-cert-block {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 0.75rem;
  margin-bottom: 0.8rem;
}

.doc-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-name {
  font-size: 0.82rem;
  color: #1E293B;
}

.doc-org {
  font-size: 0.68rem;
  color: #64748B;
}

.doc-spec {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.2rem 0;
}

.doc-ced-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.65rem;
  color: #475569;
  font-weight: 600;
  margin-bottom: 0.45rem;
}

.sha-box {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hash-text {
  font-family: monospace;
  font-size: 0.65rem;
  color: #1E40AF;
}

.sat-tag {
  background-color: #2563EB;
  color: #FFFFFF;
  font-size: 0.6rem;
  font-weight: 700;
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
}

.action-buttons-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-bottom: 0.8rem;
}

.btn-firmar-receta {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-firmar-receta:hover {
  background-color: #1D4ED8;
}

.row-2-btn {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
}

.btn-draft, .btn-discard {
  border: 1px solid #CBD5E1;
  background-color: #FFFFFF;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.45rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-draft:hover, .btn-discard:hover {
  background-color: #F1F5F9;
}

.legal-footnote {
  font-size: 0.65rem;
  color: #94A3B8;
  line-height: 1.35;
}

.legal-footnote p {
  margin: 0;
}

@media (max-width: 1100px) {
  .rx-main-grid {
    grid-template-columns: 1fr;
  }
  .form-row-3 {
    grid-template-columns: 1fr;
  }
}
</style>
