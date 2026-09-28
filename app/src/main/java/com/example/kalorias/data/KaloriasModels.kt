package com.example.kalorias.data

import java.util.UUID

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val mealType: MealType
)

enum class MealType(val label: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Almuerzo"),
    DINNER("Cena"),
    SNACK("Snack")
}

enum class ActivityType(val label: String, val calorieMultiplier: Double, val icon: String) {
    WALK("Caminata", 65.0, "🚶‍♂️"),
    RUN("Correr", 95.0, "🏃‍♂️"),
    HIKE("Senderismo", 80.0, "🏔️"),
    TREADMILL("Cinta", 60.0, "🏃‍♀️"),
    CYCLE("Ciclismo", 55.0, "🚴‍♂️")
}

data class WalkRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val distanceKm: Double,
    val steps: Int,
    val durationMinutes: Int,
    val activityName: String = "Caminata",
    val caloriesBurned: Int = (distanceKm * 65).toInt()
)

data class UserProfile(
    val name: String = "Carlos",
    val targetCalories: Int = 2100,
    val weightKg: Double = 72.5,
    val heightCm: Int = 178,
    val goal: String = "Mantener peso y tonificar"
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)
