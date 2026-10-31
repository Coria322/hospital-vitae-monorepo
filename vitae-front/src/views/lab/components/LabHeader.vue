<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import type { LabUser } from '../types'

const props = defineProps<{
  user: LabUser
  currentDateTimeStr: string
  totalPendientes: number
  statCount: number
}>()

const router = useRouter()
const menuAbierto = ref(false)

const iniciales = computed(() => {
  const partes = (props.user.nombre || '').replace(/^(Q\.F\.B\.|Lic\.|Dr\.|Dra\.)\s*/i, '').trim().split(' ')
  if (partes.length >= 2 && partes[0] && partes[1]) {
    return `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
  }
  return (partes[0]?.slice(0, 2) || 'MT').toUpperCase()
})

const toggleMenu = () => {
  menuAbierto.value = !menuAbierto.value
}

const cerrarMenu = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.user-menu-container')) {
    menuAbierto.value = false
  }
}

const handleCerrarSesion = async () => {
  menuAbierto.value = false
  await authService.logout()
  router.push('/')
}

onMounted(() => {
  window.addEventListener('click', cerrarMenu)
})

onUnmounted(() => {
  window.removeEventListener('click', cerrarMenu)
})
</script>

<template>
  <header class="lab-header">
    <div class="lab-header-left">
      <div class="lab-logo-box">
        <svg viewBox="0 0 24 24" class="lab-icon-svg" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
        </svg>
      </div>
      <div class="lab-brand-info">
        <div class="brand-title-row">
          <span class="brand-title">HOSPITAL VITAE</span>
          <span class="brand-sub">SISTEMA DE INFORMACIÓN HOSPITALARIA</span>
        </div>
        <div class="brand-dept-row">
          <span class="dept-name">{{ user.area }}</span>
          <span class="sep">•</span>
          <span class="dept-time">
            <svg viewBox="0 0 24 24" class="mini-icon" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            {{ currentDateTimeStr }}
          </span>
        </div>
      </div>
    </div>

    <div class="lab-header-center">
      <div class="chip-counter">
        <svg viewBox="0 0 24 24" class="chip-svg" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 12h-6l-2 3h-4l-2-3H2"/>
          <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>
        </svg>
        <span class="chip-num">{{ totalPendientes }}</span>
        <span class="chip-lbl">Pendientes</span>
      </div>

      <div v-if="statCount > 0" class="alert-triage-badge">
        <svg viewBox="0 0 24 24" class="alert-svg pulse" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
        <span>ALERTA TRIAGE: CÓDIGO ROJO / CHOQUE ACTIVO ({{ statCount }} STAT)</span>
      </div>
    </div>

    <div class="lab-header-right">
      <div class="user-badge user-menu-container">
        <div class="avatar-circle">
          {{ iniciales }}
        </div>
        <div class="user-info">
          <strong class="user-name">{{ user.nombre }}</strong>
          <span class="user-role">{{ user.rol }} · CÉD. {{ user.cedula }}</span>
        </div>
        <button
          type="button"
          class="btn-more-options"
          :class="{ active: menuAbierto }"
          aria-label="Opciones de usuario"
          title="Opciones de cuenta y sesión"
          @click.stop="toggleMenu"
        >
          ⋮
        </button>

        <!-- Menú flotante de perfil y cierre de sesión -->
        <transition name="fade-slide">
          <div v-if="menuAbierto" class="user-dropdown-menu">
            <div class="dropdown-header">
              <div class="dropdown-avatar">{{ iniciales }}</div>
              <div class="dropdown-user-text">
                <strong>{{ user.nombre }}</strong>
                <small>CÉD. {{ user.cedula }}</small>
                <span class="user-role-tag">LABORATORIO CLÍNICO</span>
              </div>
            </div>

            <div class="dropdown-divider"></div>

            <div class="dropdown-context-info">
              <span class="ctx-item">
                <svg viewBox="0 0 24 24" class="ctx-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                {{ user.area }}
              </span>
              <span class="ctx-item">
                <svg viewBox="0 0 24 24" class="ctx-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                Sesión segura SSL
              </span>
            </div>

            <div class="dropdown-divider"></div>

            <button type="button" class="dropdown-item btn-logout" @click="handleCerrarSesion">
              <svg viewBox="0 0 24 24" class="dropdown-icon" fill="currentColor">
                <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
              </svg>
              <span>Cerrar sesión</span>
            </button>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<style scoped>
.lab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 1.25rem;
  position: sticky;
  top: 0;
  z-index: 40;
  user-select: none;
}

.lab-header-left {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.lab-logo-box {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #ffffff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.lab-icon-svg {
  width: 22px;
  height: 22px;
}

.lab-brand-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title-row {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.brand-title {
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #0f172a;
}

.brand-sub {
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: #64748b;
}

.brand-dept-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: #475569;
}

.dept-name {
  font-weight: 600;
  color: #0369a1;
}

.sep {
  color: #cbd5e1;
}

.dept-time {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.mini-icon {
  width: 12px;
  height: 12px;
}

.lab-header-center {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.chip-counter {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
  font-size: 0.75rem;
  color: #334155;
}

.chip-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.chip-num {
  font-weight: 700;
  color: #0f172a;
}

.alert-triage-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #b91c1c;
  color: #ffffff;
  padding: 0.35rem 0.85rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  box-shadow: 0 2px 8px rgba(185, 28, 28, 0.3);
}

.alert-svg {
  width: 15px;
  height: 15px;
}

.pulse {
  animation: alertPulse 1.8s infinite;
}

@keyframes alertPulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.15); opacity: 0.85; }
  100% { transform: scale(1); opacity: 1; }
}

.lab-header-right {
  display: flex;
  align-items: center;
}

.user-menu-container {
  position: relative;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #BAE6FD;
  color: #0369A1;
  font-weight: 700;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  display: block;
  font-size: 0.85rem;
  color: #1E293B;
}

.user-role {
  display: block;
  font-size: 0.7rem;
  color: #64748B;
}

.btn-more-options {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  font-size: 1.1rem;
  line-height: 1;
  color: #64748B;
  cursor: pointer;
  padding: 0.2rem 0.45rem;
  border-radius: 6px;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-more-options:hover,
.btn-more-options.active {
  color: #0F4C81;
  background-color: #F1F5F9;
  border-color: #CBD5E1;
}

/* MENÚ FLOTANTE DE USUARIO Y CIERRE DE SESIÓN */
.user-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 250px;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.15), 0 8px 10px -6px rgba(15, 23, 42, 0.1);
  padding: 0.75rem;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.25rem 0.25rem 0.5rem 0.25rem;
}

.dropdown-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0F4C81, #2563EB);
  color: #FFFFFF;
  font-weight: 800;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dropdown-user-text strong {
  display: block;
  font-size: 0.82rem;
  color: #1E293B;
  line-height: 1.2;
}

.dropdown-user-text small {
  display: block;
  font-size: 0.68rem;
  color: #64748B;
  font-family: monospace;
}

.user-role-tag {
  display: inline-block;
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  margin-top: 0.25rem;
}

.dropdown-divider {
  height: 1px;
  background-color: #F1F5F9;
  margin: 0 -0.25rem;
}

.dropdown-context-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.68rem;
  color: #64748B;
  padding: 0.2rem 0.3rem;
}

.ctx-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.ctx-svg {
  width: 13px;
  height: 13px;
  color: #0284c7;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  padding: 0.55rem 0.75rem;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.btn-logout {
  color: #DC2626;
  background-color: #FEF2F2;
}

.btn-logout:hover {
  background-color: #FEE2E2;
  color: #B91C1C;
}

.dropdown-icon {
  width: 18px;
  height: 18px;
}

/* Transición del menú */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
