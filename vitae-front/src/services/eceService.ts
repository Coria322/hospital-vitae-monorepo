import api from './api'

export interface AntecedenteClinicoPayload {
  tipo: 'HEREDOFAMILIAR' | 'PATOLOGICO' | 'NO_PATOLOGICO' | 'GINECOOBSTETRICO' | 'ALERGIA'
  descripcion: string
  fecha_diagnostico?: string
  observaciones?: string
}

export interface NotaMedicaPayload {
  tipo: 'EVOLUCION' | 'INGRESO' | 'EGRESO' | 'INTERCONSULTA'
  contenido: string
  diagnostico_cie?: string
  plan_tratamiento?: string
}

export interface NotaAclaratoriaPayload {
  contenido: string
  justificacion: string
}

export interface RecetaPayload {
  id_expediente: string
  fraccion: 'I' | 'II' | 'III' | 'NINGUNA'
  medicamentos: string
  indicaciones: string
}

export interface FirmaRecetaPayload {
  firma_electronica: string
  certificado?: string
}

export interface SolicitudEstudioPayload {
  id_expediente: string
  tipo_estudio: string
  prioridad?: 'NORMAL' | 'URGENTE'
  indicaciones_clinicas?: string
}

export interface BreakTheGlassPayload {
  id_paciente: string
  motivo: string
  duracion_minutos?: number
}

export interface ExpedienteDetalle {
  id_expediente: string
  id_paciente: string
  id_usuario: string
  motivo_consulta: string
  diagnostico?: string
  estudios_previos?: string
  fecha_creacion: string
  fecha_ultimo_acto_medico: string
  estado_activo: boolean
  bloqueo_legal: boolean
  paciente?: {
    id_paciente: string
    nombre_completo: string
    curp?: string
    sexo?: string
    fecha_nacimiento?: string
    identificado: boolean
    folio_urgencias?: string
  }
  antecedentes?: Array<{
    id_antecedente: string
    tipo: string
    descripcion: string
    fecha_diagnostico?: string
    observaciones?: string
  }>
  notas_medicas?: Array<{
    id_nota: string
    id_usuario: string
    tipo: string
    contenido: string
    diagnostico_cie?: string
    plan_tratamiento?: string
    firma_digital: string
    hash_sha256: string
    fecha_hora_creacion: string
    cerrada: boolean
    es_aclaratoria: boolean
    id_nota_original?: string
    nota_aclaratoria?: {
      id_nota: string
      contenido: string
      justificacion: string
      fecha_hora_creacion: string
    }
  }>
  recetas?: Array<{
    id_receta: string
    folio: string
    fraccion: string
    medicamentos: string
    indicaciones: string
    firmada: boolean
    surtida: boolean
    codigo_qr?: string
    hash_sha256?: string
    fecha_emision: string
    fecha_vencimiento: string
  }>
  solicitudes_estudio?: Array<{
    id_solicitud: string
    tipo_estudio: string
    prioridad: string
    indicaciones_clinicas?: string
    estado: string
    fecha_hora_solicitud: string
    resultado?: {
      id_resultado: string
      datos_cifrados?: string
      interpretacion_medica?: string
      fecha_hora_resultado: string
    }
  }>
}

export const eceService = {
  /**
   * Consulta los datos de un paciente incluyendo su expediente vinculado si existe
   */
  async consultarPaciente(idPaciente: string) {
    const response = await api.get(`/pacientes/${idPaciente}`)
    return response.data.data
  },

  /**
   * Apertura formal de expediente clínico
   */
  async crearExpediente(payload: {
    id_paciente: string
    motivo_consulta: string
    diagnostico?: string
    estudios_previos?: string
  }) {
    const response = await api.post('/expedientes-clinicos', payload)
    return response.data.data
  },

  /**
   * Consulta integral de expediente clínico (notas, antecedentes, recetas y estudios)
   */
  async obtenerExpediente(idExpediente: string): Promise<ExpedienteDetalle> {
    const response = await api.get(`/expedientes-clinicos/${idExpediente}`)
    return response.data.data
  },

  /**
   * Registra antecedentes clínicos estructurados
   */
  async registrarAntecedente(idExpediente: string, payload: AntecedenteClinicoPayload) {
    const response = await api.post(`/expedientes-clinicos/${idExpediente}/antecedentes`, payload)
    return response.data.data
  },

  /**
   * Emite una nota médica de evolución/ingreso inalterable (SOAPIE)
   */
  async guardarNotaMedica(idExpediente: string, payload: NotaMedicaPayload) {
    const response = await api.post(`/expedientes-clinicos/${idExpediente}/notas`, payload)
    return response.data.data
  },

  /**
   * Registra una nota aclaratoria vinculada a una nota médica cerrada
   */
  async guardarNotaAclaratoria(idExpediente: string, idNota: string, payload: NotaAclaratoriaPayload) {
    const response = await api.post(`/expedientes-clinicos/${idExpediente}/notas/${idNota}/aclaraciones`, payload)
    return response.data.data
  },

  /**
   * Emite una receta médica (ordinaria o controlada Fracción I, II, III)
   */
  async emitirReceta(payload: RecetaPayload) {
    const response = await api.post('/recetas', payload)
    return response.data.data
  },

  /**
   * Firma digital con e.firma y activa el código QR único de un solo uso
   */
  async firmarReceta(idReceta: string, payload: FirmaRecetaPayload) {
    const response = await api.post(`/recetas/${idReceta}/firma`, payload)
    return response.data.data
  },

  /**
   * Consulta el detalle y estado de una receta médica
   */
  async obtenerReceta(idReceta: string) {
    const response = await api.get(`/recetas/${idReceta}`)
    return response.data.data
  },

  /**
   * Surtir receta en farmacia hospitalaria y consumir QR
   */
  async surtirReceta(folio: string) {
    const response = await api.post(`/recetas/${folio}/surtir`)
    return response.data.data
  },

  /**
   * Solicita estudios de laboratorio o imagenología médica
   */
  async solicitarEstudio(payload: SolicitudEstudioPayload) {
    const response = await api.post('/solicitudes-estudio', payload)
    return response.data.data
  },

  /**
   * Lista solicitudes de estudios para laboratorio
   */
  async listarSolicitudesEstudio() {
    const response = await api.get('/solicitudes-estudio')
    return response.data.data
  },

  /**
   * Activa el protocolo de emergencia Break-the-Glass para obtener pase de 120 minutos
   */
  async activarBreakTheGlass(payload: BreakTheGlassPayload) {
    const response = await api.post('/accesos-emergencia', payload)
    return response.data.data
  },

  /**
   * Exporta expediente clínico en PDF auditado con marca de agua disociada
   */
  async exportarExpedientePdf(idExpediente: string, motivoExportacion = 'Consulta médica asistencial') {
    const response = await api.post(`/expedientes-clinicos/${idExpediente}/exportaciones`, {
      motivo_exportacion: motivoExportacion,
    })
    return response.data.data
  },

  /**
   * Búsqueda en el padrón central de pacientes para verificación de identidad (CURP, Nombre o Folio)
   */
  async buscarPadronCentral(termino: string) {
    const response = await api.get('/pacientes', {
      params: { q: termino }
    })
    return response.data.data?.data || response.data.data || []
  },

  /**
   * Fusión autorizada de paciente temporal/NN con paciente definitivo (PROV-RN-007)
   */
  async fusionarPacientes(idTemporal: string, idDefinitivo: string, motivo: string) {
    const response = await api.post(`/pacientes/${idTemporal}/fusiones`, {
      id_paciente_definitivo: idDefinitivo,
      motivo
    })
    return response.data
  },
}

export default eceService
