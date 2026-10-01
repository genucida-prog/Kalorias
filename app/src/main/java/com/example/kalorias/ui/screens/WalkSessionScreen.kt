package com.example.kalorias.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.kalorias.data.ActivityType
import com.example.kalorias.service.WorkoutTrackingService
import com.example.kalorias.ui.KaloriasViewModel
import androidx.compose.runtime.collectAsState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WalkSessionScreen(
    viewModel: KaloriasViewModel,
    activityType: ActivityType = ActivityType.WALK,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val userName = viewModel.userProfile.name
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("es-ES")
                val startSpeech = when (activityType) {
                    ActivityType.RUN -> "¡Vamos $userName! Iniciando sesión de carrera. ¡A mantener un gran ritmo!"
                    ActivityType.WALK -> "¡Bienvenido $userName! Iniciando caminata en vivo. ¡A sumar pasos y salud!"
                    ActivityType.HIKE -> "¡A conquistar la ruta $userName! Sesión de senderismo iniciada."
                    ActivityType.CYCLE -> "¡A todo pedal $userName! Sesión de ciclismo activa."
                    ActivityType.TREADMILL -> "¡A darlo todo en la cinta $userName! Sesión iniciada."
                }
                tts?.speak(startSpeech, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val osmHotTileSource = remember {
        XYTileSource(
            "OSM_HOT",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.fr/hot/",
                "https://b.tile.openstreetmap.fr/hot/"
            )
        )
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Android 14; Mobile; KaloriasFitnessApp/1.0)"
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        onDispose { }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasActivityRecognitionPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasActivityRecognitionPermission = permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission || !hasActivityRecognitionPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            )
        }
    }

    LaunchedEffect(hasActivityRecognitionPermission, hasLocationPermission) {
        if (hasActivityRecognitionPermission || hasLocationPermission) {
            startForegroundService(
                context,
                Intent(context, WorkoutTrackingService::class.java).setAction(WorkoutTrackingService.ACTION_START)
            )
        }
    }

    val sessionState by WorkoutTrackingService.sessionState.collectAsState()
    val isRunning = sessionState.isRunning
    val secondsElapsed = sessionState.secondsElapsed
    val realSteps = sessionState.steps

    val userHeight = viewModel.userProfile.heightCm
    val stepLengthMeters = userHeight * 0.00415

    val gpsDistanceKm = remember(sessionState.routePoints) {
        sessionState.routePoints.zipWithNext().sumOf { (start, end) ->
            distanceBetweenKm(start.latitude, start.longitude, end.latitude, end.longitude)
        }
    }
    val distanceKm = if (gpsDistanceKm > 0.0) gpsDistanceKm else (realSteps * stepLengthMeters) / 1000.0

    val caloriesBurned = remember(distanceKm) {
        (distanceKm * activityType.calorieMultiplier).toInt()
    }

    val routeGeoPoints = remember(sessionState.routePoints) {
        sessionState.routePoints.map { GeoPoint(it.latitude, it.longitude) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Android 14; Mobile; KaloriasFitnessApp/1.0)"
                MapView(ctx).apply {
                    setTileSource(osmHotTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    controller.setCenter(routeGeoPoints.lastOrNull() ?: GeoPoint(40.4168, -3.7038))

                    val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                        enableMyLocation()
                        enableFollowLocation()
                        runOnFirstFix {
                            val myLoc = myLocation
                            if (myLoc != null) {
                                post {
                                    controller.animateTo(myLoc)
                                    controller.setZoom(18.0)
                                }
                            }
                        }
                    }
                    overlays.add(myLocationOverlay)
                    activeMapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { mapView ->
            activeMapView = mapView
            try {
                val points = routeGeoPoints
                if (points.isNotEmpty()) {
                    mapView.overlays.removeAll { it is Polyline || (it is Marker && it !is MyLocationNewOverlay) }
                    val polyline = Polyline().apply {
                        setPoints(points)
                        outlinePaint.color = Color.parseColor("#00E5FF")
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(polyline)

                    val lastPoint = points.last()
                    val marker = Marker(mapView).apply {
                        position = lastPoint
                        title = "Estás aquí (${activityType.label})"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                    mapView.invalidate()
                }
            } catch (_: Exception) {}
        }

        // Futuristic Workout HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activityType.icon} OPENSTREETMAP: ${activityType.label.uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                val currentVoiceMsg = "¡Sigue así $userName! Llevas $realSteps pasos en tu sesión de ${activityType.label}, acumulando $caloriesBurned kilocalorías quemadas."
                                tts?.speak(currentVoiceMsg, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ánimo por voz")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "TIEMPO", style = MaterialTheme.typography.labelSmall)
                            val mins = secondsElapsed / 60
                            val secs = secondsElapsed % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "DISTANCIA", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f km", distanceKm),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "PASOS REALES", style = MaterialTheme.typography.labelSmall)
                            Text(text = realSteps.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "CALORÍAS", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$caloriesBurned kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Bottom Controls Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            WorkoutTrackingService.command(
                                context,
                                if (isRunning) WorkoutTrackingService.ACTION_PAUSE else WorkoutTrackingService.ACTION_RESUME
                            )
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pausar" else "Reanudar"
                        )
                    }

                    Button(
                        onClick = {
                            val durationMins = (secondsElapsed / 60).coerceAtLeast(1)
                            val roundedDistanceKm = (distanceKm * 100.0).roundToInt() / 100.0

                            // Capture MapView Canvas Snapshot Bitmap
                            val snapshotPath = activeMapView?.let { mv ->
                                try {
                                    if (mv.width > 0 && mv.height > 0) {
                                        val bitmap = Bitmap.createBitmap(mv.width, mv.height, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bitmap)
                                        mv.draw(canvas)
                                        val file = File(context.cacheDir, "route_snapshot_${System.currentTimeMillis()}.png")
                                        FileOutputStream(file).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                        }
                                        file.absolutePath
                                    } else {
                                        null
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }

                            viewModel.addWalkRecord(
                                distanceKm = roundedDistanceKm,
                                inputSteps = realSteps,
                                durationMinutes = durationMins,
                                activityName = activityType.label,
                                mapSnapshotPath = snapshotPath,
                                routePoints = sessionState.routePoints
                            )
                            WorkoutTrackingService.command(context, WorkoutTrackingService.ACTION_STOP)
                            val finishSpeech = "¡Sesión finalizada $userName! Lograste $realSteps pasos y $caloriesBurned kilocalorías quemadas. Guardado en tu historial."
                            tts?.speak(finishSpeech, TextToSpeech.QUEUE_FLUSH, null, null)
                            onFinishSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(56.dp).weight(1f).padding(start = 16.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar y Guardar Sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun distanceBetweenKm(startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double): Double {
    val earthRadiusKm = 6371.0
    val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
    val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)
    val haversine = kotlin.math.sin(latitudeDelta / 2) * kotlin.math.sin(latitudeDelta / 2) +
            kotlin.math.cos(startLatitudeRadians) * kotlin.math.cos(endLatitudeRadians) *
            kotlin.math.sin(longitudeDelta / 2) * kotlin.math.sin(longitudeDelta / 2)
    return 2 * earthRadiusKm * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
}
