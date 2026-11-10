<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Paciente } from '../types'
import api from '@/services/api'
import { eceService, type ExpedienteDetalle } from '@/services/eceService'
import { laboratorioService, type ResultadoDetalleResponse } from '@/services/laboratorioService'
import { pdfExportService } from '@/services/pdfExportService'

const props = defineProps<{
  pacienteSeleccionado: Paciente | null
  expediente?: ExpedienteDetalle | null
}>()

const emit = defineEmits<{
  (e: 'refrescarExpediente'): void
  (e: 'pacienteFusionado', idDefinitivo: string): void
}>()

const exportandoPdf = ref(false)

// Modales y estado para verificación en padrón y fusión de expedientes (PROV-RN-007)
const modalVinculacionVisible = ref(false)
const busquedaPadronTermino = ref('')
const buscandoPadron = ref(false)
const busquedaRealizada = ref(false)
const resultadosPadron = ref<any[]>([])
const pacienteDefinitivoSeleccionado = ref<any | null>(null)
const motivoFusion = ref('')
const procesandoFusion = ref(false)

const esPacienteEmergenciaSinCurp = computed(() => {
  if (!props.pacienteSeleccionado) return false

  // Si proviene de una cita de consulta externa / recepción, NO es paciente de emergencia sin CURP
  if (
    props.pacienteSeleccionado.id_cita || 
    (typeof props.pacienteSeleccionado.id === 'string' && props.pacienteSeleccionado.id.startsWith('CITA-'))
  ) {
    return false
  }

  // Pacientes de Triage de Urgencias ingresados como N.N. o no identificados
  const ident = props.pacienteSeleccionado.identificado ?? props.expediente?.paciente?.identificado
  if (ident === false) return true

  const curp = props.pacienteSeleccionado.curp || props.expediente?.paciente?.curp
  if (!curp || curp === 'NO_PROPORCIONADA' || curp.startsWith('TEMP')) {
    // Si viene de triage y no tiene CURP
    if (props.pacienteSeleccionado.id_triage) return true
    return false
  }
  return false
})

const abrirModalVinculacion = () => {
  modalVinculacionVisible.value = true
  busquedaPadronTermino.value = ''
  resultadosPadron.value = []
  busquedaRealizada.value = false
  pacienteDefinitivoSeleccionado.value = null
  motivoFusion.value = ''
}

const cerrarModalVinculacion = () => {
  modalVinculacionVisible.value = false
}

const buscarEnPadron = async () => {
  if (!busquedaPadronTermino.value || busquedaPadronTermino.value.trim().length < 2) {
    alert('Ingrese al menos 2 caracteres para buscar en el padrón central.')
    return
  }
  buscandoPadron.value = true
  busquedaRealizada.value = true
  try {
    const list = await eceService.buscarPadronCentral(busquedaPadronTermino.value.trim())
    const idActual = props.pacienteSeleccionado?.id_paciente || props.pacienteSeleccionado?.id
    resultadosPadron.value = (list || []).filter((p: any) => p.id_paciente !== idActual)
  } catch (err: any) {
    alert('Error al consultar padrón central: ' + (err?.response?.data?.message || err.message))
  } finally {
    buscandoPadron.value = false
  }
}

const seleccionarPacienteDefinitivo = (p: any) => {
  pacienteDefinitivoSeleccionado.value = p
}

const ejecutarFusion = async () => {
  const idTemporal = props.pacienteSeleccionado?.id_paciente || props.pacienteSeleccionado?.id
  const idDefinitivo = pacienteDefinitivoSeleccionado.value?.id_paciente
  if (!idTemporal || !idDefinitivo) {
    alert('Error: Debe seleccionar un paciente definitivo del padrón.')
    return
  }
  if (!motivoFusion.value || motivoFusion.value.trim().length < 10) {
    alert('La justificación médica y legal de la fusión debe tener al menos 10 caracteres.')
    return
  }

  procesandoFusion.value = true
  try {
    const res = await eceService.fusionarPacientes(idTemporal, idDefinitivo, motivoFusion.value.trim())
    alert(`Expediente unificado exitosamente (PROV-RN-007).\n\nPaciente Definitivo: ${pacienteDefinitivoSeleccionado.value.nombre_completo}\nCURP: ${pacienteDefinitivoSeleccionado.value.curp}\nTriages reasignados: ${res.triages_migrados || 1}\nExpedientes consolidados: ${res.expedientes_migrados || 1}\n\nLos antecedentes previos, citas pasadas, recetas y solicitudes de laboratorio generadas se han consolidado en el expediente central.`)
    cerrarModalVinculacion()
    emit('pacienteFusionado', idDefinitivo)
  } catch (err: any) {
    alert('Error al fusionar expedientes: ' + (err?.response?.data?.message || err.message))
  } finally {
    procesandoFusion.value = false
  }
}

const asegurarIdExpediente = async (): Promise<string | undefined> => {
  let idExp: string | undefined = props.expediente?.id_expediente || props.pacienteSeleccionado?.id_expediente
  if (idExp) return idExp

  const p = props.pacienteSeleccionado
  if (!p) return undefined

  // 1. Intentar resolver desde /pacientes por CURP o Nombre
  const termino = p.curp || p.nombre
  if (termino) {
    try {
      const res = await api.get('/pacientes', { params: { q: termino } })
      const lista = res.data?.data?.data || res.data?.data || []
      if (Array.isArray(lista) && lista.length > 0) {
        const pacBD = lista[0]
        if (pacBD.id_paciente && !p.id_paciente) {
          p.id_paciente = pacBD.id_paciente
        }
        if (pacBD.expediente?.id_expediente) {
          idExp = pacBD.expediente.id_expediente
          p.id_expediente = idExp
          p.ece = 'ECE-' + idExp
          return idExp
        }
      }
    } catch (e) {
      console.warn('Error resolviendo paciente en ClinicalRecordSection:', e)
    }
  }

  // 2. Si tiene id_paciente pero aún no tiene expediente registrado, crearlo
  if (p.id_paciente) {
    try {
      const nuevo = await eceService.crearExpediente({
        id_paciente: p.id_paciente,
        motivo_consulta: p.motivo || 'Atención Médica'
      })
      idExp = nuevo.id_expediente
      p.id_expediente = idExp
      p.ece = 'ECE-' + idExp
      return idExp
    } catch (e: any) {
      if (e?.response?.data?.id_expediente_existente) {
        idExp = e.response.data.id_expediente_existente
        p.id_expediente = idExp
        p.ece = 'ECE-' + idExp
        return idExp
      }
    }
  }

  return undefined
}

const exportarPdfAuditado = async () => {
  const idExp = await asegurarIdExpediente()
  if (!idExp) {
    alert('Atención: El paciente debe tener un expediente clínico aperturado para exportar el documento.')
    return
  }

  exportandoPdf.value = true
  try {
    await pdfExportService.descargarExpedientePdf(idExp)
    alert(`Expediente clínico oficial con sellos de agua dinámicos de auditoría descargado exitosamente.\n\nFolio Expediente: ${idExp}\nMarca de Agua: "COPIA FIDEDIGNA DEL EXPEDIENTE"\n\nOperación registrada formalmente en bitácora inalterable AUDITORIA_I.`)
  } catch (err: any) {
    alert('Error al exportar expediente clínico en PDF: ' + (err?.response?.data?.message || err.message))
  } finally {
    exportandoPdf.value = false
  }
}

const agregarNotaAclaratoria = async (idNota: string) => {
  const idExp = await asegurarIdExpediente()
  if (!idExp) return

  const justificacion = prompt('Ingrese el motivo/justificación de la aclaración a la nota cerrada:')
  if (!justificacion || justificacion.trim().length < 5) {
    alert('La justificación es obligatoria.')
    return
  }

  const contenido = prompt('Ingrese la narrativa clínica de la nota aclaratoria:')
  if (!contenido || contenido.trim().length < 10) {
    alert('La nota aclaratoria debe contener al menos 10 caracteres.')
    return
  }

  try {
    const res = await eceService.guardarNotaAclaratoria(idExp, idNota, {
      contenido: contenido.trim(),
      justificacion: justificacion.trim()
    })
    alert(`Nota aclaratoria registrada exitosamente (ID: ${res.id_nota}).\nVinculada inalterablemente a la nota médica original.`)
    emit('refrescarExpediente')
  } catch (err: any) {
    alert('Error al registrar nota aclaratoria: ' + (err?.response?.data?.message || err.message))
  }
}

const agregarAntecedenteRapido = async () => {
  const idExp = await asegurarIdExpediente()
  if (!idExp) {
    alert('Atención: El paciente debe tener un expediente clínico aperturado para registrar antecedentes.')
    return
  }

  const tipo = prompt('Tipo de antecedente (ALERGIA, PATOLOGICO, HEREDOFAMILIAR, NO_PATOLOGICO, GINECOOBSTETRICO):', 'ALERGIA')?.trim().toUpperCase()
  if (!tipo) return

  const tiposValidos = ['ALERGIA', 'PATOLOGICO', 'HEREDOFAMILIAR', 'NO_PATOLOGICO', 'GINECOOBSTETRICO']
  if (!tiposValidos.includes(tipo)) {
    alert('Tipo no válido. Debe ser uno de: ' + tiposValidos.join(', '))
    return
  }

  const desc = prompt(`Descripción del antecedente (${tipo}):`)
  if (!desc || desc.trim().length < 3) {
    alert('La descripción debe contener al menos 3 caracteres.')
    return
  }

  try {
    await eceService.registrarAntecedente(idExp, {
      tipo: tipo as any,
      descripcion: desc.trim()
    })
    alert(`Antecedente ${tipo} registrado exitosamente en el expediente clínico.`)
    emit('refrescarExpediente')
  } catch (err: any) {
    alert('Error al registrar antecedente: ' + (err?.response?.data?.message || err.message))
  }
}

// Estudios de Laboratorio e Imagen vinculados al expediente (Sprint 4 / RF-010)
const solicitudesEstudio = computed(() => {
  return props.expediente?.solicitudes_estudio || (props.expediente as any)?.solicitudesEstudio || []
})

const modalResultadoVisible = ref(false)
const cargandoResultado = ref(false)
const resultadoActivo = ref<ResultadoDetalleResponse | null>(null)
const estudioActivo = ref<any>(null)

const consultarResultadoEstudio = async (solicitud: any) => {
  estudioActivo.value = solicitud
  cargandoResultado.value = true
  modalResultadoVisible.value = true
  try {
    const data = await laboratorioService.obtenerResultado(solicitud.id_solicitud)
    resultadoActivo.value = data
  } catch (err: any) {
    alert('Error al consultar resultado: ' + (err?.response?.data?.message || err.message))
    modalResultadoVisible.value = false
  } finally {
    cargandoResultado.value = false
  }
}
</script>

<template>
  <div class="expediente-flow">
    <!-- Banner Paciente -->
    <div class="patient-banner-card">
      <div class="breadcrumbs">
        Atención clínica / Expediente clínico electrónico / 
        <span v-if="pacienteSeleccionado" class="status-open"><svg viewBox="0 0 24 24" class="svg-inline-folder" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg> Expediente abierto · {{ pacienteSeleccionado.folio }}</span>
        <span v-else class="status-empty"><svg viewBox="0 0 24 24" class="svg-inline-folder" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg> Esperando selección de paciente</span>
      </div>

      <div v-if="pacienteSeleccionado" class="patient-main-row">
        <div class="patient-avatar-square">
          {{ pacienteSeleccionado.nombre.split(' ').map(n => n[0]).slice(0, 2).join('') }}
        </div>
        <div class="patient-meta">
          <div class="patient-title-row">
            <h2>{{ pacienteSeleccionado.nombre }}</h2>
            <span v-if="esPacienteEmergenciaSinCurp" class="pill-warning-curp">
              EMERGENCIA · SIN CURP OFICIAL
            </span>
            <span v-else-if="pacienteSeleccionado.curp" class="pill-curp-verified">
              CURP: {{ pacienteSeleccionado.curp }}
            </span>
          </div>
          <p>{{ pacienteSeleccionado.sexo }} · {{ pacienteSeleccionado.edad }} años · <strong>ECE {{ pacienteSeleccionado.id_expediente || pacienteSeleccionado.folio }}</strong> · Urgencias · {{ pacienteSeleccionado.box }}</p>
        </div>
        <div class="patient-actions">
          <button 
            class="btn-vincular-padron" 
            :class="{ highlight: esPacienteEmergenciaSinCurp }"
            title="Verificar si el paciente cuenta con registro previo en el padrón central para vincular antecedentes, citas y laboratorios"
            @click="abrirModalVinculacion"
          >
            <svg viewBox="0 0 24 24" class="svg-inline-link" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
            </svg>
            {{ esPacienteEmergenciaSinCurp ? 'Verificar Padrón y Vincular Historial' : 'Verificar / Fusionar con Padrón' }}
          </button>
          <button 
            v-if="pacienteSeleccionado?.id_expediente || expediente?.id_expediente" 
            class="btn-export-pdf" 
            :disabled="exportandoPdf"
            @click="exportarPdfAuditado"
          >
            <svg viewBox="0 0 24 24" class="svg-inline-pdf" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            {{ exportandoPdf ? 'Exportando...' : 'Exportar PDF Auditado' }}
          </button>
          <span class="pill-badge-active">En atención</span>
        </div>
      </div>

      <!-- Si NO hay paciente seleccionado -->
      <div v-else class="empty-patient-box">
        <div class="empty-icon"><svg viewBox="0 0 24 24" class="svg-inline-user" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></div>
        <div>
          <strong>Ningún paciente en consulta</strong>
          <p>Haga clic en uno de los pacientes de la lista izquierda para cargar sus signos vitales e historial.</p>
        </div>
      </div>

      <!-- ALERTA DE PACIENTE DE EMERGENCIA SIN CURP (SOLO SI APLICA) -->
      <div v-if="pacienteSeleccionado && esPacienteEmergenciaSinCurp" class="emergency-banner-alert">
        <div class="alert-content">
          <svg viewBox="0 0 24 24" class="svg-alert-icon" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <div>
            <strong>Paciente admitido de emergencia sin CURP oficial (Expediente Provisional)</strong>
            <p>Puede verificar si el paciente ya cuenta con antecedentes clínicos, citas previas o historial en el padrón central para transferir y consolidar inalterablemente todas las notas y estudios de laboratorio generados.</p>
          </div>
        </div>
        <button class="btn-alert-action" @click="abrirModalVinculacion">
          Verificar en Padrón Central
        </button>
      </div>
    </div>

    <!-- Signos Vitales -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Signos vitales</h4>
        <span v-if="pacienteSeleccionado?.signos" class="card-timestamp">Última toma · {{ pacienteSeleccionado.signos.horaToma }}</span>
        <span v-else class="card-timestamp">Pendiente de toma por Enfermería</span>
      </div>

      <div v-if="pacienteSeleccionado?.signos" class="vitals-grid">
        <div class="vital-box">
          <span class="vital-label">Presión arterial</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.pa }}</strong> <small>mmHg</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Frecuencia cardíaca</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.fc }}</strong> <small>lpm</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Temperatura</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.temp }}</strong> <small>°C</small></div>
        </div>
        <div class="vital-box">
          <span class="vital-label">Saturación O₂</span>
          <div class="vital-value"><strong>{{ pacienteSeleccionado.signos.spo2 }}</strong> <small>%</small></div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon"><svg viewBox="0 0 24 24" class="svg-inline-chart" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg></span>
        <strong>Signos Vitales Pendientes de Somatometría</strong>
        <p>Aún no se han registrado signos vitales en el filtro previo de Enfermería para este paciente. Puede capturarlos directamente durante su exploración física en la Nota Médica ECE.</p>
      </div>
    </section>

    <!-- Resumen y Antecedentes -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Resumen y antecedentes</h4>
        <div class="card-header-actions">
          <button 
            v-if="pacienteSeleccionado?.id_expediente || expediente?.id_expediente"
            class="btn-aclaratoria"
            @click="agregarAntecedenteRapido"
          >
            + Registrar Antecedente
          </button>
          <span class="card-timestamp">NOM-004-SSA3-2012</span>
        </div>
      </div>

      <div v-if="pacienteSeleccionado?.antecedentes || (expediente?.antecedentes && expediente.antecedentes.length > 0)" class="summary-grid">
        <div class="summary-col">
          <h5>Motivo de consulta</h5>
          <p>{{ pacienteSeleccionado?.antecedentes?.motivo || expediente?.motivo_consulta || pacienteSeleccionado?.motivo || 'Atención en Consulta Externa' }}</p>
          
          <div v-if="expediente?.fecha_ultimo_acto_medico" class="margin-top-xs">
            <small class="text-subtle">Último acto médico registrado: {{ new Date(expediente.fecha_ultimo_acto_medico).toLocaleString('es-MX') }}</small>
          </div>
        </div>

        <div class="summary-col">
          <h5>Antecedentes clínicos registrados</h5>

          <!-- Si existen antecedentes detallados en el expediente -->
          <div v-if="expediente?.antecedentes && expediente.antecedentes.length > 0" class="antecedentes-list-stack">
            <div 
              v-for="ant in expediente.antecedentes" 
              :key="ant.id_antecedente" 
              class="antecedente-item-pill"
              :class="{
                'is-alergia': ant.tipo === 'ALERGIA',
                'is-patologico': ant.tipo === 'PATOLOGICO',
                'is-heredofamiliar': ant.tipo === 'HEREDOFAMILIAR'
              }"
            >
              <span class="ant-badge-tipo">{{ ant.tipo }}</span>
              <strong class="ant-desc">{{ ant.descripcion }}</strong>
              <small v-if="ant.observaciones" class="ant-obs">({{ ant.observaciones }})</small>
            </div>
          </div>

          <!-- Si viene mapeado desde el objeto de antecedentes -->
          <div v-else-if="pacienteSeleccionado?.antecedentes">
            <div class="alert-box-allergy">
              <strong>Alergias:</strong>
              <span>{{ pacienteSeleccionado.antecedentes.alergias }}</span>
            </div>
            <p><strong>Crónicos:</strong> {{ pacienteSeleccionado.antecedentes.cronicos }}</p>
            <p><strong>Medicación:</strong> {{ pacienteSeleccionado.antecedentes.medicacion }}</p>
          </div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon"><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg></span>
        <strong>Sin antecedentes clínicos registrados aún</strong>
        <p>Haga clic en "+ Registrar Antecedente" para asentar alergias, patologías o antecedentes heredofamiliares en la base de datos oficial.</p>
        <button 
          v-if="pacienteSeleccionado?.id_expediente || expediente?.id_expediente"
          class="btn-aclaratoria margin-top-xs"
          @click="agregarAntecedenteRapido"
        >
          + Registrar Primer Antecedente
        </button>
      </div>
    </section>

    <!-- Historial Médico -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Historial médico y notas inmutables</h4>
        <span class="card-timestamp">Trazabilidad Inalterable</span>
      </div>

      <!-- Si el expediente cargado desde el backend contiene notas médicas -->
      <div v-if="expediente?.notas_medicas && expediente.notas_medicas.length > 0" class="notes-timeline-container">
        <div v-for="nota in expediente.notas_medicas" :key="nota.id_nota" class="medical-note-content mb-3">
          <div class="note-top-bar">
            <div>
              <strong>Nota de {{ nota.tipo }}</strong><br/>
              <small class="note-author">Médico: {{ nota.id_usuario }} · Folio {{ nota.id_nota }} · {{ new Date(nota.fecha_hora_creacion).toLocaleString('es-MX') }}</small>
            </div>
            <div class="note-status-actions">
              <span class="pill-locked">
                <svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg> 
                {{ nota.cerrada ? 'Firmada · Solo lectura' : 'Borrador' }}
              </span>
              <button 
                v-if="nota.cerrada && !nota.nota_aclaratoria" 
                class="btn-aclaratoria"
                @click="agregarNotaAclaratoria(nota.id_nota)"
              >
                + Nota Aclaratoria
              </button>
            </div>
          </div>
          <p class="note-text">{{ nota.contenido }}</p>
          <div v-if="nota.plan_tratamiento" class="note-plan">
            <strong>Plan de Tratamiento / Indicaciones:</strong>
            <p>{{ nota.plan_tratamiento }}</p>
          </div>

          <!-- Si la nota tiene una aclaratoria vinculada -->
          <div v-if="nota.nota_aclaratoria" class="aclaratoria-box">
            <div class="aclaratoria-header">
              <svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              <strong>Nota Aclaratoria Vinculada (ID: {{ nota.nota_aclaratoria.id_nota }})</strong>
              <small>{{ new Date(nota.nota_aclaratoria.fecha_hora_creacion).toLocaleString('es-MX') }}</small>
            </div>
            <p><strong>Justificación:</strong> {{ nota.nota_aclaratoria.justificacion }}</p>
            <p><strong>Contenido:</strong> {{ nota.nota_aclaratoria.contenido }}</p>
          </div>

          <div class="nom004-footer">
            <div class="nom004-info">
              <strong>Documento bloqueado · Hash SHA-256:</strong>
              <code>{{ nota.hash_sha256?.substring(0, 32) }}...</code>
            </div>
          </div>
        </div>
      </div>

      <!-- Fallback con última nota local si aún no hay notas en backend -->
      <div v-else-if="pacienteSeleccionado?.ultimaNota" class="medical-note-content">
        <div class="note-top-bar">
          <div>
            <strong>{{ pacienteSeleccionado.ultimaNota.titulo }}</strong><br/>
            <small class="note-author">{{ pacienteSeleccionado.ultimaNota.medico }} · Folio {{ pacienteSeleccionado.ultimaNota.folio }}</small>
          </div>
          <span class="pill-locked"><svg viewBox="0 0 24 24" class="svg-inline-lock" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg> Firmada · Solo lectura</span>
        </div>
        <p class="note-text">{{ pacienteSeleccionado.ultimaNota.contenido }}</p>
        <div class="nom004-footer">
          <div class="nom004-info">
            <strong>Documento bloqueado · NOM-004</strong>
            <span>Las adendas se realizan exclusivamente mediante Nota Aclaratoria.</span>
          </div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon"><svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
        <strong>Historial Clínico (Sin notas registradas)</strong>
        <p>Las notas médicas de evolución se integrarán automáticamente en este panel al asentar la primera nota.</p>
      </div>
    </section>

    <!-- Auxiliares Diagnósticos (Laboratorio e Imagenología) -->
    <section class="clinical-card">
      <div class="card-header">
        <h4>Auxiliares diagnósticos (Laboratorio e Imagenología)</h4>
        <span class="card-timestamp">NOM-024 / NOM-007</span>
      </div>

      <div v-if="solicitudesEstudio.length > 0" class="lab-studies-list">
        <div 
          v-for="sol in solicitudesEstudio" 
          :key="sol.id_solicitud"
          class="lab-study-row"
          :class="{
            'border-stat': sol.prioridad === 'URGENTE',
            'border-done': sol.estado === 'COMPLETADA',
            'border-rejected': sol.estado === 'RECHAZADA'
          }"
        >
          <div class="lab-study-main">
            <div class="lab-study-header">
              <strong class="lab-study-name">{{ sol.tipo_estudio }}</strong>
              <span 
                class="badge-pill"
                :class="{
                  'badge-red': sol.prioridad === 'URGENTE',
                  'badge-gray': sol.prioridad !== 'URGENTE'
                }"
              >
                {{ sol.prioridad }}
              </span>
              <span 
                class="status-pill"
                :class="{
                  'status-pending': sol.estado === 'PENDIENTE',
                  'status-progress': sol.estado === 'EN_PROCESO',
                  'status-done': sol.estado === 'COMPLETADA',
                  'status-rejected': sol.estado === 'RECHAZADA'
                }"
              >
                {{ sol.estado }}
              </span>
            </div>
            <div class="lab-study-meta">
              <span>Folio: {{ sol.id_solicitud.substring(0, 8).toUpperCase() }}</span>
              <span>·</span>
              <span>Solicitado: {{ new Date(sol.fecha_hora_solicitud).toLocaleString('es-MX') }}</span>
              <span v-if="sol.indicaciones_clinicas">· {{ sol.indicaciones_clinicas }}</span>
            </div>
          </div>

          <div class="lab-study-actions">
            <button
              v-if="sol.estado === 'COMPLETADA' || sol.resultado"
              type="button"
              class="btn-view-result"
              @click="consultarResultadoEstudio(sol)"
            >
              <svg viewBox="0 0 24 24" class="btn-svg-mini" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <span>Ver Resultado Cifrado</span>
            </button>
            <span v-else-if="sol.estado === 'EN_PROCESO'" class="tag-in-lab">
              En proceso en analizador
            </span>
            <span v-else-if="sol.estado === 'PENDIENTE'" class="tag-in-queue">
              En cola de laboratorio
            </span>
            <span v-else-if="sol.estado === 'RECHAZADA'" class="tag-rejected">
              Muestra rechazada
            </span>
          </div>
        </div>
      </div>

      <div v-else class="placeholder-box">
        <span class="placeholder-icon">
          <svg viewBox="0 0 24 24" class="svg-inline-doc" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 2v7.31L4.5 19.5A2 2 0 0 0 6.23 22h11.54a2 2 0 0 0 1.73-2.5L14 9.31V2"/>
            <line x1="8.5" y1="2" x2="15.5" y2="2"/>
          </svg>
        </span>
        <strong>Sin solicitudes de laboratorio activas</strong>
        <p>Utilice la pestaña "Solicitar Laboratorio" para ordenar biometrías, químicas, gasometrías o radiografías.</p>
      </div>
    </section>

    <!-- MODAL DE CONSULTA Y DESCIFRADO DE RESULTADOS (AES-256-GCM) -->
    <div v-if="modalResultadoVisible" class="modal-btg-backdrop">
      <div class="modal-btg-card modal-lab-card">
        <div class="modal-btg-header">
          <div class="modal-btg-icon">
            <svg viewBox="0 0 24 24" class="svg-inline-alert" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <div>
            <h4>Resultado de Estudio Diagnóstico Descifrado</h4>
            <p>NOM-024-SSA3-2012 · Cifrado Militar AES-256-GCM / SHA-256 Verificado</p>
          </div>
        </div>

        <div class="modal-btg-body">
          <div v-if="cargandoResultado" class="loading-box">
            <span>Descifrando informe clínico mediante clave institucional...</span>
          </div>

          <div v-else-if="resultadoActivo" class="result-details">
            <div class="result-header-row">
              <div>
                <strong>Estudio: {{ estudioActivo?.tipo_estudio }}</strong>
                <p class="text-muted">Folio: {{ estudioActivo?.id_solicitud }}</p>
              </div>
              <div class="badge-aes">AES-256-GCM OK</div>
            </div>

            <div class="result-content-box">
              <span class="content-lbl">Informe analítico validado:</span>
              <pre class="content-text">{{ resultadoActivo.contenido_descifrado }}</pre>
            </div>

            <div v-if="resultadoActivo.resultado?.observaciones" class="obs-box">
              <strong>Observaciones técnicas del laboratorio:</strong>
              <p>{{ resultadoActivo.resultado.observaciones }}</p>
            </div>

            <div class="sha-box">
              <strong>Integridad Criptográfica SHA-256:</strong>
              <code>{{ resultadoActivo.integridad_sha256 }}</code>
            </div>

            <div class="btg-actions">
              <button class="btn-confirm-btg" @click="modalResultadoVisible = false">Cerrar Informe</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL DE VERIFICACION EN PADRON Y FUSION DE EXPEDIENTE (PROV-RN-007) -->
    <div v-if="modalVinculacionVisible" class="modal-overlay-fusion" @click.self="cerrarModalVinculacion">
      <div class="modal-fusion-card">
        <div class="modal-fusion-header">
          <div>
            <span class="protocol-badge">NOM-004-SSA3-2012 · PROV-RN-007</span>
            <h3>Verificar Padrón Central y Vincular Historial</h3>
            <p class="modal-fusion-subtitle">
              Localice al paciente en el padrón central hospitalario. Al confirmar la vinculación, todas las notas médicas, recetas y estudios de laboratorio generados durante esta consulta se unificarán inalterablemente a su expediente institucional.
            </p>
          </div>
          <button class="btn-close-modal" @click="cerrarModalVinculacion">✕</button>
        </div>

        <!-- BARRA DE BUSQUEDA EN PADRON -->
        <div class="fusion-search-bar">
          <div class="search-input-wrapper">
            <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input 
              v-model="busquedaPadronTermino" 
              type="text" 
              placeholder="Buscar por CURP, nombre completo o No. Expediente..."
              @keyup.enter="buscarEnPadron"
            />
          </div>
          <button class="btn-buscar-padron" :disabled="buscandoPadron" @click="buscarEnPadron">
            {{ buscandoPadron ? 'Buscando...' : 'Buscar en Padrón' }}
          </button>
        </div>

        <!-- CUERPO DE RESULTADOS -->
        <div class="fusion-body">
          <div class="fusion-section-title">
            <span>Resultados en Padrón Hospitalario Central</span>
            <span v-if="resultadosPadron.length > 0" class="results-count">{{ resultadosPadron.length }} paciente(s) encontrado(s)</span>
          </div>

          <div v-if="buscandoPadron" class="fusion-loading">
            Buscando registros en la base central...
          </div>

          <div v-else-if="resultadosPadron.length === 0 && busquedaRealizada" class="fusion-empty">
            No se localizaron registros activos con el término ingresado. Verifique el nombre o los 18 caracteres de la CURP.
          </div>

          <div v-else-if="resultadosPadron.length > 0" class="fusion-results-list">
            <div 
              v-for="pDef in resultadosPadron" 
              :key="pDef.id_paciente"
              class="fusion-result-item"
              :class="{ selected: pacienteDefinitivoSeleccionado?.id_paciente === pDef.id_paciente }"
              @click="seleccionarPacienteDefinitivo(pDef)"
            >
              <div class="pdef-info">
                <div class="pdef-name-row">
                  <strong>{{ pDef.nombre_completo }}</strong>
                  <span v-if="pDef.identificado" class="badge-verif">Identidad Verificada</span>
                </div>
                <div class="pdef-meta">
                  <span>CURP: <code>{{ pDef.curp || 'Sin CURP' }}</code></span>
                  <span>Sexo: {{ pDef.sexo }}</span>
                  <span v-if="pDef.fecha_nacimiento">F. Nac: {{ pDef.fecha_nacimiento }}</span>
                  <span v-if="pDef.expediente?.id_expediente">Expediente: <strong>{{ pDef.expediente.id_expediente }}</strong></span>
                </div>
              </div>
              <div class="pdef-action">
                <span v-if="pacienteDefinitivoSeleccionado?.id_paciente === pDef.id_paciente" class="badge-selected">
                  Seleccionado
                </span>
                <button v-else class="btn-select-pdef">
                  Seleccionar
                </button>
              </div>
            </div>
          </div>

          <!-- PANEL DE COMPARACION Y MOTIVO DE FUSION -->
          <div v-if="pacienteDefinitivoSeleccionado" class="fusion-confirm-box">
            <h4>Consolidación y Fusión de Expediente Clínico</h4>
            <div class="fusion-comparison-grid">
              <div class="comp-col comp-temp">
                <span class="comp-tag">EXPEDIENTE TEMPORAL (CONSULTA ACTUAL)</span>
                <div class="comp-title">{{ pacienteSeleccionado?.nombre }}</div>
                <p>Folio: {{ pacienteSeleccionado?.folio }}</p>
                <p>Atención: {{ pacienteSeleccionado?.triage || 'Emergencias' }}</p>
                <p class="comp-subtext">Notas, recetas y solicitudes generadas hoy serán transferidas.</p>
              </div>
              <div class="comp-arrow">
                <svg viewBox="0 0 24 24" class="svg-inline-arrow" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>
                </svg>
              </div>
              <div class="comp-col comp-def">
                <span class="comp-tag">EXPEDIENTE DEFINITIVO (PADRON CENTRAL)</span>
                <div class="comp-title">{{ pacienteDefinitivoSeleccionado.nombre_completo }}</div>
                <p>CURP: <code>{{ pacienteDefinitivoSeleccionado.curp }}</code></p>
                <p>Expediente Central: {{ pacienteDefinitivoSeleccionado.expediente?.id_expediente || 'Se consolidará' }}</p>
                <p class="comp-subtext">Se conservan citas previas, antecedentes clínicos e historial.</p>
              </div>
            </div>

            <div class="motivo-field">
              <label>Justificación Médica y Legal de la Fusión (NOM-004 / PROV-RN-007) *</label>
              <textarea 
                v-model="motivoFusion" 
                rows="3" 
                placeholder="Indique la justificación asistencial (ej: Identidad acreditada con documento oficial por familiar; se unifican notas clínicas de emergencia con expediente definitivo institucional)..."
              ></textarea>
            </div>

            <div class="fusion-actions">
              <button class="btn-cancel" @click="cerrarModalVinculacion">Cancelar</button>
              <button 
                class="btn-confirm-fusion" 
                :disabled="procesandoFusion || motivoFusion.trim().length < 10"
                @click="ejecutarFusion"
              >
                {{ procesandoFusion ? 'Registrando en Auditoría Inalterable...' : 'Confirmar Vinculación y Fusión de Historial' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.expediente-flow {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.patient-banner-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.breadcrumbs {
  font-size: 0.72rem;
  color: #64748B;
  margin-bottom: 0.6rem;
}

.status-open {
  color: #10B981;
  font-weight: 600;
}

.status-empty {
  color: #94A3B8;
  font-style: italic;
}

.patient-main-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.patient-avatar-square {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background-color: #E0F2FE;
  color: #0369A1;
  font-weight: 800;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.patient-meta h2 {
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0;
  color: #1E293B;
}

.patient-meta p {
  font-size: 0.78rem;
  color: #64748B;
  margin: 0.2rem 0 0 0;
}

.patient-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.btn-export-pdf {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background-color: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-export-pdf:hover {
  background-color: #0369A1;
}

.svg-inline-pdf {
  width: 14px;
  height: 14px;
}

.pill-badge-active {
  background-color: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.3rem 0.6rem;
  border-radius: 9999px;
}

.empty-patient-box {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 0.85rem;
  font-size: 0.8rem;
}

.empty-icon {
  font-size: 1.8rem;
}

.empty-patient-box strong {
  display: block;
  color: #1E293B;
}

.empty-patient-box p {
  margin: 0;
  color: #64748B;
  font-size: 0.75rem;
}

/* CARDS CLÍNICAS */
.clinical-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1rem 1.25rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
}

.card-header h4 {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0;
}

.card-timestamp {
  font-size: 0.72rem;
  color: #94A3B8;
}

.vitals-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.vital-box {
  display: flex;
  flex-direction: column;
}

.vital-label {
  font-size: 0.7rem;
  color: #64748B;
  margin-bottom: 0.2rem;
}

.vital-value {
  font-size: 1.25rem;
  color: #1E293B;
}

.vital-value small {
  font-size: 0.72rem;
  color: #94A3B8;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  font-size: 0.8rem;
  line-height: 1.4;
}

.summary-col h5 {
  font-size: 0.78rem;
  font-weight: 700;
  color: #475569;
  margin: 0 0 0.4rem 0;
}

.alert-box-allergy {
  background-color: #FFF1F2;
  border: 1px solid #FFE4E6;
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  margin-bottom: 0.5rem;
  color: #E11D48;
}

.antecedentes-list-stack {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 0.5rem;
}

.antecedente-item-pill {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.6rem;
  border-radius: 6px;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  font-size: 0.78rem;
  flex-wrap: wrap;
}

.antecedente-item-pill.is-alergia {
  background-color: #FFF1F2;
  border-color: #FECDD3;
  color: #9F1239;
}

.antecedente-item-pill.is-patologico {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: #1E40AF;
}

.antecedente-item-pill.is-heredofamiliar {
  background-color: #F0FDF4;
  border-color: #BBF7D0;
  color: #166534;
}

.ant-badge-tipo {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.08);
  letter-spacing: 0.02em;
}

.ant-desc {
  font-weight: 600;
}

.ant-obs {
  font-style: italic;
  opacity: 0.85;
}

/* PLACEHOLDER */
.placeholder-box {
  background-color: #F8FAFC;
  border: 1px dashed #CBD5E1;
  border-radius: 8px;
  padding: 1.25rem;
  text-align: center;
}

.placeholder-icon {
  font-size: 1.8rem;
  display: block;
  margin-bottom: 0.3rem;
}

.placeholder-box strong {
  display: block;
  font-size: 0.85rem;
  color: #334155;
  margin-bottom: 0.2rem;
}

.placeholder-box p {
  font-size: 0.75rem;
  color: #64748B;
  margin: 0;
}

/* NOTA CLÍNICA */
.medical-note-content {
  background-color: #F8FAFC;
  border: 1px solid #EDF2F7;
  border-radius: 8px;
  padding: 0.85rem;
}

.note-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.note-author {
  color: #64748B;
  font-size: 0.7rem;
}

.pill-locked {
  background-color: #F1F5F9;
  color: #475569;
  font-size: 0.7rem;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.note-text {
  font-size: 0.8rem;
  line-height: 1.45;
  color: #334155;
  margin-bottom: 0.8rem;
}

.note-status-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-aclaratoria {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  color: #0284C7;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  cursor: pointer;
}

.btn-aclaratoria:hover {
  background-color: #F0F9FF;
  border-color: #0284C7;
}

.note-plan {
  background-color: #F8FAFC;
  border-left: 3px solid #0284C7;
  padding: 0.5rem 0.75rem;
  border-radius: 0 4px 4px 0;
  font-size: 0.78rem;
  margin-bottom: 0.75rem;
}

.note-plan strong {
  color: #0369A1;
  display: block;
  margin-bottom: 0.2rem;
}

.note-plan p {
  margin: 0;
  color: #334155;
}

.aclaratoria-box {
  background-color: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 6px;
  padding: 0.65rem 0.85rem;
  font-size: 0.78rem;
  margin-bottom: 0.75rem;
}

.aclaratoria-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: #92400E;
  margin-bottom: 0.35rem;
}

.aclaratoria-header small {
  color: #B45309;
  margin-left: auto;
}

.mb-3 {
  margin-bottom: 1rem;
}

.nom004-footer {
  border-top: 1px solid #E2E8F0;
  padding-top: 0.6rem;
  font-size: 0.72rem;
}

.nom004-info strong {
  display: block;
  color: #1E293B;
}

.nom004-info span {
  color: #64748B;
}

@media (max-width: 768px) {
  .vitals-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

.lab-studies-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.lab-study-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 1rem;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-left: 4px solid #0284C7;
  border-radius: 6px;
}

.lab-study-row.border-stat {
  border-left-color: #DC2626;
  background: #FFFBFB;
}

.lab-study-row.border-done {
  border-left-color: #10B981;
}

.lab-study-row.border-rejected {
  border-left-color: #F59E0B;
  background: #FFFDF5;
}

.lab-study-main {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.lab-study-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.lab-study-name {
  font-size: 0.95rem;
  color: #0F172A;
  font-weight: 700;
}

.lab-study-meta {
  display: flex;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: #64748B;
}

.badge-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.badge-red {
  background: #FEE2E2;
  color: #DC2626;
}

.badge-gray {
  background: #F1F5F9;
  color: #475569;
}

.status-pill {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.status-pending {
  background: #FEF3C7;
  color: #D97706;
}

.status-progress {
  background: #E0E7FF;
  color: #4338CA;
}

.status-done {
  background: #D1FAE5;
  color: #059669;
}

.status-rejected {
  background: #FEE2E2;
  color: #B91C1C;
}

.btn-view-result {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  background: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 5px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-view-result:hover {
  background: #0369A1;
}

.btn-svg-mini {
  width: 14px;
  height: 14px;
}

.tag-in-lab {
  font-size: 0.75rem;
  color: #4338CA;
  font-weight: 600;
}

.tag-in-queue {
  font-size: 0.75rem;
  color: #D97706;
  font-weight: 600;
}

.tag-rejected {
  font-size: 0.75rem;
  color: #B91C1C;
  font-weight: 600;
}

.modal-btg-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-btg-card {
  background-color: #FFFFFF;
  border-radius: 12px;
  width: 90%;
  max-width: 580px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.modal-btg-header {
  background-color: #0F172A;
  color: #FFFFFF;
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.modal-btg-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #0284C7;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
}

.modal-btg-icon svg {
  width: 22px;
  height: 22px;
}

.modal-btg-header h4 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
}

.modal-btg-header p {
  margin: 0.2rem 0 0 0;
  font-size: 0.75rem;
  color: #94A3B8;
}

.modal-btg-body {
  padding: 1.25rem 1.5rem;
}

.result-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #E2E8F0;
}

.badge-aes {
  font-size: 0.72rem;
  font-weight: 700;
  background: #D1FAE5;
  color: #065F46;
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
}

.result-content-box {
  background: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.85rem;
  margin-bottom: 1rem;
}

.content-lbl {
  display: block;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  margin-bottom: 0.4rem;
}

.content-text {
  font-family: inherit;
  font-size: 0.85rem;
  color: #1E293B;
  white-space: pre-wrap;
  margin: 0;
}

.obs-box {
  font-size: 0.8rem;
  color: #334155;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  padding: 0.6rem 0.85rem;
  border-radius: 6px;
  margin-bottom: 1rem;
}

.obs-box strong {
  color: #1E40AF;
  display: block;
  margin-bottom: 0.2rem;
}

.sha-box {
  background: #F1F5F9;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  font-size: 0.72rem;
  color: #475569;
  margin-bottom: 1.25rem;
  word-break: break-all;
}

.sha-box code {
  display: block;
  color: #0F172A;
  font-family: monospace;
  margin-top: 0.2rem;
}

.btg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-confirm-btg {
  padding: 0.6rem 1.2rem;
  background-color: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-confirm-btg:hover {
  background-color: #0369A1;
}

.loading-box {
  padding: 2rem;
  text-align: center;
  color: #64748B;
  font-size: 0.85rem;
}

/* ESTILOS DE VINCULACION EN PADRON Y FUSION (PROV-RN-007) */
.patient-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.pill-warning-curp {
  background-color: #FEF3C7;
  color: #92400E;
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: 9999px;
  border: 1px solid #FCD34D;
  letter-spacing: 0.04em;
}

.pill-curp-verified {
  background-color: #EFF6FF;
  color: #1D4ED8;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #BFDBFE;
}

.btn-vincular-padron {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 0.9rem;
  background-color: #FFFFFF;
  color: #0369A1;
  border: 1px solid #0284C7;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-vincular-padron:hover {
  background-color: #F0F9FF;
  border-color: #0369A1;
}

.btn-vincular-padron.highlight {
  background-color: #0284C7;
  color: #FFFFFF;
  border-color: #0284C7;
  box-shadow: 0 1px 3px rgba(2, 132, 199, 0.3);
}

.btn-vincular-padron.highlight:hover {
  background-color: #0369A1;
}

.svg-inline-link {
  width: 14px;
  height: 14px;
}

.emergency-banner-alert {
  margin-top: 0.75rem;
  background-color: #FFFBEB;
  border: 1px solid #FDE68A;
  border-left: 4px solid #D97706;
  border-radius: 6px;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.alert-content {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  color: #78350F;
  font-size: 0.8rem;
}

.alert-content strong {
  display: block;
  font-weight: 700;
  margin-bottom: 0.15rem;
  color: #92400E;
}

.alert-content p {
  margin: 0;
  color: #B45309;
}

.svg-alert-icon {
  width: 20px;
  height: 20px;
  stroke: #D97706;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.btn-alert-action {
  white-space: nowrap;
  padding: 0.45rem 0.85rem;
  background-color: #D97706;
  color: #FFFFFF;
  border: none;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-alert-action:hover {
  background-color: #B45309;
}

/* MODAL OVERLAY Y TARJETA DE FUSION */
.modal-overlay-fusion {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(2px);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.modal-fusion-card {
  background-color: #FFFFFF;
  border-radius: 10px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 780px;
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.modal-fusion-header {
  padding: 1.25rem 1.5rem;
  background-color: #F8FAFC;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.protocol-badge {
  font-size: 0.68rem;
  font-weight: 800;
  color: #0369A1;
  background-color: #E0F2FE;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
  display: inline-block;
  margin-bottom: 0.35rem;
}

.modal-fusion-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #0F172A;
}

.modal-fusion-subtitle {
  margin: 0.3rem 0 0 0;
  font-size: 0.78rem;
  color: #64748B;
  line-height: 1.35;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  color: #94A3B8;
  cursor: pointer;
  padding: 0.2rem;
}

.btn-close-modal:hover {
  color: #334155;
}

.fusion-search-bar {
  padding: 1rem 1.5rem;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  gap: 0.75rem;
}

.search-input-wrapper {
  position: relative;
  flex: 1;
}

.svg-inline-search {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #94A3B8;
}

.search-input-wrapper input {
  width: 100%;
  padding: 0.6rem 0.75rem 0.6rem 2.25rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #0F172A;
  outline: none;
  box-sizing: border-box;
}

.search-input-wrapper input:focus {
  border-color: #0284C7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.btn-buscar-padron {
  padding: 0.6rem 1.2rem;
  background-color: #0F172A;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-buscar-padron:hover:not(:disabled) {
  background-color: #1E293B;
}

.btn-buscar-padron:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.fusion-body {
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.fusion-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.results-count {
  font-size: 0.75rem;
  color: #0369A1;
  background-color: #F0F9FF;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
}

.fusion-loading, .fusion-empty {
  padding: 1.5rem;
  text-align: center;
  color: #64748B;
  font-size: 0.85rem;
  background-color: #F8FAFC;
  border-radius: 6px;
  border: 1px dashed #CBD5E1;
}

.fusion-results-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 220px;
  overflow-y: auto;
}

.fusion-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.fusion-result-item:hover {
  background-color: #F8FAFC;
  border-color: #94A3B8;
}

.fusion-result-item.selected {
  background-color: #F0F9FF;
  border-color: #0284C7;
  box-shadow: 0 0 0 1px #0284C7;
}

.pdef-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.pdef-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pdef-name-row strong {
  font-size: 0.88rem;
  color: #0F172A;
}

.badge-verif {
  font-size: 0.65rem;
  font-weight: 700;
  color: #065F46;
  background-color: #D1FAE5;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.pdef-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #64748B;
  flex-wrap: wrap;
}

.pdef-meta code {
  color: #0284C7;
  font-family: monospace;
}

.btn-select-pdef {
  padding: 0.35rem 0.75rem;
  background-color: #FFFFFF;
  color: #334155;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.badge-selected {
  font-size: 0.72rem;
  font-weight: 700;
  color: #0284C7;
  background-color: #E0F2FE;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
}

/* COMPARACION Y CONFIRMACION */
.fusion-confirm-box {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.fusion-confirm-box h4 {
  margin: 0;
  font-size: 0.92rem;
  color: #0F172A;
}

.fusion-comparison-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 1rem;
  align-items: center;
}

.comp-col {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.75rem;
  font-size: 0.78rem;
}

.comp-temp {
  border-left: 3px solid #F59E0B;
}

.comp-def {
  border-left: 3px solid #10B981;
}

.comp-tag {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #64748B;
  margin-bottom: 0.3rem;
  letter-spacing: 0.04em;
}

.comp-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0F172A;
  margin-bottom: 0.25rem;
}

.comp-col p {
  margin: 0.15rem 0;
  color: #475569;
}

.comp-subtext {
  font-size: 0.7rem;
  color: #0284C7 !important;
  font-style: italic;
  margin-top: 0.35rem !important;
}

.svg-inline-arrow {
  width: 24px;
  height: 24px;
  stroke: #0284C7;
}

.motivo-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.motivo-field label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #334155;
}

.motivo-field textarea {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.82rem;
  color: #0F172A;
  box-sizing: border-box;
  resize: vertical;
}

.motivo-field textarea:focus {
  border-color: #0284C7;
  outline: none;
}

.fusion-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-cancel {
  padding: 0.55rem 1rem;
  background-color: #FFFFFF;
  color: #64748B;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-confirm-fusion {
  padding: 0.55rem 1.25rem;
  background-color: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-confirm-fusion:hover:not(:disabled) {
  background-color: #0369A1;
}

.btn-confirm-fusion:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
