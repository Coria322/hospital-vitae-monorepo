<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type {
  UsuarioAdmin,
  CuentaBloqueada,
  TrazaAuditoria,
  ContenedorDocker,
  TelemetriaCluster
} from './types'

import AdminHeader from './components/AdminHeader.vue'
import AdminSidebar from './components/AdminSidebar.vue'
import AdminUsuariosSection from './components/AdminUsuariosSection.vue'
import AdminBloqueosSection from './components/AdminBloqueosSection.vue'
import AdminBitacoraSection from './components/AdminBitacoraSection.vue'
import AdminDockerSection from './components/AdminDockerSection.vue'

// --- ESTADO PRINCIPAL DE IDENTIDAD ---
const nombreUsuario = ref('Ing. Roberto Silva')
const cedulaAdmin = ref('ADM-0014')

// --- RELOJ INSTITUCIONAL EN VIVO ---
const currentDateTimeStr = ref('17 oct 2026 • 10:48 h')
let clockInterval: number | null = null

const updateClock = () => {
  const now = new Date()
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
  const d = now.getDate()
  const m = meses[now.getMonth()]
  const y = now.getFullYear()
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  currentDateTimeStr.value = `${d} ${m} ${y} • ${h}:${min} h`
}

// --- PESTAÑA ACTIVA ---
const activeTab = ref<'usuarios' | 'bloqueos' | 'bitacora' | 'docker'>('usuarios')

// --- CATÁLOGO DE USUARIOS INICIALES (184 EN TOTAL SIMULADOS, CON 6 REALISTAS EN VISTA) ---
const usuarios = ref<UsuarioAdmin[]>([
  {
    id: 'usr-1',
    nombre_completo: 'Dra. Mariana Gómez Treviño',
    iniciales: 'MG',
    cedula_matricula: '7894210',
    curp: 'GOTM840912MDFRRN01',
    correo_institucional: 'mgomez@hospitalvitae.mx',
    departamento: 'Urgencias Adultos',
    rol: 'MEDICO',
    rol_etiqueta: 'Médico ECE',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Hoy, 14:22 hrs',
    ultimo_acceso_ip: '10.14.7.88',
    terminal_origen: 'Terminal ED-04'
  },
  {
    id: 'usr-2',
    nombre_completo: 'Lic. Enf. Carlos Ramos Nava',
    iniciales: 'CR',
    cedula_matricula: '8921143',
    curp: 'RANC880215HDFRRL03',
    correo_institucional: 'cramos@hospitalvitae.mx',
    departamento: 'Triage y Choque',
    rol: 'ENFERMERIA',
    rol_etiqueta: 'Enfermería Operativa',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Hoy, 13:50 hrs',
    ultimo_acceso_ip: '10.14.2.12',
    terminal_origen: 'Tablet T-01'
  },
  {
    id: 'usr-3',
    nombre_completo: 'Lorena Soto Delgado',
    iniciales: 'LS',
    cedula_matricula: 'ADM-002',
    curp: 'SODL920511MDFRLN05',
    correo_institucional: 'lsoto@hospitalvitae.mx',
    departamento: 'Recepción Central',
    rol: 'RECEPCION',
    rol_etiqueta: 'Admisión / Cajas',
    mfa_activo: false,
    estado: 'INACTIVO',
    ultimo_acceso_fecha: 'Ayer, 21:00 hrs',
    ultimo_acceso_ip: '10.14.0.84',
    terminal_origen: 'FrontDesk-02'
  },
  {
    id: 'usr-4',
    nombre_completo: 'Q.F.B. Valeria Méndez Castro',
    iniciales: 'QV',
    cedula_matricula: '6512983',
    curp: 'MECV870420MDFRSR02',
    correo_institucional: 'vmendez@hospitalvitae.mx',
    departamento: 'Laboratorio Clínico',
    rol: 'LABORATORIO',
    rol_etiqueta: 'Laboratorio / Imagen',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Hoy, 11:15 hrs',
    ultimo_acceso_ip: '10.14.5.21',
    terminal_origen: 'LIS-Station'
  },
  {
    id: 'usr-5',
    nombre_completo: 'Dr. Fernando Palacios Ríos',
    iniciales: 'FP',
    cedula_matricula: '4418291',
    curp: 'PARF791008HDFRRN09',
    correo_institucional: 'fpalacios@hospitalvitae.mx',
    departamento: 'Consulta Externa',
    rol: 'MEDICO',
    rol_etiqueta: 'Médico ECE',
    mfa_activo: true,
    estado: 'BLOQUEADO',
    ultimo_acceso_fecha: '22 Oct, 18:40 hrs',
    ultimo_acceso_ip: '10.14.7.12',
    terminal_origen: 'Consultorio 04'
  },
  {
    id: 'usr-6',
    nombre_completo: 'Ing. Roberto Silva (Tú)',
    iniciales: 'RS',
    cedula_matricula: 'ADM-0014',
    curp: 'SILR830719HDFRNN08',
    correo_institucional: 'rsilva@hospitalvitae.mx',
    departamento: 'Seguridad y Sistemas',
    rol: 'ADMINISTRADOR',
    rol_etiqueta: 'Administrador TI',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Sesión Actual',
    ultimo_acceso_ip: '10.14.0.1',
    terminal_origen: 'Consola Segura'
  }
])

// --- LISTADO DE CUENTAS BLOQUEADAS PRIORITARIAS (2 INICIALES) ---
const cuentasBloqueadas = ref<CuentaBloqueada[]>([
  {
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
  },
  {
    id: 'bloq-2',
    id_usuario: 'ENF-3012',
    nombre: 'Enf. Manuel Torres',
    iniciales: 'MT',
    rol: 'Enfermero Triage',
    servicio: 'Triage y Choque',
    correo: 'enfermeria.triage@hospitalvitae.mx',
    terminal: 'Estación Triage-01',
    ip: '192.168.10.15',
    hora_bloqueo: '09:40 h',
    tiempo_transcurrido: 'hace 59 min',
    intentos_fallidos: 3,
    motivo_bloqueo: 'ORIGEN: Terminal Fija',
    pacientes_en_espera: 2,
    impacto_clinico: 'ALTO'
  }
])

// --- TRAZAS DE AUDITORÍA NOM-024 (TIEMPO REAL) ---
const trazas = ref<TrazaAuditoria[]>([
  {
    id: 'tr-1',
    timestamp_ntp: '10:48:12.418 UTC-6',
    usuario_nombre: 'Dr. Carlos Mendoza',
    usuario_id: 'MED-8841',
    usuario_rol: 'Médico Adscrito',
    ip_origen: '192.168.10.12',
    mac_address: '4A:60:B2:1A',
    terminal: 'WS-MED-04',
    accion: 'Apertura ECE',
    recurso_disociado: 'ECE-DEMO-0248',
    resultado_codigo: 200,
    resultado_etiqueta: '200 OK',
    hash_sha256: '4f9a72c18d4022a186e4ff98c6d17b84aa8981cb984f7621e2e50529c9124be3',
    jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  },
  {
    id: 'tr-2',
    timestamp_ntp: '10:45:04.102 UTC-6',
    usuario_nombre: 'Q.F.B. Mariana Téllez',
    usuario_id: 'LAB-1849',
    usuario_rol: 'Analista Laboratorio',
    ip_origen: '192.168.30.64',
    mac_address: '7E:D1:4F:9C',
    terminal: 'LIS-Station',
    accion: 'Validación STAT',
    recurso_disociado: 'EST-2026-9041',
    resultado_codigo: 200,
    resultado_etiqueta: '200 OK',
    hash_sha256: 'e83d11ac72b1467406a092c4311059f338827fa112009cb987621a22441098bd',
    jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  },
  {
    id: 'tr-3',
    timestamp_ntp: '10:15:33.892 UTC-6',
    usuario_nombre: 'IP No Reconocida',
    usuario_id: 'sdelgado (Target)',
    usuario_rol: 'Intento Anónimo',
    ip_origen: '192.168.10.45',
    mac_address: 'FF:FF:11:00:22',
    terminal: 'Terminal Urgencias Box 02',
    accion: 'Fallo Auth (x3)',
    recurso_disociado: 'AUTH-SERVICE',
    resultado_codigo: 403,
    resultado_etiqueta: '403 Bloqueado',
    hash_sha256: '99a0b33bd87192a001192e44810bce90451122a76810239485710293481239aa',
    jwt_token_preview: 'N/A - Rechazado por PAM'
  },
  {
    id: 'tr-4',
    timestamp_ntp: '09:58:20.604 UTC-6',
    usuario_nombre: 'Dra. Valeria Gómez',
    usuario_id: 'MED-2901',
    usuario_rol: 'Cirugía General',
    ip_origen: '192.168.12.80',
    mac_address: '1C:D0:FE:A1',
    terminal: 'Terminal QX-01',
    accion: 'Firma de Receta',
    recurso_disociado: 'RX-2026-7810',
    resultado_codigo: 200,
    resultado_etiqueta: '200 OK',
    hash_sha256: '3c19a4f0291ba786110a3341fe89a1024509bd231a44e55120398711993412aa',
    jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  },
  {
    id: 'tr-5',
    timestamp_ntp: '09:32:11.750 UTC-6',
    usuario_nombre: 'Enf. Sergio Rosas',
    usuario_id: 'ENF-6120',
    usuario_rol: 'Triage & Cuidados',
    ip_origen: '192.168.10.15',
    mac_address: 'B4:2A:80:3F',
    terminal: 'Tablet T-01',
    accion: 'Registro Signos',
    recurso_disociado: 'ECE-DEMO-0248',
    resultado_codigo: 200,
    resultado_etiqueta: '200 OK',
    hash_sha256: '7b11a9e3381a4b56890f110a2291cd4a8721ef65a1209b019348102a00293411',
    jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  },
  {
    id: 'tr-6',
    timestamp_ntp: '09:10:02.115 UTC-6',
    usuario_nombre: 'Ing. Roberto Silva',
    usuario_id: 'ADM-0014',
    usuario_rol: 'Admin Sistemas',
    ip_origen: '192.168.1.100',
    mac_address: '18:50:39:40',
    terminal: 'Consola SysAdmin',
    accion: 'Desbloqueo Cuenta',
    recurso_disociado: 'USR-ENF-4180',
    resultado_codigo: 200,
    resultado_etiqueta: '200 OK',
    hash_sha256: '1a88cfe49887102938475610293847561029384756102938475610294711823a',
    jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  }
])

// --- MATRIZ DE CONTENEDORES DOCKER (6 SERVICIOS) ---
const contenedores = ref<ContenedorDocker[]>([
  {
    id: 'c-1',
    nombre: 'vite-hospital-ui',
    imagen_version: 'v2.14.0-prod',
    estado: 'ONLINE',
    puertos: '80, 443 (Reverse Proxy Nginx)',
    latencia_rtt: '32ms RTT',
    descripcion: 'Frontend Clínico Vue 3 / Vite optimizado'
  },
  {
    id: 'c-2',
    nombre: 'postgres-ece-db',
    imagen_version: 'PostgreSQL 16.2',
    estado: 'SALUDABLE',
    puertos: 'Puerto: 5432',
    latencia_rtt: '4ms RTT',
    conexiones_activas: 184,
    conexiones_max: 250,
    descripcion: 'Base de Datos Transaccional ECE • Cifrado AES-256'
  },
  {
    id: 'c-3',
    nombre: 'redis-cache',
    imagen_version: 'Redis Alpine 7.2',
    estado: 'SALUDABLE',
    puertos: 'Puerto: 6379',
    latencia_rtt: '0ms de cola',
    descripcion: 'Gestión de sesiones concurrentes activas & JWT Store'
  },
  {
    id: 'c-4',
    nombre: 'hl7-fhir-gateway',
    imagen_version: 'FHIR R4 / HL7 v2.5',
    estado: 'SINCRONIZADO',
    puertos: 'Cifrado TLS 1.3 mTLS',
    latencia_rtt: '12ms RTT',
    descripcion: '8 Analizadores conectados (Laboratorio Central y Rayos X)'
  },
  {
    id: 'c-5',
    nombre: 'audit-logger',
    imagen_version: 'NOM-024 Compliant',
    estado: 'ONLINE',
    puertos: 'Puerto: 9092 (Append-Only)',
    latencia_rtt: '2ms RTT',
    descripcion: 'Bitácora Inmutable SHA-512 Encadenada • Trazabilidad Pericial'
  },
  {
    id: 'c-6',
    nombre: 'docker-auth-mfa',
    imagen_version: 'OAuth2 / TOTP',
    estado: 'ACTIVO',
    puertos: 'Puerto: 8443 Seguro',
    latencia_rtt: '8ms RTT',
    descripcion: 'Emisión JWT con rotación 15m • Políticas RBAC Clínicas'
  }
])

// --- TELEMETRÍA DEL CLÚSTER DE HARDWARE ---
const telemetria = ref<TelemetriaCluster>({
  cpu_modelo: 'Intel Xeon E5-2690v4',
  cpu_uso_pct: 24,
  cpu_carga_media: 0.42,
  cpu_nucleos: 28,
  ram_usada_gb: 18.4,
  ram_total_gb: 64,
  ram_uso_pct: 28,
  ram_buffer_gb: 6.2,
  nvme_usado_tb: 1.2,
  nvme_total_tb: 4.0,
  nvme_libre_tb: 2.8,
  nvme_io_rate: '820 MB/s',
  red_vlan: 'VLAN Hospitalaria',
  red_latencia_ms: 1.8,
  red_throughput: '10 Gbps SFP+',
  red_paquetes_perdidos_pct: 0.00
})

// --- MANEJADORES DE ACCIÓN ---
const handleSelectTab = (tab: 'usuarios' | 'bloqueos' | 'bitacora' | 'docker') => {
  activeTab.value = tab
}

const handleCrearUsuario = (nuevo: Partial<UsuarioAdmin>) => {
  usuarios.value.unshift({
    id: `usr-${Date.now()}`,
    nombre_completo: nuevo.nombre_completo || '',
    iniciales: nuevo.iniciales || 'US',
    cedula_matricula: nuevo.cedula_matricula || 'S/N',
    curp: nuevo.curp || '',
    correo_institucional: nuevo.correo_institucional || '',
    departamento: nuevo.departamento || 'General',
    rol: nuevo.rol || 'MEDICO',
    rol_etiqueta: nuevo.rol_etiqueta || 'Médico ECE',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Hoy, Reciente',
    ultimo_acceso_ip: '10.14.0.1',
    terminal_origen: 'Consola Central'
  })
}

const handleToggleBloqueo = (id: string) => {
  const u = usuarios.value.find(user => user.id === id)
  if (!u) return
  if (u.estado === 'BLOQUEADO') {
    u.estado = 'ACTIVO'
    const idx = cuentasBloqueadas.value.findIndex(cb => cb.id_usuario === u.cedula_matricula || cb.nombre === u.nombre_completo)
    if (idx !== -1) cuentasBloqueadas.value.splice(idx, 1)
  } else {
    u.estado = 'BLOQUEADO'
    cuentasBloqueadas.value.unshift({
      id: `bloq-${Date.now()}`,
      id_usuario: u.cedula_matricula,
      nombre: u.nombre_completo,
      iniciales: u.iniciales,
      rol: u.rol_etiqueta,
      servicio: u.departamento,
      correo: u.correo_institucional,
      terminal: u.terminal_origen,
      ip: u.ultimo_acceso_ip,
      hora_bloqueo: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' h',
      tiempo_transcurrido: 'hace 1 min',
      intentos_fallidos: 3,
      motivo_bloqueo: '#MANUAL-ADMIN-LOCK',
      pacientes_en_espera: 0,
      impacto_clinico: 'MEDIO'
    })
  }
}

const handleResetPassword = (u: UsuarioAdmin) => {
  alert(`Se han regenerado las credenciales provisionales para ${u.nombre_completo}. Enlace criptográfico despachado a ${u.correo_institucional}.`)
}

const handleDesbloquearCuenta = (payload: { id: string; motivo: string; medida: string }) => {
  const index = cuentasBloqueadas.value.findIndex(c => c.id === payload.id)
  if (index !== -1) {
    const cuenta = cuentasBloqueadas.value[index]
    if (!cuenta) return
    const u = usuarios.value.find(user => user.nombre_completo.includes(cuenta.nombre) || user.cedula_matricula === cuenta.id_usuario)
    if (u) u.estado = 'ACTIVO'
    cuentasBloqueadas.value.splice(index, 1)

    // Asentar en bitácora
    trazas.value.unshift({
      id: `tr-${Date.now()}`,
      timestamp_ntp: `${new Date().toLocaleTimeString()} UTC-6`,
      usuario_nombre: 'Ing. Roberto Silva',
      usuario_id: 'ADM-0014',
      usuario_rol: 'Admin Sistemas',
      ip_origen: '192.168.1.100',
      mac_address: '18:50:39:40',
      terminal: 'Consola SysAdmin',
      accion: 'Desbloqueo Cuenta',
      recurso_disociado: cuenta.id_usuario,
      resultado_codigo: 200,
      resultado_etiqueta: '200 OK',
      hash_sha256: 'a9821ef3901ba8721ef65a1209b019348102a0029341188219921ef4501239aa',
      jwt_token_preview: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
    })
  }
}

// --- ATAJOS GLOBALES DE TECLADO (Alt+U, Alt+B, Alt+A, Alt+S) ---
const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.altKey) {
    const key = e.key.toLowerCase()
    if (key === 'u') {
      e.preventDefault()
      activeTab.value = 'usuarios'
    } else if (key === 'b') {
      e.preventDefault()
      activeTab.value = 'bloqueos'
    } else if (key === 'a') {
      e.preventDefault()
      activeTab.value = 'bitacora'
    } else if (key === 's') {
      e.preventDefault()
      activeTab.value = 'docker'
    }
  }
}

onMounted(() => {
  updateClock()
  clockInterval = window.setInterval(updateClock, 30000)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  if (clockInterval) clearInterval(clockInterval)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="admin-app-container">
    <!-- 1. ENCABEZADO PERSISTENTE -->
    <AdminHeader
      :nombre-usuario="nombreUsuario"
      :cedula-admin="cedulaAdmin"
      :current-date-time-str="currentDateTimeStr"
      :bloqueos-count="cuentasBloqueadas.length"
      :docker-online-count="contenedores.length"
      :docker-total-count="contenedores.length"
      @ir-bloqueos="activeTab = 'bloqueos'"
    />

    <!-- 2. LAYOUT PRINCIPAL: SIDEBAR + MAIN WORKSPACE -->
    <div class="admin-main-layout">
      <!-- SIDEBAR -->
      <AdminSidebar
        :active-tab="activeTab"
        :bloqueos-count="cuentasBloqueadas.length"
        :total-usuarios="usuarios.length"
        @select-tab="handleSelectTab"
      />

      <!-- ÁREA DE CONTENIDO -->
      <main class="admin-workspace-content">
        <!-- PANTALLA 1: GESTIÓN DE USUARIOS (Alt+U) -->
        <AdminUsuariosSection
          v-if="activeTab === 'usuarios'"
          :usuarios="usuarios"
          @crear-usuario="handleCrearUsuario"
          @toggle-bloqueo="handleToggleBloqueo"
          @reset-password="handleResetPassword"
        />

        <!-- PANTALLA 2: CUENTAS BLOQUEADAS (Alt+B) -->
        <AdminBloqueosSection
          v-else-if="activeTab === 'bloqueos'"
          :cuentas-bloqueadas="cuentasBloqueadas"
          @desbloquear="handleDesbloquearCuenta"
          @ir-bitacora="activeTab = 'bitacora'"
        />

        <!-- PANTALLA 3: BITÁCORA NOM-024 (Alt+A) -->
        <AdminBitacoraSection
          v-else-if="activeTab === 'bitacora'"
          :trazas="trazas"
        />

        <!-- PANTALLA 4: ESTADO DEL SISTEMA DOCKER (Alt+S) -->
        <AdminDockerSection
          v-else-if="activeTab === 'docker'"
          :contenedores="contenedores"
          :telemetria="telemetria"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-app-container {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #0f172a;
}

.admin-main-layout {
  display: flex;
  flex: 1;
}

.admin-workspace-content {
  flex: 1;
  padding: 1.25rem 1.5rem;
  overflow-y: auto;
  box-sizing: border-box;
}
</style>
