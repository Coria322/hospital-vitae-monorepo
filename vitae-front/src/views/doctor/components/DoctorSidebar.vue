<script setup lang="ts">
import type { Paciente } from '../types'

defineProps<{
  seccionActiva: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'
  pacientesCola: Paciente[]
  pacienteSeleccionado: Paciente | null
}>()

defineEmits<{
  (e: 'cambiarSeccion', sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'): void
  (e: 'seleccionarPaciente', paciente: Paciente): void
}>()
</script>

<template>
  <aside class="left-sidebar">
    <div class="hospital-banner-card">
      <div class="hospital-overlay"></div>
      <div class="hospital-info">
        <div class="banner-badge-top">
          <span class="badge-sede">SEDE CENTRAL</span>
          <span class="badge-status-online">Activo</span>
        </div>
        <strong class="hospital-name">Complejo Hospitalario Vitae</strong>
      </div>
    </div>

    <div class="section-title">
      <small>ESPACIO MÉDICO</small>
      <h3>Atención clínica</h3>
    </div>

    <nav class="quick-tools-nav">
      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'atencion' }"
        @click="$emit('cambiarSeccion', 'atencion')"
      >
        <span class="tool-icon">🩺</span>
        <span class="tool-label">Expediente Clínico</span>
        <kbd>Alt+E</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'nota' }"
        @click="$emit('cambiarSeccion', 'nota')"
      >
        <span class="tool-icon">📝</span>
        <span class="tool-label">Nueva Nota ECE</span>
        <kbd>Alt+N</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'receta' }"
        @click="$emit('cambiarSeccion', 'receta')"
      >
        <span class="tool-icon">📋</span>
        <span class="tool-label">Emitir Receta</span>
        <kbd class="kbd-blue">Alt+R</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'lab' }"
        @click="$emit('cambiarSeccion', 'lab')"
      >
        <span class="tool-icon">🔬</span>
        <span class="tool-label">Solicitar Laboratorio</span>
        <kbd>Alt+L</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'triage' }"
        @click="$emit('cambiarSeccion', 'triage')"
      >
        <span class="tool-icon">〽️</span>
        <span class="tool-label">Reclasificar Triage</span>
        <kbd>Alt+T</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'cola' }"
        @click="$emit('cambiarSeccion', 'cola')"
      >
        <span class="tool-icon">👥</span>
        <span class="tool-label">Cola de Espera</span>
        <span class="count-pill">{{ String(pacientesCola.length).padStart(2, '0') }}</span>
      </button>
    </nav>

    <!-- Cola de Pacientes -->
    <div class="patients-queue">
      <div class="queue-header">
        <h4>Mis pacientes</h4>
        <span class="queue-count">{{ pacientesCola.length }} en espera</span>
      </div>

      <div class="sub-tab-pill">
        ✓ Asignados a mi Box
      </div>

      <div class="queue-list">
        <div 
          v-for="p in pacientesCola" 
          :key="p.id" 
          class="queue-item"
          :class="{ selected: pacienteSeleccionado?.id === p.id }"
          @click="$emit('seleccionarPaciente', p)"
          title="Haga clic para cargar este paciente"
        >
          <div class="queue-item-top">
            <strong>{{ p.nombre }}</strong>
            <span class="queue-time">{{ p.transcurridosMin }} min</span>
          </div>
          <div class="queue-item-bottom">
            <span>{{ p.motivo }}</span>
            <span class="level-indicator" :style="{ color: p.triageColor }">
              ● {{ p.triage.split('·')[0]?.trim() || p.triage }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="sidebar-footer-lock">
      <span>🛡️ NOM-024-SSA3 · Sesión Médica Segura</span>
    </div>
  </aside>
</template>

<style scoped>
.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.hospital-banner-card {
  height: 52px;
  border-radius: 8px;
  position: relative;
  background-image: url('@/assets/hospital-fachada.jpg');
  background-size: cover;
  background-position: center;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 0.5rem 0.65rem;
  color: #FFFFFF;
}

.hospital-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 30, 54, 0.75) 0%, rgba(8, 20, 40, 0.92) 100%);
}

.hospital-info {
  position: relative;
  z-index: 2;
  width: 100%;
}

.banner-badge-top {
  display: flex;
  justify-content: space-between;
  font-size: 0.58rem;
  font-weight: 700;
  margin-bottom: 0.1rem;
}

.badge-sede {
  color: #38BDF8;
}

.badge-status-online {
  background-color: #10B981;
  padding: 0.05rem 0.35rem;
  border-radius: 3px;
  font-size: 0.55rem;
}

.hospital-name {
  font-size: 0.75rem;
  font-weight: 700;
  display: block;
}

.section-title small {
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748B;
  letter-spacing: 0.05em;
  display: block;
}

.section-title h3 {
  font-size: 1.05rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
}

.quick-tools-nav {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  border-radius: 8px;
  border: 1px solid transparent;
  background-color: #FFFFFF;
  color: #1E293B;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tool-btn:hover {
  background-color: #F1F5F9;
}

.tool-btn.active {
  background-color: #2563EB;
  color: #FFFFFF;
}

.tool-btn.active kbd {
  background: rgba(255, 255, 255, 0.2);
  color: #FFFFFF;
  border: none;
}

.tool-icon {
  margin-right: 0.4rem;
}

.tool-label {
  flex: 1;
  text-align: left;
}

kbd {
  font-family: monospace;
  font-size: 0.68rem;
  padding: 0.15rem 0.35rem;
  background: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  color: #64748B;
}

.count-pill {
  background-color: #E2E8F0;
  color: #0F4C81;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 9999px;
}

/* LISTA DE PACIENTES DE LA COLA */
.patients-queue {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.75rem;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.queue-header h4 {
  font-size: 0.8rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
}

.queue-count {
  font-size: 0.68rem;
  color: #64748B;
}

.sub-tab-pill {
  background-color: #EFF6FF;
  color: #1E40AF;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.3rem 0.5rem;
  border-radius: 6px;
  margin-bottom: 0.6rem;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.queue-item {
  padding: 0.5rem;
  border-radius: 6px;
  background-color: #F8FAFC;
  border: 1px solid #EDF2F7;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.queue-item:hover {
  border-color: #3B82F6;
  background-color: #EFF6FF;
}

.queue-item.selected {
  border-color: #2563EB;
  background-color: #DBEAFE;
}

.queue-item-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.15rem;
}

.queue-time {
  color: #64748B;
  font-size: 0.7rem;
}

.queue-item-bottom {
  display: flex;
  justify-content: space-between;
  font-size: 0.68rem;
  color: #64748B;
}

.level-indicator {
  font-weight: 600;
}

.sidebar-footer-lock {
  font-size: 0.68rem;
  color: #94A3B8;
  line-height: 1.3;
}
</style>
