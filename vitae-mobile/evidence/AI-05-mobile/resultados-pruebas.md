# Resultados de pruebas

Generado con `resumen-pruebas.mjs` a partir de las ejecuciones reales.

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
| data.remoto.AuthYPacienteRemotoTest | 8 | 0 |
| data.remoto.ClinicoRemotoTest | 10 | 0 |
| data.remoto.PrivacidadArcoRemotoTest | 12 | 0 |
| data.seguridad.DetectorRootDebugTest | 3 | 0 |
| data.seguridad.DetectorRootTest | 12 | 0 |
| data.seguridad.GuardiaDispositivoTest | 7 | 0 |
| data.SoloLecturaTest | 4 | 0 |
| ExampleUnitTest | 1 | 0 |
| ui.arco.ArcoViewModelTest | 9 | 0 |
| ui.components.CargaViewModelTest | 4 | 0 |
| ui.components.FormatoTest | 2 | 0 |
| ui.login.LoginViewModelTest | 8 | 0 |
| ui.otp.OtpViewModelTest | 5 | 0 |
| ui.paciente.PacienteViewModelTest | 4 | 0 |
| ui.perfil.PerfilViewModelTest | 5 | 0 |
| ui.privacidad.AvisoViewModelTest | 14 | 0 |
| ui.recetas.QrTest | 6 | 0 |
| ui.resultados.ResultadoVisorViewModelTest | 5 | 0 |
| ui.screens.ResumenViewModelTest | 9 | 0 |
| ui.seguridad.ContadorProteccionTest | 3 | 0 |
| **Total** | **227** | **0** |

## Instrumentadas (teléfono físico): `am instrument`

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
