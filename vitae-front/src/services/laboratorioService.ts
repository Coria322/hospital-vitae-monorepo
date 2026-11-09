import api from './api'

export interface SolicitudEstudioBackend {
  id_solicitud: string
  id_expediente: string
  id_usuario: string
  tipo_estudio: string
  prioridad: 'NORMAL' | 'URGENTE' | 'STAT'
  indicaciones_clinicas?: string | null
  estado: 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADA' | 'RECHAZADA'
  fecha_hora_solicitud: string
  expediente?: {
    id_expediente: string
    id_paciente: string
    paciente?: {
      id_paciente: string
      nombre_completo: string
      sexo?: 'M' | 'F' | 'O'
      fecha_nacimiento?: string
      curp?: string
    }
  }
  medico_solicitante?: {
    id_usuario: string
    nombre_completo: string
    cedula_profesional?: string
    especialidad?: string
  }
  resultado?: {
    id_resultado: string
    id_solicitud: string
    mime: string
    cargado_por: string
    observaciones?: string | null
    hash_sha256: string
    fecha_hora: string
  }
}

export interface KpisLaboratorio {
  pendientes_total: number
  stat_urgentes: number
  validados_hoy: number
  sla_promedio_min: number
  cumplimiento_pct: number
  tasa_rechazo_pct: number
}

export interface CargarResultadoPayload {
  archivo?: File
  contenido_texto?: string
  observaciones?: string
  mime?: string
}

export interface ResultadoDetalleResponse {
  resultado: {
    id_resultado: string
    id_solicitud: string
    mime: string
    cargado_por: string
    observaciones?: string | null
    hash_sha256: string
    fecha_hora: string
  }
  contenido_descifrado: string | null
  integridad_sha256: string
}

export const laboratorioService = {
  /**
   * Obtiene la bandeja de solicitudes de laboratorio e imagenologia
   */
  async obtenerSolicitudes(filtros?: {
    estado?: string
    prioridad?: string
    id_expediente?: string
    per_page?: number
  }) {
    const response = await api.get('/solicitudes-estudio', { params: filtros })
    return response.data.data
  },

  /**
   * Marca una solicitud de estudio en procesamiento tecnico
   */
  async marcarEnProceso(id: string) {
    const response = await api.post(`/solicitudes-estudio/${id}/procesar`)
    return response.data.data
  },

  /**
   * Registra rechazo tecnico de muestra biologica (NOM-007)
   */
  async rechazarMuestra(id: string, motivo: string) {
    const response = await api.post(`/solicitudes-estudio/${id}/rechazo`, { motivo })
    return response.data.data
  },

  /**
   * Carga resultado con cifrado AES-256-GCM y sellado SHA-256
   */
  async cargarResultado(id: string, payload: CargarResultadoPayload) {
    if (payload.archivo) {
      const formData = new FormData()
      formData.append('archivo', payload.archivo)
      if (payload.observaciones) {
        formData.append('observaciones', payload.observaciones)
      }
      if (payload.mime) {
        formData.append('mime', payload.mime)
      }
      const response = await api.post(`/solicitudes-estudio/${id}/resultados`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      return response.data.data
    }

    const response = await api.post(`/solicitudes-estudio/${id}/resultados`, {
      contenido_texto: payload.contenido_texto,
      observaciones: payload.observaciones,
      mime: payload.mime || 'text/plain',
    })
    return response.data.data
  },

  /**
   * Obtiene y descifra el resultado criptografico de un estudio
   */
  async obtenerResultado(id: string): Promise<ResultadoDetalleResponse> {
    const response = await api.get(`/solicitudes-estudio/${id}/resultados`)
    return response.data.data
  },

  /**
   * Obtiene los KPIs operativos del turno del laboratorio
   */
  async obtenerKpis(): Promise<KpisLaboratorio> {
    const response = await api.get('/solicitudes-estudio/kpis')
    return response.data.data
  },
}

export default laboratorioService
