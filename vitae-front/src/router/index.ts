import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DoctorWorkspaceView from '../views/doctor/DoctorWorkspaceView.vue'
import NurseWorkspaceView from '../views/nurse/NurseWorkspaceView.vue'
import ReceptionWorkspaceView from '../views/reception/ReceptionWorkspaceView.vue'
import LabWorkspaceView from '../views/lab/LabWorkspaceView.vue'
import AdminWorkspaceView from '../views/admin/AdminWorkspaceView.vue'
import SalaEsperaPublicaView from '../views/SalaEsperaPublicaView.vue'
import HomeView from '../views/HomeView.vue'
import authService from '../services/authService'
import guardiasService from '../services/guardiasService'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/medico',
      name: 'medico',
      component: DoctorWorkspaceView,
      meta: { requiresAuth: true },
    },
    {
      path: '/doctor',
      redirect: '/medico',
    },
    {
      path: '/enfermeria',
      name: 'enfermeria',
      component: NurseWorkspaceView,
      meta: { requiresAuth: true },
    },
    {
      path: '/triage',
      redirect: '/enfermeria',
    },
    {
      path: '/recepcion',
      name: 'recepcion',
      component: ReceptionWorkspaceView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admision',
      redirect: '/recepcion',
    },
    {
      path: '/laboratorio',
      name: 'laboratorio',
      component: LabWorkspaceView,
      meta: { requiresAuth: true },
    },
    {
      path: '/lab',
      redirect: '/laboratorio',
    },
    {
      path: '/administrador',
      name: 'administrador',
      component: AdminWorkspaceView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      redirect: '/administrador',
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/sala-espera',
      name: 'sala-espera-publica',
      component: SalaEsperaPublicaView,
      meta: { requiresAuth: false },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

router.beforeEach((to, from, next) => {
  const isAuth = authService.isAuthenticated()

  if (to.meta.requiresAuth && !isAuth) {
    authService.limpiarSesion()
    return next({ path: '/' })
  }

  // Validación de asignación física de guardia (Médico y Enfermero requieren Box/Consultorio asignado)
  const esRutaClinica = to.path.startsWith('/medico') || to.path.startsWith('/doctor') || to.path.startsWith('/enfermeria') || to.path.startsWith('/triage')
  if (isAuth && esRutaClinica) {
    const usuario = authService.getUsuario()
    const rol = (usuario?.rol || '').toUpperCase()
    if (rol === 'MEDICO' || rol === 'ENFERMERIA' || rol === 'ENFERMERO') {
      const asignado = guardiasService.estaPersonalAsignado(
        usuario?.id_usuario,
        usuario?.nombre_completo,
        rol,
        usuario?.cedula || undefined
      )
      if (!asignado) {
        alert(`ACCESO ASISTENCIAL BLOQUEADO: El profesional ${usuario?.nombre_completo || ''} (${rol}) no cuenta con un Consultorio, Box o Zona asignada para el turno activo. Solicite al Administrador de Guardia su asignación física antes de ingresar al módulo clínico.`)
        return next({ path: '/home' })
      }
    }
  }

  // Protección de rutas por rol institucional
  if (isAuth && (to.path.startsWith('/administrador') || to.path === '/admin')) {
    const usuario = authService.getUsuario()
    const rol = (usuario?.rol || '').toUpperCase()
    if (rol !== 'ADMINISTRADOR') {
      alert(`Acceso restringido: El panel de administración requiere credenciales de ADMINISTRADOR (ej. ADM-001). Tu sesión actual tiene rol '${rol}'.`)
      if (rol === 'MEDICO') return next({ path: '/medico' })
      if (rol === 'ENFERMERIA') return next({ path: '/enfermeria' })
      if (rol === 'LABORATORISTA' || rol === 'LABORATORIO') return next({ path: '/laboratorio' })
      return next({ path: '/recepcion' })
    }
  }

  // Si ya tiene sesión activa e intenta ir al login, dirigir a su área de trabajo
  if (to.path === '/' && isAuth) {
    const usuario = authService.getUsuario()
    const rol = (usuario?.rol || '').toUpperCase()

    if (rol === 'ADMINISTRADOR') return next({ path: '/administrador' })
    if (rol === 'MEDICO') {
      const asignado = guardiasService.estaPersonalAsignado(usuario?.id_usuario, usuario?.nombre_completo, rol, usuario?.cedula || undefined)
      return next({ path: asignado ? '/medico' : '/home' })
    }
    if (rol === 'ENFERMERIA' || rol === 'ENFERMERO') {
      const asignado = guardiasService.estaPersonalAsignado(usuario?.id_usuario, usuario?.nombre_completo, rol, usuario?.cedula || undefined)
      return next({ path: asignado ? '/enfermeria' : '/home' })
    }
    if (rol === 'LABORATORISTA' || rol === 'LABORATORIO') return next({ path: '/laboratorio' })
    if (rol === 'RECEPCION') return next({ path: '/recepcion' })
  }

  next()
})

export default router
