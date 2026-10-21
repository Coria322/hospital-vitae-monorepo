import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DoctorWorkspaceView from '../views/doctor/DoctorWorkspaceView.vue'
import NurseWorkspaceView from '../views/nurse/NurseWorkspaceView.vue'
import ReceptionWorkspaceView from '../views/reception/ReceptionWorkspaceView.vue'
import HomeView from '../views/HomeView.vue'

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
    },
    {
      path: '/enfermeria',
      name: 'enfermeria',
      component: NurseWorkspaceView,
    },
    {
      path: '/recepcion',
      name: 'recepcion',
      component: ReceptionWorkspaceView,
    },
    {
      path: '/admision',
      redirect: '/recepcion'
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
