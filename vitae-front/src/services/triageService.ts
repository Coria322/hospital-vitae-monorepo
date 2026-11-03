import api from './api'

export interface TriageRegistroPayload {
  id_paciente: string
  presion_art: string
  frecuencia_card: number
  frecuencia_resp?: number
  temperatura?: number
  spo2?: number
  nivel_glucosa?: number | null
  escala_eva: number
  nivel_conciencia: 'ALERTA' | 'VERBAL' | 'DOLOR' | 'INCONSCIENTE'
  estimacion_sangre_perdida?: string
  captura_retroactiva?: boolean
  fecha_hora_ingreso?: string
}

export interface PacienteUrgenciaCola {
  id_triage: string
  folio_ingreso: string
  id_paciente: string
  paciente_nombre: string
  identificado: boolean
  color_asig: 'ROJO' | 'NARANJA' | 'AMARILLO' | 'VERDE' | 'AZUL'
  prioridad_nivel: number
  fecha_hora_ingreso: string
  minutos_espera: number
  sla_vencimiento_en: string
  minutos_restantes_sla: number
  sla_vencido: boolean
  desviacion_minutos: number
  estado_atencion: 'EN_ESPERA' | 'EN_ATENCION' | 'ATENDIDO'
  id_sala: string | null
  sala_nombre?: string
  signos: {
    presion_art: string
    frecuencia_card: number
    frecuencia_resp?: number
    temperatura?: number
    spo2?: number
    nivel_glucosa?: number
    escala_eva: number
    nivel_conciencia: string
  }
}

export interface SalaEsperaPublicaItem {
  posicion: number
  folio: string
  iniciales: string
  color: string
  icono: string
  estado_clinico: string
  estado_atencion: string
  sala: string
  sla_vencido: boolean
}

export const triageService = {
  /**
   * Registra un nuevo triage con cálculo Manchester (RF-004, RF-005)
   */
  async registrarTriage(datos: TriageRegistroPayload) {
    const response = await api.post('/triages', datos)
    return response.data
  },

  /**
   * Consulta la cola viva de urgencias ponderada por prioridad y SLA (RF-006)
   */
  async obtenerCola(): Promise<PacienteUrgenciaCola[]> {
    const response = await api.get('/triages/cola')
    return response.data.data
  },

  /**
   * Reclasificación médica manual con justificación obligatoria (RF-007)
   */
  async reclasificar(idTriage: string, nuevoColor: string, motivo: string) {
    const response = await api.post(`/triages/${idTriage}/reclasificaciones`, {
      nuevo_color: nuevoColor,
      motivo,
    })
    return response.data
  },

  /**
   * Libera una sala/consultorio y llama al siguiente paciente (RF-006)
   */
  async liberarSala(idSala: string) {
    const response = await api.post(`/salas/${idSala}/liberar`)
    return response.data
  },

  /**
   * Consulta la lista de salas y consultorios con disponibilidad
   */
  async obtenerSalas() {
    const response = await api.get('/salas')
    return response.data.data
  },

  /**
   * Consulta la pantalla pública de sala de espera disociada (sin PII, PROV-UX-002)
   */
  async obtenerSalaEsperaPublica(): Promise<{
    total_en_espera: number
    total_en_atencion: number
    pacientes: SalaEsperaPublicaItem[]
    ultima_actualizacion_utc: string
  }> {
    const response = await api.get('/sala-espera')
    return response.data.data
  },

  /**
   * Conecta con el canal SSE en tiempo real para actualización viva
   */
  conectarStream(onActualizacion: (datos: any) => void, onError?: (err: any) => void): () => void {
    const baseUrl = import.meta.env.VITE_API_URL || 'http://127.0.0.1:8000/api/v1'
    const eventSource = new EventSource(`${baseUrl}/triage/stream`)

    eventSource.addEventListener('sala_actualizada', (e) => {
      try {
        const datos = JSON.parse(e.data)
        onActualizacion(datos)
      } catch (err) {
        console.error('Error parseando SSE triage:', err)
      }
    })

    eventSource.onerror = (err) => {
      if (onError) onError(err)
    }

    return () => {
      eventSource.close()
    }
  },
}

export default triageService
