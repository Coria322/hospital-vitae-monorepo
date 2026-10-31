<script setup lang="ts">
import { ref } from 'vue'
import type { TrazaAuditoria } from '../types'

const props = defineProps<{
  trazas: TrazaAuditoria[]
}>()

const selectedTraza = ref<TrazaAuditoria>(props.trazas[0] || {
  id: 'tr-1',
  timestamp_ntp: '10:48:12.418 UTC-6',
  usuario_nombre: 'Dr. Carlos Mendoza',
  usuario_id: 'MED-8841',
  usuario_rol: 'Médico Adscrito',
  ip_origen: '192.168.10.12',
  mac_address: '4A:60:B2:1A',
  terminal: 'WS-MED-04 (Ubuntu LTS 24.04 OS)',
  accion: 'Apertura ECE',
  recurso_disociado: 'ECE-DEMO-0248',
  resultado_codigo: 200,
  resultado_etiqueta: '200 OK',
  hash_sha256: '4f9a72c18d4022a186e4ff98c6d17b84aa8981cb984f7621e2e50529c9124be3',
  jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
})

const verifyFeedback = ref('')

const handleVerificarCadena = () => {
  verifyFeedback.value = 'Cadena de custodia Block-Tree verificada: 142,890 bloques concatenados íntegros sin bifurcaciones.'
  setTimeout(() => {
    verifyFeedback.value = ''
  }, 4000)
}

const handleExportarReporte = () => {
  verifyFeedback.value = 'Generando dictamen pericial forense certificado en PDF con firma PKI y sello de tiempo CENAM...'
  setTimeout(() => {
    verifyFeedback.value = ''
  }, 3500)
}

const handleValidarHashTraza = () => {
  verifyFeedback.value = `Hash ${selectedTraza.value.hash_sha256.slice(0, 16)}... coincide exactamente con el bloque sellado #82941.`
  setTimeout(() => {
    verifyFeedback.value = ''
  }, 3500)
}
</script>

<template>
  <div class="admin-bitacora-section">
    <!-- ENCABEZADO Y SELLADO CRIPTOGRÁFICO -->
    <div class="bitacora-header-card">
      <div class="bh-left">
        <div class="tags-row">
          <span class="badge-nom">NOM-024-SSA3-2012 / ISO 27001</span>
          <span class="sep">•</span>
          <span class="badge-seal">
            <span class="dot-blue"></span>
            Criptográficamente Sellada (SHA-256 Append-Only)
          </span>
        </div>
        <h2 class="title">Bitácora Inmutable de Auditoría y Trazabilidad</h2>
        <p class="desc">
          Trazabilidad forense continua de operaciones del Expediente Clínico Electrónico (ECE). Datos de pacientes anonimizados y disociados conforme al protocolo de separación funcional de privilegios.
        </p>
      </div>

      <div class="bh-right">
        <button type="button" class="btn-outline-action" @click="handleVerificarCadena">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
          </svg>
          <span>Verificar Cadena</span>
        </button>

        <button type="button" class="btn-primary-action" @click="handleExportarReporte">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>Exportar Reporte Certificado</span>
        </button>
      </div>
    </div>

    <!-- NOTIFICACIÓN DE AUDITORÍA -->
    <div v-if="verifyFeedback" class="feedback-banner">
      <svg viewBox="0 0 24 24" class="fb-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>{{ verifyFeedback }}</span>
    </div>

    <!-- 4 MÉTRICAS FORENSES -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-head">
          <span class="kpi-lbl">EVENTOS AUDITADOS (24H)</span>
          <svg viewBox="0 0 24 24" class="kpi-icon" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-val">142,890</span>
          <span class="kpi-sub">
            <svg viewBox="0 0 24 24" class="sync-mini" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"/>
              <polyline points="1 20 1 14 7 14"/>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
            </svg>
            Sincronizado NTP ±0.8ms
          </span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <span class="kpi-lbl">CONSULTAS ECE DISOCIADAS</span>
          <svg viewBox="0 0 24 24" class="kpi-icon blue" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
            <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
          </svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-val">19,421</span>
          <span class="kpi-sub blue">Cero exposición clínica</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <span class="kpi-lbl">ALERTAS BLOQUEADAS (WAF/AUTH)</span>
          <svg viewBox="0 0 24 24" class="kpi-icon red" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-val danger">14</span>
          <span class="kpi-sub red">2 IPs en cuarentena preventiva</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-head">
          <span class="kpi-lbl">INTEGRIDAD BLOCK-TREE</span>
          <svg viewBox="0 0 24 24" class="kpi-icon green" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-val green">100.0%</span>
          <span class="kpi-sub green">Cadena SHA-256 válida</span>
        </div>
      </div>
    </div>

    <!-- FILTROS DE TRAZABILIDAD FORENSE -->
    <div class="filters-card">
      <div class="filters-header">
        <div class="fh-title-left">
          <svg viewBox="0 0 24 24" class="filter-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
          </svg>
          <strong>Filtros de Trazabilidad Forense</strong>
          <span class="sub-filter">• Registro Inmutable en Tiempo Real</span>
        </div>
        <span class="syslog-status">Receptor Syslog RFC-5424 Activo</span>
      </div>

      <div class="filters-row">
        <div class="filter-group">
          <label class="flbl">Ventana de Tiempo</label>
          <select class="fselect">
            <option>Últimas 24 Horas</option>
            <option>Últimos 7 Días</option>
            <option>Últimos 30 Días (Auditoría Mensual)</option>
          </select>
        </div>

        <div class="filter-group">
          <label class="flbl">Usuario / Cédula / Matrícula</label>
          <input type="text" placeholder="Ej. Dr. Mendoza, MED-8841" class="finput" />
        </div>

        <div class="filter-group">
          <label class="flbl">Dirección IP / Origen</label>
          <input type="text" placeholder="Ej. 192.168.10.12" class="finput" />
        </div>

        <div class="filter-group">
          <label class="flbl">Acción Registrada</label>
          <select class="fselect">
            <option>Todos los Eventos (Consolidado)</option>
            <option>Apertura ECE</option>
            <option>Validación STAT</option>
            <option>Firma de Receta</option>
            <option>Fallo Auth / Bloqueo</option>
            <option>Desbloqueo Cuenta</option>
          </select>
        </div>

        <div class="filter-group">
          <label class="flbl">Severidad / Dictamen</label>
          <select class="fselect">
            <option>Todos los Niveles</option>
            <option>200 OK (Autorizado)</option>
            <option>403 Bloqueado (Alerta)</option>
          </select>
        </div>
      </div>
    </div>

    <!-- DOS COLUMNAS PRINCIPALES: TABLA DE AUDITORÍA Y PANEL DE INSPECCIÓN -->
    <div class="bitacora-grid">
      <!-- COLUMNA IZQUIERDA: TABLA DE TRAZAS AUDITADAS -->
      <div class="trazas-table-col">
        <div class="table-card">
          <div class="table-card-header">
            <div class="tch-left">
              <h4>Trazas Auditadas</h4>
              <span class="view-tag">{{ trazas.length }} registros en vista</span>
            </div>
            <div class="tch-right">
              <span class="live-pill">
                <span class="dot-green"></span>
                Tiempo Real (Live)
              </span>
              <span class="page-count">Página 1 de 214</span>
            </div>
          </div>

          <table class="audit-table">
            <thead>
              <tr>
                <th>TIMESTAMP (NTP)</th>
                <th>USUARIO EJECUTOR</th>
                <th>IP / TERMINAL</th>
                <th>ACCIÓN</th>
                <th>RECURSO DISOCIADO</th>
                <th>RESULTADO</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="t in trazas"
                :key="t.id"
                :class="{ active: selectedTraza.id === t.id }"
                @click="selectedTraza = t"
              >
                <td class="cell-ntp">
                  <span class="ntp-time">{{ t.timestamp_ntp }}</span>
                </td>

                <td class="cell-user">
                  <strong class="user-name">{{ t.usuario_nombre }}</strong>
                  <span class="user-meta">{{ t.usuario_rol }} • {{ t.usuario_id }}</span>
                </td>

                <td class="cell-ip">
                  <span class="ip-text">{{ t.ip_origen }}</span>
                  <span class="mac-text">MAC: {{ t.mac_address }}</span>
                </td>

                <td class="cell-action">
                  <span class="action-chip" :class="{ alert: t.resultado_codigo === 403 }">
                    {{ t.accion }}
                  </span>
                </td>

                <td class="cell-resource">
                  <strong class="res-code">{{ t.recurso_disociado }}</strong>
                </td>

                <td class="cell-result">
                  <span class="result-badge" :class="{ success: t.resultado_codigo === 200, danger: t.resultado_codigo === 403 }">
                    {{ t.resultado_etiqueta }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="table-footer">
            <div class="tf-left">
              <span>Mostrando 6 de 142,890 registros</span>
              <span class="sep">•</span>
              <span class="retention-info">Buffer de retención: 5 años obligatorios NOM-024</span>
            </div>
            <div class="tf-right">
              <button type="button" class="btn-pg" disabled>Anterior</button>
              <button type="button" class="btn-pg active">1</button>
              <button type="button" class="btn-pg">2</button>
              <button type="button" class="btn-pg">3</button>
              <span class="dots">...</span>
              <button type="button" class="btn-pg">214</button>
              <button type="button" class="btn-pg">Siguiente</button>
            </div>
          </div>
        </div>
      </div>

      <!-- COLUMNA DERECHA: INSPECCIÓN FORENSE Y ESTADO WORM -->
      <div class="inspection-sidebar-col">
        <!-- INSPECCIÓN FORENSE DE TRAZA -->
        <div class="inspect-card">
          <div class="inspect-header">
            <div class="ih-left">
              <svg viewBox="0 0 24 24" class="ih-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <h4>Inspección Forense de Traza</h4>
            </div>
            <span class="log-id-badge">LOG-{{ selectedTraza.id }}</span>
          </div>

          <div class="inspect-body">
            <div class="disclaimer-box">
              <svg viewBox="0 0 24 24" class="disc-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <p>
                <strong>Aislamiento NOM-024:</strong> El visor muestra únicamente telemetría de auditoría sin exponer datos sensibles, diagnósticos ni notas clínicas del paciente.
              </p>
            </div>

            <div class="field-block">
              <span class="f-lbl">EVENTO AUDITADO:</span>
              <div class="f-val-row">
                <strong>{{ selectedTraza.accion }}</strong>
                <span class="badge-status-code">{{ selectedTraza.resultado_etiqueta }}</span>
              </div>
            </div>

            <div class="field-block">
              <span class="f-lbl">IDENTIDAD DEL EJECUTOR:</span>
              <div class="exec-card">
                <div class="exec-meta">
                  <strong>{{ selectedTraza.usuario_nombre }}</strong>
                  <span>{{ selectedTraza.usuario_rol }} • ID: {{ selectedTraza.usuario_id }}</span>
                  <span class="token-lbl">Token Sesión JWT: {{ selectedTraza.jwt_token_preview }}</span>
                </div>
              </div>
            </div>

            <div class="field-block-2col">
              <div>
                <span class="f-lbl">DIRECCIÓN IP:</span>
                <span class="f-val-mono">{{ selectedTraza.ip_origen }}</span>
              </div>
              <div>
                <span class="f-lbl">TERMINAL HOST:</span>
                <span class="f-val-mono">{{ selectedTraza.terminal }}</span>
              </div>
            </div>

            <div class="field-block">
              <span class="f-lbl">RECURSO AFECTADO (DISOCIADO):</span>
              <div class="resource-box">
                <strong class="res-bold">{{ selectedTraza.recurso_disociado }}</strong>
                <span>Identificador clínico disociado mediante hash con salting hospitalario.</span>
              </div>
            </div>

            <div class="field-block">
              <div class="sha-head-row">
                <span class="f-lbl">HASH SHA-256 DE LA TRAZA:</span>
                <span class="valid-chip">VÁLIDO</span>
              </div>
              <div class="hash-box">
                {{ selectedTraza.hash_sha256 }}
              </div>
            </div>

            <button type="button" class="btn-validate-hash" @click="handleValidarHashTraza">
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <polyline points="9 12 11 14 15 10"/>
              </svg>
              <span>Validar Hash de Integridad Criptográfica</span>
            </button>
          </div>
        </div>

        <!-- SERVICIO DE TRAZABILIDAD CENTRAL (WORM / NTP) -->
        <div class="worm-card">
          <div class="worm-header">
            <h4>Servicio de Trazabilidad Central</h4>
            <span class="worm-status-pill">
              <span class="dot-green"></span>
              Operativo
            </span>
          </div>
          <div class="worm-body">
            <div class="worm-line">
              <span>Buffer de escritura:</span>
              <strong>12 msgs/sec</strong>
            </div>
            <div class="worm-line">
              <span>Almacenamiento WORM:</span>
              <strong>Activo (No sobreescribible)</strong>
            </div>
            <div class="worm-line">
              <span>Servidor NTP:</span>
              <strong class="cenam-tag">cronos.cenam.mx (Sincronizado)</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- DECLARACIÓN TÉCNICA NOM-024 / ISO 27001 FOOTER -->
    <div class="iso-declaration-footer">
      <svg viewBox="0 0 24 24" class="iso-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
      </svg>
      <div class="iso-text">
        <strong>Declaración Técnica de Separación de Privilegios</strong>
        <p>
          En cumplimiento con la norma <strong>NOM-024-SSA3-2012</strong> (Sistemas de Información de Registro Electrónico para la Salud) y directrices de seguridad <strong>ISO/IEC 27001:2022</strong>, el perfil de Administrador de Sistemas cuenta con trazabilidad técnica irrestricta de accesos (quién, cuándo, desde qué terminal y qué recurso) garantizando la <em>total disociación de la materia clínica</em>: las notas médicas, diagnósticos, recetas terapéuticas y resultados están encriptados y no son renderizados en consolas administrativas.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-bitacora-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.bitacora-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.bh-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 65%;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-nom {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.66rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.sep {
  color: #cbd5e1;
}

.badge-seal {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  color: #0284c7;
  font-weight: 600;
}

.dot-blue {
  width: 6px;
  height: 6px;
  background-color: #0284c7;
  border-radius: 50%;
}

.title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.desc {
  font-size: 0.74rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.bh-right {
  display: flex;
  align-items: center;
  gap: 0.65rem;
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
  transition: all 0.15s ease;
}

.btn-outline-action:hover {
  background-color: #f8fafc;
  color: #0f172a;
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
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.25);
}

.btn-primary-action:hover {
  background-color: #1e293b;
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
  gap: 0.4rem;
}

.kpi-head {
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
  color: #64748b;
}

.kpi-icon.blue { color: #0284c7; }
.kpi-icon.red { color: #b91c1c; }
.kpi-icon.green { color: #16a34a; }

.kpi-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kpi-val {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
}

.kpi-val.danger { color: #b91c1c; }
.kpi-val.green { color: #16a34a; }

.kpi-sub {
  font-size: 0.66rem;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.kpi-sub.blue { color: #0284c7; font-weight: 600; }
.kpi-sub.red { color: #b91c1c; font-weight: 600; }
.kpi-sub.green { color: #16a34a; font-weight: 600; }

.sync-mini {
  width: 12px;
  height: 12px;
  color: #0284c7;
}

.filters-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filters-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fh-title-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.76rem;
  color: #0f172a;
}

.filter-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.sub-filter {
  color: #64748b;
  font-size: 0.68rem;
}

.syslog-status {
  font-size: 0.66rem;
  font-weight: 700;
  color: #16a34a;
}

.filters-row {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 1.2fr 1.5fr 1fr;
  gap: 0.65rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.flbl {
  font-size: 0.66rem;
  font-weight: 700;
  color: #475569;
}

.finput, .fselect {
  width: 100%;
  padding: 0.35rem 0.55rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 0.74rem;
  color: #0f172a;
  outline: none;
  background-color: #f8fafc;
  box-sizing: border-box;
}

.finput:focus, .fselect:focus {
  border-color: #0284c7;
  background-color: #ffffff;
}

.bitacora-grid {
  display: grid;
  grid-template-columns: 1fr 370px;
  gap: 1.25rem;
}

.trazas-table-col {
  display: flex;
  flex-direction: column;
}

.table-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.table-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.tch-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tch-left h4 {
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.view-tag {
  font-size: 0.64rem;
  background-color: #e2e8f0;
  color: #334155;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}

.tch-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.68rem;
}

.live-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: #166534;
  font-weight: 700;
}

.dot-green {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #16a34a;
}

.page-count {
  color: #64748b;
}

.audit-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.73rem;
}

.audit-table th {
  text-align: left;
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748b;
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.audit-table td {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.audit-table tr:hover td {
  background-color: #f8fafc;
}

.audit-table tr.active td {
  background-color: #f0f9ff;
}

.cell-ntp {
  font-family: monospace;
  font-size: 0.7rem;
  color: #0f172a;
}

.cell-user {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.76rem;
  color: #0f172a;
}

.user-meta {
  font-size: 0.64rem;
  color: #64748b;
}

.cell-ip {
  display: flex;
  flex-direction: column;
}

.ip-text {
  font-family: monospace;
  font-size: 0.7rem;
  color: #0284c7;
}

.mac-text {
  font-size: 0.6rem;
  color: #94a3b8;
  font-family: monospace;
}

.action-chip {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.66rem;
  font-weight: 700;
  background-color: #f1f5f9;
  color: #334155;
}

.action-chip.alert {
  background-color: #fee2e2;
  color: #b91c1c;
}

.res-code {
  font-family: monospace;
  font-size: 0.72rem;
  color: #0f172a;
}

.result-badge {
  display: inline-block;
  font-family: monospace;
  font-size: 0.66rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

.result-badge.success {
  background-color: #f0fdf4;
  color: #166534;
}

.result-badge.danger {
  background-color: #fef2f2;
  color: #b91c1c;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 0.68rem;
  color: #64748b;
}

.tf-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tf-right {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.btn-pg {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.68rem;
  cursor: pointer;
}

.btn-pg.active {
  background-color: #0f172a;
  color: #ffffff;
  border-color: #0f172a;
}

.dots {
  color: #94a3b8;
}

.inspection-sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.inspect-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.inspect-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.ih-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.ih-svg {
  width: 15px;
  height: 15px;
  color: #0284c7;
}

.ih-left h4 {
  font-size: 0.78rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.log-id-badge {
  font-size: 0.62rem;
  font-weight: 800;
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 0.12rem 0.35rem;
  border-radius: 4px;
  font-family: monospace;
}

.inspect-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.disclaimer-box {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.55rem;
  border-radius: 6px;
}

.disc-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
  flex-shrink: 0;
  margin-top: 1px;
}

.disclaimer-box p {
  margin: 0;
  font-size: 0.65rem;
  color: #64748b;
  line-height: 1.3;
}

.disclaimer-box strong {
  color: #0f172a;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.f-lbl {
  font-size: 0.64rem;
  font-weight: 700;
  color: #64748b;
}

.f-val-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.badge-status-code {
  font-family: monospace;
  font-size: 0.65rem;
  font-weight: 800;
  background-color: #f0fdf4;
  color: #166534;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.exec-card {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem;
}

.exec-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.exec-meta strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.exec-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.token-lbl {
  font-family: monospace;
  font-size: 0.6rem;
  color: #0284c7;
  margin-top: 2px;
}

.field-block-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.f-val-mono {
  display: block;
  font-family: monospace;
  font-size: 0.68rem;
  color: #0f172a;
}

.resource-box {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.45rem;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
}

.res-bold {
  font-family: monospace;
  font-size: 0.74rem;
  color: #0284c7;
}

.resource-box span {
  font-size: 0.62rem;
  color: #64748b;
}

.sha-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.valid-chip {
  font-size: 0.6rem;
  font-weight: 800;
  background-color: #f0fdf4;
  color: #166534;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.hash-box {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.4rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.62rem;
  color: #0f172a;
  word-break: break-all;
  line-height: 1.25;
}

.btn-validate-hash {
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

.btn-validate-hash:hover {
  background-color: #0369a1;
}

.worm-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.worm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.worm-header h4 {
  font-size: 0.74rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.worm-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.64rem;
  font-weight: 700;
  color: #16a34a;
}

.worm-body {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.68rem;
}

.worm-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.worm-line span {
  color: #64748b;
}

.worm-line strong {
  color: #0f172a;
}

.cenam-tag {
  color: #0284c7 !important;
}

.iso-declaration-footer {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}

.iso-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
  flex-shrink: 0;
  margin-top: 2px;
}

.iso-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.iso-text strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.iso-text p {
  margin: 0;
  font-size: 0.68rem;
  color: #475569;
  line-height: 1.35;
}
</style>
