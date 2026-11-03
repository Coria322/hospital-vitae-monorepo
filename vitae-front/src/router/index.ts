import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DoctorWorkspaceView from '../views/doctor/DoctorWorkspaceView.vue'
import NurseWorkspaceView from '../views/nurse/NurseWorkspaceView.vue'
import ReceptionWorkspaceView from '../views/reception/ReceptionWorkspaceView.vue'
import LabWorkspaceView from '../views/lab/LabWorkspaceView.vue'
import AdminWorkspaceView from '../views/admin/AdminWorkspaceView.vue'
import HomeView from '../views/HomeView.vue'
import authService from '../services/authService'

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
      path: '/enfermeria',
      name: 'enfermeria',
      component: NurseWorkspaceView,
      meta: { requiresAuth: true },
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

  // Si ya tiene sesión activa e intenta ir al login, dirigir a su área de trabajo
  if (to.path === '/' && isAuth) {
    const usuario = authService.getUsuario()
    const rol = (usuario?.rol || '').toUpperCase()

    if (rol === 'MEDICO') return next({ path: '/medico' })
    if (rol === 'ENFERMERIA') return next({ path: '/enfermeria' })
    if (rol === 'ADMINISTRADOR' || rol === 'RECEPCION') return next({ path: '/recepcion' })
  }

  next()
})

export default router
