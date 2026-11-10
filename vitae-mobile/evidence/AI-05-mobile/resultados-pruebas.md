# Resultados de pruebas

Generado a partir de los XML de las ejecuciones reales (etapa 10, 10 de noviembre de 2026).

## Unitarias (JVM): `./gradlew testDebugUnitTest`

| Clase | Pruebas | Fallos |
|---|---|---|
| ContrasteTest | 3 | 0 |
| data.auth.InactivityMonitorTest | 4 | 0 |
| data.auth.MockAuthRepositoryTest | 10 | 0 |
| data.auth.PkceTest | 3 | 0 |
| data.auth.SessionManagerTest | 7 | 0 |
| data.clinico.MockClinicoTest | 6 | 0 |
| data.fhir.ContratoFhirTest | 5 | 0 |
| data.fhir.FhirParserTest | 9 | 0 |
| data.fhir.FhirRecetasTest | 12 | 0 |
| data.network.ApiClientTest | 9 | 0 |
| data.network.DispositivoInterceptorTest | 4 | 0 |
| data.network.PinningTest | 8 | 0 |
| data.network.SeguridadRedTest | 3 | 0 |
| data.perfil.MockPerfilRepositoryTest | 2 | 0 |
| data.perfil.PerfilTest | 3 | 0 |
| data.privacidad.MockPrivacidadYArcoTest | 8 | 0 |
| data.remoto.AuthRemotoTest | 8 | 0 |
| data.remoto.ClinicoRemotoTest | 11 | 0 |
| data.remoto.PrivacidadArcoRemotoTest | 15 | 0 |
| data.seguridad.DetectorRootDebugTest | 3 | 0 |
| data.seguridad.DetectorRootTest | 12 | 0 |
| data.seguridad.GuardiaDispositivoTest | 7 | 0 |
| data.SoloLecturaTest | 5 | 0 |
| ExampleUnitTest | 1 | 0 |
| ui.arco.ArcoViewModelTest | 9 | 0 |
| ui.components.CargaViewModelTest | 4 | 0 |
| ui.components.FormatoTest | 2 | 0 |
| ui.login.LoginViewModelTest | 8 | 0 |
| ui.otp.OtpViewModelTest | 5 | 0 |
| ui.perfil.PerfilViewModelTest | 5 | 0 |
| ui.privacidad.AvisoViewModelTest | 14 | 0 |
| ui.recetas.QrTest | 6 | 0 |
| ui.resultados.ResultadoVisorViewModelTest | 5 | 0 |
| ui.screens.ResumenViewModelTest | 9 | 0 |
| ui.seguridad.ContadorProteccionTest | 3 | 0 |
| **Total** | **228** | **0** |

## Instrumentadas (teléfono físico): `./gradlew connectedDebugAndroidTest -Pvitae.backend=simulado`

| Clase | Pruebas OK | Fallos |
|---|---|---|
| data.auth.KeystoreTokenStoreTest | 3 | 0 |
| ExampleInstrumentedTest | 1 | 0 |
| ui.AccesibilidadTest | 4 | 0 |
| ui.ArcoTest | 4 | 0 |
| ui.AvisoPrevioTest | 3 | 0 |
| ui.ClinicoTest | 9 | 0 |
| ui.HomeTest | 6 | 0 |
| ui.PrivacidadTest | 6 | 0 |
| ui.SeguridadTest | 7 | 0 |
| ui.SinEdicionTest | 1 | 0 |
| **Total** | **44** | **0** |
