<script setup lang="ts">
import { ref } from 'vue'
import type { CuentaBloqueada } from '../types'

const props = defineProps<{
  cuentasBloqueadas: CuentaBloqueada[]
}>()

const emit = defineEmits<{
  (e: 'desbloquear', payload: { id: string; motivo: string; medida: string }): void
  (e: 'ir-bitacora'): void
}>()

const cuentaSeleccionada = ref<CuentaBloqueada>(props.cuentasBloqueadas[0] || {
  id: 'bloq-1',
  id_usuario: 'MED-8831',
  nombre: 'Dra. Sofía Delgado',
  iniciales: 'SD',
  rol: 'Médico Urgenciólogo',
  servicio: 'Urgencias Box 02',
  correo: 'urgencias.box02@hospitalvitae.mx',
  terminal: 'Terminal Urgencias Box 02',
  ip: '192.168.10.45',
  hora_bloqueo: '10:15 h',
  tiempo_transcurrido: 'hace 24 min',
  intentos_fallidos: 3,
  motivo_bloqueo: '#ERR-PW-CONSEC',
  pacientes_en_espera: 4,
  impacto_clinico: 'ALTO'
})

const motivoDesbloqueo = ref('Olvido de clave en cambio de turno urgente, validado presencialmente en Urgencias Box 02 con Jefe de Servicio.')
const medidaSeguridad = ref<'CAMBIO_CLAVE' | 'TOKEN_SMS'>('CAMBIO_CLAVE')
const confirmacionIdentidad = ref(true)
const feedbackAction = ref('')

const handleConfirmarDesbloqueo = () => {
  if (!motivoDesbloqueo.value.trim()) {
    alert('Debe especificar el motivo institucional obligatorio del desbloqueo.')
    return
  }
  if (!confirmacionIdentidad.value) {
    alert('Debe confirmar formalmente la validación de identidad del colaborador.')
    return
  }

  emit('desbloquear', {
    id: cuentaSeleccionada.value.id,
    motivo: motivoDesbloqueo.value,
    medida: medidaSeguridad.value
  })

  feedbackAction.value = `¡Cuenta de ${cuentaSeleccionada.value.nombre} reactivada con éxito! Credenciales restauradas y registradas en bitácora.`
  setTimeout(() => {
    feedbackAction.value = ''
  }, 4000)
}

const handleSeleccionarCuenta = (cuenta: CuentaBloqueada) => {
  cuentaSeleccionada.value = cuenta
}
</script>

<template>
  <div class="admin-bloqueos-section">
    <!-- CABECERA INSTITUCIONAL -->
    <div class="bloqueos-topbar">
      <div class="topbar-left">
        <div class="tags-row">
          <span class="badge-directiva">DIRECTIVA NOM-024-SSA3 / ISO 27001</span>
          <span class="sep">•</span>
          <span class="rule-chip">Regla: Bloqueo estricto tras 3 intentos fallidos consecutivos</span>
        </div>
        <h2 class="title">Centro de Reactivación y Desbloqueo de Cuentas</h2>
      </div>
    </div>

    <!-- BANNER DE ALERTA ROJA PRIORITARIA -->
    <div class="alert-banner-bloqueos">
      <div class="banner-left">
        <div class="shield-danger-box">
          <svg viewBox="0 0 24 24" class="shield-danger-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <div class="banner-meta">
          <div class="banner-title-row">
            <h3>{{ cuentasBloqueadas.length }} Cuentas con Bloqueo Activo</h3>
            <span class="window-pill">Ventana 2h</span>
          </div>
          <p class="banner-text">
            Interrupción preventiva de credenciales para resguardo de expedientes clínicos. Requiere validación obligatoria de identidad física o canal criptográfico antes de restituir facultades operativas.
          </p>
        </div>
      </div>

      <button type="button" class="btn-attend-pending">
        <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 11 12 14 22 4"/>
          <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
        </svg>
        <span>Atender Pendientes</span>
      </button>
    </div>

    <!-- NOTIFICACIÓN DE ACCIÓN -->
    <div v-if="feedbackAction" class="feedback-box">
      <svg viewBox="0 0 24 24" class="fb-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>{{ feedbackAction }}</span>
    </div>

    <!-- BÚSQUEDA RÁPIDA / GAFETE RFID -->
    <div class="rfid-search-bar">
      <div class="rfid-input-wrap">
        <svg viewBox="0 0 24 24" class="rfid-svg" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="16" rx="2"/>
          <circle cx="9" cy="10" r="2"/>
          <line x1="15" y1="8" x2="17" y2="8"/>
          <line x1="15" y1="12" x2="17" y2="12"/>
        </svg>
        <input
          type="text"
          placeholder="Escaneo de gafete RFID institucional o ingrese ID de usuario (ej. MED-8831)..."
          class="rfid-input"
        />
      </div>

      <button type="button" class="btn-find-account">
        <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <span>Localizar Cuenta Bloqueada [Enter]</span>
      </button>
    </div>

    <!-- DOS COLUMNAS PRINCIPALES -->
    <div class="bloqueos-grid">
      <!-- COLUMNA IZQUIERDA: LISTADO PRIORITARIO DE CONTENCIÓN -->
      <div class="containment-list-col">
        <div class="list-header-row">
          <div class="lh-left">
            <span class="lh-title">Listado Prioritario de Contención</span>
            <span class="badge-count">{{ cuentasBloqueadas.length }}</span>
          </div>
          <span class="live-pulse-text">
            <span class="dot-blue"></span>
            Actualización en tiempo real
          </span>
        </div>

        <div class="cards-list">
          <div
            v-for="cuenta in cuentasBloqueadas"
            :key="cuenta.id"
            class="locked-user-card"
            :class="{ selected: cuentaSeleccionada.id === cuenta.id }"
            @click="handleSeleccionarCuenta(cuenta)"
          >
            <!-- CABECERA DE USUARIO BLOQUEADO -->
            <div class="user-card-top">
              <div class="user-meta-top">
                <div class="avatar-box">{{ cuenta.iniciales }}</div>
                <div class="user-title-box">
                  <div class="name-status-row">
                    <strong class="user-name">{{ cuenta.nombre }}</strong>
                    <span class="badge-preventivo">Bloqueo Preventivo</span>
                    <span class="badge-dept">{{ cuenta.servicio }}</span>
                  </div>
                  <span class="user-email-id">{{ cuenta.correo }} • ID: {{ cuenta.id_usuario }}</span>
                  <span class="user-time-ip">
                    <svg viewBox="0 0 24 24" class="time-svg" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <polyline points="12 6 12 12 16 14"/>
                    </svg>
                    {{ cuenta.hora_bloqueo }} ({{ cuenta.tiempo_transcurrido }}) • IP: {{ cuenta.ip }} ({{ cuenta.terminal }})
                  </span>
                </div>
              </div>

              <div class="failed-badge">
                <span class="failed-num">{{ cuenta.intentos_fallidos }} INTENTOS FALLIDOS</span>
                <span class="failed-hash">HASH: {{ cuenta.motivo_bloqueo }}</span>
              </div>
            </div>

            <!-- CHIPS DE IMPACTO ASISTENCIAL -->
            <div class="clinical-impact-bar">
              <div class="impact-chip">
                <svg viewBox="0 0 24 24" class="impact-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 14 14"/>
                </svg>
                <span>Validación de Turno: <strong>Matutino Activo</strong></span>
              </div>

              <div class="impact-chip">
                <svg viewBox="0 0 24 24" class="impact-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                </svg>
                <span>Pacientes en espera en Box: <strong>{{ cuenta.pacientes_en_espera }}</strong></span>
              </div>

              <div class="impact-chip danger">
                <svg viewBox="0 0 24 24" class="impact-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
                </svg>
                <span>Impacto Operativo Alto: <strong>{{ cuenta.rol }}</strong></span>
              </div>
            </div>

            <!-- BOTONES DE ACCIÓN RÁPIDA -->
            <div class="card-action-btns">
              <button type="button" class="btn-sec-action">
                <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                  <line x1="12" y1="18" x2="12.01" y2="18"/>
                </svg>
                <span>Verificar por Código OTP</span>
              </button>

              <button type="button" class="btn-sec-action">
                <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <span>Forzar Cambio de Contraseña</span>
              </button>

              <button
                type="button"
                class="btn-unlock-fast"
                @click.stop="handleConfirmarDesbloqueo"
              >
                <svg viewBox="0 0 24 24" class="btn-svg-sm" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                </svg>
                <span>Desbloquear Inmediatamente</span>
              </button>
            </div>
          </div>
        </div>

        <!-- CUMPLIMIENTO DE SEGURIDAD NOM-024 FOOTER -->
        <div class="compliance-footer-card">
          <div class="cf-left">
            <svg viewBox="0 0 24 24" class="cf-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <div class="cf-meta">
              <strong>Cumplimiento de Seguridad NOM-024</strong>
              <span>Toda reactivación es asentada en el log inmutable con firma digital del Administrador.</span>
            </div>
          </div>

          <div class="cf-metrics">
            <div class="metric-item">
              <span class="m-val">0</span>
              <span class="m-lbl">ATAQUES BRUTE FORCE</span>
            </div>
            <div class="metric-item">
              <span class="m-val green">100%</span>
              <span class="m-lbl">TRAZABILIDAD</span>
            </div>
          </div>
        </div>
      </div>

      <!-- COLUMNA DERECHA: PROTOCOLO DE REACTIVACIÓN E HISTORIAL DEL TURNO -->
      <div class="protocol-sidebar-col">
        <!-- PROTOCOLO DE REACTIVACIÓN -->
        <div class="protocol-card">
          <div class="protocol-header">
            <div class="ph-left">
              <svg viewBox="0 0 24 24" class="ph-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
              <h4>Protocolo de Reactivación</h4>
            </div>
            <span class="prot-nom-pill">PROT-NOM024</span>
          </div>

          <div class="protocol-body">
            <!-- TARGET USER -->
            <div class="target-user-strip">
              <div class="target-avatar">{{ cuentaSeleccionada.iniciales }}</div>
              <div class="target-info">
                <strong>{{ cuentaSeleccionada.nombre }}</strong>
                <span>ID: {{ cuentaSeleccionada.id_usuario }} • {{ cuentaSeleccionada.servicio }}</span>
              </div>
              <span class="target-status">Activo</span>
            </div>

            <!-- MOTIVO INSTITUCIONAL -->
            <div class="fg">
              <div class="lbl-row">
                <label class="flbl">Motivo institucional del desbloqueo</label>
                <span class="req">*Obligatorio</span>
              </div>
              <textarea
                v-model="motivoDesbloqueo"
                rows="3"
                class="ftextarea"
                placeholder="Especifique la causa justificada (ej. Olvido de clave en cambio de turno, validado presencialmente)..."
              ></textarea>
            </div>

            <!-- MEDIDA DE SEGURIDAD POSTERIOR -->
            <div class="fg">
              <label class="flbl">Medida de Seguridad Posterior</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input
                    v-model="medidaSeguridad"
                    type="radio"
                    value="CAMBIO_CLAVE"
                  />
                  <span>Forzar cambio de clave</span>
                </label>
                <label class="radio-label">
                  <input
                    v-model="medidaSeguridad"
                    type="radio"
                    value="TOKEN_SMS"
                  />
                  <span>Token temporal SMS</span>
                </label>
              </div>
            </div>

            <!-- CHECKBOX DE CONFIRMACIÓN PRESENCIAL -->
            <label class="confirm-check-label">
              <input v-model="confirmacionIdentidad" type="checkbox" class="confirm-cb" />
              <span>
                Confirmo validación de identidad presencial o por canal seguro institucional bajo responsabilidad de administrador activo.
              </span>
            </label>

            <!-- BOTÓN PRINCIPAL DE REACTIVACIÓN -->
            <button
              type="button"
              class="btn-submit-reactivation"
              @click="handleConfirmarDesbloqueo"
            >
              <svg viewBox="0 0 24 24" class="btn-svg-lg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
              </svg>
              <span>Confirmar Reactivación Inmediata [Ctrl+Enter]</span>
            </button>
          </div>
        </div>

        <!-- HISTORIAL DE REACTIVACIONES DEL TURNO -->
        <div class="history-card">
          <div class="hc-header">
            <h4>Historial de Reactivaciones del Turno</h4>
            <span class="hc-sub">Hoy • Turno Matutino</span>
          </div>

          <div class="hc-body">
            <div class="history-item">
              <div class="hi-top">
                <strong>Dr. Carlos Morales</strong>
                <span class="hi-time">08:12 h</span>
              </div>
              <span class="hi-desc">ID: MED-5501 • Pediatría Box 01. Motivo: Desconexión de terminal en pase de visita.</span>
              <span class="hi-signer">
                <svg viewBox="0 0 24 24" class="pen-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 20h9"/>
                  <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                Firmado: Ing. Roberto Silva (ADM-0014)
              </span>
            </div>

            <div class="history-item">
              <div class="hi-top">
                <strong>Lic. Mariana Vaca</strong>
                <span class="hi-time">07:45 h</span>
              </div>
              <span class="hi-desc">ID: ADM-1120 • Farmacia Central. Motivo: Bloqueo por teclado numérico bloqueado.</span>
              <span class="hi-signer">
                <svg viewBox="0 0 24 24" class="pen-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 20h9"/>
                  <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                Firmado: Ing. Roberto Silva (ADM-0014)
              </span>
            </div>

            <div class="history-item">
              <div class="hi-top">
                <strong>Dra. Claudia Ortiz</strong>
                <span class="hi-time">06:50 h</span>
              </div>
              <span class="hi-desc">ID: MED-7492 • Terapia Intensiva. Motivo: Reseteo de credenciales en ingreso a guardia.</span>
              <span class="hi-signer">
                <svg viewBox="0 0 24 24" class="pen-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 20h9"/>
                  <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                Firmado: Ing. Roberto Silva (ADM-0014)
              </span>
            </div>

            <button type="button" class="btn-goto-bitacora" @click="emit('ir-bitacora')">
              <span>Ver bitácora de auditoría completa NOM-024</span>
              <svg viewBox="0 0 24 24" class="arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-bloqueos-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.bloqueos-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-directiva {
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.66rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.sep {
  color: #cbd5e1;
}

.rule-chip {
  font-size: 0.72rem;
  color: #475569;
  font-weight: 600;
}

.title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0.25rem 0 0 0;
}

.alert-banner-bloqueos {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #7f1d1d, #991b1b);
  color: #ffffff;
  padding: 1rem 1.25rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(153, 27, 27, 0.2);
}

.banner-left {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  max-width: 75%;
}

.shield-danger-box {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.shield-danger-svg {
  width: 20px;
  height: 20px;
  color: #fca5a5;
}

.banner-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.banner-title-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.banner-title-row h3 {
  font-size: 1rem;
  font-weight: 800;
  margin: 0;
}

.window-pill {
  font-size: 0.65rem;
  font-weight: 700;
  background-color: rgba(0, 0, 0, 0.3);
  color: #fef08a;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.banner-text {
  font-size: 0.74rem;
  color: #fee2e2;
  margin: 0;
  line-height: 1.35;
}

.btn-attend-pending {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  color: #991b1b;
  border: none;
  padding: 0.55rem 1rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  transition: all 0.15s ease;
}

.btn-attend-pending:hover {
  background-color: #fee2e2;
}

.feedback-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
}

.fb-svg {
  width: 15px;
  height: 15px;
}

.rfid-search-bar {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 0.75rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
}

.rfid-input-wrap {
  display: flex;
  align-items: center;
  position: relative;
}

.rfid-svg {
  position: absolute;
  left: 10px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
}

.rfid-input {
  width: 100%;
  padding: 0.5rem 0.75rem 0.5rem 2.25rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.78rem;
  color: #0f172a;
  outline: none;
  background-color: #f8fafc;
}

.rfid-input:focus {
  border-color: #0284c7;
  background-color: #ffffff;
}

.btn-find-account {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background-color: #0f172a;
  color: #ffffff;
  border: none;
  padding: 0.5rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-find-account:hover {
  background-color: #1e293b;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.bloqueos-grid {
  display: grid;
  grid-template-columns: 1fr 370px;
  gap: 1.25rem;
}

.containment-list-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.lh-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.lh-title {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0f172a;
}

.badge-count {
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.66rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.live-pulse-text {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  color: #0284c7;
  font-weight: 600;
}

.dot-blue {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #0284c7;
}

.cards-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.locked-user-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #b91c1c;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.15s ease;
}

.locked-user-card:hover {
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.07);
  border-color: #cbd5e1;
}

.locked-user-card.selected {
  border-color: #0284c7;
  background-color: #f0f9ff;
}

.user-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.user-meta-top {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
}

.avatar-box {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  background-color: #fee2e2;
  color: #b91c1c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 800;
  border: 1px solid #fecaca;
}

.user-title-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-status-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.user-name {
  font-size: 0.88rem;
  color: #0f172a;
}

.badge-preventivo {
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.62rem;
  font-weight: 800;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}

.badge-dept {
  background-color: #f1f5f9;
  color: #475569;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}

.user-email-id {
  font-size: 0.68rem;
  color: #64748b;
  font-family: monospace;
}

.user-time-ip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.66rem;
  color: #475569;
}

.time-svg {
  width: 12px;
  height: 12px;
  color: #0284c7;
}

.failed-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.55rem;
  border-radius: 6px;
}

.failed-num {
  font-size: 0.68rem;
  font-weight: 800;
  color: #b91c1c;
}

.failed-hash {
  font-family: monospace;
  font-size: 0.6rem;
  color: #64748b;
}

.clinical-impact-bar {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  flex-wrap: wrap;
}

.impact-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  color: #475569;
}

.impact-chip.danger {
  color: #b91c1c;
  font-weight: 700;
}

.impact-svg {
  width: 13px;
  height: 13px;
}

.card-action-btns {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-sec-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-sec-action:hover {
  background-color: #e2e8f0;
}

.btn-unlock-fast {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.btn-unlock-fast:hover {
  background-color: #0369a1;
}

.btn-svg-sm {
  width: 13px;
  height: 13px;
}

.compliance-footer-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.65rem 0.85rem;
}

.cf-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.cf-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.cf-meta {
  display: flex;
  flex-direction: column;
}

.cf-meta strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.cf-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.cf-metrics {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.m-val {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0f172a;
}

.m-val.green {
  color: #16a34a;
}

.m-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748b;
}

.protocol-sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.protocol-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.protocol-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.ph-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.ph-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.ph-left h4 {
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.prot-nom-pill {
  font-size: 0.62rem;
  font-weight: 800;
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 0.12rem 0.35rem;
  border-radius: 4px;
}

.protocol-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.target-user-strip {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.5rem;
}

.target-avatar {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background-color: #0f172a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.74rem;
  font-weight: 800;
}

.target-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.target-info strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.target-info span {
  font-size: 0.64rem;
  color: #64748b;
}

.target-status {
  font-size: 0.62rem;
  font-weight: 700;
  color: #16a34a;
  background-color: #f0fdf4;
  padding: 0.12rem 0.35rem;
  border-radius: 4px;
}

.fg {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.lbl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flbl {
  font-size: 0.7rem;
  font-weight: 700;
  color: #334155;
}

.req {
  font-size: 0.62rem;
  color: #b91c1c;
  font-weight: 600;
}

.ftextarea {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.74rem;
  color: #0f172a;
  outline: none;
  box-sizing: border-box;
  resize: vertical;
}

.ftextarea:focus {
  border-color: #0284c7;
}

.radio-group {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 2px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  color: #475569;
  cursor: pointer;
}

.confirm-check-label {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
  font-size: 0.66rem;
  color: #475569;
  line-height: 1.35;
  cursor: pointer;
}

.confirm-cb {
  margin-top: 2px;
}

.btn-submit-reactivation {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  background-color: #0284c7;
  color: #ffffff;
  padding: 0.65rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 800;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);
}

.btn-submit-reactivation:hover {
  background-color: #0369a1;
}

.btn-svg-lg {
  width: 16px;
  height: 16px;
}

.history-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.hc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.hc-header h4 {
  font-size: 0.76rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.hc-sub {
  font-size: 0.62rem;
  color: #64748b;
}

.hc-body {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.history-item:last-of-type {
  border-bottom: none;
}

.hi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hi-top strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.hi-time {
  font-size: 0.64rem;
  color: #64748b;
  font-family: monospace;
}

.hi-desc {
  font-size: 0.66rem;
  color: #475569;
  line-height: 1.25;
}

.hi-signer {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.62rem;
  color: #0284c7;
  font-weight: 600;
}

.pen-svg {
  width: 11px;
  height: 11px;
}

.btn-goto-bitacora {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: none;
  border: 1px solid #e2e8f0;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  color: #0284c7;
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  margin-top: 0.25rem;
  transition: all 0.15s ease;
}

.btn-goto-bitacora:hover {
  background-color: #f0f9ff;
  border-color: #bae6fd;
}

.arrow-svg {
  width: 13px;
  height: 13px;
}
</style>
