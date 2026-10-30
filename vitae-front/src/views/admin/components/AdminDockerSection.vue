<script setup lang="ts">
import { ref } from 'vue'
import type { ContenedorDocker, TelemetriaCluster } from '../types'

const props = defineProps<{
  contenedores: ContenedorDocker[]
  telemetria: TelemetriaCluster
}>()

const busquedaContenedores = ref('')
const terminalLogs = ref<string[]>([
  '[14:31:50] [audit-logger] HASH_OK: Block #82941 signed successfully. NOM-024 check passed.',
  '[14:31:54] [postgres-ece-db] CHECKPOINT starting: time 14:31:54, immediate false',
  '[14:31:55] [hl7-fhir-gateway] Received ORU^R01 from Analizador Hematologia Sysmex XN-1000 [OK: 200]',
  '[14:32:01] [redis-cache] DB 0: 412 keys, expires 381. Hit-rate 99.4%',
  '[14:32:04] [docker-auth-mfa] Token refresh validated for user ADM-0014 via TOTP.',
  '[14:32:10] [vite-hospital-ui] GET /api/telemetria HTTP/1.1 200 OK 32ms TLSv1.3'
])

const terminalInput = ref('')
const autoScroll = ref(true)
const diagFeedback = ref('')

const handleRestartContainer = (c: ContenedorDocker) => {
  terminalLogs.value.push(`[${new Date().toLocaleTimeString()}] [docker] Restart signal sent to container ${c.nombre} (Graceful SIGTERM).`)
  diagFeedback.value = `Contenedor ${c.nombre} reiniciado de forma segura.`
  setTimeout(() => {
    diagFeedback.value = ''
  }, 3500)
}

const handleRunCommand = () => {
  if (!terminalInput.value.trim()) return
  const cmd = terminalInput.value.trim()
  terminalLogs.value.push(`[${new Date().toLocaleTimeString()}] [sysadmin] $ ${cmd}`)
  if (cmd.includes('conninfo') || cmd.includes('psql')) {
    terminalLogs.value.push(`You are connected to database "vitae_ece_prod" as user "vitae_sysadmin" on host "postgres-ece-db" at port "5432". SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384).`)
  } else {
    terminalLogs.value.push(`[sandbox-ok] Execution logged under NOM-024 auditor. Result: Command '${cmd}' dispatched successfully.`)
  }
  terminalInput.value = ''
}

const handleLimpiarLogs = () => {
  terminalLogs.value = []
}

const handleDiagnosticoIntegridad = () => {
  diagFeedback.value = 'Diagnóstico de Integridad SysOps completado: 0 vulnerabilidades, cifrado LUKS AES-256 verificado, certificados DigiCert vigentes.'
  terminalLogs.value.push(`[${new Date().toLocaleTimeString()}] [sysops-audit] Full integrity diagnosis finished: ALL 6 CONTAINERS HEALTHY.`)
  setTimeout(() => {
    diagFeedback.value = ''
  }, 4000)
}
</script>

<template>
  <div class="admin-docker-section">
    <!-- ENCABEZADO DE INFRAESTRUCTURA -->
    <div class="docker-header-card">
      <div class="dh-left">
        <div class="dh-tags">
          <span class="cluster-badge">PROD-CLUSTER-01</span>
          <span class="sep">•</span>
          <span class="status-online-text">
            <span class="dot-green"></span>
            Todos los servicios operando normalmente
          </span>
          <span class="sep">•</span>
          <span class="uptime-text">Uptime: 99.98% (48 días, 14 hrs continuas)</span>
        </div>
        <h2 class="title">Estado de Infraestructura y Servidor Docker</h2>
      </div>

      <div class="dh-right">
        <div class="ntp-sync-box">
          <span class="ntp-lbl">SINCRONIZACIÓN NTP</span>
          <strong class="ntp-val">04:38:31 UTC-6</strong>
        </div>

        <button type="button" class="btn-outline-action" @click="handleDiagnosticoIntegridad">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          <span>Actualizar Telemetría</span>
        </button>

        <button type="button" class="btn-primary-action" @click="handleDiagnosticoIntegridad">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>Exportar Informe SysOps</span>
        </button>
      </div>
    </div>

    <!-- FEEDBACK DE ACCIONES -->
    <div v-if="diagFeedback" class="feedback-banner">
      <svg viewBox="0 0 24 24" class="fb-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>{{ diagFeedback }}</span>
    </div>

    <!-- 4 TARJETAS DE RECURSOS DE HARDWARE -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-head">
          <div class="kpi-title-block">
            <span class="kpi-lbl">USO DE CPU CLUSTER</span>
            <strong class="hw-model">{{ telemetria.cpu_modelo }}</strong>
          </div>
          <svg viewBox="0 0 24 24" class="hw-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="4" y="4" width="16" height="16" rx="2"/>
            <rect x="9" y="9" width="6" height="6"/>
            <line x1="9" y1="1" x2="9" y2="4"/>
            <line x1="15" y1="1" x2="15" y2="4"/>
            <line x1="9" y1="20" x2="9" y2="23"/>
            <line x1="15" y1="20" x2="15" y2="23"/>
            <line x1="20" y1="9" x2="23" y2="9"/>
            <line x1="20" y1="14" x2="23" y2="14"/>
            <line x1="1" y1="9" x2="4" y2="9"/>
            <line x1="1" y1="14" x2="4" y2="14"/>
          </svg>
        </div>
        <div class="kpi-body">
          <div class="metric-row">
            <span class="main-metric">{{ telemetria.cpu_uso_pct }} %</span>
            <span class="badge-stable">Estable</span>
          </div>
          <div class="sub-metric">
            <span>{{ telemetria.cpu_nucleos }} Núcleos asignados</span>
            <span class="sep">•</span>
            <span>Carga Media: {{ telemetria.cpu_carga_media }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <div class="kpi-title-block">
            <span class="kpi-lbl">MEMORIA RAM ASIGNADA</span>
            <strong class="hw-model">DDR4 ECC Reg</strong>
          </div>
          <svg viewBox="0 0 24 24" class="hw-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
            <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
          </svg>
        </div>
        <div class="kpi-body">
          <div class="metric-row">
            <span class="main-metric">{{ telemetria.ram_usada_gb }} <span class="unit">/ {{ telemetria.ram_total_gb }} GB</span></span>
            <span class="badge-stable blue">{{ telemetria.ram_uso_pct }}% En Uso</span>
          </div>
          <div class="sub-metric">
            <span>Buffer/Caché: {{ telemetria.ram_buffer_gb }} GB</span>
            <span class="sep">•</span>
            <span>Libre: {{ (telemetria.ram_total_gb - telemetria.ram_usada_gb).toFixed(1) }} GB</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <div class="kpi-title-block">
            <span class="kpi-lbl">ALMACENAMIENTO NVME</span>
            <strong class="hw-model">RAID 10 Clínico</strong>
          </div>
          <svg viewBox="0 0 24 24" class="hw-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="12" x2="2" y2="12"/>
            <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>
            <line x1="6" y1="16" x2="6.01" y2="16"/>
            <line x1="10" y1="16" x2="10.01" y2="16"/>
          </svg>
        </div>
        <div class="kpi-body">
          <div class="metric-row">
            <span class="main-metric">{{ telemetria.nvme_usado_tb }} <span class="unit">/ {{ telemetria.nvme_total_tb }} TB</span></span>
            <span class="badge-stable blue">70% Ocupado</span>
          </div>
          <div class="sub-metric">
            <span>I/O E/S: {{ telemetria.nvme_io_rate }}</span>
            <span class="sep">•</span>
            <span>Libre: {{ telemetria.nvme_libre_tb }} TB</span>
          </div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <div class="kpi-title-block">
            <span class="kpi-lbl">LATENCIA DE RED INTERNA</span>
            <strong class="hw-model">{{ telemetria.red_vlan }}</strong>
          </div>
          <svg viewBox="0 0 24 24" class="hw-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="2" y1="12" x2="22" y2="12"/>
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
          </svg>
        </div>
        <div class="kpi-body">
          <div class="metric-row">
            <span class="main-metric">{{ telemetria.red_latencia_ms }} <span class="unit">ms</span></span>
            <span class="badge-stable green">Óptimo</span>
          </div>
          <div class="sub-metric">
            <span>Pérdida Paquetes: {{ telemetria.red_paquetes_perdidos_pct }}%</span>
            <span class="sep">•</span>
            <span>{{ telemetria.red_throughput }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- DOS COLUMNAS: CONTENEDORES + CONSOLAS LATERALES -->
    <div class="docker-layout-grid">
      <!-- COLUMNA IZQUIERDA: MATRIZ DE CONTENEDORES + TERMINAL STREAMING -->
      <div class="containers-col">
        <!-- MATRIZ DE CONTENEDORES DOCKER -->
        <div class="containers-card">
          <div class="card-top-bar">
            <div class="ctb-left">
              <svg viewBox="0 0 24 24" class="docker-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
              </svg>
              <h4>Matriz de Contenedores Docker</h4>
              <span class="badge-active-count">{{ contenedores.length }}/{{ contenedores.length }} Activos</span>
            </div>

            <div class="ctb-right">
              <div class="input-filter-wrap">
                <svg viewBox="0 0 24 24" class="search-mini" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input
                  v-model="busquedaContenedores"
                  type="text"
                  placeholder="Filtrar por puerto, id, rol..."
                  class="container-search"
                />
              </div>

              <button type="button" class="btn-compose">
                <svg viewBox="0 0 24 24" class="compose-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="16 18 22 12 16 6"/>
                  <polyline points="8 6 2 12 8 18"/>
                </svg>
                <span>Compose</span>
              </button>
            </div>
          </div>

          <!-- LISTA DE CONTENEDORES -->
          <div class="containers-list">
            <div
              v-for="c in contenedores"
              :key="c.id"
              class="container-item-row"
            >
              <div class="c-left">
                <div class="c-icon-box">
                  <svg viewBox="0 0 24 24" class="c-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
                    <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
                  </svg>
                </div>

                <div class="c-meta">
                  <div class="c-title-row">
                    <strong class="c-name">{{ c.nombre }}</strong>
                    <span class="c-status-chip" :class="c.estado.toLowerCase()">{{ c.estado }}</span>
                    <span class="c-ver">{{ c.imagen_version }}</span>
                  </div>

                  <div class="c-details-row">
                    <span class="detail-item">Puertos: {{ c.puertos }}</span>
                    <span class="sep">•</span>
                    <span class="detail-item rtt">{{ c.latencia_rtt }}</span>
                    <span v-if="c.conexiones_activas" class="sep">•</span>
                    <span v-if="c.conexiones_activas" class="detail-item">{{ c.conexiones_activas }} conex. activas</span>
                  </div>

                  <span class="c-desc">{{ c.descripcion }}</span>
                </div>
              </div>

              <div class="c-actions">
                <button type="button" class="btn-c-action">
                  <svg viewBox="0 0 24 24" class="act-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  </svg>
                  <span>Logs</span>
                </button>

                <button type="button" class="btn-c-action">
                  <svg viewBox="0 0 24 24" class="act-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="20" x2="18" y2="10"/>
                    <line x1="12" y1="20" x2="12" y2="4"/>
                    <line x1="6" y1="20" x2="6" y2="14"/>
                  </svg>
                  <span>Métricas</span>
                </button>

                <button
                  type="button"
                  class="btn-c-action restart"
                  title="Reiniciar contenedor de forma segura"
                  @click="handleRestartContainer(c)"
                >
                  <svg viewBox="0 0 24 24" class="act-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- CONSOLA / LOGS EN VIVO (docker logs -f) -->
        <div class="terminal-card">
          <div class="term-header">
            <div class="term-title-left">
              <span class="dot-stream"></span>
              <strong>docker logs --tail 25 -f (Todos los servicios)</strong>
            </div>

            <div class="term-controls">
              <button type="button" class="btn-term-ctrl" @click="handleLimpiarLogs">Limpiar</button>
              <button
                type="button"
                class="btn-term-ctrl"
                :class="{ active: autoScroll }"
                @click="autoScroll = !autoScroll"
              >
                Auto-scroll: {{ autoScroll ? 'ON' : 'OFF' }}
              </button>
            </div>
          </div>

          <div class="term-screen">
            <div v-for="(line, idx) in terminalLogs" :key="idx" class="term-line">
              {{ line }}
            </div>
          </div>
        </div>
      </div>

      <!-- COLUMNA DERECHA: ALERTAS DE INTEGRIDAD, CONSOLA SYSADMIN Y ESTADO DEL RACK -->
      <div class="sidebar-col">
        <!-- ALERTAS DE INTEGRIDAD -->
        <div class="side-card">
          <div class="sc-header">
            <div class="sc-h-left">
              <svg viewBox="0 0 24 24" class="sc-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <h4>Alertas de Integridad</h4>
            </div>
            <span class="nom-iso-pill">NOM-024 / ISO 27001</span>
          </div>

          <div class="sc-body">
            <div class="sec-item">
              <div class="sec-item-head">
                <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <strong>Cifrado de Base de Datos en Reposo</strong>
              </div>
              <p>Algoritmo AES-256-XTS activo en particiones LUKS y volumen de Postgres. Llaves gestionadas vía HSM.</p>
              <span class="sec-status-ok">
                <svg viewBox="0 0 24 24" class="check-mini" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                Activo y verificado
              </span>
            </div>

            <div class="sec-item">
              <div class="sec-item-head">
                <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 2l-2 2m-1.5 1.5L16 7l-1.5-1.5M7 12a5 5 0 1 1 5-5L9 10"/>
                </svg>
                <strong>Certificados SSL/TLS Vigentes</strong>
              </div>
              <p>TLS 1.3 mTLS en pasarela FHIR y portal ECE. Let's Encrypt / DigiCert Wildcard.</p>
              <span class="sec-sub">Vence en 240 días (Renovación automática)</span>
            </div>

            <div class="sec-item">
              <div class="sec-item-head">
                <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                <strong>Copia de Respaldo Automatizada</strong>
              </div>
              <p>Snapshots ZFS + Dump SQL replicado fuera de sitio (Sede Hospitalaria Alterna).</p>
              <span class="sec-sub blue">Último backup: Hoy 04:00 h (Exitoso - 42.1 GB)</span>
            </div>

            <button type="button" class="btn-execute-diag" @click="handleDiagnosticoIntegridad">
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
              <span>Ejecutar Diagnóstico de Integridad [Alt+D]</span>
            </button>
          </div>
        </div>

        <!-- CONSOLA RÁPIDA SYSADMIN (SANDBOX) -->
        <div class="side-card">
          <div class="sc-header">
            <div class="sc-h-left">
              <svg viewBox="0 0 24 24" class="sc-svg" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="4 17 10 11 4 5"/>
                <line x1="12" y1="19" x2="20" y2="19"/>
              </svg>
              <h4>Consola Rápida SysAdmin</h4>
            </div>
            <span class="sandbox-pill">Sandbox Seguro</span>
          </div>

          <div class="sc-body">
            <p class="console-desc">Ejecución restringida de utilidades de diagnóstico:</p>

            <div class="cmd-box">
              <span class="prompt-sym">$</span>
              <input
                v-model="terminalInput"
                type="text"
                placeholder="Escriba comando de diagnóstico (ej. psql \conninfo)..."
                class="cmd-input"
                @keyup.enter="handleRunCommand"
              />
              <button type="button" class="btn-send-cmd" @click="handleRunCommand">
                <svg viewBox="0 0 24 24" class="send-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
              </button>
            </div>

            <div class="sandbox-footer-row">
              <span>Modo: Solo Lectura / Diagnóstico</span>
              <span class="sep">•</span>
              <span>Auditado por NOM-024</span>
            </div>
          </div>
        </div>

        <!-- ESTADO DEL RACK FÍSICO Y SENSORES (SUSTITUYE FOTO INNECESARIA) -->
        <div class="side-card rack-card">
          <div class="sc-header">
            <div class="sc-h-left">
              <svg viewBox="0 0 24 24" class="sc-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="2" ry="2"/>
                <line x1="2" y1="8" x2="22" y2="8"/>
                <line x1="2" y1="16" x2="22" y2="16"/>
              </svg>
              <h4>Ubicación del Rack Físico</h4>
            </div>
            <span class="tower-tag">Data Center Torre B</span>
          </div>

          <div class="rack-body">
            <div class="rack-sensor-item">
              <span class="sensor-lbl">Módulo:</span>
              <strong>Rack A-04 (Piso Técnico 02)</strong>
            </div>
            <div class="rack-sensor-item">
              <span class="sensor-lbl">Temperatura Ambiente:</span>
              <strong class="temp-val">19.2 °C (Clima OK)</strong>
            </div>
            <div class="rack-sensor-item">
              <span class="sensor-lbl">Humedad Relativa:</span>
              <strong>45% HR Nominal</strong>
            </div>
            <div class="rack-sensor-item">
              <span class="sensor-lbl">Suministro Eléctrico:</span>
              <strong class="ups-val">UPS Doble Conversión + Planta Diésel</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-docker-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.docker-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.dh-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 60%;
}

.dh-tags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
}

.cluster-badge {
  background-color: #0f172a;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-online-text {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: #166534;
  font-weight: 700;
}

.dot-green {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #16a34a;
}

.uptime-text {
  color: #64748b;
}

.sep {
  color: #cbd5e1;
}

.title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.dh-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ntp-sync-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
}

.ntp-lbl {
  font-size: 0.6rem;
  font-weight: 800;
  color: #64748b;
}

.ntp-val {
  font-family: monospace;
  font-size: 0.76rem;
  color: #0f172a;
}

.btn-outline-action {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary-action {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0f172a;
  color: #ffffff;
  border: none;
  padding: 0.5rem 0.95rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.feedback-banner {
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

.fb-svg {
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
  gap: 0.5rem;
}

.kpi-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.kpi-title-block {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.kpi-lbl {
  font-size: 0.62rem;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.04em;
}

.hw-model {
  font-size: 0.76rem;
  color: #0f172a;
}

.hw-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.metric-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.main-metric {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
}

.main-metric .unit {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
}

.badge-stable {
  background-color: #f0fdf4;
  color: #166534;
  font-size: 0.64rem;
  font-weight: 800;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}

.badge-stable.blue {
  background-color: #e0f2fe;
  color: #0369a1;
}

.badge-stable.green {
  background-color: #f0fdf4;
  color: #15803d;
}

.sub-metric {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.66rem;
  color: #64748b;
  margin-top: 2px;
}

.docker-layout-grid {
  display: grid;
  grid-template-columns: 1fr 370px;
  gap: 1.25rem;
}

.containers-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.containers-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.ctb-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.docker-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.ctb-left h4 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-active-count {
  font-size: 0.64rem;
  background-color: #e0f2fe;
  color: #0369a1;
  font-weight: 800;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}

.ctb-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.input-filter-wrap {
  display: flex;
  align-items: center;
  position: relative;
}

.search-mini {
  position: absolute;
  left: 8px;
  width: 13px;
  height: 13px;
  color: #94a3b8;
}

.container-search {
  padding: 0.35rem 0.65rem 0.35rem 1.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 0.72rem;
  outline: none;
}

.btn-compose {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.compose-svg {
  width: 12px;
  height: 12px;
}

.containers-list {
  display: flex;
  flex-direction: column;
}

.container-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  transition: background-color 0.15s ease;
}

.container-item-row:hover {
  background-color: #f8fafc;
}

.container-item-row:last-of-type {
  border-bottom: none;
}

.c-left {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.c-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background-color: #f1f5f9;
  color: #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.c-svg {
  width: 16px;
  height: 16px;
}

.c-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.c-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.c-name {
  font-family: monospace;
  font-size: 0.82rem;
  color: #0f172a;
}

.c-status-chip {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.c-status-chip.online, .c-status-chip.saludable, .c-status-chip.activo {
  background-color: #f0fdf4;
  color: #166534;
}

.c-status-chip.sincronizado {
  background-color: #e0f2fe;
  color: #0369a1;
}

.c-ver {
  font-family: monospace;
  font-size: 0.65rem;
  color: #64748b;
}

.c-details-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.68rem;
  color: #64748b;
}

.detail-item.rtt {
  color: #0284c7;
  font-weight: 600;
}

.c-desc {
  font-size: 0.66rem;
  color: #475569;
}

.c-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-c-action {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  padding: 0.3rem 0.55rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.btn-c-action:hover {
  background-color: #e2e8f0;
}

.btn-c-action.restart:hover {
  background-color: #fee2e2;
  color: #b91c1c;
}

.act-svg {
  width: 12px;
  height: 12px;
}

.terminal-card {
  background-color: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

.term-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  background-color: #1e293b;
  border-bottom: 1px solid #334155;
}

.term-title-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-family: monospace;
  font-size: 0.72rem;
}

.dot-stream {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #38bdf8;
  animation: pulseTerm 1.5s infinite;
}

@keyframes pulseTerm {
  0% { opacity: 0.4; }
  50% { opacity: 1; }
  100% { opacity: 0.4; }
}

.term-controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-term-ctrl {
  background-color: #334155;
  border: none;
  color: #e2e8f0;
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
}

.btn-term-ctrl.active {
  background-color: #0284c7;
  color: #ffffff;
}

.term-screen {
  padding: 0.75rem;
  font-family: monospace;
  font-size: 0.68rem;
  line-height: 1.45;
  max-height: 180px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.term-line {
  word-break: break-all;
  color: #93c5fd;
}

.sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.side-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.sc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.sc-h-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.sc-svg {
  width: 15px;
  height: 15px;
  color: #0284c7;
}

.sc-h-left h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.nom-iso-pill {
  font-size: 0.6rem;
  font-weight: 800;
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.sc-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.sec-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.sec-item:last-of-type {
  border-bottom: none;
}

.sec-item-head {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.sec-svg {
  width: 13px;
  height: 13px;
  color: #0284c7;
}

.sec-item-head strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.sec-item p {
  margin: 0;
  font-size: 0.66rem;
  color: #64748b;
  line-height: 1.25;
}

.sec-status-ok {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #16a34a;
  font-size: 0.65rem;
  font-weight: 700;
  margin-top: 2px;
}

.check-mini {
  width: 11px;
  height: 11px;
}

.sec-sub {
  font-size: 0.64rem;
  color: #64748b;
}

.sec-sub.blue {
  color: #0284c7;
  font-weight: 600;
}

.btn-execute-diag {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  padding: 0.55rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.sandbox-pill {
  font-size: 0.6rem;
  font-weight: 800;
  background-color: #f1f5f9;
  color: #334155;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.console-desc {
  font-size: 0.68rem;
  color: #64748b;
  margin: 0;
}

.cmd-box {
  display: flex;
  align-items: center;
  background-color: #0f172a;
  border-radius: 6px;
  padding: 0.35rem 0.5rem;
  gap: 0.4rem;
}

.prompt-sym {
  color: #38bdf8;
  font-family: monospace;
  font-weight: 700;
}

.cmd-input {
  flex: 1;
  background: none;
  border: none;
  color: #ffffff;
  font-family: monospace;
  font-size: 0.72rem;
  outline: none;
}

.btn-send-cmd {
  background: none;
  border: none;
  color: #38bdf8;
  cursor: pointer;
  padding: 2px;
}

.send-svg {
  width: 13px;
  height: 13px;
}

.sandbox-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.64rem;
  color: #64748b;
}

.tower-tag {
  font-size: 0.62rem;
  font-weight: 700;
  color: #475569;
}

.rack-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-size: 0.7rem;
}

.rack-sensor-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sensor-lbl {
  color: #64748b;
}

.rack-sensor-item strong {
  color: #0f172a;
}

.temp-val {
  color: #16a34a !important;
}

.ups-val {
  font-size: 0.66rem;
  color: #0284c7 !important;
}
</style>
