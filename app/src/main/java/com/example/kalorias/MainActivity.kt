package com.example.kalorias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kalorias.ui.KaloriasViewModel
import com.example.kalorias.ui.screens.ChallengesScreen
import com.example.kalorias.ui.screens.HistoryScreen
import com.example.kalorias.ui.screens.HomeScreen
import com.example.kalorias.ui.screens.NutritionScreen
import com.example.kalorias.ui.screens.ProfileScreen
import com.example.kalorias.ui.screens.WalkScreen
import com.example.kalorias.ui.theme.KaloriasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaloriasTheme {
                KaloriasApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun KaloriasApp(viewModel: KaloriasViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Futuristic Header
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "⚡ KALORIAS OS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Fitness & Nutrition Intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Navigation Items
                    AppDestinations.entries.forEach { destination ->
                        NavigationDrawerItem(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label, fontWeight = FontWeight.SemiBold) },
                            selected = destination == currentDestination,
                            onClick = {
                                currentDestination = destination
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.label,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú Lateral")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val bottomNavItems = listOf(
                        AppDestinations.DASHBOARD to "Dashboard",
                        AppDestinations.WALK to "Actividad",
                        AppDestinations.NUTRITION to "Nutrición",
                        AppDestinations.HISTORY to "Historial"
                    )
                    bottomNavItems.forEach { (destination, shortLabel) ->
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = shortLabel) },
                            label = { Text(shortLabel, fontWeight = FontWeight.Bold) },
                            selected = destination == currentDestination,
                            onClick = { currentDestination = destination },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    AppDestinations.DASHBOARD -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToNutrition = { currentDestination = AppDestinations.NUTRITION },
                        onNavigateToWalk = { currentDestination = AppDestinations.WALK }
                    )
                    AppDestinations.NUTRITION -> NutritionScreen(viewModel = viewModel)
                    AppDestinations.WALK -> WalkScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { currentDestination = AppDestinations.HISTORY }
                    )
                    AppDestinations.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppDestinations.CHALLENGES -> ChallengesScreen(viewModel = viewModel)
                    AppDestinations.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("Resumen & Dashboard", Icons.Default.Home),
    NUTRITION("Menú & Nutrición", Icons.Default.Restaurant),
    WALK("Caminatas & Actividad", Icons.AutoMirrored.Filled.DirectionsWalk),
    HISTORY("Historial & Rutas", Icons.Default.History),
    CHALLENGES("Retos & Logros", Icons.Default.EmojiEvents),
    PROFILE("Perfil & Metas", Icons.Default.Person),
}

@Preview(showBackground = true)
@Composable
fun KaloriasAppPreview() {
    KaloriasTheme {
        KaloriasApp()
    }
}
