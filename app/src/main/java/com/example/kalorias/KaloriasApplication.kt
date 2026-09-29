package com.example.kalorias

import android.app.Application
import org.osmdroid.config.Configuration

class KaloriasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Android 14; Mobile; KaloriasFitnessApp/1.0)"
    }
}
