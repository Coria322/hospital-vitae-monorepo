<script setup lang="ts">
import type { Paciente, Doctor } from '../types'

defineProps<{
  pacienteSeleccionado: Paciente | null
  doctor: Doctor
}>()

defineEmits<{
  (e: 'cambiarSeccion', sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'): void
}>()
</script>

<template>
  <aside class="right-panel">
    <!-- TARJETA: TRATAMIENTO Y PRESCRIPCIÓN OFICIAL -->
    <div class="prescription-summary-card">
      <div class="card-top-head">
        <div class="title-with-icon">
          <span class="rx-icon">💊</span>
          <h3>Tratamiento y Recetas</h3>
        </div>
        <span class="badge-status-pill">Oficial</span>
      </div>

      <div v-if="pacienteSeleccionado" class="patient-rx-meta">
        <strong>{{ pacienteSeleccionado.nombre }}</strong>
        <small>ECE {{ pacienteSeleccionado.ece }} · Episodio de Urgencias</small>
      </div>
      <div v-else class="patient-rx-meta disabled">
        <strong>Sin paciente seleccionado</strong>
        <small>Seleccione un paciente para gestionar su prescripción</small>
      </div>

      <!-- RESUMEN DE FARMACOS -->
      <div v-if="pacienteSeleccionado?.antecedentes?.medicacion" class="active-meds-box">
        <span class="meds-lbl">Medicación habitual / de base:</span>
        <p class="meds-val">{{ pacienteSeleccionado.antecedentes.medicacion }}</p>
      </div>

      <div class="rx-action-box">
        <p class="rx-callout-text">
          La emisión, firma con e.firma y control de interacciones de medicamentos se gestionan en el módulo central de prescripción.
        </p>
        <button 
          class="btn-open-full-rx"
          :disabled="!pacienteSeleccionado"
          @click="$emit('cambiarSeccion', 'receta')"
        >
          <span>📋</span>
          <span>Emitir Receta Oficial [Alt+R]</span>
        </button>
      </div>
    </div>

    <!-- TARJETA: ESTUDIOS DIAGNÓSTICOS -->
    <div class="studies-card">
      <div class="studies-header">
        <h4>Estudios diagnósticos</h4>
        <span class="badge-subtle">HL7 / FHIR</span>
      </div>
      <div class="studies-body">
        <div class="tube-icon">🧪</div>
        <div class="studies-text">
          <p>Bandeja de solicitudes de laboratorio clínico, imagenología y gabinete.</p>
          <button 
            class="btn-open-lab" 
            :disabled="!pacienteSeleccionado"
            @click="$emit('cambiarSeccion', 'lab')"
          >
            Solicitar Laboratorio o Imagen [Alt+L] →
          </button>
        </div>
      </div>
    </div>

    <!-- NOTA NORMATIVA DISCRETA -->
    <div class="normative-pill-box">
      <span>🛡️ NOM-004-SSA3-2012 · Expediente Único</span>
    </div>
  </aside>
</template>

<style scoped>
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.prescription-summary-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.1rem;
}

.card-top-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.title-with-icon h3 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.badge-status-pill {
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.patient-rx-meta strong {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
}

.patient-rx-meta small {
  display: block;
  font-size: 0.68rem;
  color: #64748B;
  margin-bottom: 0.75rem;
}

.patient-rx-meta.disabled {
  opacity: 0.6;
}

.active-meds-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.55rem 0.7rem;
  margin-bottom: 0.75rem;
}

.meds-lbl {
  display: block;
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748B;
  margin-bottom: 0.15rem;
}

.meds-val {
  font-size: 0.75rem;
  color: #1E293B;
  margin: 0;
  line-height: 1.35;
}

.rx-action-box {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.rx-callout-text {
  font-size: 0.7rem;
  color: #64748B;
  margin: 0;
  line-height: 1.35;
}

.btn-open-full-rx {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 0.85rem;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-open-full-rx:hover:not(:disabled) {
  background-color: #0c3c64;
}

.btn-open-full-rx:disabled, .btn-open-lab:disabled {
  background-color: #94A3B8;
  cursor: not-allowed;
  opacity: 0.6;
}

/* TARJETA DE ESTUDIOS */
.studies-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
}

.studies-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.studies-header h4 {
  font-size: 0.82rem;
  font-weight: 700;
  margin: 0;
  color: #1E293B;
}

.badge-subtle {
  font-size: 0.65rem;
  color: #0284C7;
  font-weight: 700;
  background-color: #F0F9FF;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.studies-body {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  font-size: 0.72rem;
}

.tube-icon {
  width: 32px;
  height: 32px;
  background-color: #EFF6FF;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.studies-text p {
  margin: 0 0 0.5rem 0;
  color: #64748B;
  line-height: 1.35;
}

.btn-open-lab {
  background: none;
  border: none;
  padding: 0;
  color: #2563EB;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-open-lab:hover:not(:disabled) {
  text-decoration: underline;
}

.normative-pill-box {
  text-align: center;
  font-size: 0.65rem;
  color: #94A3B8;
}
</style>
