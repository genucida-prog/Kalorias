package com.example.kalorias.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kalorias.data.ActivityType
import com.example.kalorias.ui.KaloriasViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: KaloriasViewModel) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

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
                Column {
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

                IconButton(
                    onClick = {
                        val speechText = "Hola $userName. En tu historial tienes un total de ${walkRecords.size} actividades guardadas, acumulando ${String.format(Locale.getDefault(), "%.1f", totalDistance)} kilómetros y ${totalBurned} kilocalorías quemadas. ¡Excelente trabajo!"
                        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar Historial con Voz")
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

        // List of Saved Sessions
        item {
            Text(
                text = "Sesiones Registradas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(walkRecords) { walk ->
            val activityObj = ActivityType.entries.find { it.label == walk.activityName } ?: ActivityType.WALK

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
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

                    IconButton(
                        onClick = {
                            val speechText = "Sesión de ${walk.activityName} completada por $userName. Recorriste ${walk.distanceKm} kilómetros en ${walk.durationMinutes} minutos, dando ${walk.steps} pasos y quemando ${walk.caloriesBurned} kilocalorías. ¡Excelente logro!"
                            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar Resumen de Actividad")
                    }
                }
            }
        }
    }
}
