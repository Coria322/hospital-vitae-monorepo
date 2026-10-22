import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import authService from './services/authService'

// Inicializar el ciclo de vida de autenticación y auto-refresh de tokens
authService.initAuth()

const app = createApp(App)

app.use(router)

app.mount('#app')
