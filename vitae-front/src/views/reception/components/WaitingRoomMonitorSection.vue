<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(
  defineProps<{
    horaActual?: string
  }>(),
  {
    horaActual: '20:36:26'
  }
)

type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: SeccionRecepcion): void
}>()

const mostrarAlerta = (mensaje: string) => {
  alert(mensaje)
}

// =========================================================================
// PANTALLA 5: MONITOR GENERAL DE SALA DE ESPERA Y TURNOS (CONFORME A CAPTURA)
// =========================================================================
interface TurnoPresencial {
  folio: string
  iniciales: string
  llegada: string
  espera: string
  esperaMin: number
  alertaEspera?: boolean
  destino: string
  categoria: 'admision' | 'triage' | 'caja'
  estatus: 'En Ventanilla' | 'En Espera'
  modulo: string
  responsable?: string
}

const filtroTurnos = ref<'todos' | 'admision' | 'triage' | 'caja'>('todos')
const altavozConectado = ref(true)
const hdmiActivo = ref(true)

// Lista de turnos presenciales pre-cargada conforme a la captura oficial
const turnosPresenciales = ref<TurnoPresencial[]>([
  {
    folio: 'FOL-9482',
    iniciales: 'M.R.',
    llegada: '10:18 h',
    espera: '30 min',
    esperaMin: 30,
    alertaEspera: false,
    destino: 'Módulo 01 (Admisión)',
    categoria: 'admision',
    estatus: 'En Ventanilla',
    modulo: 'Módulo 01',
    responsable: 'Lic. Morales'
  },
  {
    folio: 'FOL-9483',
    iniciales: 'A.P.',
    llegada: '10:24 h',
    espera: '24 min',
    esperaMin: 24,
    alertaEspera: true,
    destino: 'Módulo 02 (Admisión)',
    categoria: 'admision',
    estatus: 'En Espera',
    modulo: 'Módulo 02',
    responsable: 'Lic. Estrada'
  },
  {
    folio: 'FOL-9484',
    iniciales: 'J.G.',
    llegada: '10:31 h',
    espera: '17 min',
    esperaMin: 17,
    destino: 'Triage Respiratorio',
    categoria: 'triage',
    estatus: 'En Espera',
    modulo: 'Triage 01'
  },
  {
    folio: 'FOL-9485',
    iniciales: 'L.H.',
    llegada: '10:35 h',
    espera: '13 min',
    esperaMin: 13,
    destino: 'Ventanilla Caja 01',
    categoria: 'caja',
    estatus: 'En Espera',
    modulo: 'Caja 01'
  },
  {
    folio: 'FOL-9486',
    iniciales: 'S.C.',
    llegada: '10:39 h',
    espera: '9 min',
    esperaMin: 9,
    destino: 'Módulo 03 (Admisión)',
    categoria: 'admision',
    estatus: 'En Espera',
    modulo: 'Módulo 03'
  },
  {
    folio: 'FOL-9487',
    iniciales: 'V.N.',
    llegada: '10:41 h',
    espera: '7 min',
    esperaMin: 7,
    destino: 'Triage Adultos',
    categoria: 'triage',
    estatus: 'En Espera',
    modulo: 'Triage 02'
  },
  {
    folio: 'FOL-9488',
    iniciales: 'E.D.',
    llegada: '10:44 h',
    espera: '4 min',
    esperaMin: 4,
    destino: 'Módulo 04 (Seguros)',
    categoria: 'admision',
    estatus: 'En Espera',
    modulo: 'Módulo 04'
  },
  {
    folio: 'FOL-9489',
    iniciales: 'R.Z.',
    llegada: '10:46 h',
    espera: '2 min',
    esperaMin: 2,
    destino: 'Ventanilla Caja 02',
    categoria: 'caja',
    estatus: 'En Espera',
    modulo: 'Caja 02'
  }
])

const turnoActualLlamado = ref<TurnoPresencial | null>(turnosPresenciales.value[0] || null)
const siguienteTurno = ref<TurnoPresencial | null>(turnosPresenciales.value[1] || null)

const turnosFiltrados = computed(() => {
  if (filtroTurnos.value === 'todos') return turnosPresenciales.value
  return turnosPresenciales.value.filter(t => t.categoria === filtroTurnos.value)
})

const conteoPorCategoria = (cat: 'todos' | 'admision' | 'triage' | 'caja') => {
  if (cat === 'todos') return turnosPresenciales.value.length
  return turnosPresenciales.value.filter(t => t.categoria === cat).length
}

const llamarSiguienteTurnoFila = () => {
  const enEspera = turnosPresenciales.value.find(t => t.estatus === 'En Espera')
  if (enEspera) {
    if (turnoActualLlamado.value) {
      turnoActualLlamado.value.estatus = 'En Espera'
    }
    enEspera.estatus = 'En Ventanilla'
    turnoActualLlamado.value = enEspera
    const next = turnosPresenciales.value.find(t => t !== enEspera && t.estatus === 'En Espera')
    siguienteTurno.value = next || null
    mostrarAlerta(`📢 Llamando turno ${enEspera.folio} (${enEspera.iniciales}) a ${enEspera.destino}. Altavoz activado en sala.`)
  } else {
    mostrarAlerta('No hay más turnos en espera para llamar.')
  }
}

const reemitirAltavoz = () => {
  if (!turnoActualLlamado.value) {
    mostrarAlerta('No hay turno actual en pantalla para re-emitir.')
    return
  }
  mostrarAlerta(`🔊 Re-emitiendo por altavoz HDMI-01: "Turno ${turnoActualLlamado.value.folio}, iniciales ${turnoActualLlamado.value.iniciales}, favor de pasar a ${turnoActualLlamado.value.destino}".`)
}

const marcarAusenteTurnoActual = () => {
  if (!turnoActualLlamado.value) {
    mostrarAlerta('No hay turno activo para marcar como ausente.')
    return
  }
  const folio = turnoActualLlamado.value.folio
  turnosPresenciales.value = turnosPresenciales.value.filter(t => t.folio !== folio)
  turnoActualLlamado.value = siguienteTurno.value || null
  if (turnoActualLlamado.value) {
    turnoActualLlamado.value.estatus = 'En Ventanilla'
  }
  const next = turnosPresenciales.value.find(t => t.estatus === 'En Espera')
  siguienteTurno.value = next || null
  mostrarAlerta(`🚫 Turno ${folio} marcado como ausente y retirado de la fila de atención.`)
}

const llamarTurnoEspecifico = (turno: TurnoPresencial) => {
  turnosPresenciales.value.forEach(t => {
    if (t.estatus === 'En Ventanilla') t.estatus = 'En Espera'
  })
  turno.estatus = 'En Ventanilla'
  turnoActualLlamado.value = turno
  mostrarAlerta(`📢 Proyectando turno ${turno.folio} (${turno.iniciales}) en simulador TV y altavoz principal.`)
}

const transferirTurno = (turno: TurnoPresencial) => {
  mostrarAlerta(`➡️ Seleccione módulo destino para transferir el turno ${turno.folio} (${turno.iniciales}): Admisión, Triage o Trabajo Social.`)
}

const imprimirTicketTurno = (turno: TurnoPresencial) => {
  mostrarAlerta(`🖨️ Imprimiendo ticket térmico de turno ${turno.folio} con código QR y estimado de espera.`)
}

const probarCampanilla = () => {
  mostrarAlerta('🔔 Ding-dong! Prueba de audio del altavoz de sala ejecutada en canal HDMI-01.')
}
</script>

<template>
  <div class="view-monitor-container">
    
    <!-- 1. Encabezado del Monitor -->
    <div class="section-top-banner">
      <div class="banner-left-wrap">
        <div class="banner-monitor-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
            <path d="M21 3H3c-1.11 0-2 .89-2 2v12c0 1.1.89 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.11-.9-2-2-2zm0 14H3V5h18v12z"/>
          </svg>
        </div>
        <div class="banner-text-col">
          <div class="banner-title-row">
            <h2>Monitor General de Sala de Espera y Turnos Presenciales</h2>
            <span class="pill-turnador">TURNADOR DIGITAL V4.2</span>
          </div>
          <div class="banner-privacy-notice">
            <span class="shield-icon">🛡️</span>
            <p>Protección de Privacidad: La pantalla pública y los monitores proyectan únicamente Folio e Iniciales. Prohibido mostrar diagnósticos clínicos o motivos de consulta (NOM-024 / Ley de Privacidad).</p>
          </div>
        </div>
      </div>

      <div class="banner-right-kpis-row">
        <div class="hdmi-sync-badge">
          <span class="dot-hdmi"></span>
          <span>Sincronización HDMI-01 Activa</span>
        </div>
        <div class="speaker-status-box">
          <span>📢 Altavoz de Sala: <strong>Conectado</strong></span>
        </div>
      </div>
    </div>

    <!-- 2. Cuadrícula Superior de 4 KPIs de Flujo -->
    <div class="top-kpi-grid">
      
      <!-- KPI 1: PACIENTES EN SALA -->
      <div class="kpi-card">
        <div class="kpi-content-col">
          <span class="kpi-label">PACIENTES EN SALA</span>
          <div class="kpi-main-val">
            <strong class="big-kpi-num">{{ turnosPresenciales.length }}</strong>
            <span class="kpi-num-sub">turnos activos</span>
          </div>
          <small class="kpi-foot-sub">Aforo: 23% ocupación</small>
        </div>
        <div class="kpi-icon-square blue">
          👥
        </div>
      </div>

      <!-- KPI 2: TIEMPO PROMEDIO ESPERA -->
      <div class="kpi-card">
        <div class="kpi-content-col">
          <span class="kpi-label">TIEMPO PROMEDIO ESPERA</span>
          <div class="kpi-main-val">
            <strong class="big-kpi-num">14</strong>
            <span class="kpi-num-sub">minutos</span>
          </div>
          <small class="kpi-foot-sub text-blue">Meta de servicio &lt; 20 min</small>
        </div>
        <div class="kpi-icon-square cyan">
          ⏱️
        </div>
      </div>

      <!-- KPI 3: TURNO EN VENTANILLA -->
      <div class="kpi-card">
        <div class="kpi-content-col">
          <span class="kpi-label">TURNO EN VENTANILLA</span>
          <div class="kpi-main-val">
            <strong class="big-kpi-folio blue">{{ turnoActualLlamado ? turnoActualLlamado.folio : 'FOL-9482' }}</strong>
            <span class="initials-badge">{{ turnoActualLlamado ? turnoActualLlamado.iniciales : 'M.R.' }}</span>
          </div>
          <small class="kpi-foot-sub">En Módulo 01 · Lic. Morales</small>
        </div>
        <div class="kpi-icon-square blue-solid">
          👤
        </div>
      </div>

      <!-- KPI 4: SIGUIENTE EN FILA -->
      <div class="kpi-card">
        <div class="kpi-content-col">
          <span class="kpi-label">SIGUIENTE EN FILA</span>
          <div class="kpi-main-val">
            <strong class="big-kpi-folio dark">{{ siguienteTurno ? siguienteTurno.folio : 'FOL-9483' }}</strong>
            <span class="initials-badge">{{ siguienteTurno ? siguienteTurno.iniciales : 'A.P.' }}</span>
          </div>
          <small class="kpi-foot-sub">Esperando llamado [Alt+S]</small>
        </div>
        <div class="kpi-icon-square gray">
          ↪
        </div>
      </div>

    </div>

    <!-- 3. Barra de Operaciones de Control de Llamadas y Filtros -->
    <div class="queue-control-bar">
      <div class="queue-action-buttons">
        <button type="button" class="btn-call-next-hero" @click="llamarSiguienteTurnoFila">
          <span>📢 Llamar Siguiente Turno</span>
          <kbd class="kbd-space">Espacio</kbd>
        </button>
        <button type="button" class="btn-action-outline" @click="reemitirAltavoz">
          <span>🔊 Re-emitir por Altavoz</span>
        </button>
        <button type="button" class="btn-action-outline" @click="marcarAusenteTurnoActual">
          <span>🚫 Marcar Ausente</span>
        </button>
      </div>

      <div class="tabs-filter-container">
        <button 
          type="button" 
          :class="['tab-pill-btn', { active: filtroTurnos === 'todos' }]" 
          @click="filtroTurnos = 'todos'"
        >
          Todos ({{ conteoPorCategoria('todos') }})
        </button>
        <button 
          type="button" 
          :class="['tab-pill-btn', { active: filtroTurnos === 'admision' }]" 
          @click="filtroTurnos = 'admision'"
        >
          Módulos de Admisión ({{ conteoPorCategoria('admision') }})
        </button>
        <button 
          type="button" 
          :class="['tab-pill-btn', { active: filtroTurnos === 'triage' }]" 
          @click="filtroTurnos = 'triage'"
        >
          Espera de Triage ({{ conteoPorCategoria('triage') }})
        </button>
        <button 
          type="button" 
          :class="['tab-pill-btn', { active: filtroTurnos === 'caja' }]" 
          @click="filtroTurnos = 'caja'"
        >
          Caja y Der...
        </button>
      </div>
    </div>

    <!-- 4. Cuadrícula Principal (Tabla de Turnos a la Izquierda + Simulador TV y Telemetría a la Derecha) -->
    <div class="curp-layout-grid">
      
      <!-- SUBCOLUMNA IZQUIERDA: Tabla de Turnos Presenciales -->
      <div class="curp-left-col">
        
        <div class="step-card">
          <div class="step-card-header">
            <div class="header-title-flex">
              <span class="icon-menu-list">📑</span>
              <h4>Turnos Presenciales en Proceso</h4>
              <span class="badge-count-patients">{{ turnosFiltrados.length }} pacientes</span>
            </div>
            <div class="legend-indicators-row">
              <span class="legend-item"><span class="legend-dot red"></span> Alerta > 20 min</span>
              <span class="legend-sep">•</span>
              <span class="legend-item"><span class="legend-dot blue"></span> En Atención</span>
            </div>
          </div>

          <div class="step-body no-padding">
            <div class="table-responsive">
              <table class="tickets-table">
                <thead>
                  <tr>
                    <th>TURNO / FOLIO</th>
                    <th>INICIALES</th>
                    <th>LLEGADA</th>
                    <th>ESPERA</th>
                    <th>DESTINO / MÓDULO</th>
                    <th>ESTATUS</th>
                    <th class="text-right">ACCIONES OPERATIVAS</th>
                  </tr>
                </thead>
                <tbody>
                  <tr 
                    v-for="(turno, idx) in turnosFiltrados" 
                    :key="idx"
                    :class="{ 'row-active-call': turno.estatus === 'En Ventanilla' }"
                  >
                    <!-- Columna Folio con barra de acento -->
                    <td class="col-folio">
                      <div class="folio-accent-wrapper">
                        <span 
                          class="accent-bar" 
                          :class="{
                            blue: turno.estatus === 'En Ventanilla',
                            red: turno.alertaEspera && turno.estatus !== 'En Ventanilla',
                            neutral: !turno.alertaEspera && turno.estatus !== 'En Ventanilla'
                          }"
                        ></span>
                        <strong class="folio-text">{{ turno.folio }}</strong>
                      </div>
                    </td>

                    <!-- Iniciales -->
                    <td class="col-initials">
                      <span class="initials-chip">{{ turno.iniciales }}</span>
                    </td>

                    <!-- Llegada -->
                    <td class="col-llegada">{{ turno.llegada }}</td>

                    <!-- Espera -->
                    <td class="col-espera">
                      <span v-if="turno.alertaEspera" class="wait-alert-pill">
                        ⚠️ {{ turno.espera }}
                      </span>
                      <span v-else-if="turno.estatus === 'En Ventanilla'" class="wait-text-blue">
                        {{ turno.espera }}
                      </span>
                      <span v-else class="wait-text-muted">
                        {{ turno.espera }}
                      </span>
                    </td>

                    <!-- Destino / Módulo -->
                    <td class="col-destino">
                      <strong>{{ turno.destino }}</strong>
                    </td>

                    <!-- Estatus -->
                    <td class="col-status">
                      <span 
                        v-if="turno.estatus === 'En Ventanilla'" 
                        class="status-pill-blue"
                      >
                        👥 En Ventanilla
                      </span>
                      <span 
                        v-else 
                        class="status-pill-gray"
                      >
                        ⏳ En Espera
                      </span>
                    </td>

                    <!-- Acciones -->
                    <td class="text-right col-actions">
                      <div class="ticket-action-btns-row">
                        <!-- Botón Llamar -->
                        <button 
                          v-if="turno.estatus === 'En Ventanilla'"
                          type="button" 
                          class="btn-icon-subtle"
                          @click="reemitirAltavoz"
                          title="Re-llamar por altavoz"
                        >
                          📢
                        </button>
                        <button 
                          v-else
                          type="button" 
                          class="btn-call-inline"
                          @click="llamarTurnoEspecifico(turno)"
                          title="Proyectar turno en pantalla y altavoz"
                        >
                          📢 Llamar
                        </button>

                        <!-- Botón Transferir -->
                        <button 
                          type="button" 
                          class="btn-icon-subtle"
                          @click="transferirTurno(turno)"
                          title="Transferir a otro módulo"
                        >
                          <span v-if="idx < 2">📥</span>
                          <span v-else>↔️</span>
                        </button>

                        <!-- Botón Reimprimir Ticket -->
                        <button 
                          type="button" 
                          class="btn-icon-subtle"
                          @click="imprimirTicketTurno(turno)"
                          title="Reimprimir ticket térmico"
                        >
                          🖨️
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Pie Informativo de Telemetría -->
        <div class="monitor-footer-bar">
          <div class="footer-left-sync">
            <span class="sync-icon">🔄</span>
            <span>Actualización automática cada 15 segundos con servidor de llamadas.</span>
          </div>
          <div class="footer-right-f11">
            <span>Modo de pantalla completa:</span>
            <kbd class="kbd-f11">⛶ F11 TV</kbd>
          </div>
        </div>

      </div>

      <!-- SUBCOLUMNA DERECHA: Simulador TV Sala, Aforo y CCTV -->
      <aside class="curp-right-col">
        
        <!-- 1. Simulador TV Sala (HDMI-01) -->
        <div class="tv-simulator-frame">
          <div class="tv-bezel-header">
            <div class="tv-title-row">
              <span class="tv-screen-icon">📺</span>
              <strong>SIMULADOR TV SALA (HDMI-01)</strong>
            </div>
            <span class="tv-res-badge">● 1080p @ 60Hz</span>
          </div>

          <!-- Pantalla TV Realista Oscura -->
          <div class="tv-screen-bezel">
            <div class="tv-screen-inner">
              <div class="tv-brand-row">
                <div class="tv-vitae-brand">
                  <div class="vitae-mini-box">✚</div>
                  <span>HOSPITAL VITAE</span>
                </div>
                <div class="tv-live-clock">{{ horaActual }}</div>
              </div>

              <div class="tv-main-callout">
                <span class="tv-callout-label">TURNO ACTUAL EN LLAMADO</span>
                <strong class="tv-huge-folio">{{ turnoActualLlamado ? turnoActualLlamado.folio : 'FOL-9482' }}</strong>
                <div class="tv-initials-row">
                  <span class="tv-initials-label">INICIALES:</span>
                  <strong class="tv-initials-badge">{{ turnoActualLlamado ? turnoActualLlamado.iniciales : 'M.R.' }}</strong>
                </div>
              </div>

              <!-- Barra brillante inferior -->
              <div class="tv-bottom-glow-bar"></div>
            </div>
          </div>

          <div class="tv-bezel-footer">
            <div class="tv-device-label">
              <span>🖥️</span>
              <span>Monitor Sony Bravia 65" Sala Principal</span>
            </div>
            <button type="button" class="btn-test-chime" @click="probarCampanilla">
              Prueba de campanilla (Chime)
            </button>
          </div>
        </div>

        <!-- 2. Censo y Aforo de Sala de Espera -->
        <div class="side-widget-card aforo-card">
          <div class="widget-header">
            <div class="title-row-sm">
              <span class="widget-icon-chair">🪑</span>
              <strong>Censo y Aforo de Sala</strong>
            </div>
            <span class="badge-aforo-blue">23% Aforo</span>
          </div>

          <div class="aforo-count-row">
            <span class="text-occupied">8 butacas ocupadas</span>
            <span class="text-capacity">Capacidad total: 35</span>
          </div>

          <div class="occ-progress-track">
            <div class="occ-fill" style="width: 23%;"></div>
          </div>

          <div class="aforo-status-row">
            <span class="text-available">27 butacas disponibles</span>
            <span class="text-ventilation">Ventilación Sala: Nominal</span>
          </div>

          <div class="aforo-actions-section">
            <small class="aforo-actions-title">ACCIONES DE TURNO Y OPERACIÓN</small>
            <div class="aforo-buttons-grid">
              <button type="button" class="btn-aforo-action" @click="mostrarAlerta('Descargando reporte de aforo y tiempos de espera en formato CSV/Excel...')">
                <span>📥</span>
                <span>Descargar Reporte</span>
              </button>
              <button type="button" class="btn-aforo-action danger" @click="mostrarAlerta('Enviando alerta preventiva a jefatura de seguridad de accesos...')">
                <span>🚨</span>
                <span>Llamar Seguridad</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 3. Supervisión CCTV de Sala -->
        <div class="side-widget-card cctv-card">
          <div class="widget-header">
            <div class="title-row-sm">
              <strong>CCTV - SUPERVISIÓN DE SALA</strong>
            </div>
            <span class="badge-rec">● REC CAM-03</span>
          </div>

          <div class="cctv-screen-mock">
            <img src="@/assets/hospital-fachada.jpg" alt="CCTV Sala" class="cctv-img" />
            <div class="cctv-img-filter"></div>
            <div class="cctv-overlay-text">
              Sala de Espera Norte · Acceso Principal
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
.view-monitor-container {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
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

.banner-left-wrap {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  flex: 1;
  min-width: 320px;
}

.banner-monitor-icon {
  width: 44px;
  height: 44px;
  background-color: #0F2D4A;
  color: #FFFFFF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.banner-text-col {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.banner-title-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.banner-title-row h2 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0;
}

.pill-turnador {
  background-color: #2563EB;
  color: #FFFFFF;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.banner-privacy-notice {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: #64748B;
  line-height: 1.35;
}

.banner-privacy-notice p {
  margin: 0;
}

.banner-right-kpis-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.hdmi-sync-badge {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  background-color: #F0F9FF;
  border: 1px solid #BAE6FD;
  color: #0284C7;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.45rem 0.8rem;
  border-radius: 6px;
}

.dot-hdmi {
  width: 7px;
  height: 7px;
  background-color: #0284C7;
  border-radius: 50%;
}

.speaker-status-box {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #334155;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.45rem 0.8rem;
  border-radius: 6px;
}

/* =========================================================================
   4 KPIS SUPERIORES (TOP KPI GRID)
   ========================================================================= */
.top-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0.95rem 1.15rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.kpi-content-col {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.kpi-label {
  font-size: 0.62rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
}

.kpi-main-val {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}

.big-kpi-num {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0F2D4A;
  line-height: 1.1;
}

.big-kpi-folio {
  font-size: 1.5rem;
  font-weight: 800;
  font-family: monospace;
  line-height: 1.1;
}

.big-kpi-folio.blue {
  color: #0284C7;
}

.big-kpi-folio.dark {
  color: #0F2D4A;
}

.initials-badge {
  background-color: #F1F5F9;
  border: 1px solid #E2E8F0;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.kpi-num-sub {
  font-size: 0.8rem;
  font-weight: 600;
  color: #0284C7;
}

.kpi-foot-sub {
  font-size: 0.68rem;
  color: #64748B;
  margin-top: 0.1rem;
}

.kpi-foot-sub.text-blue {
  color: #0284C7;
  font-weight: 600;
}

.kpi-icon-square {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
}

.kpi-icon-square.blue {
  background-color: #EFF6FF;
}

.kpi-icon-square.cyan {
  background-color: #EFF6FF;
}

.kpi-icon-square.blue-solid {
  background-color: #DBEAFE;
  color: #1D4ED8;
}

.kpi-icon-square.gray {
  background-color: #F1F5F9;
  color: #475569;
  font-weight: 800;
}

/* =========================================================================
   BARRA DE OPERACIONES Y FILTROS
   ========================================================================= */
.queue-control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.queue-action-buttons {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.btn-call-next-hero {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 0.65rem 1.15rem;
  font-size: 0.78rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
  transition: all 0.15s;
}

.btn-call-next-hero:hover {
  background-color: #1D4ED8;
}

.kbd-space {
  background-color: rgba(255, 255, 255, 0.25);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-family: monospace;
}

.btn-action-outline {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #334155;
  border-radius: 8px;
  padding: 0.65rem 1rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-outline:hover {
  background-color: #F8FAFC;
}

.tabs-filter-container {
  display: flex;
  align-items: center;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.25rem;
  gap: 0.25rem;
}

.tab-pill-btn {
  background: none;
  border: none;
  padding: 0.45rem 0.85rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748B;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.tab-pill-btn:hover {
  color: #1E293B;
}

.tab-pill-btn.active {
  background-color: #F1F5F9;
  color: #0F4C81;
  font-weight: 700;
}

/* =========================================================================
   GRID PRINCIPAL (TABLA + LATERAL DERECHO)
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
  gap: 0.75rem;
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
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.step-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 1.15rem;
  border-bottom: 1px solid #E2E8F0;
  background-color: #FFFFFF;
}

.header-title-flex {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.icon-menu-list {
  font-size: 1.1rem;
}

.header-title-flex h4 {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0;
}

.badge-count-patients {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: 9999px;
}

.legend-indicators-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.68rem;
  color: #64748B;
  font-weight: 600;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.legend-dot.red {
  background-color: #EF4444;
}

.legend-dot.blue {
  background-color: #2563EB;
}

.legend-sep {
  color: #CBD5E1;
}

.no-padding {
  padding: 0;
}

/* =========================================================================
   TABLA DE TICKETS Y TURNOS
   ========================================================================= */
.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.tickets-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.75rem;
}

.tickets-table th {
  background-color: #F8FAFC;
  color: #64748B;
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #E2E8F0;
}

.tickets-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #F1F5F9;
  vertical-align: middle;
  color: #334155;
}

.tickets-table tr:hover {
  background-color: #F8FAFC;
}

.folio-accent-wrapper {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.accent-bar {
  width: 3.5px;
  height: 24px;
  border-radius: 2px;
}

.accent-bar.blue {
  background-color: #2563EB;
}

.accent-bar.red {
  background-color: #EF4444;
}

.accent-bar.neutral {
  background-color: transparent;
}

.folio-text {
  font-family: monospace;
  font-size: 0.82rem;
  font-weight: 800;
  color: #0F2D4A;
}

.initials-chip {
  background-color: #F1F5F9;
  border: 1px solid #E2E8F0;
  color: #334155;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.col-llegada {
  color: #475569;
  font-size: 0.72rem;
}

.wait-alert-pill {
  background-color: #FEE2E2;
  color: #DC2626;
  border: 1px solid #FECACA;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.wait-text-blue {
  color: #2563EB;
  font-weight: 700;
  font-size: 0.72rem;
}

.wait-text-muted {
  color: #64748B;
  font-size: 0.72rem;
}

.col-destino strong {
  font-size: 0.75rem;
  color: #1E293B;
}

.status-pill-blue {
  background-color: #2563EB;
  color: #FFFFFF;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.status-pill-gray {
  background-color: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.text-right {
  text-align: right;
}

.ticket-action-btns-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.35rem;
}

.btn-call-inline {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.68rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-call-inline:hover {
  background-color: #1D4ED8;
}

.btn-icon-subtle {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  color: #475569;
  border-radius: 6px;
  padding: 0.35rem 0.55rem;
  font-size: 0.72rem;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-icon-subtle:hover {
  background-color: #F1F5F9;
  color: #0F4C81;
}

/* Footer de Telemetría */
.monitor-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.65rem 1rem;
  font-size: 0.72rem;
  color: #64748B;
}

.footer-left-sync {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.sync-icon {
  color: #2563EB;
}

.footer-right-f11 {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.kbd-f11 {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-family: monospace;
  font-weight: 700;
  color: #0F2D4A;
}

/* =========================================================================
   SUBCOLUMNA DERECHA: SIMULADOR TV, AFORO, CCTV
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
  font-weight: 800;
}

/* 1. SIMULADOR TV */
.tv-simulator-frame {
  background-color: #0F2D4A;
  border-radius: 10px;
  padding: 0.85rem;
  color: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.tv-bezel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.68rem;
}

.tv-title-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 800;
  color: #93C5FD;
  letter-spacing: 0.05em;
}

.tv-res-badge {
  color: #38BDF8;
  font-size: 0.65rem;
  font-family: monospace;
}

.tv-screen-bezel {
  background-color: #071322;
  border: 1px solid #1E3A5F;
  border-radius: 8px;
  padding: 0.95rem;
  position: relative;
  overflow: hidden;
}

.tv-screen-inner {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  align-items: center;
}

.tv-brand-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.tv-vitae-brand {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #FFFFFF;
}

.vitae-mini-box {
  width: 16px;
  height: 16px;
  background-color: #2563EB;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  border-radius: 3px;
}

.tv-live-clock {
  font-family: monospace;
  font-size: 0.82rem;
  color: #38BDF8;
  font-weight: 700;
}

.tv-main-callout {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  padding: 0.5rem 0;
  text-align: center;
}

.tv-callout-label {
  font-size: 0.65rem;
  font-weight: 800;
  color: #38BDF8;
  letter-spacing: 0.1em;
}

.tv-huge-folio {
  font-size: 2.3rem;
  font-weight: 900;
  font-family: monospace;
  color: #FFFFFF;
  letter-spacing: 0.05em;
  line-height: 1.1;
  text-shadow: 0 0 15px rgba(56, 189, 248, 0.4);
}

.tv-initials-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: #94A3B8;
}

.tv-initials-badge {
  color: #FFFFFF;
  font-weight: 800;
  font-size: 0.9rem;
}

.tv-bottom-glow-bar {
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #0284C7, #38BDF8, #0284C7);
  border-radius: 2px;
  box-shadow: 0 0 8px #38BDF8;
}

.tv-bezel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.68rem;
  color: #94A3B8;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 0.5rem;
}

.tv-device-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-test-chime {
  background: none;
  border: none;
  color: #60A5FA;
  font-size: 0.68rem;
  cursor: pointer;
  text-decoration: underline;
}

/* 2. CENSO Y AFORO */
.aforo-card {
  gap: 0.65rem;
}

.widget-icon-chair {
  font-size: 1rem;
}

.badge-aforo-blue {
  color: #0284C7;
  font-size: 0.78rem;
  font-weight: 800;
}

.aforo-count-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: #475569;
}

.text-occupied {
  font-weight: 700;
  color: #1E293B;
}

.text-capacity {
  color: #64748B;
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
  background-color: #0284C7;
  border-radius: 3px;
}

.aforo-status-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
}

.text-available {
  color: #64748B;
}

.text-ventilation {
  color: #16A34A;
  font-weight: 700;
}

.aforo-actions-section {
  border-top: 1px solid #F1F5F9;
  padding-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.aforo-actions-title {
  font-size: 0.62rem;
  font-weight: 800;
  color: #64748B;
  letter-spacing: 0.05em;
}

.aforo-buttons-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.btn-aforo-action {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.4rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-aforo-action:hover {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: #1D4ED8;
}

.btn-aforo-action.danger {
  color: #B91C1C;
}

.btn-aforo-action.danger:hover {
  background-color: #FEF2F2;
  border-color: #FECACA;
  color: #DC2626;
}

/* 3. CCTV */
.cctv-card {
  gap: 0.6rem;
}

.badge-rec {
  color: #DC2626;
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.cctv-screen-mock {
  position: relative;
  height: 125px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #0F172A;
}

.cctv-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: contrast(1.1) brightness(0.9);
}

.cctv-img-filter {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.25);
}

.cctv-overlay-text {
  position: absolute;
  bottom: 6px;
  left: 6px;
  right: 6px;
  background-color: rgba(15, 23, 42, 0.8);
  color: #FFFFFF;
  font-family: monospace;
  font-size: 0.65rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  text-align: center;
}
</style>
