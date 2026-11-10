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
      meta: { requiresAuth: true, roles: ['MEDICO', 'ADMINISTRADOR'] },
    },
    {
      path: '/doctor',
      redirect: '/medico',
    },
    {
      path: '/enfermeria',
      name: 'enfermeria',
      component: NurseWorkspaceView,
      meta: { requiresAuth: true, roles: ['ENFERMERIA', 'ENFERMERO', 'ADMINISTRADOR'] },
    },
    {
      path: '/triage',
      redirect: '/enfermeria',
    },
    {
      path: '/recepcion',
      name: 'recepcion',
      component: ReceptionWorkspaceView,
      meta: { requiresAuth: true, roles: ['RECEPCION', 'ADMINISTRADOR'] },
    },
    {
      path: '/admision',
      redirect: '/recepcion',
    },
    {
      path: '/laboratorio',
      name: 'laboratorio',
      component: LabWorkspaceView,
      meta: { requiresAuth: true, roles: ['LABORATORISTA', 'LABORATORIO', 'ADMINISTRADOR'] },
    },
    {
      path: '/lab',
      redirect: '/laboratorio',
    },
    {
      path: '/administrador',
      name: 'administrador',
      component: AdminWorkspaceView,
      meta: { requiresAuth: true, roles: ['ADMINISTRADOR'] },
    },
    {
      path: '/admin',
      redirect: '/administrador',
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true, roles: ['ADMINISTRADOR', 'MEDICO', 'ENFERMERIA', 'ENFERMERO', 'RECEPCION', 'LABORATORISTA', 'LABORATORIO'] },
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

  // 1. Verificación de autenticación general
  if (to.meta.requiresAuth && !isAuth) {
    authService.limpiarSesion()
    return next({ path: '/' })
  }

  // 2. Control de Acceso Basado en Roles (RBAC / Menor Privilegio - NOM-024 / ISO-27001)
  if (isAuth && to.meta.roles && Array.isArray(to.meta.roles)) {
    const usuario = authService.getUsuario()
    const rol = (usuario?.rol || '').toUpperCase()
    const rolesPermitidos = (to.meta.roles as string[]).map(r => r.toUpperCase())

    if (!rolesPermitidos.includes(rol)) {
      const nombreModulo = String(to.name || to.path).toUpperCase()
      alert(`ACCESO ASISTENCIAL DENEGADO (RBAC / NOM-024)\n\nEl módulo '${nombreModulo}' está reservado exclusivamente para perfiles: [${rolesPermitidos.join(', ')}].\n\nTu sesión activa tiene el rol '${rol}'. Por principio de menor privilegio y seguridad clínica, serás redirigido a tu módulo autorizado.`)

      if (rol === 'MEDICO') return next({ path: '/medico' })
      if (rol === 'ENFERMERIA' || rol === 'ENFERMERO') return next({ path: '/enfermeria' })
      if (rol === 'LABORATORISTA' || rol === 'LABORATORIO') return next({ path: '/laboratorio' })
      if (rol === 'RECEPCION') return next({ path: '/recepcion' })
      if (rol === 'ADMINISTRADOR') return next({ path: '/administrador' })
      return next({ path: '/' })
    }
  }

  // 3. Validación de asignación física de guardia (Médico y Enfermero requieren Box/Consultorio asignado)
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

  // 4. Si ya tiene sesión activa e intenta ir al login, dirigir a su área de trabajo
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
