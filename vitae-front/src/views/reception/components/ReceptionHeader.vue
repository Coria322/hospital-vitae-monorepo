<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'

const props = defineProps<{
  recepcionista: {
    nombre: string
    cedula: string
    cargo: string
    sede: string
  }
  currentDateTimeStr: string
  horaActual?: string
}>()

const router = useRouter()
const menuAbierto = ref(false)

const iniciales = computed(() => {
  const partes = (props.recepcionista.nombre || '').replace(/^(Lic\.|Ing\.|Dr\.|Dra\.)\s*/i, '').trim().split(' ')
  if (partes.length >= 2 && partes[0] && partes[1]) {
    return `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
  }
  return (partes[0]?.slice(0, 2) || 'AD').toUpperCase()
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
  <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE (HOSPITAL VITAE) -->
  <header class="reception-header">
    <div class="header-left">
      <div class="logo-square">
        <svg viewBox="0 0 24 24" class="header-icon" fill="currentColor">
          <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
        </svg>
      </div>
      <div class="brand-text">
        <h2>Hospital Vitae</h2>
        <span>SISTEMA DE INFORMACIÓN HOSPITALARIA</span>
      </div>

      <div class="service-pill">
        <span class="service-icon">📋</span>
        <strong>Módulo de Admisión y Registro</strong>
      </div>

      <div class="clock-badge">
        <span class="clock-icon">🕒</span>
        <span>{{ currentDateTimeStr || 'Cargando reloj...' }}</span>
      </div>

      <div class="status-connection-badge">
        <span class="pulse-dot"></span>
        <span>Conexión RENAPO / CURP Activa</span>
      </div>
    </div>

    <div class="header-right">
      <!-- Indicador rápido de capacidad hospitalaria -->
      <div class="kpi-tag">
        <span class="kpi-icon">🛏️</span>
        <div>
          <small>CAMAS</small>
          <strong>24 Disp. <span class="subtle">/ 82 Ocup.</span></strong>
        </div>
      </div>

      <!-- Perfil del Personal en Sesión -->
      <div class="user-profile-badge user-menu-container">
        <div class="avatar-circle">{{ iniciales }}</div>
        <div class="user-info">
          <strong>{{ recepcionista.nombre }}</strong>
          <small>{{ recepcionista.cargo }} · {{ recepcionista.cedula }}</small>
        </div>
        <button 
          class="btn-profile-menu" 
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
                <strong>{{ recepcionista.nombre }}</strong>
                <small>{{ recepcionista.cedula }}</small>
                <span class="user-role-tag">RECEPCIÓN Y ADMISIÓN</span>
              </div>
            </div>

            <div class="dropdown-divider"></div>

            <div class="dropdown-context-info">
              <span>📍 {{ recepcionista.sede }}</span>
              <span>🔒 Sesión segura SSL</span>
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
/* 1. HEADER SUPERIOR PERSISTENTE */
.reception-header {
  height: 60px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(1rem, 2vw, 2rem);
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.logo-square {
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

.brand-text h2 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #0F4C81;
  margin: 0;
  line-height: 1.1;
}

.brand-text span {
  font-size: 0.65rem;
  color: #64748B;
  letter-spacing: 0.05em;
  display: block;
}

.service-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #F1F5F9;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.75rem;
  color: #1E293B;
}

.clock-badge {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  color: #475569;
  background-color: #F8FAFC;
  padding: 0.35rem 0.6rem;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.status-connection-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: #0284C7;
  font-weight: 600;
  background-color: #F0F9FF;
  border: 1px solid #BAE6FD;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  background-color: #0284C7;
  border-radius: 50%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.kpi-tag {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.3rem 0.6rem;
}

.kpi-tag small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
  font-weight: 700;
}

.kpi-tag strong {
  font-size: 0.75rem;
  color: #0F4C81;
}

.kpi-tag .subtle {
  font-weight: 500;
  color: #64748B;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding-left: 0.6rem;
  border-left: 1px solid #E2E8F0;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  background-color: #1E293B;
  color: #FFFFFF;
  font-weight: 700;
  font-size: 0.8rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-info strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.user-info small {
  font-size: 0.65rem;
  color: #64748B;
}

.btn-profile-menu {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: #64748B;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  transition: all 0.15s;
}

.btn-profile-menu:hover,
.btn-profile-menu.active {
  color: #0F4C81;
  background-color: #F1F5F9;
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
  background: linear-gradient(135deg, #1E293B, #334155);
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
  background-color: #F1F5F9;
  color: #334155;
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
