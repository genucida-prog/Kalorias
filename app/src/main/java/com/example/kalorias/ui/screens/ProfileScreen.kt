package com.example.kalorias.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kalorias.data.UserProfile
import com.example.kalorias.ui.KaloriasViewModel

@Composable
fun ProfileScreen(viewModel: KaloriasViewModel) {
    val profile = viewModel.userProfile

    var name by remember { mutableStateOf(profile.name) }
    var targetCalories by remember { mutableStateOf(profile.targetCalories.toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
    var height by remember { mutableStateOf(profile.heightCm.toString()) }
    var goal by remember { mutableStateOf(profile.goal) }
    var savedMessage by remember { mutableStateOf(false) }

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
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetCalories,
                        onValueChange = { targetCalories = it },
                        label = { Text("Meta diaria de calorías (kcal)") },
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
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Altura (cm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Objetivo (ej. Perder grasa, Ganar músculo)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cals = targetCalories.toIntOrNull() ?: profile.targetCalories
                            val wgt = weight.toDoubleOrNull() ?: profile.weightKg
                            val hgt = height.toIntOrNull() ?: profile.heightCm
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
                }
            }
        }
    }
}
