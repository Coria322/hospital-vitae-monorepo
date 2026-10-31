export type PrioridadEstudio = 'STAT' | 'URGENTE' | 'ORDINARIA'
export type CodigoColor = 'ROJO' | 'NARANJA' | 'GRIS'
export type CategoriaEstudio = 'LABORATORIO' | 'IMAGENOLOGIA' | 'GABINETE'
export type EstadoEstudio = 'PENDIENTE' | 'EN_PROCESO' | 'VALIDADO' | 'RECHAZADO'

export interface LabUser {
  nombre: string
  cedula: string
  rol: string
  turno: string
  area: string
}

export interface DeterminacionAnalitica {
  codigo: string
  nombre: string
  muestra_requerida: string
  valor_referencia: string
  valor_obtenido?: string
  alerta_critica?: boolean
  estado: string
}

export interface FaseCustodia {
  paso: number
  titulo: string
  hora: string
  responsable: string
  completado: boolean
  activo?: boolean
}

export interface ArchivoResultado {
  nombre: string
  formato: 'PDF' | 'DICOM' | 'IMG'
  tamano: string
  sha256: string
  fecha_firma?: string
  firmado_por?: string
  ack_hl7?: string
}

export interface EstudioDiagnostico {
  id: string
  folio: string
  codigo_tubo?: string
  paciente_nombre: string
  paciente_iniciales: string
  paciente_edad: number
  paciente_sexo: 'M' | 'F'
  prioridad: PrioridadEstudio
  codigo_color: CodigoColor
  estado: EstadoEstudio
  categoria: CategoriaEstudio
  estudio_requerido: string
  servicio_solicitante: string
  medico_prescriptor: string
  sla_restante_min: number
  hora_solicitud: string
  analizador_asignado?: string
  determinaciones?: DeterminacionAnalitica[]
  trazabilidad?: FaseCustodia[]
  archivo_resultado?: ArchivoResultado
  observaciones_tecnicas?: string
}

export interface LabKPIs {
  validados_hoy: number
  sla_promedio_min: number
  cumplimiento_pct: number
  tasa_rechazo_pct: number
}

export interface IntercomExtension {
  servicio: string
  extension: string
  medicos: string
}
