<script setup lang="ts">
import { ref, watch } from 'vue'
import type { EstudioDiagnostico, ArchivoResultado } from '../types'

const props = defineProps<{
  selectedOrder: EstudioDiagnostico | null
}>()

const emit = defineEmits<{
  (e: 'guardar-resultado', payload: { folio: string; archivo: ArchivoResultado; observaciones: string }): void
  (e: 'rechazar-muestra', payload: { folio: string; motivo: string }): void
}>()

// Estado reactivo del formulario
const folioInput = ref(props.selectedOrder?.folio || 'EST-2026-9041')
const prioridadSelect = ref(props.selectedOrder?.prioridad || 'STAT')
const categoriaSelect = ref(props.selectedOrder?.categoria || 'LABORATORIO')
const equipoAnalizador = ref(props.selectedOrder?.analizador_asignado || 'Cobas b221 - Gasómetro Crítico Urgencias')
const observaciones = ref('')
const tokenFirma = ref('••••••••••••••••')
const certificacionAceptada = ref(true)
const formatoTab = ref<'PDF' | 'DICOM' | 'IMG'>('PDF')
const isDragging = ref(false)
const notificationSuccess = ref('')
const errorMessage = ref('')

// Archivos cargados en cola
const stagedFiles = ref<Array<{ name: string; size: string; hash: string; type: string }>>([
  {
    name: 'INF_GAS_TROP_9041_COBAS.pdf',
    size: '1.4 MB',
    hash: '4fa9c182b8a93e198df4920bce1101ab648821ec39062fa883908c2b',
    type: 'PDF'
  }
])

watch(() => props.selectedOrder, (newVal) => {
  if (newVal) {
    folioInput.value = newVal.folio
    prioridadSelect.value = newVal.prioridad
    categoriaSelect.value = newVal.categoria
    if (newVal.analizador_asignado) {
      equipoAnalizador.value = newVal.analizador_asignado
    }
  }
}, { immediate: true })

const handleImportarCobas = () => {
  stagedFiles.value = [
    {
      name: `AUTO_COBAS_B221_${folioInput.value.replace(/[^a-zA-Z0-9]/g, '')}.pdf`,
      size: '1.2 MB',
      hash: 'e83d11ac72b1467406a092c4311059f338827fa112009cb987621a22',
      type: 'PDF'
    }
  ]
  observaciones.value = 'Telemetría importada directamente desde analizador Roche Cobas b221. Verificación automática de control de calidad Delta 0.02 superada.'
}

const handleRemoveFile = (index: number) => {
  stagedFiles.value.splice(index, 1)
}

const handleFirmarYNotificar = () => {
  if (!folioInput.value) {
    errorMessage.value = 'Debe especificar el folio de estudio a asociar.'
    return
  }
  if (!certificacionAceptada.value) {
    errorMessage.value = 'Debe aceptar la certificación normativa NOM-007-SSA3-2011.'
    return
  }
  if (stagedFiles.value.length === 0) {
    errorMessage.value = 'Debe cargar al menos un archivo o informe de resultados.'
    return
  }

  errorMessage.value = ''
  notificationSuccess.value = `¡Estudio ${folioInput.value} validado y transmitido con éxito al ECE de Urgencias!`

  const primerArchivo = stagedFiles.value[0]
  if (!primerArchivo) {
    errorMessage.value = 'Debe cargar al menos un archivo válido.'
    return
  }
  emit('guardar-resultado', {
    folio: folioInput.value,
    archivo: {
      nombre: primerArchivo.name,
      formato: formatoTab.value,
      tamano: primerArchivo.size,
      sha256: primerArchivo.hash,
      fecha_firma: '17/10/2026 - 10:48 CST',
      firmado_por: 'Q.F.B. Mariana Téllez Morales',
      ack_hl7: 'HL7 v2.5.1 ORU^R01 ACK_AA 200 OK'
    },
    observaciones: observaciones.value
  })

  setTimeout(() => {
    notificationSuccess.value = ''
  }, 4000)
}

const handleRechazo = () => {
  const motivo = prompt('Ingrese el motivo técnico de rechazo de muestra (ej. Muestra hemolizada, volumen insuficiente, coágulo con EDTA):')
  if (motivo) {
    emit('rechazar-muestra', { folio: folioInput.value, motivo })
  }
}
</script>

<template>
  <div class="lab-cargar-section">
    <!-- BANNER DE ENCABEZADO TÉCNICO -->
    <div class="header-technical-card">
      <div class="tech-card-left">
        <div class="tech-tag-row">
          <span class="badge-tech">MÓDULO TÉCNICO ESPECIALIZADO</span>
          <span class="sep">•</span>
          <span class="badge-tech-sub">
            <svg viewBox="0 0 24 24" class="tech-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            Canal Seguro SSL LIS-PACS HL7v2.8
          </span>
        </div>
        <h2 class="tech-title">Carga y Validación de Resultados Diagnósticos</h2>
        <p class="tech-desc">
          Subida técnica de informes analíticos certificados (PDF), series DICOM radiológicas y telemetría de analizador con sellado criptográfico normativo (NOM-024 / NOM-007).
        </p>
      </div>

      <div class="tech-card-right">
        <div class="peripheral-pill">
          <div class="dot-status green"></div>
          <div class="peripheral-info">
            <span class="p-title">Lector Óptico USB</span>
            <span class="p-sub">Activo [Auto-Capture]</span>
          </div>
        </div>

        <div class="peripheral-pill">
          <div class="dot-status green"></div>
          <div class="peripheral-info">
            <span class="p-title">Analizador Roche</span>
            <span class="p-sub">Cobas b221 Online</span>
          </div>
        </div>

        <div class="peripheral-pill">
          <div class="dot-status blue"></div>
          <div class="peripheral-info">
            <span class="p-title">Nodo PACS</span>
            <span class="p-sub">Sincronizado</span>
          </div>
        </div>
      </div>
    </div>

    <!-- NOTIFICACIÓN DE ÉXITO O ERROR -->
    <div v-if="notificationSuccess" class="alert-box success">
      <svg viewBox="0 0 24 24" class="alert-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>{{ notificationSuccess }}</span>
    </div>

    <div v-if="errorMessage" class="alert-box error">
      <svg viewBox="0 0 24 24" class="alert-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- LAYOUT PRINCIPAL DE CARGA: COLUMNA IZQUIERDA FORMULARIO + COLUMNA DERECHA CONTROL QA Y FIRMA -->
    <div class="upload-grid-container">
      <!-- COLUMNA PRINCIPAL DE TRABAJO (PASO 1 Y PASO 2) -->
      <div class="upload-main-col">
        <!-- PASO 1: ASOCIACIÓN TÉCNICA -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-number-circle">1</div>
            <div class="step-header-text">
              <h3>Asociación Técnica de Muestra u Orden</h3>
              <span class="step-sub">PASO 1 DE 2 • VINCULACIÓN AL REGISTRO</span>
            </div>
          </div>

          <div class="step-card-body">
            <div class="form-row-2col">
              <div class="form-group">
                <label class="form-lbl">
                  Folio de Solicitud / Código de Muestra *
                  <span class="hint-lbl">Escaneo o ingreso manual</span>
                </label>
                <div class="input-with-action">
                  <svg viewBox="0 0 24 24" class="input-icon" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2"/>
                    <line x1="7" y1="3" x2="7" y2="21"/>
                    <line x1="17" y1="3" x2="17" y2="21"/>
                  </svg>
                  <input
                    v-model="folioInput"
                    type="text"
                    placeholder="Ej. EST-2026-9041"
                    class="form-input"
                  />
                  <button type="button" class="btn-input-search" title="Buscar y asociar orden">
                    <svg viewBox="0 0 24 24" class="btn-mini-svg" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"/>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                    </svg>
                    <span>Buscar</span>
                  </button>
                </div>
              </div>

              <div class="form-group">
                <label class="form-lbl">Prioridad Operativa *</label>
                <select v-model="prioridadSelect" class="form-select">
                  <option value="STAT">CÓDIGO ROJO - STAT (Choque Inmediato)</option>
                  <option value="URGENTE">CÓDIGO NARANJA - Urgente</option>
                  <option value="ORDINARIA">ORDINARIA - Rutina Hospitalaria</option>
                </select>
              </div>
            </div>

            <div class="form-row-2col">
              <div class="form-group">
                <label class="form-lbl">Categoría y Servicio Diagnóstico *</label>
                <select v-model="categoriaSelect" class="form-select">
                  <option value="LABORATORIO">Laboratorio Clínico (Hematología, Bioquímica, Gases)</option>
                  <option value="IMAGENOLOGIA">Imagenología y Radiología Digital (Rayos X, TAC)</option>
                  <option value="GABINETE">Gabinete Especializado (ECG, Espirometría)</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-lbl">Equipo / Estación Analítica Emisora *</label>
                <input
                  v-model="equipoAnalizador"
                  type="text"
                  placeholder="Asignar estación procesadora..."
                  class="form-input"
                />
              </div>
            </div>

            <!-- CHECKBOX DE ESTADO NOM -->
            <div class="status-nom-box">
              <svg viewBox="0 0 24 24" class="nom-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <polyline points="9 12 11 14 15 10"/>
              </svg>
              <div class="nom-text">
                <strong>Estado de Asociación de Muestra: Vinculación Certificada</strong>
                <p>
                  El sistema verifica el código de barras unidireccional y prepara el canal seguro de subida. Conforme a las normas NOM-024-SSA3 y NOM-007-SSA3, este módulo no almacena ni expone el historial clínico general del paciente.
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- PASO 2: CARGA DE ARCHIVOS -->
        <div class="step-card">
          <div class="step-card-header">
            <div class="step-number-circle">2</div>
            <div class="step-header-text">
              <h3>Carga de Archivos Auxiliares y Series Diagnósticas</h3>
              <span class="step-sub">FORMATOS OFICIALES CERTIFICADOS</span>
            </div>
          </div>

          <div class="step-card-body">
            <!-- TABS DE FORMATO -->
            <div class="format-tabs">
              <button
                type="button"
                class="format-tab"
                :class="{ active: formatoTab === 'PDF' }"
                @click="formatoTab = 'PDF'"
              >
                <svg viewBox="0 0 24 24" class="tab-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                <span>Informe PDF Oficial Firmado</span>
              </button>

              <button
                type="button"
                class="format-tab"
                :class="{ active: formatoTab === 'DICOM' }"
                @click="formatoTab = 'DICOM'"
              >
                <svg viewBox="0 0 24 24" class="tab-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"/>
                  <line x1="7" y1="2" x2="7" y2="22"/>
                  <line x1="17" y1="2" x2="17" y2="22"/>
                  <line x1="2" y1="12" x2="22" y2="12"/>
                </svg>
                <span>Serie DICOM (.dcm / .zip)</span>
              </button>

              <button
                type="button"
                class="format-tab"
                :class="{ active: formatoTab === 'IMG' }"
                @click="formatoTab = 'IMG'"
              >
                <svg viewBox="0 0 24 24" class="tab-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span>Imágenes Clínicas (JPG / PNG HD)</span>
              </button>
            </div>

            <!-- ZONA DROPZONE -->
            <div
              class="dropzone-box"
              :class="{ dragging: isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="isDragging = false"
            >
              <div class="dropzone-icon-box">
                <svg viewBox="0 0 24 24" class="cloud-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
              </div>
              <h4 class="dropzone-title">Arrastre aquí los archivos generados por el analizador o examine en su equipo</h4>
              <p class="dropzone-sub">
                Soporta archivos generados directamente por terminales LIS/RIS: PDF con firmas vectoriales, series DICOM comprimidas, TIFF y exportaciones HL7 estructuradas.
              </p>

              <div class="dropzone-actions">
                <button type="button" class="btn-browse">
                  <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                  </svg>
                  <span>Explorar Archivos Locales</span>
                </button>

                <button type="button" class="btn-import-device" @click="handleImportarCobas">
                  <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  <span>Importar de Cobas b221</span>
                </button>
              </div>

              <span class="limit-caption">Límite de transferencia: 100 MB por lote • Encriptación AES-256 en tránsito</span>
            </div>

            <!-- CARACTERÍSTICAS TÉCNICAS (3 PILLS) -->
            <div class="tech-pills-row">
              <div class="tech-feature-pill">
                <div class="feature-dot blue"></div>
                <div class="feature-meta">
                  <strong>Checksum SHA-256</strong>
                  <span>Validación automática de integridad digital.</span>
                </div>
              </div>

              <div class="tech-feature-pill">
                <div class="feature-dot blue"></div>
                <div class="feature-meta">
                  <strong>Extracción OCR / HL7</strong>
                  <span>Lectura de valores analíticos en segundo plano.</span>
                </div>
              </div>

              <div class="tech-feature-pill">
                <div class="feature-dot blue"></div>
                <div class="feature-meta">
                  <strong>Anonimización DICOM</strong>
                  <span>Limpieza de metadatos clínicos (NOM-024).</span>
                </div>
              </div>
            </div>

            <!-- ARCHIVOS EN COLA DE ENVÍO -->
            <div class="staged-files-section">
              <div class="staged-header">
                <span class="staged-title">Archivos en Cola de Envío ({{ stagedFiles.length }})</span>
                <span class="staged-size">Total: 1.40 MB</span>
              </div>

              <div v-if="stagedFiles.length > 0" class="staged-files-list">
                <div v-for="(file, idx) in stagedFiles" :key="idx" class="staged-item">
                  <div class="file-type-badge">{{ file.type }}</div>
                  <div class="file-meta">
                    <span class="file-name">{{ file.name }}</span>
                    <span class="file-hash">SHA-256: {{ file.hash }} • {{ file.size }}</span>
                  </div>
                  <button type="button" class="btn-remove-file" title="Eliminar archivo" @click="handleRemoveFile(idx)">
                    <svg viewBox="0 0 24 24" class="remove-svg" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </div>
              </div>

              <div v-else class="staged-empty">
                <svg viewBox="0 0 24 24" class="staged-empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                <span>No hay archivos en cola de procesamiento.</span>
              </div>
            </div>

            <!-- OBSERVACIONES TÉCNICAS -->
            <div class="form-group observations-group">
              <label class="form-lbl">Observaciones Técnicas del Laboratorio / Hallazgos Críticos Rápidos</label>
              <textarea
                v-model="observaciones"
                rows="3"
                placeholder="Ingrese notas operativas de la muestra (ej. Suero ligeramente ictérico, índice lipémico nominal, valores críticos verificados por duplicado)..."
                class="form-textarea"
              ></textarea>
            </div>
          </div>
        </div>
      </div>

      <!-- COLUMNA LATERAL DERECHA (QA ANALÍTICO, FIRMA DIGITAL Y PROTOCOLO) -->
      <div class="upload-sidebar-col">
        <!-- TARJETA 1: CONTROL DE CALIDAD ANALÍTICO -->
        <div class="side-card">
          <div class="side-card-header">
            <div class="side-header-left">
              <svg viewBox="0 0 24 24" class="side-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
                <line x1="8.5" y1="2" x2="15.5" y2="2"/>
              </svg>
              <h4>Control de Calidad Analítico</h4>
            </div>
            <span class="badge-calibrated">Calibrado</span>
          </div>

          <div class="side-card-body">
            <div class="qa-item">
              <div class="qa-meta">
                <span class="qa-lbl">Calibración Diaria Cobas</span>
                <span class="qa-val">06:00 h • Válida (Delta 0.02)</span>
              </div>
              <svg viewBox="0 0 24 24" class="qa-check-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>

            <div class="qa-item">
              <div class="qa-meta">
                <span class="qa-lbl">Control Bio-Rad Nivel 1 & 2</span>
                <span class="qa-val">Dentro de 2 Desviaciones Std</span>
              </div>
              <svg viewBox="0 0 24 24" class="qa-check-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>

            <div class="qa-item">
              <div class="qa-meta">
                <span class="qa-lbl">Hora Recepción de Muestra</span>
                <span class="qa-val">10:41 h (Protocolo STAT Activo)</span>
              </div>
              <svg viewBox="0 0 24 24" class="qa-clock-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- TARJETA 2: FIRMA ELECTRÓNICA Y SELLADO -->
        <div class="side-card signature-card">
          <div class="side-card-header">
            <div class="side-header-left">
              <svg viewBox="0 0 24 24" class="side-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <h4>Firma Electrónica y Sellado</h4>
            </div>
          </div>

          <div class="side-card-body">
            <!-- FICHA RESPONSABLE -->
            <div class="responsible-box">
              <div class="resp-avatar">MT</div>
              <div class="resp-meta">
                <strong class="resp-name">Q.F.B. Mariana Téllez Morales</strong>
                <span class="resp-cedula">Cédula Profesional LAB-449102</span>
                <span class="resp-sub">Responsable de Turno • Lab Central</span>
              </div>
              <svg viewBox="0 0 24 24" class="verified-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <polyline points="9 12 11 14 15 10"/>
              </svg>
            </div>

            <!-- TOKEN INPUT -->
            <div class="form-group">
              <div class="token-lbl-row">
                <label class="form-lbl">Token Criptográfico / Clave e-Firma *</label>
                <span class="sha-pill">SHA-256</span>
              </div>
              <div class="token-input-box">
                <svg viewBox="0 0 24 24" class="token-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <input
                  v-model="tokenFirma"
                  type="password"
                  placeholder="Ingrese PIN o clave e.firma..."
                  class="token-input"
                />
              </div>
            </div>

            <!-- CHECKBOX NORMATIVO -->
            <label class="checkbox-norma-label">
              <input v-model="certificacionAceptada" type="checkbox" class="norma-checkbox" />
              <span>
                Certifico formalmente que los resultados cargados corresponden a la muestra recibida y han superado las curvas de calibración y controles analíticos según la NOM-007-SSA3-2011.
              </span>
            </label>

            <!-- BOTONES DE ACCIÓN PRINCIPALES -->
            <div class="signature-actions">
              <button
                type="button"
                class="btn-firmar-notificar"
                @click="handleFirmarYNotificar"
              >
                <svg viewBox="0 0 24 24" class="btn-svg-lg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                <span>Firmar y Notificar a Urgencias [Enter]</span>
              </button>

              <div class="secondary-btn-row">
                <button type="button" class="btn-draft">
                  <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                    <polyline points="17 21 17 13 7 13 7 21"/>
                    <polyline points="7 3 7 8 15 8"/>
                  </svg>
                  <span>Guardar Borrador</span>
                </button>

                <button type="button" class="btn-reject" @click="handleRechazo">
                  <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                  </svg>
                  <span>Rechazar Muestra</span>
                </button>
              </div>
            </div>

            <!-- MOTIVOS DE RECHAZO AUTOMATIZADO -->
            <div class="reject-hints-box">
              <span class="hint-title">Motivos de Rechazo Automatizado:</span>
              <p>Muestra hemolizada, volumen insuficiente, tubo coagulado con EDTA o discrepancia en el código de rotulado.</p>
            </div>

            <div class="audit-immutable-row">
              <svg viewBox="0 0 24 24" class="lock-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <span>Registro inmutable en bitácora de auditoría hospitalaria con firma digital PKI.</span>
            </div>
          </div>
        </div>

        <!-- TARJETA 3: PROTOCOLO DE NOTIFICACIÓN INMEDIATA -->
        <div class="protocol-card">
          <div class="protocol-header">
            <svg viewBox="0 0 24 24" class="protocol-svg" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
            </svg>
            <h4>Protocolo de Notificación Inmediata</h4>
          </div>
          <p class="protocol-text">
            Los resultados clasificados como <strong>STAT / Código Rojo</strong> emitirán una alerta sonora instantánea en la Central de Monitoreo de Choque y en la tableta del médico tratante en menos de 3 segundos tras la firma.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-cargar-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.header-technical-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.tech-card-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 65%;
}

.tech-tag-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-tech {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  letter-spacing: 0.04em;
}

.badge-tech-sub {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  color: #64748b;
  font-weight: 600;
}

.tech-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.sep {
  color: #cbd5e1;
}

.tech-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.tech-desc {
  font-size: 0.75rem;
  color: #64748b;
  line-height: 1.35;
  margin: 0;
}

.tech-card-right {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.peripheral-pill {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
}

.dot-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-status.green {
  background-color: #16a34a;
  box-shadow: 0 0 6px rgba(22, 163, 74, 0.4);
}

.dot-status.blue {
  background-color: #0284c7;
  box-shadow: 0 0 6px rgba(2, 132, 199, 0.4);
}

.peripheral-info {
  display: flex;
  flex-direction: column;
}

.p-title {
  font-size: 0.7rem;
  font-weight: 700;
  color: #0f172a;
}

.p-sub {
  font-size: 0.62rem;
  color: #64748b;
}

.alert-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
}

.alert-box.success {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}

.alert-box.error {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.alert-icon-svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.upload-grid-container {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 1.25rem;
}

.upload-main-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.step-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 0.75rem 1rem;
}

.step-number-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background-color: #0284c7;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 800;
}

.step-header-text {
  display: flex;
  flex-direction: column;
}

.step-header-text h3 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.step-sub {
  font-size: 0.64rem;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.04em;
}

.step-card-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-lbl {
  font-size: 0.72rem;
  font-weight: 700;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hint-lbl {
  font-size: 0.65rem;
  font-weight: 500;
  color: #94a3b8;
}

.input-with-action {
  display: flex;
  align-items: center;
  position: relative;
}

.input-icon {
  position: absolute;
  left: 10px;
  width: 15px;
  height: 15px;
  color: #94a3b8;
}

.form-input, .form-select {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.78rem;
  color: #0f172a;
  outline: none;
  background-color: #ffffff;
  box-sizing: border-box;
}

.input-with-action .form-input {
  padding-left: 2rem;
  padding-right: 5.5rem;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.btn-input-search {
  position: absolute;
  right: 4px;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.btn-input-search:hover {
  background-color: #e2e8f0;
}

.btn-mini-svg {
  width: 13px;
  height: 13px;
  color: #0284c7;
}

.status-nom-box {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 0.75rem;
  border-radius: 6px;
}

.nom-svg {
  width: 20px;
  height: 20px;
  color: #16a34a;
  flex-shrink: 0;
  margin-top: 2px;
}

.nom-text {
  font-size: 0.72rem;
  color: #166534;
}

.nom-text strong {
  display: block;
  font-size: 0.76rem;
  color: #14532d;
  margin-bottom: 2px;
}

.nom-text p {
  margin: 0;
  line-height: 1.35;
}

.format-tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.5rem;
}

.format-tab {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background-color: #f8fafc;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.format-tab:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.format-tab.active {
  background-color: #0284c7;
  border-color: #0284c7;
  color: #ffffff;
}

.tab-svg {
  width: 14px;
  height: 14px;
}

.dropzone-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;
  background-color: #f8fafc;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  text-align: center;
  transition: all 0.15s ease;
}

.dropzone-box.dragging {
  border-color: #0284c7;
  background-color: #f0f9ff;
}

.dropzone-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: #e0f2fe;
  color: #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.65rem;
}

.cloud-svg {
  width: 22px;
  height: 22px;
}

.dropzone-title {
  font-size: 0.88rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.35rem 0;
}

.dropzone-sub {
  font-size: 0.74rem;
  color: #64748b;
  max-width: 520px;
  margin: 0 0 1rem 0;
  line-height: 1.35;
}

.dropzone-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.btn-browse {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0284c7;
  color: #ffffff;
  padding: 0.45rem 1rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
}

.btn-import-device {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.45rem 1rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-svg-sm {
  width: 14px;
  height: 14px;
}

.limit-caption {
  font-size: 0.66rem;
  color: #94a3b8;
}

.tech-pills-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}

.tech-feature-pill {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.55rem;
  border-radius: 6px;
}

.feature-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.feature-dot.blue {
  background-color: #0284c7;
}

.feature-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-meta strong {
  font-size: 0.72rem;
  color: #0f172a;
}

.feature-meta span {
  font-size: 0.65rem;
  color: #64748b;
  line-height: 1.25;
}

.staged-files-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.staged-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
}

.staged-files-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.staged-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
}

.file-type-badge {
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}

.file-meta {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.file-name {
  font-size: 0.78rem;
  font-weight: 700;
  color: #0f172a;
}

.file-hash {
  font-size: 0.64rem;
  color: #64748b;
  font-family: monospace;
}

.btn-remove-file {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.btn-remove-file:hover {
  background-color: #fee2e2;
  color: #b91c1c;
}

.remove-svg {
  width: 14px;
  height: 14px;
}

.staged-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 1rem;
  background-color: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  font-size: 0.74rem;
  color: #94a3b8;
}

.staged-empty-svg {
  width: 16px;
  height: 16px;
}

.observations-group {
  margin-top: 0.5rem;
}

.form-textarea {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.78rem;
  color: #0f172a;
  outline: none;
  box-sizing: border-box;
  resize: vertical;
}

.upload-sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.side-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.side-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.side-header-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.side-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.side-header-left h4 {
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-calibrated {
  background-color: #e0f2fe;
  color: #0284c7;
  font-size: 0.64rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.side-card-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.qa-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.qa-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.qa-meta {
  display: flex;
  flex-direction: column;
}

.qa-lbl {
  font-size: 0.72rem;
  font-weight: 700;
  color: #0f172a;
}

.qa-val {
  font-size: 0.68rem;
  color: #64748b;
}

.qa-check-svg {
  width: 16px;
  height: 16px;
  color: #16a34a;
}

.qa-clock-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.responsible-box {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.65rem;
}

.resp-avatar {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background-color: #0f172a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
}

.resp-meta {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.resp-name {
  font-size: 0.76rem;
  color: #0f172a;
}

.resp-cedula {
  font-size: 0.66rem;
  color: #0284c7;
  font-weight: 600;
}

.resp-sub {
  font-size: 0.62rem;
  color: #64748b;
}

.verified-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.token-lbl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sha-pill {
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748b;
  background-color: #f1f5f9;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.token-input-box {
  display: flex;
  align-items: center;
  position: relative;
}

.token-svg {
  position: absolute;
  left: 10px;
  width: 14px;
  height: 14px;
  color: #94a3b8;
}

.token-input {
  width: 100%;
  padding: 0.45rem 0.65rem 0.45rem 2rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.78rem;
  color: #0f172a;
  outline: none;
}

.checkbox-norma-label {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.68rem;
  color: #475569;
  line-height: 1.35;
  cursor: pointer;
}

.norma-checkbox {
  margin-top: 3px;
  cursor: pointer;
}

.signature-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.btn-firmar-notificar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  background-color: #0284c7;
  color: #ffffff;
  padding: 0.65rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 800;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);
  transition: all 0.15s ease;
}

.btn-firmar-notificar:hover {
  background-color: #0369a1;
}

.btn-svg-lg {
  width: 16px;
  height: 16px;
}

.secondary-btn-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.btn-draft, .btn-reject {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.45rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-draft {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
}

.btn-draft:hover {
  background-color: #e2e8f0;
}

.btn-reject {
  background-color: #fee2e2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.btn-reject:hover {
  background-color: #fca5a5;
}

.reject-hints-box {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.65rem;
  color: #64748b;
  line-height: 1.3;
}

.hint-title {
  font-weight: 700;
  color: #475569;
  display: block;
  margin-bottom: 2px;
}

.reject-hints-box p {
  margin: 0;
}

.audit-immutable-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.65rem;
  color: #64748b;
}

.lock-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
  flex-shrink: 0;
}

.protocol-card {
  background: linear-gradient(135deg, #7f1d1d, #991b1b);
  color: #ffffff;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  box-shadow: 0 4px 10px rgba(153, 27, 27, 0.25);
}

.protocol-header {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.protocol-svg {
  width: 16px;
  height: 16px;
  color: #fca5a5;
}

.protocol-header h4 {
  font-size: 0.78rem;
  font-weight: 800;
  margin: 0;
}

.protocol-text {
  font-size: 0.7rem;
  color: #fee2e2;
  line-height: 1.35;
  margin: 0;
}

.protocol-text strong {
  color: #ffffff;
}
</style>
