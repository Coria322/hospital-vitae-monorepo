<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import guardiasService, {
  type TurnoGuardia,
  type AsignacionGuardiaBox,
  type PersonalGuardia,
  type PisoHospital,
  CATALOGO_MEDICOS,
  CATALOGO_ENFERMERIA
} from '@/services/guardiasService'

const turnoSeleccionado = ref<TurnoGuardia>('matutino')
const filtroPiso = ref<'todos' | 'P1' | 'P2' | 'P3' | 'QX'>('todos')
const mensajeAlerta = ref('')
const tipoAlerta = ref<'exito' | 'info' | 'alerta'>('info')

const medicosLista = ref<PersonalGuardia[]>(CATALOGO_MEDICOS)
const enfermerosLista = ref<PersonalGuardia[]>(CATALOGO_ENFERMERIA)
const boxes = ref<AsignacionGuardiaBox[]>([])

const cargarBoxes = () => {
  boxes.value = guardiasService.obtenerAsignaciones(turnoSeleccionado.value)
}

// Modal registrar nueva cama / box
const modalRegistroAbierto = ref(false)
const formNuevoBox = ref<{
  piso: PisoHospital
  id: string
  nombre: string
  subtitulo: string
  servicio: string
}>({
  piso: 'P1',
  id: '',
  nombre: '',
  subtitulo: '',
  servicio: 'Urgencias'
})

const abrirModalRegistro = (pisoSugerido?: PisoHospital) => {
  formNuevoBox.value = {
    piso: pisoSugerido || (filtroPiso.value === 'todos' ? 'P1' : filtroPiso.value),
    id: '',
    nombre: '',
    subtitulo: '',
    servicio: pisoSugerido === 'P2' ? 'Medicina Interna' : pisoSugerido === 'P3' ? 'Pediatría' : pisoSugerido === 'QX' ? 'Quirófano' : 'Urgencias'
  }
  modalRegistroAbierto.value = true
}

const cerrarModalRegistro = () => {
  modalRegistroAbierto.value = false
}

const guardarNuevoBox = () => {
  if (!formNuevoBox.value.id.trim() || !formNuevoBox.value.nombre.trim()) {
    mostrarMensaje('El código identificador y el nombre son obligatorios.', 'alerta')
    return
  }

  const res = guardiasService.registrarNuevaCama({
    id: formNuevoBox.value.id,
    nombre: formNuevoBox.value.nombre,
    piso: formNuevoBox.value.piso,
    subtitulo: formNuevoBox.value.subtitulo || 'Servicio Hospitalario',
    servicio: formNuevoBox.value.servicio || 'Servicio Hospitalario'
  })

  if (!res.ok) {
    mostrarMensaje(res.mensaje, 'alerta')
    return
  }

  cargarBoxes()
  cerrarModalRegistro()
  mostrarMensaje(res.mensaje, 'exito')
}

onMounted(() => {
  cargarBoxes()
  window.addEventListener('vitae-camas-actualizadas', cargarBoxes)
})

onUnmounted(() => {
  window.removeEventListener('vitae-camas-actualizadas', cargarBoxes)
})

const cambiarTurno = (nuevoTurno: TurnoGuardia) => {
  turnoSeleccionado.value = nuevoTurno
  cargarBoxes()
  mostrarMensaje(`Turno ${nuevoTurno.toUpperCase()} cargado correctamente.`, 'info')
}

// Estadisticas de Cobertura
const totalBoxes = computed(() => boxes.value.length)
const boxesCubiertosTotalmente = computed(() =>
  boxes.value.filter(b => b.medicoId && b.enfermeroId).length
)
const medicosAsignadosCount = computed(() =>
  new Set(boxes.value.map(b => b.medicoId).filter(Boolean)).size
)
const enfermerosAsignadosCount = computed(() =>
  new Set(boxes.value.map(b => b.enfermeroId).filter(Boolean)).size
)
const porcentajeCobertura = computed(() => {
  if (totalBoxes.value === 0) return 0
  const puntuacion = boxes.value.reduce((acc, b) => {
    let p = 0
    if (b.medicoId) p += 0.5
    if (b.enfermeroId) p += 0.5
    return acc + p
  }, 0)
  return Math.round((puntuacion / totalBoxes.value) * 100)
})

const conteoPiso = (p: 'P1' | 'P2' | 'P3' | 'QX') => {
  return boxes.value.filter(b => b.piso === p).length
}

// Agrupacion por Pisos/Zonas sincronizada con el Censo Hospitalario
const zonasAgrupadas = computed(() => {
  const grupos: Record<string, { titulo: string; piso: 'P1' | 'P2' | 'P3' | 'QX'; items: AsignacionGuardiaBox[] }> = {
    P1: { titulo: `Piso 1 · Urgencias Médicas, Choque y Triage (${conteoPiso('P1')} Boxes)`, piso: 'P1', items: [] },
    P2: { titulo: `Piso 2 · Hospitalización Adultos y Cirugía (${conteoPiso('P2')} Camas)`, piso: 'P2', items: [] },
    P3: { titulo: `Piso 3 · Pediatría y Ginecoobstetricia (${conteoPiso('P3')} Cunas/Camas)`, piso: 'P3', items: [] },
    QX: { titulo: `Bloque Quirúrgico Central QX (${conteoPiso('QX')} Salas)`, piso: 'QX', items: [] }
  }

  for (const b of boxes.value) {
    const grupo = grupos[b.piso]
    if (grupo) {
      grupo.items.push(b)
    }
  }

  const resultado = Object.values(grupos).filter(g => g.items.length > 0)
  if (filtroPiso.value === 'todos') {
    return resultado
  }
  return resultado.filter(g => g.piso === filtroPiso.value)
})

// Actualizacion manual de Box
const onCambioPersonal = (box: AsignacionGuardiaBox) => {
  boxes.value = guardiasService.actualizarBox(
    turnoSeleccionado.value,
    box.id,
    box.medicoId || '',
    box.enfermeroId || '',
    medicosLista.value,
    enfermerosLista.value
  )
  mostrarMensaje(`Asignación actualizada para ${box.nombre}.`, 'exito')
}

const cambiarEstadoOperativo = (box: AsignacionGuardiaBox, nuevoEstado: 'OPERATIVO' | 'EN_SERVICIO' | 'MANTENIMIENTO') => {
  box.estadoOperativo = nuevoEstado
  guardiasService.guardarAsignaciones(turnoSeleccionado.value, boxes.value)
  mostrarMensaje(`Estado de ${box.nombre} cambiado a ${nuevoEstado}.`, 'info')
}

// Guardar Asignacion Oficial de Guardia
const guardarAsignacionManual = () => {
  guardiasService.guardarAsignaciones(turnoSeleccionado.value, boxes.value)
  mostrarMensaje(
    `Asignaciones de guardia para el turno ${turnoSeleccionado.value.toUpperCase()} guardadas y confirmadas correctamente. El personal asignado tiene ahora acceso autorizado a sus áreas clínicas.`,
    'exito'
  )
}

// Distribucion Automatica de Guardia
const ejecutarDistribucionAutomatica = () => {
  boxes.value = guardiasService.distribucionAutomatica(
    turnoSeleccionado.value,
    medicosLista.value,
    enfermerosLista.value
  )
  mostrarMensaje(
    `Distribución automática de guardia aplicada con éxito para el turno ${turnoSeleccionado.value.toUpperCase()}.`,
    'exito'
  )
}

const restablecerGuardia = () => {
  if (confirm('¿Desea restablecer todas las zonas a la configuración inicial libre (23 camas)?')) {
    guardiasService.restablecerTodoLibre()
    cargarBoxes()
    mostrarMensaje('Zonas y guardias restablecidas a la plantilla inicial libre.', 'info')
  }
}

const mostrarMensaje = (texto: string, tipo: 'exito' | 'info' | 'alerta') => {
  mensajeAlerta.value = texto
  tipoAlerta.value = tipo
  setTimeout(() => {
    if (mensajeAlerta.value === texto) {
      mensajeAlerta.value = ''
    }
  }, 4500)
}

const obtenerAsignacionDeMedico = (medicoId: string) => {
  const box = boxes.value.find(b => b.medicoId === medicoId)
  return box ? box.nombre : null
}

const obtenerAsignacionDeEnfermero = (enfermeroId: string) => {
  const box = boxes.value.find(b => b.enfermeroId === enfermeroId)
  return box ? box.nombre : null
}

</script>

<template>
  <div class="zonas-section-container">
    <!-- BARRA SUPERIOR DE ENCABEZADO Y ACCIONES -->
    <header class="zonas-header-card">
      <div class="zonas-title-block">
        <div class="badge-nom">NOM-024-SSA3-2012 · Censo 1:1</div>
        <h1 class="zonas-main-title">Asignación de Guardias, Consultorios y Zonas Hospitalarias</h1>
        <p class="zonas-subtitle">
          Supervisión administrativa de asignación de médicos y personal de enfermería en las 36 camas, boxes y quirófanos del hospital.
        </p>
      </div>

      <!-- SELECTOR DE TURNO -->
      <div class="turno-selector-bar">
        <span class="turno-label">Turno Activo:</span>
        <div class="turno-pills-group">
          <button
            type="button"
            class="turno-pill"
            :class="{ active: turnoSeleccionado === 'matutino' }"
            @click="cambiarTurno('matutino')"
          >
            Matutino (07:00 - 15:00)
          </button>
          <button
            type="button"
            class="turno-pill"
            :class="{ active: turnoSeleccionado === 'vespertino' }"
            @click="cambiarTurno('vespertino')"
          >
            Vespertino (15:00 - 23:00)
          </button>
          <button
            type="button"
            class="turno-pill"
            :class="{ active: turnoSeleccionado === 'nocturno' }"
            @click="cambiarTurno('nocturno')"
          >
            Nocturno (23:00 - 07:00)
          </button>
        </div>
      </div>
    </header>

    <!-- BANNER DE NOTIFICACION DE ACCION -->
    <div v-if="mensajeAlerta" class="alert-banner" :class="tipoAlerta">
      <span class="alert-icon">[INFO]</span>
      <span class="alert-text">{{ mensajeAlerta }}</span>
    </div>

    
    <!-- PANEL DE MONITOREO DE ACCESO Y ADVERTENCIA POR PROFESIONAL -->
    <div class="roster-monitoring-card">
      <div class="roster-header">
        <div>
          <h3 class="roster-title">Estado de Acceso Asistencial por Profesional Clínico</h3>
          <p class="roster-sub">NOM-024 / Seguridad: Solo el personal con consultorio o box asignado en guardia tiene habilitado el acceso a actos médicos, notas ECE y recetas.</p>
        </div>
        <div class="roster-stats">
          <span class="badge-roster green">{{ medicosAsignadosCount + enfermerosAsignadosCount }} Habilitados</span>
          <span class="badge-roster red">{{ (medicosLista.length + enfermerosLista.length) - (medicosAsignadosCount + enfermerosAsignadosCount) }} En Advertencia</span>
        </div>
      </div>

      <div class="roster-columns-grid">
        <!-- Columna Medicos -->
        <div class="roster-col">
          <div class="roster-col-title">
            <span>Plantilla Médica ({{ medicosLista.length }})</span>
            <span class="roster-col-count">{{ medicosAsignadosCount }} Asignados</span>
          </div>
          <div class="roster-items-list">
            <div 
              v-for="med in medicosLista" 
              :key="med.id" 
              class="roster-person-card"
              :class="{ 'card-warning': !obtenerAsignacionDeMedico(med.id), 'card-ok': !!obtenerAsignacionDeMedico(med.id) }"
            >
              <div class="person-main-info">
                <strong>{{ med.nombre }}</strong>
                <span class="person-meta">{{ med.especialidad }} · Céd. {{ med.cedula || 'Pendiente' }}</span>
              </div>
              <div class="person-badge-col">
                <span v-if="obtenerAsignacionDeMedico(med.id)" class="badge-status-ok">
                  {{ obtenerAsignacionDeMedico(med.id) }} · HABILITADO
                </span>
                <span v-else class="badge-status-warn">
                  SIN CONSULTORIO · ADVERTENCIA (BLOQUEADO)
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Columna Enfermeria -->
        <div class="roster-col">
          <div class="roster-col-title">
            <span>Plantilla de Enfermería ({{ enfermerosLista.length }})</span>
            <span class="roster-col-count">{{ enfermerosAsignadosCount }} Asignados</span>
          </div>
          <div class="roster-items-list">
            <div 
              v-for="enf in enfermerosLista" 
              :key="enf.id" 
              class="roster-person-card"
              :class="{ 'card-warning': !obtenerAsignacionDeEnfermero(enf.id), 'card-ok': !!obtenerAsignacionDeEnfermero(enf.id) }"
            >
              <div class="person-main-info">
                <strong>{{ enf.nombre }}</strong>
                <span class="person-meta">{{ enf.especialidad }} · Céd. {{ enf.cedula || 'Pendiente' }}</span>
              </div>
              <div class="person-badge-col">
                <span v-if="obtenerAsignacionDeEnfermero(enf.id)" class="badge-status-ok">
                  {{ obtenerAsignacionDeEnfermero(enf.id) }} · HABILITADO
                </span>
                <span v-else class="badge-status-warn">
                  SIN BOX · ADVERTENCIA (BLOQUEADO)
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- TARJETAS DE INDICADORES DE COBERTURA -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-lbl">Total Boxes / Camas</div>
        <div class="kpi-val highlight-blue">{{ totalBoxes }} Posiciones</div>
        <div class="kpi-sub">Sincronizado con Censo Hospitalario</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-lbl">Médicos en Guardia</div>
        <div class="kpi-val highlight-cyan">{{ medicosAsignadosCount }} / {{ medicosLista.length }} Activos</div>
        <div class="kpi-sub">Especialistas asignados</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-lbl">Enfermería en Guardia</div>
        <div class="kpi-val highlight-cyan">{{ enfermerosAsignadosCount }} / {{ enfermerosLista.length }} Activos</div>
        <div class="kpi-sub">Personal operativo en box</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-lbl">Cobertura Operativa</div>
        <div class="kpi-val" :class="porcentajeCobertura >= 80 ? 'highlight-green' : 'highlight-amber'">
          {{ porcentajeCobertura }}%
        </div>
        <div class="kpi-sub">
          {{ boxesCubiertosTotalmente }} de {{ totalBoxes }} áreas con médico y enfermero
        </div>
      </div>
    </div>

    <!-- BOTONES DE ACCION MASIVA -->
    <div class="actions-toolbar">
      <div class="toolbar-left">
        <button
          type="button"
          class="btn-action-save"
          @click="guardarAsignacionManual"
          title="Guardar y confirmar las asignaciones de guardia para este turno"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          Guardar Asignación
        </button>
        <button
          type="button"
          class="btn-action-primary"
          @click="abrirModalRegistro()"
          title="Dar de alta una nueva cama o box en cualquier zona"
        >
          + Registrar Cama / Box
        </button>
        <button
          type="button"
          class="btn-action-primary"
          @click="ejecutarDistribucionAutomatica"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="16 3 21 3 21 8"/>
            <line x1="4" y1="20" x2="21" y2="3"/>
            <polyline points="21 16 21 21 16 21"/>
            <line x1="15" y1="15" x2="21" y2="21"/>
            <line x1="4" y1="4" x2="9" y2="9"/>
          </svg>
          Distribución Automática de Guardia
        </button>
        <span class="toolbar-hint">Balancea y rota equitativamente al personal según área crítica.</span>
      </div>

      <div class="toolbar-right">
        <button
          type="button"
          class="btn-action-secondary"
          @click="restablecerGuardia"
        >
          Restablecer Zonas Libres (23)
        </button>
      </div>
    </div>

    <!-- SELECTOR DE FILTRO POR PISO/ZONA -->
    <div class="piso-filter-bar">
      <span class="filter-label">Ver Zona Hospitalaria:</span>
      <div class="filter-buttons">
        <button
          type="button"
          class="btn-filter-piso"
          :class="{ active: filtroPiso === 'todos' }"
          @click="filtroPiso = 'todos'"
        >
          Todas las Zonas ({{ totalBoxes }})
        </button>
        <button
          type="button"
          class="btn-filter-piso"
          :class="{ active: filtroPiso === 'P1' }"
          @click="filtroPiso = 'P1'"
        >
          Piso 1 · Urgencias ({{ conteoPiso('P1') }})
        </button>
        <button
          type="button"
          class="btn-filter-piso"
          :class="{ active: filtroPiso === 'P2' }"
          @click="filtroPiso = 'P2'"
        >
          Piso 2 · Hospitalización ({{ conteoPiso('P2') }})
        </button>
        <button
          type="button"
          class="btn-filter-piso"
          :class="{ active: filtroPiso === 'P3' }"
          @click="filtroPiso = 'P3'"
        >
          Piso 3 · Pediatría y Gineco ({{ conteoPiso('P3') }})
        </button>
        <button
          type="button"
          class="btn-filter-piso"
          :class="{ active: filtroPiso === 'QX' }"
          @click="filtroPiso = 'QX'"
        >
          Bloque QX · Quirófanos ({{ conteoPiso('QX') }})
        </button>
      </div>
    </div>

    <!-- LISTADO DE ZONAS Y CONSULTORIOS/BOXES -->
    <div class="zonas-grid-container">
      <section
        v-for="zona in zonasAgrupadas"
        :key="zona.piso"
        class="zona-group-block"
      >
        <div class="zona-group-header">
          <h2 class="zona-group-title">{{ zona.titulo }}</h2>
          <span class="zona-count-badge">{{ zona.items.length }} Posiciones</span>
        </div>

        <div class="boxes-cards-grid">
          <article
            v-for="box in zona.items"
            :key="box.id"
            class="box-detail-card"
            :class="{
              'box-complete': box.medicoId && box.enfermeroId,
              'box-partial': (box.medicoId && !box.enfermeroId) || (!box.medicoId && box.enfermeroId),
              'box-vacant': !box.medicoId && !box.enfermeroId,
              'box-maintenance': box.estadoOperativo === 'MANTENIMIENTO'
            }"
          >
            <!-- CABECERA DEL BOX -->
            <div class="box-card-top">
              <div class="box-id-badge">{{ box.id }}</div>
              <div class="box-status-selector">
                <select
                  v-model="box.estadoOperativo"
                  class="status-select"
                  @change="cambiarEstadoOperativo(box, box.estadoOperativo)"
                >
                  <option value="OPERATIVO">OPERATIVO</option>
                  <option value="EN_SERVICIO">EN SERVICIO</option>
                  <option value="MANTENIMIENTO">MANTENIMIENTO</option>
                </select>
              </div>
            </div>

            <div class="box-card-body">
              <h3 class="box-nombre">{{ box.nombre }}</h3>
              <p class="box-subtitulo">{{ box.subtitulo }}</p>

              <!-- ASIGNACION DE MEDICO -->
              <div class="assignment-row">
                <label class="assignment-label">
                  <span class="label-role">Médico a Cargo:</span>
                  <select
                    v-model="box.medicoId"
                    class="assignment-select"
                    :disabled="box.estadoOperativo === 'MANTENIMIENTO'"
                    @change="onCambioPersonal(box)"
                  >
                    <option value="">-- Sin médico asignado --</option>
                    <option
                      v-for="m in medicosLista"
                      :key="m.id"
                      :value="m.id"
                    >
                      {{ m.nombre }} ({{ m.especialidad }})
                    </option>
                  </select>
                </label>
                <div v-if="box.medicoNombre" class="selected-details">
                  <span class="detail-name">{{ box.medicoNombre }}</span>
                  <span class="detail-spec">{{ box.medicoEspecialidad }}</span>
                </div>
              </div>

              <!-- ASIGNACION DE ENFERMERIA -->
              <div class="assignment-row">
                <label class="assignment-label">
                  <span class="label-role">Enfermería Responsable:</span>
                  <select
                    v-model="box.enfermeroId"
                    class="assignment-select"
                    :disabled="box.estadoOperativo === 'MANTENIMIENTO'"
                    @change="onCambioPersonal(box)"
                  >
                    <option value="">-- Sin enfermero/a asignado/a --</option>
                    <option
                      v-for="e in enfermerosLista"
                      :key="e.id"
                      :value="e.id"
                    >
                      {{ e.nombre }} ({{ e.especialidad }})
                    </option>
                  </select>
                </label>
                <div v-if="box.enfermeroNombre" class="selected-details">
                  <span class="detail-name">{{ box.enfermeroNombre }}</span>
                  <span class="detail-spec">{{ box.enfermeroEspecialidad }}</span>
                </div>
              </div>
            </div>

            <!-- PIE DE TARJETA CON INDICADOR -->
            <div class="box-card-footer">
              <div v-if="box.estadoOperativo === 'MANTENIMIENTO'" class="footer-badge maintenance">
                Mantenimiento / Desinfección
              </div>
              <div v-else-if="box.medicoId && box.enfermeroId" class="footer-badge complete">
                Cobertura Completa (Médico + Enf.)
              </div>
              <div v-else-if="box.medicoId" class="footer-badge partial">
                Atención: Requiere Enfermería
              </div>
              <div v-else-if="box.enfermeroId" class="footer-badge partial">
                Atención: Requiere Médico
              </div>
              <div v-else class="footer-badge vacant">
                Box Desocupado / Sin Personal
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>

    <!-- MODAL REGISTRO DE NUEVA CAMA O BOX DESDE ADMINISTRADOR -->
    <div v-if="modalRegistroAbierto" class="modal-backdrop-custom">
      <div class="modal-card-custom">
        <div class="modal-header-custom">
          <h3>Registrar Nueva Posición Hospitalaria (Box / Cama / Sala)</h3>
          <button type="button" class="btn-close-modal" @click="cerrarModalRegistro">✕</button>
        </div>
        <div class="modal-body-custom">
          <div class="form-group-item">
            <label class="field-label-sm">Zona Hospitalaria / Piso</label>
            <select v-model="formNuevoBox.piso" class="form-select">
              <option value="P1">Piso 1 · Urgencias Médicas y Choque</option>
              <option value="P2">Piso 2 · Hospitalización Adultos</option>
              <option value="P3">Piso 3 · Pediatría y Ginecoobstetricia</option>
              <option value="QX">Bloque Quirúrgico Central QX</option>
            </select>
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Identificador (ej. BOX-U09, HAB-204-A, QUI-06)</label>
            <input type="text" v-model="formNuevoBox.id" placeholder="BOX-U09" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Nombre Mostrado (ej. BOX U-09, HABITACIÓN 204-A)</label>
            <input type="text" v-model="formNuevoBox.nombre" placeholder="BOX U-09" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Subtítulo / Especialidad (ej. Reanimación, Cirugía)</label>
            <input type="text" v-model="formNuevoBox.subtitulo" placeholder="Observación Urgencias" class="form-input" />
          </div>
          <div class="form-group-item">
            <label class="field-label-sm">Servicio Clínico</label>
            <input type="text" v-model="formNuevoBox.servicio" placeholder="Urgencias" class="form-input" />
          </div>
        </div>
        <div class="modal-footer-custom">
          <button type="button" class="btn-secondary-custom" @click="cerrarModalRegistro">Cancelar</button>
          <button type="button" class="btn-primary-custom" @click="guardarNuevoBox">Habilitar en Zona y Guardia</button>
        </div>
      </div>
    </div>

</template>

<style scoped>
.zonas-section-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  background-color: #f8fafc;
  min-height: 100%;
  font-family: inherit;
}

/* ENCABEZADO */
.zonas-header-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.zonas-title-block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.badge-nom {
  display: inline-flex;
  align-self: flex-start;
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background-color: #e0f2fe;
  color: #0369a1;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.zonas-main-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.2;
}

.zonas-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

/* SELECTOR DE TURNO */
.turno-selector-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid #f1f5f9;
}

.turno-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #334155;
}

.turno-pills-group {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.turno-pill {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.4rem 0.85rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.turno-pill:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.turno-pill.active {
  background: #0284c7;
  border-color: #0284c7;
  color: #ffffff;
}

/* ALERTA */
.alert-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
}

.alert-banner.exito {
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.alert-banner.info {
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0369a1;
}

.alert-icon {
  font-weight: 700;
  font-size: 0.75rem;
}

/* KPIS */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.kpi-lbl {
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
}

.highlight-blue {
  color: #0284c7;
}

.highlight-cyan {
  color: #0d9488;
}

.highlight-green {
  color: #16a34a;
}

.highlight-amber {
  color: #d97706;
}

.kpi-sub {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* ACTIONS TOOLBAR */
.actions-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.85rem 1.25rem;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn-action-save {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #059669;
  color: #ffffff;
  border: none;
  font-size: 0.875rem;
  font-weight: 700;
  padding: 0.55rem 1.15rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-action-save:hover {
  background-color: #047857;
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #0284c7;
  color: #ffffff;
  border: none;
  font-size: 0.875rem;
  font-weight: 600;
  padding: 0.55rem 1.1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-action-primary:hover {
  background-color: #0369a1;
}

.btn-icon-svg {
  width: 16px;
  height: 16px;
}

.toolbar-hint {
  font-size: 0.8125rem;
  color: #64748b;
}

.btn-action-secondary {
  background-color: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.9rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-action-secondary:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

/* FILTRO POR PISO */
.piso-filter-bar {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.65rem 1rem;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #334155;
}

.filter-buttons {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.btn-filter-piso {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 0.78125rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-filter-piso:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-filter-piso.active {
  background: #0f172a;
  border-color: #0f172a;
  color: #ffffff;
}

/* ZONAS Y BOXES */
.zonas-grid-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.zona-group-block {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.zona-group-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.zona-group-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.zona-count-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  background-color: #e2e8f0;
  color: #475569;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.boxes-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
}

/* TARJETA DE BOX */
.box-detail-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}

.box-detail-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.box-detail-card.box-complete {
  border-left: 4px solid #16a34a;
}

.box-detail-card.box-partial {
  border-left: 4px solid #f59e0b;
}

.box-detail-card.box-vacant {
  border-left: 4px solid #94a3b8;
}

.box-detail-card.box-maintenance {
  border-left: 4px solid #ef4444;
  opacity: 0.8;
}

.box-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.box-id-badge {
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  background-color: #f1f5f9;
  color: #334155;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.status-select {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #334155;
}

.box-card-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  flex: 1;
}

.box-nombre {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.box-subtitulo {
  font-size: 0.75rem;
  color: #64748b;
  margin: -0.5rem 0 0.25rem 0;
}

.assignment-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.assignment-label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.label-role {
  font-size: 0.6875rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
}

.assignment-select {
  width: 100%;
  font-size: 0.8125rem;
  padding: 0.4rem 0.6rem;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #0f172a;
  outline: none;
}

.assignment-select:focus {
  border-color: #0284c7;
}

.assignment-select:disabled {
  background-color: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.selected-details {
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #f1f5f9;
}

.detail-name {
  font-size: 0.75rem;
  font-weight: 700;
  color: #1e293b;
}

.detail-spec {
  font-size: 0.6875rem;
  color: #64748b;
}

/* PIE DE TARJETA */
.box-card-footer {
  padding: 0.65rem 1rem;
  border-top: 1px solid #f8fafc;
  background-color: #fafbfc;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
}

.footer-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  display: inline-block;
}

.footer-badge.complete {
  background-color: #ecfdf5;
  color: #15803d;
}

.footer-badge.partial {
  background-color: #fffbeb;
  color: #b45309;
}

.footer-badge.vacant {
  background-color: #f1f5f9;
  color: #64748b;
}

.footer-badge.maintenance {
  background-color: #fef2f2;
  color: #b91c1c;
}

/* MODAL CUSTOM ADMINISTRADOR */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.modal-card-custom {
  background: #ffffff;
  border-radius: 10px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
  border: 1px solid #e2e8f0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header-custom h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #0f172a;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: #64748b;
}

.modal-body-custom {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.modal-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 0.85rem 1.25rem;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.btn-secondary-custom {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-weight: 600;
  font-size: 0.8125rem;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary-custom {
  background: #0284c7;
  border: none;
  color: #ffffff;
  font-weight: 600;
  font-size: 0.8125rem;
  padding: 0.5rem 1.1rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary-custom:hover {
  background: #0369a1;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.field-label-sm {
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
}

.form-select, .form-input {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 0.8125rem;
  color: #0f172a;
}


.roster-monitoring-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.roster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 0.85rem;
  margin-bottom: 1rem;
}

.roster-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
  color: #0f172a;
}

.roster-sub {
  margin: 0.25rem 0 0;
  font-size: 0.78rem;
  color: #64748b;
}

.roster-stats {
  display: flex;
  gap: 0.5rem;
}

.badge-roster {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 6px;
}

.badge-roster.green {
  background: #dcfce7;
  color: #15803d;
}

.badge-roster.red {
  background: #fee2e2;
  color: #b91c1c;
}

.roster-columns-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.roster-col-title {
  display: flex;
  justify-content: space-between;
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
  padding-bottom: 0.35rem;
  border-bottom: 2px solid #e2e8f0;
}

.roster-col-count {
  color: #0284c7;
}

.roster-items-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 260px;
  overflow-y: auto;
}

.roster-person-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.55rem 0.75rem;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.roster-person-card.card-ok {
  border-left: 3px solid #10b981;
}

.roster-person-card.card-warning {
  border-left: 3px solid #ef4444;
  background: #fffafa;
}

.person-main-info {
  display: flex;
  flex-direction: column;
}

.person-main-info strong {
  font-size: 0.84rem;
  color: #0f172a;
}

.person-meta {
  font-size: 0.72rem;
  color: #64748b;
}

.badge-status-ok {
  font-size: 0.68rem;
  font-weight: 700;
  color: #15803d;
  background: #dcfce7;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.badge-status-warn {
  font-size: 0.68rem;
  font-weight: 700;
  color: #b91c1c;
  background: #fee2e2;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

</style>