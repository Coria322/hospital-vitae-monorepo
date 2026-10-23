<script setup lang="ts">
import type { Paciente } from '../types'

defineProps<{
  pacienteSeleccionado: Paciente | null
}>()

defineEmits<{
  (e: 'deseleccionarPaciente'): void
}>()
</script>

<template>
  <!-- BANNER DE PRIORIDAD SLA CON PACIENTE SELECCIONADO -->
  <section v-if="pacienteSeleccionado" class="priority-sla-bar">
    <div class="sla-left">
      <div class="clock-alert-icon" :style="{ color: pacienteSeleccionado.triageColor }">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-4.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/>
        </svg>
      </div>
      <div class="sla-patient-info">
        <small class="tag-alert" :style="{ color: pacienteSeleccionado.triageColor }">PACIENTE EN ATENCIÓN CLÍNICA</small>
        <div class="sla-name-row">
          <strong>{{ pacienteSeleccionado.nombre }}</strong>
          <span class="dot-sep">•</span>
          <span class="sub-pill">ECE {{ pacienteSeleccionado.ece }} ({{ pacienteSeleccionado.folio }})</span>
        </div>
      </div>
      <div class="manchester-pill" :style="{ backgroundColor: pacienteSeleccionado.triageColor }">
        {{ pacienteSeleccionado.triage }}
      </div>
      <span class="sla-limit-text">{{ pacienteSeleccionado.limiteSla }}</span>
    </div>

    <div class="sla-right">
      <div class="sla-metric">
        <strong>{{ pacienteSeleccionado.transcurridosMin }} <small>min</small></strong>
        <span>TRANSCURRIDOS</span>
      </div>
      <div class="sla-metric alert-urgent" :style="{ color: pacienteSeleccionado.triageColor }">
        <strong>{{ pacienteSeleccionado.restantesMin }} <small>min</small></strong>
        <span>RESTANTES SLA</span>
      </div>
      <button class="btn-change-patient" @click="$emit('deseleccionarPaciente')">
        ✕ Cambiar
      </button>
    </div>
  </section>

  <!-- BANNER VACÍO HASTA SELECCIÓN -->
  <section v-else class="empty-sla-bar">
    <div class="empty-sla-content">
      <span class="status-pulse-dot"></span>
      <span>Sin paciente en consulta activa. Seleccione uno de <strong>Mis Pacientes</strong> en la barra lateral para inicializar el expediente.</span>
    </div>
  </section>
</template>

<style scoped>
.priority-sla-bar {
  background-color: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  padding: 0.5rem clamp(1rem, 2.5vw, 2rem);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.sla-left {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.clock-alert-icon {
  width: 26px;
  height: 26px;
}

.tag-alert {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  display: block;
}

.sla-name-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.88rem;
}

.dot-sep {
  color: #CBD5E1;
}

.sub-pill {
  color: #0284C7;
  font-weight: 600;
  font-size: 0.82rem;
}

.manchester-pill {
  color: #FFFFFF;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 9999px;
}

.sla-limit-text {
  font-size: 0.75rem;
  color: #64748B;
}

.sla-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.sla-metric {
  text-align: right;
}

.sla-metric strong {
  font-size: 1.25rem;
  font-weight: 800;
  display: block;
  line-height: 1;
}

.sla-metric span {
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748B;
}

.btn-change-patient {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-change-patient:hover {
  background-color: #E2E8F0;
  color: #0F172A;
}

.empty-sla-bar {
  background-color: #F8FAFC;
  border-bottom: 1px solid #E2E8F0;
  padding: 0.6rem clamp(1rem, 2.5vw, 2rem);
  font-size: 0.8rem;
  color: #64748B;
}

.empty-sla-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #F59E0B;
}
</style>
