<script setup lang="ts">
type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

defineProps<{
  seccionActiva: SeccionRecepcion
  recepcionista?: {
    sede: string
  }
}>()

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: SeccionRecepcion): void
}>()
</script>

<template>
  <!-- SIDEBAR LATERAL IZQUIERDA -->
  <aside class="reception-sidebar">
    <!-- Tarjeta de Sede Hospitalaria Compacta -->
    <div class="hospital-sede-card-compact">
      <div class="sede-badge-row">
        <span class="sede-label">SEDE CENTRAL</span>
        <span class="status-chip-active">● Activo</span>
      </div>
      <h4>{{ recepcionista?.sede || 'Sede Principal Norte' }}</h4>
      <span class="sede-sub">Admisión y Registro</span>
    </div>

    <!-- Navegación de Módulos Operativos -->
    <div class="sidebar-menu-section">
      <span class="menu-heading">MÓDULOS OPERATIVOS</span>
      <nav class="sidebar-nav">
        
        <!-- Botón 1: Registrar Nuevo Paciente [Alt+P] -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'registrar' }"
          @click="emit('cambiarSeccion', 'registrar')"
        >
          <div class="nav-left">
            <span class="nav-icon">👤➕</span>
            <span class="nav-label">Registrar Nuevo Paciente</span>
          </div>
          <kbd class="shortcut-tag">Alt+P</kbd>
        </button>

        <!-- Botón 2: Búsqueda por CURP [Alt+B] -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'curp' }"
          @click="emit('cambiarSeccion', 'curp')"
        >
          <div class="nav-left">
            <span class="nav-icon">🪪</span>
            <span class="nav-label">Búsqueda por CURP</span>
          </div>
          <kbd class="shortcut-tag">Alt+B</kbd>
        </button>

        <!-- Botón 3: Agendar Cita [Alt+C] -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'agendar' }"
          @click="emit('cambiarSeccion', 'agendar')"
        >
          <div class="nav-left">
            <span class="nav-icon">📅</span>
            <span class="nav-label">Agendar Cita</span>
          </div>
          <kbd class="shortcut-tag">Alt+C</kbd>
        </button>

        <!-- Botón 4: Capturar Consentimiento -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'consentimiento' }"
          @click="emit('cambiarSeccion', 'consentimiento')"
        >
          <div class="nav-left">
            <span class="nav-icon">✍️</span>
            <span class="nav-label">Capturar Consentimiento</span>
          </div>
        </button>

        <!-- Botón 5: Monitor de Sala -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'monitor' }"
          @click="emit('cambiarSeccion', 'monitor')"
        >
          <div class="nav-left">
            <span class="nav-icon">🖥️</span>
            <span class="nav-label">Monitor de Sala</span>
          </div>
          <span class="count-badge">8</span>
        </button>

        <!-- Botón 6: Tablero de Camas y Censos -->
        <button 
          type="button" 
          class="nav-item-btn" 
          :class="{ active: seccionActiva === 'censo' }"
          @click="emit('cambiarSeccion', 'censo')"
        >
          <div class="nav-left">
            <span class="nav-icon">🗂️</span>
            <span class="nav-label">Tablero de Camas y Censos</span>
          </div>
        </button>

      </nav>
    </div>

    <!-- Aviso Inferior de Privacidad NOM-024 Compacto -->
    <div class="privacy-compliance-pill">
      <span>🛡️ NOM-024-SSA3 · RENAPO Activo</span>
    </div>
  </aside>
</template>

<style scoped>
/* SIDEBAR */
.reception-sidebar {
  background-color: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  height: 100%;
}

.hospital-sede-card-compact {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background-color: #F8FAFC;
  padding: 0.5rem 0.65rem;
}

.sede-badge-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.15rem;
}

.status-chip-active {
  background-color: #2563EB;
  color: #FFFFFF;
  font-size: 0.58rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 9999px;
}

.sede-label {
  font-size: 0.58rem;
  color: #64748B;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.hospital-sede-card-compact h4 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
  line-height: 1.2;
}

.sede-sub {
  font-size: 0.65rem;
  color: #64748B;
}

.privacy-compliance-pill {
  margin-top: auto;
  text-align: center;
  font-size: 0.65rem;
  color: #94A3B8;
  padding: 0.3rem 0;
}

.sede-info h4 {
  font-size: 0.82rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0.1rem 0;
}

.sede-sub {
  font-size: 0.65rem;
  color: #64748B;
}

.sidebar-menu-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.menu-heading {
  font-size: 0.65rem;
  font-weight: 800;
  color: #94A3B8;
  letter-spacing: 0.05em;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.nav-item-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background-color: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 0.55rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}

.nav-item-btn:hover {
  background-color: #F1F5F9;
  color: #0F4C81;
}

.nav-item-btn.active {
  background-color: #2563EB;
  color: #FFFFFF;
  border-color: #2563EB;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.shortcut-tag {
  background-color: rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.1);
  padding: 0.15rem 0.35rem;
  border-radius: 4px;
  font-size: 0.65rem;
  font-family: monospace;
}

.nav-item-btn.active .shortcut-tag {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: #FFFFFF;
}

.count-badge {
  background-color: #0284C7;
  color: #FFFFFF;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
}

.privacy-compliance-box {
  margin-top: auto;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.6rem;
  display: flex;
  gap: 0.5rem;
  font-size: 0.68rem;
  color: #64748B;
}

.privacy-compliance-box strong {
  display: block;
  color: #1E293B;
  font-size: 0.7rem;
}
</style>
