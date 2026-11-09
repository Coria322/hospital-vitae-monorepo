# 1. TLS y certificate pinning (RNF-003)

## Qué se comprobó

| Control | Cómo | Resultado |
|---|---|---|
| Sin tráfico en claro en release | `SeguridadRedTest.releaseNoPermiteTraficoEnClaro` lee `res/xml/network_security_config.xml` | `cleartextTrafficPermitted="false"` |
| Debug solo habla en claro con el backend local | `SeguridadRedTest.debugSoloPermiteElBackendLocal` | únicos dominios: `10.0.2.2` y `localhost` |
| El manifiesto usa esa configuración y no tiene `usesCleartextTraffic` | `SeguridadRedTest.elManifiestoUsaLaConfiguracionDeRed` | sí |
| El pin del servidor deja pasar la conexión | `PinningTest.conElPinDelServidorLaConexionSePermite` | `CertificatePinner.check` sin excepción |
| Otro certificado se rechaza | `PinningTest.conOtroCertificadoLaConexionSeRechaza` | `SSLPeerUnverifiedException` "Certificate pinning failure" |
| Rotación: basta un pin entre varios | `PinningTest.conVariosPinesBastaUnoQueCoincida` | sí |
| Los pines valen solo para el host de la API | `PinningTest.losPinesSoloAplicanAlHostDeLaApi` | otro host no queda atado |
| El cliente real lleva el pinner solo si hay pines | `PinningTest.elClienteLlevaElPinnerSoloSiHayPines` | 0 pines en debug, 1 con pin |
| TLS 1.2 o superior | `PinningTest.elClienteSoloUsaTls12OSuperior` | versiones permitidas: solo TLS 1.2 y 1.3; no `COMPATIBLE_TLS` |
| El release no arranca sin pines | `ApiConfig.crearCliente`: `check(BuildConfig.DEBUG \|\| pines.isNotEmpty())` | falla con "Release sin certificate pinning (RNF-003)" |

Los dos certificados de `app/src/test/resources/seguridad/` son autofirmados, **solo públicos** (no hay clave privada en el repositorio) y sirven de "servidor" y "atacante". Su CN termina en `.invalid`.

## Qué NO se verificó

- **Pines del servidor real**: `API_PINES` está vacío en release a propósito hasta que Seguridad entregue el certificado definitivo. Con el valor vacío el release no crea el cliente.
- **Conexión TLS real** contra el servidor ni versión negociada en el cable.
- **Interceptación con proxy** (mitmproxy o Burp): no hay endpoints móviles en el backend con los que ejercitarlo.
- El `CertificatePinner` se prueba llamando a `check`, no con un handshake completo; la librería para levantar un servidor TLS de prueba (`okhttp-tls`) no está en el proyecto y no se añadió.
