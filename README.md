# 🏥 Hospital Vitae - Monorepo

[![Laravel](https://img.shields.io/badge/Laravel-12.x-FF2D20?style=for-the-badge&logo=laravel&logoColor=white)](https://laravel.com)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org)
[![Vite](https://img.shields.io/badge/Vite-8.x-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://developer.android.com)
[![Vercel](https://img.shields.io/badge/Vercel-Deployment-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://vercel.com)
[![Render](https://img.shields.io/badge/Render-Deployment-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://render.com)

Monorepo del sistema de **Hospital Vitae**, desarrollado para la materia de **Auditoría de Software (9no Semestre)**. Integra el Backend de servicios RESTful, la Plataforma Web cliente, la Aplicación Móvil nativa y colecciones de pruebas de API.

---

## 📁 Estructura del Monorepo

```text
hospital-vitae-monorepo/
├── 📄 README.md              # Documentación general del Monorepo
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

* **Configuración del proyecto en Vercel:**
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
* **Uso:** Abrir Bruno y seleccionar la carpeta `vitae-bruno` para cargar todos los endpoints preconfigurados (GET, POST, PUT, DELETE).

---

## 🛠️ Despliegue en Producción

```mermaid
graph LR
    User([Usuario / Cliente]) -->|Navegador| Vercel[Frontend - Vercel]
    User -->|App Android| Mobile[Mobile App]
    Vercel -->|HTTPS / API REST| Render[Backend - Render]
    Mobile -->|HTTPS / API REST| Render
    Render -->|SQL| DB[(Base de Datos)]
```

---

## ✒️ Licencia & Créditos

Proyecto desarrollado como parte del programa académico de **Ingeniería de Software - 9no Semestre**.  
* **Repositorio oficial:** [github.com/Coria322/hospital-vitae-monorepo](https://github.com/Coria322/hospital-vitae-monorepo)
