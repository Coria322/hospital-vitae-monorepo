package com.example.vitae_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vitae_mobile.ui.navigation.VitaeNavHost
import com.example.vitae_mobile.ui.theme.VitaemobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitaemobileTheme {
                VitaeNavHost()
            }
        }
    }
}
