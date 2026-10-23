export interface Paciente {
  id: string
  nombre: string
  folio: string
  ece: string
  edad: number
  sexo: string
  box: string
  motivo: string
  triage: string
  triageColor: string
  limiteSla: string
  transcurridosMin: number
  restantesMin: number
  signos?: {
    pa: string
    fc: string
    temp: string
    spo2: string
    horaToma: string
  }
  antecedentes?: {
    motivo: string
    alergias: string
    cronicos: string
    medicacion: string
  }
  ultimaNota?: {
    titulo: string
    medico: string
    folio: string
    fecha: string
    contenido: string
  }
}

export interface Doctor {
  nombre: string
  cedula: string
  box: string
}

export interface Receta {
  medicamento: string
  dosis: string
  via: string
  frecuencia: string
  duracion: string
  indicaciones: string
}
