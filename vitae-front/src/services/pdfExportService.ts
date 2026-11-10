import api from './api'

/**
 * Servicio Central de Exportación y Descarga de Documentos Clínicos y Administrativos en PDF
 * Hospital Vitae - Conforme a la Guía de Integración de Plantillas de Impresión
 * NOM-004-SSA3-2012, NOM-007-SSA3-2011, NOM-024-SSA3-2012, LFPDPPP
 */
export const pdfExportService = {
  /**
   * Helper para descargar cualquier Blob PDF devuelto por la API
   */
  async descargarBlobPdf(endpoint: string, nombreArchivoDefault: string): Promise<void> {
    const response = await api.get(endpoint, {
      responseType: 'blob',
      headers: {
        Accept: 'application/pdf',
      },
    })

    const blob = new Blob([response.data], { type: 'application/pdf' })
    const blobUrl = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = blobUrl
    link.download = nombreArchivoDefault
    document.body.appendChild(link)
    link.click()

    setTimeout(() => {
      document.body.removeChild(link)
      window.URL.revokeObjectURL(blobUrl)
    }, 150)
  },

  /**
   * Helper para abrir el PDF en una pestaña nueva para visualización o impresión directa con window.print()
   */
  async abrirPdfEnPestana(endpoint: string): Promise<void> {
    const separator = endpoint.includes('?') ? '&' : '?'
    const inlineEndpoint = `${endpoint}${separator}inline=1`

    const response = await api.get(inlineEndpoint, {
      responseType: 'blob',
      headers: {
        Accept: 'application/pdf',
      },
    })

    const blob = new Blob([response.data], { type: 'application/pdf' })
    const blobUrl = window.URL.createObjectURL(blob)
    window.open(blobUrl, '_blank')

    setTimeout(() => {
      window.URL.revokeObjectURL(blobUrl)
    }, 60000)
  },

  /**
   * Formato 1: Receta Médica Institucional con Código QR
   */
  async descargarRecetaPdf(idReceta: string, folio = 'REC-FOLIO'): Promise<void> {
    await this.descargarBlobPdf(`/recetas/${idReceta}/pdf`, `Receta-${folio}.pdf`)
  },

  /**
   * Formato 2: Hoja Oficial de Consentimiento Informado
   */
  async descargarConsentimientoPdf(idConsentimiento: string): Promise<void> {
    await this.descargarBlobPdf(`/consentimientos/${idConsentimiento}/pdf`, `Consentimiento-${idConsentimiento}.pdf`)
  },

  /**
   * Formato 3: Hoja Oficial de Enfermería y Somatometría
   */
  async descargarTriagePdf(idTriage: string, folio = 'TRIAGE'): Promise<void> {
    await this.descargarBlobPdf(`/triages/${idTriage}/pdf`, `Hoja-Enfermeria-Triage-${folio}.pdf`)
  },

  /**
   * Formato 4: Solicitud / Volante de Estudios de Laboratorio y Gabinete
   */
  async descargarSolicitudEstudioPdf(idSolicitud: string): Promise<void> {
    await this.descargarBlobPdf(`/solicitudes-estudio/${idSolicitud}/pdf`, `Volante-Laboratorio-${idSolicitud}.pdf`)
  },

  /**
   * Formato 5: Reporte de Resultados Validados de Laboratorio
   */
  async descargarResultadoEstudioPdf(idSolicitudOResultado: string): Promise<void> {
    await this.descargarBlobPdf(`/solicitudes-estudio/${idSolicitudOResultado}/resultados/pdf`, `Resultado-Laboratorio-${idSolicitudOResultado}.pdf`)
  },

  /**
   * Formato 6: Expediente Clínico Completo y Foliado / Resumen Clínico
   */
  async descargarExpedientePdf(idExpediente: string, enmascararPii = false): Promise<void> {
    const params = enmascararPii ? '?enmascarar_pii=1' : ''
    await this.descargarBlobPdf(`/expedientes-clinicos/${idExpediente}/pdf${params}`, `Expediente-${idExpediente}.pdf`)
  },
}

export default pdfExportService

