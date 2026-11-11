<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import DoctorHeader from './components/DoctorHeader.vue'
import DoctorSlaBanner from './components/DoctorSlaBanner.vue'
import DoctorSidebar from './components/DoctorSidebar.vue'
import ClinicalRecordSection from './components/ClinicalRecordSection.vue'
import PrescriptionPanel from './components/PrescriptionPanel.vue'
import NuevaNotaEceSection from './components/NuevaNotaEceSection.vue'
import EmitirRecetaSection from './components/EmitirRecetaSection.vue'
import SolicitarLaboratorioSection from './components/SolicitarLaboratorioSection.vue'
import ReclasificarTriageDoctorSection from './components/ReclasificarTriageDoctorSection.vue'
import ColaEsperaDoctorSection from './components/ColaEsperaDoctorSection.vue'
import DoctorPlaceholderSection from './components/DoctorPlaceholderSection.vue'
import type { Paciente, Doctor, Receta } from './types'
import authService from '@/services/authService'
import guardiasService from '@/services/guardiasService'
import { triageService, type PacienteUrgenciaCola } from '@/services/triageService'
import { eceService, type ExpedienteDetalle } from '@/services/eceService'
import citasService from '@/services/citasService'
import api from '@/services/api'

// --- DATOS DEL MÉDICO EN SESIÓN (DESDE API SI EXISTE) ---
const usuarioAuth = authService.getUsuario()
const boxInicial = guardiasService.obtenerBoxAsignadoMedico(
  usuarioAuth?.id_usuario,
  usuarioAuth?.nombre_completo
)
const tieneBoxAsignado = ref(guardiasService.estaPersonalAsignado(
  usuarioAuth?.id_usuario,
  usuarioAuth?.nombre_completo,
  'MEDICO',
  usuarioAuth?.cedula || undefined
))
const doctor = ref<Doctor>({
  nombre: usuarioAuth?.nombre_completo || 'Dr. Carlos Mendoza',
  cedula: usuarioAuth?.cedula ? `CÉD. ${usuarioAuth.cedula}` : 'CED. DEMO-000001',
  box: boxInicial || 'Sin Consultorio Asignado'
})

const sincronizarBoxAsignado = () => {
  const boxDetectado = guardiasService.obtenerBoxAsignadoMedico(
    usuarioAuth?.id_usuario,
    usuarioAuth?.nombre_completo
  )
  doctor.value.box = boxDetectado || 'Sin Consultorio Asignado'
  tieneBoxAsignado.value = guardiasService.estaPersonalAsignado(
    usuarioAuth?.id_usuario,
    usuarioAuth?.nombre_completo,
    'MEDICO',
    usuarioAuth?.cedula || undefined
  )
}

// --- RELOJ Y FECHA EN TIEMPO REAL ---
const currentDateTimeStr = ref('')
let timerId: number | undefined

const updateClock = () => {
  const now = new Date()
  const opcionesFecha: Intl.DateTimeFormatOptions = { 
    day: 'numeric', 
    month: 'short', 
    year: 'numeric' 
  }
  const fecha = now.toLocaleDateString('es-MX', opcionesFecha).replace('.', '')
  const horas = String(now.getHours()).padStart(2, '0')
  const minutos = String(now.getMinutes()).padStart(2, '0')
  const segundos = String(now.getSeconds()).padStart(2, '0')
  currentDateTimeStr.value = `${fecha} · ${horas}:${minutos}:${segundos} h`
}

// --- LISTA DE PACIENTES EN COLA (INICIA VACÍA, CARGA DINÁMICAMENTE DE TRIAGE) ---
const pacientesCola = ref<Paciente[]>([])

// Paciente seleccionado (inicia en null para no tener datos fijos)
const pacienteSeleccionado = ref<Paciente | null>(null)
const expedienteActivo = ref<ExpedienteDetalle | null>(null)
const cargandoExpediente = ref(false)
const mostrarModalBreakTheGlass = ref(false)
const motivoBreakTheGlass = ref('')

// Diagnóstico clínico unificado y compartido entre módulos
const diagnosticoGlobal = ref('')

// Cargar la cola viva de urgencias y citas de consulta externa
const cargarColaReal = async () => {
  let colaUrgencias: Paciente[] = []
  try {
    const data = await triageService.obtenerCola()
    if (data && data.length > 0) {
      const triageColorMap: Record<string, string> = {
        ROJO: '#EF4444',
        NARANJA: '#F97316',
        AMARILLO: '#F59E0B',
        VERDE: '#10B981',
        AZUL: '#3B82F6'
      }
      colaUrgencias = data.map((p: PacienteUrgenciaCola) => ({
        id: p.id_paciente || p.id_triage,
        id_paciente: p.id_paciente,
        id_triage: p.id_triage,
        id_expediente: undefined,
        nombre: p.paciente_nombre,
        folio: p.folio_ingreso,
        ece: 'ECE-' + (p.folio_ingreso?.replace('FOL-TRI-', '') || '001'),
        edad: p.minutos_espera ? 35 : 28,
        sexo: 'Indeterminado',
        box: p.sala_nombre || (p.id_sala ? `Sala ${p.id_sala}` : 'Box de Espera'),
        motivo: `Ingreso clasificado ${p.color_asig}`,
        triage: `Manchester ${p.color_asig} · Nivel ${p.prioridad_nivel}`,
        triageColor: triageColorMap[p.color_asig] || '#3B82F6',
        limiteSla: p.sla_vencido ? 'SLA VENCIDO' : `Límite: ${p.minutos_restantes_sla} min`,
        transcurridosMin: p.minutos_espera,
        restantesMin: p.minutos_restantes_sla,
        signos: p.signos ? {
          pa: p.signos.presion_art || '120/80',
          fc: String(p.signos.frecuencia_card || 75),
          temp: String(p.signos.temperatura || 36.5),
          spo2: String(p.signos.spo2 || 98),
          horaToma: p.fecha_hora_ingreso ? p.fecha_hora_ingreso.substring(11, 16) + ' h' : '10:00 h'
        } : undefined
      }))
    }
  } catch (err) {
    console.warn('Uso de cola local de contingencia', err)
  }

  // FUSIONAR CITAS ACTIVAS DE CONSULTA EXTERNA PARA ESTE MÉDICO O CONSULTORIO
  try {
    await citasService.sincronizarConBackend()
  } catch {}

  const citasDelMedico = citasService.obtenerCitasParaMedico(
    usuarioAuth?.id_usuario,
    doctor.value.nombre,
    doctor.value.box
  )

  const citasMapeadas: Paciente[] = citasDelMedico.map(c => ({
    id: c.id,
    id_paciente: c.idPaciente,
    id_triage: undefined,
    id_cita: c.id,
    id_expediente: c.idExpediente || undefined,
    nombre: c.pacienteNombre,
    folio: c.folio,
    curp: c.pacienteCurp || '',
    identificado: true, // Paciente registrado en recepción con CURP o datos formales
    ece: c.idExpediente ? ('ECE-' + c.idExpediente) : ('ECE-' + c.folio.replace('VTE-', '')),
    edad: c.pacienteEdad || 35,
    sexo: c.pacienteSexo || 'Indeterminado',
    box: c.consultorio || doctor.value.box || 'Cons. 104',
    motivo: `Consulta Externa (${c.tipoIntervencion === 'primera' ? 'Primera Vez' : c.tipoIntervencion === 'preoperatoria' ? 'Preoperatoria' : 'Control'}) - ${c.hora} hrs`,
    triage: `Consulta Externa · ${c.hora} hrs (${c.estado})`,
    triageColor: c.estado === 'EN_ATENCION' ? '#F59E0B' : '#10B981',
    limiteSla: `Cita: ${c.hora} hrs`,
    transcurridosMin: 0,
    restantesMin: 30,
    signos: c.signos ? {
      pa: c.signos.pa,
      fc: String(c.signos.fc),
      temp: String(c.signos.temp),
      spo2: String(c.signos.spo2),
      horaToma: c.signos.horaToma || (c.hora + ' h')
    } : undefined
  }))

  pacientesCola.value = [...colaUrgencias, ...citasMapeadas]
}

const cargarExpedientePaciente = async (p: Paciente) => {
  // Si no cuenta con id_paciente UUID directo, resolverlo mediante búsqueda en backend por CURP o Nombre
  if (!p.id_paciente) {
    const termino = p.curp || p.nombre
    if (termino && termino !== 'NO_PROPORCIONADA' && !termino.startsWith('TEMP')) {
      try {
        const res = await api.get('/pacientes', { params: { q: termino } })
        const lista = res.data?.data?.data || res.data?.data || []
        if (Array.isArray(lista) && lista.length > 0) {
          const pacBD = lista[0]
          p.id_paciente = pacBD.id_paciente || pacBD.id
          if (pacBD.curp) p.curp = pacBD.curp
          if (pacBD.identificado !== undefined) p.identificado = pacBD.identificado
          if (pacBD.nombre_completo) p.nombre = pacBD.nombre_completo
        }
      } catch (e) {
        console.warn('Error resolviendo paciente por CURP/nombre:', e)
      }
    }
  }

  // Si aún no se localiza en el backend, asegurar su alta formal en PostgreSQL para aperturar expediente
  if (!p.id_paciente) {
    try {
      const curpValida = p.curp && p.curp.length === 18 ? p.curp : ('TEMP' + Date.now().toString(36).toUpperCase()).padEnd(18, '0')
      const resAlta = await api.post('/pacientes', {
        identificado: true,
        curp: curpValida,
        nombre_completo: p.nombre || 'PACIENTE CONSULTA EXTERNA',
        fecha_nacimiento: '1980-01-01',
        sexo: p.sexo || 'MASCULINO',
        domicilio: 'Hospital Vitae'
      })
      const pacCreado = resAlta.data?.data
      if (pacCreado) {
        p.id_paciente = pacCreado.id_paciente || pacCreado.id
        p.curp = pacCreado.curp
      }
    } catch (e) {
      console.warn('Error al auto-registrar paciente para expediente:', e)
    }
  }

  if (!p.id_paciente) return
  cargandoExpediente.value = true
  try {
    const pacienteData = await eceService.consultarPaciente(p.id_paciente)
    if (pacienteData) {
      p.curp = pacienteData.curp || p.curp
      p.identificado = pacienteData.identificado ?? p.identificado
      p.folio_urgencias = pacienteData.folio_urgencias
      if (pacienteData.nombre_completo && (!p.nombre || p.nombre.includes('DESCONOCIDO') || p.nombre.includes('NN'))) {
        p.nombre = pacienteData.nombre_completo
      }
    }

    let idExp = p.id_expediente || pacienteData?.expediente?.id_expediente

    if (!idExp) {
      try {
        const nuevoExp = await eceService.crearExpediente({
          id_paciente: p.id_paciente,
          motivo_consulta: p.motivo || 'Ingreso a consulta asistencial',
          diagnostico: diagnosticoGlobal.value || undefined
        })
        idExp = nuevoExp.id_expediente
      } catch (eExp: any) {
        if (eExp?.response?.data?.id_expediente_existente) {
          idExp = eExp.response.data.id_expediente_existente
        } else {
          try {
            const pacRecheck = await eceService.consultarPaciente(p.id_paciente)
            idExp = pacRecheck?.expediente?.id_expediente
          } catch {
            // Ignorar
          }
        }
      }
    }

    if (idExp) {
      p.id_expediente = idExp
      p.ece = 'ECE-' + idExp
      if (p.id_cita) {
        citasService.actualizarExpedienteCita(p.id_cita, p.id_paciente, idExp)
      }
    }

    if (idExp) {
      try {
        const expCompleto = await eceService.obtenerExpediente(idExp)
        expedienteActivo.value = expCompleto

        if (expCompleto.antecedentes && expCompleto.antecedentes.length > 0) {
          const alergias = expCompleto.antecedentes.filter(a => a.tipo === 'ALERGIA').map(a => a.descripcion).join(', ') || 'Ninguna conocida'
          const patologicos = expCompleto.antecedentes.filter(a => a.tipo === 'PATOLOGICO').map(a => a.descripcion).join(', ') || 'Sin antecedentes referidos'
          const otros = expCompleto.antecedentes.filter(a => a.tipo !== 'ALERGIA' && a.tipo !== 'PATOLOGICO').map(a => `${a.tipo}: ${a.descripcion}`).join(', ')
          p.antecedentes = {
            motivo: expCompleto.motivo_consulta || p.motivo,
            alergias,
            cronicos: otros ? `${patologicos} | ${otros}` : patologicos,
            medicacion: 'Ver notas médicas'
          }
        } else {
          p.antecedentes = {
            motivo: expCompleto.motivo_consulta || p.motivo || 'Consulta Médica General',
            alergias: 'Ninguna registrada',
            cronicos: 'Sin antecedentes patológicos previos',
            medicacion: 'Sin medicación activa'
          }
        }

        if (expCompleto.notas_medicas && expCompleto.notas_medicas.length > 0) {
          const ultima = expCompleto.notas_medicas[expCompleto.notas_medicas.length - 1]
          if (ultima) {
            p.ultimaNota = {
              titulo: `Nota de ${ultima.tipo} (${ultima.cerrada ? 'Firmada' : 'Borrador'})`,
              medico: ultima.id_usuario,
              folio: ultima.id_nota,
              fecha: new Date(ultima.fecha_hora_creacion).toLocaleString('es-MX'),
              contenido: ultima.contenido
            }
          }
        }
      } catch (errExp: any) {
        if (errExp?.response?.status === 403 && errExp?.response?.data?.code === 'BREAK_THE_GLASS_REQUIRED') {
          mostrarModalBreakTheGlass.value = true
        } else {
          console.warn('Detalle de expediente:', errExp?.response?.data?.message || errExp.message)
        }
      }
    }
  } catch (err: any) {
    if (err?.response?.status === 403 && err?.response?.data?.code === 'BREAK_THE_GLASS_REQUIRED') {
      mostrarModalBreakTheGlass.value = true
    } else {
      console.warn('Detalle de expediente:', err?.response?.data?.message || err.message)
    }
  } finally {
    cargandoExpediente.value = false
  }
}

const manejarPacienteFusionado = async (idDefinitivo: string) => {
  try {
    cargandoExpediente.value = true
    const pacienteData = await eceService.consultarPaciente(idDefinitivo)
    if (pacienteSeleccionado.value && pacienteData) {
      pacienteSeleccionado.value.id = pacienteData.id_paciente
      pacienteSeleccionado.value.id_paciente = pacienteData.id_paciente
      pacienteSeleccionado.value.nombre = pacienteData.nombre_completo
      pacienteSeleccionado.value.curp = pacienteData.curp
      pacienteSeleccionado.value.identificado = true
      pacienteSeleccionado.value.sexo = pacienteData.sexo || pacienteSeleccionado.value.sexo
      if (pacienteData.expediente?.id_expediente) {
        pacienteSeleccionado.value.id_expediente = pacienteData.expediente.id_expediente
        pacienteSeleccionado.value.ece = 'ECE-' + pacienteData.expediente.id_expediente
      }
      await cargarExpedientePaciente(pacienteSeleccionado.value)
      await cargarColaReal()
    }
  } catch (err: any) {
    console.error('Error al actualizar paciente tras fusión:', err)
  } finally {
    cargandoExpediente.value = false
  }
}

const confirmarBreakTheGlass = async () => {
  if (!pacienteSeleccionado.value?.id_paciente) return
  if (!motivoBreakTheGlass.value || motivoBreakTheGlass.value.trim().length < 15) {
    alert('Atención: La justificación médica de urgencia debe contener al menos 15 caracteres conforme a NOM-004.')
    return
  }

  try {
    await eceService.activarBreakTheGlass({
      id_paciente: pacienteSeleccionado.value.id_paciente,
      motivo: motivoBreakTheGlass.value.trim(),
      duracion_minutos: 120
    })
    alert('Protocolo Break-the-Glass activado exitosamente. Acceso de emergencia concedido por 120 minutos con registro inalterable de auditoría.')
    mostrarModalBreakTheGlass.value = false
    motivoBreakTheGlass.value = ''
    if (pacienteSeleccionado.value) {
      await cargarExpedientePaciente(pacienteSeleccionado.value)
    }
  } catch (err: any) {
    alert('Error al activar protocolo de emergencia: ' + (err?.response?.data?.message || err.message))
  }
}

const modalEgresoAbierto = ref(false)
const motivoEgreso = ref('Alta médica por mejoría clínica')
const tipoEgreso = ref<'ALTA_DOMICILIO' | 'HOSPITALIZACION' | 'OBSERVACION' | 'TRASLADO'>('ALTA_DOMICILIO')

const abrirModalEgreso = () => {
  if (!pacienteSeleccionado.value) return
  motivoEgreso.value = 'Alta médica por mejoría clínica y estabilización'
  tipoEgreso.value = 'ALTA_DOMICILIO'
  modalEgresoAbierto.value = true
}

const cerrarModalEgreso = () => {
  modalEgresoAbierto.value = false
}

const confirmarEgresoConsulta = async () => {
  if (!pacienteSeleccionado.value) return
  const p = pacienteSeleccionado.value

  // Si el destino es Hospitalización u Observación, generar solicitud de cama en Censo de Recepción
  if (tipoEgreso.value === 'HOSPITALIZACION' || tipoEgreso.value === 'OBSERVACION') {
    const prioridadCalc = p.triage?.toUpperCase().includes('ROJO') || p.triage?.toUpperCase().includes('NARANJA')
      ? 'Triage 2 - Naranja'
      : p.triage?.toUpperCase().includes('AMARILLO')
        ? 'Triage 3 - Amarillo'
        : 'Programado'

    const servicioCalc = tipoEgreso.value === 'HOSPITALIZACION' ? 'Hospitalización Adultos' : 'Observación Urgencias'

    guardiasService.crearSolicitudCama({
      folio: p.folio || `HOSP-${Math.floor(1000 + Math.random() * 9000)}`,
      idPaciente: p.id_paciente,
      paciente: p.nombre,
      diagnostico: diagnosticoGlobal.value || p.motivo || motivoEgreso.value,
      medicoTratante: doctor.value.nombre,
      servicio: servicioCalc,
      prioridad: prioridadCalc
    })
  }

  // Si proviene de cita de consulta externa
  if (p.id_cita || (typeof p.id === 'string' && p.id.startsWith('CITA-'))) {
    const citaId = p.id_cita || p.id
    citasService.actualizarEstadoCita(citaId, 'ATENDIDA')
    const destExtra = (tipoEgreso.value === 'HOSPITALIZACION' || tipoEgreso.value === 'OBSERVACION')
      ? ' Se ha enviado la solicitud de asignación de cama al Censo Hospitalario en Recepción.'
      : ''
    alert(`Consulta médica finalizada con éxito. El paciente ${p.nombre} ha concluido su atención y el Box/Consultorio ha sido liberado.${destExtra}`)
    modalEgresoAbierto.value = false
    pacienteSeleccionado.value = null
    expedienteActivo.value = null
    seccionActiva.value = 'cola'
    await cargarColaReal()
    return
  }

  const idTriage = p.id_triage || p.id
  if (!idTriage) {
    alert('No se identificó el registro de triage para egresar.')
    return
  }

  try {
    const textoMotivo = `Egreso: ${tipoEgreso.value} - ${motivoEgreso.value.trim()}`
    await triageService.egresarPaciente(idTriage, textoMotivo)
    
    const destExtra = (tipoEgreso.value === 'HOSPITALIZACION' || tipoEgreso.value === 'OBSERVACION')
      ? ' Se ha enviado la solicitud de asignación de cama al Censo Hospitalario en Recepción.'
      : ''
    alert(`Consulta médica finalizada con éxito. El paciente ${p.nombre} ha sido egresado del área asistencial y el Box/Consultorio ha sido liberado.${destExtra}`)
    modalEgresoAbierto.value = false
    pacienteSeleccionado.value = null
    expedienteActivo.value = null
    seccionActiva.value = 'cola'
    await cargarColaReal()
  } catch (err: any) {
    alert('Error al egresar paciente: ' + (err?.response?.data?.message || err.message))
  }
}

const seleccionarPaciente = async (p: Paciente) => {
  if (!tieneBoxAsignado.value) {
    alert('ACCESO ASISTENCIAL BLOQUEADO: No cuenta con un consultorio o box asignado en el turno actual. Solicite a Administración la asignación física de puesto para iniciar la atención clínica.')
    return
  }
  pacienteSeleccionado.value = p
  if (p.motivo && !diagnosticoGlobal.value) {
    diagnosticoGlobal.value = p.motivo
  }

  // Si proviene de cita de consulta externa, marcar como EN_ATENCION
  if (p.id_cita || (typeof p.id === 'string' && p.id.startsWith('CITA-'))) {
    citasService.actualizarEstadoCita(p.id_cita || p.id, 'EN_ATENCION')
  }

  // Notificar al backend cambio a EN_ATENCION y asignación de Box si es de triage
  const idTriage = p.id_triage
  if (idTriage) {
    try {
      await triageService.iniciarAtencion(idTriage, undefined, doctor.value.box)
    } catch (e) {
      console.warn('Estatus EN_ATENCION sincronizado', e)
    }
  }

  // Cargar expediente clínico formal en backend (busca por id_paciente, CURP o Nombre)
  await cargarExpedientePaciente(p)
}

const ingresarConsultaDesdeCola = async (p: Paciente) => {
  if (!tieneBoxAsignado.value) {
    alert('ACCESO RESTRINGIDO: No cuenta con un consultorio o box asignado en el turno actual. Solicite a Administración la asignación física de puesto para iniciar la atención clínica.')
    return
  }
  await seleccionarPaciente(p)
  seccionActiva.value = 'atencion'
}

const deseleccionarPaciente = () => {
  pacienteSeleccionado.value = null
}

// Control de secciones activas de la barra lateral
const seccionActiva = ref<'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola'>('nota')

const cambiarSeccion = (sec: 'atencion' | 'nota' | 'receta' | 'lab' | 'triage' | 'cola') => {
  seccionActiva.value = sec
}

// Formulario de emisión de receta en panel lateral
const receta = ref<Receta>({
  medicamento: '',
  dosis: '',
  via: 'Oral',
  frecuencia: '',
  duracion: '',
  indicaciones: ''
})

const emitirReceta = () => {
  alert('Prescripción emitida con éxito.')
}

// Atajos de teclado del módulo médico
const handleKeyboardShortcuts = (e: KeyboardEvent) => {
  if (e.altKey && (e.key === 'e' || e.key === 'E')) {
    e.preventDefault()
    cambiarSeccion('atencion')
  } else if (e.altKey && (e.key === 'n' || e.key === 'N')) {
    e.preventDefault()
    cambiarSeccion('nota')
  } else if (e.altKey && (e.key === 'r' || e.key === 'R')) {
    e.preventDefault()
    cambiarSeccion('receta')
  } else if (e.altKey && (e.key === 'l' || e.key === 'L')) {
    e.preventDefault()
    cambiarSeccion('lab')
  } else if (e.altKey && (e.key === 't' || e.key === 'T')) {
    e.preventDefault()
    cambiarSeccion('triage')
  }
}

let cerrarStreamSseDoctor: (() => void) | null = null

onMounted(() => {
  updateClock()
  timerId = window.setInterval(updateClock, 1000)
  window.addEventListener('keydown', handleKeyboardShortcuts)
  window.addEventListener('vitae-guardia-actualizada', sincronizarBoxAsignado)
  window.addEventListener('vitae-citas-actualizadas', cargarColaReal)
  sincronizarBoxAsignado()
  cargarColaReal()

  try {
    cerrarStreamSseDoctor = triageService.conectarStream(() => {
      cargarColaReal()
    })
  } catch (e) {
    console.warn('SSE stream no disponible en estacion medica:', e)
  }
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
  window.removeEventListener('keydown', handleKeyboardShortcuts)
  window.removeEventListener('vitae-guardia-actualizada', sincronizarBoxAsignado)
  window.removeEventListener('vitae-citas-actualizadas', cargarColaReal)
  if (cerrarStreamSseDoctor) {
    cerrarStreamSseDoctor()
    cerrarStreamSseDoctor = null
  }
})
</script>

<template>
  <div class="doctor-workspace-wrapper">
    <!-- 1. ENCABEZADO SUPERIOR PERSISTENTE -->
    <DoctorHeader 
      :doctor="doctor"
      :currentDateTimeStr="currentDateTimeStr"
    />

    <!-- BANNER DE ADVERTENCIA: PROFESIONAL SIN CONSULTORIO ASIGNADO -->
    <div v-if="!tieneBoxAsignado" class="session-warning-banner-doctor">
      <div class="warning-icon-box">
        <svg viewBox="0 0 24 24" class="svg-warning" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
      </div>
      <div class="warning-text-block">
        <strong>ESTADO EN ADVERTENCIA: ACCESO ASISTENCIAL BLOQUEADO</strong>
        <p>El profesional {{ doctor.nombre }} no cuenta con consultorio o box asignado para el turno activo. El inicio de consulta médica, registro de notas clínicas ECE, recetas y auxiliares diagnósticos se encuentran bloqueados hasta que Administración le asigne puesto físico.</p>
      </div>
    </div>


    <!-- 2. BANNER DE PRIORIDAD SLA (VISIBLE CON PACIENTE EN CONSULTA) -->
    <DoctorSlaBanner 
      v-if="pacienteSeleccionado"
      :pacienteSeleccionado="pacienteSeleccionado"
      @deseleccionarPaciente="deseleccionarPaciente"
      @terminarConsulta="abrirModalEgreso"
    />

    <!-- 3. WORKSPACE: GRID ADAPTABLE (3 COLUMNAS EN EXPEDIENTE, 2 COLUMNAS EN PANTALLAS DE TRABAJO) -->
    <div class="workspace-grid" :class="{ 'two-columns-layout': seccionActiva !== 'atencion' }">
      
      <!-- ================= COLUMNA 1: SIDEBAR HERRAMIENTAS Y COLA ================= -->
      <DoctorSidebar 
        :seccionActiva="seccionActiva"
        :pacientesCola="pacientesCola"
        :pacienteSeleccionado="pacienteSeleccionado"
        @cambiarSeccion="cambiarSeccion"
        @seleccionarPaciente="seleccionarPaciente"
      />

      <!-- ================= CONTENIDO CENTRAL DINÁMICO ================= -->
      <main class="center-workspace">
        <!-- PANTALLA 1: EXPEDIENTE CLÍNICO -->
        <ClinicalRecordSection 
          v-if="seccionActiva === 'atencion'"
          :pacienteSeleccionado="pacienteSeleccionado"
          :expediente="expedienteActivo"
          @refrescarExpediente="() => pacienteSeleccionado && cargarExpedientePaciente(pacienteSeleccionado)"
          @pacienteFusionado="manejarPacienteFusionado"
        />

        <!-- PANTALLA 2: NUEVA NOTA MÉDICA ECE (CAPTURA 1) -->
        <NuevaNotaEceSection
          v-else-if="seccionActiva === 'nota'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @guardarYFirmar="() => pacienteSeleccionado && cargarExpedientePaciente(pacienteSeleccionado)"
        />

        <!-- PANTALLA 3: MÓDULO DE PRESCRIPCIÓN ELECTRÓNICA (CAPTURA 2) -->
        <EmitirRecetaSection
          v-else-if="seccionActiva === 'receta'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @emitirReceta="() => pacienteSeleccionado && cargarExpedientePaciente(pacienteSeleccionado)"
        />

        <!-- PANTALLA 4: SOLICITUD DE LABORATORIO E IMAGEN (CAPTURA 3) -->
        <SolicitarLaboratorioSection
          v-else-if="seccionActiva === 'lab'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          :diagnosticoGlobal="diagnosticoGlobal"
          @actualizarDiagnostico="(d: string) => diagnosticoGlobal = d"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @enviarSolicitud="() => pacienteSeleccionado && cargarExpedientePaciente(pacienteSeleccionado)"
        />

        <!-- PANTALLA 5: RECLASIFICACIÓN DE TRIAGE (CAPTURA 4) -->
        <ReclasificarTriageDoctorSection
          v-else-if="seccionActiva === 'triage'"
          :doctor="doctor"
          :pacienteSeleccionado="pacienteSeleccionado"
          :pacientesCola="pacientesCola"
          @seleccionarPaciente="seleccionarPaciente"
          @deseleccionarPaciente="deseleccionarPaciente"
          @cambiarSeccion="cambiarSeccion"
        />

        <!-- PANTALLA 6: COLA DE ESPERA Y FLUJO DE DEMANDA (CAPTURA 5) -->
        <ColaEsperaDoctorSection 
          v-else-if="seccionActiva === 'cola'"
          :doctor="doctor"
          :pacientesCola="pacientesCola"
          :pacienteSeleccionado="pacienteSeleccionado"
          @seleccionarPaciente="seleccionarPaciente"
          @ingresarConsulta="ingresarConsultaDesdeCola"
        />

        <DoctorPlaceholderSection 
          v-else
          :seccionActiva="seccionActiva"
          :pacientesCola="pacientesCola"
          @cambiarSeccion="cambiarSeccion"
          @seleccionarPaciente="seleccionarPaciente"
        />
      </main>

      <!-- ================= COLUMNA 3: RESUMEN TERAPÉUTICO (SOLO EN EXPEDIENTE CLÍNICO) ================= -->
      <PrescriptionPanel 
        v-if="seccionActiva === 'atencion'"
        :pacienteSeleccionado="pacienteSeleccionado"
        :doctor="doctor"
        @cambiarSeccion="cambiarSeccion"
      />

    </div>

    <!-- MODAL DE EMERGENCIA BREAK-THE-GLASS -->
    <div v-if="mostrarModalBreakTheGlass" class="modal-btg-backdrop">
      <div class="modal-btg-card">
        <div class="modal-btg-header">
          <div class="modal-btg-icon">
            <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <div>
            <h4>Protocolo Break-the-Glass (Acceso de Emergencia)</h4>
            <p>NOM-004-SSA3-2012 / Auditoría de Seguridad Reforzada</p>
          </div>
        </div>
        <div class="modal-btg-body">
          <p class="modal-btg-desc">
            No cuenta con relación asistencial previa con el paciente <strong>{{ pacienteSeleccionado?.nombre }}</strong>. Para acceder a su expediente clínico de urgencia, justifique clínicamente la necesidad médica.
          </p>
          <div class="form-group-btg">
            <label>Justificación Médica Obligatoria (Mínimo 15 caracteres) *</label>
            <textarea 
              v-model="motivoBreakTheGlass"
              rows="3"
              class="textarea-btg"
              placeholder="Describa el motivo médico urgente que justifica romper el cristal de seguridad..."
            ></textarea>
          </div>
          <div class="btg-actions">
            <button class="btn-cancel" @click="mostrarModalBreakTheGlass = false">Cancelar</button>
            <button class="btn-confirm-btg" @click="confirmarBreakTheGlass">Activar Acceso de Emergencia (120 min)</button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL DE TERMINAR CONSULTA Y EGRESAR PACIENTE -->
    <div v-if="modalEgresoAbierto" class="modal-btg-backdrop">
      <div class="modal-btg-card">
        <div class="modal-egreso-header">
          <div class="modal-egreso-icon">
            <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 11l3 3L22 4"/>
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
            </svg>
          </div>
          <div>
            <h3>Finalizar Consulta y Egresar Paciente</h3>
            <span class="sub-legal">NOM-004-SSA3-2012 · Liberación de Box y Censo</span>
          </div>
        </div>
        <div class="modal-btg-body">
          <p class="modal-btg-desc">
            Paciente: <strong>{{ pacienteSeleccionado?.nombre }}</strong> (Folio: {{ pacienteSeleccionado?.folio }})<br/>
            Ubicación actual: <strong>{{ doctor.box }}</strong>
          </p>

          <div class="form-group-btg">
            <label>Tipo de Destino / Egreso Clínico *</label>
            <select v-model="tipoEgreso" class="textarea-btg form-select-custom">
              <option value="ALTA_DOMICILIO">Alta Médica a Domicilio (Mejoría clínica)</option>
              <option value="HOSPITALIZACION">Pase a Hospitalización / Cama Censable</option>
              <option value="OBSERVACION">Pase a Sala de Observación Urgencias</option>
              <option value="TRASLADO">Traslado a Otra Unidad Hospitalaria</option>
            </select>
          </div>

          <div class="form-group-btg">
            <label>Motivo o Resumen de Cierre de Consulta *</label>
            <textarea 
              v-model="motivoEgreso"
              rows="3"
              class="textarea-btg"
              placeholder="Indique el motivo de egreso o indicaciones finales al paciente..."
            ></textarea>
          </div>

          <div class="btg-actions">
            <button class="btn-cancel" @click="cerrarModalEgreso">Cancelar</button>
            <button class="btn-confirm-egreso" @click="confirmarEgresoConsulta">Confirmar Egreso y Liberar Sala</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-btg-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-btg-card {
  background: #FFFFFF;
  border-radius: 12px;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.25);
  border: 1px solid #E2E8F0;
  overflow: hidden;
}

.modal-btg-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem 1.25rem;
  background-color: #FEF2F2;
  border-bottom: 1px solid #FEE2E2;
}

.modal-btg-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #EF4444;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.svg-inline-alert {
  width: 22px;
  height: 22px;
}

.modal-btg-header h4 {
  margin: 0;
  font-size: 1rem;
  color: #991B1B;
  font-weight: 700;
}

.modal-btg-header p {
  margin: 0.15rem 0 0 0;
  font-size: 0.75rem;
  color: #B91C1C;
}

.modal-btg-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal-btg-desc {
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.45;
  margin: 0;
}

.form-group-btg label {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 0.35rem;
  text-transform: uppercase;
}

.textarea-btg {
  width: 100%;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 0.6rem;
  font-size: 0.85rem;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.textarea-btg:focus {
  outline: none;
  border-color: #EF4444;
}

.btg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn-cancel {
  background: #F1F5F9;
  color: #475569;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem 1rem;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm-btg {
  background: #DC2626;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.55rem 1rem;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm-btg:hover {
  background: #B91C1C;
}

<style scoped>
.doctor-workspace-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #F8FAFC;
  color: #1E293B;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 340px;
  gap: 1.2rem;
  padding: 1rem clamp(0.75rem, 2vw, 1.5rem);
  flex: 1;
  align-items: start;
}

.workspace-grid.two-columns-layout {
  grid-template-columns: 240px minmax(0, 1fr);
}

.center-workspace {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

@media (max-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 220px 1fr 300px;
    gap: 0.75rem;
  }
}

@media (max-width: 1000px) {
  .workspace-grid {
    grid-template-columns: 220px 1fr;
  }
}

@media (max-width: 768px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
.modal-egreso-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem 1.25rem;
  background-color: #F0FDF4;
  border-bottom: 1px solid #DCFCE7;
}

.modal-egreso-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #16A34A;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-confirm-egreso {
  background-color: #16A34A;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1.25rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-confirm-egreso:hover {
  background-color: #15803D;
}

.form-select-custom {
  height: auto;
  padding: 0.6rem;
  font-family: inherit;
  font-size: 0.85rem;
}

.session-warning-banner-doctor {
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: #fff1f2;
  border-bottom: 2px solid #e11d48;
  padding: 0.85rem 1.5rem;
  box-shadow: 0 2px 4px rgba(225, 29, 72, 0.08);
}

.warning-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e11d48;
  flex-shrink: 0;
}

.warning-icon-box svg {
  width: 20px;
  height: 20px;
}

.warning-text-block strong {
  display: block;
  font-size: 0.85rem;
  color: #9f1239;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.warning-text-block p {
  margin: 0.15rem 0 0 0;
  font-size: 0.78rem;
  color: #be123c;
}

</style>
