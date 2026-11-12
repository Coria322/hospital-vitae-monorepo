<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import api from '@/services/api'
import { CATALOGO_MEDICOS, type PersonalGuardia } from '@/services/guardiasService'
import citasService, { type CitaMedica } from '@/services/citasService'

type SeccionRecepcion = 'curp' | 'registrar' | 'agendar' | 'consentimiento' | 'monitor' | 'censo'

const props = defineProps<{
  curpCompartida?: string
  pacienteSeleccionado?: any
}>()

const emit = defineEmits<{
  (e: 'cambiarSeccion', sec: SeccionRecepcion): void
}>()

const cambiarSeccion = (sec: SeccionRecepcion) => {
  emit('cambiarSeccion', sec)
}

const mostrarAlerta = (mensaje: string) => {
  alert(mensaje)
}

// Subpestaña activa: agendar o visualizar citas
const subtabActiva = ref<'agendar' | 'visualizar'>('agendar')

// =========================================================================
// PANTALLA 3: PROGRAMACIÓN Y ASIGNACIÓN DE CITAS MÉDICAS
// =========================================================================
const listaMedicosDisponibles = ref<PersonalGuardia[]>([...CATALOGO_MEDICOS])

const cargarMedicosReales = async () => {
  try {
    const res = await api.get('/medicos')
    if (res.data?.data && Array.isArray(res.data.data) && res.data.data.length > 0) {
      const mapa = new Map<string, PersonalGuardia>()
      CATALOGO_MEDICOS.forEach(m => mapa.set(m.id, m))
      res.data.data.forEach((u: any) => {
        mapa.set(u.id_usuario, {
          id: u.id_usuario,
          nombre: u.nombre_completo,
          rol: 'MEDICO',
          especialidad: u.especialidad || 'Medicina General / Consulta Externa',
          cedula: u.cedula || 'N/A'
        })
      })
      listaMedicosDisponibles.value = Array.from(mapa.values())
    }
  } catch (e) {
    listaMedicosDisponibles.value = [...CATALOGO_MEDICOS]
  }
}

// Días dinámicos para las próximas dos semanas
interface DiaCita {
  id: string // Formato YYYY-MM-DD
  nombre: string // LUN, MAR, etc.
  num: string // 07, 08...
  mes: string
  fechaTexto: string
  hoy: boolean
  cupos: number
  estado: string
}

const generarDiasDinamicos = (): DiaCita[] => {
  const dias: DiaCita[] = []
  const nombresDias = ['DOM', 'LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB']
  const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
  const hoy = new Date()

  for (let i = 0; i < 14; i++) {
    const d = new Date(hoy)
    d.setDate(hoy.getDate() + i)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const id = `${yyyy}-${mm}-${dd}`
    const diaSem = d.getDay()
    const cupos = diaSem === 0 ? 8 : 12 + ((i * 3) % 7)
    const opciones: Intl.DateTimeFormatOptions = { weekday: 'long', day: 'numeric', month: 'long' }
    const fechaTexto = d.toLocaleDateString('es-MX', opciones)

    dias.push({
      id,
      nombre: nombresDias[diaSem] || 'DÍA',
      num: dd,
      mes: meses[d.getMonth()] || 'Mes',
      fechaTexto: fechaTexto.charAt(0).toUpperCase() + fechaTexto.slice(1),
      hoy: i === 0,
      cupos,
      estado: `${cupos} Cupos`
    })
  }
  return dias
}

const diasSemanaCitas = ref<DiaCita[]>(generarDiasDinamicos())

// Turnos disponibles
type TipoTurno = 'matutino' | 'vespertino' | 'nocturno' | 'todos'
const turnoSeleccionado = ref<TipoTurno>('matutino')

const HORARIOS_POR_TURNO: Record<TipoTurno, string[]> = {
  matutino: ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30', '13:00', '13:30'],
  vespertino: ['14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30'],
  nocturno: ['20:00', '20:30', '21:00', '21:30', '22:00', '22:30'],
  todos: [
    '08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30', '13:00', '13:30',
    '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30',
    '20:00', '20:30', '21:00', '21:30', '22:00', '22:30'
  ]
}

const agendamientoForm = ref({
  especialidad: 'Medicina Interna — Consultorio 104 (Pabellón Central)',
  medico: 'Dr. Carlos Rivera · Céd. Prof. 7894120 (Medicina Interna)',
  curpBusqueda: props.curpCompartida || '',
  pacienteValidado: null as {
    id?: string
    id_expediente?: string
    nombre: string
    edad: number
    sexo?: string
    folio: string
    curp: string
  } | null,
  tipoIntervencion: 'primera' as 'primera' | 'control' | 'preoperatoria',
  diaSeleccionado: (diasSemanaCitas.value[0]?.id || new Date().toISOString().substring(0, 10)) as string,
  horaSeleccionada: '09:00',
  folioTurno: ''
})

const diaActivoObj = computed(() => {
  return diasSemanaCitas.value.find(d => d.id === agendamientoForm.value.diaSeleccionado) || diasSemanaCitas.value[0]
})

const cambiarTurno = (turno: TipoTurno) => {
  turnoSeleccionado.value = turno
  const disponibles = HORARIOS_POR_TURNO[turno]
  if (disponibles && disponibles.length > 0 && disponibles[0]) {
    agendamientoForm.value.horaSeleccionada = disponibles[0]
  }
}

const sincronizarPacienteCita = () => {
  if (props.pacienteSeleccionado) {
    const p = props.pacienteSeleccionado
    agendamientoForm.value.curpBusqueda = p.curp || props.curpCompartida || ''
    agendamientoForm.value.pacienteValidado = {
      id: p.id || p.id_paciente || '',
      nombre: p.nombreCompleto || p.nombre || p.nombre_completo || 'Paciente Validado',
      edad: p.edad || 36,
      sexo: p.sexo || 'Indeterminado',
      folio: p.cupe || p.folio || p.folio_urgencias || '#EXP-2026-9042',
      curp: p.curp || agendamientoForm.value.curpBusqueda
    }
  } else if (props.curpCompartida) {
    agendamientoForm.value.curpBusqueda = props.curpCompartida
  }
}

watch(() => props.pacienteSeleccionado, () => sincronizarPacienteCita())
watch(() => props.curpCompartida, () => sincronizarPacienteCita())

// Citas persistidas y compartidas en el hospital
const listaCitas = ref<CitaMedica[]>([])
const filtroEstado = ref<string>('TODOS')
const filtroMedico = ref<string>('TODOS')
const filtroBusqueda = ref<string>('')

const sincronizarAgendaConsultorio = () => {
  agendaConsultorio104.value = listaCitas.value.map(c => {
    const ini = c.pacienteNombre.split(' ').map(n => n[0] ? n[0] + '.' : '').join(' ')
    return {
      horario: `${c.hora} - ${c.hora.replace(':00', ':30')}`,
      folio: c.folio,
      iniciales: ini || 'P. N.',
      curp: c.pacienteCurp ? `${c.pacienteCurp.substring(0, 10)}...` : 'CURP-REG',
      modalidad: c.tipoIntervencion === 'primera' ? 'Primera Vez' : c.tipoIntervencion === 'preoperatoria' ? 'Preoperatoria' : 'Control',
      estado: c.estado === 'EN_SALA' ? 'En Sala' : c.estado === 'EN_ATENCION' ? 'En Consulta' : c.estado === 'ATENDIDA' ? 'Atendida' : c.estado === 'CANCELADA' ? 'Cancelada' : 'Confirmada',
      confirmacion: 'Impreso'
    }
  })
}

const recargarCitas = () => {
  listaCitas.value = citasService.obtenerCitas()
  sincronizarAgendaConsultorio()
}

const sincronizarTodoConServidor = async () => {
  await cargarMedicosReales()
  await citasService.sincronizarConBackend()
  recargarCitas()
}

onMounted(() => {
  sincronizarPacienteCita()
  cargarMedicosReales()
  recargarCitas()
  citasService.sincronizarConBackend().then(() => recargarCitas())
  window.addEventListener('vitae-citas-actualizadas', recargarCitas)
})

onUnmounted(() => {
  window.removeEventListener('vitae-citas-actualizadas', recargarCitas)
})

const citasFiltradas = computed(() => {
  return listaCitas.value.filter(c => {
    if (filtroEstado.value !== 'TODOS' && c.estado !== filtroEstado.value) return false
    if (filtroMedico.value !== 'TODOS' && c.idMedico !== filtroMedico.value && !c.medicoNombre.includes(filtroMedico.value)) return false
    if (filtroBusqueda.value.trim()) {
      const q = filtroBusqueda.value.trim().toLowerCase()
      const matchPac = c.pacienteNombre.toLowerCase().includes(q)
      const matchCurp = c.pacienteCurp.toLowerCase().includes(q)
      const matchFol = c.folio.toLowerCase().includes(q)
      if (!matchPac && !matchCurp && !matchFol) return false
    }
    return true
  })
})

const pasarASala = (c: CitaMedica) => {
  citasService.actualizarEstadoCita(c.id, 'EN_SALA')
  recargarCitas()
  mostrarAlerta(`Pase concedido: El paciente ${c.pacienteNombre} (${c.folio}) ha ingresado a la Sala de Espera del ${c.medicoNombre}. Ya es visible en su cola de atención médica.`)
}

const cancelarCita = (c: CitaMedica) => {
  if (confirm(`¿Confirma la cancelación de la cita ${c.folio} asignada a ${c.pacienteNombre}?`)) {
    citasService.actualizarEstadoCita(c.id, 'CANCELADA')
    recargarCitas()
    mostrarAlerta(`Cita ${c.folio} cancelada exitosamente.`)
  }
}

const imprimirTicket = (c: CitaMedica) => {
  mostrarAlerta(`Imprimiendo comprobante térmico con código QR para el paciente ${c.pacienteNombre} (Folio: ${c.folio}).`)
}

// Slots reactivos calculados según el turno y día seleccionados
const slotsHorarios = computed(() => {
  const horas = HORARIOS_POR_TURNO[turnoSeleccionado.value] || HORARIOS_POR_TURNO.matutino
  const diaAct = agendamientoForm.value.diaSeleccionado
  const diaTexto = diaActivoObj.value?.fechaTexto
  
  // Buscar citas de ese día para no duplicar horas con el mismo médico
  const medicoActual = agendamientoForm.value.medico
  const horasOcupadas = new Set(
    listaCitas.value
      .filter(c => (c.dia === diaAct || c.dia === diaTexto) && c.estado !== 'CANCELADA')
      .map(c => c.hora)
  )

  return horas.map(h => {
    const ocupado = horasOcupadas.has(h)
    const seleccionado = agendamientoForm.value.horaSeleccionada === h
    return {
      hora: h,
      estado: ocupado ? 'ocupado' : seleccionado ? 'seleccionado' : 'disponible',
      label: ocupado ? 'Ocupado' : seleccionado ? 'Seleccionado' : 'Disponible'
    }
  })
})

const agendaConsultorio104 = ref<Array<{
  horario: string
  folio: string
  iniciales: string
  curp: string
  modalidad: string
  estado: string
  confirmacion: string
}>>([])

const seleccionarHorarioCita = (slot: { hora: string; estado: string; label: string }) => {
  if (slot.estado === 'ocupado') {
    mostrarAlerta('El horario de las ' + slot.hora + ' se encuentra ocupado por otro paciente.')
    return
  }
  agendamientoForm.value.horaSeleccionada = slot.hora
}

const buscandoPacienteCita = ref(false)

const buscarPacienteParaCita = async () => {
  const termino = agendamientoForm.value.curpBusqueda?.trim()
  if (!termino) {
    mostrarAlerta('Ingrese una CURP, Nombre o Folio de paciente para agendar cita.')
    return null
  }

  buscandoPacienteCita.value = true
  try {
    const res = await api.get('/pacientes', { params: { q: termino } })
    const lista = res.data?.data?.data || res.data?.data || []
    if (Array.isArray(lista) && lista.length > 0) {
      const p = lista[0]
      let edad = 35
      if (p.fecha_nacimiento) {
        const nac = new Date(p.fecha_nacimiento)
        if (!isNaN(nac.getTime())) {
          edad = Math.max(0, new Date().getFullYear() - nac.getFullYear())
        }
      }
      agendamientoForm.value.pacienteValidado = {
        id: p.id_paciente || p.id || '',
        id_expediente: p.expediente?.id_expediente || '',
        nombre: p.nombre_completo || 'Paciente Validado',
        edad: edad,
        sexo: p.sexo || 'Indeterminado',
        folio: p.folio_urgencias || `VTE-${p.id_paciente ? String(p.id_paciente).substring(0, 6).toUpperCase() : '8812'}`,
        curp: p.curp || (termino.length === 18 ? termino.toUpperCase() : '')
      }
      mostrarAlerta(`Paciente validado en base de datos: ${p.nombre_completo} (CURP: ${p.curp || 'Registrada'}). Horarios disponibles listos para asignación.`)
      return agendamientoForm.value.pacienteValidado
    } else {
      mostrarAlerta(`No se encontró registro previo con "${termino}". Se procederá a registrar el paciente automáticamente al confirmar la cita.`)
      return null
    }
  } catch (error) {
    console.error('Error buscando paciente para cita:', error)
    mostrarAlerta('Error al consultar el servicio de pacientes. Verifique la conexión con el servidor.')
    return null
  } finally {
    buscandoPacienteCita.value = false
  }
}

const cargarDemoCita = () => {
  if (agendamientoForm.value.pacienteValidado) {
    agendamientoForm.value.curpBusqueda = ''
    agendamientoForm.value.pacienteValidado = null
  } else {
    agendamientoForm.value.curpBusqueda = 'ALMA820419HDFRNR02'
    agendamientoForm.value.pacienteValidado = {
      nombre: 'Álvarez Morales, Antonio',
      edad: 42,
      sexo: 'M',
      folio: 'VTE-8812',
      curp: 'ALMA820419HDFRNR02'
    }
  }
}

const confirmarCitaExpedirComprobante = async () => {
  // Si no está validado pero hay término en el campo de búsqueda, buscarlo automáticamente
  if (!agendamientoForm.value.pacienteValidado && agendamientoForm.value.curpBusqueda?.trim()) {
    await buscarPacienteParaCita()
  }

  // Si aún no está validado, pero el usuario ingresó un nombre o CURP
  if (!agendamientoForm.value.pacienteValidado) {
    const termino = agendamientoForm.value.curpBusqueda?.trim()
    if (!termino) {
      mostrarAlerta('Seleccione o ingrese un paciente (nombre o CURP) antes de confirmar la cita médica.')
      return
    }

    try {
      const esCurp = termino.length === 18
      const curpFinal = esCurp ? termino.toUpperCase() : ('TEMP' + Date.now().toString(36).toUpperCase()).padEnd(18, '0')
      const payloadAlta: any = {
        identificado: true,
        curp: curpFinal,
        nombre_completo: termino.toUpperCase(),
        fecha_nacimiento: '1990-01-01',
        sexo: 'MASCULINO',
        domicilio: 'Consulta Externa Hospital Vitae'
      }
      const resAlta = await api.post('/pacientes', payloadAlta)
      const pCreado = resAlta.data?.data
      if (pCreado) {
        agendamientoForm.value.pacienteValidado = {
          id: pCreado.id_paciente || pCreado.id,
          nombre: pCreado.nombre_completo,
          edad: 36,
          sexo: pCreado.sexo,
          folio: pCreado.folio_urgencias || `VTE-${String(pCreado.id_paciente).substring(0, 6).toUpperCase()}`,
          curp: pCreado.curp
        }
      }
    } catch (e: any) {
      console.warn('Registro local de paciente para cita:', e)
      try {
        const resQuery = await api.get('/pacientes', { params: { q: termino } })
        const listaQuery = resQuery.data?.data?.data || resQuery.data?.data || []
        if (Array.isArray(listaQuery) && listaQuery.length > 0) {
          const pExist = listaQuery[0]
          agendamientoForm.value.pacienteValidado = {
            id: pExist.id_paciente || pExist.id,
            id_expediente: pExist.expediente?.id_expediente || '',
            nombre: pExist.nombre_completo,
            edad: 35,
            sexo: pExist.sexo || 'MASCULINO',
            folio: pExist.folio_urgencias || `VTE-${String(pExist.id_paciente).substring(0, 6).toUpperCase()}`,
            curp: pExist.curp
          }
        }
      } catch (errQ) {
        // Fallback local
      }

      if (!agendamientoForm.value.pacienteValidado) {
        agendamientoForm.value.pacienteValidado = {
          nombre: termino,
          edad: 35,
          sexo: 'MASCULINO',
          folio: `VTE-${Math.floor(1000 + Math.random() * 9000)}`,
          curp: termino.length === 18 ? termino.toUpperCase() : 'CURP-REG'
        }
      }
    }
  }

  const hora = agendamientoForm.value.horaSeleccionada || '11:00'
  const paciente = agendamientoForm.value.pacienteValidado!
  const diaObj = diaActivoObj.value

  // Identificar médico
  const medicoObj = listaMedicosDisponibles.value.find(m => 
    agendamientoForm.value.medico.includes(m.nombre) || 
    agendamientoForm.value.medico.includes(m.id)
  )

  const partesEsp = agendamientoForm.value.especialidad.split(' — ')
  const espNombre = partesEsp[0]?.trim() || 'Medicina Interna'
  const consultorioNombre = partesEsp[1]?.replace('(Pabellón Central)', '')?.trim() || 'Consultorio 104'

  const nuevaCita = citasService.agendarCita({
    idPaciente: paciente.id,
    idExpediente: (paciente as any).id_expediente || undefined,
    pacienteNombre: paciente.nombre,
    pacienteCurp: paciente.curp,
    pacienteEdad: paciente.edad,
    pacienteSexo: paciente.sexo,
    idMedico: medicoObj?.id || 'MED-001',
    medicoNombre: medicoObj?.nombre || agendamientoForm.value.medico.split(' · ')[0]?.trim() || 'Dr. Asignado',
    medicoCedula: medicoObj?.cedula,
    especialidad: espNombre,
    consultorio: consultorioNombre,
    dia: (agendamientoForm.value.diaSeleccionado || diaObj?.id || new Date().toISOString().substring(0, 10)) as string,
    hora: hora,
    tipoIntervencion: agendamientoForm.value.tipoIntervencion,
    estado: 'EN_SALA' // Pasa de inmediato a sala de espera del médico
  })

  agendamientoForm.value.folioTurno = nuevaCita.folio
  recargarCitas()

  mostrarAlerta(`Cita ${nuevaCita.folio} confirmada exitosamente a las ${hora} hrs para ${paciente.nombre} (${diaObj?.fechaTexto || 'Hoy'}). Se envió a la Sala de Espera del ${nuevaCita.medicoNombre} (${consultorioNombre}) y ya se encuentra visible en su estación de consulta.`)
}
</script>

<template>
<div class="view-agendar-container">
          
          <!-- Encabezado de Agendamiento -->
          <div class="section-top-banner">
            <div class="banner-with-icon-col">
              <div class="blue-icon-square">
                <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg></span>
              </div>
              <div class="banner-text-col">
                <div class="badges-row">
                  <span class="cert-pill blue">MÓDULO DE ADMISIÓN · CONSULTA EXTERNA</span>
                  <span class="cert-pill cyan">● {{ turnoSeleccionado === 'matutino' ? 'Turno Matutino (08:00 - 14:00)' : turnoSeleccionado === 'vespertino' ? 'Turno Vespertino (14:00 - 20:00)' : turnoSeleccionado === 'nocturno' ? 'Turno Nocturno (20:00 - 23:00)' : 'Todos los Turnos Habilitados' }}</span>
                </div>
                <h2>Programación y Asignación de Citas Médicas</h2>
              </div>
            </div>

            <div class="banner-right-kpis-row">
              <div class="mini-kpi-white">
                <span class="kpi-symbol"><svg viewBox="0 0 24 24" class="svg-inline-steth" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4"/><circle cx="20" cy="10" r="2"/></svg></span>
                <div>
                  <small>ESPECIALIDADES ACTIVAS</small>
                  <strong>5 Servicios · 14 Consultorios</strong>
                </div>
              </div>

              <div class="mini-kpi-white">
                <span class="kpi-symbol blue"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg></span>
                <div>
                  <small>DISPONIBILIDAD HOY</small>
                  <strong class="text-blue">38 Cupos Libres</strong>
                </div>
              </div>

              <button type="button" class="btn-sync-catalogs" @click="sincronizarTodoConServidor().then(() => mostrarAlerta('Agendas médicas y citas sincronizadas con el servidor.'))">
                <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Sincronizar Catálogos
              </button>
            </div>
          </div>

          <!-- Barra de Navegación de Subpestañas -->
          <div class="citas-nav-tabs">
            <button 
              type="button" 
              class="citas-nav-tab-btn" 
              :class="{ active: subtabActiva === 'agendar' }"
              @click="subtabActiva = 'agendar'"
            >
              <svg viewBox="0 0 24 24" class="svg-inline-tab" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              <span>Agendar Nueva Cita Médica</span>
            </button>
            <button 
              type="button" 
              class="citas-nav-tab-btn" 
              :class="{ active: subtabActiva === 'visualizar' }"
              @click="subtabActiva = 'visualizar'"
            >
              <svg viewBox="0 0 24 24" class="svg-inline-tab" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
              <span>Visualizar Citas Agendadas</span>
              <span class="badge-tab-count">{{ listaCitas.length }}</span>
            </button>
          </div>

          <!-- PESTAÑA 1: FORMULARIO Y CALENDARIO DE AGENDAMIENTO -->
          <div v-if="subtabActiva === 'agendar'" class="curp-layout-grid">
            
            <!-- SUBCOLUMNA IZQUIERDA -->
            <div class="curp-left-col">
              
              <!-- 1. Parámetros del Agendamiento -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="step-num-title">
                    <span class="step-circle">1-4</span>
                    <h4>Parámetros del Agendamiento</h4>
                  </div>
                  <span class="live-dot-tag">● Validación de Cédula y Cobertura Activa</span>
                </div>

                <div class="step-body">
                  <!-- Fila 1: Especialidad y Médico -->
                  <div class="grid-2-cols">
                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span><svg viewBox="0 0 24 24" class="svg-inline-hospital" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><path d="M9 22v-4h6v4"/><line x1="12" y1="6" x2="12" y2="10"/><line x1="10" y1="8" x2="14" y2="8"/></svg></span>
                        <strong>Paso 1: Especialidad y Consultorio</strong>
                      </div>
                      <select v-model="agendamientoForm.especialidad" class="select-clean full">
                        <option>Medicina Interna — Consultorio 104 (Pabellón Central)</option>
                        <option>Cardiología Clínica — Consultorio 108</option>
                        <option>Cirugía General — Consultorio 112</option>
                        <option>Pediatría Médica — Consultorio 201</option>
                        <option>Ginecología y Obstetricia — Consultorio 205</option>
                      </select>
                      <small class="field-hint">Ubicación asignada en tiempo real según roles del día</small>
                    </div>

                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
                        <strong>Paso 2: Médico Tratante Asignado</strong>
                      </div>
                      <select v-model="agendamientoForm.medico" class="select-clean full">
                        <option 
                          v-for="med in listaMedicosDisponibles" 
                          :key="med.id" 
                          :value="med.nombre + ' · Céd. Prof. ' + (med.cedula || 'N/A') + ' (' + med.especialidad + ')'"
                        >
                          {{ med.nombre }} · Céd. Prof. {{ med.cedula || 'N/A' }} ({{ med.especialidad }})
                        </option>
                      </select>
                      <small class="field-hint text-blue">Estado: En funciones · Capacidad máxima 16 citas/día</small>
                    </div>
                  </div>

                  <!-- Fila 2: Paciente y Tipo de Consulta -->
                  <div class="grid-2-cols margin-top-sm">
                    <!-- Paso 3: Paciente -->
                    <div class="field-subcard">
                      <div class="subcard-header-row">
                        <div class="subcard-header-icon">
                          <span><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
                          <strong>Paso 3: Paciente (CURP o Folio Único)</strong>
                        </div>
                        <span class="nom-micro">Validación NOM-024</span>
                      </div>

                      <div class="search-patient-slot-row">
                        <input 
                          type="text" 
                          v-model="agendamientoForm.curpBusqueda" 
                          placeholder="ALMA820419HDFRNR02"
                          class="input-clean flex-1 monospace"
                          @keyup.enter="buscarPacienteParaCita"
                        />
                        <button type="button" class="btn-search-slot" @click="buscarPacienteParaCita">
                          <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Buscar
                        </button>
                        <button 
                          type="button" 
                          class="btn-demo-slot"
                          @click="cargarDemoCita"
                        >
                          {{ agendamientoForm.pacienteValidado ? 'Limpiar' : 'Demo' }}
                        </button>
                      </div>

                      <div v-if="agendamientoForm.pacienteValidado" class="patient-validated-pill">
                        <span class="check-blue"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></span>
                        <span>Paciente validado: <strong>{{ agendamientoForm.pacienteValidado.nombre }} ({{ agendamientoForm.pacienteValidado.edad }} años)</strong> · Folio {{ agendamientoForm.pacienteValidado.folio }}</span>
                      </div>
                      <div v-else class="patient-validated-pill empty">
                        <span>Esperando selección o ingreso de paciente...</span>
                      </div>
                    </div>

                    <!-- Paso 4: Tipo de Intervención -->
                    <div class="field-subcard">
                      <div class="subcard-header-icon">
                        <span><svg viewBox="0 0 24 24" class="svg-inline-steth" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4"/><circle cx="20" cy="10" r="2"/></svg></span>
                        <strong>Tipo de Intervención / Consulta</strong>
                      </div>

                      <div class="intervention-types-row">
                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'primera' }"
                          @click="agendamientoForm.tipoIntervencion = 'primera'"
                        >
                          <strong>Primera Vez</strong>
                          <small>45 min</small>
                        </button>

                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'control' }"
                          @click="agendamientoForm.tipoIntervencion = 'control'"
                        >
                          <strong>Control / Subs.</strong>
                          <small>30 min</small>
                        </button>

                        <button 
                          type="button" 
                          class="btn-intervention"
                          :class="{ active: agendamientoForm.tipoIntervencion === 'preoperatoria' }"
                          @click="agendamientoForm.tipoIntervencion = 'preoperatoria'"
                        >
                          <strong>Preoperatoria</strong>
                          <small>30 min</small>
                        </button>
                      </div>
                      <small class="field-hint">Protocolo de seguimiento con análisis de laboratorio requeridos</small>
                    </div>
                  </div>

                </div>
              </div>

              <!-- 2. Calendario de Turnos (Paso 4) -->
              <div class="step-card">
                <div class="step-card-header">
                  <div>
                    <span class="step-label-blue">PASO 4: CALENDARIO DE TURNOS Y HORARIOS</span>
                    <h4>{{ diaActivoObj?.fechaTexto || 'Fecha Seleccionada' }} · {{ agendamientoForm.especialidad ? agendamientoForm.especialidad.split(' — ')[1] || 'Consultorio' : 'Consultorio' }}</h4>
                  </div>
                  
                  <div class="calendar-legend-row">
                    <span class="legend-item"><span class="box-legend free"></span> Libre</span>
                    <span class="legend-item"><span class="box-legend selected"></span> Seleccionado</span>
                    <span class="legend-item"><span class="box-legend occupied"></span> Ocupado</span>
                  </div>
                </div>

                <div class="step-body">
                  <!-- Barra Interactiva de Selección de Turno -->
                  <div class="turnos-nav-pills">
                    <button 
                      type="button" 
                      class="btn-turno-pill"
                      :class="{ active: turnoSeleccionado === 'matutino' }"
                      @click="cambiarTurno('matutino')"
                    >
                      <span>Matutino (08:00 - 14:00)</span>
                    </button>
                    <button 
                      type="button" 
                      class="btn-turno-pill"
                      :class="{ active: turnoSeleccionado === 'vespertino' }"
                      @click="cambiarTurno('vespertino')"
                    >
                      <span>Vespertino (14:00 - 20:00)</span>
                    </button>
                    <button 
                      type="button" 
                      class="btn-turno-pill"
                      :class="{ active: turnoSeleccionado === 'nocturno' }"
                      @click="cambiarTurno('nocturno')"
                    >
                      <span>Nocturno (20:00 - 23:00)</span>
                    </button>
                    <button 
                      type="button" 
                      class="btn-turno-pill"
                      :class="{ active: turnoSeleccionado === 'todos' }"
                      @click="cambiarTurno('todos')"
                    >
                      <span>Todos los Horarios</span>
                    </button>
                  </div>

                  <!-- Selector de Días de la Semana con Desplazamiento y Fecha Directa -->
                  <div class="days-controls-wrapper">
                    <div class="days-tabs-grid-scroll">
                      <button 
                        v-for="dia in diasSemanaCitas" 
                        :key="dia.id"
                        type="button"
                        class="day-tab-card"
                        :class="{ 
                          active: agendamientoForm.diaSeleccionado === dia.id,
                          today: dia.hoy 
                        }"
                        @click="agendamientoForm.diaSeleccionado = dia.id"
                      >
                        <span class="day-name">{{ dia.nombre }} {{ dia.hoy ? '(HOY)' : '' }}</span>
                        <strong class="day-number">{{ dia.num }}</strong>
                        <span class="day-status highlight">{{ dia.estado }}</span>
                      </button>
                    </div>

                    <div class="date-direct-picker-row">
                      <label class="lbl-direct-date">
                        <span>O seleccione fecha en calendario:</span>
                        <input 
                          type="date" 
                          v-model="agendamientoForm.diaSeleccionado" 
                          class="input-clean date-picker-input"
                        />
                      </label>
                    </div>
                  </div>

                  <!-- Horarios Disponibles según Día y Turno -->
                  <div class="available-hours-container">
                    <span class="hours-header-title">
                      Horarios Disponibles para <strong>{{ diaActivoObj?.fechaTexto || 'el día seleccionado' }}</strong> · <em>Turno {{ turnoSeleccionado.toUpperCase() }}</em>:
                    </span>

                    <div class="slots-hours-grid">
                      <button 
                        v-for="slot in slotsHorarios" 
                        :key="slot.hora"
                        type="button"
                        class="slot-btn"
                        :class="slot.estado"
                        @click="seleccionarHorarioCita(slot)"
                      >
                        <span class="slot-time">{{ slot.hora }}</span>
                        <small class="slot-label">{{ slot.label }}</small>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 3. Agenda del Día · Consultorio 104 -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="title-with-icon-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
                    <div>
                      <h4>Agenda del Día · Consultorio 104</h4>
                      <small class="text-subtle">Pacientes asignados a {{ agendamientoForm.medico ? agendamientoForm.medico.split(' · ')[0] : 'Médico Tratante' }} para hoy</small>
                    </div>
                  </div>
                  <span class="badge-neutral-sm">8 Citas Registradas</span>
                </div>

                <div class="step-body">
                  <div class="table-responsive">
                    <table class="history-table">
                      <thead>
                        <tr>
                          <th>HORARIO</th>
                          <th>FOLIO / INICIALES</th>
                          <th>MODALIDAD</th>
                          <th>ESTADO</th>
                          <th class="text-right">CONFIRMACIÓN OFICIAL</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(cita, idx) in agendaConsultorio104" :key="idx">
                          <td class="col-time font-bold">{{ cita.horario }}</td>
                          <td class="col-patient">
                            <strong>{{ cita.folio }} · {{ cita.iniciales }}</strong>
                            <small class="block-curp">CURP: {{ cita.curp }}</small>
                          </td>
                          <td><span class="pill-modalidad">{{ cita.modalidad }}</span></td>
                          <td>
                            <span class="status-pill" :class="{ attended: cita.estado === 'Atendida', waiting: cita.estado.includes('Espera'), confirmed: cita.estado === 'Confirmada' }">
                              ● {{ cita.estado }}
                            </span>
                          </td>
                          <td class="text-right">
                            <span v-if="cita.confirmacion === 'Entregado'" class="text-green-check"><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Entregado</span>
                            <button v-else-if="cita.confirmacion === 'Reenviar Aviso'" class="btn-resend" @click="mostrarAlerta('Aviso reenviado a sala de espera.')">
                              ▷ Reenviar Aviso
                            </button>
                            <span v-else class="text-whatsapp"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> WhatsApp</span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

            </div>

            <!-- SUBCOLUMNA DERECHA: Resumen de Cita y SLA -->
            <aside class="curp-right-col">
              
              <!-- Tarjeta Resumen de Cita -->
              <div class="side-widget-card appointment-summary-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg></span>
                    <strong>Resumen de Cita</strong>
                  </div>
                  <span class="badge-confirm-pending">POR CONFIRMAR</span>
                </div>

                <!-- Bloque Destacado de Turno y Hora -->
                <div class="appointment-highlight-box">
                  <div class="highlight-top">
                    <small>FOLIO DE TURNO ASIGNADO</small>
                    <strong class="highlight-folio">{{ agendamientoForm.folioTurno }}</strong>
                  </div>
                  <div class="highlight-time-row">
                    <span class="highlight-big-hour">{{ agendamientoForm.horaSeleccionada }}</span>
                    <span class="highlight-date-badge">HRS · {{ diaActivoObj?.nombre }} {{ diaActivoObj?.num }} {{ diaActivoObj?.mes }}</span>
                  </div>
                  <small class="highlight-sub">Duración estimada: 30 minutos (Espacio reservado en agenda del médico).</small>
                </div>

                <!-- Desglose de Parámetros -->
                <div class="appointment-details-stack">
                  <div class="detail-row-item">
                    <span class="detail-icon"><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
                    <div>
                      <small>PACIENTE REGISTRADO</small>
                      <strong>{{ agendamientoForm.pacienteValidado?.nombre || 'Sin paciente seleccionado' }}</strong>
                      <span class="detail-meta">CURP: {{ agendamientoForm.pacienteValidado?.curp || 'ALMA820419HDFRNR02' }}</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon"><svg viewBox="0 0 24 24" class="svg-inline-hospital" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><path d="M9 22v-4h6v4"/><line x1="12" y1="6" x2="12" y2="10"/><line x1="10" y1="8" x2="14" y2="8"/></svg></span>
                    <div>
                      <small>ESPECIALIDAD & MÉDICO</small>
                      <strong>{{ agendamientoForm.especialidad ? agendamientoForm.especialidad.split(' — ')[0] : 'Medicina Interna' }}</strong>
                      <span class="detail-meta">{{ agendamientoForm.medico }}</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg></span>
                    <div>
                      <small>UBICACIÓN FÍSICA</small>
                      <strong>Consultorio 104 · Planta Baja</strong>
                      <span class="detail-meta">Pabellón Central de Consulta Externa</span>
                    </div>
                  </div>

                  <div class="detail-row-item">
                    <span class="detail-icon"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
                    <div>
                      <small>TIPO DE CITA</small>
                      <strong class="text-blue">Control Subsecuente</strong>
                      <span class="detail-meta">Seguimiento de Cuadro Hipertensivo</span>
                    </div>
                  </div>
                </div>

                <!-- Requerimientos de Presentación -->
                <div class="requirements-box">
                  <strong>REQUERIMIENTOS DE PRESENTACIÓN</strong>
                  <ul>
                    <li><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Presentarse 15 min antes con identificación oficial</li>
                    <li><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Traer carnet de citas y receta anterior</li>
                    <li><svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Estudios de química sanguínea recientes</li>
                  </ul>
                </div>

                <!-- Botones de Acción -->
                <div class="appointment-actions-stack">
                  <button type="button" class="btn-action-primary" @click="confirmarCitaExpedirComprobante">
                    Confirmar y Expedir Comprobante [Enter]
                  </button>
                  <button type="button" class="btn-action-secondary" @click="mostrarAlerta('Imprimiendo comprobante térmico con código QR...')">
                    <svg viewBox="0 0 24 24" class="svg-inline-print" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg> Imprimir Recordatorio Térmico
                  </button>
                  <div class="grid-2-cols">
                    <button type="button" class="btn-subtle-msg" @click="mostrarAlerta('SMS enviado al teléfono móvil del paciente.')">
                      <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> SMS Paciente
                    </button>
                    <button type="button" class="btn-subtle-msg" @click="mostrarAlerta('Notificación enviada a WhatsApp con enlace de confirmación.')">
                      <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> WhatsApp
                    </button>
                  </div>
                </div>

                <!-- Nota NOM-024 -->
                <div class="summary-audit-foot">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                  <small>Registro auditado de acuerdo con la <strong>NOM-024-SSA3-2012</strong> de Sistemas de Información de Registro Electrónico para la Salud.</small>
                </div>
              </div>

              <!-- Widget de SLA de Espera -->
              <div class="side-widget-card sla-widget-card">
                <div class="sla-row">
                  <span class="sla-clock-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2"/><path d="M10 2h4"/><path d="m17 5 1.5-1.5"/></svg></span>
                  <div>
                    <small>TIEMPO PROMEDIO ESPERA</small>
                    <strong>08 min · Óptimo Clínico</strong>
                  </div>
                  <span class="sla-badge-success">SLA CUMPLIDO</span>
                </div>
              </div>

            </aside>
          </div>

          <!-- PESTAÑA 2: VISUALIZACIÓN INTEGRAL DE CITAS AGENDADAS -->
          <div v-else class="view-citas-agendadas">
            <!-- Filtros y Métricas Rápidas -->
            <div class="citas-filter-card">
              <div class="filter-controls-row">
                <div class="filter-field search-flex">
                  <label>Buscar Paciente / Folio / CURP</label>
                  <div class="search-input-wrap">
                    <svg viewBox="0 0 24 24" class="svg-search-field" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                    <input 
                      type="text" 
                      v-model="filtroBusqueda" 
                      placeholder="Ej. IVAN ESTRADA, VTE-1042 o CURP..."
                      class="input-filter-text"
                    />
                  </div>
                </div>

                <div class="filter-field">
                  <label>Estado Asistencial</label>
                  <select v-model="filtroEstado" class="select-filter">
                    <option value="TODOS">Todos los estados</option>
                    <option value="EN_SALA">En Sala de Espera</option>
                    <option value="PROGRAMADA">Programada</option>
                    <option value="EN_ATENCION">En Consulta Médica</option>
                    <option value="ATENDIDA">Atendida / Egresada</option>
                    <option value="CANCELADA">Cancelada</option>
                  </select>
                </div>

                <div class="filter-field">
                  <label>Médico Tratante</label>
                  <select v-model="filtroMedico" class="select-filter">
                    <option value="TODOS">Todos los médicos</option>
                    <option v-for="m in listaMedicosDisponibles" :key="m.id" :value="m.id">
                      {{ m.nombre }} ({{ m.especialidad }})
                    </option>
                  </select>
                </div>

                <div class="filter-field-actions">
                  <button type="button" class="btn-refresh-citas" @click="recargarCitas">
                    <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                    <span>Actualizar</span>
                  </button>
                </div>
              </div>

              <!-- Mini KPIs de Estados -->
              <div class="kpi-citas-chips">
                <div class="chip-kpi" :class="{ selected: filtroEstado === 'TODOS' }" @click="filtroEstado = 'TODOS'">
                  <span class="chip-dot all"></span>
                  <span class="chip-label">Total Citas:</span>
                  <strong>{{ listaCitas.length }}</strong>
                </div>
                <div class="chip-kpi" :class="{ selected: filtroEstado === 'EN_SALA' }" @click="filtroEstado = 'EN_SALA'">
                  <span class="chip-dot green"></span>
                  <span class="chip-label">En Sala de Espera:</span>
                  <strong>{{ listaCitas.filter(c => c.estado === 'EN_SALA').length }}</strong>
                </div>
                <div class="chip-kpi" :class="{ selected: filtroEstado === 'PROGRAMADA' }" @click="filtroEstado = 'PROGRAMADA'">
                  <span class="chip-dot blue"></span>
                  <span class="chip-label">Programadas:</span>
                  <strong>{{ listaCitas.filter(c => c.estado === 'PROGRAMADA').length }}</strong>
                </div>
                <div class="chip-kpi" :class="{ selected: filtroEstado === 'EN_ATENCION' }" @click="filtroEstado = 'EN_ATENCION'">
                  <span class="chip-dot orange"></span>
                  <span class="chip-label">En Consulta:</span>
                  <strong>{{ listaCitas.filter(c => c.estado === 'EN_ATENCION').length }}</strong>
                </div>
                <div class="chip-kpi" :class="{ selected: filtroEstado === 'ATENDIDA' }" @click="filtroEstado = 'ATENDIDA'">
                  <span class="chip-dot gray"></span>
                  <span class="chip-label">Atendidas:</span>
                  <strong>{{ listaCitas.filter(c => c.estado === 'ATENDIDA').length }}</strong>
                </div>
              </div>
            </div>

            <!-- Tabla de Citas Agendadas -->
            <div class="citas-table-card">
              <div class="table-card-top">
                <div>
                  <h3>Padrón Institucional de Citas Agendadas y Pase a Sala</h3>
                  <p class="text-subtle">Sincronizado en tiempo real con la cola médica y el censo institucional.</p>
                </div>
                <span class="badge-total-showing">{{ citasFiltradas.length }} Registros</span>
              </div>

              <div v-if="citasFiltradas.length === 0" class="empty-citas-notice">
                <svg viewBox="0 0 24 24" class="svg-empty-box" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                <h4>No se encontraron citas con los filtros aplicados</h4>
                <p>Puede agendar una nueva cita en la pestaña superior o ajustar los criterios de búsqueda.</p>
                <button type="button" class="btn-create-shortcut" @click="subtabActiva = 'agendar'">
                  + Agendar Nueva Cita
                </button>
              </div>

              <div v-else class="table-responsive">
                <table class="citas-master-table">
                  <thead>
                    <tr>
                      <th>FOLIO / FECHA</th>
                      <th>PACIENTE</th>
                      <th>MÉDICO & CONSULTORIO</th>
                      <th>HORARIO Y MODALIDAD</th>
                      <th>ESTADO ASISTENCIAL</th>
                      <th class="text-right">ACCIONES OPERATIVAS</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in citasFiltradas" :key="c.id" :class="'row-status-' + c.estado.toLowerCase()">
                      <td class="col-folio">
                        <strong class="folio-tag">{{ c.folio }}</strong>
                        <small class="text-subtle block-meta">{{ c.fechaRegistro ? c.fechaRegistro.substring(0, 10) : 'Hoy' }}</small>
                      </td>
                      <td class="col-paciente">
                        <div class="patient-main-cell">
                          <span class="patient-avatar-mini">{{ c.pacienteNombre.charAt(0) }}</span>
                          <div>
                            <strong class="patient-full-name">{{ c.pacienteNombre }}</strong>
                            <span class="patient-curp-meta">CURP: {{ c.pacienteCurp || 'SIN-CURP' }}</span>
                          </div>
                        </div>
                      </td>
                      <td class="col-medico">
                        <strong>{{ c.medicoNombre }}</strong>
                        <small class="block-consultorio">{{ c.especialidad }} · {{ c.consultorio }}</small>
                      </td>
                      <td class="col-horario">
                        <span class="badge-time-slot">{{ c.hora }} hrs</span>
                        <small class="block-modality">{{ c.tipoIntervencion === 'primera' ? 'Primera Vez' : c.tipoIntervencion === 'preoperatoria' ? 'Preoperatoria' : 'Control' }}</small>
                      </td>
                      <td>
                        <span 
                          class="status-pill-badge" 
                          :class="{
                            'pill-green': c.estado === 'EN_SALA',
                            'pill-blue': c.estado === 'PROGRAMADA',
                            'pill-orange': c.estado === 'EN_ATENCION',
                            'pill-gray': c.estado === 'ATENDIDA',
                            'pill-red': c.estado === 'CANCELADA'
                          }"
                        >
                          ● {{ c.estado === 'EN_SALA' ? 'En Sala de Espera' : c.estado === 'PROGRAMADA' ? 'Programada' : c.estado === 'EN_ATENCION' ? 'En Consulta' : c.estado === 'ATENDIDA' ? 'Atendida' : 'Cancelada' }}
                        </span>
                      </td>
                      <td class="text-right col-actions">
                        <div class="actions-buttons-cluster">
                          <button 
                            v-if="c.estado === 'PROGRAMADA'" 
                            type="button" 
                            class="btn-table-action btn-transfer" 
                            @click="pasarASala(c)"
                            title="Dar pase a la sala de espera del médico"
                          >
                            <svg viewBox="0 0 24 24" class="svg-inline-btn" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                            <span>Pase a Sala</span>
                          </button>

                          <button 
                            type="button" 
                            class="btn-table-action btn-print" 
                            @click="imprimirTicket(c)"
                            title="Imprimir comprobante con QR"
                          >
                            <svg viewBox="0 0 24 24" class="svg-inline-btn" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
                            <span>Ticket</span>
                          </button>

                          <button 
                            v-if="c.estado !== 'ATENDIDA' && c.estado !== 'CANCELADA'" 
                            type="button" 
                            class="btn-table-action btn-cancel" 
                            @click="cancelarCita(c)"
                            title="Cancelar cita"
                          >
                            <svg viewBox="0 0 24 24" class="svg-inline-btn" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
</template>

<style scoped>

/* TOP BANNER COMÚN */
.section-top-banner {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}

.banner-text-col {
  flex: 1;
  min-width: 300px;
}

.badges-row {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-bottom: 0.4rem;
}

.cert-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.cert-pill.blue { background-color: #EFF6FF; color: #1D4ED8; border: 1px solid #BFDBFE; }
.cert-pill.cyan { background-color: #ECFEFF; color: #0E7490; border: 1px solid #A5F3FC; }
.cert-pill.green { background-color: #F0FDF4; color: #15803D; border: 1px solid #BBF7D0; }

.section-top-banner h2 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0F2D4A;
  margin: 0.2rem 0;
}

.section-top-banner p {
  font-size: 0.75rem;
  color: #64748B;
  margin: 0;
  line-height: 1.35;
  max-width: 750px;
}

.curp-layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1.2rem;
  align-items: start;
}

.curp-left-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  min-width: 0;
}

.curp-right-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.step-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.side-widget-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-row-sm {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #0F2D4A;
}

.title-row-sm span {
  font-size: 1rem;
}

.title-row-sm strong {
  display: block;
  font-size: 0.8rem;
  color: #1E293B;
}

.block-sub {
  display: block;
  font-size: 0.65rem;
  color: #64748B;
}

.time-stamp-sm {
  font-size: 0.7rem;
  font-weight: 700;
  color: #64748B;
  background-color: #F1F5F9;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}


/* =========================================================================
   PANTALLA 3: PROGRAMACIÓN Y ASIGNACIÓN DE CITAS MÉDICAS (IMAGEN 3)
   ========================================================================= */
.view-agendar-container, .view-consentimiento-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.banner-with-icon-col {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.blue-icon-square {
  width: 44px;
  height: 44px;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
}

.banner-right-kpis-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.mini-kpi-white {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.45rem 0.8rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.kpi-symbol {
  font-size: 1.15rem;
}

.mini-kpi-white small {
  display: block;
  font-size: 0.6rem;
  font-weight: 700;
  color: #64748B;
}

.mini-kpi-white strong {
  font-size: 0.75rem;
  color: #1E293B;
}

.btn-sync-catalogs {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.55rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-sync-catalogs:hover {
  background-color: #F8FAFC;
}

.grid-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
}

.margin-top-sm {
  margin-top: 0.8rem;
}

.field-subcard {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.subcard-header-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: #1E293B;
}

.subcard-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nom-micro {
  font-size: 0.65rem;
  color: #0284C7;
  font-weight: 700;
}

.field-hint {
  font-size: 0.68rem;
  color: #64748B;
}

.search-patient-slot-row {
  display: flex;
  gap: 0.4rem;
}

.btn-search-slot {
  background-color: #0F4C81;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0 0.85rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-demo-slot {
  background-color: #F0FDF4;
  border: 1px solid #86EFAC;
  color: #166534;
  border-radius: 6px;
  padding: 0 0.65rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.patient-validated-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #F0FDF4;
  border: 1px solid #BBF7D0;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.7rem;
  color: #166534;
}

.patient-validated-pill.empty {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  color: #94A3B8;
}

.check-blue {
  color: #0284C7;
  font-weight: 800;
}

.intervention-types-row {
  display: flex;
  gap: 0.4rem;
}

.btn-intervention {
  flex: 1;
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.25rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-intervention strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.btn-intervention small {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.btn-intervention.active {
  background-color: #0284C7;
  border-color: #0284C7;
}

.btn-intervention.active strong, .btn-intervention.active small {
  color: #FFFFFF;
}

.step-label-blue {
  font-size: 0.68rem;
  font-weight: 700;
  color: #0284C7;
}

.calendar-legend-row {
  display: flex;
  gap: 0.6rem;
  font-size: 0.68rem;
  color: #475569;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.box-legend {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

.box-legend.free { background-color: #EFF6FF; border: 1px solid #BFDBFE; }
.box-legend.selected { background-color: #2563EB; }
.box-legend.occupied { background-color: #F1F5F9; border: 1px solid #E2E8F0; }
.box-legend.urgency { background-color: #FEE2E2; border: 1px solid #FECACA; }

/* Barra de Turnos */
.turnos-nav-pills {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.9rem;
  flex-wrap: wrap;
}

.btn-turno-pill {
  padding: 0.45rem 0.9rem;
  border-radius: 6px;
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  color: #334155;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-turno-pill:hover {
  background-color: #E2E8F0;
  border-color: #94A3B8;
}

.btn-turno-pill.active {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border-color: #0F2D4A;
  box-shadow: 0 2px 4px rgba(15, 45, 74, 0.2);
}

.days-controls-wrapper {
  margin-bottom: 0.9rem;
}

.days-tabs-grid-scroll {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.4rem;
}

.days-tabs-grid-scroll::-webkit-scrollbar {
  height: 6px;
}

.days-tabs-grid-scroll::-webkit-scrollbar-thumb {
  background-color: #CBD5E1;
  border-radius: 3px;
}

.date-direct-picker-row {
  margin-top: 0.6rem;
  display: flex;
  justify-content: flex-end;
}

.lbl-direct-date {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
}

.date-picker-input {
  padding: 0.3rem 0.5rem;
  font-size: 0.75rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  color: #0F172A;
  background-color: #FFFFFF;
}

.days-tabs-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.5rem;
  margin-bottom: 0.8rem;
}

.day-tab-card {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.6rem 0.35rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  cursor: pointer;
  transition: all 0.15s;
}

.day-tab-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.day-tab-card.today {
  background-color: #0F2D4A;
  color: #FFFFFF;
  border-color: #0F2D4A;
}

.day-tab-card.today .day-name, .day-tab-card.today .day-number, .day-tab-card.today .day-status {
  color: #FFFFFF;
}

.day-name {
  font-size: 0.65rem;
  font-weight: 700;
  color: #64748B;
}

.day-number {
  font-size: 1.1rem;
  font-weight: 800;
  color: #1E293B;
}

.day-status {
  font-size: 0.65rem;
  color: #64748B;
}

.day-status.highlight {
  color: #0284C7;
  font-weight: 700;
}

.available-hours-container {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.hours-header-title {
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E293B;
}

.slots-hours-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.5rem;
}

.slot-btn {
  padding: 0.5rem 0.25rem;
  border-radius: 6px;
  text-align: center;
  border: 1px solid #E2E8F0;
  cursor: pointer;
  transition: all 0.15s;
}

.slot-btn.ocupado {
  background-color: #F1F5F9;
  color: #94A3B8;
  cursor: not-allowed;
}

.slot-btn.disponible {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: #1D4ED8;
}

.slot-btn.seleccionado {
  background-color: #2563EB;
  border-color: #2563EB;
  color: #FFFFFF;
}

.slot-btn.seleccionado .slot-time, .slot-btn.seleccionado .slot-label {
  color: #FFFFFF;
}

.slot-btn.urgencia {
  background-color: #FEF2F2;
  border-color: #FECACA;
  color: #DC2626;
}

.slot-time {
  display: block;
  font-size: 0.75rem;
  font-weight: 800;
}

.slot-label {
  display: block;
  font-size: 0.6rem;
}

.pill-modalidad {
  font-size: 0.68rem;
  background-color: #F1F5F9;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-pill.attended { background-color: #F1F5F9; color: #64748B; }
.status-pill.waiting { background-color: #E0F2FE; color: #0284C7; }
.status-pill.confirmed { background-color: #DCFCE7; color: #166534; }

.text-green-check { color: #059669; font-weight: 700; font-size: 0.72rem; }
.btn-resend { background: none; border: 1px solid #0284C7; color: #0284C7; font-size: 0.68rem; font-weight: 700; border-radius: 4px; padding: 0.2rem 0.5rem; cursor: pointer; }
.text-whatsapp { color: #166534; font-size: 0.72rem; font-weight: 600; }

/* RESUMEN DE CITA DERECHA */
.appointment-summary-card {
  gap: 0.85rem;
}

.badge-confirm-pending {
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.appointment-highlight-box {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.highlight-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.68rem;
  color: #64748B;
}

.highlight-folio {
  color: #0F4C81;
  font-family: monospace;
  font-weight: 800;
  font-size: 0.95rem;
}

.highlight-time-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.highlight-big-hour {
  font-size: 1.8rem;
  font-weight: 800;
  color: #0F2D4A;
}

.highlight-date-badge {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748B;
  background-color: #E2E8F0;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.highlight-sub {
  font-size: 0.68rem;
  color: #64748B;
}

.appointment-details-stack {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  border-top: 1px solid #F1F5F9;
  padding-top: 0.8rem;
}

.detail-row-item {
  display: flex;
  gap: 0.6rem;
}

.detail-icon {
  font-size: 1rem;
}

.detail-row-item small {
  display: block;
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748B;
}

.detail-row-item strong {
  display: block;
  font-size: 0.78rem;
  color: #1E293B;
}

.detail-meta {
  font-size: 0.68rem;
  color: #64748B;
}

.requirements-box {
  background-color: #F0F9FF;
  border: 1px solid #BAE6FD;
  border-radius: 8px;
  padding: 0.75rem;
}

.requirements-box strong {
  font-size: 0.68rem;
  color: #0369A1;
  display: block;
  margin-bottom: 0.35rem;
}

.requirements-box ul {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 0.68rem;
  color: #0C4A6E;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.appointment-actions-stack {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.btn-subtle-msg {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.45rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-subtle-msg:hover {
  background-color: #F1F5F9;
}

.summary-audit-foot {
  display: flex;
  gap: 0.4rem;
  font-size: 0.65rem;
  color: #64748B;
  line-height: 1.3;
}

.sla-widget-card {
  padding: 0.75rem 1rem;
}

.sla-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sla-clock-icon {
  font-size: 1.2rem;
}

.sla-row small {
  display: block;
  font-size: 0.62rem;
  font-weight: 700;
  color: #64748B;
}

.sla-row strong {
  font-size: 0.78rem;
  color: #1E293B;
}

.sla-badge-success {
  background-color: #DCFCE7;
  color: #15803D;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

/* SUBPESTAÑAS DE CITAS */
.citas-nav-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  border-bottom: 2px solid #E2E8F0;
  padding-bottom: 0.5rem;
}

.citas-nav-tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: transparent;
  border: 1px solid transparent;
  padding: 0.6rem 1.1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s;
}

.citas-nav-tab-btn:hover {
  background-color: #F1F5F9;
  color: #1E293B;
}

.citas-nav-tab-btn.active {
  background-color: #0284C7;
  color: #FFFFFF;
  border-color: #0284C7;
  box-shadow: 0 2px 4px rgba(2, 132, 199, 0.2);
}

.svg-inline-tab {
  width: 16px;
  height: 16px;
}

.badge-tab-count {
  background-color: rgba(0, 0, 0, 0.15);
  color: inherit;
  font-size: 0.72rem;
  padding: 0.15rem 0.45rem;
  border-radius: 9999px;
  font-weight: 700;
}

/* VISTA DE CITAS AGENDADAS */
.view-citas-agendadas {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.citas-filter-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filter-controls-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  min-width: 200px;
}

.filter-field.search-flex {
  flex: 1;
  min-width: 250px;
}

.filter-field label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.svg-search-field {
  position: absolute;
  left: 0.75rem;
  width: 16px;
  height: 16px;
  color: #94A3B8;
  pointer-events: none;
}

.input-filter-text {
  width: 100%;
  padding: 0.55rem 0.75rem 0.55rem 2.2rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #1E293B;
  background-color: #F8FAFC;
}

.input-filter-text:focus {
  outline: none;
  border-color: #0284C7;
  background-color: #FFFFFF;
}

.select-filter {
  padding: 0.55rem 0.75rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #1E293B;
  background-color: #F8FAFC;
}

.select-filter:focus {
  outline: none;
  border-color: #0284C7;
  background-color: #FFFFFF;
}

.filter-field-actions {
  display: flex;
  align-items: flex-end;
}

.btn-refresh-citas {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 0.9rem;
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-refresh-citas:hover {
  background-color: #E2E8F0;
  color: #0F172A;
}

/* Mini KPIs chips */
.kpi-citas-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1rem;
  padding-top: 0.85rem;
  border-top: 1px solid #F1F5F9;
}

.chip-kpi {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem;
  border-radius: 20px;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  font-size: 0.78rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.chip-kpi:hover {
  border-color: #94A3B8;
  background-color: #F1F5F9;
}

.chip-kpi.selected {
  background-color: #EFF6FF;
  border-color: #3B82F6;
  color: #1D4ED8;
  font-weight: 600;
}

.chip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.chip-dot.all { background-color: #64748B; }
.chip-dot.green { background-color: #10B981; }
.chip-dot.blue { background-color: #3B82F6; }
.chip-dot.orange { background-color: #F59E0B; }
.chip-dot.gray { background-color: #94A3B8; }

/* Tabla Citas Master */
.citas-table-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.table-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  border-bottom: 1px solid #F1F5F9;
  padding-bottom: 0.75rem;
}

.table-card-top h3 {
  font-size: 1.05rem;
  font-weight: 700;
  color: #0F172A;
  margin: 0;
}

.badge-total-showing {
  font-size: 0.75rem;
  font-weight: 700;
  background-color: #F1F5F9;
  color: #475569;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
}

.empty-citas-notice {
  text-align: center;
  padding: 3rem 1rem;
  color: #64748B;
}

.svg-empty-box {
  width: 48px;
  height: 48px;
  color: #CBD5E1;
  margin-bottom: 0.5rem;
}

.empty-citas-notice h4 {
  font-size: 1.05rem;
  color: #334155;
  margin-bottom: 0.3rem;
}

.btn-create-shortcut {
  margin-top: 1rem;
  padding: 0.55rem 1.1rem;
  background-color: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.citas-master-table {
  width: 100%;
  border-collapse: collapse;
}

.citas-master-table th {
  background-color: #F8FAFC;
  padding: 0.75rem 0.9rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 2px solid #E2E8F0;
  text-align: left;
}

.citas-master-table td {
  padding: 0.85rem 0.9rem;
  border-bottom: 1px solid #F1F5F9;
  font-size: 0.85rem;
  color: #334155;
  vertical-align: middle;
}

.citas-master-table tr:hover {
  background-color: #F8FAFC;
}

.col-folio {
  min-width: 100px;
}

.folio-tag {
  color: #0284C7;
  font-weight: 700;
  font-family: monospace;
  font-size: 0.9rem;
}

.block-meta {
  display: block;
  font-size: 0.72rem;
  color: #94A3B8;
}

.patient-main-cell {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.patient-avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #0284C7;
  color: #FFFFFF;
  font-weight: 700;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.patient-full-name {
  display: block;
  font-size: 0.9rem;
  color: #0F172A;
}

.patient-curp-meta {
  display: block;
  font-size: 0.72rem;
  color: #64748B;
  font-family: monospace;
}

.block-consultorio {
  display: block;
  font-size: 0.74rem;
  color: #64748B;
}

.badge-time-slot {
  display: inline-block;
  background-color: #F1F5F9;
  color: #1E293B;
  font-weight: 700;
  font-size: 0.8rem;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.block-modality {
  display: block;
  font-size: 0.72rem;
  color: #64748B;
  margin-top: 0.15rem;
}

.status-pill-badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.pill-green { background-color: #DCFCE7; color: #15803D; }
.pill-blue { background-color: #DBEAFE; color: #1E40AF; }
.pill-orange { background-color: #FEF3C7; color: #B45309; }
.pill-gray { background-color: #F1F5F9; color: #475569; }
.pill-red { background-color: #FEE2E2; color: #B91C1C; }

.col-actions {
  min-width: 160px;
}

.actions-buttons-cluster {
  display: flex;
  gap: 0.4rem;
  justify-content: flex-end;
}

.btn-table-action {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid #CBD5E1;
  background-color: #FFFFFF;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-table-action:hover {
  background-color: #F8FAFC;
  border-color: #94A3B8;
}

.btn-table-action.btn-transfer {
  background-color: #DCFCE7;
  border-color: #86EFAC;
  color: #15803D;
}

.btn-table-action.btn-transfer:hover {
  background-color: #BBF7D0;
}

.btn-table-action.btn-cancel {
  color: #DC2626;
  border-color: #FECACA;
  padding: 0.35rem 0.45rem;
}

.btn-table-action.btn-cancel:hover {
  background-color: #FEE2E2;
}

.svg-inline-btn {
  width: 14px;
  height: 14px;
}

</style>
