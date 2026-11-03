<script setup lang="ts">
defineProps<{
  seccionActiva: 'triage' | 'reclasificar' | 'sala' | 'nota'
}>()

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: 'triage' | 'reclasificar' | 'sala' | 'nota'): void
  (e: 'activarChoque'): void
}>()
</script>

<template>
  <aside class="left-sidebar">
    <!-- Banner Fachada Hospital -->
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
      <small>ESPACIO CLÍNICO</small>
      <h3>Triage y Urgencias</h3>
    </div>

    <!-- BOTÓN CRÍTICO DE ACTIVAR CHOQUE -->
    <button class="btn-choque-directo" @click="emit('activarChoque')">
      <span class="choque-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></span>
      <div class="choque-text">
        <strong>ACTIVAR CHOQUE</strong>
        <small>&lt; 1s Respuesta Inmediata</small>
      </div>
    </button>

    <div class="nav-subhead">PROTOCOLOS Y FLUJOS</div>

    <!-- BOTONES DE NAVEGACIÓN ENTRE LAS PANTALLAS -->
    <nav class="quick-tools-nav">
      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'triage' }"
        @click="emit('cambiarSeccion', 'triage')"
      >
        <span class="tool-icon"><svg viewBox="0 0 24 24" class="svg-inline-activity" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg></span>
        <span class="tool-label">Capturar Signos Triage</span>
        <kbd class="kbd-blue">Alt+S</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'reclasificar' }"
        @click="emit('cambiarSeccion', 'reclasificar')"
      >
        <span class="tool-icon"><svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg></span>
        <span class="tool-label">Reclasificar Manchester</span>
        <span class="dot-indicator blue"></span>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'nota' }"
        @click="emit('cambiarSeccion', 'nota')"
      >
        <span class="tool-icon"><svg viewBox="0 0 24 24" class="svg-inline-edit" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
        <span class="tool-label">Nota de Enfermería</span>
        <kbd>Alt+E</kbd>
      </button>

      <button 
        class="tool-btn" 
        :class="{ active: seccionActiva === 'sala' }"
        @click="emit('cambiarSeccion', 'sala')"
      >
        <span class="tool-icon"><svg viewBox="0 0 24 24" class="svg-inline-users" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg></span>
        <span class="tool-label">Sala de Espera y Monitoreo</span>
        <span class="count-pill">03</span>
      </button>
    </nav>

    <div class="sidebar-footer-lock">
      <svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> Entorno clínico protegido<br/>
      <small>Trazabilidad auditada bajo protocolo de urgencias NOM-027 / NOM-024.</small>
    </div>
  </aside>
</template>

<style scoped>
.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
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
  border-radius: 4px;
}

.hospital-name {
  font-size: 0.78rem;
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

.btn-choque-directo {
  background-color: #7F1D1D;
  border: 1px solid #991B1B;
  color: #FFFFFF;
  padding: 0.65rem 0.8rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(127, 29, 29, 0.3);
}

.btn-choque-directo:hover {
  background-color: #991B1B;
  transform: translateY(-1px);
}

.choque-icon {
  font-size: 1.4rem;
}

.choque-text {
  text-align: left;
}

.choque-text strong {
  display: block;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
}

.choque-text small {
  display: block;
  font-size: 0.65rem;
  color: #FCA5A5;
}

.nav-subhead {
  font-size: 0.65rem;
  font-weight: 800;
  color: #94A3B8;
  letter-spacing: 0.05em;
  margin-top: 0.4rem;
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

.dot-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-indicator.blue {
  background-color: #38BDF8;
}

.sidebar-footer-lock {
  font-size: 0.68rem;
  color: #94A3B8;
  line-height: 1.3;
  margin-top: 1rem;
}
</style>
