# 🏥 Arquitectura de Módulos y Vistas (Views) · Hospital Vitae

## 1. Introducción
Este directorio (`src/views/`) contiene las vistas y espacios de trabajo clínicos del frontend de **Hospital Vitae**. El sistema está diseñado con una arquitectura modular por roles que desacopla la lógica de negocio y las interfaces de usuario según el perfil del personal de salud:

```
src/views/
├── LoginView.vue                 # Pantalla de acceso institucional con validación API
├── README.md                     # Esta guía general de arquitectura
│
├── doctor/                       # 🩺 Módulo Médico (Consulta, Expediente, Recetas, Notas ECE)
│   ├── DoctorWorkspaceView.vue
│   ├── types.ts
│   ├── README.md                 # Documentación específica del módulo médico
│   └── components/
│
├── nurse/                        # 👩‍⚕️ Módulo de Enfermería (Triage Manchester, Kardex, Sala de espera)
│   ├── NurseWorkspaceView.vue
│   ├── README.md                 # Documentación específica del módulo de enfermería
│   └── components/
│
└── reception/                    # 📋 Módulo de Recepción (CURP, Registro, Citas, Consentimiento, Camas)
    ├── ReceptionWorkspaceView.vue
    ├── README.md                 # Documentación específica del módulo de recepción
    └── components/
```

---

## 2. Flujo de Autenticación y Conexión con la API

### 2.1 Librería y Configuración de Red
* **Cliente HTTP:** Se utiliza **Axios versión `1.20.0`** (fijada textualmente en `package.json` para garantizar estabilidad y seguridad).
* **URL Base de la API:** `http://127.0.0.1:8000/api/v1` (configurada en `.env` y con proxy inverso en `vite.config.ts`).
* **Servicio Central:** [`src/services/api.ts`](file:///C:/Users/Admin/Desktop/monorepo/hospital-vitae-monorepo/vitae-front/src/services/api.ts) inyecta de forma automática la cabecera `Authorization: Bearer <token>` en cada solicitud una vez iniciada la sesión.

### 2.2 Redirección por Roles (RBAC)
Al iniciar sesión en [`LoginView.vue`](file:///C:/Users/Admin/Desktop/monorepo/hospital-vitae-monorepo/vitae-front/src/views/LoginView.vue) contra el endpoint `POST /api/v1/auth/login`, la API devuelve el rol oficial del usuario, redirigiendo automáticamente:

```typescript
const rol = usuario.rol.toUpperCase()

if (rol === 'MEDICO') {
  router.push('/medico')       // -> DoctorWorkspaceView.vue
} else if (rol === 'ENFERMERIA') {
  router.push('/enfermeria')   // -> NurseWorkspaceView.vue
} else if (rol === 'ADMINISTRADOR' || rol === 'RECEPCION') {
  router.push('/recepcion')    // -> ReceptionWorkspaceView.vue
}
```

---

## 3. Cuentas de Prueba Pre-sembradas en Base de Datos (SQLite)

Para que el equipo pueda probar inmediatamente todos los flujos sin crear registros manuales:

| Usuario / Matrícula | Contraseña | Rol Oficial | Espacio Asignado | Nombre del Personal |
| :--- | :--- | :--- | :--- | :--- |
| `MED-001` | `password123` | `MEDICO` | `/medico` | Dr. Carlos Rivera |
| `ENF-001` | `password123` | `ENFERMERIA` | `/enfermeria` | Enf. Laura Gómez |
| `ADM-001` | `password123` | `ADMINISTRADOR` | `/recepcion` | Administrador Sistema Vitae |

---

## 4. Patrones de Diseño Compartidos entre Módulos

1. **Persistencia del Paciente Activo:**
   * Cada orquestador (`DoctorWorkspaceView.vue`, `NurseWorkspaceView.vue`, `ReceptionWorkspaceView.vue`) mantiene un estado reactivo centralizado del paciente.
   * La información viaja limpiamente hacia los componentes hijos mediante `props`, y los componentes hijos notifican actualizaciones mediante `emits`.
   * **Beneficio:** Si cambias de pestaña o de herramienta rápida, no se pierden los datos del paciente ni sus signos vitales.

2. **Encabezado y Menú Flotante de Usuario:**
   * Todos los módulos implementan un encabezado estandarizado (`DoctorHeader.vue`, `NurseHeader.vue`, `ReceptionHeader.vue`).
   * Al presionar el botón de tres puntos verticales (`⋮`), se despliega una tarjeta flotante con el perfil del usuario, el rol activo y la opción **🚪 Cerrar sesión**, la cual revoca el token en el backend y devuelve al usuario al login.
   * El avatar calcula automáticamente las iniciales dinámicas del nombre del personal autenticado.

3. **Barras Laterales Compactas (52px):**
   * Se sustituyeron los banners fotográficos pesados por identificadores institucionales compactos de 52px con overlay oscuro, maximizando el espacio vertical para herramientas clínicas y listas de espera.

4. **Compatibilidad con Normas Sanitarias Mexicanas:**
   * **NOM-004-SSA3-2012:** Expediente clínico, notas de evolución estructuradas SOAP/SOAPIE y consentimiento informado.
   * **NOM-024-SSA3-2012:** Interoperabilidad de sistemas de información clínica y trazabilidad inalterable de auditoría.
   * **NOM-027-SSA3-2013:** Regulación del triage y servicios de urgencias médicas bajo protocolo Manchester.

---

## 5. Instrucciones para Ejecutar el Proyecto Localmente

### 1. Iniciar el Backend (Laravel)
En una terminal:
```bash
cd vitae-backend
# Si es la primera vez o la base de datos está limpia:
php artisan migrate --seed
# Levantar el servidor API:
php artisan serve
```
El servidor responderá en `http://127.0.0.1:8000`.

### 2. Iniciar el Frontend (Vue 3 + Vite)
En otra terminal:
```bash
cd vitae-front
npm install
npm run dev
```
La aplicación web estará disponible en `http://localhost:5173/`.
