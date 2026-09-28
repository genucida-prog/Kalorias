package com.example.kalorias.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.example.kalorias.data.ActivityType
import com.example.kalorias.ui.KaloriasViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun WalkSessionScreen(
    viewModel: KaloriasViewModel,
    activityType: ActivityType = ActivityType.WALK,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            )
        }
    }

    var isRunning by remember { mutableStateOf(true) }
    var secondsElapsed by remember { mutableIntStateOf(0) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }

    val userHeight = viewModel.userProfile.heightCm
    val stepLengthMeters = userHeight * 0.00415

    val currentSteps = remember(distanceKm, userHeight) {
        if (distanceKm > 0) ((distanceKm * 1000) / stepLengthMeters).toInt() else 0
    }

    val caloriesBurned = remember(distanceKm) {
        (distanceKm * activityType.calorieMultiplier).toInt()
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
    }

    // Route points for Google Maps Polyline
    val routePoints = remember { mutableStateOf(mutableListOf(LatLng(40.4168, -3.7038), LatLng(40.4178, -3.7048))) }

    val darkMapStyle = """
        [
          {"elementType":"geometry","stylers":[{"color":"#121826"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
          {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1e293b"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b0f19"}]}
        ]
    """.trimIndent()

    // Timer and live movement simulation
    LaunchedEffect(isRunning) {
        var currentLat = 40.4168
        var currentLng = -3.7038
        while (isRunning) {
            delay(1000L)
            secondsElapsed++
            distanceKm += 0.0012

            currentLat += 0.0001
            currentLng += 0.0001
            routePoints.value.add(LatLng(currentLat, currentLng))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Google Maps MapView View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.getMapAsync { googleMap ->
                try {
                    googleMap.setMapStyle(MapStyleOptions(darkMapStyle))
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    if (hasLocationPermission) {
                        try {
                            googleMap.isMyLocationEnabled = true
                        } catch (_: Exception) {}
                    }
                    val points = routePoints.value
                    if (points.isNotEmpty()) {
                        googleMap.clear()
                        val polylineOptions = PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor("#00E5FF")) // Neon Cyan
                            .width(10f)
                        googleMap.addPolyline(polylineOptions)

                        val lastPoint = points.last()
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(lastPoint)
                                .title("🚶‍♂️ Estás aquí (${activityType.label})")
                        )
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 16f))
                    }
                } catch (_: Exception) {}
            }
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
                    Text(
                        text = "${activityType.icon} GOOGLE MAPS: ${activityType.label.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

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
                            Text(text = "PASOS", style = MaterialTheme.typography.labelSmall)
                            Text(text = currentSteps.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pausar" else "Reanudar"
                        )
                    }

                    Button(
                        onClick = {
                            isRunning = false
                            val durationMins = (secondsElapsed / 60).coerceAtLeast(1)
                            viewModel.addWalkRecord(
                                distanceKm = String.format(Locale.getDefault(), "%.2f", distanceKm).toDouble(),
                                inputSteps = currentSteps,
                                durationMinutes = durationMins,
                                activityName = activityType.label
                            )
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
