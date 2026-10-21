<script setup lang="ts">
import type { Paciente } from '../types'

defineProps<{
  seccionActiva: 'nota' | 'receta' | 'lab' | 'triage' | 'cola'
  pacientesCola: Paciente[]
}>()

defineEmits<{
  (e: 'cambiarSeccion', sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'): void
  (e: 'seleccionarPaciente', paciente: Paciente): void
}>()
</script>

<template>
  <div>
    <!-- MODO 2: NUEVA NOTA ECE -->
    <div v-if="seccionActiva === 'nota'" class="placeholder-section-card">
      <div class="placeholder-section-header">
        <h3>📝 Módulo: Redacción de Nueva Nota Médica ECE</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon">📋</span>
        <h4>Sección en preparación para actualización de Backend</h4>
        <p>Aquí se integrará el editor estructurado de notas de evolución médica con sellado de tiempo UTC, antecedentes y diagnósticos CIE-10 (NOM-004-SSA3-2012).</p>
        <div class="tech-spec-box">
          <code>Endpoint previsto: POST /api/v1/expedientes/{id}/notas</code>
        </div>
      </div>
    </div>

    <!-- MODO 3: EMISIÓN DE RECETA EXPANDIDA -->
    <div v-else-if="seccionActiva === 'receta'" class="placeholder-section-card">
      <div class="placeholder-section-header">
        <h3>💊 Módulo: Catálogo y Emisión Avanzada de Recetas</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon">🏷️</span>
        <h4>Sección en preparación para conexión con farmacia</h4>
        <p>Buscador de fármacos por denominación genérica, validación de medicamentos controlados (Fracciones I, II y III) y generación de código QR único antifalsificación.</p>
        <div class="tech-spec-box">
          <code>Endpoint previsto: POST /api/v1/recetas/firmar</code>
        </div>
      </div>
    </div>

    <!-- MODO 4: SOLICITUD DE LABORATORIO -->
    <div v-else-if="seccionActiva === 'lab'" class="placeholder-section-card">
      <div class="placeholder-section-header">
        <h3>🔬 Módulo: Solicitudes Auxiliares de Diagnóstico (Laboratorio e Imagen)</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon">🧪</span>
        <h4>Sección en preparación para interoperabilidad HL7/FHIR</h4>
        <p>Bandeja para ordenar biometrías, químicas sanguíneas, radiografías y tomografías vinculadas al laboratorista en turno.</p>
        <div class="tech-spec-box">
          <code>Recurso FHIR previsto: ServiceRequest / Observation</code>
        </div>
      </div>
    </div>

    <!-- MODO 5: RECLASIFICAR TRIAGE -->
    <div v-else-if="seccionActiva === 'triage'" class="placeholder-section-card">
      <div class="placeholder-section-header">
        <h3>〽️ Módulo: Reclasificación Manual de Urgencias</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon">⚠️</span>
        <h4>Sección con regla de negocio de auditoría (NOM-024)</h4>
        <p>Permitirá al médico reclasificar el color asignado por Triage (Manchester) con exigencia obligatoria de justificación clínica en texto y temporizador de 30 minutos.</p>
        <div class="tech-spec-box">
          <code>Registro en tabla: AUDITORIA_I (Evento: TRIAGE_RECLASSIFIED)</code>
        </div>
      </div>
    </div>

    <!-- MODO 6: COLA DE ESPERA COMPLETA -->
    <div v-else-if="seccionActiva === 'cola'" class="placeholder-section-card">
      <div class="placeholder-section-header">
        <h3>👥 Monitor General de la Cola de Espera de Urgencias</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon">⏱️</span>
        <h4>Visualización global de salas y boxes en tiempo real</h4>
        <p>Seleccione un paciente de la tabla para abrir inmediatamente su expediente:</p>
        <div class="patient-picker-table">
          <table>
            <thead>
              <tr>
                <th>Paciente</th>
                <th>Nivel Manchester</th>
                <th>Tiempo Espera</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in pacientesCola" :key="p.id">
                <td><strong>{{ p.nombre }}</strong> ({{ p.folio }})</td>
                <td><span class="level-indicator" :style="{ color: p.triageColor }">● {{ p.triage }}</span></td>
                <td>{{ p.transcurridosMin }} min</td>
                <td>
                  <button class="btn-subtle" @click="$emit('seleccionarPaciente', p)">
                    Atender en este Box
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.placeholder-section-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.5rem;
}

.placeholder-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.8rem;
  margin-bottom: 1.5rem;
}

.placeholder-section-header h3 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
}

.placeholder-section-body {
  text-align: center;
  max-width: 600px;
  margin: 2rem auto;
}

.big-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 1rem;
}

.placeholder-section-body h4 {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 0.5rem;
}

.placeholder-section-body p {
  font-size: 0.88rem;
  color: #64748B;
  line-height: 1.5;
  margin-bottom: 1.25rem;
}

.tech-spec-box {
  background-color: #F1F5F9;
  padding: 0.6rem 1rem;
  border-radius: 6px;
  display: inline-block;
}

.tech-spec-box code {
  font-family: monospace;
  font-size: 0.82rem;
  color: #0F4C81;
}

.btn-subtle {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-subtle:hover {
  background-color: #F1F5F9;
}

.patient-picker-table {
  margin-top: 1.5rem;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
}

.patient-picker-table table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.82rem;
}

.patient-picker-table th {
  background-color: #F8FAFC;
  padding: 0.65rem 1rem;
  color: #475569;
  font-weight: 700;
  border-bottom: 1px solid #E2E8F0;
}

.patient-picker-table td {
  padding: 0.65rem 1rem;
  border-bottom: 1px solid #F1F5F9;
}

.level-indicator {
  font-weight: 600;
}
</style>
