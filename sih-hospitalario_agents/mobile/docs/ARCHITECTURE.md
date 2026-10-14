# ARCHITECTURE.md — SIH · mobile/

> Fuente: Manual de Calidad de Arquitectura - Sistema de Información Hospitalaria (DOC-ARQ-SIH-001, rev 2.0)
> Patrón: MVVM (Jetpack ViewModel) + Repository sobre la API de `web/`
> Fecha: 2026-10-09

---

## VISIÓN GENERAL
App Android nativa (Kotlin, minSdk 29) para pacientes: solo lectura de citas, recetas y resultados. Consume la MISMA API que la SPA Vue, pero únicamente el namespace `/api/v1/mobile/*` y los endpoints de autenticación móviles. No usa SDK de Supabase ni accede a la BD. Fuente de verdad del contrato: `web/docs/ARCHITECTURE.md` (sección Superficie de API).

## CAPAS
```
View (Compose) → ViewModel (estado, reglas de UI) → Repository (Retrofit/OkHttp, caché cifrada) → API /api/v1/mobile/*
```

## MÓDULOS
| Módulo | Responsabilidad | Requerimientos |
|---|---|---|
| auth-ui / auth-client / session | Login, OTP, PKCE, tokens en Keystore, inactividad 5 min | PROV-SEC-001/002, RF-002 |
| api-client / network | Retrofit, pinning, interceptor 401/403 | RF-003, RNF-003 |
| app-security | Root/Magisk, FLAG_SECURE, R8 | PROV-SEC-011 |
| recetas-ui / resultados-ui / citas-ui | Lectura | RF-009, RF-010, PROV-RF-002 |
| privacy-ui / arco-ui | Aviso, consentimientos, ARCO | PROV-RN-008/009 |
| fhir-client | Modelos Patient/MedicationRequest | RNF-007 |

## ENDPOINTS CONSUMIDOS

| Método | Ruta | Módulo | Requerimiento | Descripción | Web | Móvil |
|---|---|---|---|---|---|---|
| POST | `/api/v1/mobile/auth/token` | `auth` | `PROV-SEC-002` | Código OAuth2 + PKCE S256 | — | ✔ |
| POST | `/api/v1/mobile/auth/otp-verify` | `auth` | `PROV-SEC-001` | OTP de dispositivo nuevo | — | ✔ |
| POST | `/api/v1/mobile/auth/refresh` | `auth` | `PROV-SEC-002` | Refresh rotativo | — | ✔ |
| POST | `/api/v1/mobile/auth/logout` | `auth` | `PROV-SEC-002` | Revocación inmediata | — | ✔ |
| GET | `/api/v1/fhir/{Patient|Encounter|Observation|MedicationRequest}/{id}` | `fhir` | `RNF-007` | FHIR R4 (PascalCase, SUP-010) | ✔ | ✔ |
| GET | `/api/v1/mobile/perfil` | `api-mobile` | `RF-011` | Patient FHIR del paciente | — | ✔ |
| GET | `/api/v1/mobile/citas` | `api-mobile` | `PROV-RF-002` | Citas propias | — | ✔ |
| GET | `/api/v1/mobile/recetas | /{id}` | `api-mobile` | `RF-009` | MedicationRequest propios | — | ✔ |
| GET | `/api/v1/mobile/resultados | /{id}/archivo` | `api-mobile` | `RF-010` | Resultados propios (stream) | — | ✔ |
| GET | `/api/v1/mobile/aviso-privacidad` | `api-mobile` | `PROV-RN-008` | Aviso vigente | — | ✔ |
| POST | `/api/v1/mobile/consentimientos` | `api-mobile` | `PROV-RN-008` | Escritura lista blanca (SUP-002) | — | ✔ |
| GET/POST | `/api/v1/mobile/solicitudes-arco` | `api-mobile` | `PROV-RN-009` | Escritura lista blanca (SUP-002) | — | ✔ |

## SEGURIDAD EN CLIENTE
| Área | Mecanismo | Fuente |
|---|---|---|
| Tokens | EncryptedSharedPreferences + Keystore; borrado al logout + revocación en servidor | PROV-SEC-002 |
| Transporte | TLS >= 1.2, pinning, cleartext deshabilitado | RNF-003 |
| Dispositivo | Root/Magisk bloquea la app | PROV-SEC-011 |
| Sesión | 5 min de inactividad | PROV-SEC-001 |
| Datos | Sin PII en claro; sin id_paciente en peticiones | RF-002.1 |

## MANEJO DE ERRORES
| Código | Comportamiento en la app |
|---|---|
| 401 | Limpiar estado e ir a login |
| 403 | Mensaje de permisos; sin cerrar sesión |
| 423 | Alerta de contactar a administración |
| 426 | Pantalla de incompatibilidad |
| red/5xx | Estado de error con reintento |

## SUPUESTOS ARQUITECTÓNICOS
| ID | Supuesto | Alternativa |
|---|---|---|
| SUP-ARQ-M-01 | Jetpack Compose + Hilt | Vistas XML |
| SUP-ARQ-M-02 | Lista blanca de escrituras (SUP-002) | Hacer consentimiento/ARCO solo en admisión |
