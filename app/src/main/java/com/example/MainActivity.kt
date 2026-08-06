package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.AiDifficulty
import com.example.model.PlayerColor
import com.example.model.PlayerType
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GameSetupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WifiMultiplayerScreen
import com.example.ui.theme.LudoMasterTheme
import com.example.ui.viewmodel.LudoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LudoViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()

            LudoMasterTheme(darkTheme = settings.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LudoAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun LudoAppNavigation(viewModel: LudoViewModel) {
    val navController = rememberNavController()
    val savedGame by viewModel.savedGame.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                savedGame = savedGame,
                onQuickPlay = {
                    viewModel.startNewGame(
                        playerCount = 4,
                        playerTypes = mapOf(
                            PlayerColor.RED to PlayerType.HUMAN,
                            PlayerColor.GREEN to PlayerType.AI,
                            PlayerColor.YELLOW to PlayerType.AI,
                            PlayerColor.BLUE to PlayerType.AI
                        ),
                        aiDifficulties = mapOf(
                            PlayerColor.GREEN to AiDifficulty.MEDIUM,
                            PlayerColor.YELLOW to AiDifficulty.MEDIUM,
                            PlayerColor.BLUE to AiDifficulty.MEDIUM
                        )
                    )
                    navController.navigate("game")
                },
                onCustomSetup = { navController.navigate("game_setup") },
                onNavigateWifiMultiplayer = { navController.navigate("wifi_multiplayer") },
                onResumeGame = { state ->
                    viewModel.resumeSavedGame(state)
                    navController.navigate("game")
                },
                onNavigateStats = { navController.navigate("stats") },
                onNavigateSettings = { navController.navigate("settings") },
                onNavigateRules = { navController.navigate("rules") },
                onNavigateAbout = { navController.navigate("about") }
            )
        }

        composable("wifi_multiplayer") {
            WifiMultiplayerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onStartGame = { navController.navigate("game") }
            )
        }

        composable("game_setup") {
            GameSetupScreen(
                onStartGame = { count, types, diffs ->
                    viewModel.startNewGame(count, types, diffs)
                    navController.navigate("game")
                },
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("game") {
            GameScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("stats") {
            StatsScreen(
                viewModel = viewModel,
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("rules") {
            RulesScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("about") {
            AboutScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}
