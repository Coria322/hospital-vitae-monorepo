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
        <h3><svg viewBox="0 0 24 24" class="svg-inline-edit" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg> Módulo: Redacción de Nueva Nota Médica ECE</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
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
        <h3><svg viewBox="0 0 24 24" class="svg-inline-pill" fill="none" stroke="currentColor" stroke-width="2"><path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/><path d="m8.5 8.5 7 7"/></svg> Módulo: Catálogo y Emisión Avanzada de Recetas</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z"/><circle cx="7" cy="7" r=".5" fill="currentColor"/></svg></span>
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
        <h3><svg viewBox="0 0 24 24" class="svg-inline-lab" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/><line x1="8.5" y1="2" x2="15.5" y2="2"/></svg> Módulo: Solicitudes Auxiliares de Diagnóstico (Laboratorio e Imagen)</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.5 3h15M6 3v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V3M6 14h12"/></svg></span>
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
        <h3><svg viewBox="0 0 24 24" class="svg-inline-activity" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg> Módulo: Reclasificación Manual de Urgencias</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon"><svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></span>
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
        <h3><svg viewBox="0 0 24 24" class="svg-inline-users" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg> Monitor General de la Cola de Espera de Urgencias</h3>
        <button class="btn-subtle" @click="$emit('cambiarSeccion', 'atencion')">← Volver al Expediente</button>
      </div>
      <div class="placeholder-section-body">
        <span class="big-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></span>
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
