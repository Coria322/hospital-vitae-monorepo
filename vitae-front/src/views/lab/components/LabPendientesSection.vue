<script setup lang="ts">
import { ref, computed } from 'vue'
import type { EstudioDiagnostico, CategoriaEstudio } from '../types'

const props = defineProps<{
  orders: EstudioDiagnostico[]
}>()

const emit = defineEmits<{
  (e: 'select-order', order: EstudioDiagnostico): void
  (e: 'cargar-estudio', order: EstudioDiagnostico): void
}>()

const filtroCategoria = ref<string>('TODOS')
const busquedaTexto = ref('')

const pedidosFiltrados = computed(() => {
  return props.orders.filter(item => {
    // Filtro por tab de categoría
    if (filtroCategoria.value === 'STAT' && item.prioridad !== 'STAT') return false
    if (filtroCategoria.value === 'LABORATORIO' && item.categoria !== 'LABORATORIO') return false
    if (filtroCategoria.value === 'IMAGENOLOGIA' && item.categoria !== 'IMAGENOLOGIA') return false

    // Filtro por texto
    if (busquedaTexto.value.trim()) {
      const q = busquedaTexto.value.toLowerCase().trim()
      const matchFolio = item.folio.toLowerCase().includes(q)
      const matchPac = item.paciente_iniciales.toLowerCase().includes(q) || item.paciente_nombre.toLowerCase().includes(q)
      const matchEstudio = item.estudio_requerido.toLowerCase().includes(q)
      const matchServicio = item.servicio_solicitante.toLowerCase().includes(q)
      return matchFolio || matchPac || matchEstudio || matchServicio
    }

    return true
  })
})

const countStat = computed(() => props.orders.filter(o => o.prioridad === 'STAT').length)
const countLab = computed(() => props.orders.filter(o => o.categoria === 'LABORATORIO').length)
const countImg = computed(() => props.orders.filter(o => o.categoria === 'IMAGENOLOGIA').length)
</script>

<template>
  <div class="lab-pendientes-section">
    <!-- CABECERA DE BANDEJA -->
    <div class="section-topbar">
      <div class="topbar-left">
        <div class="topbar-title-row">
          <svg viewBox="0 0 24 24" class="topbar-icon" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-6l-2 3h-4l-2-3H2"/>
            <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>
          </svg>
          <h2>Solicitudes Pendientes</h2>
          <span class="badge-total">Total: {{ orders.length }} Estudios</span>
        </div>
        <span class="norma-caption">NOM-024 / NOM-007 • Consulta y captura de auxiliares de diagnóstico clínico</span>
      </div>

      <div class="topbar-right">
        <button type="button" class="btn-refresh" title="Actualizar bandeja de entrada">
          <svg viewBox="0 0 24 24" class="btn-icon" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          <span>Sincronizar LIS</span>
        </button>
      </div>
    </div>

    <!-- BARRA DE FILTROS Y BÚSQUEDA -->
    <div class="filter-bar">
      <div class="filter-pills">
        <button
          type="button"
          class="pill-btn"
          :class="{ active: filtroCategoria === 'TODOS' }"
          @click="filtroCategoria = 'TODOS'"
        >
          Todos ({{ orders.length }})
        </button>
        <button
          type="button"
          class="pill-btn stat-pill"
          :class="{ active: filtroCategoria === 'STAT' }"
          @click="filtroCategoria = 'STAT'"
        >
          <svg viewBox="0 0 24 24" class="pill-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          STAT / Choque ({{ countStat }})
        </button>
        <button
          type="button"
          class="pill-btn"
          :class="{ active: filtroCategoria === 'LABORATORIO' }"
          @click="filtroCategoria = 'LABORATORIO'"
        >
          Laboratorio ({{ countLab }})
        </button>
        <button
          type="button"
          class="pill-btn"
          :class="{ active: filtroCategoria === 'IMAGENOLOGIA' }"
          @click="filtroCategoria = 'IMAGENOLOGIA'"
        >
          Imagenología ({{ countImg }})
        </button>
      </div>

      <div class="search-box">
        <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="busquedaTexto"
          type="text"
          placeholder="Filtrar por folio, iniciales, estudio o servicio solicitante..."
          class="search-input"
        />
        <button v-if="busquedaTexto" class="clear-search-btn" @click="busquedaTexto = ''">
          <svg viewBox="0 0 24 24" class="clear-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- LISTA DE ESTUDIOS PENDIENTES -->
    <div class="orders-list">
      <div
        v-for="orden in pedidosFiltrados"
        :key="orden.id"
        class="order-card"
        :class="{
          'priority-red': orden.codigo_color === 'ROJO',
          'priority-orange': orden.codigo_color === 'NARANJA',
          'priority-gray': orden.codigo_color === 'GRIS'
        }"
      >
        <!-- CABECERA DE TARJETA -->
        <div class="card-header">
          <div class="badge-priority" :class="orden.codigo_color.toLowerCase()">
            <span v-if="orden.prioridad === 'STAT'">CÓDIGO ROJO • STAT</span>
            <span v-else-if="orden.prioridad === 'URGENTE'">CÓDIGO NARANJA • URGENTE</span>
            <span v-else>ORDINARIA • {{ orden.categoria }}</span>
          </div>

          <div class="order-identifiers">
            <span class="order-folio">{{ orden.folio }}</span>
            <span class="patient-pill">PAC: {{ orden.paciente_iniciales }} ({{ orden.paciente_edad }}a, {{ orden.paciente_sexo }})</span>
          </div>

          <div class="sla-pill" :class="{ danger: orden.sla_restante_min <= 10 }">
            <svg viewBox="0 0 24 24" class="sla-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>{{ orden.sla_restante_min }} min SLA Restante</span>
          </div>
        </div>

        <!-- CUERPO DE TARJETA -->
        <div class="card-body">
          <div class="card-col main-study">
            <span class="col-lbl">Estudio Requerido</span>
            <strong class="study-title">{{ orden.estudio_requerido }}</strong>
          </div>

          <div class="card-col origin">
            <span class="col-lbl">Origen Clínico</span>
            <span class="origin-service">{{ orden.servicio_solicitante }}</span>
          </div>

          <div class="card-col doctor">
            <span class="col-lbl">Médico Prescriptor</span>
            <span class="doc-name">{{ orden.medico_prescriptor }}</span>
          </div>
        </div>

        <!-- PIE DE TARJETA: ESTADO DE MUESTRA Y BOTÓN DE ACCIÓN -->
        <div class="card-footer">
          <div class="sample-status">
            <svg viewBox="0 0 24 24" class="sample-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
              <line x1="8.5" y1="2" x2="15.5" y2="2"/>
            </svg>
            <span v-if="orden.analizador_asignado" class="status-text">
              Muestra en Proceso (<strong>{{ orden.analizador_asignado }}</strong>) • Solicitud: {{ orden.hora_solicitud }} h
            </span>
            <span v-else class="status-text">
              Muestra Recibida y Registrada • Solicitud: {{ orden.hora_solicitud }} h
            </span>
          </div>

          <div class="card-actions">
            <button
              v-if="orden.prioridad === 'STAT'"
              type="button"
              class="btn-action stat-btn"
              @click="emit('cargar-estudio', orden)"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              <span>Subir Resultado STAT</span>
            </button>

            <button
              v-else-if="orden.categoria === 'IMAGENOLOGIA'"
              type="button"
              class="btn-action dicom-btn"
              @click="emit('cargar-estudio', orden)"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"/>
                <line x1="7" y1="2" x2="7" y2="22"/>
                <line x1="17" y1="2" x2="17" y2="22"/>
                <line x1="2" y1="12" x2="22" y2="12"/>
                <line x1="2" y1="7" x2="7" y2="7"/>
                <line x1="2" y1="17" x2="7" y2="17"/>
                <line x1="17" y1="17" x2="22" y2="17"/>
                <line x1="17" y1="7" x2="22" y2="7"/>
              </svg>
              <span>Vincular DICOM / PACS</span>
            </button>

            <button
              v-else
              type="button"
              class="btn-action normal-btn"
              @click="emit('cargar-estudio', orden)"
            >
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="12" y1="18" x2="12" y2="12"/>
                <line x1="9" y1="15" x2="15" y2="15"/>
              </svg>
              <span>Cargar Resultados</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="pedidosFiltrados.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" class="empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <span class="empty-title">No hay estudios pendientes con ese criterio</span>
        <span class="empty-sub">Intente ajustar los filtros de búsqueda o categoría.</span>
      </div>
    </div>

    <!-- RESUMEN DE TURNO INFERIOR -->
    <div class="turn-summary-footer">
      <div class="turn-summary-left">
        <svg viewBox="0 0 24 24" class="footer-svg" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 14 14"/>
        </svg>
        <span>Turno Activo: <strong>Matutino Laboratorio e Imagen Central</strong></span>
      </div>
      <div class="turn-summary-metrics">
        <span>Procesados hoy: <strong>68</strong></span>
        <span class="sep">•</span>
        <span>Tasa de rechazo: <strong>0.4%</strong></span>
        <span class="sep">•</span>
        <span class="sla-good">SLA Global: <strong>98.2% en tiempo</strong></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-pendientes-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-title-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.topbar-icon {
  width: 20px;
  height: 20px;
  color: #0284c7;
}

.topbar-title-row h2 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-total {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.norma-caption {
  font-size: 0.72rem;
  color: #64748b;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-refresh:hover {
  background-color: #f8fafc;
  border-color: #94a3b8;
  color: #0f172a;
}

.btn-icon {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.6rem 0.85rem;
}

.filter-pills {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.pill-btn {
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.pill-btn:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}

.pill-btn.active {
  background-color: #0284c7;
  border-color: #0284c7;
  color: #ffffff;
}

.pill-btn.stat-pill {
  color: #b91c1c;
  border-color: #fca5a5;
  background-color: #fef2f2;
}

.pill-btn.stat-pill.active {
  background-color: #b91c1c;
  border-color: #b91c1c;
  color: #ffffff;
}

.pill-svg {
  width: 13px;
  height: 13px;
}

.search-box {
  display: flex;
  align-items: center;
  position: relative;
  min-width: 320px;
}

.search-svg {
  position: absolute;
  left: 10px;
  width: 15px;
  height: 15px;
  color: #94a3b8;
}

.search-input {
  width: 100%;
  padding: 0.4rem 1.8rem 0.4rem 2rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.76rem;
  color: #0f172a;
  outline: none;
  background-color: #f8fafc;
}

.search-input:focus {
  border-color: #0284c7;
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.clear-search-btn {
  position: absolute;
  right: 6px;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px;
}

.clear-svg {
  width: 14px;
  height: 14px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.order-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.order-card:hover {
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.07);
  border-color: #cbd5e1;
}

.order-card.priority-red {
  border-left: 4px solid #b91c1c;
}

.order-card.priority-orange {
  border-left: 4px solid #ea580c;
}

.order-card.priority-gray {
  border-left: 4px solid #94a3b8;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.badge-priority {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.badge-priority.rojo {
  background-color: #fee2e2;
  color: #b91c1c;
}

.badge-priority.naranja {
  background-color: #ffedd5;
  color: #c2410c;
}

.badge-priority.gris {
  background-color: #f1f5f9;
  color: #475569;
}

.order-identifiers {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.order-folio {
  font-family: monospace;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
}

.patient-pill {
  font-size: 0.72rem;
  color: #475569;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.sla-pill {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  background-color: #f1f5f9;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.sla-pill.danger {
  background-color: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.sla-svg {
  width: 13px;
  height: 13px;
}

.card-body {
  display: grid;
  grid-template-columns: 2fr 1.2fr 1.2fr;
  gap: 1rem;
  background-color: #f8fafc;
  padding: 0.65rem 0.85rem;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.card-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.col-lbl {
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.study-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f172a;
}

.origin-service, .doc-name {
  font-size: 0.78rem;
  color: #334155;
  font-weight: 500;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sample-status {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.72rem;
  color: #64748b;
}

.sample-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.status-text strong {
  color: #0369a1;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.95rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.15s ease;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.stat-btn {
  background-color: #b91c1c;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(185, 28, 28, 0.25);
}

.stat-btn:hover {
  background-color: #991b1b;
}

.normal-btn {
  background-color: #0284c7;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.normal-btn:hover {
  background-color: #0369a1;
}

.dicom-btn {
  background-color: #0f766e;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(15, 118, 110, 0.25);
}

.dicom-btn:hover {
  background-color: #115e59;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  background-color: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  gap: 0.5rem;
}

.empty-svg {
  width: 36px;
  height: 36px;
  color: #94a3b8;
}

.empty-title {
  font-size: 0.88rem;
  font-weight: 700;
  color: #334155;
}

.empty-sub {
  font-size: 0.75rem;
  color: #64748b;
}

.turn-summary-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.6rem 0.85rem;
  font-size: 0.72rem;
  color: #475569;
}

.turn-summary-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.footer-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.turn-summary-metrics {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.sla-good {
  color: #15803d;
}

.sep {
  color: #cbd5e1;
}
</style>
