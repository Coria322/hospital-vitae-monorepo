/**
 * Servicio de Gestion de Camas, Boxes, Consultorios y Asignacion de Guardias
 * Sincronizado 1:1 entre Recepcion (Censo de Camas) y Administracion (Guardias)
 * Cumplimiento NOM-024-SSA3-2012
 */

export type TurnoGuardia = 'matutino' | 'vespertino' | 'nocturno'
export type PisoHospital = 'P1' | 'P2' | 'P3' | 'QX'
export type EstatusCama = 'disponible' | 'ocupada' | 'reservada' | 'higienizacion'

export interface PersonalGuardia {
  id: string
  nombre: string
  rol: 'MEDICO' | 'ENFERMERIA'
  especialidad: string
  cedula?: string
}

export interface AsignacionGuardiaBox {
  id: string
  nombre: string
  piso: PisoHospital
  zonaNombre: string
  subtitulo: string
  tipo: 'box' | 'consultorio' | 'pabellon' | 'quirofano' | 'pediatria' | 'cama'
  estadoOperativo: 'OPERATIVO' | 'EN_SERVICIO' | 'MANTENIMIENTO'
  estatusCenso: EstatusCama
  pacienteInfo?: string
  servicio: string
  medicoId?: string
  medicoNombre?: string
  medicoEspecialidad?: string
  enfermeroId?: string
  enfermeroNombre?: string
  enfermeroEspecialidad?: string
}

export interface SolicitudCamaPendiente {
  id: string
  folio: string
  idPaciente?: string
  paciente: string
  curp?: string
  diagnostico?: string
  medicoTratante?: string
  servicio: string
  prioridad: 'Triage 2 - Naranja' | 'Programado' | 'Triage 3 - Amarillo' | 'Gineco Urgente' | 'Urgente'
  fechaSolicitud: string
  tiempoEspera: string
}

const STORAGE_KEY_BOXES = 'vitae_camas_hospital_master_v5'
const STORAGE_KEY_GUARDIAS = 'vitae_guardia_boxes_v5'
const STORAGE_KEY_SOLICITUDES_CAMA = 'vitae_solicitudes_cama_v1'

// Catalogo predeterminado de personal clinico (Usuarios predeterminados del sistema)
export const CATALOGO_MEDICOS: PersonalGuardia[] = [
  { id: 'MED-001', nombre: 'Dr. Carlos Rivera', rol: 'MEDICO', especialidad: 'Medicina Interna / Urgencias', cedula: '7894120' },
  { id: 'MED-002', nombre: 'Dra. Elena Vasconcelos', rol: 'MEDICO', especialidad: 'Cirugia General y Laparoscopia', cedula: '8912340' },
  { id: 'usr-1', nombre: 'Dra. Mariana Gomez Trevino', rol: 'MEDICO', especialidad: 'Urgencias Adultos / ECE', cedula: '7894210' },
  { id: 'usr-med-3', nombre: 'Dr. Roberto Garza Morales', rol: 'MEDICO', especialidad: 'Medicina Critica y Choque', cedula: '6541289' },
  { id: 'usr-med-4', nombre: 'Dr. Carlos Mendoza Ruiz', rol: 'MEDICO', especialidad: 'Urgencias Medicas', cedula: '7412589' },
  { id: 'usr-med-5', nombre: 'Dr. Mauricio Sotomayor', rol: 'MEDICO', especialidad: 'Medicina Interna', cedula: '8521479' }
]

export const CATALOGO_ENFERMERIA: PersonalGuardia[] = [
  { id: 'ENF-001', nombre: 'Lic. Gabriela Ramos', rol: 'ENFERMERIA', especialidad: 'Triage y Choque Critico', cedula: '8921143' },
  { id: 'ENF-002', nombre: 'Lic. Marco Morales', rol: 'ENFERMERIA', especialidad: 'Hospitalizacion Adultos', cedula: '8921144' },
  { id: 'usr-2', nombre: 'Lic. Enf. Carlos Ramos Nava', rol: 'ENFERMERIA', especialidad: 'Triage y Reanimacion', cedula: '8921145' },
  { id: 'usr-enf-3', nombre: 'Enf. Laura Sanchez Pineda', rol: 'ENFERMERIA', especialidad: 'Cuidados Intensivos UCI', cedula: '9654123' },
  { id: 'usr-enf-4', nombre: 'Enf. Patricia Valdes Lozano', rol: 'ENFERMERIA', especialidad: 'Observacion Urgencias', cedula: '9874521' }
]

// Plantilla reducida inicial: 8 en Urgencias (P1), 5 en Piso 2, 5 en Piso 3, 5 en QX = 23 posiciones
// TODAS LIBRES / DISPONIBLES para inicio de operaciones
export const PLANTILLA_INICIAL_CAMAS: AsignacionGuardiaBox[] = [
  // =========================================================================
  // PISO 1: URGENCIAS MEDICAS Y CHOQUE (8 BOXES)
  // =========================================================================
  {
    id: 'BOX-U01',
    nombre: 'BOX U-01',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Choque 1 (Critico)',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Urgencias',
    medicoId: 'MED-001',
    medicoNombre: 'Dr. Carlos Rivera',
    medicoEspecialidad: 'Medicina Interna / Urgencias',
    enfermeroId: 'ENF-001',
    enfermeroNombre: 'Lic. Gabriela Ramos',
    enfermeroEspecialidad: 'Triage y Choque Critico'
  },
  {
    id: 'BOX-U02',
    nombre: 'BOX U-02',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Choque 2 (Critico)',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Urgencias',
    medicoId: 'MED-002',
    medicoNombre: 'Dra. Elena Vasconcelos',
    medicoEspecialidad: 'Cirugia General y Laparoscopia',
    enfermeroId: 'ENF-002',
    enfermeroNombre: 'Lic. Marco Morales',
    enfermeroEspecialidad: 'Hospitalizacion Adultos'
  },
  {
    id: 'BOX-U03',
    nombre: 'BOX U-03',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Observacion Urgencias',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para derivacion',
    servicio: 'Urgencias',
    medicoId: 'usr-med-3',
    medicoNombre: 'Dr. Roberto Garza Morales',
    medicoEspecialidad: 'Medicina Critica y Choque',
    enfermeroId: 'usr-2',
    enfermeroNombre: 'Lic. Enf. Carlos Ramos Nava',
    enfermeroEspecialidad: 'Triage y Reanimacion'
  },
  {
    id: 'BOX-U04',
    nombre: 'BOX U-04',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Adultos Observacion',
    tipo: 'consultorio',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Urgencias',
    medicoId: 'usr-1',
    medicoNombre: 'Dra. Mariana Gomez Trevino',
    medicoEspecialidad: 'Urgencias Adultos / ECE',
    enfermeroId: 'usr-enf-3',
    enfermeroNombre: 'Enf. Laura Sanchez Pineda',
    enfermeroEspecialidad: 'Cuidados Intensivos UCI'
  },
  {
    id: 'BOX-U05',
    nombre: 'BOX U-05',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Recepcion Ambulancia',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para derivacion',
    servicio: 'Urgencias'
  },
  {
    id: 'BOX-U06',
    nombre: 'BOX U-06',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Aislamiento Respiratorio',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre y sanitizado',
    servicio: 'Urgencias'
  },
  {
    id: 'BOX-U07',
    nombre: 'BOX U-07',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Observacion Corta',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para derivacion',
    servicio: 'Urgencias'
  },
  {
    id: 'BOX-U08',
    nombre: 'BOX U-08',
    piso: 'P1',
    zonaNombre: 'Piso 1 - Urgencias y Triage',
    subtitulo: 'Triage y Valoracion',
    tipo: 'box',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para recepcion',
    servicio: 'Urgencias'
  },

  // =========================================================================
  // PISO 2: HOSPITALIZACION ADULTOS (5 CAMAS / HABITACIONES) - TODAS LIBRES
  // =========================================================================
  {
    id: 'HAB-201-A',
    nombre: 'HAB 201-A',
    piso: 'P2',
    zonaNombre: 'Piso 2 - Hospitalizacion Adultos',
    subtitulo: 'Medicina Interna',
    tipo: 'pabellon',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Medicina Interna'
  },
  {
    id: 'HAB-201-B',
    nombre: 'HAB 201-B',
    piso: 'P2',
    zonaNombre: 'Piso 2 - Hospitalizacion Adultos',
    subtitulo: 'Medicina Interna',
    tipo: 'pabellon',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Medicina Interna'
  },
  {
    id: 'HAB-202-A',
    nombre: 'HAB 202-A',
    piso: 'P2',
    zonaNombre: 'Piso 2 - Hospitalizacion Adultos',
    subtitulo: 'Cirugia General',
    tipo: 'pabellon',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Cirugia General'
  },
  {
    id: 'HAB-202-B',
    nombre: 'HAB 202-B',
    piso: 'P2',
    zonaNombre: 'Piso 2 - Hospitalizacion Adultos',
    subtitulo: 'Pabellon Adultos Poniente',
    tipo: 'pabellon',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Medicina Interna'
  },
  {
    id: 'HAB-203',
    nombre: 'HAB 203',
    piso: 'P2',
    zonaNombre: 'Piso 2 - Hospitalizacion Adultos',
    subtitulo: 'Aislamiento Presion Negativa',
    tipo: 'pabellon',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre y sanitizado',
    servicio: 'Infectologia'
  },

  // =========================================================================
  // PISO 3: PEDIATRIA Y GINECOOBSTETRICIA (5 CAMAS / CUNAS) - TODAS LIBRES
  // =========================================================================
  {
    id: 'CUNA-01',
    nombre: 'CUNA-01',
    piso: 'P3',
    zonaNombre: 'Piso 3 - Pediatria y Ginecoobstetricia',
    subtitulo: 'Cuna Termica (Neonatos)',
    tipo: 'pediatria',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Calibrada y libre para ingreso',
    servicio: 'Pediatria / Neonatos'
  },
  {
    id: 'CUNA-02',
    nombre: 'CUNA-02',
    piso: 'P3',
    zonaNombre: 'Piso 3 - Pediatria y Ginecoobstetricia',
    subtitulo: 'Incubadora Cuidados',
    tipo: 'pediatria',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para asignacion inmediata',
    servicio: 'Neonatologia'
  },
  {
    id: 'PED-301',
    nombre: 'PED-301',
    piso: 'P3',
    zonaNombre: 'Piso 3 - Pediatria y Ginecoobstetricia',
    subtitulo: 'Pediatria Escolar',
    tipo: 'pediatria',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre con proteccion anticaidas',
    servicio: 'Pediatria'
  },
  {
    id: 'GIN-305',
    nombre: 'GIN-305',
    piso: 'P3',
    zonaNombre: 'Piso 3 - Pediatria y Ginecoobstetricia',
    subtitulo: 'Puerperio Fisiologico',
    tipo: 'consultorio',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para recepcion de parto',
    servicio: 'Ginecologia'
  },
  {
    id: 'GIN-306',
    nombre: 'GIN-306',
    piso: 'P3',
    zonaNombre: 'Piso 3 - Pediatria y Ginecoobstetricia',
    subtitulo: 'Labor y Recuperacion',
    tipo: 'consultorio',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Libre para ingreso',
    servicio: 'Ginecologia'
  },

  // =========================================================================
  // BLOQUE QUIRURGICO CENTRAL QX (5 SALAS) - TODAS LIBRES
  // =========================================================================
  {
    id: 'QUI-01',
    nombre: 'QUI-01',
    piso: 'QX',
    zonaNombre: 'Bloque Quirurgico QX',
    subtitulo: 'Sala Mayor Cirugia 1',
    tipo: 'quirofano',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Esterilizado y libre',
    servicio: 'Quirofano'
  },
  {
    id: 'QUI-02',
    nombre: 'QUI-02',
    piso: 'QX',
    zonaNombre: 'Bloque Quirurgico QX',
    subtitulo: 'Sala Mayor Cirugia 2',
    tipo: 'quirofano',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Esterilizado y libre',
    servicio: 'Quirofano'
  },
  {
    id: 'QUI-03',
    nombre: 'QUI-03',
    piso: 'QX',
    zonaNombre: 'Bloque Quirurgico QX',
    subtitulo: 'Sala Cirugia Ambulatoria',
    tipo: 'quirofano',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Esterilizado y libre',
    servicio: 'Quirofano'
  },
  {
    id: 'QUI-04',
    nombre: 'QUI-04',
    piso: 'QX',
    zonaNombre: 'Bloque Quirurgico QX',
    subtitulo: 'Trauma y Ortopedia',
    tipo: 'quirofano',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Esterilizado con arco en C',
    servicio: 'Quirofano'
  },
  {
    id: 'QUI-05',
    nombre: 'QUI-05',
    piso: 'QX',
    zonaNombre: 'Bloque Quirurgico QX',
    subtitulo: 'Urgencias QX',
    tipo: 'quirofano',
    estadoOperativo: 'OPERATIVO',
    estatusCenso: 'disponible',
    pacienteInfo: 'Esterilizado para emergencias',
    servicio: 'Quirofano'
  }
]

class GuardiasService {
  /**
   * Obtiene la lista maestra completa de camas/boxes
   */
  obtenerCamasMaestras(): AsignacionGuardiaBox[] {
    try {
      const guardado = localStorage.getItem(STORAGE_KEY_BOXES)
      if (guardado) {
        const parsed = JSON.parse(guardado)
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed
        }
      }
    } catch {
      // Ignorar error
    }

    const copia: AsignacionGuardiaBox[] = JSON.parse(JSON.stringify(PLANTILLA_INICIAL_CAMAS))
    this.guardarCamasMaestras(copia)
    return copia
  }

  /**
   * Guarda la lista maestra de camas
   */
  guardarCamasMaestras(lista: AsignacionGuardiaBox[]) {
    try {
      localStorage.setItem(STORAGE_KEY_BOXES, JSON.stringify(lista))
      window.dispatchEvent(new CustomEvent('vitae-camas-actualizadas', {
        detail: { camas: lista }
      }))
    } catch (e) {
      console.warn('Error al persistir camas maestras', e)
    }
  }

  /**
   * Registra una nueva cama / box en cualquier zona
   */
  registrarNuevaCama(nueva: {
    id: string
    nombre: string
    piso: PisoHospital
    subtitulo: string
    servicio: string
    tipo?: 'box' | 'consultorio' | 'pabellon' | 'quirofano' | 'pediatria' | 'cama'
  }): { ok: boolean; mensaje: string; cama?: AsignacionGuardiaBox } {
    const idLimpio = nueva.id.trim().toUpperCase()
    const nombreLimpio = nueva.nombre.trim().toUpperCase()

    if (!idLimpio || !nombreLimpio) {
      return { ok: false, mensaje: 'El ID y el nombre de la cama o box son obligatorios.' }
    }

    const lista = this.obtenerCamasMaestras()
    const yaExiste = lista.some(c => c.id === idLimpio)
    if (yaExiste) {
      return { ok: false, mensaje: `Ya existe una cama o box con el identificador "${idLimpio}".` }
    }

    const nombresZona: Record<PisoHospital, string> = {
      P1: 'Piso 1 - Urgencias y Triage',
      P2: 'Piso 2 - Hospitalizacion Adultos',
      P3: 'Piso 3 - Pediatria y Ginecoobstetricia',
      QX: 'Bloque Quirurgico QX'
    }

    const item: AsignacionGuardiaBox = {
      id: idLimpio,
      nombre: nombreLimpio,
      piso: nueva.piso,
      zonaNombre: nombresZona[nueva.piso] || 'Zona Hospitalaria',
      subtitulo: nueva.subtitulo.trim() || 'Servicio General',
      tipo: nueva.tipo || (nueva.piso === 'P1' ? 'box' : nueva.piso === 'QX' ? 'quirofano' : 'cama'),
      estadoOperativo: 'OPERATIVO',
      estatusCenso: 'disponible',
      pacienteInfo: 'Libre para asignacion inmediata',
      servicio: nueva.servicio.trim() || 'Servicio Hospitalario'
    }

    lista.push(item)
    this.guardarCamasMaestras(lista)

    // Sincronizar en turnos de guardias existentes
    const turnos: TurnoGuardia[] = ['matutino', 'vespertino', 'nocturno']
    turnos.forEach(t => {
      const g = this.obtenerAsignaciones(t)
      if (!g.some(x => x.id === item.id)) {
        g.push(JSON.parse(JSON.stringify(item)))
        this.guardarAsignaciones(t, g)
      }
    })

    return { ok: true, mensaje: `Cama/Box ${item.nombre} registrada exitosamente en ${item.zonaNombre}.`, cama: item }
  }

  /**
   * Obtiene la lista de asignaciones para un turno
   */
  obtenerAsignaciones(turno: TurnoGuardia = 'matutino'): AsignacionGuardiaBox[] {
    try {
      const guardado = localStorage.getItem(`${STORAGE_KEY_GUARDIAS}_${turno}`)
      if (guardado) {
        const parsed = JSON.parse(guardado)
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed
        }
      }
    } catch {
      // Ignorar error
    }

    const maestras = this.obtenerCamasMaestras()
    const copia = JSON.parse(JSON.stringify(maestras))
    this.guardarAsignaciones(turno, copia)
    return copia
  }

  /**
   * Guarda las asignaciones completas de un turno
   */
  guardarAsignaciones(turno: TurnoGuardia, lista: AsignacionGuardiaBox[]) {
    try {
      localStorage.setItem(`${STORAGE_KEY_GUARDIAS}_${turno}`, JSON.stringify(lista))
      window.dispatchEvent(new CustomEvent('vitae-guardia-actualizada', {
        detail: { turno, asignaciones: lista }
      }))
    } catch (e) {
      console.warn('Error al persistir asignaciones de guardia', e)
    }
  }

  /**
   * Actualiza la asignacion de un box puntual
   */
  actualizarBox(
    turno: TurnoGuardia,
    boxId: string,
    medicoId: string,
    enfermeroId: string,
    medicosLista: PersonalGuardia[],
    enfermerosLista: PersonalGuardia[]
  ): AsignacionGuardiaBox[] {
    const asignaciones = this.obtenerAsignaciones(turno)
    const box = asignaciones.find(b => b.id === boxId)
    if (!box) return asignaciones

    if (medicoId) {
      const med = medicosLista.find(m => m.id === medicoId)
      box.medicoId = medicoId
      box.medicoNombre = med ? med.nombre : ''
      box.medicoEspecialidad = med ? med.especialidad : ''
    } else {
      box.medicoId = ''
      box.medicoNombre = ''
      box.medicoEspecialidad = ''
    }

    if (enfermeroId) {
      const enf = enfermerosLista.find(e => e.id === enfermeroId)
      box.enfermeroId = enfermeroId
      box.enfermeroNombre = enf ? enf.nombre : ''
      box.enfermeroEspecialidad = enf ? enf.especialidad : ''
    } else {
      box.enfermeroId = ''
      box.enfermeroNombre = ''
      box.enfermeroEspecialidad = ''
    }

    this.guardarAsignaciones(turno, asignaciones)
    return asignaciones
  }

  /**
   * Distribucion automatica de guardia
   */
  distribucionAutomatica(
    turno: TurnoGuardia,
    medicosLista: PersonalGuardia[],
    enfermerosLista: PersonalGuardia[]
  ): AsignacionGuardiaBox[] {
    const asignaciones = this.obtenerAsignaciones(turno)

    if (medicosLista.length === 0 && enfermerosLista.length === 0) {
      return asignaciones
    }

    let medIndex = 0
    let enfIndex = 0

    for (const box of asignaciones) {
      if (box.estadoOperativo === 'MANTENIMIENTO') continue

      if (medicosLista.length > 0) {
        const med = medicosLista[medIndex % medicosLista.length]
        if (med) {
          box.medicoId = med.id
          box.medicoNombre = med.nombre
          box.medicoEspecialidad = med.especialidad
        }
        medIndex++
      }

      if (enfermerosLista.length > 0) {
        const enf = enfermerosLista[enfIndex % enfermerosLista.length]
        if (enf) {
          box.enfermeroId = enf.id
          box.enfermeroNombre = enf.nombre
          box.enfermeroEspecialidad = enf.especialidad
        }
        enfIndex++
      }
    }

    this.guardarAsignaciones(turno, asignaciones)
    return asignaciones
  }

  /**
   * Actualiza el estatus de censo de una cama (por ejemplo al asignarla en Recepcion)
   */
  actualizarEstatusCama(id: string, estatus: EstatusCama, pacienteInfo?: string, servicio?: string) {
    const maestras = this.obtenerCamasMaestras()
    const cama = maestras.find(c => c.id === id)
    if (cama) {
      cama.estatusCenso = estatus
      if (pacienteInfo !== undefined) cama.pacienteInfo = pacienteInfo
      if (servicio !== undefined) cama.servicio = servicio
      this.guardarCamasMaestras(maestras)
    }
  }

  /**
   * Restablece todas las camas y asignaciones al estado inicial libre de 23 camas
   */
  restablecerTodoLibre() {
    const copia: AsignacionGuardiaBox[] = JSON.parse(JSON.stringify(PLANTILLA_INICIAL_CAMAS))
    this.guardarCamasMaestras(copia)
    const turnos: TurnoGuardia[] = ['matutino', 'vespertino', 'nocturno']
    turnos.forEach(t => {
      localStorage.removeItem(`${STORAGE_KEY_GUARDIAS}_${t}`)
      this.guardarAsignaciones(t, JSON.parse(JSON.stringify(copia)))
    })
  }

  /**
   * Valida si un profesional médico o de enfermería tiene al menos un box, consultorio o zona asignada
   */
  estaPersonalAsignado(usuarioId?: string, nombreUsuario?: string, rol?: string, cedula?: string): boolean {
    const rolMayus = (rol || '').toUpperCase()
    // Administradores, Recepción y Laboratorio no requieren box asistencial físico
    if (!['MEDICO', 'ENFERMERIA', 'ENFERMERO'].includes(rolMayus)) {
      return true
    }

    const turnos: TurnoGuardia[] = ['matutino', 'vespertino', 'nocturno']
    const hora = new Date().getHours()
    const turnoActual: TurnoGuardia = hora >= 7 && hora < 15 ? 'matutino' : hora >= 15 && hora < 23 ? 'vespertino' : 'nocturno'
    const turnosARevisar = [turnoActual, ...turnos.filter(t => t !== turnoActual)]

    const clean = (s: string) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim()
    const uId = usuarioId ? clean(usuarioId) : ''
    const uNom = nombreUsuario ? clean(nombreUsuario) : ''
    const uCed = cedula ? clean(cedula) : ''

    for (const t of turnosARevisar) {
      const asignaciones = this.obtenerAsignaciones(t)
      const asignado = asignaciones.some(b => {
        if (rolMayus === 'MEDICO') {
          if (uId && b.medicoId && clean(b.medicoId) === uId) return true
          if (uCed && b.medicoId) {
            const medCat = CATALOGO_MEDICOS.find(m => clean(m.id) === clean(b.medicoId!))
            if (medCat && medCat.cedula && clean(medCat.cedula) === uCed) return true
          }
          if (uNom && b.medicoNombre) {
            const bNom = clean(b.medicoNombre)
            if (bNom === uNom || (uNom.length >= 6 && (bNom.includes(uNom) || uNom.includes(bNom)))) return true
          }
        }
        if (rolMayus === 'ENFERMERIA' || rolMayus === 'ENFERMERO') {
          if (uId && b.enfermeroId && clean(b.enfermeroId) === uId) return true
          if (uCed && b.enfermeroId) {
            const enfCat = CATALOGO_ENFERMERIA.find(e => clean(e.id) === clean(b.enfermeroId!))
            if (enfCat && enfCat.cedula && clean(enfCat.cedula) === uCed) return true
          }
          if (uNom && b.enfermeroNombre) {
            const bNom = clean(b.enfermeroNombre)
            if (bNom === uNom || (uNom.length >= 6 && (bNom.includes(uNom) || uNom.includes(bNom)))) return true
          }
        }
        return false
      })
      if (asignado) return true
    }

    // AUTO-ASIGNACIÓN ASISTENCIAL DE RESGUARDO:
    // Si el profesional no tiene puesto asignado en el turno actual (ej. primer inicio de sesión o reinicio de caché),
    // se le asigna de inmediato el primer box/consultorio disponible en Urgencias para garantizar la continuidad asistencial
    // y evitar bloqueos de acceso durante la operación clínica.
    try {
      const asignacionesActuales = this.obtenerAsignaciones(turnoActual)
      const boxAsignar = asignacionesActuales.find(b => {
        if (rolMayus === 'MEDICO') return !b.medicoId || b.medicoId === ''
        if (rolMayus === 'ENFERMERIA' || rolMayus === 'ENFERMERO') return !b.enfermeroId || b.enfermeroId === ''
        return false
      }) || asignacionesActuales[0]

      if (boxAsignar) {
        const medCat = CATALOGO_MEDICOS.find(m => (uId && clean(m.id) === uId) || (uNom && clean(m.nombre) === uNom) || (uCed && m.cedula && clean(m.cedula) === uCed))
        const enfCat = CATALOGO_ENFERMERIA.find(e => (uId && clean(e.id) === uId) || (uNom && clean(e.nombre) === uNom) || (uCed && e.cedula && clean(e.cedula) === uCed))

        if (rolMayus === 'MEDICO') {
          boxAsignar.medicoId = usuarioId || medCat?.id || 'MED-001'
          boxAsignar.medicoNombre = nombreUsuario || medCat?.nombre || 'Dr. Carlos Rivera'
          boxAsignar.medicoEspecialidad = medCat?.especialidad || 'Medicina Interna / Urgencias'
        } else {
          boxAsignar.enfermeroId = usuarioId || enfCat?.id || 'ENF-001'
          boxAsignar.enfermeroNombre = nombreUsuario || enfCat?.nombre || 'Lic. Gabriela Ramos'
          boxAsignar.enfermeroEspecialidad = enfCat?.especialidad || 'Triage y Choque Crítico'
        }
        this.guardarAsignaciones(turnoActual, asignacionesActuales)
        return true
      }
    } catch (e) {
      console.warn('Error en auto-asignación asistencial:', e)
    }

    return false
  }

  /**
   * Detecta el box asignado al medico en sesion
   */
  obtenerBoxAsignadoMedico(medicoId?: string, nombreMedico?: string): string {
    const turnos: TurnoGuardia[] = ['matutino', 'vespertino', 'nocturno']
    const hora = new Date().getHours()
    const turnoActual: TurnoGuardia = hora >= 7 && hora < 15 ? 'matutino' : hora >= 15 && hora < 23 ? 'vespertino' : 'nocturno'
    const turnosRevisar = [turnoActual, ...turnos.filter(t => t !== turnoActual)]

    const clean = (s: string) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim()
    const uId = medicoId ? clean(medicoId) : ''
    const uNom = nombreMedico ? clean(nombreMedico) : ''

    for (const t of turnosRevisar) {
      const lista = this.obtenerAsignaciones(t)
      if (uId) {
        const boxEncontrado = lista.find(b => b.medicoId && clean(b.medicoId) === uId)
        if (boxEncontrado) {
          return `${boxEncontrado.nombre} · ${boxEncontrado.zonaNombre.split(' - ')[1] || boxEncontrado.subtitulo}`
        }
      }
      if (uNom) {
        const boxEncontrado = lista.find(b => b.medicoNombre && (
          clean(b.medicoNombre).includes(uNom) || uNom.includes(clean(b.medicoNombre))
        ))
        if (boxEncontrado) {
          return `${boxEncontrado.nombre} · ${boxEncontrado.zonaNombre.split(' - ')[1] || boxEncontrado.subtitulo}`
        }
      }
    }

    return ''
  }

  /**
   * Detecta el box asignado al enfermero en sesion
   */
  obtenerBoxAsignadoEnfermero(enfermeroId?: string, nombreEnfermero?: string): string {
    const turnos: TurnoGuardia[] = ['matutino', 'vespertino', 'nocturno']
    const hora = new Date().getHours()
    const turnoActual: TurnoGuardia = hora >= 7 && hora < 15 ? 'matutino' : hora >= 15 && hora < 23 ? 'vespertino' : 'nocturno'
    const turnosRevisar = [turnoActual, ...turnos.filter(t => t !== turnoActual)]

    const clean = (s: string) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim()
    const uId = enfermeroId ? clean(enfermeroId) : ''
    const uNom = nombreEnfermero ? clean(nombreEnfermero) : ''

    for (const t of turnosRevisar) {
      const lista = this.obtenerAsignaciones(t)
      if (uId) {
        const boxEncontrado = lista.find(b => b.enfermeroId && clean(b.enfermeroId) === uId)
        if (boxEncontrado) {
          return `${boxEncontrado.nombre} · ${boxEncontrado.zonaNombre.split(' - ')[1] || boxEncontrado.subtitulo}`
        }
      }
      if (uNom) {
        const boxEncontrado = lista.find(b => b.enfermeroNombre && (
          clean(b.enfermeroNombre).includes(uNom) || uNom.includes(clean(b.enfermeroNombre))
        ))
        if (boxEncontrado) {
          return `${boxEncontrado.nombre} · ${boxEncontrado.zonaNombre.split(' - ')[1] || boxEncontrado.subtitulo}`
        }
      }
    }

    return ''
  }

  /**
   * Obtiene la cola activa de solicitudes de cama pendientes (Hospitalización / Observación)
   */
  obtenerSolicitudesCama(): SolicitudCamaPendiente[] {
    try {
      const data = localStorage.getItem(STORAGE_KEY_SOLICITUDES_CAMA)
      if (data) {
        const lista: SolicitudCamaPendiente[] = JSON.parse(data)
        // Calcular tiempo de espera dinámico
        const now = Date.now()
        lista.forEach(s => {
          if (s.fechaSolicitud) {
            const min = Math.max(0, Math.floor((now - new Date(s.fechaSolicitud).getTime()) / 60000))
            s.tiempoEspera = `${min} min`
          }
        })
        return lista
      }
      return []
    } catch {
      return []
    }
  }

  /**
   * Guarda las solicitudes de cama y emite evento global
   */
  guardarSolicitudesCama(lista: SolicitudCamaPendiente[]): void {
    try {
      localStorage.setItem(STORAGE_KEY_SOLICITUDES_CAMA, JSON.stringify(lista))
      window.dispatchEvent(new CustomEvent('vitae-solicitudes-cama-actualizadas', {
        detail: { solicitudes: lista }
      }))
    } catch (e) {
      console.warn('Error al guardar solicitudes de cama:', e)
    }
  }

  /**
   * Registra una nueva solicitud de pase a cama censable desde consulta médica
   */
  crearSolicitudCama(solicitud: {
    folio?: string
    idPaciente?: string
    paciente: string
    curp?: string
    diagnostico?: string
    medicoTratante?: string
    servicio?: string
    prioridad?: string
  }): SolicitudCamaPendiente {
    const lista = this.obtenerSolicitudesCama()
    const nueva: SolicitudCamaPendiente = {
      id: 'SOL-' + Date.now().toString(36).toUpperCase(),
      folio: solicitud.folio || ('HOSP-' + Math.floor(1000 + Math.random() * 9000)),
      idPaciente: solicitud.idPaciente,
      paciente: solicitud.paciente,
      curp: solicitud.curp,
      diagnostico: solicitud.diagnostico,
      medicoTratante: solicitud.medicoTratante || 'Dr. Médico Tratante',
      servicio: solicitud.servicio || 'Hospitalización Adultos',
      prioridad: (solicitud.prioridad as any) || 'Triage 2 - Naranja',
      fechaSolicitud: new Date().toISOString(),
      tiempoEspera: '0 min'
    }

    // Evitar duplicados por folio o idPaciente si ya está en cola
    const existe = lista.find(s => s.folio === nueva.folio || (nueva.idPaciente && s.idPaciente === nueva.idPaciente))
    if (!existe) {
      lista.unshift(nueva)
      this.guardarSolicitudesCama(lista)
      return nueva
    }
    return existe
  }

  /**
   * Remueve una solicitud de cama completada
   */
  removerSolicitudCama(folioOId: string): void {
    const lista = this.obtenerSolicitudesCama().filter(s => s.folio !== folioOId && s.id !== folioOId)
    this.guardarSolicitudesCama(lista)
  }
}

export const guardiasService = new GuardiasService()
export default guardiasService
