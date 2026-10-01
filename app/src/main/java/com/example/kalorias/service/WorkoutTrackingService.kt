package com.example.kalorias.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class WorkoutSessionState(
    val secondsElapsed: Int = 0,
    val steps: Int = 0,
    val isRunning: Boolean = false
)

class WorkoutTrackingService : Service(), SensorEventListener {
    companion object {
        const val ACTION_START = "com.example.kalorias.action.START_WORKOUT"
        const val ACTION_PAUSE = "com.example.kalorias.action.PAUSE_WORKOUT"
        const val ACTION_RESUME = "com.example.kalorias.action.RESUME_WORKOUT"
        const val ACTION_STOP = "com.example.kalorias.action.STOP_WORKOUT"
        private const val CHANNEL_ID = "workout_tracking"
        private const val NOTIFICATION_ID = 701

        val sessionState = MutableStateFlow(WorkoutSessionState())

        fun command(context: Context, action: String) {
            context.startService(Intent(context, WorkoutTrackingService::class.java).setAction(action))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var timerJob: Job? = null
    private var elapsedBeforeRun = 0
    private var runStartedAt = 0L
    @Volatile
    private var isTracking = false
    private var lastCounterReading: Float? = null

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences("active_workout", Context.MODE_PRIVATE)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        elapsedBeforeRun = preferences.getInt("seconds", 0)
        publishState(preferences.getInt("steps", 0), false)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseTracking()
            ACTION_STOP -> stopTracking()
            ACTION_RESUME -> startTracking()
            ACTION_START, null -> {
                if (!preferences.getBoolean("active", false)) {
                    elapsedBeforeRun = 0
                    preferences.edit().putInt("seconds", 0).putInt("steps", 0).putBoolean("active", true).apply()
                    publishState(0, false)
                }
                startTracking()
            }
        }
        if (intent?.action != ACTION_STOP) {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        runStartedAt = SystemClock.elapsedRealtime()
        preferences.edit().putBoolean("active", true).apply()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isTracking) {
                delay(1000L)
                val elapsed = elapsedBeforeRun + ((SystemClock.elapsedRealtime() - runStartedAt) / 1000L).toInt()
                preferences.edit().putInt("seconds", elapsed).apply()
                sessionState.value = sessionState.value.copy(secondsElapsed = elapsed, isRunning = true)
            }
        }
        sessionState.value = sessionState.value.copy(isRunning = true)
    }

    private fun pauseTracking() {
        if (!isTracking) return
        elapsedBeforeRun += ((SystemClock.elapsedRealtime() - runStartedAt) / 1000L).toInt()
        isTracking = false
        timerJob?.cancel()
        sensorManager.unregisterListener(this)
        preferences.edit().putInt("seconds", elapsedBeforeRun).apply()
        sessionState.value = sessionState.value.copy(secondsElapsed = elapsedBeforeRun, isRunning = false)
    }

    private fun stopTracking() {
        pauseTracking()
        preferences.edit().putBoolean("active", false).putInt("seconds", 0).putInt("steps", 0).apply()
        elapsedBeforeRun = 0
        sessionState.value = WorkoutSessionState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event == null) return
        val currentSteps = sessionState.value.steps
        val increment = when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> 1
            Sensor.TYPE_STEP_COUNTER -> {
                val previousReading = lastCounterReading
                lastCounterReading = event.values[0]
                if (previousReading == null) 0 else (event.values[0] - previousReading).toInt().coerceAtLeast(0)
            }
            else -> 0
        }
        if (increment > 0) {
            val updatedSteps = currentSteps + increment
            preferences.edit().putInt("steps", updatedSteps).apply()
            sessionState.value = sessionState.value.copy(steps = updatedSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun publishState(steps: Int, running: Boolean) {
        sessionState.value = WorkoutSessionState(elapsedBeforeRun, steps, running)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Seguimiento de actividad", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentTitle("Kalorias está siguiendo tu actividad")
        .setContentText("El tiempo y los pasos seguirán actualizándose con la pantalla bloqueada")
        .setOngoing(true)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

}