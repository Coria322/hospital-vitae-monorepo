<script setup lang="ts">
import { ref } from 'vue'
import type { EstudioDiagnostico } from '../types'

const props = defineProps<{
  activeStudy: EstudioDiagnostico
}>()

const emit = defineEmits<{
  (e: 'cargar-estudio', order: EstudioDiagnostico): void
  (e: 'imprimir-etiqueta', folio: string): void
  (e: 'reportar-incidencia', folio: string): void
}>()

const modoBusqueda = ref<'FOLIO' | 'TUBO' | 'PACIENTE'>('FOLIO')
const terminoBusqueda = ref('EST-2026-9041')
const scanFeedback = ref('')

const handleTestLector = () => {
  scanFeedback.value = 'Lectura de código de barras simulada: BC-HEP-88190-OK'
  setTimeout(() => {
    scanFeedback.value = ''
  }, 3500)
}
</script>

<template>
  <div class="lab-busqueda-section">
    <!-- ENCABEZADO DE BÚSQUEDA Y PROTOCOLO -->
    <div class="search-header-panel">
      <div class="search-header-left">
        <div class="header-tags">
          <span class="badge-nom">NOM-024 / NOM-007 AUXILIARES</span>
          <span class="sep">•</span>
          <span class="terminal-info">Terminal: LAB-EST-04 • Planta Baja</span>
          <span class="sep">•</span>
          <span class="sync-info">
            <span class="dot-green"></span>
            Conexión LIS Central: Sincronizada (2ms)
          </span>
        </div>
        <h2 class="search-main-title">Búsqueda y Localización de Estudios Diagnósticos</h2>
        <p class="search-main-desc">
          Localice solicitudes de laboratorio clínico, gasometrías y radiología por folio único institucional, código de muestra o iniciales. Consulta estricta sin acceso al expediente clínico integral conforme a la normativa de privacidad.
        </p>
      </div>

      <div class="search-header-right">
        <div class="scanner-badge">
          <span class="dot-scanner"></span>
          <div class="scanner-meta">
            <strong>LECTOR HONEYWELL 1950G</strong>
            <span>Modo Cuña HID • Activo</span>
          </div>
        </div>
        <button type="button" class="btn-guide-f1" title="Manual de códigos y atajos">
          <svg viewBox="0 0 24 24" class="guide-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <span>Guía F1</span>
        </button>
      </div>
    </div>

    <!-- MÉTODOS DE BÚSQUEDA Y BARRA PRINCIPAL -->
    <div class="search-bar-card">
      <div class="search-mode-tabs">
        <button
          type="button"
          class="mode-btn"
          :class="{ active: modoBusqueda === 'FOLIO' }"
          @click="modoBusqueda = 'FOLIO'"
        >
          <svg viewBox="0 0 24 24" class="mode-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
          <span>Por Folio de Estudio (EST-XXXX)</span>
        </button>

        <button
          type="button"
          class="mode-btn"
          :class="{ active: modoBusqueda === 'TUBO' }"
          @click="modoBusqueda = 'TUBO'"
        >
          <svg viewBox="0 0 24 24" class="mode-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <line x1="7" y1="3" x2="7" y2="21"/>
            <line x1="17" y1="3" x2="17" y2="21"/>
          </svg>
          <span>Por Código de Tubo / Alícuota</span>
        </button>

        <button
          type="button"
          class="mode-btn"
          :class="{ active: modoBusqueda === 'PACIENTE' }"
          @click="modoBusqueda = 'PACIENTE'"
        >
          <svg viewBox="0 0 24 24" class="mode-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          <span>Por Iniciales y Fecha de Toma</span>
        </button>
      </div>

      <div class="search-input-composite">
        <div class="input-wrap">
          <svg viewBox="0 0 24 24" class="search-input-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            v-model="terminoBusqueda"
            type="text"
            placeholder="Escriba el folio oficial ej. EST-2026-9041 o acerque el escáner..."
            class="main-search-input"
          />
          <button type="button" class="btn-scan-trigger" @click="handleTestLector">
            <svg viewBox="0 0 24 24" class="scan-mini-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 7V5a2 2 0 0 1 2-2h2"/>
              <path d="M17 3h2a2 2 0 0 1 2 2v2"/>
              <path d="M21 17v2a2 2 0 0 1-2 2h-2"/>
              <path d="M7 21H5a2 2 0 0 1-2-2v-2"/>
            </svg>
            <span>Escanear</span>
          </button>
        </div>

        <button type="button" class="btn-submit-search">
          <svg viewBox="0 0 24 24" class="btn-search-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <span>Consultar Base Diagnóstica [Enter]</span>
        </button>
      </div>

      <div v-if="scanFeedback" class="scan-feedback-banner">
        <svg viewBox="0 0 24 24" class="feedback-svg" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <span>{{ scanFeedback }}</span>
      </div>
    </div>

    <!-- CONTENIDO EN DOS COLUMNAS -->
    <div class="busqueda-grid">
      <!-- COLUMNA PRINCIPAL IZQUIERDA: DETALLE DEL ESTUDIO ENCONTRADO + CADENA DE CUSTODIA -->
      <div class="busqueda-main-col">
        <!-- ALERTA DE ESTUDIO CRÍTICO -->
        <div class="critical-found-card">
          <div class="critical-header-bar">
            <div class="critical-left">
              <svg viewBox="0 0 24 24" class="crit-pulse-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>ESTUDIO CRÍTICO LOCALIZADO • PROTOCOLO DE URGENCIA CHOQUE ACTIVO (STAT)</span>
            </div>
            <span class="sla-badge">TIEMPO RESTANTE SLA: 14 MIN</span>
          </div>

          <!-- DETALLE PRINCIPAL DEL ESTUDIO -->
          <div class="study-found-detail">
            <div class="study-flask-box">
              <svg viewBox="0 0 24 24" class="flask-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
                <line x1="8.5" y1="2" x2="15.5" y2="2"/>
              </svg>
            </div>

            <div class="study-found-info">
              <div class="folio-priority-row">
                <span class="found-folio">{{ activeStudy.folio }}</span>
                <span class="badge-red-stat">STAT CÓDIGO ROJO</span>
              </div>
              <div class="patient-id-row">
                <span class="patient-nom">
                  Identificador NOM-024: <strong>{{ activeStudy.paciente_iniciales }}</strong> ({{ activeStudy.paciente_edad }} años, Femenino)
                </span>
                <span class="barcode-pill">
                  <svg viewBox="0 0 24 24" class="barcode-mini-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="4" y1="4" x2="4" y2="20"/>
                    <line x1="8" y1="4" x2="8" y2="20"/>
                    <line x1="12" y1="4" x2="12" y2="20"/>
                    <line x1="16" y1="4" x2="16" y2="20"/>
                    <line x1="20" y1="4" x2="20" y2="20"/>
                  </svg>
                  BC-HEP-88190
                </span>
              </div>
            </div>

            <div class="study-location-card">
              <div class="loc-header">
                <svg viewBox="0 0 24 24" class="loc-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                </svg>
                <strong class="loc-title">Urgencias Adultos • Box 01 (Choque)</strong>
              </div>
              <span class="loc-doc">Médico Tratante: Dr. Carlos Mendoza P.</span>
              <span class="loc-time">Recepción de Solicitud: 10:35:12 h</span>
            </div>
          </div>

          <!-- TRAZABILIDAD INTEGRAL DE LA MUESTRA (CADENA DE CUSTODIA LIS EN 4 FASES) -->
          <div class="custody-chain-section">
            <h4 class="custody-title">TRAZABILIDAD INTEGRAL DE LA MUESTRA [CADENA DE CUSTODIA LIS]</h4>
            <div class="custody-steps-row">
              <div class="custody-step completed">
                <div class="step-circle">
                  <svg viewBox="0 0 24 24" class="check-step-svg" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
                <div class="step-meta">
                  <strong>1. Orden Recibida</strong>
                  <span>10:35 h • Urgencias Box 01</span>
                </div>
              </div>

              <div class="custody-connector active"></div>

              <div class="custody-step completed">
                <div class="step-circle">
                  <svg viewBox="0 0 24 24" class="check-step-svg" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
                <div class="step-meta">
                  <strong>2. Muestra en Laboratorio</strong>
                  <span>10:41 h • Q.F.B. Mariana T.</span>
                </div>
              </div>

              <div class="custody-connector active"></div>

              <div class="custody-step in-progress">
                <div class="step-circle spinning">
                  <svg viewBox="0 0 24 24" class="spin-step-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                </div>
                <div class="step-meta">
                  <strong>3. En Procesamiento</strong>
                  <span>Equipo Cobas b221 & e411</span>
                </div>
              </div>

              <div class="custody-connector"></div>

              <div class="custody-step pending">
                <div class="step-circle pending-circle">4</div>
                <div class="step-meta">
                  <strong>4. Validación y Carga</strong>
                  <span>Pendiente de captura</span>
                </div>
              </div>
            </div>
          </div>

          <!-- TABLA DE DETERMINACIONES ANALÍTICAS -->
          <div class="analytics-table-wrap">
            <table class="analytics-table">
              <thead>
                <tr>
                  <th>CÓDIGO PRUEBA</th>
                  <th>DETERMINACIÓN ANALÍTICA</th>
                  <th>MUESTRA REQUERIDA</th>
                  <th>VALOR DE REFERENCIA INSTITUCIONAL</th>
                  <th>ESTADO</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="code-cell">GAS-ART-01</td>
                  <td>
                    <strong>Gasometría Arterial Completa</strong>
                    <span class="sub-analyte">pH, pCO2, pO2, HCO3-, Lactato, SatO2</span>
                  </td>
                  <td>Jeringa Heparinizada (Sangre Arterial)</td>
                  <td class="ref-cell">pH: 7.35 - 7.45 • Lac &lt; 2.0 mmol/L</td>
                  <td>
                    <span class="status-chip blue">En Analizador</span>
                  </td>
                </tr>
                <tr>
                  <td class="code-cell">TROP-US-02</td>
                  <td>
                    <strong>Troponina I de Alta Sensibilidad (hs-cTnI)</strong>
                    <span class="sub-analyte">Cuantificación urgente por quimioluminiscencia</span>
                  </td>
                  <td>Suero / Gel Separador (Tubo Amarillo)</td>
                  <td class="ref-cell">P99 &lt; 14.0 ng/L</td>
                  <td>
                    <span class="status-chip orange">En Espera de Suero</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- BOTONES DE ACCIÓN RÁPIDA -->
          <div class="found-actions-row">
            <div class="actions-left">
              <button
                type="button"
                class="btn-action-primary"
                @click="emit('cargar-estudio', activeStudy)"
              >
                <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                <span>Cargar Resultados de este Folio [Alt+C]</span>
              </button>

              <button
                type="button"
                class="btn-action-outline"
                @click="emit('imprimir-etiqueta', activeStudy.folio)"
              >
                <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="6 9 6 2 18 2 18 9"/>
                  <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
                  <rect x="6" y="14" width="12" height="8"/>
                </svg>
                <span>Imprimir Etiquetas Tubo</span>
              </button>
            </div>

            <button
              type="button"
              class="btn-action-danger"
              @click="emit('reportar-incidencia', activeStudy.folio)"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <span>Reportar Incidencia de Muestra (Hemólisis / Coágulo)</span>
            </button>
          </div>
        </div>

        <!-- TABLA HISTORIAL RECIENTE DE CONSULTAS -->
        <div class="recent-history-card">
          <div class="recent-header">
            <svg viewBox="0 0 24 24" class="recent-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 14 14"/>
            </svg>
            <div class="recent-title-wrap">
              <h4>Historial Reciente de Consultas (Turno Matutino)</h4>
              <span class="recent-sub">Mostrando las últimas 4 consultas de la sesión</span>
            </div>
          </div>

          <table class="recent-table">
            <thead>
              <tr>
                <th>FOLIO</th>
                <th>PACIENTE (PRIVACIDAD)</th>
                <th>ESTUDIO SOLICITADO</th>
                <th>SERVICIO ORIGEN</th>
                <th>HORA</th>
                <th>ESTADO DEL ESTUDIO</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="folio-code">EST-2026-9040</td>
                <td>J.L.P. (68a)</td>
                <td>Biometría Hemática + Tiempos (TP/TTP)</td>
                <td>Triage • Sillón 04</td>
                <td>10:44 h</td>
                <td><span class="status-chip blue">En Proceso (Sysmex)</span></td>
              </tr>
              <tr>
                <td class="folio-code">EST-2026-9038</td>
                <td>M.A.V. (29a)</td>
                <td>EGO + Urocultivo Automatizado</td>
                <td>Consulta Externa Ginecología</td>
                <td>10:28 h</td>
                <td><span class="status-chip green">Resultados Cargados</span></td>
              </tr>
              <tr>
                <td class="folio-code">EST-2026-9035</td>
                <td>R.G.H. (55a)</td>
                <td>Perfil Lipídico + Química 24</td>
                <td>Hospitalización Piso 2 Cama 214</td>
                <td>10:15 h</td>
                <td><span class="status-chip green">Validado / Notificado</span></td>
              </tr>
              <tr>
                <td class="folio-code">EST-2026-9031</td>
                <td>F.D.S. (11a)</td>
                <td>Radiografía de Tórax AP y Lateral</td>
                <td>Urgencias Pediátricas</td>
                <td>09:50 h</td>
                <td><span class="status-chip purple">PACS / DICOM Listo</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- COLUMNA LATERAL DERECHA: PERIFÉRICOS, INTERCOM Y MARCO LEGAL -->
      <div class="busqueda-side-col">
        <!-- PERIFÉRICOS DIAGNÓSTICOS -->
        <div class="side-panel-box">
          <div class="side-panel-header">
            <div class="side-h-left">
              <svg viewBox="0 0 24 24" class="side-panel-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <line x1="7" y1="3" x2="7" y2="21"/>
                <line x1="17" y1="3" x2="17" y2="21"/>
              </svg>
              <h4>PERIFÉRICOS DIAGNÓSTICOS</h4>
            </div>
            <span class="dot-online"></span>
          </div>

          <div class="side-panel-body">
            <div class="device-card">
              <div class="device-meta">
                <strong class="device-name">Lector 2D / GS1 DataBar</strong>
                <span class="device-sub">Respuesta &lt; 50ms • Calibrado</span>
              </div>
            </div>

            <div class="device-specs">
              <div class="spec-row">
                <span class="spec-lbl">Sensibilidad Láser:</span>
                <span class="spec-val">Alta (Lectura en tubo curvo)</span>
              </div>
              <div class="spec-row">
                <span class="spec-lbl">Simbologías activas:</span>
                <span class="spec-val">Code 128, DataMatrix, QR</span>
              </div>
            </div>

            <button type="button" class="btn-test-scanner" @click="handleTestLector">
              <svg viewBox="0 0 24 24" class="test-svg" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="4" y1="21" x2="4" y2="14"/>
                <line x1="4" y1="10" x2="4" y2="3"/>
                <line x1="12" y1="21" x2="12" y2="12"/>
                <line x1="12" y1="8" x2="12" y2="3"/>
                <line x1="20" y1="21" x2="20" y2="16"/>
                <line x1="20" y1="12" x2="20" y2="3"/>
                <line x1="1" y1="14" x2="7" y2="14"/>
                <line x1="9" y1="8" x2="15" y2="8"/>
                <line x1="17" y1="16" x2="23" y2="16"/>
              </svg>
              <span>Test de Lectura de Tubo</span>
            </button>
          </div>
        </div>

        <!-- INTERCOM RÁPIDO A URGENCIAS -->
        <div class="side-panel-box">
          <div class="side-panel-header">
            <div class="side-h-left">
              <svg viewBox="0 0 24 24" class="side-panel-svg red" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
              </svg>
              <h4>Intercom Rápido a Urgencias</h4>
            </div>
          </div>

          <div class="side-panel-body">
            <p class="intercom-intro">Comunicación prioritaria para notificación de valores críticos de pánico:</p>

            <div class="intercom-list">
              <div class="intercom-item urgent">
                <div class="intercom-meta">
                  <strong>Urgencias Choque Box 01-03</strong>
                  <span>Dr. Mendoza / Dra. Ríos</span>
                </div>
                <div class="intercom-ext danger">Ext. 2101</div>
              </div>

              <div class="intercom-item">
                <div class="intercom-meta">
                  <strong>Triage Primer Contacto</strong>
                  <span>Enfermería General</span>
                </div>
                <div class="intercom-ext">Ext. 2105</div>
              </div>

              <div class="intercom-item">
                <div class="intercom-meta">
                  <strong>Hospitalización Piso 2</strong>
                  <span>Central de Enfermería</span>
                </div>
                <div class="intercom-ext">Ext. 2204</div>
              </div>
            </div>
          </div>
        </div>

        <!-- MARCO LEGAL Y CUSTODIA -->
        <div class="side-panel-box legal-box">
          <div class="side-panel-header">
            <div class="side-h-left">
              <svg viewBox="0 0 24 24" class="side-panel-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <h4>MARCO LEGAL Y CUSTODIA</h4>
            </div>
          </div>

          <div class="side-panel-body">
            <p class="legal-paragraph">
              Esta terminal opera bajo aislamiento estricto de expediente clínico conforme a la norma <strong>NOM-024-SSA3-2012</strong> y <strong>NOM-007-SSA3-2011</strong>. Queda registrado el identificador de sesión y cédula profesional para cada consulta diagnóstica efectuada.
            </p>
            <div class="legal-footer-row">
              <span class="session-lbl">Sesión activa: #SES-9941</span>
              <span class="traz-badge">Trazabilidad OK</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-busqueda-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.search-header-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.search-header-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 65%;
}

.header-tags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
  color: #64748b;
}

.badge-nom {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.terminal-info {
  font-weight: 600;
  color: #334155;
}

.sync-info {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: #166534;
}

.dot-green {
  width: 7px;
  height: 7px;
  background-color: #16a34a;
  border-radius: 50%;
}

.sep {
  color: #cbd5e1;
}

.search-main-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.search-main-desc {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.search-header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.scanner-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
}

.dot-scanner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #0284c7;
  box-shadow: 0 0 6px rgba(2, 132, 199, 0.5);
}

.scanner-meta {
  display: flex;
  flex-direction: column;
}

.scanner-meta strong {
  font-size: 0.72rem;
  color: #0f172a;
}

.scanner-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.btn-guide-f1 {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
}

.guide-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.search-bar-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.search-mode-tabs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.mode-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mode-btn:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.mode-btn.active {
  background-color: #0284c7;
  border-color: #0284c7;
  color: #ffffff;
}

.mode-svg {
  width: 14px;
  height: 14px;
}

.search-input-composite {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 0.75rem;
}

.input-wrap {
  display: flex;
  align-items: center;
  position: relative;
}

.search-input-svg {
  position: absolute;
  left: 12px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
}

.main-search-input {
  width: 100%;
  padding: 0.55rem 6.5rem 0.55rem 2.25rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.82rem;
  color: #0f172a;
  outline: none;
  background-color: #f8fafc;
}

.main-search-input:focus {
  border-color: #0284c7;
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.btn-scan-trigger {
  position: absolute;
  right: 6px;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
}

.scan-mini-svg {
  width: 13px;
  height: 13px;
}

.btn-submit-search {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.btn-submit-search:hover {
  background-color: #0369a1;
}

.btn-search-svg {
  width: 15px;
  height: 15px;
}

.scan-feedback-banner {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
}

.feedback-svg {
  width: 14px;
  height: 14px;
}

.busqueda-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.25rem;
}

.busqueda-main-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.critical-found-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.critical-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #7f1d1d;
  color: #ffffff;
  padding: 0.55rem 1rem;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.critical-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.crit-pulse-svg {
  width: 16px;
  height: 16px;
  color: #fca5a5;
  animation: pulseCrit 1.6s infinite;
}

@keyframes pulseCrit {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.sla-badge {
  background-color: rgba(0, 0, 0, 0.35);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.68rem;
  color: #fef08a;
}

.study-found-detail {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
  align-items: center;
}

.study-flask-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background-color: #fee2e2;
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flask-svg {
  width: 22px;
  height: 22px;
}

.study-found-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.folio-priority-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.found-folio {
  font-family: monospace;
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
}

.badge-red-stat {
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.patient-id-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #475569;
}

.barcode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.68rem;
  color: #0f172a;
}

.barcode-mini-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.study-location-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.6rem 0.75rem;
  border-radius: 6px;
}

.loc-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #0369a1;
}

.loc-svg {
  width: 14px;
  height: 14px;
}

.loc-title {
  font-size: 0.76rem;
}

.loc-doc, .loc-time {
  font-size: 0.68rem;
  color: #64748b;
}

.custody-chain-section {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid #e2e8f0;
  background-color: #f8fafc;
}

.custody-title {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #475569;
  margin: 0 0 0.85rem 0;
}

.custody-steps-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.custody-step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.step-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #16a34a;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 700;
}

.custody-step.in-progress .step-circle {
  background-color: #0284c7;
}

.spinning svg {
  animation: spinCustody 2s linear infinite;
}

@keyframes spinCustody {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.pending-circle {
  background-color: #cbd5e1;
  color: #475569;
}

.check-step-svg, .spin-step-svg {
  width: 14px;
  height: 14px;
}

.step-meta {
  display: flex;
  flex-direction: column;
}

.step-meta strong {
  font-size: 0.72rem;
  color: #0f172a;
}

.step-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.custody-connector {
  flex: 1;
  height: 2px;
  background-color: #cbd5e1;
  margin: 0 0.5rem;
}

.custody-connector.active {
  background-color: #16a34a;
}

.analytics-table-wrap {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.analytics-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.74rem;
}

.analytics-table th {
  text-align: left;
  font-size: 0.66rem;
  font-weight: 700;
  color: #64748b;
  padding: 0.5rem;
  border-bottom: 1px solid #e2e8f0;
}

.analytics-table td {
  padding: 0.65rem 0.5rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.code-cell {
  font-family: monospace;
  font-weight: 700;
  color: #0284c7;
}

.sub-analyte {
  display: block;
  font-size: 0.66rem;
  color: #64748b;
}

.ref-cell {
  font-family: monospace;
  font-size: 0.72rem;
}

.status-chip {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.66rem;
  font-weight: 700;
}

.status-chip.blue {
  background-color: #e0f2fe;
  color: #0369a1;
}

.status-chip.orange {
  background-color: #ffedd5;
  color: #c2410c;
}

.status-chip.green {
  background-color: #f0fdf4;
  color: #15803d;
}

.status-chip.purple {
  background-color: #f3e8ff;
  color: #7e22ce;
}

.found-actions-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  background-color: #f8fafc;
}

.actions-left {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0284c7;
  color: #ffffff;
  padding: 0.5rem 0.95rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.btn-action-primary:hover {
  background-color: #0369a1;
}

.btn-action-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-danger {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #fee2e2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.recent-history-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.recent-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.recent-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.recent-title-wrap h4 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.recent-sub {
  font-size: 0.65rem;
  color: #64748b;
}

.recent-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.74rem;
}

.recent-table th {
  text-align: left;
  font-size: 0.66rem;
  font-weight: 700;
  color: #64748b;
  padding: 0.45rem 0.5rem;
  border-bottom: 1px solid #e2e8f0;
}

.recent-table td {
  padding: 0.55rem 0.5rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.folio-code {
  font-family: monospace;
  font-weight: 700;
  color: #0284c7;
}

.busqueda-side-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.side-panel-box {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.side-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.side-h-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.side-panel-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.side-panel-svg.red {
  color: #b91c1c;
}

.side-h-left h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  letter-spacing: 0.03em;
}

.dot-online {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #16a34a;
  box-shadow: 0 0 6px rgba(22, 163, 74, 0.4);
}

.side-panel-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.device-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
}

.device-meta {
  display: flex;
  flex-direction: column;
}

.device-name {
  font-size: 0.76rem;
  color: #0f172a;
}

.device-sub {
  font-size: 0.65rem;
  color: #64748b;
}

.device-specs {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.7rem;
}

.spec-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spec-lbl {
  color: #64748b;
}

.spec-val {
  font-weight: 600;
  color: #0f172a;
}

.btn-test-scanner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 0.45rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
}

.btn-test-scanner:hover {
  background-color: #e2e8f0;
}

.test-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.intercom-intro {
  font-size: 0.7rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.intercom-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.intercom-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
}

.intercom-item.urgent {
  background-color: #fef2f2;
  border-color: #fecaca;
}

.intercom-meta {
  display: flex;
  flex-direction: column;
}

.intercom-meta strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.intercom-meta span {
  font-size: 0.65rem;
  color: #64748b;
}

.intercom-ext {
  font-family: monospace;
  font-size: 0.78rem;
  font-weight: 800;
  color: #0284c7;
  background-color: #e0f2fe;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}

.intercom-ext.danger {
  color: #b91c1c;
  background-color: #fee2e2;
}

.legal-paragraph {
  font-size: 0.7rem;
  color: #475569;
  line-height: 1.35;
  margin: 0;
}

.legal-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.65rem;
  margin-top: 0.35rem;
}

.session-lbl {
  color: #64748b;
  font-family: monospace;
}

.traz-badge {
  background-color: #f0fdf4;
  color: #16a34a;
  font-weight: 700;
  padding: 0.15rem 0.35rem;
  border-radius: 4px;
}
</style>
