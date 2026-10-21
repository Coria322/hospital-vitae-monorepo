import api from './api'

export interface UsuarioAuth {
  id_usuario: string
  nombre_completo: string
  rol: 'MEDICO' | 'ENFERMERIA' | 'ADMINISTRADOR' | string
  cedula: string | null
  estado_cuenta: string
  debe_cambiar_password?: boolean
}

export interface LoginResponseData {
  token: string
  token_type: string
  expires_in: number
  usuario: UsuarioAuth
}

export interface ApiResponse<T> {
  status: 'success' | 'error'
  message?: string
  data?: T
}

export const authService = {
  /**
   * Realiza la autenticación del personal médico o administrativo
   */
  async login(idUsuario: string, password: string): Promise<LoginResponseData> {
    const response = await api.post<ApiResponse<LoginResponseData>>('/auth/login', {
      id_usuario: idUsuario.trim(),
      password: password,
    })

    if (response.data.status === 'success' && response.data.data) {
      const authData = response.data.data
      localStorage.setItem('vitae_token', authData.token)
      localStorage.setItem('vitae_usuario', JSON.stringify(authData.usuario))
      return authData
    }

    throw new Error(response.data.message || 'Error en la respuesta del servidor.')
  },

  /**
   * Cierra la sesión en el backend y limpia el almacenamiento local
   */
  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout')
    } catch {
      // Si falla la petición de logout igual limpiamos el cliente
    } finally {
      this.limpiarSesion()
    }
  },

  /**
   * Obtiene los datos del usuario autenticado guardados localmente
   */
  getUsuario(): UsuarioAuth | null {
    const data = localStorage.getItem('vitae_usuario')
    if (!data) return null
    try {
      return JSON.parse(data) as UsuarioAuth
    } catch {
      return null
    }
  },

  /**
   * Obtiene el token JWT / Sanctum guardado
   */
  getToken(): string | null {
    return localStorage.getItem('vitae_token')
  },

  /**
   * Determina si hay una sesión activa
   */
  isAuthenticated(): boolean {
    return !!this.getToken()
  },

  /**
   * Limpia credenciales locales
   */
  limpiarSesion(): void {
    localStorage.removeItem('vitae_token')
    localStorage.removeItem('vitae_usuario')
  },
}

export default authService
