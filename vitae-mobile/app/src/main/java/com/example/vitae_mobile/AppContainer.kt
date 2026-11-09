package com.example.vitae_mobile

import android.content.Context
import android.os.SystemClock
import com.example.vitae_mobile.data.arco.ArcoRepository
import com.example.vitae_mobile.data.auth.AuthRepository
import com.example.vitae_mobile.data.citas.CitasRepository
import com.example.vitae_mobile.data.network.ApiConfig
import com.example.vitae_mobile.data.perfil.PerfilRepository
import com.example.vitae_mobile.data.privacidad.PrivacidadRepository
import com.example.vitae_mobile.data.recetas.RecetasRepository
import com.example.vitae_mobile.data.remoto.ApiBackend
import com.example.vitae_mobile.data.remoto.ArcoRemoto
import com.example.vitae_mobile.data.remoto.AuthRemoto
import com.example.vitae_mobile.data.remoto.CitasRemoto
import com.example.vitae_mobile.data.remoto.PerfilRemoto
import com.example.vitae_mobile.data.remoto.PrivacidadRemoto
import com.example.vitae_mobile.data.remoto.RecetasRemoto
import com.example.vitae_mobile.data.remoto.ResultadosRemoto
import com.example.vitae_mobile.data.remoto.SesionBackend
import com.example.vitae_mobile.data.resultados.ResultadosRepository
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.InactivityMonitor
import com.example.vitae_mobile.data.auth.KeystoreTokenStore
import com.example.vitae_mobile.data.auth.MotivoCierre
import com.example.vitae_mobile.data.auth.SessionManager
import com.example.vitae_mobile.data.arco.crearArcoRepository
import com.example.vitae_mobile.data.auth.crearAuthRepository
import com.example.vitae_mobile.data.citas.crearCitasRepository
import com.example.vitae_mobile.data.perfil.crearPerfilRepository
import com.example.vitae_mobile.data.privacidad.AvisoPrevioLocal
import com.example.vitae_mobile.data.privacidad.crearPrivacidadRepository
import com.example.vitae_mobile.data.recetas.crearRecetasRepository
import com.example.vitae_mobile.data.resultados.crearResultadosRepository
import com.example.vitae_mobile.data.seguridad.EntornoAndroid
import com.example.vitae_mobile.data.seguridad.GuardiaDispositivo
import com.example.vitae_mobile.data.seguridad.crearDetectorRoot
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Contenedor de dependencias manual (supuesto S2 del plan: sin Hilt). */
class AppContainer(contexto: Context) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val almacen = KeystoreTokenStore(contexto)

    // Con BACKEND_REAL (por omisión; -Pvitae.backend=simulado lo apaga en debug) la app habla con los endpoints normales de
    // vitae-backend. Los lambdas leen `sesion` y `dispositivo` al llamar, no al crear el cliente: se declaran más abajo.
    private val api: ApiBackend by lazy {
        ApiConfig.crearCliente(
            token = { sesion.token() },
            alSesionInvalida = { sesion.expirar() },
            bloqueado = { dispositivo.bloqueado() },
            alIncompatible = { dispositivo.marcarIncompatible() },
        ).create(ApiBackend::class.java)
    }

    /** Quién inició sesión y qué paciente se consulta; null con datos simulados. */
    val backend: SesionBackend? = if (BuildConfig.BACKEND_REAL) SesionBackend(api) else null

    val authRepository: AuthRepository = backend?.let { AuthRemoto(api, it) } ?: crearAuthRepository(almacen::idInstalacion)
    val sesion = SessionManager(almacen, authRepository, alcance)

    // PROV-SEC-011: en un dispositivo bloqueado (root, 426, Android no soportado) la sesión se borra solo en local; revocarla
    // en el servidor sería una petición, y de un dispositivo bloqueado no sale ninguna.
    private val entorno = EntornoAndroid(contexto)
    val dispositivo = GuardiaDispositivo(entorno, crearDetectorRoot(entorno)) {
        if (sesion.estado.value is EstadoSesion.Autenticada) sesion.expirar()
    }
    val avisoPrevio = AvisoPrevioLocal(contexto)
    val perfilRepository: PerfilRepository = backend?.let { PerfilRemoto(api, it) } ?: crearPerfilRepository()
    val citasRepository: CitasRepository = backend?.let { CitasRemoto() } ?: crearCitasRepository()
    val recetasRepository: RecetasRepository = backend?.let { RecetasRemoto(api, it) } ?: crearRecetasRepository()
    val privacidadRepository: PrivacidadRepository =
        backend?.let { PrivacidadRemoto(api, it, contexto.getString(R.string.aviso_previo_texto)) } ?: crearPrivacidadRepository()
    val arcoRepository: ArcoRepository = backend?.let { ArcoRemoto(api, it) } ?: crearArcoRepository()

    // Los resultados se descargan aquí, en la caché interna; nunca en almacenamiento compartido (RF-010).
    private val directorioResultados = File(contexto.cacheDir, "resultados")
    val resultadosRepository: ResultadosRepository =
        backend?.let { ResultadosRemoto(api, it, directorioResultados) } ?: crearResultadosRepository(directorioResultados)

    val inactividad = InactivityMonitor(alcance, SystemClock::elapsedRealtime) {
        alcance.launch { sesion.cerrar(MotivoCierre.Inactividad) }
    }

    init {
        alcance.launch { sesion.descartarSesionPrevia() }
        // El temporizador de inactividad corre solo mientras hay sesión.
        alcance.launch {
            sesion.estado.collect {
                if (it is EstadoSesion.Autenticada) {
                    inactividad.iniciar()
                } else {
                    inactividad.detener()
                    backend?.olvidar() // sin sesión no se recuerda ni al usuario ni al paciente
                    // Sin sesión no debe quedar ningún resultado clínico en disco (también al arrancar: restos de un proceso muerto).
                    withContext(Dispatchers.IO) { directorioResultados.deleteRecursively() }
                }
            }
        }
    }
}
