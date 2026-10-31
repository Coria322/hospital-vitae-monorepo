<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

const props = withDefaults(
  defineProps<{
    recepcionista?: {
      nombre: string
      cedula: string
      cargo?: string
      sede?: string
    }
    pacienteSeleccionado?: any
  }>(),
  {
    recepcionista: () => ({
      nombre: 'Lic. Andrea Morales',
      cedula: 'ADM-102941',
      cargo: 'Admisión y Archivo Clínico',
      sede: 'Sede Principal Norte'
    })
  }
)

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: SeccionRecepcion): void
}>()

const mostrarAlerta = (mensaje: string) => {
  alert(mensaje)
}

// =========================================================================
// PANTALLA 4: GESTIÓN DE CONSENTIMIENTOS INFORMADOS (CONFORME A CAPTURA ORIGINAL)
// =========================================================================
const consentimientoForm = ref({
  curpBusqueda: 'ALMN880415HDFRRL09',
  pacienteVinculado: {
    nombre: 'Martínez Álvarez, Marco Ant...',
    edad: 36,
    sexo: 'Masculino',
    curp: 'ALMN880415HDFRRL09',
    folio: 'Folio: ADM-2026-8812',
    cama: 'Cama Asignada: 304-B (Medicina Interna)',
    regimen: 'IMSS Ordinario Activo'
  },
  nombreOtorgante: 'Elena García Rivas',
  parentesco: 'Cónyuge',
  identificacionOficial: 'INE / Credencial Elector',
  folioIdentificacion: 'IDMEX1984329412<<8829',
  telefonoCelular: '+52 55 4910 2094',
  correoNotificacion: 'elena.garcia@email.com',
  documentoSeleccionado: 'admision' as 'admision' | 'privacidad' | 'procedimientos' | 'informados',
  declaracionAceptada: true,
  trazoFirmado: true,
  huellaRegistrada: true
})

const sincronizarPacienteConsentimiento = () => {
  if (props.pacienteSeleccionado) {
    const p = props.pacienteSeleccionado
    consentimientoForm.value.curpBusqueda = p.curp || consentimientoForm.value.curpBusqueda
    consentimientoForm.value.pacienteVinculado = {
      nombre: p.nombreCompleto || p.nombre || 'Paciente Seleccionado',
      edad: p.edad || 36,
      sexo: p.sexo || 'No especificado',
      curp: p.curp || consentimientoForm.value.curpBusqueda,
      folio: p.cupe || p.folio || 'Folio: ADM-2026-8812',
      cama: 'Cama Asignada: 304-B (Admisión)',
      regimen: p.regimen || 'IMSS Ordinario Activo'
    }
  }
}

watch(() => props.pacienteSeleccionado, () => sincronizarPacienteConsentimiento())
onMounted(() => sincronizarPacienteConsentimiento())

const historialConsentimientosExpediente = ref([
  { documento: 'Aviso Privacidad Integral', fecha: '17/Oct/2026', estado: 'Vigente', hash: '0e4f...b781a' },
  { documento: 'Consentimiento Quirúrgico (Previo)', fecha: '14/Mar/2025', estado: 'Archivado', hash: '4b20...a810f' },
  { documento: 'Transfusión Sanguínea', fecha: '14/Mar/2025', estado: 'Archivado', hash: '1c09...03d2e' }
])

const buscarPacienteParaConsentimiento = () => {
  if (!consentimientoForm.value.curpBusqueda) {
    mostrarAlerta('Ingrese la CURP o Folio de Ingreso Clínico.')
    return
  }
  consentimientoForm.value.pacienteVinculado = {
    nombre: 'Martínez Álvarez, Marco Ant...',
    edad: 36,
    sexo: 'Masculino',
    curp: consentimientoForm.value.curpBusqueda.toUpperCase(),
    folio: 'Folio: ADM-2026-8812',
    cama: 'Cama Asignada: 304-B (Medicina Interna)',
    regimen: 'IMSS Ordinario Activo'
  }
}

const guardarConsentimientoLegal = () => {
  if (!consentimientoForm.value.pacienteVinculado) {
    mostrarAlerta('Debe vincular un paciente antes de formalizar el documento legal.')
    return
  }
  if (!consentimientoForm.value.nombreOtorgante) {
    mostrarAlerta('Ingrese el Nombre Completo del Otorgante o Apoderado Legal.')
    return
  }
  if (!consentimientoForm.value.declaracionAceptada) {
    mostrarAlerta('Debe marcar la casilla de aceptación bajo protesta de decir verdad.')
    return
  }
  mostrarAlerta('<svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Consentimiento formalizado con sellado de tiempo criptográfico SHA-256 e incorporado al expediente clínico institucional.')
}
</script>

<template>
  <div class="view-consentimiento-container">
    
    <!-- Encabezado de Consentimiento -->
    <div class="section-top-banner">
      <div class="banner-text-col">
        <div class="badges-row">
          <span class="cert-pill blue"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> VALIDACIÓN BIOMÉTRICA ACTIVA</span>
          <span class="cert-pill gray"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="M7 21h10"/><path d="M12 3v18"/><path d="M3 7h18"/></svg> NOM-004-SSA3-2012 / NOM-024</span>
          <span class="cert-pill gray"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="M7 21h10"/><path d="M12 3v18"/><path d="M3 7h18"/></svg> LGPDPPSO Art. 21</span>
        </div>
        <h2>Gestión y Registro de Consentimientos Informados y Aviso de Privacidad</h2>
        <p>Formalización legal inmediata con sellado de tiempo criptográfico SHA-256 para expediente clínico hospitalario.</p>
      </div>

      <div class="banner-right-kpis-row">
        <!-- KPI 1: Estatus Legal -->
        <div class="kpi-card-white">
          <div class="kpi-icon-square blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>
            </svg>
          </div>
          <div class="kpi-text">
            <small>ESTATUS LEGAL</small>
            <strong>3 / 4 <span class="subtle-blue">Completados</span></strong>
          </div>
        </div>

        <!-- KPI 2: Hardware Biométrico -->
        <div class="kpi-card-white">
          <div class="kpi-icon-square cyan">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M17.81 4.47c-.08 0-.16-.02-.23-.06C15.66 3.42 14 3 12.01 3c-1.98 0-3.86.47-5.57 1.41-.24.13-.54.04-.68-.2-.13-.24-.04-.55.2-.68C7.82 2.52 9.86 2 12.01 2c2.13 0 3.99.47 6.03 1.52.25.13.34.43.21.67-.09.18-.26.28-.44.28zM3.5 9.72c-.1 0-.2-.03-.29-.09-.23-.16-.28-.47-.12-.7.99-1.4 2.25-2.5 3.75-3.27C9.9 4.04 14 4.03 17.15 5.65c1.5.77 2.76 1.86 3.75 3.25.16.22.11.54-.12.7-.23.16-.54.11-.7-.12-.9-1.26-2.04-2.25-3.39-2.94-2.87-1.47-6.54-1.47-9.4.01-1.36.7-2.5 1.7-3.4 2.96-.08.14-.23.21-.39.21zm6.25 12.07c-.13 0-.26-.05-.35-.15-.87-.87-1.34-1.43-2.01-2.64-.69-1.23-1.05-2.73-1.05-4.34 0-2.97 2.54-5.39 5.66-5.39s5.66 2.42 5.66 5.39c0 .28-.22.5-.5.5s-.5-.22-.5-.5c0-2.42-2.09-4.39-4.66-4.39-2.57 0-4.66 1.97-4.66 4.39 0 1.44.32 2.77.93 3.85.64 1.15 1.08 1.64 1.85 2.42.2.2.2.51 0 .71-.11.1-.24.16-.37.16zm9.2-3.39c-.11 0-.21-.04-.3-.11-.22-.17-.26-.48-.09-.7 1.05-1.38 1.61-3.04 1.61-4.81 0-3.97-3.44-7.2-7.66-7.2-4.23 0-7.66 3.23-7.66 7.2 0 1.77.56 3.43 1.61 4.81.17.22.13.53-.09.7-.22.17-.53.13-.7-.09-1.2-1.58-1.83-3.47-1.83-5.42 0-4.52 3.91-8.2 8.66-8.2 4.75 0 8.66 3.68 8.66 8.2 0 1.95-.63 3.84-1.83 5.42-.09.12-.23.19-.38.19z"/>
            </svg>
          </div>
          <div class="kpi-text">
            <small>HARDWARE BIOMÉTRICO</small>
            <strong class="text-blue">● SecuGen U20 Listo</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- Cuadrícula Principal de Consentimientos (Izquierda Formulario + Derecha Resumen Legal) -->
    <div class="curp-layout-grid">
      
      <!-- SUBCOLUMNA IZQUIERDA -->
      <div class="curp-left-col">
        
        <!-- 1. Búsqueda y Vinculación de Paciente -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-num-title">
              <span class="step-header-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><line x1="15" y1="8" x2="17" y2="8"/><line x1="15" y1="12" x2="17" y2="12"/><line x1="7" y1="16" x2="17" y2="16"/></svg></span>
              <h4>1. Búsqueda y Vinculación de Paciente</h4>
            </div>
            <span class="step-label-muted">PASO OBLIGATORIO</span>
          </div>

          <div class="step-body">
            <label class="field-label-sm">Búsqueda por CURP o Folio de Ingreso Clínico</label>
            <div class="search-input-action-row">
              <div class="curp-field-wrapper">
                <span class="field-prefix"><svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
                <input 
                  type="text" 
                  v-model="consentimientoForm.curpBusqueda" 
                  placeholder="ALMN880415HDFRRL09"
                  class="curp-input"
                  @keyup.enter="buscarPacienteParaConsentimiento"
                />
              </div>
              <button type="button" class="btn-renapo-dark" @click="buscarPacienteParaConsentimiento">
                <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z"/><path d="M7 21h10"/><path d="M12 3v18"/><path d="M3 7h18"/></svg></span>
                <span>Validar RENAPO</span>
              </button>
            </div>

            <!-- Tarjeta de Paciente Vinculado -->
            <div v-if="consentimientoForm.pacienteVinculado" class="linked-patient-banner">
              <div class="patient-square-avatar">MA</div>
              <div class="linked-patient-info">
                <div class="linked-patient-header">
                  <strong>{{ consentimientoForm.pacienteVinculado.nombre }}</strong>
                  <span class="folio-tag">{{ consentimientoForm.pacienteVinculado.folio }}</span>
                </div>
                <div class="linked-patient-meta">
                  <span>{{ consentimientoForm.pacienteVinculado.edad }} años · {{ consentimientoForm.pacienteVinculado.sexo }}</span>
                  <span class="sep">•</span>
                  <span>CURP: {{ consentimientoForm.pacienteVinculado.curp }}</span>
                </div>
                <span class="bed-location-tag">{{ consentimientoForm.pacienteVinculado.cama }}</span>
              </div>
              <span class="imss-active-pill"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> {{ consentimientoForm.pacienteVinculado.regimen }}</span>
            </div>
          </div>
        </div>

        <!-- 2. Identificación del Declarante u Otorgante -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-num-title">
              <span class="step-header-icon"><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
              <h4>2. Identificación del Declarante u Otorgante</h4>
            </div>
            <span class="step-label-blue">Titular o Apoderado Legal</span>
          </div>

          <div class="step-body">
            <div class="form-grid-3">
              <div class="field-group">
                <label>Nombre Completo del Otorgante</label>
                <input 
                  type="text" 
                  v-model="consentimientoForm.nombreOtorgante" 
                  placeholder="Elena García Rivas" 
                  class="form-input" 
                />
              </div>
              <div class="field-group">
                <label>Parentesco / Relación</label>
                <select v-model="consentimientoForm.parentesco" class="form-select">
                  <option>Cónyuge</option>
                  <option>Titular (Mismo Paciente)</option>
                  <option>Hijo/a</option>
                  <option>Padre / Madre</option>
                  <option>Tutor / Representante Legal</option>
                </select>
              </div>
              <div class="field-group">
                <label>Identificación Oficial</label>
                <select v-model="consentimientoForm.identificacionOficial" class="form-select">
                  <option>INE / Credencial Elector</option>
                  <option>Pasaporte Mexicano</option>
                  <option>Cédula Profesional</option>
                  <option>Cartilla Militar</option>
                </select>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field-group">
                <label>Folio de Identificación (IDMEX / OCR / Pasaporte)</label>
                <input 
                  type="text" 
                  v-model="consentimientoForm.folioIdentificacion" 
                  placeholder="IDMEX1984329412<<8829" 
                  class="form-input" 
                />
              </div>
              <div class="field-group">
                <label>Teléfono Celular de Notificación Inmediata</label>
                <input 
                  type="tel" 
                  v-model="consentimientoForm.telefonoCelular" 
                  placeholder="+52 55 4910 2094" 
                  class="form-input" 
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 3. Selección del Documento Legal a Formalizar -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-num-title">
              <span class="step-header-icon"><svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
              <h4>3. Selección del Documento Legal a Formalizar</h4>
            </div>
            <span class="step-label-muted">Seleccione uno para proyectar</span>
          </div>

          <div class="step-body">
            <div class="docs-selector-grid">
              <!-- Doc 1: General de Admisión (Activo) -->
              <div 
                class="doc-legal-card"
                :class="{ selected: consentimientoForm.documentoSeleccionado === 'admision' }"
                @click="consentimientoForm.documentoSeleccionado = 'admision'"
              >
                <div class="doc-header-row">
                  <div class="doc-title-icon">
                    <span class="doc-icon-blue"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M2 12h20M4.93 4.93l14.14 14.14M4.93 19.07l14.14-14.14"/></svg></span>
                    <strong>Consentimiento General de Admisión</strong>
                  </div>
                  <span class="badge-status-doc active-pill">Activo</span>
                </div>
                <p>Tratamiento hospitalario ordinario, administración de fármacos intravenosos, monitorización y cuidados de enfermería.</p>
              </div>

              <!-- Doc 2: Aviso de Privacidad Integral -->
              <div 
                class="doc-legal-card"
                :class="{ selected: consentimientoForm.documentoSeleccionado === 'privacidad' }"
                @click="consentimientoForm.documentoSeleccionado = 'privacidad'"
              >
                <div class="doc-header-row">
                  <div class="doc-title-icon">
                    <span class="doc-icon-gray"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                    <strong>Aviso de Privacidad Integral</strong>
                  </div>
                  <span class="badge-status-doc signed-pill">Firmado</span>
                </div>
                <p>Protección de datos personales sensibles y autorización de expediente digital según NOM-024.</p>
              </div>

              <!-- Doc 3: Procedimientos Menores -->
              <div 
                class="doc-legal-card"
                :class="{ selected: consentimientoForm.documentoSeleccionado === 'procedimientos' }"
                @click="consentimientoForm.documentoSeleccionado = 'procedimientos'"
              >
                <div class="doc-header-row">
                  <div class="doc-title-icon">
                    <span class="doc-icon-red"><svg viewBox="0 0 24 24" class="svg-inline-syringe" fill="none" stroke="currentColor" stroke-width="2"><path d="m18 2 4 4"/><path d="m17 7 3-3"/><path d="M19 9 8.7 19.3c-1 1-2.5 1-3.4 0l-.6-.6c-1-1-1-2.5 0-3.4L15 5"/><path d="m9 11 4 4"/><path d="m5 19-3 3"/></svg></span>
                    <strong>Procedimientos Menores y Muestras</strong>
                  </div>
                  <span class="badge-status-doc pending-pill">Pendiente</span>
                </div>
                <p>Toma de muestras biológicas, inserción de catéter venoso periférico y pruebas diagnósticas radiológicas.</p>
              </div>

              <!-- Doc 4: Designación de Informados -->
              <div 
                class="doc-legal-card"
                :class="{ selected: consentimientoForm.documentoSeleccionado === 'informados' }"
                @click="consentimientoForm.documentoSeleccionado = 'informados'"
              >
                <div class="doc-header-row">
                  <div class="doc-title-icon">
                    <span class="doc-icon-red"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><line x1="15" y1="8" x2="17" y2="8"/><line x1="15" y1="12" x2="17" y2="12"/><line x1="7" y1="16" x2="17" y2="16"/></svg></span>
                    <strong>Designación de Informados</strong>
                  </div>
                  <span class="badge-status-doc pending-pill">Pendiente</span>
                </div>
                <p>Registro explícito de personas autorizadas legalmente para recibir partes médicos y diagnósticos reservados.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. Visualizador Legal y Zona de Firma Biométrica -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-num-title">
              <span class="step-header-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/></svg></span>
              <h4>4. Visualizador Legal y Zona de Firma Biométrica</h4>
            </div>
            <div class="actions-right-stack">
              <button type="button" class="btn-subtle-reader" @click="mostrarAlerta('Ampliando visualizador de documento a pantalla completa...')">
                <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Ampliar
              </button>
              <button type="button" class="btn-subtle-reader" @click="mostrarAlerta('Generando vista previa certificada para el otorgante...')">
                <svg viewBox="0 0 24 24" class="svg-inline-eye" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg> Vista Previa
              </button>
            </div>
          </div>

          <div class="step-body">
            <!-- Cuadro de Texto Legal Conforme a NOM-004 -->
            <div class="legal-document-viewer">
              <h5>HOSPITAL VITAE — CONSENTIMIENTO INFORMADO GENERAL DE ADMISIÓN</h5>
              <p>
                En la Ciudad de México, siendo las 10:50 horas del día 17 de octubre de 2026, el(la) suscrito(a), plenamente consciente y en ejercicio de mis facultades legales o en representación fidedigna del paciente <strong>MARCO ANTONIO MARTÍNEZ ÁLVAREZ</strong>, con fundamento en lo establecido por la <strong>Ley General de Salud</strong>, su Reglamento en Materia de Prestación de Servicios de Atención Médica, y la <strong>Norma Oficial Mexicana NOM-004-SSA3-2012</strong> del Expediente Clínico:
              </p>
              <p>
                <strong>PRIMERA (Autorización de Procedimientos):</strong> Otorgo mi pleno consentimiento al personal facultativo de Hospital Vitae para realizar los actos de diagnóstico clínico, vigilancia médica, administración terapéutica de medicamentos prescritos y cuidados de soporte vital pertinentes.
              </p>
              <p>
                <strong>SEGUNDA (Información sobre Riesgos):</strong> Manifiesto haber sido enterado(a) comprensiblemente de los beneficios y posibles contingencias inherentes al tratamiento hospitalario.
              </p>
            </div>

            <!-- Checkbox Legal Obligatorio -->
            <div class="consent-check-row">
              <input 
                type="checkbox" 
                id="check-protesta"
                v-model="consentimientoForm.declaracionAceptada" 
              />
              <label for="check-protesta">
                Declaro bajo protesta de decir verdad que he leído íntegramente las cláusulas anteriores, recibí aclaración satisfactoria a todas mis dudas por parte del personal facultativo de Hospital Vitae y acepto los términos del consentimiento informado en representación del paciente.
              </label>
            </div>

            <!-- Bloque de Doble Captura Biométrica (Firma Táctil + Huella SecuGen) -->
            <div class="biometrics-dual-grid">
              
              <!-- Lado Izquierdo: Trazo Digital / Firma en Pantalla -->
              <div class="biometric-box-card">
                <div class="bio-header">
                  <div class="bio-title">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/></svg></span>
                    <strong>Trazo Digital / Táctil</strong>
                  </div>
                  <span class="bio-tag-blue">Pantalla Táctil Activa</span>
                </div>

                <div class="signature-canvas-mock">
                  <span class="mock-ts">TS: 2026-10-17 10:52:11 CST</span>
                  <svg viewBox="0 0 320 70" class="signature-svg">
                    <path d="M 20,45 Q 40,15 65,45 T 110,40 T 150,25 T 190,50 T 240,35 T 290,45" fill="none" stroke="#0F2D4A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span class="sign-line-text">FIRME SOBRE LA LÍNEA</span>
                </div>

                <div class="bio-footer-actions">
                  <button type="button" class="btn-bio-clean" @click="mostrarAlerta('Lienzo de firma táctil limpiado.')">
                    <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Limpiar Trazo
                  </button>
                  <button type="button" class="btn-bio-verified" @click="mostrarAlerta('Trazo biométrico validado con 128 puntos de presión.')">
                    <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Firma Verificada
                  </button>
                </div>
              </div>

              <!-- Lado Derecho: Sensor Biométrico Dactilar -->
              <div class="biometric-box-card">
                <div class="bio-header">
                  <div class="bio-title">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v10"/><path d="M12 6a4 4 0 0 1 4 4v7a4 4 0 0 1-8 0v-7a4 4 0 0 1 4-4z"/></svg></span>
                    <strong>Biometría Dactilar</strong>
                  </div>
                  <span class="bio-tag-blue">● Sensor Calibrado</span>
                </div>

                <div class="fingerprint-scan-box">
                  <div class="fingerprint-icon-box">
                    <svg viewBox="0 0 24 24" width="32" height="32" fill="#2563EB">
                      <path d="M17.81 4.47c-.08 0-.16-.02-.23-.06C15.66 3.42 14 3 12.01 3c-1.98 0-3.86.47-5.57 1.41-.24.13-.54.04-.68-.2-.13-.24-.04-.55.2-.68C7.82 2.52 9.86 2 12.01 2c2.13 0 3.99.47 6.03 1.52.25.13.34.43.21.67-.09.18-.26.28-.44.28zM3.5 9.72c-.1 0-.2-.03-.29-.09-.23-.16-.28-.47-.12-.7.99-1.4 2.25-2.5 3.75-3.27C9.9 4.04 14 4.03 17.15 5.65c1.5.77 2.76 1.86 3.75 3.25.16.22.11.54-.12.7-.23.16-.54.11-.7-.12-.9-1.26-2.04-2.25-3.39-2.94-2.87-1.47-6.54-1.47-9.4.01-1.36.7-2.5 1.7-3.4 2.96-.08.14-.23.21-.39.21zm6.25 12.07c-.13 0-.26-.05-.35-.15-.87-.87-1.34-1.43-2.01-2.64-.69-1.23-1.05-2.73-1.05-4.34 0-2.97 2.54-5.39 5.66-5.39s5.66 2.42 5.66 5.39c0 .28-.22.5-.5.5s-.5-.22-.5-.5c0-2.42-2.09-4.39-4.66-4.39-2.57 0-4.66 1.97-4.66 4.39 0 1.44.32 2.77.93 3.85.64 1.15 1.08 1.64 1.85 2.42.2.2.2.51 0 .71-.11.1-.24.16-.37.16zm9.2-3.39c-.11 0-.21-.04-.3-.11-.22-.17-.26-.48-.09-.7 1.05-1.38 1.61-3.04 1.61-4.81 0-3.97-3.44-7.2-7.66-7.2-4.23 0-7.66 3.23-7.66 7.2 0 1.77.56 3.43 1.61 4.81.17.22.13.53-.09.7-.22.17-.53.13-.7-.09-1.2-1.58-1.83-3.47-1.83-5.42 0-4.52 3.91-8.2 8.66-8.2 4.75 0 8.66 3.68 8.66 8.2 0 1.95-.63 3.84-1.83 5.42-.09.12-.23.19-.38.19z"/>
                    </svg>
                  </div>
                  <strong class="finger-title">Huella Registrada: Pulgar Derecho</strong>
                  <small class="finger-quality">Calidad de Coincidencia: <strong>98.4% (ANSI/NIST)</strong></small>
                </div>

                <div class="bio-footer-actions">
                  <button type="button" class="btn-bio-clean" @click="mostrarAlerta('Sensor biométrico reiniciado para nueva captura.')">
                    <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Recapturar
                  </button>
                  <button type="button" class="btn-bio-token" @click="mostrarAlerta('Token criptográfico generado: SHA-256 #8A20.')">
                    <svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg> Token SHA: #8A20
                  </button>
                </div>
              </div>

            </div>
          </div>
        </div>

      </div>

      <!-- SUBCOLUMNA DERECHA: Acción Legal, Estatus, Testigo e Historial -->
      <aside class="curp-right-col">
        
        <!-- 1. Acción Legal Inmediata -->
        <div class="side-widget-card action-widget">
          <small class="side-widget-header-title">ACCIÓN LEGAL INMEDIATA</small>

          <div class="actions-stack-legal">
            <button type="button" class="btn-save-consent" @click="guardarConsentimientoLegal">
              <span class="btn-save-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/></svg></span>
              <span>Guardar Consentimiento [Enter]</span>
            </button>
            <button type="button" class="btn-download-pdf" @click="mostrarAlerta('Generando PDF foliado del consentimiento con código de barras y firma digital...')">
              <span><svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
              <span>Descargar Copia Foliada en PDF</span>
            </button>

            <div class="notify-email-row">
              <label>Notificar al Correo del Otorgante</label>
              <input 
                type="email" 
                v-model="consentimientoForm.correoNotificacion" 
                placeholder="elena.garcia@email.com" 
                class="form-input email-input-flat" 
              />
            </div>
          </div>
        </div>

        <!-- 2. Estatus de Admisión Legal -->
        <div class="side-widget-card">
          <div class="widget-header">
            <strong class="widget-title-md">Estatus de Admisión Legal</strong>
            <span class="badge-progress-dark">75% Completo</span>
          </div>

          <div class="occ-progress-track">
            <div class="occ-fill" style="width: 75%;"></div>
          </div>

          <div class="legal-check-list">
            <div class="legal-check-item">
              <div class="check-item-left">
                <span class="icon-circle check"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
                <span>Identificación de Paciente</span>
              </div>
              <span class="status-label blue">Validado</span>
            </div>

            <div class="legal-check-item">
              <div class="check-item-left">
                <span class="icon-circle check"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
                <span>Aviso de Privacidad</span>
              </div>
              <span class="status-label blue">Firmado</span>
            </div>

            <div class="legal-check-item active">
              <div class="check-item-left">
                <span class="icon-circle current">⋯</span>
                <strong>Admisión Hospitalaria</strong>
              </div>
              <span class="status-label blue font-bold">En captura</span>
            </div>

            <div class="legal-check-item pending">
              <div class="check-item-left">
                <span class="icon-circle pending">○</span>
                <span>Procedimientos Menores</span>
              </div>
              <span class="status-label gray">Pendiente</span>
            </div>
          </div>
        </div>

        <!-- 3. Personal Testigo del Proceso -->
        <div class="side-widget-card witness-card">
          <small class="witness-header">PERSONAL TESTIGO DEL PROCESO</small>

          <div class="witness-profile-row">
            <div class="avatar-circle">LA</div>
            <div class="witness-info">
              <div class="witness-name-check">
                <strong>{{ recepcionista.nombre }}</strong>
                <span class="check-shield-blue"><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
              </div>
              <small>Céd. Admisión: {{ recepcionista.cedula }}</small>
            </div>
          </div>

          <div class="audit-specs-list">
            <div class="spec-row">
              <span>Terminal:</span>
              <code>ADM-DESK-04 (IP: 10.14.2.88)</code>
            </div>
            <div class="spec-row">
              <span>Certificado:</span>
              <code>AC-SSA-2026-VAL</code>
            </div>
            <div class="spec-row">
              <span>Algoritmo:</span>
              <code>ECDSA P-256 / SHA-256</code>
            </div>
          </div>
        </div>

        <!-- 4. Historial en Expediente -->
        <div class="side-widget-card">
          <div class="widget-header">
            <strong class="widget-title-md">Historial en Expediente</strong>
            <span class="text-subtle">3 registros</span>
          </div>

          <div class="history-records-list">
            <div v-for="(rec, idx) in historialConsentimientosExpediente" :key="idx" class="history-record-item">
              <div class="record-top">
                <strong>{{ rec.documento }}</strong>
                <span class="record-date-badge" :class="{ active: rec.estado === 'Vigente' }">
                  {{ rec.fecha }} <span v-if="rec.estado === 'Vigente'">Vigente</span><span v-else>Archivado</span>
                </span>
              </div>
              <small class="hash-code">HASH: {{ rec.hash }}</small>
            </div>
          </div>
        </div>

      </aside>
    </div>

  </div>
</template>

<style scoped>
/* =========================================================================
   CONTENEDOR GENERAL Y ENCABEZADO SUPERIOR
   ========================================================================= */
.view-consentimiento-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.cert-pill.blue {
  background-color: #EFF6FF;
  color: #1D4ED8;
  border: 1px solid #BFDBFE;
}

.cert-pill.gray {
  background-color: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
}

.section-top-banner h2 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0.25rem 0;
}

.section-top-banner p {
  font-size: 0.75rem;
  color: #64748B;
  margin: 0;
  line-height: 1.35;
}

.banner-right-kpis-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.kpi-card-white {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.45rem 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.kpi-icon-square {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-icon-square.blue {
  background-color: #EFF6FF;
  color: #2563EB;
}

.kpi-icon-square.cyan {
  background-color: #ECFEFF;
  color: #0284C7;
}

.kpi-text small {
  display: block;
  font-size: 0.6rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
}

.kpi-text strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.subtle-blue {
  color: #2563EB;
  font-weight: 600;
}

.text-blue {
  color: #2563EB;
}

/* =========================================================================
   GRID PRINCIPAL
   ========================================================================= */
.curp-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
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

/* =========================================================================
   TARJETAS DE PASO (STEP CARD)
   ========================================================================= */
.step-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.step-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1.15rem;
  background-color: #FFFFFF;
  border-bottom: 1px solid #F1F5F9;
}

.step-num-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.step-header-icon {
  font-size: 1.1rem;
}

.step-num-title h4 {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0;
}

.step-label-muted {
  font-size: 0.65rem;
  font-weight: 700;
  color: #94A3B8;
  letter-spacing: 0.05em;
}

.step-label-blue {
  font-size: 0.68rem;
  font-weight: 700;
  color: #2563EB;
}

.step-body {
  padding: 1rem 1.15rem;
}

/* =========================================================================
   PASO 1: BÚSQUEDA Y VINCULACIÓN
   ========================================================================= */
.field-label-sm {
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.4rem;
  display: block;
}

.search-input-action-row {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

.curp-field-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.field-prefix {
  position: absolute;
  left: 0.85rem;
  font-size: 0.9rem;
  color: #94A3B8;
}

.curp-input {
  width: 100%;
  border: 1.5px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.65rem 0.85rem 0.65rem 2.4rem;
  font-size: 0.9rem;
  font-weight: 700;
  font-family: monospace;
  color: #0F2D4A;
  outline: none;
  background-color: #FFFFFF;
  transition: border-color 0.15s;
}

.curp-input:focus {
  border-color: #2563EB;
}

.btn-renapo-dark {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1.2rem;
  font-size: 0.78rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s;
}

.btn-renapo-dark:hover {
  background-color: #1E3A8A;
}

.linked-patient-banner {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-top: 0.75rem;
  flex-wrap: wrap;
}

.patient-square-avatar {
  width: 44px;
  height: 44px;
  background-color: #DBEAFE;
  color: #1D4ED8;
  font-weight: 800;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
}

.linked-patient-info {
  flex: 1;
  min-width: 250px;
}

.linked-patient-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.linked-patient-header strong {
  font-size: 0.85rem;
  font-weight: 800;
  color: #1E293B;
}

.folio-tag {
  font-family: monospace;
  font-size: 0.68rem;
  font-weight: 700;
  color: #2563EB;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
}

.linked-patient-meta {
  font-size: 0.72rem;
  color: #64748B;
  display: flex;
  gap: 0.4rem;
  align-items: center;
  margin-top: 0.15rem;
}

.sep {
  color: #CBD5E1;
}

.bed-location-tag {
  display: block;
  font-size: 0.72rem;
  color: #2563EB;
  font-weight: 600;
  margin-top: 0.2rem;
}

.imss-active-pill {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 6px;
  white-space: nowrap;
}

/* =========================================================================
   PASO 2: DECLARANTE U OTORGANTE
   ========================================================================= */
.form-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.85rem;
  margin-bottom: 0.85rem;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-group label {
  font-size: 0.72rem;
  font-weight: 600;
  color: #334155;
}

.form-input, .form-select {
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem 0.75rem;
  font-size: 0.78rem;
  color: #1E293B;
  background-color: #F8FAFC;
  outline: none;
  transition: all 0.15s;
}

.form-input:focus, .form-select:focus {
  background-color: #FFFFFF;
  border-color: #2563EB;
}

/* =========================================================================
   PASO 3: SELECCIÓN DE DOCUMENTO
   ========================================================================= */
.docs-selector-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.doc-legal-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.doc-legal-card:hover {
  border-color: #CBD5E1;
}

.doc-legal-card.selected {
  border: 2px solid #2563EB;
  background-color: #FFFFFF;
  box-shadow: 0 0 0 1px #2563EB;
}

.doc-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-title-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.doc-title-icon strong {
  font-size: 0.78rem;
  color: #0F2D4A;
  font-weight: 800;
}

.doc-legal-card p {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0;
  line-height: 1.4;
}

.badge-status-doc {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.badge-status-doc.active-pill {
  background-color: #2563EB;
  color: #FFFFFF;
}

.badge-status-doc.signed-pill {
  background-color: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
}

.badge-status-doc.pending-pill {
  background-color: #FEE2E2;
  color: #DC2626;
  border: 1px solid #FECACA;
}

/* =========================================================================
   PASO 4: VISUALIZADOR Y FIRMAS BIOMÉTRICAS
   ========================================================================= */
.actions-right-stack {
  display: flex;
  gap: 0.4rem;
}

.btn-subtle-reader {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-subtle-reader:hover {
  background-color: #F8FAFC;
}

.legal-document-viewer {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 1rem 1.15rem;
  max-height: 160px;
  overflow-y: auto;
  font-size: 0.72rem;
  line-height: 1.45;
  color: #334155;
}

.legal-document-viewer h5 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0 0 0.5rem 0;
}

.legal-document-viewer p {
  margin: 0 0 0.5rem 0;
}

.consent-check-row {
  display: flex;
  gap: 0.55rem;
  align-items: flex-start;
  margin: 0.75rem 0;
  font-size: 0.72rem;
  color: #1E293B;
  line-height: 1.35;
}

.consent-check-row input {
  margin-top: 0.15rem;
  accent-color: #2563EB;
  width: 16px;
  height: 16px;
}

.biometrics-dual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.biometric-box-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.bio-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bio-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.bio-title strong {
  font-size: 0.75rem;
  color: #0F2D4A;
  font-weight: 800;
}

.bio-tag-blue {
  font-size: 0.65rem;
  font-weight: 700;
  color: #2563EB;
}

.signature-canvas-mock {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  height: 105px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem;
}

.mock-ts {
  align-self: flex-start;
  font-size: 0.58rem;
  color: #94A3B8;
  font-family: monospace;
}

.signature-svg {
  width: 85%;
  height: 55px;
}

.sign-line-text {
  font-size: 0.6rem;
  color: #94A3B8;
  border-top: 1px solid #E2E8F0;
  width: 100%;
  text-align: center;
  padding-top: 0.15rem;
}

.fingerprint-scan-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  height: 105px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.4rem;
}

.fingerprint-icon-box {
  width: 44px;
  height: 44px;
  background-color: #EFF6FF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.finger-title {
  font-size: 0.72rem;
  color: #0F2D4A;
  font-weight: 800;
}

.finger-quality {
  font-size: 0.65rem;
  color: #64748B;
}

.bio-footer-actions {
  display: flex;
  gap: 0.4rem;
}

.btn-bio-clean {
  flex: 1;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.4rem 0.5rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-bio-verified {
  flex: 1;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 0.4rem 0.5rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: #1D4ED8;
  cursor: pointer;
}

.btn-bio-token {
  flex: 1;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 0.4rem 0.5rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: #1D4ED8;
  cursor: pointer;
}

/* =========================================================================
   SUBCOLUMNA DERECHA (WIDGETS)
   ========================================================================= */
.side-widget-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.side-widget-header-title {
  font-size: 0.65rem;
  font-weight: 800;
  color: #0F2D4A;
  letter-spacing: 0.05em;
  display: block;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.widget-title-md {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0F2D4A;
}

.actions-stack-legal {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.btn-save-consent {
  width: 100%;
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 0.82rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
  transition: all 0.15s;
}

.btn-save-consent:hover {
  background-color: #1D4ED8;
}

.btn-download-pdf {
  width: 100%;
  background-color: #FFFFFF;
  color: #334155;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.65rem 1rem;
  font-size: 0.78rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download-pdf:hover {
  background-color: #F8FAFC;
}

.notify-email-row {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-top: 0.2rem;
}

.notify-email-row label {
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 600;
}

.email-input-flat {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
}

.badge-progress-dark {
  background-color: #0F4C81;
  color: #FFFFFF;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
}

.occ-progress-track {
  width: 100%;
  height: 6px;
  background-color: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.occ-fill {
  height: 100%;
  background-color: #2563EB;
}

.legal-check-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.legal-check-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.72rem;
  color: #64748B;
}

.legal-check-item.active {
  color: #1E293B;
}

.check-item-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.icon-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
}

.icon-circle.check {
  background-color: #2563EB;
  color: #FFFFFF;
  font-weight: 800;
}

.icon-circle.current {
  background-color: #2563EB;
  color: #FFFFFF;
  font-size: 0.8rem;
  line-height: 1;
}

.icon-circle.pending {
  border: 1px solid #CBD5E1;
  color: #94A3B8;
}

.status-label.blue {
  color: #2563EB;
  font-weight: 600;
  font-size: 0.7rem;
}

.status-label.gray {
  color: #94A3B8;
  font-size: 0.7rem;
}

.font-bold {
  font-weight: 800;
}

.witness-card {
  gap: 0.6rem;
}

.witness-header {
  font-size: 0.62rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
  display: block;
}

.witness-profile-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  background-color: #1E293B;
  color: #FFFFFF;
  font-weight: 700;
  font-size: 0.8rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.witness-info {
  display: flex;
  flex-direction: column;
}

.witness-name-check {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.witness-name-check strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.check-shield-blue {
  color: #2563EB;
  font-size: 0.75rem;
}

.witness-info small {
  font-size: 0.65rem;
  color: #64748B;
}

.audit-specs-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.65rem;
  color: #64748B;
  border-top: 1px solid #F1F5F9;
  padding-top: 0.5rem;
}

.spec-row {
  display: flex;
  justify-content: space-between;
}

.spec-row code {
  font-family: monospace;
  color: #0F2D4A;
  font-weight: 600;
}

.text-subtle {
  font-size: 0.68rem;
  color: #64748B;
}

.history-records-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.history-record-item {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
}

.record-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.72rem;
}

.record-top strong {
  color: #1E293B;
}

.record-date-badge {
  font-size: 0.65rem;
  color: #64748B;
}

.record-date-badge.active {
  color: #2563EB;
  font-weight: 700;
}

.hash-code {
  display: block;
  font-family: monospace;
  font-size: 0.62rem;
  color: #94A3B8;
  margin-top: 0.15rem;
}
</style>
