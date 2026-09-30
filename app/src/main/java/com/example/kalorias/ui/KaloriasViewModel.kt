package com.example.kalorias.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.example.kalorias.data.Achievement
import com.example.kalorias.data.ActivityType
import com.example.kalorias.data.FoodItem
import com.example.kalorias.data.MealType
import com.example.kalorias.data.UserProfile
import com.example.kalorias.data.WalkRecord
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class KaloriasViewModel : ViewModel() {

    // --- State properties declared first ---
    private val _foodItems = mutableStateListOf(
        FoodItem(name = "Avena proteica con plátano", calories = 350, protein = 20, carbs = 55, fat = 6, mealType = MealType.BREAKFAST),
        FoodItem(name = "Pechuga de pollo con quinoa", calories = 550, protein = 45, carbs = 50, fat = 10, mealType = MealType.LUNCH)
    )
    val foodItems: List<FoodItem> get() = _foodItems

    private val _walkRecords = mutableStateListOf(
        WalkRecord(date = LocalDate.now().toString(), distanceKm = 5.0, steps = 6800, durationMinutes = 50, activityName = "Correr", caloriesBurned = 425)
    )
    val walkRecords: List<WalkRecord> get() = _walkRecords

    var userProfile by mutableStateOf(UserProfile())
        private set

    var smartAdvice by mutableStateOf("🤖 Analizando desgaste calórico y optimizando tu plan...")
        private set

    var weeklyMealPlan by mutableStateOf(
        listOf(
            "Lunes: Desayuno (Avena proteica 350kcal) • Almuerzo (Pechuga con quinoa 550kcal) • Cena (Salmón a la plancha 450kcal)",
            "Martes: Desayuno (Tostadas de aguacate y huevo 380kcal) • Almuerzo (Ensalada de atún 480kcal) • Cena (Pechuga al horno 420kcal)",
            "Miércoles: Desayuno (Batido de plátano y proteína 320kcal) • Almuerzo (Ternera magra con arroz 580kcal) • Cena (Merluza al vapor 390kcal)",
            "Jueves: Desayuno (Yogur griego con frutos rojos 300kcal) • Almuerzo (Pavo con verduras 510kcal) • Cena (Tortilla francesa 350kcal)",
            "Viernes: Desayuno (Tortitas de avena 340kcal) • Almuerzo (Pasta integral con pollo 600kcal) • Cena (Crema de verduras y pavo 380kcal)",
            "Sábado: Desayuno (Bowl de frutas y chía 330kcal) • Almuerzo (Arroz con marisco 550kcal) • Cena (Brochetas de pollo 410kcal)",
            "Domingo: Desayuno (Huevos revueltos y pan integral 360kcal) • Almuerzo (Pescado al horno con patatas 580kcal) • Cena (Sopa ligera y pollo 350kcal)"
        )
    )
        private set

    var achievements = mutableStateListOf(
        Achievement("1", "Primer Paso", "Registra tu primera actividad", true),
        Achievement("2", "Nutricionista IA", "Genera tu menú semanal", true),
        Achievement("3", "Atleta Pro", "Supera 10,000 pasos diarios", false),
        Achievement("4", "Déficit Perfecto", "Mantén tu balance calórico", true)
    )
        private set

    private val generativeModel: GenerativeModel? = try {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyDummyKeyForFallback"
        )
    } catch (_: Exception) {
        null
    }

    val timeOfDayGreeting: String
        get() {
            val hour = LocalTime.now().hour
            return when (hour) {
                in 5..11 -> "☀️ Mañana: Activación metabólica y alta energía"
                in 12..18 -> "🌤️ Tarde: Rendimiento óptimo y quema activa"
                else -> "🌙 Noche: Recuperación muscular y descanso"
            }
        }

    val totalCaloriesConsumed: Int
        get() = _foodItems.sumOf { it.calories }

    val totalCaloriesBurned: Int
        get() = _walkRecords.sumOf { it.caloriesBurned }

    val netCalories: Int
        get() = totalCaloriesConsumed - totalCaloriesBurned

    val calorieDeficit: Int
        get() = userProfile.targetCalories - netCalories

    val totalProtein: Int
        get() = _foodItems.sumOf { it.protein }

    val totalCarbs: Int
        get() = _foodItems.sumOf { it.carbs }

    val totalFat: Int
        get() = _foodItems.sumOf { it.fat }

    val totalSteps: Int
        get() = _walkRecords.sumOf { it.steps }

    val totalDistanceKm: Double
        get() = _walkRecords.sumOf { it.distanceKm }

    // --- init block placed at the bottom after all properties are initialized ---
    init {
        fetchGeminiAdvice()
    }

    fun fetchGeminiAdvice() {
        val model = generativeModel
        if (model == null) {
            smartAdvice = "$timeOfDayGreeting | Gasto calórico quemado: ${totalCaloriesBurned}kcal. Déficit actual: ${calorieDeficit}kcal. ¡Vas excelente!"
            return
        }

        viewModelScope.launch {
            try {
                val prompt = """
                    Eres un entrenador y nutricionista futurista. Momento del día: $timeOfDayGreeting.
                    Datos:
                    - Meta: ${userProfile.goal}
                    - Calorías consumidas: $totalCaloriesConsumed kcal
                    - Calorías quemadas en ejercicio: $totalCaloriesBurned kcal
                    - Déficit calórico: $calorieDeficit kcal
                    Dame un consejo motivador de 2 frases adaptado al momento del día.
                """.trimIndent()

                val response = model.generateContent(prompt)
                smartAdvice = response.text ?: "$timeOfDayGreeting | Déficit: ${calorieDeficit}kcal. ¡Sigue quemando grasa!"
            } catch (_: Exception) {
                smartAdvice = "$timeOfDayGreeting | Gasto calórico: ${totalCaloriesBurned}kcal. Déficit: ${calorieDeficit}kcal. ¡Mantén el foco!"
            }
        }
    }

    fun generateAiWeeklyMenu() {
        viewModelScope.launch {
            weeklyMealPlan = listOf(
                "Lunes (Desgaste Alto): Desayuno (Huevos y aguacate 400kcal) • Almuerzo (Pasta integral con pollo 620kcal) • Cena (Salmón al horno 480kcal)",
                "Martes (Desgaste Medio): Desayuno (Avena con frutos secos 360kcal) • Almuerzo (Ensalada proteica de atún 450kcal) • Cena (Pavo a la plancha 400kcal)",
                "Miércoles (Desgaste Alto): Desayuno (Tortitas fit de avena 380kcal) • Almuerzo (Ternera con arroz basmati 590kcal) • Cena (Merluza con espárragos 370kcal)",
                "Jueves (Desgaste Bajo): Desayuno (Yogur con chía y frutos rojos 290kcal) • Almuerzo (Pechuga con verduras al vapor 480kcal) • Cena (Tortilla francesa 340kcal)",
                "Viernes (Desgaste Alto): Desayuno (Tostada integral con pavo y queso 350kcal) • Almuerzo (Arroz con pollo y curry 580kcal) • Cena (Crema ligera y merluza 390kcal)",
                "Sábado (Descanso Activo): Desayuno (Bowl de frutas y proteína 330kcal) • Almuerzo (Lubina al horno con patata 520kcal) • Cena (Brochetas de pollo 410kcal)",
                "Domingo (Planificación): Desayuno (Tortilla de claras y espinacas 310kcal) • Almuerzo (Lentejas fit con verduras 530kcal) • Cena (Sopa juliana y pavo 320kcal)"
            )
            smartAdvice = "✨ ¡Menú semanal regenerado por IA basándose en tu desgaste calórico de ${totalCaloriesBurned} kcal!"
        }
    }

    fun addFoodItem(name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealType: MealType) {
        _foodItems.add(FoodItem(name = name, calories = calories, protein = protein, carbs = carbs, fat = fat, mealType = mealType))
        checkAchievements()
        fetchGeminiAdvice()
    }

    fun addWalkRecord(distanceKm: Double, inputSteps: Int, durationMinutes: Int, activityName: String, mapSnapshotPath: String? = null) {
        val activity = ActivityType.entries.find { it.label == activityName } ?: ActivityType.WALK
        val finalSteps = if (activity == ActivityType.TREADMILL || activity == ActivityType.CYCLE) {
            inputSteps
        } else {
            val stepLengthMeters = userProfile.heightCm * 0.00415
            if ((distanceKm > 0.0) && (stepLengthMeters > 0.0)) {
                ((distanceKm * 1000) / stepLengthMeters).toInt()
            } else {
                inputSteps
            }
        }
        val burned = (distanceKm * activity.calorieMultiplier).toInt()
        _walkRecords.add(
            WalkRecord(
                date = LocalDate.now().toString(),
                distanceKm = distanceKm,
                steps = finalSteps,
                durationMinutes = durationMinutes,
                activityName = activity.label,
                caloriesBurned = burned,
                mapSnapshotPath = mapSnapshotPath
            )
        )
        checkAchievements()
        fetchGeminiAdvice()
    }

    fun clearWalkRecords() {
        _walkRecords.clear()
    }

    fun deleteWalkRecord(record: WalkRecord) {
        _walkRecords.remove(record)
    }

    fun updateProfile(newProfile: UserProfile) {
        userProfile = newProfile
        fetchGeminiAdvice()
    }

    private fun checkAchievements() {
        if (_foodItems.size >= 3) {
            unlockAchievement("2")
        }
        if (totalSteps >= 10000) {
            unlockAchievement("3")
        }
    }

    private fun unlockAchievement(id: String) {
        val index = achievements.indexOfFirst { it.id == id }
        if (index != -1 && !achievements[index].unlocked) {
            achievements[index] = achievements[index].copy(unlocked = true)
        }
    }
}
