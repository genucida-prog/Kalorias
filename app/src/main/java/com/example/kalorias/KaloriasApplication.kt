package com.example.kalorias

import android.app.Application
import org.osmdroid.config.Configuration

class KaloriasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = "KaloriasFitnessApp/1.0 (Android)"
    }
}
