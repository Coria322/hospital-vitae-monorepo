# 🤖 AGENTS.md — Hospital Vitae (Monorepo)

> **Documento Central de Directrices para Agentes y Desarrolladores**  
> Ver documento completo y exhaustivo en: [sih-hospitalario_agents/AGENTS.md](file:///c:/Ingenieria_software/9no%20semestre/auditoria/Proyecto/sih-hospitalario_agents/AGENTS.md)

---

## 🚨 REGLAS CRÍTICAS DE CONTROL DE VERSIONES (GIT)

1. 🚫 **NUNCA HACER COMMIT A `main`**:
   - La rama `main` es sagrada y protegida. Contiene únicamente releases estables y auditados.
   - Ningún desarrollador ni agente tiene permitido commitear directamente a `main`.

2. 🌿 **TODA RAMIFICACIÓN SALE DE `dev` Y MERGEA A `dev`**:
   - `dev` es la rama base de trabajo e integración activa.
   - Ramas temporales obligatorias:
     - `feature/<alcance>-<descripcion>` (nuevas funciones)
     - `bugfix/<alcance>-<descripcion>` (correcciones)
     - `refactor/<alcance>-<descripcion>`
     - `docs/<alcance>-<descripcion>`
     - `test/<alcance>-<descripcion>`
     - `chore/<alcance>-<descripcion>`
   - La integración a `dev` se realiza **exclusivamente mediante Pull Requests (PR)** aprobados por revisión por pares y con CI exitoso.

3. 📝 **NOMENCLATURA Y ESTRUCTURA DE COMMITS (Conventional Commits)**:
   ```
   <tipo>(<alcance>): <descripción corta en imperativo>

   [cuerpo opcional]

   Refs: #TASK-ID, Req: RF-XXX, Audit: AI-0X
   ```
   - **Tipos válidos:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.
   - **Alcances válidos:** `backend`, `front`, `mobile`, `bruno`, `db`, `infra`, `auth`, `triage`, `expediente`, `recetas`, `auditoria`, `fhir`.
   - **Sin mensajes genéricos:** Prohibido usar `"cambios"`, `"wip"`, `"fix"`, etc.

4. 🔒 **SEGURIDAD Y NORMATIVAS (NOM-004, NOM-024, ISO 27001)**:
   - **Cero secretos:** Prohibido subir tokens, contraseñas, `.env` o llaves privadas.
   - **Cero datos reales:** Prohibido subir datos de pacientes reales (PII). Usar únicamente datos sintéticos.
   - **Inmutabilidad:** Jamás permitir `UPDATE` ni `DELETE` en la tabla `AUDITORIA_I`.

---

Para consultar la guía completa con ejemplos, tablas de comandos y matriz de trazabilidad, revise [sih-hospitalario_agents/AGENTS.md](file:///c:/Ingenieria_software/9no%20semestre/auditoria/Proyecto/sih-hospitalario_agents/AGENTS.md).
