<script setup lang="ts">
import { ref } from 'vue'
import type { LabKPIs } from '../types'

const props = defineProps<{
  kpis: LabKPIs
}>()

const filtroSubtipo = ref('TODOS')
const filtroEstado = ref('NOTIFICADO')
const terminoBusqueda = ref('')
const selectedRowFolio = ref('EST-2026-9039')
const modalNotice = ref('')

const bitacoraRows = [
  {
    folio: 'EST-2026-9039',
    hora: '10:41 h',
    paciente: 'J.L.M.',
    edad: '58a',
    sexo: 'Masculino',
    estudio: 'Enzimas Cardíacas / Troponina I',
    metodo: 'Quimioluminiscencia - Alta Sensibilidad',
    servicio: 'Urgencias • Box 01',
    medico: 'Dr. Carlos Mendoza',
    formato: 'PDF',
    tamano: '1.4 MB',
    shaPreview: '4fa9...8c2b',
    shaFull: '4fa9c182b8a93e198df4920bce1101ab648821ec39062fa883908c2b'
  },
  {
    folio: 'EST-2026-9035',
    hora: '10:28 h',
    paciente: 'R.T.B.',
    edad: '34a',
    sexo: 'Femenino',
    estudio: 'Gasometría Arterial + Lactato',
    metodo: 'Potenciometría y Amperometría',
    servicio: 'Urgencias • Choque 02',
    medico: 'Dra. Verónica Peña',
    formato: 'PDF',
    tamano: '890 KB',
    shaPreview: '7b11...99e3',
    shaFull: '7b11a9e3381a4b56890f110a2291cd4a8721ef65a1209b019348102a'
  },
  {
    folio: 'EST-2026-9028',
    hora: '10:15 h',
    paciente: 'S.P.G.',
    edad: '67a',
    sexo: 'Masculino',
    estudio: 'Radiografía de Tórax PA / Lat',
    metodo: 'Radiología Digital Directa (DR)',
    servicio: 'Urgencias • Obs. Adultos',
    medico: 'Dr. Alberto Garza',
    formato: 'DICOM',
    tamano: '42 MB',
    shaPreview: 'e83d...11ac',
    shaFull: 'e83d11ac72b1467406a092c4311059f338827fa112009cb987621a22'
  },
  {
    folio: 'EST-2026-9014',
    hora: '09:54 h',
    paciente: 'M.A.V.',
    edad: '42a',
    sexo: 'Femenino',
    estudio: 'Biometría Hemática Completa',
    metodo: 'Citometría de Flujo con 5 Diff',
    servicio: 'Piso 3 • Med. Interna',
    medico: 'Dra. Claudia Domínguez',
    formato: 'PDF',
    tamano: '1.1 MB',
    shaPreview: '3c19...4f02',
    shaFull: '3c19a4f0291ba786110a3341fe89a1024509bd231a44e55120398711'
  },
  {
    folio: 'EST-2026-9008',
    hora: '09:30 h',
    paciente: 'D.K.N.',
    edad: '29a',
    sexo: 'Femenino',
    estudio: 'Tiempos Coagulación (TP, TTP, INR)',
    metodo: 'Coagulometría Foto-Óptica',
    servicio: 'Quirófano Central • QX-4',
    medico: 'Dr. Sergio Navarro',
    formato: 'PDF',
    tamano: '920 KB',
    shaPreview: '99a0...33bd',
    shaFull: '99a0b33bd87192a001192e44810bce90451122a76810239485710293'
  },
  {
    folio: 'EST-2026-8995',
    hora: '08:45 h',
    paciente: 'G.H.L.',
    edad: '51a',
    sexo: 'Masculino',
    estudio: 'Química Clínica 32 Elementos',
    metodo: 'Espectrofotometría Automatizada',
    servicio: 'Consulta Externa • C-08',
    medico: 'Dra. Elena Ríos',
    formato: 'PDF',
    tamano: '1.6 MB',
    shaPreview: '1a88...fe49',
    shaFull: '1a88cfe4988710293847561029384756102938475610293847561029'
  }
]

const handleExport = (type: string) => {
  modalNotice.value = `Exportando bitácora completa en formato ${type}... Descarga generada con firma PKI.`
  setTimeout(() => {
    modalNotice.value = ''
  }, 3500)
}

const handleGuardia = () => {
  modalNotice.value = 'Generando acta digital de Entrega de Guardia de Laboratorio con sello de tiempo NTP.'
  setTimeout(() => {
    modalNotice.value = ''
  }, 3500)
}
</script>

<template>
  <div class="lab-historial-section">
    <!-- ENCABEZADO DE SECCIÓN -->
    <div class="historial-header-bar">
      <div class="header-h-left">
        <div class="header-h-tags">
          <span class="badge-firma-activa">
            <span class="dot-green"></span>
            e.firma Activa
          </span>
          <span class="sep">•</span>
          <span class="norma-lbl">NOM-024 / NOM-007</span>
        </div>
        <div class="title-with-shield">
          <svg viewBox="0 0 24 24" class="shield-title-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <polyline points="9 12 11 14 15 10"/>
          </svg>
          <h2>Historial y Bitácora de Cargas Validadas</h2>
        </div>
        <p class="historial-sub">
          Registro auditable de estudios con resultados emitidos, firma electrónica avanzada y trazabilidad de entrega telemática al ECE.
        </p>
      </div>

      <div class="header-h-right">
        <button type="button" class="btn-export" @click="handleExport('CSV / PDF')">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>Exportar Bitácora [CSV / PDF]</span>
        </button>

        <button type="button" class="btn-guardia" @click="handleGuardia">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
          <span>Entrega de Guardia</span>
        </button>
      </div>
    </div>

    <!-- NOTIFICACIÓN TEMPORAL -->
    <div v-if="modalNotice" class="notice-bar">
      <svg viewBox="0 0 24 24" class="notice-svg" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="20 6 9 17 4 12"/>
      </svg>
      <span>{{ modalNotice }}</span>
    </div>

    <!-- 4 TARJETAS DE INDICADORES CLAVE (KPIS) -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-lbl">ESTUDIOS VALIDADOS HOY</span>
          <svg viewBox="0 0 24 24" class="kpi-icon blue" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="kpi-card-body">
          <div class="kpi-main-val">{{ kpis.validados_hoy }} <span class="kpi-unit">informes</span></div>
          <div class="kpi-trend positive">
            <svg viewBox="0 0 24 24" class="trend-svg" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
              <polyline points="17 6 23 6 23 12"/>
            </svg>
            <span>+12% vs turno previo</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-lbl">SLA PROMEDIO DE ENTREGA</span>
          <svg viewBox="0 0 24 24" class="kpi-icon blue" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="kpi-card-body">
          <div class="kpi-main-val">{{ kpis.sla_promedio_min }} <span class="kpi-unit">min</span></div>
          <div class="kpi-sub-row">
            <span class="meta-desc">Meta: &lt;20 min (Urgencias)</span>
            <span class="badge-optimo">ÓPTIMO</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-lbl">CUMPLIMIENTO GLOBAL SLA</span>
          <svg viewBox="0 0 24 24" class="kpi-icon blue" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
        <div class="kpi-card-body">
          <div class="kpi-main-val">{{ kpis.cumplimiento_pct }} <span class="kpi-unit">%</span></div>
          <div class="kpi-progress-bar">
            <div class="progress-fill" :style="{ width: `${kpis.cumplimiento_pct}%` }"></div>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-lbl">TASA RECHAZO DE MUESTRA</span>
          <svg viewBox="0 0 24 24" class="kpi-icon blue" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
            <line x1="8.5" y1="2" x2="15.5" y2="2"/>
          </svg>
        </div>
        <div class="kpi-card-body">
          <div class="kpi-main-val">{{ kpis.tasa_rechazo_pct }} <span class="kpi-unit">%</span></div>
          <div class="kpi-sub-row">
            <span class="meta-desc">ISO 15189 • Umbral estricto &lt;1.5%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- FILTROS Y BUSCADOR -->
    <div class="historial-controls-card">
      <div class="controls-row-top">
        <div class="pills-categories">
          <button
            type="button"
            class="cat-pill"
            :class="{ active: filtroSubtipo === 'TODOS' }"
            @click="filtroSubtipo = 'TODOS'"
          >
            Todos los Estudios (68)
          </button>
          <button
            type="button"
            class="cat-pill"
            :class="{ active: filtroSubtipo === 'LAB' }"
            @click="filtroSubtipo = 'LAB'"
          >
            Laboratorio Clínico (51)
          </button>
          <button
            type="button"
            class="cat-pill"
            :class="{ active: filtroSubtipo === 'IMG' }"
            @click="filtroSubtipo = 'IMG'"
          >
            Imagenología / Rayos X (12)
          </button>
          <button
            type="button"
            class="cat-pill"
            :class="{ active: filtroSubtipo === 'GAB' }"
            @click="filtroSubtipo = 'GAB'"
          >
            Gabinete Especializado (5)
          </button>
        </div>

        <div class="shift-selector">
          <span class="shift-chip">
            <svg viewBox="0 0 24 24" class="shift-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            Turno Matutino [Hoy 07:00 - 15:00]
          </span>
          <button type="button" class="btn-shift-prev">
            <svg viewBox="0 0 24 24" class="cal-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <span>Fecha Previa</span>
          </button>
        </div>
      </div>

      <div class="controls-row-bottom">
        <div class="state-filter-pills">
          <span class="state-lbl">ESTADO:</span>
          <button
            type="button"
            class="state-pill"
            :class="{ active: filtroEstado === 'NOTIFICADO' }"
            @click="filtroEstado = 'NOTIFICADO'"
          >
            <svg viewBox="0 0 24 24" class="state-svg" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span>Notificado y Recibido en ECE</span>
          </button>
          <button
            type="button"
            class="state-pill"
            :class="{ active: filtroEstado === 'FIRMADO' }"
            @click="filtroEstado = 'FIRMADO'"
          >
            <span>Firmado / Pendiente Lectura Médica</span>
          </button>
          <button
            type="button"
            class="state-pill"
            :class="{ active: filtroEstado === 'RECHAZADO' }"
            @click="filtroEstado = 'RECHAZADO'"
          >
            <span>Muestra Rechazada (0)</span>
          </button>
        </div>

        <div class="search-wrap">
          <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            v-model="terminoBusqueda"
            type="text"
            placeholder="Buscar por Folio (ej. EST-2026-9039) o ID Muestra... Ctrl+K"
            class="search-hist-input"
          />
        </div>
      </div>
    </div>

    <!-- LAYOUT PRINCIPAL: TABLA + PANEL COMPROBANTE ECE -->
    <div class="historial-grid">
      <!-- TABLA DE REGISTROS EMITIDOS -->
      <div class="historial-table-col">
        <div class="table-container">
          <div class="table-header-title-row">
            <div class="tbl-left-h">
              <svg viewBox="0 0 24 24" class="tbl-icon" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
              <h3>Registros Emitidos con e.firma y Acuse HL7/FHIR</h3>
            </div>
            <span class="badge-concluidos">
              <span class="dot-blue"></span>
              68 de 68 Concluidos
            </span>
          </div>

          <table class="bitacora-table">
            <thead>
              <tr>
                <th>HORA / FOLIO</th>
                <th>PACIENTE (NOM-024)</th>
                <th>ESTUDIO REALIZADO</th>
                <th>SERVICIO SOLICITANTE</th>
                <th>FORMATO & SELLO</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in bitacoraRows"
                :key="row.folio"
                :class="{ active: selectedRowFolio === row.folio }"
                @click="selectedRowFolio = row.folio"
              >
                <td class="cell-time-folio">
                  <span class="row-hour">{{ row.hora }}</span>
                  <span class="row-folio">{{ row.folio }}</span>
                </td>

                <td class="cell-patient">
                  <strong class="pac-initials">{{ row.paciente }}</strong>
                  <span class="pac-meta">{{ row.edad }} • {{ row.sexo }}</span>
                </td>

                <td class="cell-study">
                  <strong class="study-name">{{ row.estudio }}</strong>
                  <span class="study-method">{{ row.metodo }}</span>
                </td>

                <td class="cell-service">
                  <span class="svc-name">{{ row.servicio }}</span>
                  <span class="svc-doc">{{ row.medico }}</span>
                </td>

                <td class="cell-file">
                  <div class="file-chip">
                    <svg viewBox="0 0 24 24" class="file-icon" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                    <span>{{ row.formato }} {{ row.tamano }}</span>
                  </div>
                  <div class="sha-link">
                    <svg viewBox="0 0 24 24" class="lock-mini" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    <span>SHA: {{ row.shaPreview }}</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="pagination-footer">
            <span class="pagination-info">Mostrando registros 1 a 6 de 68 validados en el turno [Página 1 de 12]</span>
            <div class="pagination-btns">
              <button type="button" class="btn-page" disabled>Anterior</button>
              <button type="button" class="btn-page active">1</button>
              <button type="button" class="btn-page">2</button>
              <button type="button" class="btn-page">3</button>
              <span class="page-dots">...</span>
              <button type="button" class="btn-page">12</button>
              <button type="button" class="btn-page">Siguiente</button>
            </div>
          </div>
        </div>
      </div>

      <!-- PANEL LATERAL DERECHO: COMPROBANTE DE ENTREGA ECE Y TELEMETRÍA HL7 -->
      <div class="historial-detail-col">
        <!-- TARJETA: COMPROBANTE DE ENTREGA ECE -->
        <div class="ece-receipt-card">
          <div class="ece-header">
            <div class="ece-h-left">
              <svg viewBox="0 0 24 24" class="ece-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
              <h4>Comprobante de Entrega ECE</h4>
            </div>
            <span class="badge-acuse">ACUSE ACTIVO</span>
          </div>

          <div class="ece-body">
            <div class="receipt-fields">
              <div class="field-item">
                <span class="field-lbl">FOLIO ÚNICO:</span>
                <span class="field-val highlight">{{ selectedRowFolio }}</span>
              </div>
              <div class="field-item">
                <span class="field-lbl">PACIENTE:</span>
                <span class="field-val">J.L.M. (58 años)</span>
              </div>
              <div class="field-item">
                <span class="field-lbl">ESTUDIO:</span>
                <span class="field-val">Troponina I Alta Sensibilidad</span>
              </div>
              <div class="field-item">
                <span class="field-lbl">MÉDICO RECEPTOR:</span>
                <span class="field-val">Dr. Carlos Mendoza (Box 01)</span>
              </div>
              <div class="field-item">
                <span class="field-lbl">TIMESTAMP:</span>
                <span class="field-val">17/10/2026 • 10:41:18 CST</span>
              </div>
            </div>

            <div class="crypto-traz-box">
              <div class="crypto-header">
                <span class="crypto-lbl">TRAZABILIDAD CRIPTOGRÁFICA:</span>
                <span class="conciliated-chip">CONCILIADO</span>
              </div>
              <span class="hash-tag">HASH DIGEST (SHA-256)</span>
              <div class="hash-string-box">
                4fa9c182b8a93e198df4920bce1101ab648821ec39062fa883908c2b
              </div>
            </div>

            <div class="hl7-reception-box">
              <span class="hl7-lbl">RECEPCIÓN TELEMÁTICA ECE:</span>
              <div class="hl7-item">
                <div class="hl7-meta">
                  <strong>HL7 v2.5.1 ORU^R01</strong>
                  <span>Confirmado por Servidor Central (ACK_AA)</span>
                </div>
                <div class="code-ok-badge">200 OK</div>
              </div>
            </div>

            <div class="ece-actions-row">
              <button type="button" class="btn-validate-seal">
                <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  <polyline points="9 12 11 14 15 10"/>
                </svg>
                <span>Validar Sello</span>
              </button>

              <button type="button" class="btn-copy-foil">
                <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                </svg>
                <span>Copia Foliada</span>
              </button>
            </div>
          </div>
        </div>

        <!-- TARJETA: CERTIFICADO DE FIRMA DIGITAL ACTIVO -->
        <div class="pki-card">
          <div class="pki-header">
            <h4>CERTIFICADO DE FIRMA DIGITAL ACTIVO</h4>
            <svg viewBox="0 0 24 24" class="key-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 2l-2 2m-1.5 1.5L16 7l-1.5-1.5M7 12a5 5 0 1 1 5-5L9 10"/>
            </svg>
          </div>

          <div class="pki-body">
            <div class="pki-user-row">
              <div class="pki-avatar">MT</div>
              <div class="pki-user-meta">
                <strong>Q.F.B. Mariana Téllez Morales</strong>
                <span>Cédula Profesional LAB-449102</span>
                <span class="ca-text">Autoridad Certificadora: Vitae PKI Root CA</span>
              </div>
            </div>

            <div class="pki-details-list">
              <div class="pki-detail-item">
                <span>Algoritmo:</span>
                <strong>ECDSA P-256 / SHA-256</strong>
              </div>
              <div class="pki-detail-item">
                <span>Vigencia:</span>
                <strong>Hasta 14 de mayo de 2027</strong>
              </div>
              <div class="pki-detail-item">
                <span>Token Físico:</span>
                <span class="token-online">Conectado (USB eToken HS-04)</span>
              </div>
            </div>
          </div>
        </div>

        <!-- TARJETA: CONECTIVIDAD HL7 / FHIR -->
        <div class="hl7-status-card">
          <div class="hl7-card-header">
            <span class="hl7-stat-title">CONECTIVIDAD HL7 / FHIR</span>
            <span class="dot-online-text">
              <span class="dot-green"></span>
              En Línea
            </span>
          </div>

          <div class="hl7-stat-body">
            <div class="stat-line">
              <span>Enlace Gateway Hospitalario:</span>
              <strong>TLS 1.3 / mTLS Activo</strong>
            </div>
            <div class="stat-line">
              <span>Mensajes ORU Emitidos Hoy:</span>
              <strong>68 / 68 sin errores</strong>
            </div>
            <div class="stat-line">
              <span>Latencia de Notificación:</span>
              <strong>128 ms</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- FOOTER INMUTABILIDAD CRIPTOGRÁFICA -->
    <div class="crypto-audit-footer">
      <div class="footer-left-info">
        <svg viewBox="0 0 24 24" class="clock-audit-svg" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <div class="audit-text">
          <strong>Bitácora Criptográfica Inmutable (Log Append-Only)</strong>
          <span>Cada reporte emitido queda sellado con estampa de tiempo NTP del CENAM y firma digital conforme a NOM-024-SSA3.</span>
        </div>
      </div>

      <div class="ntp-badge">
        <span>NTP SYNC: ±0.002s</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-historial-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.historial-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-h-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 65%;
}

.header-h-tags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
}

.badge-firma-activa {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-weight: 700;
  font-size: 0.68rem;
}

.dot-green {
  width: 6px;
  height: 6px;
  background-color: #16a34a;
  border-radius: 50%;
}

.norma-lbl {
  color: #64748b;
  font-weight: 600;
}

.sep {
  color: #cbd5e1;
}

.title-with-shield {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.shield-title-svg {
  width: 22px;
  height: 22px;
  color: #0284c7;
}

.title-with-shield h2 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.historial-sub {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.header-h-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-export {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-export:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}

.btn-guardia {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0f172a;
  color: #ffffff;
  padding: 0.5rem 0.95rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.25);
}

.btn-guardia:hover {
  background-color: #1e293b;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.notice-bar {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
}

.notice-svg {
  width: 15px;
  height: 15px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-lbl {
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.04em;
}

.kpi-icon {
  width: 16px;
  height: 16px;
}

.kpi-icon.blue {
  color: #0284c7;
}

.kpi-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.kpi-main-val {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
}

.kpi-unit {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 700;
}

.kpi-trend.positive {
  color: #0284c7;
}

.trend-svg {
  width: 13px;
  height: 13px;
}

.kpi-sub-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.68rem;
}

.meta-desc {
  color: #64748b;
}

.badge-optimo {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.62rem;
  font-weight: 800;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.kpi-progress-bar {
  width: 100%;
  height: 6px;
  background-color: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 4px;
}

.progress-fill {
  height: 100%;
  background-color: #0284c7;
  border-radius: 3px;
}

.historial-controls-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.controls-row-top, .controls-row-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pills-categories {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.cat-pill {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.cat-pill:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.cat-pill.active {
  background-color: #0284c7;
  border-color: #0284c7;
  color: #ffffff;
}

.shift-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.shift-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  color: #334155;
  background-color: #f1f5f9;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  font-weight: 600;
}

.shift-svg {
  width: 13px;
  height: 13px;
  color: #0284c7;
}

.btn-shift-prev {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  font-size: 0.72rem;
  color: #475569;
  cursor: pointer;
}

.cal-svg {
  width: 13px;
  height: 13px;
}

.state-filter-pills {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.state-lbl {
  font-size: 0.68rem;
  font-weight: 800;
  color: #64748b;
}

.state-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  font-size: 0.72rem;
  color: #475569;
  cursor: pointer;
}

.state-pill.active {
  background-color: #e0f2fe;
  color: #0369a1;
  border-color: #bae6fd;
  font-weight: 700;
}

.state-svg {
  width: 13px;
  height: 13px;
  color: #0284c7;
}

.search-wrap {
  display: flex;
  align-items: center;
  position: relative;
  min-width: 340px;
}

.search-svg {
  position: absolute;
  left: 10px;
  width: 15px;
  height: 15px;
  color: #94a3b8;
}

.search-hist-input {
  width: 100%;
  padding: 0.45rem 0.75rem 0.45rem 2rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.76rem;
  color: #0f172a;
  outline: none;
  background-color: #f8fafc;
}

.search-hist-input:focus {
  border-color: #0284c7;
  background-color: #ffffff;
}

.historial-grid {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 1.25rem;
}

.historial-table-col {
  display: flex;
  flex-direction: column;
}

.table-container {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.table-header-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.tbl-left-h {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.tbl-icon {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.tbl-left-h h3 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-concluidos {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: #0369a1;
}

.dot-blue {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #0284c7;
}

.bitacora-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.74rem;
}

.bitacora-table th {
  text-align: left;
  font-size: 0.66rem;
  font-weight: 700;
  color: #64748b;
  padding: 0.55rem 0.85rem;
  border-bottom: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.bitacora-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.bitacora-table tr:hover td {
  background-color: #f8fafc;
}

.bitacora-table tr.active td {
  background-color: #f0f9ff;
  border-bottom-color: #bae6fd;
}

.cell-time-folio {
  display: flex;
  flex-direction: column;
}

.row-hour {
  font-size: 0.8rem;
  font-weight: 700;
  color: #0f172a;
}

.row-folio {
  font-family: monospace;
  font-size: 0.72rem;
  color: #0284c7;
  font-weight: 700;
}

.cell-patient {
  vertical-align: middle;
}

.pac-initials {
  display: block;
  font-size: 0.8rem;
  color: #0f172a;
}

.pac-meta {
  font-size: 0.68rem;
  color: #64748b;
}

.cell-study {
  vertical-align: middle;
}

.study-name {
  display: block;
  font-size: 0.78rem;
  color: #0f172a;
}

.study-method {
  font-size: 0.66rem;
  color: #64748b;
}

.cell-service {
  vertical-align: middle;
}

.svc-name {
  display: block;
  font-size: 0.74rem;
  color: #334155;
  font-weight: 600;
}

.svc-doc {
  font-size: 0.66rem;
  color: #64748b;
}

.cell-file {
  vertical-align: middle;
}

.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #fee2e2;
  color: #b91c1c;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 700;
}

.file-icon {
  width: 12px;
  height: 12px;
}

.sha-link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.64rem;
  color: #0284c7;
  font-family: monospace;
  margin-top: 2px;
}

.lock-mini {
  width: 10px;
  height: 10px;
}

.pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 1rem;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.pagination-info {
  font-size: 0.7rem;
  color: #64748b;
}

.pagination-btns {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.btn-page {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.25rem 0.55rem;
  border-radius: 4px;
  font-size: 0.7rem;
  color: #334155;
  cursor: pointer;
}

.btn-page.active {
  background-color: #0f172a;
  color: #ffffff;
  border-color: #0f172a;
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-dots {
  color: #94a3b8;
  font-size: 0.7rem;
}

.historial-detail-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ece-receipt-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.ece-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.ece-h-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.ece-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.ece-h-left h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-acuse {
  background-color: #e0f2fe;
  color: #0284c7;
  font-size: 0.64rem;
  font-weight: 800;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.ece-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.receipt-fields {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.72rem;
}

.field-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.field-lbl {
  color: #64748b;
  font-size: 0.66rem;
  font-weight: 700;
}

.field-val {
  font-weight: 600;
  color: #0f172a;
  text-align: right;
}

.field-val.highlight {
  font-family: monospace;
  font-weight: 800;
  color: #0284c7;
}

.crypto-traz-box {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.65rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.crypto-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.crypto-lbl {
  font-size: 0.66rem;
  font-weight: 700;
  color: #475569;
}

.conciliated-chip {
  background-color: #f0fdf4;
  color: #16a34a;
  font-size: 0.62rem;
  font-weight: 800;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.hash-tag {
  font-size: 0.62rem;
  color: #64748b;
  font-weight: 600;
}

.hash-string-box {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.35rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.62rem;
  color: #0f172a;
  word-break: break-all;
  line-height: 1.25;
}

.hl7-reception-box {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.hl7-lbl {
  font-size: 0.66rem;
  font-weight: 700;
  color: #475569;
}

.hl7-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
}

.hl7-meta {
  display: flex;
  flex-direction: column;
}

.hl7-meta strong {
  font-size: 0.74rem;
  color: #0369a1;
}

.hl7-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.code-ok-badge {
  background-color: #0284c7;
  color: #ffffff;
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}

.ece-actions-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.btn-validate-seal, .btn-copy-foil {
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

.btn-validate-seal {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
}

.btn-validate-seal:hover {
  background-color: #e2e8f0;
}

.btn-copy-foil {
  background-color: #0284c7;
  border: none;
  color: #ffffff;
}

.btn-copy-foil:hover {
  background-color: #0369a1;
}

.btn-svg-sm {
  width: 13px;
  height: 13px;
}

.pki-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.pki-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pki-header h4 {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #475569;
  margin: 0;
}

.key-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.pki-user-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #f8fafc;
  padding: 0.5rem;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.pki-avatar {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background-color: #0f172a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.74rem;
  font-weight: 800;
}

.pki-user-meta {
  display: flex;
  flex-direction: column;
}

.pki-user-meta strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.pki-user-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.pki-user-meta .ca-text {
  font-size: 0.62rem;
  color: #0284c7;
  font-weight: 600;
}

.pki-details-list {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.7rem;
}

.pki-detail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pki-detail-item span {
  color: #64748b;
}

.pki-detail-item strong {
  color: #0f172a;
}

.token-online {
  color: #16a34a;
  font-weight: 700;
}

.hl7-status-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hl7-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hl7-stat-title {
  font-size: 0.66rem;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.04em;
}

.dot-online-text {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.66rem;
  font-weight: 700;
  color: #16a34a;
}

.hl7-stat-body {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.7rem;
}

.stat-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-line span {
  color: #64748b;
}

.stat-line strong {
  color: #0f172a;
}

.crypto-audit-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.65rem 1rem;
}

.footer-left-info {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.clock-audit-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.audit-text {
  display: flex;
  flex-direction: column;
}

.audit-text strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.audit-text span {
  font-size: 0.66rem;
  color: #64748b;
}

.ntp-badge {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 0.25rem 0.55rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.68rem;
  font-weight: 700;
  color: #0369a1;
}
</style>
