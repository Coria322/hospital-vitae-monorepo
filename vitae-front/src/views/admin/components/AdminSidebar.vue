<script setup lang="ts">
defineProps<{
  activeTab: 'usuarios' | 'bloqueos' | 'bitacora' | 'docker'
  bloqueosCount: number
  totalUsuarios: number
}>()

const emit = defineEmits<{
  (e: 'select-tab', tab: 'usuarios' | 'bloqueos' | 'bitacora' | 'docker'): void
}>()
</script>

<template>
  <aside class="admin-sidebar">
    <!-- 1. IDENTIFICADOR COMPACTO DE SEDE -->
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
        <span class="branch-sub">Complejo Hospitalario Vitae</span>
      </div>
      <span class="badge-active">ACTIVO</span>
    </div>

    <!-- 2. NAVEGACIÓN PRINCIPAL -->
    <div class="nav-section">
      <div class="nav-section-title">
        <span>SEGURIDAD Y SISTEMAS</span>
        <span class="badge-sub-pill">TI-ROOT</span>
      </div>

      <nav class="nav-menu">
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'usuarios' }"
          @click="emit('select-tab', 'usuarios')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span class="nav-label">Gestión de Usuarios</span>
          </div>
          <span class="key-shortcut">[Alt+U]</span>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'bloqueos' }"
          @click="emit('select-tab', 'bloqueos')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span class="nav-label">Cuentas Bloqueadas</span>
          </div>
          <div class="nav-right">
            <span v-if="bloqueosCount > 0" class="badge-bloqueos">{{ bloqueosCount }}</span>
            <span class="key-shortcut">[Alt+B]</span>
          </div>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'bitacora' }"
          @click="emit('select-tab', 'bitacora')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <span class="nav-label">Bitácora NOM-024</span>
          </div>
          <span class="key-shortcut">[Alt+A]</span>
        </button>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeTab === 'docker' }"
          @click="emit('select-tab', 'docker')"
        >
          <div class="nav-left">
            <svg viewBox="0 0 24 24" class="nav-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
              <line x1="6" y1="6" x2="6.01" y2="6"/>
              <line x1="6" y1="18" x2="6.01" y2="18"/>
            </svg>
            <span class="nav-label">Estado del Sistema</span>
          </div>
          <span class="key-shortcut">[Alt+S]</span>
        </button>
      </nav>
    </div>

    <!-- 3. BALANCE OPERATIVO DE SEGURIDAD -->
    <div class="balance-card">
      <div class="balance-title-row">
        <svg viewBox="0 0 24 24" class="balance-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="20" x2="18" y2="10"/>
          <line x1="12" y1="20" x2="12" y2="4"/>
          <line x1="6" y1="20" x2="6" y2="14"/>
        </svg>
        <span>BALANCE OPERATIVO TI</span>
      </div>
      <div class="balance-item">
        <span class="balance-lbl">Usuarios Registrados</span>
        <span class="balance-val">{{ totalUsuarios }}</span>
      </div>
      <div class="balance-item critical">
        <span class="balance-lbl">Cuentas en Cuarentena</span>
        <span class="balance-val" :class="{ danger: bloqueosCount > 0 }">{{ bloqueosCount }}</span>
      </div>
      <div class="balance-item">
        <span class="balance-lbl">Contenedores Clúster</span>
        <span class="balance-val healthy">6 / 6 Online</span>
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
          <strong>ISO 27001 / NOM-024</strong>
          <span>Separación de Funciones: Auditoría estricta de accesos. Datos clínicos disociados sin acceso a notas médicas.</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.admin-sidebar {
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
  background-color: #0f172a;
  color: #ffffff;
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
  font-size: 0.66rem;
  color: #64748b;
}

.badge-active {
  font-size: 0.6rem;
  font-weight: 700;
  color: #16a34a;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
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
  background-color: #0f172a;
  color: #ffffff;
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
  background-color: #0f172a;
  color: #ffffff;
  border-color: #0f172a;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.25);
}

.nav-item.active .nav-svg {
  color: #ffffff;
}

.nav-item.active .key-shortcut {
  color: rgba(255, 255, 255, 0.75);
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

.badge-bloqueos {
  background-color: #b91c1c;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
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

.balance-val.healthy {
  color: #16a34a;
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
