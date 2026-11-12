import api from '@/services/api'

export interface CitaMedica {
  id: string
  folio: string
  idPaciente?: string
  idExpediente?: string
  pacienteNombre: string
  pacienteCurp: string
  pacienteEdad?: number
  pacienteSexo?: string
  idMedico?: string
  medicoNombre: string
  medicoCedula?: string
  especialidad: string
  consultorio: string
  dia: string
  hora: string
  tipoIntervencion: 'primera' | 'control' | 'preoperatoria'
  estado: 'PROGRAMADA' | 'EN_SALA' | 'EN_ATENCION' | 'ATENDIDA' | 'CANCELADA'
  fechaRegistro: string
  signos?: {
    pa: string
    fc: string
    temp: string
    spo2: string
    horaToma: string
  }
}

const STORAGE_KEY_CITAS = 'vitae_citas_programadas_v3'

class CitasService {
  obtenerCitas(): CitaMedica[] {
    try {
      const data = localStorage.getItem(STORAGE_KEY_CITAS)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  guardarCitas(citas: CitaMedica[]): void {
    try {
      localStorage.setItem(STORAGE_KEY_CITAS, JSON.stringify(citas))
      window.dispatchEvent(new CustomEvent('vitae-citas-actualizadas', { detail: citas }))
    } catch (e) {
      console.warn('Error al persistir citas:', e)
    }
  }

  agendarCita(cita: Omit<CitaMedica, 'id' | 'folio' | 'fechaRegistro'>): CitaMedica {
    const citas = this.obtenerCitas()
    const nueva: CitaMedica = {
      ...cita,
      id: 'CITA-' + Date.now().toString(36).toUpperCase(),
      folio: 'VTE-' + Math.floor(1000 + Math.random() * 9000),
      fechaRegistro: new Date().toISOString()
    }
    citas.unshift(nueva)
    this.guardarCitas(citas)

    // Sincronización asíncrona con el backend relacional
    const esIsoDia = /^\d{4}-\d{2}-\d{2}$/.test(cita.dia || '')
    const diaIso = esIsoDia ? cita.dia : new Date().toISOString().substring(0, 10)
    const horaNormalizada = cita.hora && /^\d{2}:\d{2}/.test(cita.hora) ? cita.hora.substring(0, 5) : '10:00'
    let fechaHoraPayload = `${diaIso} ${horaNormalizada}:00`
    try {
      const fechaLocal = new Date(`${diaIso}T${horaNormalizada}:00`)
      if (!isNaN(fechaLocal.getTime())) {
        fechaHoraPayload = fechaLocal.toISOString()
      }
    } catch {
      // Conservar formato estándar si falla conversión
    }
    const idMedicoValido = cita.idMedico && cita.idMedico.startsWith('MED-') ? cita.idMedico : 'MED-001'

    api.post('/citas', {
      id_paciente: cita.idPaciente || cita.pacienteCurp,
      id_usuario: idMedicoValido,
      fecha_hora: fechaHoraPayload,
      especialidad: cita.especialidad || 'Medicina General',
      motivo: `Consulta ${cita.tipoIntervencion}`,
      consultorio: cita.consultorio || 'Cons. 1'
    }).catch(e => {
      console.warn('Cita almacenada localmente; fallo de réplica en API:', e)
    })

    return nueva
  }

  async sincronizarConBackend(): Promise<CitaMedica[]> {
    try {
      const res = await api.get('/citas')
      const remoteList = res.data?.data?.data || res.data?.data || []
      if (Array.isArray(remoteList) && remoteList.length > 0) {
        const local = this.obtenerCitas()
        for (const r of remoteList) {
          const yaExiste = local.some(c => c.id === r.id_cita || c.id === r.id)
          if (!yaExiste) {
            local.push({
              id: r.id_cita || r.id,
              folio: r.folio || ('VTE-' + (r.id_cita ? r.id_cita.substring(0, 4) : '0000')),
              idPaciente: r.id_paciente,
              pacienteNombre: r.paciente?.nombre_completo || 'Paciente Cita Externa',
              pacienteCurp: r.paciente?.curp || 'S/N',
              idMedico: r.id_usuario,
              medicoNombre: r.medico?.nombre_completo || 'Médico Asignado',
              especialidad: r.especialidad || 'Medicina General',
              consultorio: r.consultorio || 'Cons. 1',
              dia: r.fecha_hora ? r.fecha_hora.substring(0, 10) : new Date().toISOString().substring(0, 10),
              hora: r.fecha_hora ? r.fecha_hora.substring(11, 16) : '10:00',
              tipoIntervencion: 'primera',
              estado: r.estado === 'PENDIENTE' ? 'PROGRAMADA' : (r.estado as any || 'PROGRAMADA'),
              fechaRegistro: r.created_at || new Date().toISOString()
            })
          }
        }
        this.guardarCitas(local)
        return local
      }
    } catch {
      // Si el backend no responde, se conserva el estado local sin interrupciones
    }
    return this.obtenerCitas()
  }

  actualizarEstadoCita(id: string, nuevoEstado: CitaMedica['estado']): void {
    const citas = this.obtenerCitas()
    const cita = citas.find(c => c.id === id || c.folio === id)
    if (cita) {
      cita.estado = nuevoEstado
      this.guardarCitas(citas)
    }
  }

  actualizarExpedienteCita(id: string, idPaciente?: string, idExpediente?: string): void {
    const citas = this.obtenerCitas()
    const cita = citas.find(c => c.id === id || c.folio === id)
    if (cita) {
      if (idPaciente) cita.idPaciente = idPaciente
      if (idExpediente) cita.idExpediente = idExpediente
      this.guardarCitas(citas)
    }
  }

  obtenerCitasParaMedico(medicoId?: string, nombreMedico?: string, boxConsultorio?: string): CitaMedica[] {
    const citas = this.obtenerCitas()
    const clean = (s: string) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim()
    const mId = medicoId ? clean(medicoId) : ''
    const mNom = nombreMedico ? clean(nombreMedico) : ''
    const bCons = boxConsultorio ? clean(boxConsultorio) : ''

    return citas.filter(c => {
      // No mostrar citas que ya fueron concluidas o canceladas
      if (c.estado === 'ATENDIDA' || c.estado === 'CANCELADA') return false

      // Si no hay médico especificado, mostrar todas las citas activas
      if (!mId && !mNom && !bCons) return true

      if (mId && c.idMedico && clean(c.idMedico) === mId) return true
      if (mNom && c.medicoNombre) {
        const cNom = clean(c.medicoNombre)
        if (cNom.includes(mNom) || mNom.includes(cNom)) return true
        const palabras = mNom.split(/\s+/).filter(p => p.length > 3)
        if (palabras.some(p => cNom.includes(p))) return true
      }
      if (bCons && c.consultorio) {
        const cCons = clean(c.consultorio)
        if (cCons.includes(bCons) || bCons.includes(cCons)) return true
      }

      // Si sólo hay un médico o para pruebas locales, permitir visibilidad en sala de consulta externa
      return true
    })
  }

  eliminarCita(id: string): void {
    const citas = this.obtenerCitas().filter(c => c.id !== id && c.folio !== id)
    this.guardarCitas(citas)
  }
}

export const citasService = new CitasService()
export default citasService
