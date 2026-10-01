package com.example.kalorias

import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.example.kalorias.data.MealType
import com.example.kalorias.ui.KaloriasViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KaloriasPersistenceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun clearStoredData() {
        application.getSharedPreferences("walk_history", 0).edit().clear().commit()
        application.getSharedPreferences("nutrition_data", 0).edit().clear().commit()
    }

    @Test
    fun clearedHistoryStaysEmptyAfterViewModelRestart() {
        val firstViewModel = KaloriasViewModel(application)
        firstViewModel.clearWalkRecords()

        val restartedViewModel = KaloriasViewModel(application)

        assertTrue(restartedViewModel.walkRecords.isEmpty())
    }

    @Test
    fun addedFoodStaysSavedAfterViewModelRestart() {
        val firstViewModel = KaloriasViewModel(application)
        firstViewModel.addFoodItem("Manzana", 80, 0, 21, 0, MealType.SNACK)

        val restartedViewModel = KaloriasViewModel(application)

        assertEquals("Manzana", restartedViewModel.foodItems.last().name)
        assertEquals(3, restartedViewModel.foodItems.size)
    }
}