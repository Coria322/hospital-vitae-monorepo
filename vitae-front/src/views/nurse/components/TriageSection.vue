<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{
  pacienteActivo?: {
    nombre: string
    edad: number | null
    sexo: string
    folio?: string
    signos?: any
    motivoConsulta?: string
    alergias?: string
    enfermedadesPrevias?: string
  }
}>()

const emit = defineEmits<{
  (e: 'activarChoque'): void
  (e: 'guardarTriage', datos: any): void
}>()

// --- FORMULARIO DE TRIAGE ---
const triageForm = ref({
  nombrePaciente: '',
  edad: null as number | null,
  sexo: '',
  paSistolica: '',
  paDiastolica: '',
  fc: '',
  fr: '',
  spo2: '',
  temp: '',
  glucosa: '',
  avdi: 'A' as 'A' | 'V' | 'D' | 'I',
  evaDolor: 0,
  motivoConsulta: '',
  alergias: '',
  enfermedadesPrevias: ''
})

const destinoSeleccionado = ref('Box de Urgencias Generales')

// Inicializar si ya viene paciente activo
onMounted(() => {
  if (props.pacienteActivo?.nombre) {
    triageForm.value.nombrePaciente = props.pacienteActivo.nombre
    triageForm.value.edad = props.pacienteActivo.edad
    triageForm.value.sexo = props.pacienteActivo.sexo || ''
    if (props.pacienteActivo.signos) {
      Object.assign(triageForm.value, props.pacienteActivo.signos)
    }
    if (props.pacienteActivo.motivoConsulta) {
      triageForm.value.motivoConsulta = props.pacienteActivo.motivoConsulta
    }
    if (props.pacienteActivo.alergias) {
      triageForm.value.alergias = props.pacienteActivo.alergias
    }
  }
})

// Registro de clasificaciones en guardia
const pacientesClasificados = ref<Array<{
  hora: string
  paciente: string
  signos: string
  clasificacion: string
  colorClass: string
  destino: string
}>>([
  {
    hora: '14:10',
    paciente: 'Elena Torres Morales (54a)',
    signos: 'PA 135/85 · FC 88 · SpO2 96%',
    clasificacion: 'Nivel III · Amarillo',
    colorClass: 'yellow',
    destino: 'Sala de Espera A'
  },
  {
    hora: '13:45',
    paciente: 'Carlos Mendoza Vega (68a)',
    signos: 'PA 150/95 · FC 104 · SpO2 93%',
    clasificacion: 'Nivel II · Naranja',
    colorClass: 'orange',
    destino: 'Box 02 Urgencias'
  }
])

const limpiarSignos = () => {
  triageForm.value.paSistolica = ''
  triageForm.value.paDiastolica = ''
  triageForm.value.fc = ''
  triageForm.value.fr = ''
  triageForm.value.spo2 = ''
  triageForm.value.temp = ''
  triageForm.value.glucosa = ''
  triageForm.value.avdi = 'A'
  triageForm.value.evaDolor = 0
}

const guardarTriage = () => {
  if (!triageForm.value.nombrePaciente) {
    alert('Ingrese el nombre del paciente o asigne un folio de desconocido.')
    return
  }

  const now = new Date()
  const hora = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const nivel = triageForm.value.avdi === 'I' 
    ? 'Nivel I · Reanimación' 
    : (triageForm.value.evaDolor >= 8 ? 'Nivel II · Muy Urgente' : 'Nivel III · Urgente')
  const colorClass = triageForm.value.avdi === 'I' ? 'red' : (triageForm.value.evaDolor >= 8 ? 'orange' : 'yellow')
  const colorHex = triageForm.value.avdi === 'I' ? '#EF4444' : (triageForm.value.evaDolor >= 8 ? '#EA580C' : '#D97706')

  // Agregar a la tabla de pacientes clasificados
  pacientesClasificados.value.unshift({
    hora,
    paciente: `${triageForm.value.nombrePaciente} ${triageForm.value.edad ? `(${triageForm.value.edad}a)` : ''}`,
    signos: `PA ${triageForm.value.paSistolica || '120'}/${triageForm.value.paDiastolica || '80'} · FC ${triageForm.value.fc || '80'} · SpO2 ${triageForm.value.spo2 || '98'}%`,
    clasificacion: nivel,
    colorClass,
    destino: destinoSeleccionado.value
  })

  // Emitir evento con datos persistentes
  emit('guardarTriage', {
    nombre: triageForm.value.nombrePaciente,
    edad: triageForm.value.edad,
    sexo: triageForm.value.sexo,
    folio: `FOL-TRI-${Math.floor(1000 + Math.random() * 9000)}`,
    box: destinoSeleccionado.value,
    triage: nivel,
    triageColor: colorHex,
    signos: {
      paSistolica: triageForm.value.paSistolica,
      paDiastolica: triageForm.value.paDiastolica,
      fc: triageForm.value.fc,
      fr: triageForm.value.fr,
      spo2: triageForm.value.spo2,
      temp: triageForm.value.temp,
      glucosa: triageForm.value.glucosa,
      avdi: triageForm.value.avdi,
      evaDolor: triageForm.value.evaDolor
    },
    motivoConsulta: triageForm.value.motivoConsulta,
    alergias: triageForm.value.alergias,
    enfermedadesPrevias: triageForm.value.enfermedadesPrevias
  })

  alert(`✓ Triage registrado con éxito para: ${triageForm.value.nombrePaciente}.\nAsignado a: ${destinoSeleccionado.value} con prioridad ${nivel}.`)
}

const derivarAChoque = () => {
  emit('activarChoque')
}
</script>

<template>
  <div class="triage-layout-grid">
    <!-- SUBCOLUMNA CENTRAL: FORMULARIO EN BLANCO -->
    <div class="triage-form-col">
      <!-- Barra Superior Informativa -->
      <div class="triage-status-banner">
        <div class="banner-left-info">
          <span class="module-tag">MÓDULO ACTIVO</span>
          <h3 class="module-title">Triage y Clasificación Inicial</h3>
          
          <div class="patient-turn-row">
            <span class="icon-user">👤</span>
            <div class="patient-turn-text">
              <small>PACIENTE EN TURNO</small>
              <strong v-if="triageForm.nombrePaciente">{{ triageForm.nombrePaciente }}</strong>
              <strong v-else class="text-waiting">[ Esperando asignación / Captura de nuevo ingreso ]</strong>
            </div>
          </div>

          <div class="status-chip-blue">
            ⏳ Esperando valoración inicial en Box
          </div>
        </div>

        <div class="banner-right-info">
          <div class="prescription-locked-badge">
            🔒 Prescripción médica bloqueada (NOM-004 Enfermería)
          </div>
          <div class="room-status-badge">
            <span class="status-dot green"></span>
            SALA DE CHOQUE: <strong>DISPONIBLE (BOX 01 LISTO)</strong>
          </div>
        </div>
      </div>

      <!-- 1. Identificación Inicial de Ingreso -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon">🪪</span>
            <h4>1. Identificación Inicial de Ingreso</h4>
          </div>
          <span class="box-reg-code">ENF - REG - 2026 - VITE</span>
        </div>

        <div class="patient-id-grid">
          <div class="input-field-group">
            <label>Nombre Completo del Paciente</label>
            <input 
              v-model="triageForm.nombrePaciente" 
              type="text" 
              class="input-clinical" 
              placeholder="Apellidos y Nombre..."
            />
          </div>

          <div class="input-field-group">
            <label>Edad / Grupo Etario</label>
            <div class="age-input-row">
              <input 
                v-model="triageForm.edad" 
                type="number" 
                class="input-clinical" 
                placeholder="ej. 42"
              />
              <span class="unit-box">Años</span>
            </div>
          </div>

          <div class="input-field-group">
            <label>Sexo Biológico</label>
            <select v-model="triageForm.sexo" class="input-clinical">
              <option value="">Seleccione género...</option>
              <option value="Masculino">Masculino</option>
              <option value="Femenino">Femenino</option>
              <option value="No Especificado">No especificado (Urgencia)</option>
            </select>
          </div>
        </div>
      </section>

      <!-- 2. Telemetría de Signos Vitales -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon">📈</span>
            <h4>2. Telemetría de Signos Vitales</h4>
            <small class="box-subtitle">Navegación secuencial por teclado con tecla TAB. Cálculo reactivo.</small>
          </div>
          <button type="button" class="btn-clean" @click="limpiarSignos">↺ Limpiar</button>
        </div>

        <div class="telemetry-grid">
          <!-- Presión Arterial -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">PRESIÓN ARTERIAL</span>
              <span class="tag-unit">mmHg</span>
            </div>
            <div class="pa-inputs-row">
              <input v-model="triageForm.paSistolica" type="text" placeholder="ej. 120" class="input-telemetry" />
              <span class="pa-slash">/</span>
              <input v-model="triageForm.paDiastolica" type="text" placeholder="ej. 80" class="input-telemetry" />
            </div>
            <div class="telemetry-footer-labels">
              <span>Sistólica</span>
              <span>Diastólica</span>
            </div>
          </div>

          <!-- Frecuencia Cardíaca -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">FRECUENCIA CARDÍACA</span>
              <span class="tag-unit">lpm</span>
            </div>
            <input v-model="triageForm.fc" type="text" placeholder="ej. 75" class="input-telemetry full" />
            <span class="telemetry-hint">Normocardia: 60-100 lpm</span>
          </div>

          <!-- Frecuencia Respiratoria -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">FREC. RESPIRATORIA</span>
              <span class="tag-unit">rpm</span>
            </div>
            <input v-model="triageForm.fr" type="text" placeholder="ej. 16" class="input-telemetry full" />
            <span class="telemetry-hint">Eupnea: 12-20 rpm</span>
          </div>

          <!-- Saturación SpO2 -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">SATURACIÓN SPO₂</span>
              <span class="tag-unit">% O₂</span>
            </div>
            <input v-model="triageForm.spo2" type="text" placeholder="ej. 98" class="input-telemetry full" />
            <span class="telemetry-hint">Rango Seguro: 95-100%</span>
          </div>

          <!-- Temperatura -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">TEMP. AXILAR/ÓTICA</span>
              <span class="tag-unit">°C</span>
            </div>
            <input v-model="triageForm.temp" type="text" placeholder="ej. 36.5" class="input-telemetry full" />
            <span class="telemetry-hint">Normotermia: 36.0 - 37.4°C</span>
          </div>

          <!-- Glucosa -->
          <div class="telemetry-card">
            <div class="card-tag-row">
              <span class="tag-name">GLUCOSA CAPILAR</span>
              <span class="tag-unit">mg/dL</span>
            </div>
            <input v-model="triageForm.glucosa" type="text" placeholder="ej. 110" class="input-telemetry full" />
            <span class="telemetry-hint">Ayuno: 70-130 mg/dL</span>
          </div>
        </div>

        <!-- Escala AVDI y Dolor EVA -->
        <div class="scales-split-row">
          <!-- AVDI -->
          <div class="scale-box">
            <div class="scale-box-header">
              <strong>🧠 Escala AVDI</strong>
              <span class="badge-neutral">Consciencia</span>
            </div>
            <small class="avdi-rule">
              <strong>Regla de Reanimación:</strong> Seleccionar <u>Inconsciente</u> fuerza Código Rojo Manchester Nivel I y activa Sala de Choque.
            </small>
            <div class="avdi-buttons">
              <button 
                type="button" 
                class="btn-avdi" 
                :class="{ active: triageForm.avdi === 'A' }"
                @click="triageForm.avdi = 'A'"
              >
                <strong>A</strong>
                <small>Alerta</small>
              </button>
              <button 
                type="button" 
                class="btn-avdi" 
                :class="{ active: triageForm.avdi === 'V' }"
                @click="triageForm.avdi = 'V'"
              >
                <strong>V</strong>
                <small>Verbal</small>
              </button>
              <button 
                type="button" 
                class="btn-avdi" 
                :class="{ active: triageForm.avdi === 'D' }"
                @click="triageForm.avdi = 'D'"
              >
                <strong>D</strong>
                <small>Dolor</small>
              </button>
              <button 
                type="button" 
                class="btn-avdi alert-red" 
                :class="{ active: triageForm.avdi === 'I' }"
                @click="triageForm.avdi = 'I'"
              >
                <strong>I</strong>
                <small>Inconsciente</small>
              </button>
            </div>
          </div>

          <!-- Escala Dolor EVA -->
          <div class="scale-box">
            <div class="scale-box-header">
              <strong>😣 Escala Dolor (EVA)</strong>
              <span class="badge-neutral">{{ triageForm.evaDolor }} / 10</span>
            </div>
            <small class="scale-desc">Intensidad manifestada por el paciente o evaluada mediante escala observacional.</small>
            <div class="eva-slider-container">
              <input 
                v-model="triageForm.evaDolor" 
                type="range" 
                min="0" 
                max="10" 
                class="slider-eva"
              />
              <div class="slider-labels">
                <span>0 (Sin Dolor)</span>
                <span>3 (Leve)</span>
                <span>6 (Moderado)</span>
                <span>8 (Severo)</span>
                <span>10 (Intolerable)</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 3. Motivo de Urgencia y Antecedentes Críticos -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon">📝</span>
            <h4>3. Motivo de Urgencia y Antecedentes Críticos</h4>
          </div>
        </div>

        <div class="urgency-fields">
          <div class="input-field-group">
            <label>Motivo principal de consulta y tiempo de evolución</label>
            <textarea 
              v-model="triageForm.motivoConsulta" 
              rows="3" 
              class="input-clinical" 
              placeholder="Describa el motivo principal de consulta y tiempo de evolución..."
            ></textarea>
          </div>

          <div class="antecedentes-2col">
            <div class="input-field-group">
              <label>Alergias Conocidas</label>
              <input 
                v-model="triageForm.alergias" 
                type="text" 
                class="input-clinical" 
                placeholder="ej. Penicilina, AINEs, mariscos, o Ninguna referida" 
              />
            </div>
            <div class="input-field-group">
              <label>Enfermedades Preexistentes / Comorbilidades</label>
              <input 
                v-model="triageForm.enfermedadesPrevias" 
                type="text" 
                class="input-clinical" 
                placeholder="ej. HTA, DM2, Asma, Cardiopatía..." 
              />
            </div>
          </div>
        </div>
      </section>

      <!-- 4. Tabla Inferior: Últimos Pacientes Clasificados en Guardia -->
      <section class="clinical-box">
        <div class="box-header">
          <div class="box-title-row">
            <span class="box-num-icon">📋</span>
            <h4>Últimos Pacientes Clasificados en Guardia</h4>
          </div>
          <span class="box-reg-code">Registro en tiempo real HL7 FHIR</span>
        </div>

        <div class="table-container">
          <table class="triage-records-table">
            <thead>
              <tr>
                <th>HORA</th>
                <th>PACIENTE</th>
                <th>SIGNOS CLAVE</th>
                <th>CLASIFICACIÓN MANCHESTER</th>
                <th>DESTINO ASIGNADO</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="pacientesClasificados.length === 0" class="empty-table-row">
                <td colspan="5">
                  <div class="empty-table-msg">
                    <span>📁 Sin registros en esta guardia aún</span>
                    <small>Los pacientes clasificados se listarán aquí en tiempo real.</small>
                  </div>
                </td>
              </tr>
              <tr v-for="(p, idx) in pacientesClasificados" :key="idx" class="classified-row">
                <td><strong class="time-text">{{ p.hora }}</strong></td>
                <td><span class="patient-name-cell">{{ p.paciente }}</span></td>
                <td><span class="signs-cell">{{ p.signos }}</span></td>
                <td>
                  <span class="manchester-pill" :class="p.colorClass">
                    ● {{ p.clasificacion }}
                  </span>
                </td>
                <td><span class="dest-badge">{{ p.destino }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- SUBCOLUMNA DERECHA: ALGORITMO MANCHESTER Y DECISIÓN -->
    <aside class="triage-decision-col">
      <div class="decision-card">
        <div class="decision-header">
          <span class="decision-icon">📊</span>
          <h4>Algoritmo Manchester</h4>
        </div>

        <!-- Bloque: Clasificación Calculada -->
        <div class="calculated-triage-box" :class="{ 'forced-red': triageForm.avdi === 'I' }">
          <small>CLASIFICACIÓN SUGERIDA POR ALGORITMO</small>
          
          <template v-if="triageForm.avdi === 'I'">
            <strong class="color-red">NIVEL I · REANIMACIÓN (ROJO)</strong>
            <span>Paciente Inconsciente detectado por escala AVDI. Atención instantánea.</span>
          </template>
          
          <template v-else-if="triageForm.nombrePaciente && triageForm.paSistolica">
            <strong class="color-yellow">NIVEL III · URGENTE (AMARILLO)</strong>
            <span>Evaluación preliminar calculada por telemetría ingresada.</span>
          </template>
          
          <template v-else>
            <strong>Esperando Telemetría</strong>
            <span>Ingrese signos vitales para cálculo de severidad y SLA.</span>
          </template>
        </div>

        <!-- Referencia Oficial de Niveles de Prioridad -->
        <div class="priority-levels-list">
          <div class="priority-item">
            <span class="dot-lvl red">●</span>
            <div class="lvl-desc">
              <strong>Nivel I · Reanimación (Rojo)</strong>
              <small>Atención inmediata sin espera</small>
            </div>
            <span class="lvl-time">0 min</span>
          </div>

          <div class="priority-item">
            <span class="dot-lvl orange">●</span>
            <div class="lvl-desc">
              <strong>Nivel II · Muy Urgente (Naranja)</strong>
              <small>Riesgo vital potencial o dolor extremo</small>
            </div>
            <span class="lvl-time">10-15 min</span>
          </div>

          <div class="priority-item">
            <span class="dot-lvl yellow">●</span>
            <div class="lvl-desc">
              <strong>Nivel III · Urgente (Amarillo)</strong>
              <small>Estabilidad relativa con investigación requerida</small>
            </div>
            <span class="lvl-time">60 min</span>
          </div>

          <div class="priority-item">
            <span class="dot-lvl green">●</span>
            <div class="lvl-desc">
              <strong>Nivel IV · Estándar (Verde)</strong>
              <small>Condición menor sin deterioro esperado</small>
            </div>
            <span class="lvl-time">120 min</span>
          </div>

          <div class="priority-item">
            <span class="dot-lvl blue">●</span>
            <div class="lvl-desc">
              <strong>Nivel V · No Urgente (Azul)</strong>
              <small>Problema crónico o administrativo</small>
            </div>
            <span class="lvl-time">240 min</span>
          </div>
        </div>

        <!-- Destino Clínico -->
        <div class="destination-field">
          <label>Destino Clínico Inmediato</label>
          <select v-model="destinoSeleccionado" class="input-clinical">
            <option value="Box de Urgencias Generales">Box de Urgencias Generales</option>
            <option value="Consultorio Filtro 01 / 02">Consultorio Filtro 01 / 02</option>
            <option value="Sala de Choque Inmediato (Box 01)">Sala de Choque Inmediato (Box 01)</option>
            <option value="Sala de Espera A">Sala de Espera A</option>
          </select>
        </div>

        <!-- Botones de Acción Inmediata -->
        <button type="button" class="btn-save-triage" @click="guardarTriage">
          💾 Guardar Triage y Asignar Turno [Enter]
        </button>

        <button type="button" class="btn-direct-choque" @click="derivarAChoque">
          ⚡ Derivar a Choque Inmediato [Alt+C]
        </button>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.triage-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.2rem;
  align-items: start;
}

.triage-form-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.triage-status-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.module-tag {
  font-size: 0.65rem;
  font-weight: 800;
  color: #2563EB;
  letter-spacing: 0.05em;
  display: block;
}

.module-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0 0 0.5rem 0;
}

.patient-turn-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.6rem;
}

.icon-user {
  font-size: 1.2rem;
}

.patient-turn-text small {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
  font-weight: 700;
}

.patient-turn-text strong {
  font-size: 0.85rem;
}

.text-waiting {
  color: #64748B;
  font-style: italic;
  font-weight: normal;
}

.status-chip-blue {
  background-color: #EFF6FF;
  color: #1E40AF;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  display: inline-block;
}

.banner-right-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
  font-size: 0.72rem;
}

.prescription-locked-badge {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  color: #64748B;
  font-weight: 600;
}

.room-status-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: #1E293B;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.green {
  background-color: #10B981;
}

.clinical-box {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.5rem;
}

.box-title-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.box-title-row h4 {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
}

.box-subtitle {
  font-size: 0.7rem;
  color: #64748B;
  margin-left: 0.5rem;
}

.box-reg-code {
  font-size: 0.68rem;
  color: #94A3B8;
  font-family: monospace;
}

.btn-clean {
  background: none;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  font-size: 0.7rem;
  color: #475569;
  cursor: pointer;
}

.patient-id-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 0.8rem;
}

.input-field-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.input-field-group label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #475569;
}

.input-clinical {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.7rem;
  font-size: 0.82rem;
  color: #1E293B;
  outline: none;
  background-color: #FFFFFF;
  width: 100%;
}

.input-clinical:focus {
  border-color: #2563EB;
}

.age-input-row {
  display: flex;
}

.age-input-row input {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.unit-box {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-left: none;
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
  padding: 0.5rem 0.6rem;
  font-size: 0.75rem;
  color: #64748B;
}

.telemetry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.telemetry-card {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.65rem;
  display: flex;
  flex-direction: column;
}

.card-tag-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.65rem;
  font-weight: 700;
  color: #475569;
  margin-bottom: 0.25rem;
}

.tag-unit {
  color: #2563EB;
}

.pa-inputs-row {
  display: flex;
  align-items: center;
  gap: 0.2rem;
}

.input-telemetry {
  width: 50%;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.4rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1E293B;
  text-align: center;
  outline: none;
  background: #FFFFFF;
}

.input-telemetry.full {
  width: 100%;
}

.input-telemetry:focus {
  border-color: #2563EB;
}

.pa-slash {
  font-size: 1.2rem;
  color: #94A3B8;
}

.telemetry-footer-labels {
  display: flex;
  justify-content: space-around;
  font-size: 0.62rem;
  color: #94A3B8;
  margin-top: 0.2rem;
}

.telemetry-hint {
  font-size: 0.62rem;
  color: #94A3B8;
  margin-top: 0.2rem;
}

.scales-split-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.scale-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
}

.scale-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.3rem;
  font-size: 0.75rem;
}

.badge-neutral {
  background-color: #E2E8F0;
  color: #475569;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.avdi-rule {
  display: block;
  font-size: 0.65rem;
  color: #DC2626;
  margin-bottom: 0.6rem;
  line-height: 1.2;
}

.avdi-buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.4rem;
}

.btn-avdi {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.4rem 0.2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-avdi strong {
  font-size: 1rem;
  color: #1E293B;
}

.btn-avdi small {
  font-size: 0.62rem;
  color: #64748B;
}

.btn-avdi.active {
  background-color: #2563EB;
  border-color: #2563EB;
}

.btn-avdi.active strong,
.btn-avdi.active small {
  color: #FFFFFF;
}

.btn-avdi.alert-red.active {
  background-color: #DC2626;
  border-color: #DC2626;
}

.scale-desc {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
  margin-bottom: 0.6rem;
}

.eva-slider-container {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.slider-eva {
  width: 100%;
  accent-color: #2563EB;
  cursor: pointer;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.6rem;
  color: #94A3B8;
}

.urgency-fields {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.antecedentes-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
}

.table-container {
  overflow-x: auto;
}

.triage-records-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  text-align: left;
}

.triage-records-table th {
  background-color: #F8FAFC;
  color: #64748B;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #E2E8F0;
}

.empty-table-msg {
  padding: 1.5rem;
  color: #64748B;
  text-align: center;
}

.empty-table-msg span {
  display: block;
  font-weight: 700;
  margin-bottom: 0.2rem;
}

.empty-table-msg small {
  font-size: 0.72rem;
  color: #94A3B8;
}

.classified-row td {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid #F1F5F9;
  vertical-align: middle;
}

.time-text {
  font-family: monospace;
  color: #0F4C81;
}

.patient-name-cell {
  font-weight: 700;
  color: #1E293B;
}

.signs-cell {
  font-family: monospace;
  font-size: 0.72rem;
  color: #475569;
}

.manchester-pill {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
}

.manchester-pill.yellow {
  background-color: #FEF3C7;
  color: #B45309;
}

.manchester-pill.orange {
  background-color: #FFEDD5;
  color: #C2410C;
}

.manchester-pill.red {
  background-color: #FEE2E2;
  color: #B91C1C;
}

.dest-badge {
  background-color: #F1F5F9;
  color: #334155;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 600;
}

.triage-decision-col {
  display: flex;
  flex-direction: column;
}

.decision-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.decision-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.5rem;
}

.decision-header h4 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.calculated-triage-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  text-align: center;
}

.calculated-triage-box small {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
  margin-bottom: 0.3rem;
}

.calculated-triage-box strong {
  display: block;
  font-size: 1rem;
  color: #1E293B;
}

.calculated-triage-box.forced-red {
  background-color: #FEF2F2;
  border-color: #FECACA;
}

.color-red {
  color: #DC2626 !important;
}

.color-yellow {
  color: #D97706 !important;
}

.calculated-triage-box span {
  display: block;
  font-size: 0.7rem;
  color: #64748B;
  margin-top: 0.3rem;
}

.priority-levels-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.priority-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.5rem;
  border-radius: 6px;
  background-color: #F8FAFC;
  font-size: 0.72rem;
}

.dot-lvl {
  font-size: 0.9rem;
  margin-right: 0.4rem;
}

.dot-lvl.red { color: #DC2626; }
.dot-lvl.orange { color: #EA580C; }
.dot-lvl.yellow { color: #D97706; }
.dot-lvl.green { color: #059669; }
.dot-lvl.blue { color: #2563EB; }

.lvl-desc {
  flex: 1;
}

.lvl-desc strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
}

.lvl-desc small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.lvl-time {
  font-weight: 700;
  color: #1E293B;
  font-family: monospace;
}

.destination-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.destination-field label {
  font-size: 0.7rem;
  font-weight: 700;
  color: #475569;
}

.btn-save-triage {
  background: linear-gradient(135deg, #0F4C81, #1E40AF);
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(15, 76, 129, 0.2);
}

.btn-save-triage:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-direct-choque {
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
  border-radius: 8px;
  padding: 0.65rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-direct-choque:hover {
  background-color: #FEE2E2;
}

@media (max-width: 1100px) {
  .triage-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .patient-id-grid, .telemetry-grid, .scales-split-row, .antecedentes-2col {
    grid-template-columns: 1fr;
  }
}
</style>
