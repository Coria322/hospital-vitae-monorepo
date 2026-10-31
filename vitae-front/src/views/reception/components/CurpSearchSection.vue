<script setup lang="ts">
import { ref, computed } from 'vue'

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: 'curp' | 'registrar' | 'agendar'): void
  (e: 'seleccionarPaciente', p: any): void
  (e: 'curpSeleccionada', curp: string): void
}>()

const mostrarAlerta = (mensaje: string) => {
  alert(mensaje)
}

const modoBusqueda = ref<'curp' | 'nombre' | 'expediente' | 'ine'>('curp')
const inputCurp = ref('')
const busquedaRealizada = ref(false)

interface PacienteEncontrado {
  cupe: string
  verificadaRenapo: boolean
  moduloArchivo: string
  derechohabiencia: string
  regimen: string
  poliza: string
  nombreCompleto: string
  curp: string
  sexo: string
  edad: number
  fechaNacimiento: string
  domicilio: string
  cp: string
  contactoFamiliar: string
  parentesco: string
  telefonoContacto: string
  citasActivas: boolean
  ultimoIngreso: string
  avatarUrl?: string
}

const pacienteEncontrado = ref<PacienteEncontrado | null>(null)

const curpValidaSintaxis = computed(() => {
  const regex = /^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$/
  return regex.test(inputCurp.value.toUpperCase().trim())
})

const ejecutarBusquedaCurp = () => {
  if (!inputCurp.value) {
    alert('Ingrese una clave CURP de 18 caracteres para realizar la consulta.')
    return
  }
  busquedaRealizada.value = true
  if (inputCurp.value.toUpperCase().startsWith('MORA')) {
    cargarPacienteDemoBusqueda()
  } else {
    pacienteEncontrado.value = {
      cupe: '#EXP-2026-9042',
      verificadaRenapo: true,
      moduloArchivo: 'Módulo B-4',
      derechohabiencia: 'Vigente',
      regimen: 'IMSS Régimen Ordinario',
      poliza: '#99482-B2 · Al corriente',
      nombreCompleto: 'Morales Rivas, Laura Elena',
      curp: inputCurp.value.toUpperCase(),
      sexo: 'Femenino',
      edad: 36,
      fechaNacimiento: '15 de abril de 1988 (CDMX)',
      domicilio: 'Av. Insurgentes Sur 1420, Int. 402, Col. Actipan, Benito Juárez, CDMX',
      cp: '03230 · Verificado CFE 2026',
      contactoFamiliar: 'Roberto Rivas Méndez',
      parentesco: 'Hermano',
      telefonoContacto: '+52 55 4912 8840 · Notificación autorizada',
      citasActivas: false,
      ultimoIngreso: '12 ago 2025 (Consulta Externa)'
    }
  }
}

const agendarCitaConPaciente = () => {
  if (pacienteEncontrado.value) {
    emit('seleccionarPaciente', pacienteEncontrado.value)
    emit('curpSeleccionada', pacienteEncontrado.value.curp)
  }
  emit('cambiarSeccion', 'agendar')
}

const actualizarDatosConPaciente = () => {
  if (pacienteEncontrado.value) {
    emit('seleccionarPaciente', pacienteEncontrado.value)
    emit('curpSeleccionada', pacienteEncontrado.value.curp)
  }
  emit('cambiarSeccion', 'registrar')
}

const limpiarBusquedaCurp = () => {
  inputCurp.value = ''
  busquedaRealizada.value = false
  pacienteEncontrado.value = null
}

const cargarPacienteDemoBusqueda = () => {
  inputCurp.value = 'MORA880415MDFRRL03'
  busquedaRealizada.value = true
  pacienteEncontrado.value = {
    cupe: '#EXP-2026-9042',
    verificadaRenapo: true,
    moduloArchivo: 'Módulo B-4',
    derechohabiencia: 'Vigente',
    regimen: 'IMSS Régimen Ordinario',
    poliza: '#99482-B2 · Al corriente',
    nombreCompleto: 'Morales Rivas, Laura Elena',
    curp: 'MORA880415MDFRRL03',
    sexo: 'Femenino',
    edad: 36,
    fechaNacimiento: '15 de abril de 1988 (CDMX)',
    domicilio: 'Av. Insurgentes Sur 1420, Int. 402, Col. Actipan, Benito Juárez, CDMX',
    cp: '03230 · Verificado CFE 2026',
    contactoFamiliar: 'Roberto Rivas Méndez',
    parentesco: 'Hermano',
    telefonoContacto: '+52 55 4912 8840 · Notificación autorizada',
    citasActivas: false,
    ultimoIngreso: '12 ago 2025 (Consulta Externa)'
  }
}

const busquedaNombreForm = ref({
  primerApellido: '',
  segundoApellido: '',
  nombres: '',
  fechaNacimiento: ''
})

const ejecutarBusquedaPorNombre = () => {
  if (!busquedaNombreForm.value.primerApellido && !busquedaNombreForm.value.nombres) {
    alert('Ingrese al menos el primer apellido o nombre para realizar la búsqueda fonética.')
    return
  }
  cargarPacienteDemoBusqueda()
}

const busquedaCupeInput = ref('')

const ejecutarBusquedaPorCupe = () => {
  if (!busquedaCupeInput.value) {
    alert('Ingrese el folio de Expediente (CUPE) para buscar.')
    return
  }
  cargarPacienteDemoBusqueda()
}

const simularEscaneoIne = () => {
  inputCurp.value = 'MORA880415MDFRRL03'
  ejecutarBusquedaCurp()
  alert('[SCANNER] Código bidimensional PDF417 de Credencial INE decodificado exitosamente con RENAPO.')
}

const historialConsultas = ref([
  {
    hora: '10:32 h',
    paciente: 'G. L., Carlos Alberto',
    curp: 'GOLC740921******',
    folio: 'EXP-2024-8114',
    vigencia: 'IMSS Vigente',
    tipoVigencia: 'imss'
  },
  {
    hora: '10:14 h',
    paciente: 'V. C., Mariana',
    curp: 'VACM951103******',
    folio: 'EXP-2026-9038',
    vigencia: 'Particular / GNP',
    tipoVigencia: 'particular'
  },
  {
    hora: '09:48 h',
    paciente: 'S. T., Fernando',
    curp: 'SATF620310******',
    folio: 'EXP-2019-3301',
    vigencia: 'ISSSTE Activo',
    tipoVigencia: 'issste'
  }
])

const cargarDesdeHistorial = (item: typeof historialConsultas.value[0]) => {
  inputCurp.value = item.curp.replace(/\*/g, '0')
  ejecutarBusquedaCurp()
}
</script>

<template>
  <div class="view-curp-container">
    <!-- Encabezado de la Pantalla de Búsqueda -->
    <div class="section-top-banner">
      <div class="banner-text-col">
        <div class="badges-row">
          <span class="cert-pill blue">MODALIDAD CONSULTA NOM-024</span>
          <span class="cert-pill green">INTERCONEXIÓN RENAPO ACTIVA</span>
        </div>
        <h2>Búsqueda y Localización Inmediata de Paciente</h2>
        <p>Valide la Clave Única de Registro de Población (CURP) para abrir expediente existente o verificar antecedentes registrales.</p>
      </div>

      <div class="banner-right-kpis-row">
        <div class="mini-kpi-white">
          <span class="kpi-symbol"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg></span>
          <div>
            <small>TIEMPO RESPUESTA</small>
            <strong>24 ms · RENAPO OK</strong>
          </div>
        </div>

        <div class="mini-kpi-white">
          <span class="kpi-symbol"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
          <div>
            <small>HOMONIMIAS</small>
            <strong class="text-green">0 Detectadas</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- Cuadrícula Principal (Formulario a la Izquierda + Panel Lateral a la Derecha) -->
    <div class="curp-layout-grid">
      
      <!-- SUBCOLUMNA IZQUIERDA: Formulario de Búsqueda y Expediente -->
      <div class="curp-left-col">
        
        <!-- Tarjeta de Búsqueda con Pestañas -->
        <div class="step-card search-criteria-card">
          <div class="criteria-tabs-bar">
            <button 
              type="button" 
              class="tab-btn" 
              :class="{ active: modoBusqueda === 'curp' }"
              @click="modoBusqueda = 'curp'"
            >
              <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><line x1="15" y1="8" x2="17" y2="8"/><line x1="15" y1="12" x2="17" y2="12"/><line x1="7" y1="16" x2="17" y2="16"/></svg> Por CURP Oficial</span>
            </button>
            <button 
              type="button" 
              class="tab-btn" 
              :class="{ active: modoBusqueda === 'nombre' }"
              @click="modoBusqueda = 'nombre'"
            >
              <span><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> Nombre y Fecha Nac.</span>
            </button>
            <button 
              type="button" 
              class="tab-btn" 
              :class="{ active: modoBusqueda === 'expediente' }"
              @click="modoBusqueda = 'expediente'"
            >
              <span><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg> Expediente (CUPE)</span>
            </button>
            <button 
              type="button" 
              class="tab-btn" 
              :class="{ active: modoBusqueda === 'ine' }"
              @click="modoBusqueda = 'ine'"
            >
              <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg> Escaneo Óptico INE</span>
            </button>
          </div>

          <div class="step-body">
            
            <!-- Opción A: Búsqueda por CURP -->
            <div v-if="modoBusqueda === 'curp'" class="search-curp-input-block">
              <label class="field-label-sm">
                Clave Única de Registro de Población (18 caracteres)
                <span v-if="curpValidaSintaxis" class="sintaxis-ok"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Formato sintáctico válido</span>
              </label>

              <div class="search-input-action-row">
                <div class="curp-field-wrapper">
                  <span class="field-prefix"><svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
                  <input 
                    type="text" 
                    v-model="inputCurp" 
                    placeholder="MORA880415MDFRRL03" 
                    maxlength="18"
                    class="curp-input"
                    @keyup.enter="ejecutarBusquedaCurp"
                  />
                  <button 
                    v-if="inputCurp" 
                    type="button" 
                    class="btn-clear-input" 
                    @click="limpiarBusquedaCurp"
                    title="Limpiar campo"
                  >
                    <svg viewBox="0 0 24 24" class="svg-inline-x" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </div>

                <button 
                  type="button" 
                  class="btn-search-renapo"
                  @click="ejecutarBusquedaCurp"
                >
                  <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Validar RENAPO [Enter]
                </button>
              </div>

              <div class="search-helpers-row">
                <div class="search-rule-tip">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg></span>
                  <small>Si el paciente no cuenta con su CURP física, use la pestaña <strong>Nombre y Fecha Nac.</strong> para búsqueda fonética de RENAPO.</small>
                </div>

                <div class="scanner-status">
                  <span class="ping-dot"></span>
                  <small>Lector 2D USB: <strong>Honeywell Xenon Conectado</strong></small>
                </div>
              </div>
            </div>

            <!-- Opción B: Búsqueda por Nombre y Fecha -->
            <div v-else-if="modoBusqueda === 'nombre'" class="search-name-input-block">
              <label class="field-label-sm">Búsqueda Fonética por Nombre y Apellidos (RENAPO)</label>
              
              <div class="search-name-grid">
                <div class="form-group-item">
                  <label>Primer Apellido (Paterno)</label>
                  <input type="text" v-model="busquedaNombreForm.primerApellido" placeholder="Morales" class="form-input" />
                </div>
                <div class="form-group-item">
                  <label>Segundo Apellido (Materno)</label>
                  <input type="text" v-model="busquedaNombreForm.segundoApellido" placeholder="Rivas" class="form-input" />
                </div>
                <div class="form-group-item">
                  <label>Nombre(s)</label>
                  <input type="text" v-model="busquedaNombreForm.nombres" placeholder="Laura Elena" class="form-input" />
                </div>
                <div class="form-group-item">
                  <label>Fecha de Nacimiento</label>
                  <input type="date" v-model="busquedaNombreForm.fechaNacimiento" class="form-input" />
                </div>
              </div>

              <div class="search-actions-row-end mt-2">
                <button type="button" class="btn-search-renapo" @click="ejecutarBusquedaPorNombre">
                  <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Buscar en Base Hospitalaria y RENAPO
                </button>
              </div>
            </div>

            <!-- Opción C: Búsqueda por Expediente (CUPE) -->
            <div v-else-if="modoBusqueda === 'expediente'" class="search-cupe-input-block">
              <label class="field-label-sm">Clave Única de Expediente Clínico (CUPE)</label>
              
              <div class="search-input-action-row">
                <div class="curp-field-wrapper">
                  <span class="field-prefix"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
                  <input 
                    type="text" 
                    v-model="busquedaCupeInput" 
                    placeholder="EXP-2026-9042" 
                    class="curp-input"
                    @keyup.enter="ejecutarBusquedaPorCupe"
                  />
                </div>
                <button type="button" class="btn-search-renapo" @click="ejecutarBusquedaPorCupe">
                  <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Localizar Expediente
                </button>
              </div>
            </div>

            <!-- Opción D: Escaneo Óptico INE -->
            <div v-else-if="modoBusqueda === 'ine'" class="search-ine-input-block">
              <div class="ine-scanner-box">
                <div class="scanner-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div>
                <h4>Lector Óptico de Credencial para Votar INE / Cédula</h4>
                <p>Coloque la credencial oficial de elector frente al escáner óptico 2D para decodificar automáticamente el código PDF417.</p>
                <button type="button" class="btn-search-renapo" @click="simularEscaneoIne">
                  <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg> Simular Captura Óptica de Código INE
                </button>
              </div>
            </div>

          </div>
        </div>

        <!-- Tarjeta de Expediente Encontrado (En Blanco o Poblado) -->
        <div v-if="pacienteEncontrado" class="step-card patient-record-card">
          <div class="record-header">
            <div class="record-badge-left">
              <span class="exp-code-pill">{{ pacienteEncontrado.cupe }}</span>
              <span class="renapo-verified-pill"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> CURP Verificada RENAPO</span>
            </div>
            <div class="record-badge-right">
              <span class="archive-module-tag">Módulo Archivo Clínico: <strong>{{ pacienteEncontrado.moduloArchivo }}</strong></span>
            </div>
          </div>

          <div class="record-body">
            
            <!-- Fila Superior de Identidad -->
            <div class="patient-main-row">
              <div class="patient-avatar-box">
                <div class="avatar-ph">MR</div>
                <span class="status-dot-active"></span>
              </div>

              <div class="patient-title-info">
                <div class="name-vigencia-row">
                  <h3>{{ pacienteEncontrado.nombreCompleto }}</h3>
                  <span class="vigencia-badge-green">● DERECHOHABIENCIA: {{ pacienteEncontrado.derechohabiencia }}</span>
                </div>

                <div class="meta-pills-row">
                  <span class="meta-tag">CURP: <strong>{{ pacienteEncontrado.curp }}</strong></span>
                  <span class="meta-tag">Sexo: <strong>{{ pacienteEncontrado.sexo }}</strong></span>
                  <span class="meta-tag">Edad: <strong>{{ pacienteEncontrado.edad }} años</strong></span>
                  <span class="meta-tag">Fecha Nac: <strong>{{ pacienteEncontrado.fechaNacimiento }}</strong></span>
                </div>
              </div>
            </div>

            <!-- 3 Subtarjetas de Detalle Demográfico -->
            <div class="patient-subcards-row">
              <!-- Subtarjeta 1: Domicilio -->
              <div class="subcard-info">
                <div class="subcard-title">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg></span>
                  <strong>Domicilio Particular</strong>
                </div>
                <p>{{ pacienteEncontrado.domicilio }}</p>
                <small>C.P. {{ pacienteEncontrado.cp }}</small>
              </div>

              <!-- Subtarjeta 2: Contacto Familiar -->
              <div class="subcard-info">
                <div class="subcard-title">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg></span>
                  <strong>Contacto Familiar / Notificación</strong>
                </div>
                <p><strong>{{ pacienteEncontrado.contactoFamiliar }}</strong> ({{ pacienteEncontrado.parentesco }})</p>
                <small>{{ pacienteEncontrado.telefonoContacto }}</small>
              </div>

              <!-- Subtarjeta 3: Aseguramiento -->
              <div class="subcard-info">
                <div class="subcard-title">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                  <strong>Aseguramiento & Régimen</strong>
                </div>
                <p><strong>{{ pacienteEncontrado.regimen }}</strong></p>
                <small>Póliza: {{ pacienteEncontrado.poliza }}</small>
              </div>
            </div>

            <!-- Fila de Acciones Clínicas Inmediatas -->
            <div class="record-actions-footer">
              <div class="actions-left-group">
                <button type="button" class="btn-action-primary" @click="agendarCitaConPaciente">
                  <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg> Agendar Cita Médica <kbd>Alt + C</kbd>
                </button>
                <button type="button" class="btn-action-secondary" @click="mostrarAlerta('Generando hoja oficial de admisión hospitalaria en formato PDF...')">
                  <svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg> Expedir Hoja de Admisión
                </button>
                <button type="button" class="btn-action-neutral" @click="actualizarDatosConPaciente">
                  <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg> Actualizar Datos Demográficos
                </button>
              </div>

              <div class="history-quick-link">
                <button type="button" class="btn-link-blue" @click="mostrarAlerta('Consultando bitácora de ingresos hospitalarios del paciente...')">
                  <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> Ver Expediente Completo (3 Ingresos)
                </button>
              </div>
            </div>

          </div>
        </div>

        <!-- Estado Vacío Cuando no hay Paciente Buscado -->
        <div v-else class="step-card empty-search-placeholder">
          <div class="empty-icon-row"><svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></div>
          <h4>Sin paciente seleccionado</h4>
          <p>Ingrese una CURP de 18 caracteres y presione <strong>Validar RENAPO</strong>, o utilice el botón <strong>Cargar Demo</strong> para visualizar la ficha técnica con todos sus datos oficiales.</p>
        </div>

        <!-- Historial Reciente de Consultas del Turno (Tabla Inferior) -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-num-title">
              <span class="step-circle"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
              <div>
                <h4>Historial Reciente de Consultas del Turno</h4>
                <small>Últimas validaciones de CURP y derechohabiencia ejecutadas en este puesto de atención.</small>
              </div>
            </div>
            <span class="step-shortcut">3 REGISTROS</span>
          </div>

          <div class="step-body no-padding">
            <div class="table-responsive">
              <table class="recent-consults-table">
                <thead>
                  <tr>
                    <th>HORA</th>
                    <th>PACIENTE</th>
                    <th>CURP AUDITADA</th>
                    <th>EXPEDIENTE (CUPE)</th>
                    <th>VIGENCIA DERECHOHABIENCIA</th>
                    <th class="text-right">ACCIÓN</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in historialConsultas" :key="idx">
                    <td class="col-time font-bold">{{ item.hora }}</td>
                    <td class="col-patient font-bold">{{ item.paciente }}</td>
                    <td class="col-curp"><code>{{ item.curp }}</code></td>
                    <td class="col-folio"><span class="exp-tag">{{ item.folio }}</span></td>
                    <td>
                      <span class="vigencia-pill" :class="item.tipoVigencia">
                        ● {{ item.vigencia }}
                      </span>
                    </td>
                    <td class="text-right">
                      <button type="button" class="btn-quick-load" @click="cargarDesdeHistorial(item)">
                        Cargar Ficha
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>

      <!-- SUBCOLUMNA DERECHA: Censo, Atajos, Homonimia y Confidencialidad -->
      <aside class="curp-right-col">
        
        <!-- 1. Censo de Admisión Diario -->
        <div class="side-widget-card">
          <div class="widget-header">
            <div class="title-row-sm">
              <span class="live-dot-blue">●</span>
              <strong>Censo de Admisión Diario</strong>
            </div>
            <span class="time-stamp-sm">Tiempo Real</span>
          </div>

          <div class="census-items-list">
            <div class="census-item">
              <div class="census-item-left">
                <span><svg viewBox="0 0 24 24" class="svg-inline-users" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg></span>
                <div>
                  <strong>Pacientes Registrados Hoy</strong>
                  <small>Corte matutino activo</small>
                </div>
              </div>
              <strong class="census-val">142</strong>
            </div>

            <div class="census-item">
              <div class="census-item-left">
                <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></span>
                <div>
                  <strong>En Sala de Espera</strong>
                  <small>Turnos en ventanilla</small>
                </div>
              </div>
              <strong class="census-val text-blue">18</strong>
            </div>

            <div class="census-item">
              <div class="census-item-left">
                <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                <div>
                  <strong>Homonimias Prevenidas</strong>
                  <small>NOM-024 RENAPO</small>
                </div>
              </div>
              <strong class="census-val text-green">00</strong>
            </div>
          </div>

          <!-- Barra de Ocupación Global -->
          <div class="global-occupancy-bar">
            <div class="occ-label-row">
              <span>Capacidad Global de Ocupación</span>
              <strong>77.3%</strong>
            </div>
            <div class="occ-progress-track">
              <div class="occ-fill" style="width: 77.3%;"></div>
            </div>
          </div>
        </div>

        <!-- 2. Atajos del Puesto de Admisión -->
        <div class="side-widget-card">
          <div class="widget-header">
            <div class="title-row-sm">
              <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><line x1="6" y1="8" x2="6.01" y2="8"/><line x1="10" y1="8" x2="10.01" y2="8"/><line x1="14" y1="8" x2="14.01" y2="8"/><line x1="18" y1="8" x2="18.01" y2="8"/><line x1="6" y1="12" x2="6.01" y2="12"/><line x1="18" y1="12" x2="18.01" y2="12"/><line x1="10" y1="12" x2="14" y2="12"/><line x1="8" y1="16" x2="16" y2="16"/></svg></span>
              <strong>Atajos del Puesto de Admisión</strong>
            </div>
          </div>

          <div class="shortcuts-list">
            <div class="shortcut-row" @click="emit('cambiarSeccion', 'curp')">
              <span>Búsqueda por CURP</span>
              <kbd>Alt + B</kbd>
            </div>
            <div class="shortcut-row" @click="emit('cambiarSeccion', 'registrar')">
              <span>Registrar Nuevo Paciente</span>
              <kbd>Alt + P</kbd>
            </div>
            <div class="shortcut-row" @click="emit('cambiarSeccion', 'agendar')">
              <span>Asignar Cita Médica</span>
              <kbd>Alt + C</kbd>
            </div>
            <div class="shortcut-row" @click="limpiarBusquedaCurp">
              <span>Limpiar y Nuevo Escaneo</span>
              <kbd>Esc</kbd>
            </div>
          </div>
        </div>

        <!-- 3. Criterios de Homonimia NOM-024 (Tarjeta Oscura) -->
        <div class="homonimia-card-dark">
          <div class="homonimia-header">
            <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> CRITERIOS DE HOMONIMIA NOM-024</span>
          </div>
          <h4>Regla de Expediente Único Nacional</h4>
          <p>Queda estrictamente prohibida la apertura de un segundo folio hospitalario ante coincidencias en nombre completo y entidad federativa. Si RENAPO rechaza o devuelve homonimia, verifique acta de nacimiento física o notifique al Área de Trabajo Social y Archivo Clínico.</p>
          <div class="audit-id-badge">
            <code>Auditoría ID: AUD-2026-VTE</code>
          </div>
        </div>

        <!-- 4. Confidencialidad Hospitalaria -->
        <div class="confidentiality-card-light">
          <span class="lock-shield"><svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></span>
          <div>
            <strong>Confidencialidad Hospitalaria</strong>
            <p>Los monitores de sala pública solo desplegarán iniciales y turno de llamada. No se transmiten padecimientos ni diagnósticos.</p>
          </div>
        </div>

      </aside>
    </div>
  </div>
</template>

<style scoped>
.view-curp-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

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
}

.badges-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
  flex-wrap: wrap;
}

.cert-pill {
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.cert-pill.blue { background-color: #EFF6FF; color: #1D4ED8; }
.cert-pill.green { background-color: #ECFDF5; color: #047857; }

.banner-text-col h2 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0 0 0.25rem 0;
}

.banner-text-col p {
  font-size: 0.8rem;
  color: #64748B;
  margin: 0;
}

.banner-right-kpis-row {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.mini-kpi-white {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.5rem 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.kpi-symbol {
  font-size: 1.2rem;
}

.mini-kpi-white small {
  display: block;
  font-size: 0.6rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
}

.mini-kpi-white strong {
  font-size: 0.82rem;
  color: #1E293B;
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

.step-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.criteria-tabs-bar {
  display: flex;
  border-bottom: 1px solid #E2E8F0;
  background-color: #F8FAFC;
  overflow-x: auto;
}

.tab-btn {
  background: none;
  border: none;
  padding: 0.65rem 1rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}

.tab-btn.active {
  color: #0F4C81;
  border-bottom-color: #0F4C81;
  background-color: #FFFFFF;
  font-weight: 700;
}

.step-body {
  padding: 1.2rem;
}

.field-label-sm {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: #334155;
  margin-bottom: 0.4rem;
}

.sintaxis-ok {
  color: #059669;
  font-size: 0.7rem;
}

.search-input-action-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.curp-field-wrapper {
  flex: 1;
  min-width: 260px;
  position: relative;
  display: flex;
  align-items: center;
}

.field-prefix {
  position: absolute;
  left: 0.75rem;
  font-size: 0.9rem;
  color: #64748B;
}

.curp-input {
  width: 100%;
  border: 1.5px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.6rem 2.2rem 0.6rem 2.2rem;
  font-size: 0.95rem;
  font-weight: 800;
  font-family: monospace;
  letter-spacing: 0.05em;
  color: #0F2D4A;
  outline: none;
  background-color: #FFFFFF;
}

.curp-input:focus {
  border-color: #0284C7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.btn-clear-input {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  color: #94A3B8;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-search-renapo {
  background: linear-gradient(135deg, #0F4C81, #0284C7);
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.6rem 1.1rem;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.btn-demo-slot {
  background-color: #F0FDF4;
  border: 1px dashed #22C55E;
  color: #15803D;
  border-radius: 6px;
  padding: 0.6rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.search-helpers-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
  font-size: 0.72rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.search-rule-tip {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #475569;
}

.scanner-status {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: #0F4C81;
}

.ping-dot {
  width: 8px;
  height: 8px;
  background-color: #10B981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.1); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

.search-name-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group-item label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #475569;
}

.form-input {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
  font-size: 0.8rem;
  outline: none;
  background-color: #FFFFFF;
}

.search-actions-row-end {
  display: flex;
  justify-content: flex-end;
}

.ine-scanner-box {
  text-align: center;
  padding: 1.5rem;
}

.scanner-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.ine-scanner-box h4 {
  font-size: 1rem;
  color: #0F4C81;
  margin-bottom: 0.3rem;
}

.ine-scanner-box p {
  font-size: 0.8rem;
  color: #64748B;
  max-width: 450px;
  margin: 0 auto 1.25rem auto;
}

/* PACIENTE ENCONTRADO */
.patient-record-card {
  border-left: 4px solid #0284C7;
}

.record-header {
  background-color: #F8FAFC;
  border-bottom: 1px solid #E2E8F0;
  padding: 0.65rem 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.record-badge-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.exp-code-pill {
  background-color: #E0F2FE;
  color: #0369A1;
  font-weight: 800;
  font-size: 0.75rem;
  font-family: monospace;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.renapo-verified-pill {
  color: #059669;
  font-size: 0.72rem;
  font-weight: 700;
}

.archive-module-tag {
  font-size: 0.72rem;
  color: #64748B;
}

.record-body {
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.patient-main-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.patient-avatar-box {
  position: relative;
}

.avatar-ph {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0F2D4A, #0284C7);
  color: #FFFFFF;
  font-weight: 900;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-dot-active {
  width: 12px;
  height: 12px;
  background-color: #10B981;
  border: 2px solid #FFFFFF;
  border-radius: 50%;
  position: absolute;
  bottom: -2px;
  right: -2px;
}

.patient-title-info {
  flex: 1;
}

.name-vigencia-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 0.35rem;
  flex-wrap: wrap;
}

.name-vigencia-row h3 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0;
}

.vigencia-badge-green {
  background-color: #DCFCE7;
  color: #166534;
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.meta-pills-row {
  display: flex;
  gap: 0.8rem;
  font-size: 0.75rem;
  color: #64748B;
  flex-wrap: wrap;
}

.patient-subcards-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.85rem;
}

.subcard-info {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
}

.subcard-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: #0F2D4A;
  margin-bottom: 0.35rem;
}

.subcard-info p {
  font-size: 0.72rem;
  color: #334155;
  margin: 0 0 0.25rem 0;
  line-height: 1.35;
}

.subcard-info small {
  font-size: 0.65rem;
  color: #64748B;
}

.record-actions-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #F1F5F9;
  padding-top: 1rem;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.actions-left-group {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-action-primary {
  background: linear-gradient(135deg, #0F4C81, #0284C7);
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.55rem 0.95rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-action-secondary {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #334155;
  border-radius: 6px;
  padding: 0.55rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-neutral {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  color: #475569;
  border-radius: 6px;
  padding: 0.55rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-link-blue {
  background: none;
  border: none;
  color: #0284C7;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.empty-search-placeholder {
  padding: 3rem 1.5rem;
  text-align: center;
  color: #64748B;
}

.empty-icon-row {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.empty-search-placeholder h4 {
  font-size: 1.05rem;
  color: #1E293B;
  margin-bottom: 0.35rem;
}

.empty-search-placeholder p {
  font-size: 0.82rem;
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.4;
}

/* HISTORIAL CONSULTAS */
.step-card-header {
  padding: 0.85rem 1.2rem;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-num-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.step-circle {
  font-size: 1.2rem;
}

.step-num-title h4 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0;
}

.step-num-title small {
  font-size: 0.7rem;
  color: #64748B;
}

.step-shortcut {
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748B;
}

.no-padding {
  padding: 0 !important;
}

.table-responsive {
  overflow-x: auto;
}

.recent-consults-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  text-align: left;
}

.recent-consults-table th {
  background-color: #F8FAFC;
  padding: 0.65rem 0.85rem;
  color: #64748B;
  font-size: 0.68rem;
  font-weight: 700;
  border-bottom: 1px solid #E2E8F0;
}

.recent-consults-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid #F1F5F9;
  vertical-align: middle;
}

.font-bold { font-weight: 700; }

.col-time { color: #64748B; }
.col-patient { color: #0F2D4A; }

.col-curp code {
  background-color: #F1F5F9;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-family: monospace;
}

.exp-tag {
  font-family: monospace;
  font-weight: 700;
  color: #0F4C81;
}

.vigencia-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.vigencia-pill.imss { background-color: #ECFDF5; color: #047857; }
.vigencia-pill.particular { background-color: #EFF6FF; color: #1D4ED8; }
.vigencia-pill.issste { background-color: #F0FDF4; color: #15803D; }

.btn-quick-load {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 0.25rem 0.55rem;
  font-size: 0.68rem;
  font-weight: 700;
  color: #0F4C81;
  cursor: pointer;
}

.btn-quick-load:hover {
  background-color: #EFF6FF;
}

/* SUBCOLUMNA DERECHA */
.curp-right-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
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

.live-dot-blue {
  color: #2563EB;
}

.census-items-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.census-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem;
}

.census-item-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.census-item-left strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.census-item-left small {
  font-size: 0.62rem;
  color: #64748B;
}

.census-val {
  font-size: 1.1rem;
  font-weight: 800;
  color: #1E293B;
}

.text-blue { color: #0284C7; }
.text-green { color: #10B981; }

.global-occupancy-bar {
  border-top: 1px solid #F1F5F9;
  padding-top: 0.75rem;
}

.occ-label-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: #475569;
  margin-bottom: 0.35rem;
}

.occ-progress-track {
  height: 6px;
  background-color: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.occ-fill {
  height: 100%;
  background: linear-gradient(90deg, #0284C7, #0F2D4A);
  border-radius: 3px;
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.shortcut-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.45rem 0.6rem;
  border-radius: 6px;
  background-color: #F8FAFC;
  font-size: 0.75rem;
  color: #334155;
  cursor: pointer;
}

.shortcut-row:hover {
  background-color: #EFF6FF;
}

kbd {
  font-family: monospace;
  font-size: 0.68rem;
  padding: 0.15rem 0.35rem;
  background: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  color: #64748B;
}

.homonimia-card-dark {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border-radius: 10px;
  padding: 1rem;
}

.homonimia-header {
  font-size: 0.65rem;
  font-weight: 800;
  color: #38BDF8;
  margin-bottom: 0.4rem;
}

.homonimia-card-dark h4 {
  font-size: 0.88rem;
  margin: 0 0 0.4rem 0;
  color: #FFFFFF;
}

.homonimia-card-dark p {
  font-size: 0.7rem;
  line-height: 1.4;
  color: #CBD5E1;
  margin: 0 0 0.8rem 0;
}

.audit-id-badge code {
  font-family: monospace;
  font-size: 0.65rem;
  background-color: rgba(255, 255, 255, 0.1);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: #93C5FD;
}

.confidentiality-card-light {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.85rem;
  display: flex;
  gap: 0.6rem;
}

.lock-shield {
  font-size: 1.2rem;
}

.confidentiality-card-light strong {
  display: block;
  font-size: 0.75rem;
  color: #0F2D4A;
  margin-bottom: 0.15rem;
}

.confidentiality-card-light p {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0;
  line-height: 1.3;
}

@media (max-width: 1100px) {
  .curp-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .patient-subcards-row, .search-name-grid {
    grid-template-columns: 1fr;
  }
}
</style>
