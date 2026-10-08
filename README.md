# 🏥 Hospital Vitae - Monorepo

[![Laravel](https://img.shields.io/badge/Laravel-12.x-FF2D20?style=for-the-badge&logo=laravel&logoColor=white)](https://laravel.com)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org)
[![Vite](https://img.shields.io/badge/Vite-8.x-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://developer.android.com)
[![Vercel](https://img.shields.io/badge/Vercel-Deployment-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://vercel.com)
[![Render](https://img.shields.io/badge/Render-Deployment-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://render.com)

Monorepo del sistema del **Hospital Vitae**, desarrollado para la materia de **Auditoría de Software (9no Semestre)**. Integra el Backend de servicios RESTful, la Plataforma Web cliente, la Aplicación Móvil nativa y colecciones de pruebas de API.

---

## 📊 Diagramas del Sistema

### 1. 🏗️ Arquitectura General

```mermaid
graph TD
    subgraph Clientes ["Capa de Clientes"]
        Web["🌐 Web Client (Vue 3 + Vite)<br/>Desplegado en Vercel"]
        Mobile["📱 App Móvil (Kotlin)<br/>Android Native"]
        Bruno["🧪 API Tester<br/>Colección Bruno"]
    end

    subgraph Servicios ["Capa de Servicios"]
        API["🔌 API Gateway & Routing<br/>Laravel 12 (Render / Docker)"]
        Sanctum["🔐 Auth & Security<br/>Laravel Sanctum"]
        AuditModule["📋 Módulo de Auditorías<br/>AuditoriaController"]
    end

    subgraph Datos ["Capa de Almacenamiento"]
        DB[("🛢️ Base de Datos<br/>PostgreSQL / SQLite")]
    end

    Web -->|HTTP / JSON| API
    Mobile -->|HTTP / JSON| API
    Bruno -->|HTTP / JSON| API
    API --> Sanctum
    API --> AuditModule
    AuditModule --> DB
```

---

### 2. 🔀 Flujo de Trabajo y Secuencia de Auditoría

```mermaid
sequenceDiagram
    autonumber
    actor Auditor as 👤 Auditor / Usuario
    participant Front as 🌐 Frontend (Vue 3 / Vercel)
    participant Back as ⚙️ Backend (Laravel 12 / Render)
    participant DB as 🛢️ Base de Datos

    Auditor->>Front: Ingresa / Consulta registros de auditoría
    Front->>Back: GET /api/auditorias (Headers: Bearer Token)
    Back->>Back: Valida autenticación (Sanctum)
    Back->>DB: Query SELECT * FROM auditorias
    DB-->>Back: Retorna colección de datos
    Back-->>Front: Respuesta JSON (200 OK + Payload)
    Front-->>Auditor: Muestra tabla interactiva de auditoría
```

---

### 3. 🧩 Estructura de Componentes del Monorepo

```mermaid
graph LR
    subgraph Monorepo ["hospital-vitae-monorepo"]
        direction TB
        F["📂 vitae-front<br/>(Vue 3, Vite, TS)"]
        B["📂 vitae-backend<br/>(Laravel 12, PHP 8.2)"]
        M["📂 vitae-mobile<br/>(Kotlin, Gradle)"]
        T["📂 vitae-bruno<br/>(OpenCollection API)"]
    end

    F -.->|Consume API REST| B
    M -.->|Consume API REST| B
    T -.->|Audita & Prueba| B
```

---

## 📁 Estructura de Directorios

```text
hospital-vitae-monorepo/
├── 📄 README.md              # Documentación general con diagramas Mermaid
├── 📄 .gitignore             # Exclusiones globales de Git
├── 📂 vitae-backend/         # API RESTful en Laravel 12 (PHP 8.2+)
├── 📂 vitae-front/           # Aplicación Web en Vue 3 + Vite + TypeScript
├── 📂 vitae-mobile/          # Aplicación Móvil nativa Android (Kotlin)
└── 📂 vitae-bruno/           # Colección de pruebas de API con Bruno
```

---

## 🚀 Componentes del Proyecto

### 1. ⚙️ Backend (`vitae-backend`)
API principal encargada de la lógica de negocio, módulos de auditoría y almacenamiento de datos.
* **Tecnologías:** PHP 8.2+, Laravel 12, SQLite / PostgreSQL, Sanctum, Docker.
* **Despliegue:** Preparado para [Render](https://render.com/) mediante `Dockerfile` y `render.yaml`.

#### Configuración Local:
```bash
cd vitae-backend
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
php artisan serve
```

---

### 2. 💻 Frontend Web (`vitae-front`)
Interfaz web de usuario moderna para la gestión de auditorías hospitalarias.
* **Tecnologías:** Vue.js 3, Vite, TypeScript, Vue Router.
* **Despliegue:** Configurado para [Vercel](https://vercel.com/).

#### Configuración Local:
```bash
cd vitae-front
npm install
npm run dev
```

#### Variables de Entorno en Vercel:
| Variable | Descripción | Ejemplo |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | URL de la API Backend en Render | `https://vitae-backend.onrender.com/api` |

* **Configuración en Vercel:**
  * **Root Directory:** `vitae-front`
  * **Framework Preset:** `Vite`
  * **Build Command:** `npm run build`
  * **Output Directory:** `dist`

---

### 3. 📱 Aplicación Móvil (`vitae-mobile`)
Cliente móvil nativo para Android.
* **Tecnologías:** Kotlin, Android SDK, Gradle KTS.

#### Compilación / Ejecución:
* Abrir el directorio `vitae-mobile` en **Android Studio**.
* Sincronizar Gradle y ejecutar en emulador o dispositivo físico.

---

### 4. 🧪 Colección de Pruebas API (`vitae-bruno`)
Colección de solicitudes HTTP estructuradas para auditar y probar la API.
* **Herramienta:** [Bruno API Client](https://www.usebruno.com/).
* **Uso:** Abrir Bruno y seleccionar la carpeta `vitae-bruno` para cargar los endpoints preconfigurados.

---

## ✒️ Licencia & Créditos

Proyecto desarrollado como parte del programa académico de **Ingeniería de Software - 9no Semestre**.  
* **Repositorio oficial:** [github.com/Coria322/hospital-vitae-monorepo](https://github.com/Coria322/hospital-vitae-monorepo)
