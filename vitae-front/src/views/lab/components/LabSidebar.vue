<script setup lang="ts">
import type { PrioridadEstudio } from '../types'

defineProps<{
  activeTab: 'pendientes' | 'cargar' | 'busqueda' | 'historial'
  statCount: number
  totalCount: number
}>()

const emit = defineEmits<{
  (e: 'select-tab', tab: 'pendientes' | 'cargar' | 'busqueda' | 'historial'): void
}>()
</script>

<template>
  <aside class="lab-sidebar">
    <!-- 1. IDENTIFICADOR DE SEDE -->
    <div class="sidebar-branch-card">
      <div class="branch-icon-box">
        <svg viewBox="0 0 24 24" class="branch-svg" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="4" y="2" width="16" height="20" rx="2" ry="2"/>
          <path d="M9 22v-4h6v4"/>
          <line x1="8" y1="6" x2="8.01" y2="6"/>
          <line x1="16" y1="6" x2="16.01" y2="6"/>
          <line x1="12" y1="6" x2="12.01" y2="6"/>
          <line x1="12" y1="10" x2="12.01" y2="10"/>
          <line x1="12" y1="14" x2="12.01" y2="14"/>
        </svg>
      </div>
      <div class="branch-meta">
        <span class="branch-title">Sede Central</span>
        <span class="branch-sub">Hospital Vitae</span>
      </div>
      <span class="badge-active">ACTIVO</span>
    </div>

    <!-- 2. NAVEGACIÓN PRINCIPAL -->
    <div class="nav-section">
      <div class="nav-section-title">
        <span>SERVICIOS DIAGNÓSTICOS</span>
        <span class="badge-sub-pill">LAB-IMG</span>
      </div>

      <nav class="nav-menu">
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'pendientes' }"
          @click="emit('select-tab', 'pendientes')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 12h-6l-2 3h-4l-2-3H2"/>
              <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>
            </svg>
            <span class="nav-label">Bandeja Pendientes</span>
          </div>
          <div class="nav-right">
            <span v-if="statCount > 0" class="badge-stat">{{ statCount }} STAT</span>
            <span class="key-shortcut">[Alt+P]</span>
          </div>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'cargar' }"
          @click="emit('select-tab', 'cargar')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <span class="nav-label">Cargar Resultados</span>
          </div>
          <span class="key-shortcut">[Alt+C]</span>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'busqueda' }"
          @click="emit('select-tab', 'busqueda')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <span class="nav-label">Búsqueda por Folio</span>
          </div>
          <span class="key-shortcut">[Alt+B]</span>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'historial' }"
          @click="emit('select-tab', 'historial')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <span class="nav-label">Historial de Cargas</span>
          </div>
          <span class="key-shortcut">[Alt+H]</span>
        </button>
      </nav>
    </div>

    <!-- 3. BALANCE OPERATIVO -->
    <div class="balance-card">
      <div class="balance-title-row">
        <svg viewBox="0 0 24 24" class="balance-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="20" x2="18" y2="10"/>
          <line x1="12" y1="20" x2="12" y2="4"/>
          <line x1="6" y1="20" x2="6" y2="14"/>
        </svg>
        <span>BALANCE OPERATIVO</span>
      </div>
      <div class="balance-item">
        <span class="balance-lbl">Solicitudes Totales</span>
        <span class="balance-val">{{ totalCount }}</span>
      </div>
      <div class="balance-item critical">
        <span class="balance-lbl">Críticos / Código Rojo</span>
        <span class="balance-val danger">{{ statCount }}</span>
      </div>
    </div>

    <!-- 4. DISCLAIMER NORMATIVO / SEGURIDAD -->
    <div class="sidebar-footer">
      <div class="legal-badge">
        <svg viewBox="0 0 24 24" class="legal-svg" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <div class="legal-text">
          <strong>Acceso Restringido LIS/RIS</strong>
          <span>Sin acceso a expediente integral clínico (NOM-024 / NOM-007)</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.lab-sidebar {
  width: 250px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 1rem 0.85rem;
  height: calc(100vh - 60px);
  position: sticky;
  top: 60px;
  box-sizing: border-box;
  flex-shrink: 0;
  gap: 1.25rem;
}

.sidebar-branch-card {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.6rem 0.75rem;
}

.branch-icon-box {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: #e0f2fe;
  color: #0369a1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.branch-svg {
  width: 16px;
  height: 16px;
}

.branch-meta {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.branch-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: #0f172a;
}

.branch-sub {
  font-size: 0.68rem;
  color: #64748b;
}

.badge-active {
  font-size: 0.6rem;
  font-weight: 700;
  color: #0284c7;
  background-color: #e0f2fe;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.nav-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.68rem;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.04em;
  padding: 0 0.25rem;
}

.badge-sub-pill {
  background-color: #e2e8f0;
  color: #334155;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.75rem;
  border: 1px solid transparent;
  border-radius: 7px;
  background: none;
  cursor: pointer;
  color: #334155;
  font-size: 0.8rem;
  font-weight: 500;
  transition: all 0.15s ease;
  text-align: left;
}

.nav-item:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.nav-item.active {
  background-color: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.nav-item.active .nav-svg {
  color: #ffffff;
}

.nav-item.active .key-shortcut {
  color: rgba(255, 255, 255, 0.8);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.nav-svg {
  width: 16px;
  height: 16px;
  color: #64748b;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.badge-stat {
  background-color: #b91c1c;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.35rem;
  border-radius: 4px;
  line-height: 1;
}

.key-shortcut {
  font-size: 0.65rem;
  color: #94a3b8;
  font-family: monospace;
}

.balance-card {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.balance-title-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.68rem;
  font-weight: 700;
  color: #475569;
  letter-spacing: 0.04em;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.35rem;
}

.balance-icon-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.balance-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.74rem;
  color: #475569;
}

.balance-lbl {
  font-weight: 500;
}

.balance-val {
  font-weight: 700;
  color: #0f172a;
}

.balance-val.danger {
  color: #b91c1c;
  font-weight: 800;
}

.sidebar-footer {
  margin-top: auto;
}

.legal-badge {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.55rem;
}

.legal-svg {
  width: 16px;
  height: 16px;
  color: #64748b;
  flex-shrink: 0;
  margin-top: 2px;
}

.legal-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 0.65rem;
  line-height: 1.25;
  color: #64748b;
}

.legal-text strong {
  color: #334155;
  font-size: 0.68rem;
}
</style>
