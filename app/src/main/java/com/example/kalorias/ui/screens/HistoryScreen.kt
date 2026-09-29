package com.example.kalorias.ui.screens

import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kalorias.data.ActivityType
import com.example.kalorias.ui.KaloriasViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: KaloriasViewModel) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("es-ES")
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val walkRecords = viewModel.walkRecords
    val userName = viewModel.userProfile.name
    val totalDistance = viewModel.totalDistanceKm
    val totalBurned = viewModel.totalCaloriesBurned

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("⚠️ ¿Borrar Todo el Historial?") },
            text = { Text("Se eliminarán permanentemente todas tus sesiones guardadas, kilómetros acumulados y calorías quemadas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearWalkRecords()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar Historial")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📜 Historial de Rutas & Resultados",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Todas tus sesiones guardadas de caminatas, carreras y ejercicios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val speechText = "Hola $userName. En tu historial tienes un total de ${walkRecords.size} actividades guardadas, acumulando ${String.format(Locale.getDefault(), "%.1f", totalDistance)} kilómetros y ${totalBurned} kilocalorías quemadas. ¡Excelente trabajo!"
                            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar Historial con Voz")
                    }

                    if (walkRecords.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Borrar Todo el Historial", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Total Performance Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rendimiento Total Acumulado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Actividades", style = MaterialTheme.typography.bodySmall)
                            Text(text = "${walkRecords.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Distancia Total", style = MaterialTheme.typography.bodySmall)
                            Text(text = String.format(Locale.getDefault(), "%.1f km", totalDistance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Calorías Quemadas", style = MaterialTheme.typography.bodySmall)
                            Text(text = "$totalBurned kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // List Header & Clear Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sesiones Registradas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (walkRecords.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmDialog = true }) {
                        Text("Borrar Historial", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (walkRecords.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🏃‍♂️", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "No hay sesiones guardadas en el historial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Comienza una sesión de Caminata, Correr, Ciclismo o Senderismo desde la pestaña Actividad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(walkRecords) { walk ->
            val activityObj = ActivityType.entries.find { it.label == walk.activityName } ?: ActivityType.WALK

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = activityObj.icon, style = MaterialTheme.typography.headlineSmall)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${walk.activityName} • ${walk.distanceKm} km",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${walk.steps} Pasos • ${walk.durationMinutes} min • Fecha: ${walk.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "🔥 ${walk.caloriesBurned} kcal quemadas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val speechText = "Sesión de ${walk.activityName} completada por $userName. Recorriste ${walk.distanceKm} kilómetros en ${walk.durationMinutes} minutos, dando ${walk.steps} pasos y quemando ${walk.caloriesBurned} kilocalorías. ¡Excelente logro!"
                                    tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar Resumen de Actividad")
                            }

                            IconButton(
                                onClick = { viewModel.deleteWalkRecord(walk) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar Registro", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Map Route Snapshot Thumbnail Image
                    if (!walk.mapSnapshotPath.isNullOrEmpty()) {
                        val imgFile = File(walk.mapSnapshotPath)
                        if (imgFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                            if (bitmap != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Mapa de la Ruta",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
