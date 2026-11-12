package com.example.vitae_mobile.ui.seguridad

import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.vitae_mobile.BuildConfig
import java.util.WeakHashMap

/**
 * Cuenta las pantallas protegidas abiertas a la vez: al navegar de una a otra la nueva se compone antes de que la
 * anterior se descarte, y sin contar el flag se quitaría con la pantalla nueva ya visible.
 */
class ContadorProteccion(private val poner: () -> Unit, private val quitar: () -> Unit) {
    private var usos = 0

    fun entrar() {
        if (usos++ == 0) poner()
    }

    fun salir() {
        if (usos > 0 && --usos == 0) quitar()
    }
}

private val contadores = WeakHashMap<Window, ContadorProteccion>()

private fun contadorDe(ventana: Window) = contadores.getOrPut(ventana) {
    ContadorProteccion(
        poner = { ventana.addFlags(WindowManager.LayoutParams.FLAG_SECURE) },
        quitar = { ventana.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) },
    )
}

/**
 * PROV-SEC-011: bloquea capturas y grabación de pantalla mientras se muestra [contenido] (FLAG_SECURE).
 * Solo un build de debug con `-Pvitae.permitirCapturas=true` puede desactivarlo.
 */
@Composable
fun PantallaSegura(contenido: @Composable () -> Unit) {
    val ventana = LocalContext.current.actividad()?.window
    if (ventana != null) {
        if (BuildConfig.PERMITIR_CAPTURAS) {
            ventana.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            DisposableEffect(ventana) {
                val contador = contadorDe(ventana)
                contador.entrar()
                onDispose { contador.salir() }
            }
        }
    }
    contenido()
}

private tailrec fun Context.actividad(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
