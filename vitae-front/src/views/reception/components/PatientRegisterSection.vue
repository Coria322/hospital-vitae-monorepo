<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

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

// =========================================================================
// PANTALLA 2: REGISTRAR NUEVO PACIENTE (IMAGEN 2)
// =========================================================================
const nuevoRegistroForm = ref({
  // Paso 1: CURP
  curpValidada: false,
  curpIngresada: props.curpCompartida || '',
  // Paso 2: Datos demográficos (inician en blanco o con datos del paciente si fue transferido)
  primerApellido: '',
  segundoApellido: '',
  nombres: '',
  fechaNacimiento: '',
  sexoBiologico: '',
  estadoCivil: '',
  // Domicilio
  calleNumero: '',
  colonia: '',
  codigoPostal: '',
  entidadFederativa: '',
  municipioAlcaldia: '',
  telefonoContacto: '',
  // Cobertura
  tipoAsegurador: '',
  numeroSeguridadSocial: '',
  estatusVigencia: 'Pendiente de captura',
  // Consentimiento administrativo
  consentimientoAceptado: false
})

const sincronizarDatosTransferidos = () => {
  if (props.curpCompartida) {
    nuevoRegistroForm.value.curpIngresada = props.curpCompartida
    nuevoRegistroForm.value.curpValidada = true
  }
  if (props.pacienteSeleccionado) {
    const p = props.pacienteSeleccionado
    const nombreCompleto = p.nombreCompleto || p.nombre || ''
    const partes = nombreCompleto.split(' ')
    nuevoRegistroForm.value.primerApellido = partes[0] || ''
    nuevoRegistroForm.value.segundoApellido = partes[1] || ''
    nuevoRegistroForm.value.nombres = partes.slice(2).join(' ') || partes[0]
    nuevoRegistroForm.value.curpIngresada = p.curp || props.curpCompartida || ''
    nuevoRegistroForm.value.curpValidada = true
    nuevoRegistroForm.value.sexoBiologico = p.sexo || ''
    nuevoRegistroForm.value.telefonoContacto = p.telefonoContacto || ''
  }
}

watch(() => props.curpCompartida, () => sincronizarDatosTransferidos())
watch(() => props.pacienteSeleccionado, () => sincronizarDatosTransferidos())
onMounted(() => sincronizarDatosTransferidos())

const validarCurpEnRenapo = () => {
  if (!nuevoRegistroForm.value.curpIngresada) {
    alert('Por favor ingrese la CURP para validar en RENAPO.')
    return
  }
  nuevoRegistroForm.value.curpValidada = true
  alert('Conexión con RENAPO exitosa: CURP no duplicada en el Padrón Nacional Hospitalario.')
}

const limpiarFormularioRegistro = () => {
  nuevoRegistroForm.value = {
    curpValidada: false,
    curpIngresada: '',
    primerApellido: '',
    segundoApellido: '',
    nombres: '',
    fechaNacimiento: '',
    sexoBiologico: '',
    estadoCivil: '',
    calleNumero: '',
    colonia: '',
    codigoPostal: '',
    entidadFederativa: '',
    municipioAlcaldia: '',
    telefonoContacto: '',
    tipoAsegurador: '',
    numeroSeguridadSocial: '',
    estatusVigencia: 'Pendiente de captura',
    consentimientoAceptado: false
  }
}

const cargarDemoRegistro = () => {
  nuevoRegistroForm.value = {
    curpValidada: true,
    curpIngresada: 'GOMJ920314HDFRRN01',
    primerApellido: 'González',
    segundoApellido: 'Martínez',
    nombres: 'Juan Carlos',
    fechaNacimiento: '1992-03-14',
    sexoBiologico: 'Masculino',
    estadoCivil: 'Soltero/a',
    calleNumero: 'Av. Insurgentes Sur 1422, Int. 4B',
    colonia: 'Del Valle Sur',
    codigoPostal: '03100',
    entidadFederativa: 'Ciudad de México',
    municipioAlcaldia: 'Benito Juárez',
    telefonoContacto: '55 1234 5678',
    tipoAsegurador: 'IMSS Régimen Ordinario',
    numeroSeguridadSocial: '4819-92-01928-1',
    estatusVigencia: 'Vigente en Padrón IMSS',
    consentimientoAceptado: true
  }
}

const completarRegistroExpediente = () => {
  if (!nuevoRegistroForm.value.primerApellido || !nuevoRegistroForm.value.nombres) {
    alert('NOM-024-SSA3-2012: Ingrese al menos el Primer Apellido y Nombre(s) del paciente.')
    return
  }
  if (!nuevoRegistroForm.value.consentimientoAceptado) {
    alert('Debe marcar la casilla de Aceptación de Consentimiento Informado Administrativo.')
    return
  }
  alert('<svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Paciente registrado exitosamente. Se ha generado la Clave Única de Expediente (CUPE) con Folio ADM-2026-10492.')
}

// Datos de la cola de espera de admisión
const colaEsperaAdmision = ref([
  { folio: 'FOL-9482', iniciales: 'M.R.', estado: 'Llamando', modulo: 'Módulo Triage 01 · Prioridad Amarilla', tiempo: '4 min', prioridad: 'llamando' },
  { folio: 'FOL-9483', iniciales: 'A.S.', estado: 'En espera', modulo: 'Consultorio Admisión 02', tiempo: '11 min', prioridad: 'espera' },
  { folio: 'FOL-9484', iniciales: 'P.V.', estado: 'En espera', modulo: 'Caja y Aranceles de Ingreso', tiempo: '16 min', prioridad: 'espera' },
  { folio: 'FOL-9485', iniciales: 'L.G.', estado: 'En espera', modulo: 'Revisión de Derechos Vigentes', tiempo: '22 min', prioridad: 'espera' }
])

const llamarSiguienteTurno = () => {
  alert('<svg viewBox="0 0 24 24" class="svg-inline-speaker" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg> Llamando siguiente turno al altavoz de sala y pantalla pública de admisión.')
}
</script>

<template>
<div class="view-registrar-container">
          
          <!-- Encabezado de la Pantalla de Registro -->
          <div class="section-top-banner">
            <div class="banner-text-col">
              <div class="title-with-pill">
                <h2>Ingreso y Alta Demográfica de Paciente</h2>
                <span class="active-process-tag">PROCESO ACTIVO</span>
              </div>
              <p>Criterio obligatorio de verificación registral según <strong>NOM-024-SSA3-2012</strong>: Búsqueda previa por CURP obligatoria para prevenir expedientes duplicados.</p>
            </div>

            <div class="banner-status-stack">
              <div class="renapo-ping-badge">
                <span class="ping-dot"></span>
                <span>INTERCONEXIÓN RENAPO: <strong>En línea · Ping 24ms</strong></span>
              </div>
              <div class="ingress-folio-badge">
                <span><svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg> FOLIO DE INGRESO: <strong>ADM-2026-10492</strong></span>
              </div>
            </div>
          </div>

          <!-- Cuadrícula Principal de Registro (Formulario a la Izquierda + Panel a la Derecha) -->
          <div class="curp-layout-grid">
            
            <!-- FORMULARIO DE ALTA (PASO 1 + PASO 2) -->
            <div class="curp-left-col">
              
              <!-- PASO 1: Validación Preventiva de Identidad (CURP) -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="step-num-title">
                    <span class="step-circle">1</span>
                    <div>
                      <h4>Paso 1: Validación Preventiva de Identidad (CURP)</h4>
                      <small>Obligatorio previo a captura demográfica para impedir duplicidad de Clave Única de Expediente (CUPE).</small>
                    </div>
                  </div>
                  <span class="step-shortcut">Atajo: Alt + B</span>
                </div>

                <div class="step-body">
                  <div class="curp-validation-input-row">
                    <div class="input-with-icon">
                      <span class="id-icon"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><line x1="15" y1="8" x2="17" y2="8"/><line x1="15" y1="12" x2="17" y2="12"/><line x1="7" y1="16" x2="17" y2="16"/></svg></span>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.curpIngresada" 
                        placeholder="INGRESA LOS 18 CARACTERES DE LA CURP" 
                        maxlength="18"
                        class="input-curp-step1"
                      />
                    </div>
                    <button 
                      type="button" 
                      class="btn-validate-renapo"
                      @click="validarCurpEnRenapo"
                    >
                      <svg viewBox="0 0 24 24" class="svg-inline-search" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Validar en RENAPO / Base Local
                    </button>
                  </div>

                  <!-- Fila de advertencia y lectores alternativos -->
                  <div class="step1-footer-options">
                    <div class="step1-hint">
                      <span class="info-circle"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg></span>
                      <span v-if="nuevoRegistroForm.curpValidada" class="text-green-bold">
                        <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> CURP validada sin duplicidad. Puede continuar con el Paso 2.
                      </span>
                      <span v-else>
                        Sin consulta ejecutada. Escribe la CURP del paciente o escanea el código de barras de su identificación.
                      </span>
                    </div>

                    <div class="optical-readers-row">
                      <button type="button" class="btn-subtle-reader" @click="mostrarAlerta('Iniciando lector óptico 2D para Credencial INE / Cédula...')">
                        <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg> Lector Óptico INE
                      </button>
                      <button type="button" class="btn-subtle-reader" @click="mostrarAlerta('Habilitando protocolo de recién nacido sin CURP / Paciente extranjero con pasaporte...')">
                        <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><circle cx="9" cy="10" r="1" fill="currentColor"/><circle cx="15" cy="10" r="1" fill="currentColor"/><path d="M9 15a3 3 0 0 0 6 0"/></svg> Paciente Recién Nacido / Extranjero
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- PASO 2: Datos Demográficos y Filiares -->
              <div class="step-card">
                <div class="step-card-header">
                  <div class="step-num-title">
                    <span class="step-circle">2</span>
                    <div>
                      <h4>Paso 2: Datos Demográficos y Filiares</h4>
                      <small>Todos los campos son requeridos para la correcta integración del expediente clínico electrónico.</small>
                    </div>
                  </div>
                  <span class="mandatory-hint">^ Campos con asterisco son mandatorios</span>
                </div>

                <div class="step-body">
                  
                  <!-- Nombres y Apellidos -->
                  <div class="form-grid-3">
                    <div class="field-group">
                      <label>Primer Apellido <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.primerApellido" 
                        placeholder="Ej. González" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group">
                      <label>Segundo Apellido</label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.segundoApellido" 
                        placeholder="Ej. Martínez" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group">
                      <label>Nombre(s) <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.nombres" 
                        placeholder="Ej. Juan Carlos" 
                        class="form-input" 
                      />
                    </div>
                  </div>

                  <!-- Fecha de Nacimiento, Sexo, Estado Civil -->
                  <div class="form-grid-3">
                    <div class="field-group">
                      <label>Fecha de Nacimiento <span class="req">*</span></label>
                      <input 
                        type="date" 
                        v-model="nuevoRegistroForm.fechaNacimiento" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group">
                      <label>Sexo Biológico asignado al nacer <span class="req">*</span></label>
                      <select v-model="nuevoRegistroForm.sexoBiologico" class="form-select">
                        <option value="">Selecciona sexo...</option>
                        <option value="Masculino">Masculino</option>
                        <option value="Femenino">Femenino</option>
                        <option value="Indeterminado">Indeterminado</option>
                      </select>
                    </div>
                    <div class="field-group">
                      <label>Estado Civil</label>
                      <select v-model="nuevoRegistroForm.estadoCivil" class="form-select">
                        <option value="">Selecciona...</option>
                        <option value="Soltero/a">Soltero/a</option>
                        <option value="Casado/a">Casado/a</option>
                        <option value="Unión Libre">Unión Libre</option>
                        <option value="Divorciado/a">Divorciado/a</option>
                        <option value="Viudo/a">Viudo/a</option>
                      </select>
                    </div>
                  </div>

                  <!-- Subsección: Domicilio de Residencia Habitual -->
                  <div class="form-subsection-title">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg></span>
                    <strong>DOMICILIO DE RESIDENCIA HABITUAL</strong>
                  </div>

                  <div class="form-grid-address-1">
                    <div class="field-group flex-2">
                      <label>Calle y Número Exterior / Interior <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.calleNumero" 
                        placeholder="Ej. Av. Insurgentes Sur 1422, Int. 4B" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group flex-1">
                      <label>Colonia / Asentamiento <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.colonia" 
                        placeholder="Ej. Del Valle Sur" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group flex-1">
                      <label>Código Postal <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.codigoPostal" 
                        placeholder="Ej. 03100" 
                        class="form-input" 
                      />
                    </div>
                  </div>

                  <div class="form-grid-3">
                    <div class="field-group">
                      <label>Entidad Federativa <span class="req">*</span></label>
                      <select v-model="nuevoRegistroForm.entidadFederativa" class="form-select">
                        <option value="">Selecciona Estado...</option>
                        <option value="Ciudad de México">Ciudad de México</option>
                        <option value="Estado de México">Estado de México</option>
                        <option value="Jalisco">Jalisco</option>
                        <option value="Nuevo León">Nuevo León</option>
                        <option value="Puebla">Puebla</option>
                        <option value="Veracruz">Veracruz</option>
                        <option value="Yucatán">Yucatán</option>
                        <option value="Otro">Otra Entidad Federativa</option>
                      </select>
                    </div>
                    <div class="field-group">
                      <label>Municipio o Alcaldía <span class="req">*</span></label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.municipioAlcaldia" 
                        placeholder="Ej. Benito Juárez" 
                        class="form-input" 
                      />
                    </div>
                    <div class="field-group">
                      <label>Teléfono de Contacto Directo <span class="req">*</span></label>
                      <input 
                        type="tel" 
                        v-model="nuevoRegistroForm.telefonoContacto" 
                        placeholder="Ej. 55 1234 5678" 
                        class="form-input" 
                      />
                    </div>
                  </div>

                  <!-- Subsección: Aseguramiento y Cobertura Activa -->
                  <div class="form-subsection-title justify-between">
                    <div class="title-with-icon-sm">
                      <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                      <strong>ASEGURAMIENTO Y COBERTURA ACTIVA</strong>
                    </div>
                    <button type="button" class="btn-check-rights" @click="mostrarAlerta('Consultando vigencia en pasarela IMSS / ISSSTE / Privados...')">
                      <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Consulta Automática de Derechos
                    </button>
                  </div>

                  <div class="form-grid-3">
                    <div class="field-group">
                      <label>Tipo de Asegurador / Cobertura <span class="req">*</span></label>
                      <select v-model="nuevoRegistroForm.tipoAsegurador" class="form-select">
                        <option value="">Seleccione régimen...</option>
                        <option value="IMSS Régimen Ordinario">IMSS Régimen Ordinario</option>
                        <option value="IMSS Bienestar">IMSS Bienestar</option>
                        <option value="ISSSTE">ISSSTE</option>
                        <option value="Seguro Popular / INSABI">Seguro Popular / INSABI</option>
                        <option value="Aseguradora Privada">Aseguradora Privada (GNP, MetLife, etc.)</option>
                        <option value="Particular / Sin Cobertura">Particular / Sin Cobertura</option>
                      </select>
                    </div>

                    <div class="field-group">
                      <label>NSS / No. Póliza / Credencial</label>
                      <input 
                        type="text" 
                        v-model="nuevoRegistroForm.numeroSeguridadSocial" 
                        placeholder="Número de Seguridad Social o Póliza" 
                        class="form-input" 
                      />
                    </div>

                    <div class="field-group">
                      <label>Estatus de Vigencia</label>
                      <div class="vigencia-status-box">
                        <span>{{ nuevoRegistroForm.estatusVigencia }}</span>
                        <span class="status-hourglass"><svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 22h14"/><path d="M5 2h14"/><path d="M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22"/><path d="M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2"/></svg></span>
                      </div>
                    </div>
                  </div>

                  <!-- Consentimiento Informado Administrativo -->
                  <div class="consent-block-wrapper">
                    <div class="consent-check-col">
                      <input 
                        type="checkbox" 
                        id="check-consent"
                        v-model="nuevoRegistroForm.consentimientoAceptado" 
                      />
                    </div>
                    <div class="consent-text-col">
                      <label for="check-consent">
                        <strong>Aceptación de Consentimiento Informado Administrativo:</strong> 
                        El paciente o responsable legal declara que los datos provistos son verídicos y autoriza el tratamiento conforme al Aviso de Privacidad del Hospital Vitae bajo la <strong>Ley Federal de Protección de Datos Personales</strong>.
                      </label>
                    </div>

                    <div class="consent-action-link">
                      <button 
                        type="button" 
                        class="btn-legal-consent-nav" 
                        @click="cambiarSeccion('consentimiento')"
                      >
                        <svg viewBox="0 0 24 24" class="svg-inline-clip" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg> Proceder a Consentimiento Legal (NOM-004) <svg viewBox="0 0 24 24" class="svg-inline-arrow" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                      </button>
                    </div>
                  </div>

                </div>

                <!-- Barra de Acciones del Formulario -->
                <div class="form-bottom-actions">
                  <div class="actions-left">
                    <button type="button" class="btn-clear-form" @click="limpiarFormularioRegistro">
                      <svg viewBox="0 0 24 24" class="svg-inline-sync" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> Limpiar Formulario
                    </button>
                    <button type="button" class="btn-print-cred" @click="mostrarAlerta('Imprimiendo credencial temporal del paciente y carátula de expediente...')">
                      <svg viewBox="0 0 24 24" class="svg-inline-print" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg> Imprimir Credencial / Formato
                    </button>
                  </div>

                  <button 
                    type="button" 
                    class="btn-complete-register"
                    @click="completarRegistroExpediente"
                  >
                    <svg viewBox="0 0 24 24" class="svg-inline-check" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> Completar Registro y Abrir Expediente [Enter]
                  </button>
                </div>
              </div>

            </div>

            <!-- SUBCOLUMNA DERECHA: Censo Inmediato, Monitor de Sala y Red Central -->
            <aside class="curp-right-col">
              
              <!-- 1. Censo de Capacidad Inmediata -->
              <div class="side-widget-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-bed" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 4v16"/><path d="M2 8h18a2 2 0 0 1 2 2v10" /><path d="M2 17h20"/><path d="M6 8v9"/></svg></span>
                    <div>
                      <strong>Censo de Capacidad Inmediata</strong>
                      <small class="block-sub">Sede Principal Norte · Admisión</small>
                    </div>
                  </div>
                  <span class="time-stamp-sm">10:48 h</span>
                </div>

                <div class="capacity-stacked-list">
                  <!-- Hospitalización -->
                  <div class="cap-group">
                    <div class="cap-header-row">
                      <span>Hospitalización Adultos & Pediatría</span>
                      <strong>24 Libres / 106 Total</strong>
                    </div>
                    <div class="occ-progress-track">
                      <div class="occ-fill" style="width: 77.3%;"></div>
                    </div>
                    <small class="cap-sub">Ocupación: 77.3% (82 Camas ocupadas) · <strong class="text-blue">24 Disponibles</strong></small>
                  </div>

                  <!-- Observación Urgencias -->
                  <div class="cap-group">
                    <div class="cap-header-row">
                      <span>Observación y Triage Urgencias</span>
                      <strong class="text-red">6 Libres / 18 Total</strong>
                    </div>
                    <div class="occ-progress-track">
                      <div class="occ-fill red" style="width: 66.6%;"></div>
                    </div>
                    <small class="cap-sub">12 Pacientes en estabilización · <strong>6 Boxes Listos</strong></small>
                  </div>

                  <!-- Quirófanos Centrales -->
                  <div class="cap-group">
                    <div class="cap-header-row">
                      <span>Quirófanos Centrales (Q1 - Q8)</span>
                      <strong class="text-cyan">3 Libres / 5 En Uso</strong>
                    </div>
                    <div class="quirofanos-mini-grid">
                      <div class="q-chip red">Q-1 <span>Cirugía</span></div>
                      <div class="q-chip red">Q-2 <span>Cirugía</span></div>
                      <div class="q-chip green">Q-3 <span>Listo</span></div>
                      <div class="q-chip green">Q-4 <span>Listo</span></div>
                    </div>
                  </div>
                </div>

                <button type="button" class="btn-open-census-board" @click="cambiarSeccion('censo')">
                  <svg viewBox="0 0 24 24" class="svg-inline-folder" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg> Abrir Tablero de Camas y Censos Completo
                </button>
              </div>

              <!-- 2. Monitor de Sala de Espera (Turnos en Atención) -->
              <div class="side-widget-card">
                <div class="widget-header">
                  <div class="title-row-sm">
                    <span><svg viewBox="0 0 24 24" class="svg-inline-screen" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg></span>
                    <div>
                      <strong>Monitor de Sala de Espera</strong>
                      <small class="block-sub">Turnos en Atención Presencial</small>
                    </div>
                  </div>
                  <span class="live-count-badge">8 En Turno</span>
                </div>

                <div class="privacy-notice-mini">
                  <span><svg viewBox="0 0 24 24" class="svg-inline-shield" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span>
                  <small>Protección de Privacidad: La sala pública sólo proyecta Folio e Iniciales. Prohibido mostrar diagnósticos clínicos.</small>
                </div>

                <!-- Lista de Turnos -->
                <div class="queue-list">
                  <div 
                    v-for="turno in colaEsperaAdmision" 
                    :key="turno.folio" 
                    class="queue-item"
                    :class="{ active: turno.prioridad === 'llamando' }"
                  >
                    <div class="queue-item-left">
                      <div class="queue-avatar" :class="turno.prioridad">
                        {{ turno.iniciales }}
                      </div>
                      <div class="queue-info">
                        <strong>{{ turno.folio }} · {{ turno.iniciales }}</strong>
                        <small>{{ turno.modulo }}</small>
                      </div>
                    </div>
                    <div class="queue-item-right">
                      <span class="queue-status-tag" :class="turno.prioridad">{{ turno.estado }}</span>
                      <small class="queue-time">{{ turno.tiempo }}</small>
                    </div>
                  </div>
                </div>

                <div class="queue-actions-row">
                  <button type="button" class="btn-call-next" @click="llamarSiguienteTurno">
                    <svg viewBox="0 0 24 24" class="svg-inline-speaker" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg> Llamar Siguiente Turno
                  </button>
                  <button type="button" class="btn-popout-screen" title="Abrir pantalla pública en monitor secundario">
                    <svg viewBox="0 0 24 24" class="svg-inline-icon" fill="none" stroke="currentColor" stroke-width="2"><line x1="7" y1="17" x2="17" y2="7"/><polyline points="7 7 17 7 17 17"/></svg>
                  </button>
                </div>
              </div>

              <!-- 3. Red Central Hospital Vitae -->
              <div class="side-widget-card red-central-card">
                <div class="red-central-row">
                  <img src="@/assets/hospital-fachada.jpg" alt="Red Central" class="mini-fachada" />
                  <div>
                    <strong>Hospital Vitae · Red Central</strong>
                    <p>Conexión con Centro Regulador de Urgencias Médicas (CRUM) activa.</p>
                  </div>
                </div>
              </div>

            </aside>
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
   PANTALLA 2: REGISTRAR NUEVO PACIENTE (IMAGEN 2)
   ========================================================================= */
.view-registrar-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.title-with-pill {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.active-process-tag {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1D4ED8;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.banner-status-stack {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  align-items: flex-end;
}

.renapo-ping-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: #15803D;
  background-color: #F0FDF4;
  border: 1px solid #BBF7D0;
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
}

.ping-dot {
  width: 6px;
  height: 6px;
  background-color: #15803D;
  border-radius: 50%;
}

.ingress-folio-badge {
  font-size: 0.72rem;
  color: #0369A1;
  background-color: #F0F9FF;
  border: 1px solid #BAE6FD;
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
}

.step-card {
  background-color: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.step-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.step-num-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.step-circle {
  width: 28px;
  height: 28px;
  background-color: #2563EB;
  color: #FFFFFF;
  font-weight: 800;
  font-size: 0.85rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-num-title h4 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0;
}

.step-num-title small {
  font-size: 0.7rem;
  color: #64748B;
}

.step-shortcut {
  font-size: 0.68rem;
  font-weight: 600;
  color: #64748B;
  background-color: #F1F5F9;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.mandatory-hint {
  font-size: 0.68rem;
  color: #64748B;
}

.curp-validation-input-row {
  display: flex;
  gap: 0.6rem;
}

.input-with-icon {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0 0.6rem;
}

.id-icon {
  font-size: 1rem;
  margin-right: 0.4rem;
}

.input-curp-step1 {
  width: 100%;
  border: none;
  background: transparent;
  font-family: monospace;
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.6rem 0;
  outline: none;
}

.btn-validate-renapo {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0 1.25rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.step1-footer-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.6rem;
  font-size: 0.72rem;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.step1-hint {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #64748B;
}

.text-green-bold {
  color: #15803D;
  font-weight: 700;
}

.optical-readers-row {
  display: flex;
  gap: 0.4rem;
}

.btn-subtle-reader {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

/* FORMULARIO PASO 2 */
.form-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.8rem;
  margin-bottom: 0.8rem;
}

.form-grid-address-1 {
  display: flex;
  gap: 0.8rem;
  margin-bottom: 0.8rem;
}

.flex-2 { flex: 2; }
.flex-1 { flex: 1; }

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-group label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
}

.field-group .req {
  color: #DC2626;
}

.form-input, .form-select {
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
  font-size: 0.75rem;
  color: #1E293B;
  outline: none;
}

.form-input:focus, .form-select:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.form-subsection-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  color: #0F4C81;
  padding: 0.4rem 0;
  border-bottom: 1px solid #F1F5F9;
  margin-bottom: 0.6rem;
}

.form-subsection-title.justify-between {
  justify-content: space-between;
}

.title-with-icon-sm {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-check-rights {
  background: none;
  border: none;
  color: #0284C7;
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
}

.vigencia-status-box {
  background-color: #F1F5F9;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
  font-size: 0.75rem;
  color: #64748B;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* CONSENTIMIENTO INFORMADO */
.consent-block-wrapper {
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.consent-check-col {
  padding-top: 0.15rem;
}

.consent-text-col {
  flex: 1;
  min-width: 280px;
  font-size: 0.72rem;
  color: #334155;
  line-height: 1.35;
}

.btn-legal-consent-nav {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #1E40AF;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-legal-consent-nav:hover {
  background-color: #DBEAFE;
}

/* PIE DE ACCIONES DEL FORMULARIO */
.form-bottom-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #F1F5F9;
  padding-top: 1rem;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.actions-left {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-clear-form, .btn-print-cred, .btn-demo-form {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.55rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-demo-form {
  background-color: #F0FDF4;
  border-color: #86EFAC;
  color: #166534;
  font-weight: 700;
}

.btn-complete-register {
  background-color: #2563EB;
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1.25rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-complete-register:hover {
  background-color: #1D4ED8;
}

/* WIDGETS DERECHA REGISTRO */
.block-sub {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.time-stamp-sm {
  font-size: 0.68rem;
  font-weight: 700;
  color: #64748B;
}

.capacity-stacked-list {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.cap-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.cap-header-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  font-weight: 600;
  color: #1E293B;
}

.cap-sub {
  font-size: 0.65rem;
  color: #64748B;
}

.text-blue { color: #2563EB; }
.text-red { color: #DC2626; }
.text-cyan { color: #0284C7; }

.quirofanos-mini-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.35rem;
  margin-top: 0.2rem;
}

.q-chip {
  padding: 0.25rem;
  border-radius: 4px;
  text-align: center;
  font-size: 0.68rem;
  font-weight: 700;
}

.q-chip span {
  display: block;
  font-size: 0.58rem;
  font-weight: 500;
}

.q-chip.red { background-color: #FEE2E2; color: #DC2626; }
.q-chip.green { background-color: #DCFCE7; color: #166534; }

.btn-open-census-board {
  width: 100%;
  background-color: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
}

/* MONITOR DE SALA DE ESPERA */
.live-count-badge {
  background-color: #E0F2FE;
  color: #0369A1;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.privacy-notice-mini {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  font-size: 0.65rem;
  color: #64748B;
  line-height: 1.25;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.queue-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 0.5rem 0.65rem;
}

.queue-item.active {
  background-color: #EFF6FF;
  border-color: #93C5FD;
}

.queue-item-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.queue-avatar {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 800;
}

.queue-avatar.llamando { background-color: #0284C7; color: #FFFFFF; }
.queue-avatar.espera { background-color: #E2E8F0; color: #475569; }

.queue-info strong {
  display: block;
  font-size: 0.72rem;
  color: #1E293B;
}

.queue-info small {
  font-size: 0.65rem;
  color: #64748B;
}

.queue-item-right {
  text-align: right;
}

.queue-status-tag {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  display: inline-block;
}

.queue-status-tag.llamando { background-color: #DCFCE7; color: #166534; }
.queue-status-tag.espera { background-color: #F1F5F9; color: #64748B; }

.queue-time {
  display: block;
  font-size: 0.62rem;
  color: #64748B;
}

.queue-actions-row {
  display: flex;
  gap: 0.4rem;
}

.btn-call-next {
  flex: 1;
  background-color: #DBEAFE;
  color: #1D4ED8;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-popout-screen {
  background-color: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.red-central-card {
  padding: 0.75rem;
}

.red-central-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.mini-fachada {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
}

.red-central-row strong {
  display: block;
  font-size: 0.75rem;
  color: #1E293B;
}

.red-central-row p {
  font-size: 0.68rem;
  color: #64748B;
  margin: 0.15rem 0 0 0;
}

</style>
