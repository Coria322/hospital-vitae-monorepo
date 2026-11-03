<script setup lang="ts">
defineProps<{
  statCount: number
  urgenteCount: number
  tiempoPromedioStat: string
  vencimientoProximoMin: string
}>()
</script>

<template>
  <div class="lab-sla-banner">
    <div class="banner-left">
      <div class="banner-title-pill">
        <svg viewBox="0 0 24 24" class="banner-svg-pulse" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span class="banner-title">ALERTA URGENCIA DIAGNÓSTICA</span>
      </div>
      <div class="banner-description">
        <span class="highlight">{{ statCount }} Solicitud STAT en Código Rojo (Choque)</span>
        <span class="sep">•</span>
        <span>{{ urgenteCount }} Solicitudes en Código Naranja (Emergencia)</span>
      </div>
    </div>

    <div class="banner-right">
      <div class="metric-box">
        <div class="metric-lbl">
          <svg viewBox="0 0 24 24" class="metric-icon" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          <span>TIEMPO PROMEDIO STAT</span>
        </div>
        <div class="metric-val">{{ tiempoPromedioStat }}</div>
      </div>

      <div class="metric-box urgent">
        <div class="metric-lbl">
          <svg viewBox="0 0 24 24" class="metric-icon" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          <span>VENCIMIENTO PRÓXIMO</span>
        </div>
        <div class="metric-val danger">{{ vencimientoProximoMin }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lab-sla-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #991b1b, #b91c1c);
  color: #ffffff;
  padding: 0.65rem 1.25rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(185, 28, 28, 0.2);
  margin-bottom: 1rem;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.banner-title-pill {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  background-color: rgba(0, 0, 0, 0.25);
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.banner-svg-pulse {
  width: 16px;
  height: 16px;
  color: #fca5a5;
  animation: pulseIcon 1.6s infinite;
}

@keyframes pulseIcon {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.banner-description {
  font-size: 0.82rem;
  color: #fee2e2;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.banner-description .highlight {
  font-weight: 700;
  color: #ffffff;
}

.sep {
  color: #f87171;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.metric-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.metric-lbl {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: #fca5a5;
  letter-spacing: 0.04em;
}

.metric-icon {
  width: 12px;
  height: 12px;
}

.metric-val {
  font-size: 1.05rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: #ffffff;
}

.metric-val.danger {
  color: #fef08a;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}
</style>
