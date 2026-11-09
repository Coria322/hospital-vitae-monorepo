# Evidencia AI-05 — App móvil (Seguridad de la información)

Paquete de TASK-M-020. Reúne lo que se midió en `vitae-mobile/` hasta la etapa 7 del plan (`../../PLAN.md`). Todo lo de aquí sale de ejecuciones reales; lo que **no** se pudo medir está dicho en cada documento y resumido abajo. Los datos de las pruebas son sintéticos.

| # | Documento | Tarea / requisito | Estado |
|---|---|---|---|
| 1 | [01-tls-pinning.md](01-tls-pinning.md) | RNF-003 | Probado en JVM. **Sin servidor real**: pines y tráfico sin verificar |
| 2 | [02-root-y-hardening.md](02-root-y-hardening.md) | TASK-M-007, M-014, PROV-SEC-011 | Probado en JVM y en teléfono. **Sin root real** |
| 3 | [03-almacenamiento-seguro.md](03-almacenamiento-seguro.md) | PROV-SEC-001/002 | Probado en teléfono. Inactividad real de 5 min sin verificar |
| 4 | [04-matriz-dispositivos.md](04-matriz-dispositivos.md) | TASK-M-016, RNF-006 | **Parcial**: 1 teléfono con API ≥ 29 y 1 emulador API 28 |
| 5 | [05-cobertura-contrato-y-solo-lectura.md](05-cobertura-contrato-y-solo-lectura.md) | TASK-M-015, M-018, M-019 | Cumplido en lo local. Colección compartida y repositorios remotos pendientes |
| 6 | [06-accesibilidad.md](06-accesibilidad.md) | TASK-M-017, PROV-UX-003 | Comprobaciones propias en 14 pantallas. **Sin TalkBack ni Accessibility Scanner** |
| 7 | [07-conexion-backend.md](07-conexion-backend.md) | RF-003, RNF-007, PROV-RN-008/009 | **Probado contra el backend local** (dev). Producción sin los endpoints |
| — | [resultados-pruebas.md](resultados-pruebas.md) | todas | 227 unitarias y 44 instrumentadas en verde |
| — | `cobertura.mjs`, `resumen-pruebas.mjs` | M-019 | Scripts para reproducir las cifras |

## Cómo reproducirlo

```bash
cd vitae-mobile
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport lintDebug assembleRelease assembleDebug assembleDebugAndroidTest
node evidence/AI-05-mobile/cobertura.mjs                      # sale con 1 si baja de 80 %
# con el teléfono desbloqueado y conectado:
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.example.vitae_mobile.test/androidx.test.runner.AndroidJUnitRunner > instrumentadas.txt
node evidence/AI-05-mobile/resumen-pruebas.mjs instrumentadas.txt > evidence/AI-05-mobile/resultados-pruebas.md
```

## Lo que falta para cerrar AI-05 (no se puede hacer sin otra persona o sin backend)

1. **Backend**: la app ya habla con los endpoints normales (PLAN.md, etapa 8), pero el servidor de producción no tiene los del Sprint 4, y siguen sin existir la colección de contrato (TASK-W-091) y las cuentas de paciente con OTP y PKCE. Faltan pinning contra el servidor real e inspección de proxy.
2. **Pines de certificado de producción** (`API_PINES` en `app/build.gradle.kts`): el release se niega a crear el cliente sin ellos, por diseño.
3. **Dispositivos**: dos tamaños de pantalla más y APIs 29 y 34 o superior. Hoy solo hay un Xiaomi M2007J20CG con Android 12 y una imagen de emulador de API 28.
4. **Root real**: un teléfono con Magisk para medir falsos negativos y positivos.
5. **Auditoría manual de accesibilidad** con TalkBack y Accessibility Scanner.
6. **Firma de la APK v2+, SBOM y publicación con MFA** (TASK-M-021, etapa de liberación).
