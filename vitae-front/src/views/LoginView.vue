<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { isAxiosError } from 'axios'
import authService from '@/services/authService'

const router = useRouter()

// --- ESTADOS REACTIVOS DEL FORMULARIO ---
const cedula = ref('MED-001')
const password = ref('')
const showPassword = ref(false)
const turno = ref<'Matutino' | 'Vespertino' | 'Nocturno'>('Vespertino')
const firmaHabilitada = ref(true)

// --- ESTADOS DE CARGA Y FEEDBACK DE API ---
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// --- RELOJ SINCRONIZADO EN TIEMPO REAL ---
const currentTime = ref('00:00:00 CST')
let timerId: number | undefined

const updateClock = () => {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds} CST`
}

onMounted(() => {
  updateClock()
  timerId = window.setInterval(updateClock, 1000)
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
})

// --- ACCIONES DE AUTENTICACIÓN CON API ---
const handleLogin = async () => {
  if (!cedula.value || !password.value) {
    errorMessage.value = 'Por favor ingrese su Cédula / Matrícula y su contraseña.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const authData = await authService.login(cedula.value, password.value)
    const usuario = authData.usuario
    successMessage.value = `¡Bienvenido(a), ${usuario.nombre_completo}!`

    // Redirección inteligente según el rol asignado en base de datos
    const rol = (usuario.rol || '').toUpperCase()
    setTimeout(() => {
      if (rol === 'MEDICO') {
        router.push('/medico')
      } else if (rol === 'ENFERMERIA') {
        router.push('/enfermeria')
      } else if (rol === 'ADMINISTRADOR' || rol === 'ADMIN') {
        router.push('/administrador')
      } else if (rol === 'RECEPCION' || rol === 'ADMISION') {
        router.push('/recepcion')
      } else if (rol === 'LABORATORIO' || rol === 'LABORATORISTA') {
        router.push('/laboratorio')
      } else {
        router.push('/medico')
      }
    }, 500)
  } catch (err: unknown) {
    if (isAxiosError(err) && err.response?.data) {
      const data = err.response.data as { message?: string }
      errorMessage.value = data.message || 'Error en las credenciales proporcionadas.'
    } else if (err instanceof Error) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = 'No se pudo conectar con el servidor (127.0.0.1:8000). Verifique que la API de Laravel esté activa.'
    }
  } finally {
    isLoading.value = false
  }
}

const handleTriageEmergencia = () => {
  router.push('/enfermeria')
}
</script>

<template>
  <div class="login-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <header class="app-header">
      <div class="header-left">
        <div class="logo-box">
          <svg viewBox="0 0 24 24" class="logo-icon" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
          </svg>
        </div>
        <div class="brand-text">
          <h1>HOSPITAL VITAE</h1>
          <span>SISTEMA DE INFORMACIÓN HOSPITALARIA</span>
        </div>
      </div>

      <div class="header-right">
        <div class="status-badge ssl">
          <span class="status-dot green"></span>
          <span>Red Segura SSL</span>
        </div>
        <button class="user-avatar-btn" title="Estado de Terminal">
          <svg viewBox="0 0 24 24" class="avatar-icon" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- 2. BARRA DE ESTADO TÉCNICO Y CLÍNICO -->
    <div class="sub-header-bar">
      <div class="sub-left">
        <span class="status-dot blue"></span>
        <span class="strong-label">NODO ACTIVO:</span>
        <span class="node-info">HQ-CENTRAL-01 [Sede Principal]</span>
        <span class="separator">•</span>
        <span class="fhir-info">HL7 FHIR v4.0.1</span>
      </div>
      <div class="sub-right">
        <span class="tls-info">
          <svg viewBox="0 0 24 24" class="inline-icon" fill="currentColor">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>
          </svg>
          TLS 1.3 Seguro
        </span>
        <span class="clock-info">
          <svg viewBox="0 0 24 24" class="inline-icon" fill="currentColor">
            <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-4.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/>
          </svg>
          {{ currentTime }}
        </span>
      </div>
    </div>

    <!-- 3. CONTENEDOR PRINCIPAL: TARJETA DE LOGIN -->
    <main class="main-content">
      <div class="login-card">
        <!-- Columna Izquierda: Imagen y Marca -->
        <div class="brand-side">
          <div class="brand-overlay"></div>
          <div class="brand-content">
            <div class="brand-logo-frame">
              <svg viewBox="0 0 24 24" class="brand-plus-icon" fill="currentColor">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
            </div>
            <h2 class="brand-title">Hospital Vitae</h2>
            <div class="brand-indicator"></div>
          </div>
        </div>

        <!-- Columna Derecha: Formulario Clínico -->
        <div class="form-side">
          <form @submit.prevent="handleLogin" class="clinical-form">
            <!-- Alerta Reactiva de Autenticación -->
            <div v-if="errorMessage" class="auth-feedback-alert error">
              <span class="alert-icon">
                <svg viewBox="0 0 24 24" class="svg-feedback-icon" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </span>
              <div class="alert-text">
                <strong>Error de autenticación</strong>
                <span>{{ errorMessage }}</span>
              </div>
            </div>

            <div v-if="successMessage" class="auth-feedback-alert success">
              <span class="alert-icon">
                <svg viewBox="0 0 24 24" class="svg-feedback-icon" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </span>
              <div class="alert-text">
                <strong>Acceso Autorizado</strong>
                <span>{{ successMessage }} Redirigiendo a su espacio clínico...</span>
              </div>
            </div>

            <!-- Campo: Cédula / Matrícula -->
            <div class="form-group">
              <div class="field-header">
                <label for="cedula">Cédula Profesional o Matrícula Institucional</label>
                <button type="button" class="btn-link">
                  <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
                    <path d="M20 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4V6h16v12zm-8-2h6v-2h-6v2zm0-4h6v-2h-6v2zm-2-4H6v8h4V6z"/>
                  </svg>
                  Leer Credencial RFID
                </button>
              </div>
              <div class="input-container">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                  </svg>
                </span>
                <input 
                  id="cedula" 
                  v-model="cedula" 
                  type="text" 
                  placeholder="Ej. MED-849201" 
                  required
                />
              </div>
            </div>

            <!-- Campo: Contraseña -->
            <div class="form-group">
              <div class="field-header">
                <label for="password">Contraseña de Seguridad Clínica</label>
                <button type="button" class="btn-link" @click="showPassword = !showPassword">
                  <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
                    <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                  </svg>
                  {{ showPassword ? 'Ocultar' : 'Mostrar' }}
                </button>
              </div>
              <div class="input-container password-box">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/>
                  </svg>
                </span>
                <input 
                  id="password" 
                  v-model="password" 
                  :type="showPassword ? 'text' : 'password'" 
                  placeholder="••••••••••••" 
                  required
                />
                <span class="verified-icon" title="Validación de fortaleza">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                  </svg>
                </span>
              </div>
            </div>

            <!-- Selector de Turno -->
            <div class="turno-selector">
              <span class="turno-label">
                <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
                  <path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zm0 2c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1z"/>
                </svg>
                TURNO ACTIVO:
              </span>
              <div class="turno-buttons">
                <button 
                  type="button" 
                  class="btn-turno" 
                  :class="{ active: turno === 'Matutino' }"
                  @click="turno = 'Matutino'"
                >
                  Matutino
                </button>
                <button 
                  type="button" 
                  class="btn-turno" 
                  :class="{ active: turno === 'Vespertino' }"
                  @click="turno = 'Vespertino'"
                >
                  Vespertino
                </button>
                <button 
                  type="button" 
                  class="btn-turno" 
                  :class="{ active: turno === 'Nocturno' }"
                  @click="turno = 'Nocturno'"
                >
                  Nocturno
                </button>
              </div>
            </div>

            <!-- Checkbox de Firma Electrónica Avanzada -->
            <label class="checkbox-container">
              <input type="checkbox" v-model="firmaHabilitada" />
              <span class="custom-checkbox"></span>
              <div class="checkbox-text">
                <strong>Firma Electrónica Avanzada (FIEL / e.firma) habilitada</strong>
                <small>Válida prescripción de medicamentos controlados en esta terminal</small>
              </div>
            </label>

            <!-- Botón Principal -->
            <button type="submit" class="btn-submit" :disabled="isLoading" :class="{ loading: isLoading }">
              <span v-if="isLoading" class="btn-spinner"></span>
              <span v-if="isLoading">Validando con Servidor Central...</span>
              <span v-else>Iniciar Turno y Acceder al Sistema</span>
              <svg v-if="!isLoading" viewBox="0 0 24 24" class="btn-icon" fill="currentColor">
                <path d="M14 5l7 7m0 0l-7 7m7-7H3"/>
              </svg>
            </button>
          </form>

          <!-- Pie del Formulario: Mesa de Ayuda y Normativa -->
          <div class="form-footer-info">
            <span class="help-link">
              <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 16h-2v-2h2v2zm1.07-7.75l-.9.92C12.45 11.9 12 12.5 12 14h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.75z"/>
              </svg>
              Token / Cédula
            </span>
            <span class="separator">•</span>
            <span class="support-info">
              <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
                <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57-.35-.11-.74-.03-1.02.24l-2.2 2.2c-2.83-1.44-5.15-3.75-6.59-6.59l2.2-2.21c.28-.26.36-.65.25-1C8.7 6.45 8.5 5.25 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1zM19 12h2c0-4.97-4.03-9-9-9v2c3.87 0 7 3.13 7 7zm-4 0h2c0-2.76-2.24-5-5-5v2c1.66 0 3 1.34 3 3z"/>
              </svg>
              Mesa de Soporte TI (Ext. 4000)
            </span>
            <span class="norm-badge">
              <span class="status-dot blue"></span>
              NOM-024-SSA3
            </span>
          </div>
        </div>
      </div>

      <!-- 4. BARRA DE EMERGENCIA CLÍNICA (CÓDIGO ROJO) -->
      <aside class="emergency-bar">
        <div class="emergency-text">
          <div class="emergency-icon-wrapper">
            <svg viewBox="0 0 24 24" class="emergency-star-icon" fill="currentColor">
              <path d="M12 2L14.5 8.5H21.5L16 12.8L18.2 19.5L12 15.2L5.8 19.5L8 12.8L2.5 8.5H9.5L12 2Z"/>
            </svg>
          </div>
          <div class="emergency-desc">
            <strong>¿Paciente en Código Rojo / Paro Cardiorrespiratorio sin ficha?</strong>
            <span>Utilice el protocolo de admisión expedita sin credenciales nominales.</span>
          </div>
        </div>
        <button type="button" class="btn-emergency" @click="handleTriageEmergencia">
          Triage Vital Inmediato
        </button>
      </aside>

      <!-- Accesos Rápidos a Módulos Hospitalarios -->
      <div class="workspace-quick-bar">
        <span>Acceso directo a módulos:</span>
        <button type="button" class="btn-quick-module" @click="router.push('/medico')">
          <svg viewBox="0 0 24 24" class="quick-btn-svg" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/>
          </svg>
          <span>Espacio Médico</span>
        </button>
        <button type="button" class="btn-quick-module" @click="router.push('/enfermeria')">
          <svg viewBox="0 0 24 24" class="quick-btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
          <span>Enfermería y Triage</span>
        </button>
        <button type="button" class="btn-quick-module" @click="router.push('/recepcion')">
          <svg viewBox="0 0 24 24" class="quick-btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
            <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
          </svg>
          <span>Recepción y Admisión</span>
        </button>
        <button type="button" class="btn-quick-module" @click="router.push('/laboratorio')">
          <svg viewBox="0 0 24 24" class="quick-btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
            <line x1="8.5" y1="2" x2="15.5" y2="2"/>
          </svg>
          <span>Laboratorio LIS</span>
        </button>
        <button type="button" class="btn-quick-module" @click="router.push('/administrador')">
          <svg viewBox="0 0 24 24" class="quick-btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
            <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
          </svg>
          <span>Administración TI</span>
        </button>
      </div>
    </main>

    <!-- 5. PIE DE PÁGINA INSTITUCIONAL -->
    <footer class="app-footer">
      <div class="footer-left">
        <svg viewBox="0 0 24 24" class="mini-icon" fill="currentColor">
          <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>
        </svg>
        Terminal Certificada HIPAA / HL7 FHIR Compliant
      </div>
      <div class="footer-right">
        © 2026 Hospital Vitae Clínicas de Alta Complejidad. Todos los derechos reservados.
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* CONTENEDOR GENERAL ADAPTABLE A CUALQUIER RESOLUCIÓN */
.login-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #F8FAFC;
  color: #1E293B;
  box-sizing: border-box;
}

/* 1. ENCABEZADO SUPERIOR */
.app-header {
  height: 60px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(1rem, 3vw, 2.5rem);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-box {
  width: 32px;
  height: 32px;
  background-color: #0F4C81;
  color: #FFFFFF;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  width: 20px;
  height: 20px;
}

.brand-text h1 {
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #0F4C81;
  margin: 0;
  line-height: 1.1;
}

.brand-text span {
  font-size: 0.68rem;
  color: #64748B;
  letter-spacing: 0.08em;
  display: block;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  padding: 0.25rem 0.6rem;
  background-color: #EFF6FF;
  border-radius: 9999px;
  color: #1E3A8A;
  border: 1px solid #DBEAFE;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.green {
  background-color: #10B981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
}

.status-dot.blue {
  background-color: #3B82F6;
}

.user-avatar-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background-color: #0F4C81;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.avatar-icon {
  width: 18px;
  height: 18px;
}

/* 2. SUB-HEADER BAR */
.sub-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem clamp(1rem, 3vw, 2.5rem);
  font-size: 0.75rem;
  color: #64748B;
  border-bottom: 1px solid #EDF2F7;
  background-color: #FFFFFF;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.sub-left, .sub-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.strong-label {
  font-weight: 700;
  color: #0F4C81;
}

.separator {
  color: #CBD5E1;
}

.inline-icon {
  width: 14px;
  height: 14px;
  vertical-align: middle;
  margin-right: 0.2rem;
}

/* 3. MAIN WORKSPACE CENTRADO */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 2.5vw, 2rem);
  gap: 1.25rem;
}

/* TARJETA DE LOGIN PRINCIPAL (FLUIDA) */
.login-card {
  width: 100%;
  max-width: 960px;
  background-color: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.03);
  display: flex;
  overflow: hidden;
  border: 1px solid #E2E8F0;
  min-height: 480px;
}

/* COLUMNA IZQUIERDA: IMAGEN CON OVERLAY SEMITRANSPARENTE */
.brand-side {
  width: 44%;
  position: relative;
  background-image: url('@/assets/hospital-fachada.jpg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  overflow: hidden;
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(12, 33, 62, 0.86) 0%, rgba(8, 22, 43, 0.92) 100%);
  backdrop-filter: blur(1.5px);
}

.brand-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem;
}

.brand-logo-frame {
  width: 52px;
  height: 52px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  background: rgba(255, 255, 255, 0.08);
}

.brand-plus-icon {
  width: 32px;
  height: 32px;
  color: #38BDF8;
}

.brand-title {
  font-size: clamp(1.4rem, 2.2vw, 1.8rem);
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 0.6rem;
  letter-spacing: -0.02em;
}

.brand-indicator {
  width: 38px;
  height: 4px;
  background-color: #38BDF8;
  border-radius: 2px;
}

/* COLUMNA DERECHA: FORMULARIO */
.form-side {
  width: 56%;
  padding: clamp(1.5rem, 3.5vw, 2.5rem);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.clinical-form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-header label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #1E293B;
}

.btn-link {
  background: none;
  border: none;
  color: #2563EB;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover {
  text-decoration: underline;
}

.mini-icon {
  width: 14px;
  height: 14px;
}

/* INPUT CONTAINERS */
.input-container {
  display: flex;
  align-items: center;
  border: 1.5px solid #CBD5E1;
  border-radius: 8px;
  background-color: #FFFFFF;
  transition: all 0.2s ease;
  padding: 0 0.75rem;
}

.input-container:focus-within {
  border-color: #3B82F6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.input-icon {
  width: 20px;
  height: 20px;
  color: #94A3B8;
  display: flex;
  align-items: center;
  margin-right: 0.5rem;
}

.input-icon svg {
  width: 100%;
  height: 100%;
}

.input-container input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 0.92rem;
  padding: 0.75rem 0;
  color: #1E293B;
  background: transparent;
}

.verified-icon {
  width: 18px;
  height: 18px;
  color: #10B981;
}

/* TURNO SELECTOR */
.turno-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #F1F5F9;
  padding: 0.35rem 0.6rem;
  border-radius: 8px;
  margin-top: 0.2rem;
}

.turno-label {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #0F4C81;
}

.turno-buttons {
  display: flex;
  gap: 0.3rem;
}

.btn-turno {
  padding: 0.3rem 0.75rem;
  font-size: 0.75rem;
  border: none;
  background: transparent;
  color: #64748B;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-turno.active {
  background-color: #2563EB;
  color: #FFFFFF;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
}

/* CHECKBOX PERSONALIZADO */
.checkbox-container {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  cursor: pointer;
  margin-top: 0.2rem;
}

.checkbox-container input {
  display: none;
}

.custom-checkbox {
  width: 18px;
  height: 18px;
  border: 1.5px solid #CBD5E1;
  border-radius: 4px;
  background-color: #FFFFFF;
  display: inline-block;
  margin-top: 2px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.checkbox-container input:checked + .custom-checkbox {
  background-color: #2563EB;
  border-color: #2563EB;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M12.207 4.793a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0l-2-2a1 1 0 011.414-1.414L6.5 9.086l4.293-4.293a1 1 0 011.414 0z'/%3E%3C/svg%3E");
  background-size: cover;
}

.checkbox-text strong {
  display: block;
  font-size: 0.8rem;
  color: #1E293B;
}

.checkbox-text small {
  display: block;
  font-size: 0.7rem;
  color: #64748B;
}

/* ALERTAS DE FEEDBACK DE AUTENTICACION */
.auth-feedback-alert {
  padding: 0.75rem 0.9rem;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  font-size: 0.78rem;
  margin-bottom: 0.5rem;
}

.auth-feedback-alert.error {
  background-color: #FEF2F2;
  border: 1px solid #FCA5A5;
  color: #991B1B;
}

.auth-feedback-alert.success {
  background-color: #ECFDF5;
  border: 1px solid #A7F3D0;
  color: #065F46;
}

.alert-icon {
  font-size: 1.05rem;
  line-height: 1;
}

.alert-text strong {
  display: block;
  font-weight: 700;
  margin-bottom: 0.15rem;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none !important;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* BOTON DE ACCESO */
.btn-submit {
  width: 100%;
  padding: 0.85rem;
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.25);
  margin-top: 0.5rem;
}

.btn-submit:hover {
  background-color: #1D4ED8;
}

.btn-submit:active {
  transform: translateY(1px);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

/* PIE DE FORMULARIO */
.form-footer-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
  color: #64748B;
  border-top: 1px solid #F1F5F9;
  padding-top: 1rem;
  margin-top: 1.25rem;
}

.help-link, .support-info {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.norm-badge {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-weight: 600;
  color: #0F4C81;
}

/* 4. BARRA DE EMERGENCIA (CÓDIGO ROJO) */
.emergency-bar {
  width: 100%;
  max-width: 960px;
  background-color: #FFFFFF;
  border-radius: 10px;
  border: 1px solid #FEE2E2;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
}

.emergency-text {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.emergency-icon-wrapper {
  color: #DC2626;
  display: flex;
  align-items: center;
}

.emergency-star-icon {
  width: 22px;
  height: 22px;
}

.emergency-desc strong {
  display: block;
  font-size: 0.82rem;
  color: #1E293B;
}

.emergency-desc span {
  font-size: 0.72rem;
  color: #64748B;
}

.btn-emergency {
  background-color: #FEE2E2;
  color: #DC2626;
  border: 1px solid #FECACA;
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-emergency:hover {
  background-color: #FCA5A5;
  color: #991B1B;
}

/* 5. PIE DE PÁGINA */
.app-footer {
  padding: 0.8rem clamp(1rem, 3vw, 2.5rem);
  font-size: 0.72rem;
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #E2E8F0;
  background-color: #FFFFFF;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.workspace-quick-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  padding: 0.75rem 1rem;
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 0.75rem;
  color: #64748B;
  max-width: 950px;
  margin: 0 auto;
}

.svg-feedback-icon {
  width: 18px;
  height: 18px;
}

.workspace-quick-bar span {
  font-weight: 600;
  color: #475569;
}

.btn-quick-module {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.4rem 0.75rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #0F4C81;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: all 0.15s;
}

.quick-btn-svg {
  width: 14px;
  height: 14px;
}

.btn-quick-module:hover {
  background-color: #0F4C81;
  color: #FFFFFF;
  border-color: #0F4C81;
}

/* MEDIA QUERIES PARA ADAPTABILIDAD FLUIDA */
@media (max-width: 768px) {
  .login-card {
    flex-direction: column;
  }
  .brand-side {
    width: 100%;
    height: 180px;
  }
  .form-side {
    width: 100%;
  }
  .sub-header-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  .emergency-bar {
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
  }
  .app-footer {
    flex-direction: column;
    gap: 0.4rem;
    text-align: center;
  }
}
</style>
