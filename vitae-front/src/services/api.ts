import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError } from 'axios'

/**
 * Cliente Axios configurado para el Sistema Hospitalario Vitae
 */
const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://127.0.0.1:8000/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
})

// Interceptor de Petición: Inyecta el Token Bearer de Sanctum automáticamente
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = localStorage.getItem('vitae_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Interceptor de Respuesta: Maneja expiración de sesión (401) globalmente
api.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    return response
  },
  (error: AxiosError) => {
    if (error.response && error.response.status === 401) {
      // Si la sesión expiró o el token es inválido y no estamos en la página de login
      if (window.location.pathname !== '/') {
        localStorage.removeItem('vitae_token')
        localStorage.removeItem('vitae_usuario')
        window.location.href = '/'
      }
    }
    return Promise.reject(error)
  }
)

export default api
