package com.example.kalorias.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kalorias.ui.KaloriasViewModel
import com.example.kalorias.ui.theme.NeonCyan
import com.example.kalorias.ui.theme.NeonGreen
import com.example.kalorias.ui.theme.NeonPink

@Composable
fun HomeScreen(
    viewModel: KaloriasViewModel,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWalk: () -> Unit
) {
    val profile = viewModel.userProfile
    val consumed = viewModel.totalCaloriesConsumed
    val target = profile.targetCalories
    val remaining = target - consumed
    val burned = viewModel.totalCaloriesBurned
    val steps = viewModel.totalSteps
    val advice = viewModel.smartAdvice

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
                        text = "🎯 Kalorias OS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bienvenido, ${profile.name} • Meta: ${profile.goal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Concentric Activity Rings Card
        item {
            ActivityRingsCard(
                consumed = consumed,
                target = target,
                burned = burned,
                steps = steps
            )
        }

        // Calorie Dashboard Card (Futuristic Neon Accent)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            text = "Balance Calórico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Consumidas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$consumed kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Meta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$target kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Restantes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$remaining kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(text = "🥩 Prot: ${viewModel.totalProtein}g", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "🍞 Carbs: ${viewModel.totalCarbs}g", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "🥑 Grasas: ${viewModel.totalFat}g", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Hydration & Steps Row Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "Agua", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "${viewModel.waterGlasses} / 8 Vasos", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(text = "Pasos Hoy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "$steps pasos", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Smart AI Advisor Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🤖 Inteligencia & Asesoría Kalorias",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = advice,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Quick Navigation Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToNutrition,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir al Menú")
                }
                Button(
                    onClick = onNavigateToWalk,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actividad")
                }
            }
        }
    }
}

@Composable
fun ActivityRingsCard(
    consumed: Int,
    target: Int,
    burned: Int,
    steps: Int,
    stepTarget: Int = 8000
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Anillos de Actividad Diarios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                val consumedProgress = (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                val burnedProgress = (burned.toFloat() / 500f).coerceIn(0f, 1f)
                val stepsProgress = (steps.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeWidth = 12.dp.toPx()

                    // Outer Ring (Calorie Intake - Cyan)
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NeonCyan,
                        startAngle = -90f,
                        sweepAngle = 360f * consumedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Middle Ring (Active Calories Burned - Pink)
                    val middleRadius = (size.width / 2f) - strokeWidth - 6.dp.toPx()
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                        size = Size(middleRadius * 2, middleRadius * 2)
                    )
                    drawArc(
                        color = NeonPink,
                        startAngle = -90f,
                        sweepAngle = 360f * burnedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                        size = Size(middleRadius * 2, middleRadius * 2)
                    )

                    // Inner Ring (Steps Goal - Green)
                    val innerRadius = middleRadius - strokeWidth - 6.dp.toPx()
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2, innerRadius * 2)
                    )
                    drawArc(
                        color = NeonGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * stepsProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2, innerRadius * 2)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "$steps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "pasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NeonCyan) {}
                    Text(text = "Consumidas", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NeonPink) {}
                    Text(text = "Quemadas", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = NeonGreen) {}
                    Text(text = "Pasos", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
