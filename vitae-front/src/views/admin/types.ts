export type RolHospitalario =
  | 'MEDICO'
  | 'ENFERMERIA'
  | 'RECEPCION'
  | 'LABORATORIO'
  | 'ADMINISTRADOR'

export type EstadoUsuario = 'ACTIVO' | 'INACTIVO' | 'BLOQUEADO'

export interface UsuarioAdmin {
  id: string
  nombre_completo: string
  iniciales: string
  cedula_matricula: string
  curp: string
  correo_institucional: string
  departamento: string
  rol: RolHospitalario
  rol_etiqueta: string
  mfa_activo: boolean
  estado: EstadoUsuario
  ultimo_acceso_fecha: string
  ultimo_acceso_ip: string
  terminal_origen: string
  bloqueos_previos?: number
}

export interface CuentaBloqueada {
  id: string
  id_usuario: string
  nombre: string
  iniciales: string
  rol: string
  servicio: string
  correo: string
  terminal: string
  ip: string
  hora_bloqueo: string
  tiempo_transcurrido: string
  intentos_fallidos: number
  motivo_bloqueo: string
  pacientes_en_espera: number
  impacto_clinico: 'ALTO' | 'MEDIO' | 'BAJO'
}

export interface TrazaAuditoria {
  id: string
  timestamp_ntp: string
  usuario_nombre: string
  usuario_id: string
  usuario_rol: string
  ip_origen: string
  mac_address: string
  terminal: string
  accion: string
  recurso_disociado: string
  resultado_codigo: number
  resultado_etiqueta: string
  hash_sha256: string
  jwt_token_preview: string
}

export interface ContenedorDocker {
  id: string
  nombre: string
  imagen_version: string
  estado: 'ONLINE' | 'SALUDABLE' | 'SINCRONIZADO' | 'DEGRADADO' | 'ACTIVO'
  puertos: string
  latencia_rtt: string
  descripcion: string
  conexiones_activas?: number
  conexiones_max?: number
  detalles_tecnicos?: string
}

export interface TelemetriaCluster {
  cpu_modelo: string
  cpu_uso_pct: number
  cpu_carga_media: number
  cpu_nucleos: number
  ram_usada_gb: number
  ram_total_gb: number
  ram_uso_pct: number
  ram_buffer_gb: number
  nvme_usado_tb: number
  nvme_total_tb: number
  nvme_libre_tb: number
  nvme_io_rate: string
  red_vlan: string
  red_latencia_ms: number
  red_throughput: string
  red_paquetes_perdidos_pct: number
}
