import api from './api'

export interface UsuarioBackend {
  id_usuario: string
  nombre_completo: string
  rol: 'ADMINISTRADOR' | 'MEDICO' | 'ENFERMERIA' | 'RECEPCION' | 'LABORATORISTA' | 'AUDITOR'
  cedula?: string | null
  estado_cuenta: 'ACTIVA' | 'INACTIVA' | 'BLOQUEADA'
  intentos_fallidos: number
  ultimo_acceso_en?: string | null
  created_at?: string
}

export interface CrearUsuarioPayload {
  id_usuario?: string
  nombre_completo: string
  rol: string
  password: string
  cedula?: string | null
}

export interface AuditoriaBackendItem {
  id_auditoria: number
  id_usuario: string
  fecha_hora: string
  direccion_ip?: string
  modulo?: string
  tipo_movimiento: string
  tabla_afectada: string
  registro_id?: string
  valor_anterior?: any
  valor_nuevo?: any
  resultado: string
  motivo?: string
  hash_sha256?: string
  hash_anterior?: string
}

export interface FiltrosAuditoria {
  desde?: string
  hasta?: string
  id_usuario?: string
  tabla_afectada?: string
  tipo_movimiento?: string
  resultado?: string
  per_page?: number
  page?: number
}

export interface SaludSistemaResponse {
  status: 'UP' | 'DEGRADED'
  database: string
  storage: string
  service: string
  timestamp_utc: string
  alta_disponibilidad: {
    aislamiento_fallos: boolean
    rto_objetivo_segundos: number
  }
}

export const adminService = {
  /**
   * Consulta la lista de usuarios paginada desde el backend
   */
  async listarUsuarios(page = 1, perPage = 100) {
    const response = await api.get(`/usuarios?page=${page}&per_page=${perPage}`)
    return response.data.data
  },

  /**
   * Consulta el próximo ID de usuario secuencial calculado por el sistema
   */
  async obtenerProximoId(rol: string): Promise<string> {
    try {
      const response = await api.get(`/usuarios/proximo-id?rol=${encodeURIComponent(rol)}`)
      return response.data?.data?.proximo_id || ''
    } catch {
      return ''
    }
  },

  /**
   * Registra un nuevo usuario con ID corto y cédula (NOM-024)
   */
  async crearUsuario(payload: CrearUsuarioPayload) {
    const response = await api.post('/usuarios', payload)
    return response.data.data
  },

  /**
   * Actualiza datos de un usuario existente
   */
  async actualizarUsuario(idUsuario: string, payload: {
    nombre_completo?: string
    rol?: string
    cedula?: string | null
    estado_cuenta?: string
    password?: string
  }) {
    const response = await api.put(`/usuarios/${idUsuario}`, payload)
    return response.data.data
  },

  /**
   * Ejecuta la baja lógica del usuario (soft delete, prohibido DELETE físico)
   */
  async bajaUsuario(idUsuario: string) {
    const response = await api.post(`/usuarios/${idUsuario}/baja`)
    return response.data
  },

  /**
   * Elimina / Da de baja permanentemente un usuario vía DELETE
   */
  async eliminarUsuario(idUsuario: string) {
    const response = await api.delete(`/usuarios/${idUsuario}`)
    return response.data
  },

  /**
   * Bloquea preventivamente una cuenta de usuario (estado BLOQUEADA, intentos_fallidos 3)
   */
  async bloquearUsuario(idUsuario: string) {
    const response = await api.post(`/usuarios/${idUsuario}/bloquear`)
    return response.data
  },

  /**
   * Reactiva una cuenta inactiva o desbloquea una cuenta con intentos fallidos
   */
  async reactivarUsuario(idUsuario: string) {
    const response = await api.post(`/usuarios/${idUsuario}/reactivar`)
    return response.data
  },

  /**
   * Resetea la contraseña de un usuario generando una credencial provisional
   */
  async resetPassword(idUsuario: string, password?: string) {
    const response = await api.post(`/usuarios/${idUsuario}/reset-password`, { password })
    return response.data
  },

  /**
   * Verifica la integridad matemática de la cadena criptográfica SHA-256 (NOM-024)
   */
  async verificarCadena() {
    const response = await api.get('/auditoria/verificar-cadena')
    return response.data
  },

  /**
   * Consulta la bitácora inalterable AUDITORIA_I con filtros forenses
   */
  async obtenerBitacora(filtros: FiltrosAuditoria = {}) {
    const response = await api.get('/auditoria', { params: filtros })
    return response.data.data
  },

  /**
   * Consulta el estado de salud y alta disponibilidad del clúster
   */
  async obtenerSalud(): Promise<SaludSistemaResponse> {
    const response = await api.get('/salud')
    return response.data
  },
}

export default adminService
