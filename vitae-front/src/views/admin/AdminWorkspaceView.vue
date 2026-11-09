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
import AdminZonasSection from './components/AdminZonasSection.vue'
import adminService from '@/services/adminService'
import authService from '@/services/authService'

// --- ESTADO PRINCIPAL DE IDENTIDAD ---
const usuarioAuth = authService.getUsuario()
const nombreUsuario = ref(usuarioAuth?.nombre_completo || 'Ing. Roberto Silva')
const cedulaAdmin = ref(usuarioAuth?.id_usuario || 'ADM-001')
const rolActual = computed(() => (usuarioAuth?.rol || '').toUpperCase())
const esAdmin = computed(() => rolActual.value === 'ADMINISTRADOR')

const cerrarSesionYLoginAdmin = () => {
  authService.limpiarSesion()
  window.location.href = '/'
}

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
const activeTab = ref<'usuarios' | 'bloqueos' | 'bitacora' | 'docker' | 'zonas'>('usuarios')

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/admin/usuarios
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

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/admin/bloqueos y POST /api/v1/admin/bloqueos/{id}/desbloquear
// --- LISTADO DE CUENTAS BLOQUEADAS CONECTADO A LA BASE DE DATOS ---
const cuentasBloqueadas = ref<CuentaBloqueada[]>([])

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/auditoria (NOM-024 / AUDITORIA_I)
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

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/admin/infraestructura/contenedores
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

// TODO: [PENDIENTE-CONEXION-API] Reemplazar mock con endpoint real GET /api/v1/admin/infraestructura/telemetria
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

// --- CARGA DESDE API DE ADMINISTRACIÓN ---
const cargarUsuarios = async () => {
  try {
    const res = await adminService.listarUsuarios()
    const lista = res?.data || res || []
    if (lista.length > 0) {
      const rolEtiquetas: Record<string, string> = {
        MEDICO: 'Médico ECE',
        ENFERMERIA: 'Enfermería Operativa',
        RECEPCION: 'Admisión / Cajas',
        LABORATORIO: 'Laboratorio / Imagen',
        LABORATORISTA: 'Laboratorio / Imagen',
        ADMINISTRADOR: 'Administrador TI',
        AUDITOR: 'Auditor de Seguridad'
      }

      usuarios.value = lista.map((u: any) => {
        const partes = (u.nombre_completo || 'Usuario').split(' ')
        const ini = partes.length >= 2 && partes[0] && partes[1]
          ? `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
          : 'US'

        const rolNorm = u.rol === 'LABORATORISTA' ? 'LABORATORIO' : u.rol
        const estNorm = u.estado_cuenta === 'ACTIVA' ? 'ACTIVO' : u.estado_cuenta === 'INACTIVA' ? 'INACTIVO' : 'BLOQUEADO'

        return {
          id: u.id_usuario,
          nombre_completo: u.nombre_completo,
          iniciales: ini,
          cedula_matricula: u.cedula || u.id_usuario,
          curp: u.id_usuario,
          correo_institucional: `${u.id_usuario.toLowerCase()}@hospitalvitae.mx`,
          departamento: rolNorm === 'MEDICO' ? 'Urgencias Adultos' : rolNorm === 'ENFERMERIA' ? 'Triage y Choque' : rolNorm === 'RECEPCION' ? 'Recepción y Cajas' : rolNorm === 'LABORATORIO' ? 'Laboratorio Clínico' : 'Seguridad y Sistemas TI',
          rol: rolNorm,
          rol_etiqueta: rolEtiquetas[u.rol] || u.rol,
          mfa_activo: !!u.mfa_habilitado,
          estado: estNorm,
          ultimo_acceso_fecha: u.ultimo_acceso_en ? new Date(u.ultimo_acceso_en).toLocaleString('es-MX') : 'Sin registros recientes',
          ultimo_acceso_ip: '10.14.0.1',
          terminal_origen: 'Consola Central',
          bloqueos_previos: u.intentos_fallidos || 0
        }
      })

      // Actualizar cuentas bloqueadas desde el backend si tienen intentos_fallidos >= 3 o estado BLOQUEADA/INACTIVA/BAJA
      const bloqueados = lista.filter((u: any) => 
        u.estado_cuenta === 'BLOQUEADA' || 
        u.estado_cuenta === 'INACTIVA' || 
        u.estado_cuenta === 'BAJA' || 
        (u.intentos_fallidos && u.intentos_fallidos >= 3)
      )

      cuentasBloqueadas.value = bloqueados.map((u: any) => {
        const partes = (u.nombre_completo || 'Usuario').split(' ')
        const ini = partes.length >= 2 && partes[0] && partes[1]
          ? `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
          : 'US'

        const rolDesc = rolEtiquetas[u.rol] || u.rol
        const esBaja = u.estado_cuenta === 'BAJA' || u.estado_cuenta === 'INACTIVA'

        return {
          id: `bloq-${u.id_usuario}`,
          id_usuario: u.id_usuario,
          nombre: u.nombre_completo,
          iniciales: ini,
          rol: rolDesc,
          servicio: u.rol === 'MEDICO' ? 'Urgencias Box' : u.rol === 'ENFERMERIA' ? 'Triage y Choque' : 'Servicios Hospitalarios',
          correo: `${u.id_usuario.toLowerCase()}@hospitalvitae.mx`,
          terminal: 'Terminal Hospitalaria',
          ip: '10.14.0.1',
          hora_bloqueo: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' h',
          tiempo_transcurrido: 'Reciente',
          intentos_fallidos: u.intentos_fallidos || 3,
          motivo_bloqueo: esBaja ? 'Baja Administrativa' : '#ERR-PW-CONSEC (Bloqueada)',
          pacientes_en_espera: 0,
          impacto_clinico: u.rol === 'MEDICO' ? 'ALTO' : 'MEDIO'
        }
      })
    }
  } catch (err) {
    console.warn('Uso de catálogo de contingencia en frontend', err)
  }
}

const cargarBitacora = async (pagina = 1) => {
  try {
    const res = await adminService.obtenerBitacora({ per_page: 50, page: pagina })
    const logs = Array.isArray(res) ? res : (res?.data || [])
    if (logs.length > 0) {
      // Ordenar estrictamente descendente: del más nuevo al más antiguo
      const ordenados = [...logs].sort((a: any, b: any) => {
        const timeDiff = new Date(b.fecha_hora).getTime() - new Date(a.fecha_hora).getTime()
        if (timeDiff !== 0) return timeDiff
        return (b.id_auditoria || 0) - (a.id_auditoria || 0)
      })

      trazas.value = ordenados.map((l: any) => {
        const fecha = new Date(l.fecha_hora)
        const fechaFormateada = !isNaN(fecha.getTime())
          ? fecha.toLocaleString('es-MX', { 
              day: '2-digit', 
              month: 'short', 
              year: 'numeric',
              hour: '2-digit', 
              minute: '2-digit', 
              second: '2-digit', 
              hour12: false 
            }) + ' UTC'
          : l.fecha_hora

        return {
          id: `tr-${l.id_auditoria}`,
          timestamp_ntp: fechaFormateada,
          usuario_nombre: l.id_usuario || 'SISTEMA',
          usuario_id: l.id_usuario || 'SYS',
          usuario_rol: l.id_usuario?.startsWith('MED') ? 'Médico' : l.id_usuario?.startsWith('ENF') ? 'Enfermería' : l.id_usuario?.startsWith('ADM') ? 'Admin Sistemas' : 'Operativo',
          ip_origen: l.direccion_ip || '127.0.0.1',
          mac_address: '4A:60:B2:1A',
          terminal: l.modulo || 'Terminal Hospitalaria',
          accion: `${l.tipo_movimiento} en ${l.tabla_afectada}`,
          recurso_disociado: l.registro_id || 'REGISTRO',
          resultado_codigo: l.resultado === 'EXITOSO' ? 200 : 403,
          resultado_etiqueta: l.resultado === 'EXITOSO' ? '200 OK' : '403 Rechazado',
          hash_sha256: l.hash_sha256 || '0000000000000000000000000000000000000000000000000000000000000000',
          jwt_token_preview: 'SHA-256 Encadenado (Inalterable)'
        }
      })
    }
  } catch (err) {
    console.warn('Uso de bitácora de contingencia', err)
  }
}

const cargarSalud = async () => {
  try {
    const salud = await adminService.obtenerSalud()
    const dbContainer = contenedores.value.find(c => c.id === 'c-2' || c.nombre.includes('postgres') || c.nombre.includes('db'))
    if (dbContainer) {
      dbContainer.estado = salud.database === 'CONNECTED' ? 'SALUDABLE' : 'DEGRADADO'
      dbContainer.latencia_rtt = salud.database === 'CONNECTED' ? '2ms RTT (Live)' : 'Sin conexión'
    }
    const apiContainer = contenedores.value.find(c => c.id === 'c-1' || c.nombre.includes('vite') || c.nombre.includes('ui'))
    if (apiContainer) {
      apiContainer.estado = salud.status === 'UP' ? 'ONLINE' : 'DEGRADADO'
      apiContainer.latencia_rtt = salud.status === 'UP' ? '18ms RTT (Live)' : 'Sin conexión'
    }
  } catch (err) {
    console.warn('Error al verificar salud del clúster', err)
  }
}

// --- MANEJADORES DE ACCIÓN ---
const handleSelectTab = (tab: 'usuarios' | 'bloqueos' | 'bitacora' | 'docker' | 'zonas') => {
  activeTab.value = tab
}

const handleCrearUsuario = async (nuevo: Partial<UsuarioAdmin> & { password?: string; id_usuario?: string }) => {
  try {
    const rolBackend = nuevo.rol === 'LABORATORIO' ? 'LABORATORISTA' : (nuevo.rol || 'MEDICO')
    const payload = {
      id_usuario: nuevo.id_usuario ? nuevo.id_usuario : undefined,
      nombre_completo: nuevo.nombre_completo || 'Usuario Nuevo',
      rol: rolBackend,
      password: nuevo.password || 'password123',
      cedula: nuevo.cedula_matricula && nuevo.cedula_matricula !== 'S/N' ? nuevo.cedula_matricula : undefined
    }

    const creado = await adminService.crearUsuario(payload)
    const idAsignado = creado?.id_usuario || nuevo.id_usuario || 'ASIGNADO'
    alert(`Usuario ${nuevo.nombre_completo} creado exitosamente.\n\nIdentificador institucional asignado: ${idAsignado}\nRol: ${rolBackend}\nContraseña provisional emitida.`)
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al registrar usuario: ' + (err?.response?.data?.message || err.message))
  }
}

const handleToggleBloqueo = async (id: string) => {
  const u = usuarios.value.find(user => user.id === id)
  if (!u) return
  try {
    if (u.estado === 'ACTIVO') {
      await adminService.bloquearUsuario(id)
      alert(`Usuario ${u.nombre_completo} (${id}) ha sido BLOQUEADO preventivamente. Sus sesiones fueron revocadas y ahora aparece en el Centro de Reactivación de Cuentas.`)
    } else {
      await adminService.reactivarUsuario(id)
      alert(`Usuario ${u.nombre_completo} (${id}) ha sido DESBLOQUEADO y reactivado exitosamente.`)
    }
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al modificar estatus de usuario: ' + (err?.response?.data?.message || err.message))
  }
}

const handleResetPassword = async (u: UsuarioAdmin) => {
  try {
    const res = await adminService.resetPassword(u.id)
    const passGen = res?.data?.password_provisional || 'Vitae.2026!'
    alert(`Credenciales provisionales generadas para ${u.nombre_completo} (${u.id}).\n\nNueva contraseña temporal: ${passGen}\nSe restableció el contador de intentos fallidos.`)
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al resetear credenciales: ' + (err?.response?.data?.message || err.message))
  }
}

const handleActualizarUsuario = async (payload: {
  id: string
  nombre_completo: string
  rol: string
  cedula?: string | null
  departamento?: string
  estado_cuenta?: string
  password?: string
}) => {
  try {
    const rolBackend = payload.rol === 'LABORATORIO' ? 'LABORATORISTA' : payload.rol
    await adminService.actualizarUsuario(payload.id, {
      nombre_completo: payload.nombre_completo,
      rol: rolBackend,
      cedula: payload.cedula,
      estado_cuenta: payload.estado_cuenta,
      password: payload.password
    })
    alert(`Usuario ${payload.nombre_completo} (${payload.id}) actualizado exitosamente en el sistema.`)
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al actualizar usuario: ' + (err?.response?.data?.message || err.message))
  }
}

const handleEliminarUsuario = async (id: string) => {
  try {
    await adminService.eliminarUsuario(id)
    alert(`Usuario ${id} dado de baja exitosamente. Sesiones revocadas conforme a la NOM-024.`)
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al dar de baja usuario: ' + (err?.response?.data?.message || err.message))
  }
}

const handleDesbloquearCuenta = async (payload: { id: string; motivo: string; medida: string }) => {
  const cuenta = cuentasBloqueadas.value.find(c => c.id === payload.id)
  const idUsuario = cuenta?.id_usuario || payload.id.replace('bloq-', '')
  try {
    await adminService.reactivarUsuario(idUsuario)
    alert(`Cuenta de usuario ${idUsuario} desbloqueada exitosamente. Intentos fallidos restablecidos a 0.`)
    await cargarUsuarios()
    await cargarBitacora()
  } catch (err: any) {
    alert('Error al desbloquear cuenta: ' + (err?.response?.data?.message || err.message))
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
    } else if (key === 'z') {
      e.preventDefault()
      activeTab.value = 'zonas'
    }
  }
}

onMounted(() => {
  updateClock()
  clockInterval = window.setInterval(updateClock, 30000)
  window.addEventListener('keydown', handleGlobalKeydown)
  cargarUsuarios()
  cargarBitacora()
  cargarSalud()
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

    <!-- AVISO DE SESIÓN CUANDO NO ES ADMIN -->
    <div v-if="!esAdmin" class="session-warning-banner">
      <div class="warning-text">
        <strong>Atención de Control de Acceso (RBAC):</strong>
        <span>Has iniciado sesión como {{ nombreUsuario }} con rol institucional <b>{{ rolActual }}</b>. Las operaciones de administración (bloquear cuentas, reactivar, editar y crear colaboradores) requieren sesión de ADMINISTRADOR.</span>
      </div>
      <button type="button" class="btn-switch-admin" @click="cerrarSesionYLoginAdmin">
        Iniciar Sesión como ADM-001
      </button>
    </div>

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
          @actualizar-usuario="handleActualizarUsuario"
          @eliminar-usuario="handleEliminarUsuario"
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
          @actualizar="cargarSalud"
        />

        <!-- PANTALLA 5: ASIGNACIÓN DE GUARDIAS Y ZONAS (Alt+Z) -->
        <AdminZonasSection
          v-else-if="activeTab === 'zonas'"
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

.session-warning-banner {
  background-color: #fef2f2;
  border-bottom: 2px solid #ef4444;
  padding: 0.75rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.warning-text {
  font-size: 0.82rem;
  color: #991b1b;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.warning-text code {
  background-color: #fee2e2;
  padding: 0.1rem 0.35rem;
  border-radius: 3px;
  font-weight: 700;
}

.btn-switch-admin {
  background-color: #dc2626;
  color: #ffffff;
  border: none;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.btn-switch-admin:hover {
  background-color: #b91c1c;
}
</style>
