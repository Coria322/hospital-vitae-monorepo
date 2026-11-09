package com.example.vitae_mobile

import android.app.Application

class VitaeApplication : Application() {
    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}
