package com.example.kalorias.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kalorias.data.UserProfile
import com.example.kalorias.ui.KaloriasViewModel
import com.example.kalorias.util.AutoUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: KaloriasViewModel) {
    val profile = viewModel.userProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(profile.name) }
    var targetCalories by remember { mutableStateOf(if (profile.targetCalories == 0) "" else profile.targetCalories.toString()) }
    var weight by remember { mutableStateOf(if (profile.weightKg == 0.0) "" else profile.weightKg.toString()) }
    var height by remember { mutableStateOf(if (profile.heightCm == 0) "" else profile.heightCm.toString()) }
    var goal by remember { mutableStateOf(profile.goal) }
    var savedMessage by remember { mutableStateOf(false) }

    var updateDialogVersion by remember { mutableStateOf<String?>(null) }
    var updateDialogUrl by remember { mutableStateOf<String?>(null) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var upToDateMessage by remember { mutableStateOf(false) }

    if (updateDialogVersion != null) {
        AlertDialog(
            onDismissRequest = { updateDialogVersion = null },
            title = { Text("🚀 ¡Nueva Versión Disponible!") },
            text = { Text("Se encontró la versión $updateDialogVersion en GitHub. ¿Deseas descargar e instalar la actualización ahora?") },
            confirmButton = {
                Button(onClick = {
                    AutoUpdater.downloadAndInstallApk(context, updateDialogUrl ?: "", updateDialogVersion ?: "v1.0")
                    updateDialogVersion = null
                }) {
                    Text("Descargar e Instalar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { updateDialogVersion = null }) {
                    Text("Más Tarde")
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
            Text(
                text = "Perfil y Metas 👤",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Personaliza tus objetivos y tus datos físicos para ajustar el cálculo calórico de Kalorias.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (savedMessage) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¡Perfil actualizado con éxito!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej. Carlos") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetCalories,
                        onValueChange = { targetCalories = it },
                        label = { Text("Meta diaria de calorías (kcal)") },
                        placeholder = { Text("Ej. 2000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Peso (kg)") },
                            placeholder = { Text("Ej. 70.0") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Altura (cm)") },
                            placeholder = { Text("Ej. 175") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Objetivo") },
                        placeholder = { Text("Ej. Perder grasa, Ganar músculo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cals = targetCalories.toIntOrNull() ?: 2000
                            val wgt = weight.toDoubleOrNull() ?: 0.0
                            val hgt = height.toIntOrNull() ?: 0
                            viewModel.updateProfile(
                                UserProfile(
                                    name = name,
                                    targetCalories = cals,
                                    weightKg = wgt,
                                    heightCm = hgt,
                                    goal = goal
                                )
                            )
                            savedMessage = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Cambios")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                checkingUpdates = true
                                upToDateMessage = false
                                AutoUpdater.checkForUpdates(context, "v1.0") { tag, url ->
                                    updateDialogVersion = tag
                                    updateDialogUrl = url
                                    checkingUpdates = false
                                }
                                delay(1200L)
                                if (updateDialogVersion == null) {
                                    checkingUpdates = false
                                    upToDateMessage = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (checkingUpdates) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscando en GitHub...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscar Actualizaciones (GitHub)")
                        }
                    }

                    if (upToDateMessage) {
                        Text(
                            text = "✅ Tu aplicación está al día (versión v1.0).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
