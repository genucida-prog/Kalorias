package com.example.kalorias.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.kalorias.MainActivity
import com.example.kalorias.data.RoutePoint
import org.json.JSONArray
import org.json.JSONObject
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
    val isRunning: Boolean = false,
    val routePoints: List<RoutePoint> = emptyList()
)

class WorkoutTrackingService : Service(), SensorEventListener {
    companion object {
        const val ACTION_START = "com.example.kalorias.action.START_WORKOUT"
        const val ACTION_PAUSE = "com.example.kalorias.action.PAUSE_WORKOUT"
        const val ACTION_RESUME = "com.example.kalorias.action.RESUME_WORKOUT"
        const val ACTION_STOP = "com.example.kalorias.action.STOP_WORKOUT"
        private const val CHANNEL_ID = "workout_tracking"
        private const val NOTIFICATION_ID = 701
        private const val LOCATION_INTERVAL_MS = 3000L
        private const val LOCATION_MIN_DISTANCE_METERS = 5f

        val sessionState = MutableStateFlow(WorkoutSessionState())

        fun command(context: Context, action: String) {
            context.startService(Intent(context, WorkoutTrackingService::class.java).setAction(action))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var stepSensor: Sensor? = null
    private var locationListener: LocationListener? = null
    private var timerJob: Job? = null
    private var elapsedBeforeRun = 0
    private var runStartedAt = 0L
    @Volatile
    private var isTracking = false
    private var lastCounterReading: Float? = null
    private var routePoints = emptyList<RoutePoint>()

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences("active_workout", Context.MODE_PRIVATE)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        elapsedBeforeRun = preferences.getInt("seconds", 0)
        routePoints = loadRoutePoints()
        publishState(preferences.getInt("steps", 0), preferences.getBoolean("running", false))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START || intent?.action == null) {
            if (!preferences.getBoolean("active", false)) {
                resetSession()
            } else if (intent?.action == null && !preferences.getBoolean("running", false)) {
                isTracking = false
            }
        }

        if (intent?.action != ACTION_STOP && !startForegroundWithNotification()) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_PAUSE -> pauseTracking()
            ACTION_STOP -> stopTracking()
            ACTION_RESUME -> startTracking()
            ACTION_START -> startTracking()
            null -> if (preferences.getBoolean("running", false)) startTracking()
        }
        if (intent?.action != ACTION_STOP) updateNotification()
        return START_STICKY
    }

    private fun resetSession() {
        elapsedBeforeRun = 0
        routePoints = emptyList()
        lastCounterReading = null
        preferences.edit().putInt("seconds", 0).putInt("steps", 0).putString("route", "[]")
            .putBoolean("active", true).putBoolean("running", false).apply()
        publishState(0, false)
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        runStartedAt = SystemClock.elapsedRealtime()
        preferences.edit().putBoolean("active", true).putBoolean("running", true).apply()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
        startLocationUpdates()
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
        lastCounterReading = null
        stopLocationUpdates()
        preferences.edit().putInt("seconds", elapsedBeforeRun).putBoolean("running", false).apply()
        sessionState.value = sessionState.value.copy(secondsElapsed = elapsedBeforeRun, isRunning = false)
    }

    private fun stopTracking() {
        pauseTracking()
        preferences.edit().putBoolean("active", false).putBoolean("running", false)
            .putInt("seconds", 0).putInt("steps", 0).putString("route", "[]").apply()
        elapsedBeforeRun = 0
        routePoints = emptyList()
        lastCounterReading = null
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

    private fun startLocationUpdates() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val previousPoint = routePoints.lastOrNull()
                if (previousPoint != null) {
                    val previousLocation = Location("route").apply {
                        latitude = previousPoint.latitude
                        longitude = previousPoint.longitude
                    }
                    if (previousLocation.distanceTo(location) < LOCATION_MIN_DISTANCE_METERS) return
                }

                routePoints = routePoints + RoutePoint(location.latitude, location.longitude)
                saveRoutePoints()
                sessionState.value = sessionState.value.copy(routePoints = routePoints)
            }

            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        locationListener = listener
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL_MS,
                LOCATION_MIN_DISTANCE_METERS,
                listener,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            locationListener = null
        } catch (_: IllegalArgumentException) {
            locationListener = null
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let(locationManager::removeUpdates)
        locationListener = null
    }

    private fun loadRoutePoints(): List<RoutePoint> = try {
        val pointsJson = JSONArray(preferences.getString("route", "[]"))
        List(pointsJson.length()) { index ->
            pointsJson.getJSONObject(index).let { point ->
                RoutePoint(point.getDouble("latitude"), point.getDouble("longitude"))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveRoutePoints() {
        val pointsJson = JSONArray()
        routePoints.forEach { point ->
            pointsJson.put(JSONObject().put("latitude", point.latitude).put("longitude", point.longitude))
        }
        preferences.edit().putString("route", pointsJson.toString()).apply()
    }

    private fun publishState(steps: Int, running: Boolean) {
        sessionState.value = WorkoutSessionState(elapsedBeforeRun, steps, running, routePoints)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Seguimiento de actividad", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(): Boolean {
        val notification = buildNotification()
        val hasActivityPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        val hasLocationPermission = hasLocationPermission()
        if (!hasActivityPermission && !hasLocationPermission) {
            stopSelf()
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var foregroundTypes = 0
            if (hasActivityPermission) foregroundTypes = foregroundTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            if (hasLocationPermission) foregroundTypes = foregroundTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            if (foregroundTypes == 0) {
                stopSelf()
                return false
            }
            startForeground(NOTIFICATION_ID, notification, foregroundTypes)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundTypes = if (hasLocationPermission) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            startForeground(NOTIFICATION_ID, notification, foregroundTypes)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return true
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = if (isTracking) ACTION_PAUSE else ACTION_RESUME
        val toggleTitle = if (isTracking) "Pausar" else "Reanudar"
        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WorkoutTrackingService::class.java).setAction(toggleAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Kalorias: sesión ${if (isTracking) "en curso" else "pausada"}")
            .setContentText("${sessionState.value.steps} pasos · ${sessionState.value.secondsElapsed / 60} min")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(
                if (isTracking) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                toggleTitle,
                toggleIntent
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        if (::locationManager.isInitialized) stopLocationUpdates()
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

}