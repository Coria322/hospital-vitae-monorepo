<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import triageService, { type SalaEsperaPublicaItem } from '@/services/triageService'

const pacientes = ref<SalaEsperaPublicaItem[]>([])
const totalEnEspera = ref(0)
const totalEnAtencion = ref(0)
const cargando = ref(true)
const ultimaActualizacion = ref('')
const relojHora = ref('')
const relojFecha = ref('')
let timerReloj: number | undefined
let timerPolling: number | undefined
let cerrarStream: (() => void) | null = null

const actualizarReloj = () => {
  const now = new Date()
  relojHora.value = now.toLocaleTimeString('es-MX', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
  relojFecha.value = now.toLocaleDateString('es-MX', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const cargarDatosSala = async () => {
  try {
    const data = await triageService.obtenerSalaEsperaPublica()
    if (data) {
      pacientes.value = data.pacientes || []
      totalEnEspera.value = data.total_en_espera || 0
      totalEnAtencion.value = data.total_en_atencion || 0
      ultimaActualizacion.value = new Date().toLocaleTimeString('es-MX')
    }
  } catch (err) {
    console.error('Error al cargar pantalla pública de sala de espera:', err)
  } finally {
    cargando.value = false
  }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    document.exitFullscreen().catch(() => {})
  }
}

const obtenerClaseColor = (color: string) => {
  switch (color?.toUpperCase()) {
    case 'ROJO': return 'nivel-rojo'
    case 'NARANJA': return 'nivel-naranja'
    case 'AMARILLO': return 'nivel-amarillo'
    case 'VERDE': return 'nivel-verde'
    case 'AZUL': return 'nivel-azul'
    default: return 'nivel-neutral'
  }
}

const pacientesLlamados = computed(() => {
  return pacientes.value.filter(p => p.estado_atencion === 'EN_ATENCION')
})

const pacientesEnFila = computed(() => {
  return pacientes.value.filter(p => p.estado_atencion !== 'EN_ATENCION')
})

onMounted(() => {
  actualizarReloj()
  timerReloj = window.setInterval(actualizarReloj, 1000)
  cargarDatosSala()

  // Conexión SSE en tiempo real para pantallas de sala
  try {
    cerrarStream = triageService.conectarStream(() => {
      cargarDatosSala()
    })
  } catch (e) {
    console.warn('SSE stream no disponible en sala pública:', e)
  }

  // Intervalo de respaldo cada 15 segundos
  timerPolling = window.setInterval(cargarDatosSala, 15000)
})

onUnmounted(() => {
  if (timerReloj) clearInterval(timerReloj)
  if (timerPolling) clearInterval(timerPolling)
  if (cerrarStream) {
    cerrarStream()
    cerrarStream = null
  }
})
</script>

<template>
  <div class="pantalla-sala-kiosk">
    <!-- BARRA SUPERIOR DE TRANSMISIÓN TV -->
    <header class="kiosk-header">
      <div class="kiosk-brand">
        <div class="brand-badge">HOSPITAL VITAE</div>
        <div class="brand-titles">
          <h1>SALA DE ESPERA Y TRIAGE GENERAL</h1>
          <span class="sub-legal">SISTEMA PÚBLICO DISOCIADO · PROV-UX-002 · LFPDPPP</span>
        </div>
      </div>

      <div class="kiosk-metrics">
        <div class="metric-card en-atencion">
          <span class="metric-label">EN ATENCIÓN MÉDICA</span>
          <span class="metric-value">{{ totalEnAtencion }}</span>
        </div>
        <div class="metric-card en-espera">
          <span class="metric-label">PACIENTES EN ESPERA</span>
          <span class="metric-value">{{ totalEnEspera }}</span>
        </div>
      </div>

      <div class="kiosk-clock">
        <div class="clock-time">{{ relojHora }}</div>
        <div class="clock-date">{{ relojFecha }}</div>
      </div>

      <button class="btn-fullscreen" @click="toggleFullscreen" title="Pantalla Completa">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
        </svg>
      </button>
    </header>

    <!-- ÁREA PRINCIPAL DIVIDIDA EN 2 PANELES: LLAMADOS Y COLA GENERAL -->
    <main class="kiosk-body">
      <!-- PANEL IZQUIERDO: LLAMADOS ACTUALES (EN ATENCIÓN) -->
      <section class="panel-llamados">
        <div class="panel-header header-llamados">
          <span class="pulse-indicator"></span>
          <h2>PASAR A CONSULTORIO / SALA ASIGNADA</h2>
        </div>

        <div v-if="pacientesLlamados.length === 0" class="empty-state">
          <p>No hay llamados activos en este momento.</p>
          <span class="empty-sub">Permanezca atento al llamado de su turno.</span>
        </div>

        <div v-else class="cards-llamados-grid">
          <div
            v-for="item in pacientesLlamados"
            :key="item.folio"
            class="card-llamado"
            :class="obtenerClaseColor(item.color)"
          >
            <div class="card-top">
              <span class="badge-atencion">LLAMANDO AHORA</span>
              <span class="badge-prioridad">{{ item.color }} · {{ item.estado_clinico }}</span>
            </div>
            <div class="card-center">
              <div class="folio-grande">{{ item.folio }}</div>
              <div class="iniciales-grandes">PACIENTE: {{ item.iniciales }}</div>
            </div>
            <div class="card-bottom">
              <span class="sala-destino">{{ item.sala }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- PANEL DERECHO: COLA GENERAL DE ESPERA (ORDEN MANCHESTER) -->
      <section class="panel-espera">
        <div class="panel-header">
          <h2>TURNO DE ESPERA PONDERADO (MANCHESTER)</h2>
          <span class="header-count">{{ pacientesEnFila.length }} EN LISTA</span>
        </div>

        <div v-if="pacientesEnFila.length === 0" class="empty-state">
          <p>No hay pacientes en espera en este momento.</p>
        </div>

        <div v-else class="tabla-espera-wrapper">
          <table class="tabla-kiosk">
            <thead>
              <tr>
                <th style="width: 80px;">TURNO</th>
                <th style="width: 170px;">FOLIO</th>
                <th>INICIALES</th>
                <th style="width: 190px;">NIVEL DE PRIORIDAD</th>
                <th>SALA / MÓDULO</th>
                <th style="width: 140px; text-align: center;">ESTADO</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, index) in pacientesEnFila"
                :key="item.folio"
                :class="[obtenerClaseColor(item.color), { 'fila-sla-vencido': item.sla_vencido }]"
              >
                <td class="col-turno">#{{ index + 1 }}</td>
                <td class="col-folio">{{ item.folio }}</td>
                <td class="col-iniciales">{{ item.iniciales }}</td>
                <td class="col-prioridad">
                  <span class="badge-color-tag" :class="obtenerClaseColor(item.color)">
                    {{ item.color }}
                  </span>
                  <span class="desc-clinica">{{ item.estado_clinico }}</span>
                </td>
                <td class="col-sala">{{ item.sala }}</td>
                <td class="col-estado text-center">
                  <span class="pill-espera">En Espera</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <!-- PIE DE PÁGINA NORMATIVO MANCHESTER -->
    <footer class="kiosk-footer">
      <div class="escala-manchester">
        <span class="escala-title">SISTEMA TRIAGE MANCHESTER OFICIAL:</span>
        <div class="escala-item escala-rojo">
          <span class="dot-color rojo"></span> Nivel 1 (Rojo): Reanimación Inmediata (0 min)
        </div>
        <div class="escala-item escala-naranja">
          <span class="dot-color naranja"></span> Nivel 2 (Naranja): Muy Urgente (10 min)
        </div>
        <div class="escala-item escala-amarillo">
          <span class="dot-color amarillo"></span> Nivel 3 (Amarillo): Urgente (60 min)
        </div>
        <div class="escala-item escala-verde">
          <span class="dot-color verde"></span> Nivel 4 (Verde): Poco Urgente (120 min)
        </div>
        <div class="escala-item escala-azul">
          <span class="dot-color azul"></span> Nivel 5 (Azul): No Urgente (240 min)
        </div>
      </div>
      <div class="footer-notice">
        Datos protegidos por la Ley Federal de Protección de Datos Personales. La prioridad de atención se determina por criterio clínico médico normativo, no por estricto orden cronológico de llegada.
      </div>
    </footer>
  </div>
</template>

<style scoped>
.pantalla-sala-kiosk {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background-color: #0b1120;
  color: #f1f5f9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  overflow: hidden;
  user-select: none;
}

/* HEADER */
.kiosk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  background-color: #0f172a;
  border-bottom: 2px solid #1e293b;
  gap: 1.5rem;
}

.kiosk-brand {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.brand-badge {
  background-color: #0284c7;
  color: #ffffff;
  font-weight: 900;
  font-size: 1.1rem;
  letter-spacing: 0.1em;
  padding: 0.5rem 1rem;
  border-radius: 6px;
}

.brand-titles h1 {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #ffffff;
}

.sub-legal {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.kiosk-metrics {
  display: flex;
  gap: 1rem;
}

.metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.4rem 1.25rem;
  border-radius: 8px;
  min-width: 130px;
}

.metric-card.en-atencion {
  background-color: #047857;
  color: #ecfdf5;
}

.metric-card.en-espera {
  background-color: #1e293b;
  border: 1px solid #334155;
  color: #e2e8f0;
}

.metric-label {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.metric-value {
  font-size: 1.75rem;
  font-weight: 900;
  line-height: 1.1;
}

.kiosk-clock {
  text-align: right;
  min-width: 180px;
}

.clock-time {
  font-size: 2rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: #38bdf8;
  line-height: 1;
}

.clock-date {
  font-size: 0.75rem;
  color: #94a3b8;
  text-transform: capitalize;
  margin-top: 0.2rem;
}

.btn-fullscreen {
  background: transparent;
  border: 1px solid #334155;
  color: #94a3b8;
  padding: 0.5rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-fullscreen:hover {
  background-color: #1e293b;
  color: #ffffff;
}

/* BODY */
.kiosk-body {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 1.5rem;
  padding: 1.5rem 2rem;
  flex: 1;
  overflow: hidden;
}

.panel-llamados,
.panel-espera {
  background-color: #0f172a;
  border: 1px solid #1e293b;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  background-color: #1e293b;
  border-bottom: 1px solid #334155;
}

.panel-header h2 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: #cbd5e1;
}

.header-llamados {
  background-color: #064e3b;
}

.header-llamados h2 {
  color: #6ee7b7;
}

.pulse-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #10b981;
  margin-right: 0.5rem;
  box-shadow: 0 0 10px #10b981;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

.header-count {
  font-size: 0.8rem;
  font-weight: 700;
  color: #94a3b8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #64748b;
  font-size: 1.1rem;
}

.empty-sub {
  font-size: 0.85rem;
  margin-top: 0.35rem;
}

/* LLAMADOS GRID */
.cards-llamados-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  overflow-y: auto;
}

.card-llamado {
  background-color: #1e293b;
  border-left: 8px solid #64748b;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
}

.card-llamado.nivel-rojo { border-left-color: #ef4444; background: linear-gradient(90deg, #450a0a 0%, #1e293b 100%); }
.card-llamado.nivel-naranja { border-left-color: #ea580c; background: linear-gradient(90deg, #431407 0%, #1e293b 100%); }
.card-llamado.nivel-amarillo { border-left-color: #eab308; background: linear-gradient(90deg, #422006 0%, #1e293b 100%); }
.card-llamado.nivel-verde { border-left-color: #10b981; background: linear-gradient(90deg, #064e3b 0%, #1e293b 100%); }
.card-llamado.nivel-azul { border-left-color: #3b82f6; background: linear-gradient(90deg, #172554 0%, #1e293b 100%); }

.card-top {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}

.badge-atencion {
  background-color: #10b981;
  color: #064e3b;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.badge-prioridad {
  color: #cbd5e1;
}

.card-center {
  margin: 0.75rem 0;
}

.folio-grande {
  font-size: 2rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  color: #ffffff;
}

.iniciales-grandes {
  font-size: 1.15rem;
  font-weight: 700;
  color: #93c5fd;
  margin-top: 0.2rem;
}

.card-bottom {
  margin-top: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.sala-destino {
  font-size: 1.1rem;
  font-weight: 800;
  color: #fde047;
}

/* TABLA ESPERA */
.tabla-espera-wrapper {
  flex: 1;
  overflow-y: auto;
}

.tabla-kiosk {
  width: 100%;
  border-collapse: collapse;
}

.tabla-kiosk thead {
  position: sticky;
  top: 0;
  background-color: #1e293b;
  z-index: 2;
}

.tabla-kiosk th {
  padding: 0.9rem 1rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: #94a3b8;
  letter-spacing: 0.05em;
  text-align: left;
  border-bottom: 2px solid #334155;
}

.tabla-kiosk td {
  padding: 1rem;
  font-size: 0.95rem;
  border-bottom: 1px solid #1e293b;
  color: #e2e8f0;
}

.col-turno {
  font-weight: 900;
  color: #38bdf8;
  font-size: 1.1rem;
}

.col-folio {
  font-weight: 800;
  font-family: monospace;
  font-size: 1.05rem;
}

.col-iniciales {
  font-weight: 800;
  font-size: 1.1rem;
  color: #ffffff;
}

.badge-color-tag {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 4px;
  font-weight: 800;
  font-size: 0.75rem;
  margin-right: 0.5rem;
}

.badge-color-tag.nivel-rojo { background-color: #ef4444; color: #ffffff; }
.badge-color-tag.nivel-naranja { background-color: #ea580c; color: #ffffff; }
.badge-color-tag.nivel-amarillo { background-color: #eab308; color: #000000; }
.badge-color-tag.nivel-verde { background-color: #10b981; color: #ffffff; }
.badge-color-tag.nivel-azul { background-color: #3b82f6; color: #ffffff; }

.desc-clinica {
  font-size: 0.8rem;
  color: #94a3b8;
}

.col-sala {
  font-weight: 700;
  color: #cbd5e1;
}

.pill-espera {
  background-color: #334155;
  color: #94a3b8;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 700;
}

.fila-sla-vencido {
  animation: parpadeo-sla 2s infinite;
}

@keyframes parpadeo-sla {
  0%, 100% { background-color: transparent; }
  50% { background-color: rgba(239, 68, 68, 0.15); }
}

/* FOOTER */
.kiosk-footer {
  background-color: #0f172a;
  border-top: 1px solid #1e293b;
  padding: 0.75rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.escala-manchester {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.25rem;
  font-size: 0.8rem;
  font-weight: 700;
}

.escala-title {
  color: #94a3b8;
}

.escala-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.dot-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot-color.rojo { background-color: #ef4444; }
.dot-color.naranja { background-color: #ea580c; }
.dot-color.amarillo { background-color: #eab308; }
.dot-color.verde { background-color: #10b981; }
.dot-color.azul { background-color: #3b82f6; }

.footer-notice {
  font-size: 0.72rem;
  color: #64748b;
  text-align: center;
}
</style>
