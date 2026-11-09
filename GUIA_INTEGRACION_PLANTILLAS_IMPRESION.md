# GUÍA DE INTEGRACIÓN DE PLANTILLAS DE IMPRESIÓN Y PDF - HOSPITAL VITAE

Esta guía técnica está dirigida al desarrollador o agente de IA encargado de diseñar e integrar las plantillas de impresión física y exportación a PDF para el Sistema Hospitalario Vitae.

---

## 1. Arquitectura Técnica y Estándares de Salida

El sistema opera bajo dos modalidades de salida según el tipo de documento:

1. **Documentos Clínicos y Administrativos (Formato Carta / Media Carta):**
   - Recetas médicas, notas clínicas, consentimientos informados, resultados de laboratorio y reportes de auditoría.
   - Especificación CSS: `@page { size: letter portrait; margin: 12mm 15mm 15mm 15mm; }`
2. **Documentos Térmicos POS (Rollos continuos de 58 mm o 80 mm):**
   - Tickets de turno y comprobantes rápidos de cita.
   - Especificación CSS: `@page { size: 80mm auto; margin: 2mm; }`
3. **Etiquetas Adhesivas de Laboratorio:**
   - Tubos de ensayo y muestras biológicas.
   - Especificación CSS: `@page { size: 50mm 25mm landscape; margin: 1mm; }`

---

## 2. Inventario de Formatos y Puntos de Integración

### Formato 1: Receta Médica Institucional con Código QR
* **Módulo:** Médico / Consulta Externa y Urgencias.
* **Componente Frontend:** `src/views/doctor/components/EmitirRecetaSection.vue`
* **Endpoint Backend alternativo:** `GET /api/v1/recetas/{id}/pdf`
* **Datos requeridos (Payload):**
  - Folio único de receta (`REC-XXXX`).
  - Datos del médico: Nombre completo, Cédula profesional, Cédula de especialidad, Institución educativa.
  - Datos del paciente: Nombre, CURP, Edad, Sexo, Peso, Talla, Diagnóstico principal (CIE-10).
  - Medicamentos prescritos: Denominación genérica, forma farmacéutica, dosis, vía, frecuencia, duración e indicaciones.
  - Sello digital: Cadena Hash SHA-256 generada al guardar la receta.
  - Código QR: Contiene la URL de validación única en tiempo real (`https://vitae.hospital/verificar-receta?folio=REC-XXXX&hash=...`).
* **Normativa:** NOM-004-SSA3-2012 / COFEPRIS / Art. 65 del Reglamento de Insumos para la Salud.

### Formato 2: Hoja de Consentimiento Informado
* **Módulo:** Recepción / Admisión y Enfermería.
* **Componente Frontend:** `src/views/reception/components/InformedConsentSection.vue`
* **Datos requeridos:**
  - Procedimiento o intervención autorizada.
  - Riesgos informados y alternativas terapéuticas.
  - Datos del paciente o tutor legal (Nombre, CURP, Parentesco en su caso).
  - Nombre y cédula del profesional responsable.
  - Nombres y firmas de dos testigos.
  - Recuadro para firma física o firma digitalizada y huella dactilar.
  - Folio de expediente clínico ECE.
* **Normativa:** NOM-004-SSA3-2012, numeral 10.1.1 / Ley Federal de Protección de Datos Personales.

### Formato 3: Hoja Oficial de Enfermería y Somatometría
* **Módulo:** Enfermería / Triaje.
* **Componente Frontend:** `src/views/nurse/components/NotaEnfermeriaSection.vue`
* **Datos requeridos:**
  - Signos vitales: Presión Arterial (mmHg), Frecuencia Cardíaca (lpm), Frecuencia Respiratoria (rpm), Temperatura (°C), SpO2 (%).
  - Somatometría: Peso (kg), Talla (cm), IMC calculado, Perímetro abdominal.
  - Escalas clínicas: Glasgow, EVA (dolor), Braden o Norton (si aplica).
  - Nombre, matrícula y cédula profesional de la enfermera responsable.
* **Normativa:** NOM-004-SSA3-2012, numeral 9.

### Formato 4: Solicitud / Volante de Estudios de Laboratorio y Gabinete
* **Módulo:** Consulta Médica.
* **Componente Frontend:** `src/views/doctor/components/SolicitarLaboratorioSection.vue`
* **Datos requeridos:**
  - Folio institucional de solicitud (`LAB-XXXX`).
  - Código de barras o QR para lectura óptica por el flebotomista.
  - Lista de análisis solicitados agrupados por área (Biometría, Química sanguínea, Examen general de orina, etc.).
  - Diagnóstico presuntivo y justificación médica.
  - Indicaciones de preparación para el paciente (horas de ayuno, recolección de muestra).
* **Normativa:** NOM-007-SSA3-2011 / NOM-004-SSA3-2012.

### Formato 5: Reporte de Resultados Validados de Laboratorio
* **Módulo:** Laboratorio Clínico (LIS).
* **Componente Frontend:** `src/views/lab/components/LabCargarResultadosSection.vue`
* **Datos requeridos:**
  - Datos del paciente, fecha de toma y fecha de emisión.
  - Tabla de resultados: Prueba, Resultado, Unidades, Intervalos biológicos de referencia.
  - Marcadores de valores críticos o de pánico destacados.
  - Cédula profesional y firma del Químico Farmacobiólogo (Q.F.B.) responsable.
* **Normativa:** NOM-007-SSA3-2011.

### Formato 6: Expediente Clínico Completo y Foliado / Resumen Clínico
* **Módulo:** Consulta Médica / Dirección Médica.
* **Componente Frontend:** `src/views/doctor/components/ClinicalRecordSection.vue`
* **Endpoint Backend:** `POST /api/v1/expedientes-clinicos/{id}/exportaciones`
* **Datos requeridos:**
  - Carátula de identificación del paciente con antecedentes heredo-familiares y patológicos.
  - Notas médicas ordenadas cronológicamente con sello de tiempo UTC.
  - Marca de agua transversal de seguridad: "COPIA FIDEDIGNA DEL EXPEDIENTE - FOLIO AUDITORÍA [UUID]".
* **Normativa:** NOM-004-SSA3-2012, numerales 5.6 y 5.7.

---

## 3. Patrón de Implementación Recomendado en Vue 3

Para evitar dependencias pesadas de servidor o librerías externas que rompan la reactividad, se recomienda implementar componentes de impresión con estilos CSS print dedicados:

```html
<!-- Ejemplo de contenedor imprimible en cualquier componente Vue -->
<template>
  <div class="no-print">
    <!-- Interfaz de pantalla normal -->
    <button @click="imprimirDocumento" class="btn-imprimir">
      Imprimir Formato
    </button>
  </div>

  <!-- Contenedor visible únicamente al mandar a imprimir -->
  <div id="seccion-imprimible" class="solo-impresion">
    <div class="hoja-membretada">
      <!-- Encabezado Institucional -->
      <header class="encabezado-hospital">
        <img src="/logo-vitae.png" alt="Hospital Vitae" class="logo-print" />
        <div class="datos-sede">
          <h2>COMPLEJO HOSPITALARIO VITAE</h2>
          <p>Dirección General Médica · Sede Central</p>
          <small>Clave Única de Establecimientos de Salud (CLUES)</small>
        </div>
      </header>

      <!-- Cuerpo del documento -->
      <main class="cuerpo-documento">
        <!-- Renderizado de datos del paciente, receta, nota o consentimiento -->
      </main>

      <!-- Pie de página con sellos legales y QR -->
      <footer class="pie-seguridad">
        <div class="qr-container">
          <!-- Componente QR o imagen SVG generada -->
        </div>
        <div class="sellos-legales">
          <span>Cadena Original SHA-256: {{ hashSha256 }}</span>
          <span>Sincronización horaria: CENAM UTC · Verificación de autenticidad activa</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
const imprimirDocumento = () => {
  window.print()
}
</script>

<style scoped>
/* Ocultar la plantilla en la pantalla regular */
.solo-impresion {
  display: none;
}

/* Reglas exclusivas para el motor de impresión del navegador */
@media print {
  /* Ocultar toda la navegación del sistema hospitalario */
  .no-print,
  nav,
  aside,
  header,
  .left-sidebar,
  .top-header,
  .btn-action {
    display: none !important;
  }

  /* Mostrar exclusivamente el documento oficial */
  .solo-impresion {
    display: block !important;
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    background: #FFFFFF;
    color: #000000;
  }

  @page {
    size: letter portrait;
    margin: 15mm;
  }

  .hoja-membretada {
    font-family: Arial, sans-serif;
    font-size: 10pt;
    line-height: 1.3;
  }

  /* Evitar cortes indeseados en tablas o bloques de firma */
  .bloque-firma,
  .fila-medicamento,
  .tabla-signos {
    page-break-inside: avoid;
    break-inside: avoid;
  }
}
</style>
```

---

## 4. Checklist de Validación para el Agente

Antes de dar por concluida la plantilla de cada formato, verificar:
- [ ] No utiliza colores oscuros de fondo que desgasten tinta de impresión innecesariamente.
- [ ] La tipografía se renderiza con nitidez en blanco y negro (escala de grises limpia).
- [ ] La cédula profesional del médico y la institución emisora son claramente legibles.
- [ ] El código QR tiene un contraste óptimo (mínimo 25x25 mm) para escaneo ágil desde dispositivos móviles.
- [ ] La leyenda de confidencialidad y derechos ARCO (LFPDPPP) está presente al calce del documento.
