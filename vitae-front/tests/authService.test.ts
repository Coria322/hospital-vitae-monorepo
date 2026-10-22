import assert from 'node:assert/strict'

// Mock de localStorage y window para entorno Node
const mockStorage: Record<string, string> = {}
;(globalThis as any).localStorage = {
  getItem: (key: string) => mockStorage[key] ?? null,
  setItem: (key: string, value: string) => {
    mockStorage[key] = String(value)
  },
  removeItem: (key: string) => {
    delete mockStorage[key]
  },
  clear: () => {
    for (const key in mockStorage) delete mockStorage[key]
  },
}

;(globalThis as any).window = {
  setTimeout: (fn: () => void, ms: number) => setTimeout(fn, ms),
  clearTimeout: (id: any) => clearTimeout(id),
  location: { pathname: '/medico', href: '/medico' },
}

async function runTests() {
  console.log('🧪 Iniciando pruebas unitarias de authService y refresh token...')

  // Importar authService
  const { authService } = await import('../src/services/authService')

  // 1. Verificar estado inicial sin sesión
  authService.limpiarSesion()
  assert.equal(authService.isAuthenticated(), false, 'Sin token no debe estar autenticado')
  assert.equal(authService.getToken(), null, 'Token debe ser null')
  assert.equal(authService.getUsuario(), null, 'Usuario debe ser null')
  console.log('✓ Estado inicial sin sesión validado')

  // 2. Guardar sesión con token y expiración
  const testUser = {
    id_usuario: 'MED-TEST',
    nombre_completo: 'Dr. Test',
    rol: 'MEDICO',
    cedula: '1234567890',
    estado_cuenta: 'ACTIVA',
  }

  authService.guardarSesion({
    token: 'token_prueba_123',
    token_type: 'Bearer',
    expires_in: 900, // 15 minutos
    usuario: testUser,
  })

  assert.equal(authService.isAuthenticated(), true, 'Debe estar autenticado tras guardar sesión')
  assert.equal(authService.getToken(), 'token_prueba_123', 'Token debe coincidir')
  assert.deepEqual(authService.getUsuario(), testUser, 'Usuario debe coincidir')

  const restante = authService.getTiempoRestanteMs()
  assert.ok(restante > 890000 && restante <= 900000, `Tiempo restante debe rondar 900s: ${restante}ms`)
  console.log(`✓ Sesión y tiempo restante (${Math.round(restante / 1000)}s) validados`)

  // 3. Simulación de expiración
  mockStorage['vitae_token_expires_at'] = String(Date.now() - 5000)
  assert.equal(authService.isAuthenticated(), false, 'Si el tiempo expiró, isAuthenticated debe ser false')
  console.log('✓ Detección de token expirado validada')

  // 4. Limpieza de sesión
  authService.limpiarSesion()
  assert.equal(authService.getToken(), null, 'Token debe ser null tras limpiar')
  assert.equal(mockStorage['vitae_token'], undefined, 'Storage no debe tener vitae_token')
  assert.equal(mockStorage['vitae_token_expires_at'], undefined, 'Storage no debe tener expires_at')
  console.log('✓ Limpieza total de sesión validada')

  console.log('🎉 ¡Todas las pruebas unitarias pasaron con éxito!')
}

runTests().catch((err) => {
  console.error('❌ Error en pruebas:', err)
  process.exit(1)
})
