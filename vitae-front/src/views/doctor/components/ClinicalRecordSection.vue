<script setup lang="ts">
import type { Paciente } from '../types'

defineProps<{
  pacienteSeleccionado: Paciente | null
}>()
</script>

<template>
  <div class="expediente-flow">
    <!-- Banner Paciente -->
    <div class="patient-banner-card">
      <div class="breadcrumbs">
        Atención clínica / Expediente clínico electrónico / 
        <span v-if="pacienteSeleccionado" class="status-open">📂 Expediente abierto · {{ pacienteSeleccionado.folio }}</span>
        <span v-else class="status-empty">📁 Esperando selección de paciente</span>
      </div>

      <div v-if="pacienteSeleccionado" class="patient-main-row">
        <div class="patient-avatar-square">
          {{ pacienteSeleccionado.nombre.split(' ').map(n => n[0]).slice(0, 2).join('') }}
        </div>
        <div class="patient-meta">
          <h2>{{ pacienteSeleccionado.nombre }}</h2>
          <p>{{ pacienteSeleccionado.sexo }} · {{ pacienteSeleccionado.edad }} años · <strong>ECE {{ pacienteSeleccionado.folio }}</strong> · Urgencias · {{ pacienteSeleccionado.box }}</p>
        </div>
        <div class="patient-actions">
          <span class="pill-badge-active">En atención</span>
        </div>
      </div>

      <div v-else class="empty-patient-box">
        <div class="empty-icon">👤</div>
        <div>
          <strong>Ningún paciente en consulta</strong>
          <p>Haga clic en uno de los pacientes de la lista izquierda para cargar sus signos vitales e historial.</p>
        </div>
      </div>
    </div>

    <!-- Signos Vitales -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Signos vitales</h4>
        <span v-if="pacienteSeleccionado?.signos" class="card-timestamp">Última toma · {{ pacienteSeleccionado.signos.horaToma }}</span>
        <span v-else class="card-timestamp">Esperando conexión backend</span>
      </div>

      <div v-if="pacienteSeleccionado?.signos" class="vitals-grid">
        <div class="vital-box">
          <span class="vital-label">Presión arterial</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.pa }}</strong> <small>mmHg</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Frecuencia cardíaca</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.fc }}</strong> <small>lpm</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Temperatura</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.temp }}</strong> <small>°C</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Saturación O₂</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.spo2 }}</strong> <small>%</small></div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon">📊</span>
        <strong>Módulo de Signos Vitales (Conexión Backend Pendiente)</strong>
        <p>Aquí se graficarán los signos del Triage una vez que Enfermería procese al paciente.</p>
      </div>
    </section>

    <!-- Resumen y Antecedentes -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Resumen y antecedentes</h4>
        <span class="card-timestamp">NOM-004-SSA3-2012</span>
      </div>

      <div v-if="pacienteSeleccionado?.antecedentes" class="summary-grid">
        <div class="summary-col">
          <h5>Motivo de consulta</h5>
          <p>{{ pacienteSeleccionado.antecedentes.motivo }}</p>
        </div>
        <div class="summary-col">
          <h5>Antecedentes clínicos</h5>
          <div class="alert-box-allergy">
            <strong>Alergias:</strong>
            <span>{{ pacienteSeleccionado.antecedentes.alergias }}</span>
          </div>
          <p><strong>Crónicos:</strong> {{ pacienteSeleccionado.antecedentes.cronicos }}</p>
          <p><strong>Medicación:</strong> {{ pacienteSeleccionado.antecedentes.medicacion }}</p>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon">📋</span>
        <strong>Resumen de Antecedentes (Esperando Selección)</strong>
        <p>Los antecedentes patológicos y alergias se cargarán desde el expediente central en PostgreSQL.</p>
      </div>
    </section>

    <!-- Historial Médico -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Historial médico · Última nota</h4>
        <span class="card-timestamp">Trazabilidad Inalterable</span>
      </div>

      <div v-if="pacienteSeleccionado?.ultimaNota" class="medical-note-content">
        <div class="note-top-bar">
          <div>
            <strong>{{ pacienteSeleccionado.ultimaNota.titulo }}</strong><br/>
            <small class="note-author">{{ pacienteSeleccionado.ultimaNota.medico }} · Folio {{ pacienteSeleccionado.ultimaNota.folio }}</small>
          </div>
          <span class="pill-locked">🔒 Firmada · Solo lectura</span>
        </div>
        <p class="note-text">{{ pacienteSeleccionado.ultimaNota.contenido }}</p>
        <div class="nom004-footer">
          <div class="nom004-info">
            <strong>Documento bloqueado · NOM-004</strong>
            <span>Las adendas se realizan exclusivamente mediante Nota Aclaratoria.</span>
          </div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon">📑</span>
        <strong>Historial Clínico (Sin registros cargados)</strong>
        <p>Esta sección consultará las notas médicas históricas de la tabla <code>expediente_clinico</code>.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.expediente-flow {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.patient-banner-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.breadcrumbs {
  font-size: 0.72rem;
  color: #64748B;
  margin-bottom: 0.6rem;
}

.status-open {
  color: #10B981;
  font-weight: 600;
}

.status-empty {
  color: #94A3B8;
  font-style: italic;
}

.patient-main-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.patient-avatar-square {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background-color: #E0F2FE;
  color: #0369A1;
  font-weight: 800;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.patient-meta h2 {
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0;
  color: #1E293B;
}

.patient-meta p {
  font-size: 0.78rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
}

.pill-badge-active {
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.3rem 0.6rem;
  border-radius: 9999px;
}

.empty-patient-box {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 0.85rem;
  font-size: 0.8rem;
}

.empty-icon {
  font-size: 1.8rem;
}

.empty-patient-box strong {
  display: block;
  color: #1E293B;
}

.empty-patient-box p {
  margin: 0;
  color: #64748B;
  font-size: 0.75rem;
}

/* CARDS CLÍNICAS */
.clinical-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
}

.card-header h4 {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
}

.card-timestamp {
  font-size: 0.72rem;
  color: #94A3B8;
}

.vitals-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.vital-box {
  display: flex;
  flex-direction: column;
}

.vital-label {
  font-size: 0.7rem;
  color: #64748B;
  margin-bottom: 0.2rem;
}

.vital-value {
  font-size: 1.25rem;
  color: #1E293B;
}

.vital-value small {
  font-size: 0.72rem;
  color: #94A3B8;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  font-size: 0.8rem;
  line-height: 1.4;
}

.summary-col h5 {
  font-size: 0.78rem;
  font-weight: 700;
  color: #475569;
  margin: 0 0 0.4rem 0;
}

.alert-box-allergy {
  background-color: #FFF1F2;
  border: 1px solid #FFE4E6;
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  margin-bottom: 0.5rem;
  color: #E11D48;
}

/* PLACEHOLDER */
.placeholder-box {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 1.25rem;
  text-align: center;
}

.placeholder-icon {
  font-size: 1.8rem;
  display: block;
  margin-bottom: 0.3rem;
}

.placeholder-box strong {
  display: block;
  font-size: 0.85rem;
  color: #334155;
  margin-bottom: 0.2rem;
}

.placeholder-box p {
  font-size: 0.75rem;
  color: #64748B;
  margin: 0;
}

/* NOTA CLÍNICA */
.medical-note-content {
  background-color: #F8FAFC;
  border: 1px solid #EDF2F7;
  border-radius: 8px;
  padding: 0.85rem;
}

.note-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.note-author {
  color: #64748B;
  font-size: 0.7rem;
}

.pill-locked {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.7rem;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.note-text {
  font-size: 0.8rem;
  line-height: 1.45;
  color: #334155;
  margin-bottom: 0.8rem;
}

.nom004-footer {
  border-top: 1px solid #E2E8F0;
  padding-top: 0.6rem;
  font-size: 0.72rem;
}

.nom004-info strong {
  display: block;
  color: #1E293B;
}

.nom004-info span {
  color: #64748B;
}

@media (max-width: 768px) {
  .vitals-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
