<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'

const props = defineProps<{
  enfermera: {
    nombre: string
    cedula: string
    servicio: string
  }
  currentDateTimeStr: string
}>()

const router = useRouter()
const menuAbierto = ref(false)

const iniciales = computed(() => {
  const partes = (props.enfermera.nombre || '').replace(/^(Lic\.|Enf\.|Dr\.|Dra\.)\s*/i, '').trim().split(' ')
  if (partes.length >= 2 && partes[0] && partes[1]) {
    return `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
  }
  return (partes[0]?.slice(0, 2) || 'EN').toUpperCase()
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
  <header class="nurse-header">
    <div class="nurse-header-left">
      <div class="nurse-logo-square">
        <svg viewBox="0 0 24 24" class="header-icon" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
        </svg>
      </div>
      <div class="nurse-brand">
        <h2>Hospital Vitae</h2>
        <span>SISTEMA DE INFORMACIÓN HOSPITALARIA</span>
      </div>
      <div class="header-divider"></div>
      <div class="header-context">
        <strong>{{ enfermera.servicio }}</strong>
        <span class="live-clock">{{ currentDateTimeStr }}</span>
      </div>
    </div>

    <div class="nurse-header-right">
      <div class="nurse-badge user-menu-container">
        <div class="avatar-circle">{{ iniciales }}</div>
        <div class="nurse-info">
          <strong class="nurse-name">{{ enfermera.nombre }}</strong>
          <span class="nurse-role">ENFERMERÍA · {{ enfermera.cedula }}</span>
        </div>
        <button 
          class="btn-more-options" 
          :class="{ active: menuAbierto }"
          @click.stop="toggleMenu" 
          title="Opciones de cuenta y sesión"
        >
          ⋮
        </button>

        <!-- Menú flotante de perfil y cierre de sesión -->
        <transition name="fade-slide">
          <div v-if="menuAbierto" class="user-dropdown-menu">
            <div class="dropdown-header">
              <div class="dropdown-avatar">{{ iniciales }}</div>
              <div class="dropdown-user-text">
                <strong>{{ enfermera.nombre }}</strong>
                <small>{{ enfermera.cedula }}</small>
                <span class="user-role-tag">ENFERMERÍA Y TRIAGE</span>
              </div>
            </div>

            <div class="dropdown-divider"></div>

            <div class="dropdown-context-info">
              <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg> {{ enfermera.servicio }}</span>
              <span><svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg> Sesión segura SSL</span>
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
.nurse-header {
  height: 60px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(1rem, 2.5vw, 2rem);
  position: sticky;
  top: 0;
  z-index: 50;
}

.nurse-header-left {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.nurse-logo-square {
  width: 32px;
  height: 32px;
  background-color: #0F4C81;
  color: #FFFFFF;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon {
  width: 18px;
  height: 18px;
}

.nurse-brand h2 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
  line-height: 1.1;
}

.nurse-brand span {
  font-size: 0.65rem;
  color: #64748B;
  letter-spacing: 0.05em;
  display: block;
}

.header-divider {
  width: 1px;
  height: 26px;
  background-color: #E2E8F0;
  margin: 0 0.5rem;
}

.header-context strong {
  display: block;
  font-size: 0.82rem;
  color: #1E293B;
}

.live-clock {
  font-size: 0.72rem;
  color: #2563EB;
  font-weight: 600;
  font-family: monospace;
}

.nurse-badge {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #E0F2FE;
  color: #0284C7;
  font-weight: 700;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nurse-name {
  display: block;
  font-size: 0.85rem;
  color: #1E293B;
}

.nurse-role {
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
.user-menu-container {
  position: relative;
}

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
  background: linear-gradient(135deg, #0284C7, #0369A1);
  color: #FFFFFF;
  font-weight: 800;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
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
  background-color: #E0F2FE;
  color: #0369A1;
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
  transform: translateY(-6px);
}
</style>
