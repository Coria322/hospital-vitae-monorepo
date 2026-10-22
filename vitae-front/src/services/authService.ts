import axios from 'axios'

export interface UsuarioAuth {
  id_usuario: string
  nombre_completo: string
  rol: 'MEDICO' | 'ENFERMERIA' | 'ADMINISTRADOR' | 'RECEPCION' | string
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

export interface RefreshResponseData {
  token: string
  token_type: string
  expires_in: number
}

export interface ApiResponse<T> {
  status: 'success' | 'error'
  message?: string
  data?: T
}

const TOKEN_KEY = 'vitae_token'
const USER_KEY = 'vitae_usuario'
const EXPIRES_AT_KEY = 'vitae_token_expires_at'
const MARGEN_REFRESH_MS = 2 * 60 * 1000 // 2 minutos antes de expiración

// Instancia independiente de Axios para peticiones de Auth (evita ciclos con interceptores de api.ts)
const authHttp = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://127.0.0.1:8000/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

let refreshTimer: number | null = null
let refreshPromise: Promise<string> | null = null
let isVisibilityListenerAttached = false

export const authService = {
  /**
   * Realiza la autenticación del personal médico o administrativo
   */
  async login(idUsuario: string, password: string): Promise<LoginResponseData> {
    const response = await authHttp.post<ApiResponse<LoginResponseData>>('/auth/login', {
      id_usuario: idUsuario.trim(),
      password: password,
    })

    if (response.data.status === 'success' && response.data.data) {
      const authData = response.data.data
      this.guardarSesion(authData)
      return authData
    }

    throw new Error(response.data.message || 'Error en la respuesta del servidor.')
  },

  /**
   * Renueva el token de acceso actual llamando al endpoint /auth/refresh
   * Retorna una promesa con el nuevo token, deduplicando llamadas simultáneas.
   */
  async refreshToken(): Promise<string> {
    // Si ya existe una petición de refresco en curso, retornar la misma promesa para evitar duplicados
    if (refreshPromise) {
      return refreshPromise
    }

    const currentToken = this.getToken()
    if (!currentToken) {
      this.limpiarSesion()
      return Promise.reject(new Error('No hay sesión activa para renovar.'))
    }

    refreshPromise = (async () => {
      try {
        const response = await authHttp.post<ApiResponse<RefreshResponseData>>(
          '/auth/refresh',
          {},
          {
            headers: {
              Authorization: `Bearer ${currentToken}`,
            },
          }
        )

        if (response.data.status === 'success' && response.data.data?.token) {
          const { token, expires_in } = response.data.data
          this.guardarToken(token, expires_in)
          return token
        }

        throw new Error(response.data.message || 'Error al renovar el token.')
      } catch (error) {
        // Si el servidor rechaza la renovación (token revocado o expirado por completo), limpiar sesión
        this.limpiarSesion()
        throw error
      } finally {
        refreshPromise = null
      }
    })()

    return refreshPromise
  },

  /**
   * Guarda los datos de la sesión tras un login exitoso
   */
  guardarSesion(data: LoginResponseData): void {
    localStorage.setItem(USER_KEY, JSON.stringify(data.usuario))
    this.guardarToken(data.token, data.expires_in)
  },

  /**
   * Actualiza el token, su tiempo de expiración y programa el auto-refresh
   */
  guardarToken(token: string, expiresInSeconds: number = 900): void {
    const expiresAt = Date.now() + expiresInSeconds * 1000
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))
    this.programarAutoRefresh(expiresInSeconds)
  },

  /**
   * Programa la renovación automática antes de que el token expire
   */
  programarAutoRefresh(expiresInSeconds?: number): void {
    this.detenerAutoRefresh()

    let msRestantes = 0
    if (expiresInSeconds !== undefined) {
      msRestantes = expiresInSeconds * 1000
    } else {
      const storedExpiresAt = localStorage.getItem(EXPIRES_AT_KEY)
      if (storedExpiresAt) {
        msRestantes = Number(storedExpiresAt) - Date.now()
      }
    }

    if (msRestantes <= 0) {
      return
    }

    // Renovar con anticipación (2 minutos antes de expirar).
    // Si quedan menos de 2 minutos, renovar a la mitad del tiempo restante (mínimo 2 segundos).
    let delay = msRestantes - MARGEN_REFRESH_MS
    if (delay <= 0) {
      delay = Math.max(2000, Math.floor(msRestantes / 2))
    }

    refreshTimer = window.setTimeout(async () => {
      try {
        await this.refreshToken()
      } catch (err) {
        console.warn('[Hospital Vitae] Falló la renovación automática del token:', err)
      }
    }, delay)
  },

  /**
   * Detiene el temporizador de auto-refresh
   */
  detenerAutoRefresh(): void {
    if (refreshTimer !== null) {
      window.clearTimeout(refreshTimer)
      refreshTimer = null
    }
  },

  /**
   * Inicializa los listeners y el temporizador al cargar la aplicación o restaurar la pestaña
   */
  initAuth(): void {
    if (!isVisibilityListenerAttached && typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible' && this.isAuthenticated()) {
          const msRestantes = this.getTiempoRestanteMs()
          // Si quedan menos de 2 minutos y sigue activo, renovar de inmediato al volver a la pestaña
          if (msRestantes > 0 && msRestantes <= MARGEN_REFRESH_MS) {
            this.refreshToken().catch(() => {})
          } else if (msRestantes <= 0) {
            this.limpiarSesion()
            if (window.location.pathname !== '/') {
              window.location.href = '/'
            }
          } else {
            // Asegurar que el temporizador siga programado correctamente tras despertar el equipo
            this.programarAutoRefresh()
          }
        }
      })
      isVisibilityListenerAttached = true
    }

    if (this.isAuthenticated()) {
      const msRestantes = this.getTiempoRestanteMs()
      if (msRestantes <= 0) {
        this.limpiarSesion()
      } else {
        this.programarAutoRefresh()
      }
    }
  },

  /**
   * Cierra la sesión en el backend y limpia el almacenamiento local
   */
  async logout(): Promise<void> {
    const token = this.getToken()
    try {
      if (token) {
        await authHttp.post(
          '/auth/logout',
          {},
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        )
      }
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
    const data = localStorage.getItem(USER_KEY)
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
    return localStorage.getItem(TOKEN_KEY)
  },

  /**
   * Determina si hay una sesión activa y vigente
   */
  isAuthenticated(): boolean {
    const token = this.getToken()
    if (!token) return false
    const msRestantes = this.getTiempoRestanteMs()
    return msRestantes > 0
  },

  /**
   * Retorna los milisegundos restantes de validez del token
   */
  getTiempoRestanteMs(): number {
    const expiresAt = localStorage.getItem(EXPIRES_AT_KEY)
    if (!expiresAt) {
      // Si hay token pero no se guardó timestamp, asumimos válido por retrocompatibilidad
      return this.getToken() ? 900000 : 0
    }
    return Number(expiresAt) - Date.now()
  },

  /**
   * Limpia credenciales locales y temporizadores
   */
  limpiarSesion(): void {
    this.detenerAutoRefresh()
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(EXPIRES_AT_KEY)
  },
}

export default authService
