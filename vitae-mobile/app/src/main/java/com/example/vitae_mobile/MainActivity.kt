package com.example.vitae_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vitae_mobile.ui.navigation.VitaeNavHost
import com.example.vitae_mobile.ui.theme.VitaemobileTheme

class MainActivity : ComponentActivity() {
    private val contenedor get() = (application as VitaeApplication).contenedor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitaemobileTheme {
                VitaeNavHost(contenedor, alSalir = ::finishAffinity)
            }
        }
    }

    // PROV-SEC-011: se vuelve a comprobar cada vez que la app pasa a primer plano (Magisk pudo activarse mientras tanto).
    override fun onStart() {
        super.onStart()
        contenedor.dispositivo.verificar()
    }

    // PROV-SEC-001: cada toque o tecla reinicia los 5 minutos de inactividad.
    override fun onUserInteraction() {
        super.onUserInteraction()
        contenedor.inactividad.registrarActividad()
    }

    // Si la app estuvo en segundo plano, el temporizador pudo no correr: se compara con el reloj.
    override fun onResume() {
        super.onResume()
        contenedor.inactividad.comprobar()
    }
}
