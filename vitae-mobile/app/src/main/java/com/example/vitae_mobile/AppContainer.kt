package com.example.vitae_mobile

import android.content.Context
import android.os.SystemClock
import com.example.vitae_mobile.data.auth.EstadoSesion
import com.example.vitae_mobile.data.auth.InactivityMonitor
import com.example.vitae_mobile.data.auth.KeystoreTokenStore
import com.example.vitae_mobile.data.auth.MotivoCierre
import com.example.vitae_mobile.data.auth.SessionManager
import com.example.vitae_mobile.data.arco.crearArcoRepository
import com.example.vitae_mobile.data.auth.crearAuthRepository
import com.example.vitae_mobile.data.citas.crearCitasRepository
import com.example.vitae_mobile.data.perfil.crearPerfilRepository
import com.example.vitae_mobile.data.privacidad.crearPrivacidadRepository
import com.example.vitae_mobile.data.recetas.crearRecetasRepository
import com.example.vitae_mobile.data.resultados.crearResultadosRepository
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
    val authRepository = crearAuthRepository(almacen::idInstalacion)
    val sesion = SessionManager(almacen, authRepository, alcance)
    val perfilRepository = crearPerfilRepository()
    val citasRepository = crearCitasRepository()
    val recetasRepository = crearRecetasRepository()
    val privacidadRepository = crearPrivacidadRepository()
    val arcoRepository = crearArcoRepository()

    // Los resultados se descargan aquí, en la caché interna; nunca en almacenamiento compartido (RF-010).
    private val directorioResultados = File(contexto.cacheDir, "resultados")
    val resultadosRepository = crearResultadosRepository(directorioResultados)

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
                    // Sin sesión no debe quedar ningún resultado clínico en disco (también al arrancar: restos de un proceso muerto).
                    withContext(Dispatchers.IO) { directorioResultados.deleteRecursively() }
                }
            }
        }
    }
}
