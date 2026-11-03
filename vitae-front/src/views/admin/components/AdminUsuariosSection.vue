<script setup lang="ts">
import { ref, computed } from 'vue'
import type { UsuarioAdmin, RolHospitalario } from '../types'

const props = defineProps<{
  usuarios: UsuarioAdmin[]
}>()

const emit = defineEmits<{
  (e: 'crear-usuario', usuario: Partial<UsuarioAdmin>): void
  (e: 'toggle-bloqueo', id: string): void
  (e: 'reset-password', usuario: UsuarioAdmin): void
}>()

// Filtros de categoría y texto
const filtroRol = ref<string>('TODOS')
const busquedaTexto = ref('')
const feedbackMsg = ref('')

// Formulario de nuevo usuario
const formNombre = ref('')
const formCedula = ref('')
const formCurp = ref('')
const formCorreo = ref('')
const formDept = ref('Urgencias Adultos')
const formRol = ref<RolHospitalario>('MEDICO')
const formMfa = ref('TOTP')

const usuariosFiltrados = computed(() => {
  return props.usuarios.filter(u => {
    if (filtroRol.value !== 'TODOS' && u.rol !== filtroRol.value) {
      return false
    }
    if (busquedaTexto.value.trim()) {
      const q = busquedaTexto.value.toLowerCase().trim()
      const matchNom = u.nombre_completo.toLowerCase().includes(q)
      const matchCed = u.cedula_matricula.toLowerCase().includes(q)
      const matchDept = u.departamento.toLowerCase().includes(q)
      const matchEmail = u.correo_institucional.toLowerCase().includes(q)
      return matchNom || matchCed || matchDept || matchEmail
    }
    return true
  })
})

const countMedicos = computed(() => props.usuarios.filter(u => u.rol === 'MEDICO').length)
const countEnf = computed(() => props.usuarios.filter(u => u.rol === 'ENFERMERIA').length)
const countRecep = computed(() => props.usuarios.filter(u => u.rol === 'RECEPCION').length)
const countLab = computed(() => props.usuarios.filter(u => u.rol === 'LABORATORIO').length)
const countAdmin = computed(() => props.usuarios.filter(u => u.rol === 'ADMINISTRADOR').length)

const handleSincronizarLdap = () => {
  feedbackMsg.value = 'Sincronización LDAP con Directorio Activo (AD-CORP-DC01) exitosa: 184 identidades conciliadas.'
  setTimeout(() => {
    feedbackMsg.value = ''
  }, 4000)
}

const handleExportarCsv = () => {
  feedbackMsg.value = 'Exportando catálogo institucional de usuarios en CSV firmado con sello de tiempo...'
  setTimeout(() => {
    feedbackMsg.value = ''
  }, 3500)
}

const handleLimpiarForm = () => {
  formNombre.value = ''
  formCedula.value = ''
  formCurp.value = ''
  formCorreo.value = ''
}

const handleGuardarUsuario = () => {
  if (!formNombre.value.trim()) {
    alert('Por favor ingrese el nombre completo del colaborador.')
    return
  }

  const partes = formNombre.value.trim().split(' ')
  const ini = partes.length >= 2 && partes[0] && partes[1]
    ? `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase()
    : 'US'

  emit('crear-usuario', {
    nombre_completo: formNombre.value,
    iniciales: ini,
    cedula_matricula: formCedula.value || 'S/N',
    curp: formCurp.value || 'CURP-PENDIENTE',
    correo_institucional: formCorreo.value || `${ini.toLowerCase()}@hospitalvitae.mx`,
    departamento: formDept.value,
    rol: formRol.value,
    rol_etiqueta: formRol.value === 'MEDICO' ? 'Médico ECE' : formRol.value === 'ENFERMERIA' ? 'Enfermería Operativa' : formRol.value === 'LABORATORIO' ? 'Laboratorio / Imagen' : formRol.value === 'RECEPCION' ? 'Admisión / Cajas' : 'Administrador TI',
    mfa_activo: true,
    estado: 'ACTIVO',
    ultimo_acceso_fecha: 'Hoy, Reciente',
    ultimo_acceso_ip: '10.14.0.1',
    terminal_origen: 'Consola Central'
  })

  feedbackMsg.value = `¡Usuario ${formNombre.value} creado con credenciales provisionales emitidas!`
  handleLimpiarForm()
  setTimeout(() => {
    feedbackMsg.value = ''
  }, 4000)
}
</script>

<template>
  <div class="admin-usuarios-section">
    <!-- CABECERA INSTITUCIONAL Y ACCIONES SUPERIORES -->
    <div class="users-top-panel">
      <div class="top-panel-left">
        <div class="top-tags-row">
          <span class="badge-nom">CUMPLIMIENTO NORMATIVO NOM-024-SSA3-2012 & ISO/IEC 27001</span>
        </div>
        <h2 class="top-title">Directorio y Control de Acceso de Usuarios</h2>
        <p class="top-desc">
          Administración de identidad digital, segregación estricta de funciones asistenciales y privilegios criptográficos para el personal hospitalario.
        </p>
      </div>

      <div class="top-panel-right">
        <button type="button" class="btn-outline-action" @click="handleSincronizarLdap">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          <span>Sincronizar Directorio Activo [LDAP]</span>
        </button>

        <button type="button" class="btn-outline-action" @click="handleExportarCsv">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>Exportar Catálogo [CSV]</span>
        </button>

        <button type="button" class="btn-primary-action" @click="handleGuardarUsuario">
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span>Nuevo Usuario [Alt+N]</span>
        </button>
      </div>
    </div>

    <!-- BANNER DE FEEDBACK -->
    <div v-if="feedbackMsg" class="feedback-banner">
      <svg viewBox="0 0 24 24" class="feedback-svg" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <span>{{ feedbackMsg }}</span>
    </div>

    <!-- 4 TARJETAS DE KPIS DE IDENTIDAD -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-header-row">
          <span class="kpi-lbl">TOTAL USUARIOS ACTIVOS</span>
          <div class="kpi-icon-box">
            <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-val">{{ usuarios.length }}</span>
          <span class="kpi-sub positive">100% validados institucional</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-header-row">
          <span class="kpi-lbl">SESIONES CONCURRENTES</span>
          <div class="kpi-icon-box">
            <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
              <line x1="8" y1="21" x2="16" y2="21"/>
              <line x1="12" y1="17" x2="12" y2="21"/>
            </svg>
          </div>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-val">42</span>
          <span class="kpi-sub">Servidores Balanceados</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-header-row">
          <span class="kpi-lbl">ROLES DEFINIDOS</span>
          <div class="kpi-icon-box">
            <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-val">5</span>
          <span class="kpi-sub">RBAC Estricto (Perfilado)</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-header-row">
          <span class="kpi-lbl">PENDIENTES DE ASIGNAR ROL</span>
          <div class="kpi-icon-box">
            <svg viewBox="0 0 24 24" class="kpi-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 14 14"/>
            </svg>
          </div>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-val">0</span>
          <span class="kpi-sub green">Bandeja al día</span>
        </div>
      </div>
    </div>

    <!-- FORMULARIO DE ALTA Y MATRIZ RBAC -->
    <div class="form-matrix-grid">
      <!-- FORMULARIO DE ALTA / EDICIÓN -->
      <div class="card-form-box">
        <div class="form-header-bar">
          <div class="fh-left">
            <svg viewBox="0 0 24 24" class="fh-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            <div class="fh-meta">
              <h3>Alta y Edición de Identidad Digital</h3>
              <span>Llene la ficha técnica institucional. No se permiten accesos no asociados a personal activo.</span>
            </div>
          </div>
          <span class="nodo-pill">NODO: CAPTURA LIMPIA</span>
        </div>

        <div class="form-body">
          <div class="form-row-2">
            <div class="fg">
              <label class="flbl">Nombre Completo Institucional *</label>
              <input
                v-model="formNombre"
                type="text"
                placeholder="Nombre completo del colaborador (ej. Dr. Carlos Mendoza)"
                class="finput"
              />
              <span class="fhint">Conforme a identificación oficial y registro de recursos humanos.</span>
            </div>

            <div class="fg">
              <label class="flbl">Cédula Profesional / Matrícula <span class="opc">(Opcional para admin)</span></label>
              <input
                v-model="formCedula"
                type="text"
                placeholder="Cédula profesional / Matrícula hospitalaria"
                class="finput"
              />
              <span class="fhint">Requerida para facultades de prescripción según Ley General de Salud.</span>
            </div>
          </div>

          <div class="form-row-2">
            <div class="fg">
              <label class="flbl">CURP / Identificador Fiscal *</label>
              <input
                v-model="formCurp"
                type="text"
                placeholder="CURP / IDENTIFICADOR FISCAL (18 CARACTERES)"
                class="finput mono"
              />
              <span class="fhint">Clave unívoca nacional para desambiguación en registros de auditoría.</span>
            </div>

            <div class="fg">
              <label class="flbl">Correo Electrónico Institucional *</label>
              <input
                v-model="formCorreo"
                type="email"
                placeholder="Correo institucional corporativo (@hospitalvitae.mx)"
                class="finput"
              />
              <span class="fhint">Medio verificado para despacho de llaves temporales y tokens MFA.</span>
            </div>
          </div>

          <div class="form-row-3">
            <div class="fg">
              <label class="flbl">Servicio / Departamento *</label>
              <select v-model="formDept" class="fselect">
                <option value="Urgencias Adultos">Urgencias Adultos</option>
                <option value="Triage y Choque">Triage y Choque</option>
                <option value="Laboratorio Clínico">Laboratorio Clínico e Imagen</option>
                <option value="Consulta Externa">Consulta Externa</option>
                <option value="Recepción y Cajas">Recepción y Admisión</option>
                <option value="Seguridad y Sistemas">Seguridad y Sistemas TI</option>
              </select>
            </div>

            <div class="fg">
              <label class="flbl">Rol Funcional de Acceso *</label>
              <select v-model="formRol" class="fselect">
                <option value="MEDICO">Médico ECE (Prescriptor)</option>
                <option value="ENFERMERIA">Enfermería Operativa (Kardex)</option>
                <option value="RECEPCION">Admisión / Cajas (Demográfico)</option>
                <option value="LABORATORIO">Laboratorio / Imagen (LIS/RIS)</option>
                <option value="ADMINISTRADOR">Administrador TI (SysAdmin)</option>
              </select>
            </div>

            <div class="fg">
              <label class="flbl">Autenticación Secundaria (MFA) *</label>
              <select v-model="formMfa" class="fselect">
                <option value="TOTP">Obligatoria App TOTP (Google Authenticator)</option>
                <option value="SMS">Token Temporal por SMS Seguro</option>
                <option value="FIDO2">Llave Criptográfica FIDO2 / eToken</option>
              </select>
            </div>
          </div>

          <!-- AVISO DE PRIVILEGIO MÍNIMO -->
          <div class="privilege-box">
            <svg viewBox="0 0 24 24" class="priv-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <polyline points="9 12 11 14 15 10"/>
            </svg>
            <div class="priv-text">
              <strong>PRINCIPIO DE MENOR PRIVILEGIO (NOM-024-SSA3-2012)</strong>
              <p>
                Los permisos no otorgan acceso a datos clínicos no disociados fuera del rol asistencial directo. Las cuentas administrativas tienen revocada por arquitectura la visualización de notas de evolución y diagnósticos de pacientes.
              </p>
            </div>
          </div>

          <div class="form-actions-row">
            <button type="button" class="btn-clear" @click="handleLimpiarForm">
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="1 4 1 10 7 10"/>
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
              </svg>
              <span>Limpiar Formulario</span>
            </button>

            <button type="button" class="btn-submit-user" @click="handleGuardarUsuario">
              <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                <polyline points="17 21 17 13 7 13 7 21"/>
                <polyline points="7 3 7 8 15 8"/>
              </svg>
              <span>Guardar y Enviar Credenciales Provisionales</span>
            </button>
          </div>
        </div>
      </div>

      <!-- MATRIZ DE PRIVILEGIOS RBAC Y LDAP EN TIEMPO REAL -->
      <div class="card-matrix-col">
        <!-- MATRIZ RBAC -->
        <div class="matrix-card">
          <div class="matrix-header">
            <div class="mh-left">
              <svg viewBox="0 0 24 24" class="matrix-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <h4>Matriz de Privilegios RBAC</h4>
            </div>
            <span class="badge-nom-mini">NOM-024</span>
          </div>

          <div class="matrix-body">
            <span class="matrix-hint">
              Niveles de acceso concedidos de manera atómica según el rol institucional seleccionado (<strong>{{ formRol }}</strong>):
            </span>

            <div class="rbac-role-block" :class="{ highlighted: formRol === 'MEDICO' }">
              <div class="role-block-title">
                <strong>Médico ECE</strong>
                <span class="badge-active-perm">Firma Electrónica Avanzada</span>
              </div>
              <div class="perm-chips">
                <span class="perm-chip green">Lectura/Escritura ECE</span>
                <span class="perm-chip green">Prescripción Farmacéutica</span>
                <span class="perm-chip green">Interconsultas y Órdenes</span>
                <span class="perm-chip red">Config. Sistemas TI</span>
              </div>
            </div>

            <div class="rbac-role-block" :class="{ highlighted: formRol === 'ENFERMERIA' }">
              <div class="role-block-title">
                <strong>Enfermería Operativa</strong>
                <span class="badge-sub-role">Registro Asistencial</span>
              </div>
              <div class="perm-chips">
                <span class="perm-chip green">Captura Signos Vitales</span>
                <span class="perm-chip green">Hoja de Medicación</span>
                <span class="perm-chip blue">Diagnóstico Definitivo (R/O)</span>
                <span class="perm-chip green">Alta Hospitalaria Asistida</span>
              </div>
            </div>

            <div class="rbac-role-block" :class="{ highlighted: formRol === 'RECEPCION' }">
              <div class="role-block-title">
                <strong>Admisión / Cajas</strong>
                <span class="badge-sub-role">Datos Administrativos</span>
              </div>
              <div class="perm-chips">
                <span class="perm-chip green">Padrón Demográfico</span>
                <span class="perm-chip green">Facturación y Pagos</span>
                <span class="perm-chip red">Notas Clínicas (Ciego)</span>
                <span class="perm-chip red">Laboratorios</span>
              </div>
            </div>

            <div class="rbac-role-block" :class="{ highlighted: formRol === 'ADMINISTRADOR' }">
              <div class="role-block-title">
                <strong>Administrador TI</strong>
                <span class="badge-danger-role">Cero Acceso Clínico</span>
              </div>
              <div class="perm-chips">
                <span class="perm-chip green">Auditoría y Logs</span>
                <span class="perm-chip green">Gestión de Cuentas</span>
                <span class="perm-chip red">ECE Bloqueado (Disociado)</span>
                <span class="perm-chip green">Parches y Docker</span>
              </div>
            </div>

            <div class="encryption-notice">
              <svg viewBox="0 0 24 24" class="enc-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <span>Cifrado AES-256 en reposo con separación de llaves HSM.</span>
            </div>
          </div>
        </div>

        <!-- WIDGET SINCRONIZACIÓN LDAP -->
        <div class="ldap-card">
          <div class="ldap-header">
            <h4>Sincronización LDAP</h4>
            <span class="dot-online"></span>
          </div>
          <div class="ldap-body">
            <div class="ldap-metric-circle">98%</div>
            <div class="ldap-meta">
              <strong>AD-CORP-DC01.vitae.local</strong>
              <span>Último pulso: hace 2 min</span>
              <span class="tls-status">TLS 1.3 Seguro</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- TABLA GENERAL DE COLABORADORES E IDENTIDADES -->
    <div class="users-table-card">
      <div class="table-filters-bar">
        <div class="search-input-box">
          <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            v-model="busquedaTexto"
            type="text"
            placeholder="Buscar por Nombre, Cédula Profesional, Departamento o Correo..."
            class="search-input"
          />
        </div>

        <div class="role-filter-pills">
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'TODOS' }"
            @click="filtroRol = 'TODOS'"
          >
            Todos ({{ usuarios.length }})
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'MEDICO' }"
            @click="filtroRol = 'MEDICO'"
          >
            Médicos ({{ countMedicos }})
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'ENFERMERIA' }"
            @click="filtroRol = 'ENFERMERIA'"
          >
            Enfermería ({{ countEnf }})
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'RECEPCION' }"
            @click="filtroRol = 'RECEPCION'"
          >
            Admisión ({{ countRecep }})
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'LABORATORIO' }"
            @click="filtroRol = 'LABORATORIO'"
          >
            Laboratorio ({{ countLab }})
          </button>
          <button
            type="button"
            class="pill"
            :class="{ active: filtroRol === 'ADMINISTRADOR' }"
            @click="filtroRol = 'ADMINISTRADOR'"
          >
            Sistemas ({{ countAdmin }})
          </button>
        </div>
      </div>

      <!-- TABLA DE USUARIOS -->
      <table class="users-table">
        <thead>
          <tr>
            <th>COLABORADOR / IDENTIDAD</th>
            <th>ROL ASIGNADO</th>
            <th>DEPARTAMENTO</th>
            <th>ÚLTIMO ACCESO ECE</th>
            <th>ESTADO DE CUENTA</th>
            <th>ACCIONES</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in usuariosFiltrados" :key="user.id">
            <td class="cell-user">
              <div class="user-avatar-tag" :class="user.rol.toLowerCase()">
                {{ user.iniciales }}
              </div>
              <div class="user-ident-meta">
                <strong class="colab-name">{{ user.nombre_completo }}</strong>
                <span class="colab-sub">CÉD: {{ user.cedula_matricula }} • {{ user.correo_institucional }}</span>
              </div>
            </td>

            <td class="cell-role">
              <span class="role-badge" :class="user.rol.toLowerCase()">
                {{ user.rol_etiqueta }}
              </span>
            </td>

            <td class="cell-dept">
              <span class="dept-text">{{ user.departamento }}</span>
            </td>

            <td class="cell-access">
              <div class="access-info">
                <strong>{{ user.ultimo_acceso_fecha }}</strong>
                <span>IP: {{ user.ultimo_acceso_ip }} [{{ user.terminal_origen }}]</span>
              </div>
            </td>

            <td class="cell-status">
              <span class="status-chip" :class="user.estado.toLowerCase()">
                <span class="dot-status"></span>
                {{ user.estado }}
              </span>
            </td>

            <td class="cell-actions">
              <div class="actions-group">
                <button
                  type="button"
                  class="btn-row-action"
                  title="Editar permisos de identidad"
                >
                  <svg viewBox="0 0 24 24" class="row-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9"/>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                  </svg>
                </button>

                <button
                  type="button"
                  class="btn-row-action"
                  title="Resetear contraseña / Reemitir MFA"
                  @click="emit('reset-password', user)"
                >
                  <svg viewBox="0 0 24 24" class="row-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                </button>

                <button
                  type="button"
                  class="btn-row-action danger"
                  :title="user.estado === 'BLOQUEADO' ? 'Desbloquear cuenta' : 'Bloquear cuenta preventivamente'"
                  @click="emit('toggle-bloqueo', user.id)"
                >
                  <svg v-if="user.estado === 'BLOQUEADO'" viewBox="0 0 24 24" class="row-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="row-svg" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- PAGINACIÓN -->
      <div class="table-footer-pagination">
        <span class="pagination-text">Mostrando registros 1 a {{ usuariosFiltrados.length }} de {{ usuarios.length }} • Filtro activo: {{ filtroRol }}</span>
        <div class="pagination-controls">
          <button type="button" class="btn-pg" disabled>Anterior</button>
          <span class="pg-num active">1</span>
          <span class="pg-sep">/ 31</span>
          <button type="button" class="btn-pg">Siguiente</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-usuarios-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.users-top-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.top-panel-left {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 60%;
}

.top-tags-row {
  display: flex;
  align-items: center;
}

.badge-nom {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.66rem;
  font-weight: 800;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  letter-spacing: 0.04em;
}

.top-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.top-desc {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.top-panel-right {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.btn-outline-action {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-outline-action:hover {
  background-color: #f8fafc;
  border-color: #94a3b8;
  color: #0f172a;
}

.btn-primary-action {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  padding: 0.45rem 0.95rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
  transition: background-color 0.15s ease;
}

.btn-primary-action:hover {
  background-color: #0369a1;
}

.btn-svg {
  width: 14px;
  height: 14px;
}

.feedback-banner {
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

.feedback-svg {
  width: 15px;
  height: 15px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.kpi-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-lbl {
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.04em;
}

.kpi-icon-box {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
}

.kpi-value-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kpi-val {
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
}

.kpi-sub {
  font-size: 0.68rem;
  color: #64748b;
}

.kpi-sub.positive {
  color: #0284c7;
  font-weight: 600;
}

.kpi-sub.green {
  color: #16a34a;
  font-weight: 700;
}

.form-matrix-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 1.25rem;
}

.card-form-box {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.form-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.fh-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.fh-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
}

.fh-meta h3 {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.fh-meta span {
  font-size: 0.65rem;
  color: #64748b;
}

.nodo-pill {
  font-size: 0.62rem;
  font-weight: 800;
  background-color: #e2e8f0;
  color: #334155;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.form-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}

.fg {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.flbl {
  font-size: 0.72rem;
  font-weight: 700;
  color: #334155;
}

.opc {
  font-weight: normal;
  color: #94a3b8;
  font-size: 0.65rem;
}

.finput, .fselect {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.76rem;
  color: #0f172a;
  outline: none;
  background-color: #ffffff;
  box-sizing: border-box;
}

.finput.mono {
  font-family: monospace;
}

.finput:focus, .fselect:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.fhint {
  font-size: 0.64rem;
  color: #94a3b8;
  line-height: 1.25;
}

.privilege-box {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0.65rem;
}

.priv-svg {
  width: 18px;
  height: 18px;
  color: #0284c7;
  flex-shrink: 0;
  margin-top: 2px;
}

.priv-text strong {
  display: block;
  font-size: 0.72rem;
  color: #0f172a;
  margin-bottom: 2px;
}

.priv-text p {
  margin: 0;
  font-size: 0.68rem;
  color: #475569;
  line-height: 1.35;
}

.form-actions-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  border-top: 1px solid #f1f5f9;
  padding-top: 0.75rem;
}

.btn-clear {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit-user {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
}

.card-matrix-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.matrix-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.matrix-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.mh-left {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.matrix-svg {
  width: 16px;
  height: 16px;
  color: #0284c7;
}

.mh-left h4 {
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.badge-nom-mini {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.62rem;
  font-weight: 800;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.matrix-body {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.matrix-hint {
  font-size: 0.68rem;
  color: #64748b;
  line-height: 1.35;
}

.rbac-role-block {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.55rem;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  transition: all 0.15s ease;
}

.rbac-role-block.highlighted {
  border-color: #0284c7;
  background-color: #f0f9ff;
  box-shadow: 0 0 0 1.5px #0284c7;
}

.role-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-block-title strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.badge-active-perm {
  font-size: 0.6rem;
  font-weight: 700;
  color: #0284c7;
}

.badge-sub-role {
  font-size: 0.6rem;
  color: #64748b;
}

.badge-danger-role {
  font-size: 0.6rem;
  font-weight: 700;
  color: #b91c1c;
}

.perm-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.perm-chip {
  font-size: 0.62rem;
  font-weight: 600;
  padding: 0.12rem 0.35rem;
  border-radius: 4px;
}

.perm-chip.green {
  background-color: #f0fdf4;
  color: #166534;
}

.perm-chip.red {
  background-color: #fef2f2;
  color: #991b1b;
}

.perm-chip.blue {
  background-color: #eff6ff;
  color: #1e40af;
}

.encryption-notice {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.64rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.enc-svg {
  width: 14px;
  height: 14px;
  color: #0284c7;
  flex-shrink: 0;
}

.ldap-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.ldap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.ldap-header h4 {
  font-size: 0.76rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.dot-online {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #16a34a;
  box-shadow: 0 0 6px rgba(22, 163, 74, 0.4);
}

.ldap-body {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ldap-metric-circle {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 3px solid #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 800;
  color: #0284c7;
}

.ldap-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.ldap-meta strong {
  font-size: 0.74rem;
  color: #0f172a;
}

.ldap-meta span {
  font-size: 0.64rem;
  color: #64748b;
}

.tls-status {
  color: #166534 !important;
  font-weight: 700;
}

.users-table-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.table-filters-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  gap: 1rem;
}

.search-input-box {
  position: relative;
  min-width: 340px;
  display: flex;
  align-items: center;
}

.search-svg {
  position: absolute;
  left: 10px;
  width: 14px;
  height: 14px;
  color: #94a3b8;
}

.search-input {
  width: 100%;
  padding: 0.4rem 0.75rem 0.4rem 2rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.76rem;
  color: #0f172a;
  outline: none;
  background-color: #ffffff;
}

.search-input:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.role-filter-pills {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.pill {
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.pill:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}

.pill.active {
  background-color: #0f172a;
  border-color: #0f172a;
  color: #ffffff;
}

.users-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.74rem;
}

.users-table th {
  text-align: left;
  font-size: 0.66rem;
  font-weight: 700;
  color: #64748b;
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.users-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.cell-user {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.user-avatar-tag {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
}

.user-avatar-tag.medico {
  background-color: #e0f2fe;
  color: #0369a1;
}

.user-avatar-tag.enfermeria {
  background-color: #f1f5f9;
  color: #334155;
}

.user-avatar-tag.recepcion {
  background-color: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.user-avatar-tag.laboratorio {
  background-color: #f0fdf4;
  color: #166534;
}

.user-avatar-tag.administrador {
  background-color: #0f172a;
  color: #ffffff;
}

.user-ident-meta {
  display: flex;
  flex-direction: column;
}

.colab-name {
  font-size: 0.78rem;
  color: #0f172a;
}

.colab-sub {
  font-size: 0.66rem;
  color: #64748b;
}

.role-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 700;
}

.role-badge.medico {
  background-color: #e0f2fe;
  color: #0369a1;
}

.role-badge.enfermeria {
  background-color: #f1f5f9;
  color: #334155;
}

.role-badge.recepcion {
  background-color: #f8fafc;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.role-badge.laboratorio {
  background-color: #f0fdf4;
  color: #166534;
}

.role-badge.administrador {
  background-color: #0f172a;
  color: #ffffff;
}

.dept-text {
  font-size: 0.74rem;
  color: #334155;
}

.access-info {
  display: flex;
  flex-direction: column;
}

.access-info strong {
  font-size: 0.72rem;
  color: #0f172a;
}

.access-info span {
  font-size: 0.64rem;
  color: #64748b;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 700;
}

.status-chip .dot-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-chip.activo {
  background-color: #f0fdf4;
  color: #166534;
}

.status-chip.activo .dot-status {
  background-color: #16a34a;
}

.status-chip.bloqueado {
  background-color: #fef2f2;
  color: #991b1b;
}

.status-chip.bloqueado .dot-status {
  background-color: #b91c1c;
}

.status-chip.inactivo {
  background-color: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.status-chip.inactivo .dot-status {
  background-color: #94a3b8;
}

.actions-group {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-row-action {
  background: none;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-row-action:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.btn-row-action.danger:hover {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}

.row-svg {
  width: 13px;
  height: 13px;
}

.table-footer-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 1rem;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 0.7rem;
  color: #64748b;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-pg {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.25rem 0.55rem;
  border-radius: 4px;
  font-size: 0.7rem;
  cursor: pointer;
}

.btn-pg:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pg-num.active {
  font-weight: 800;
  color: #0f172a;
}
</style>
