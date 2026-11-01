package com.example.rompecabezas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.rompecabezas.data.repository.PuzzleRepository
import com.example.rompecabezas.ui.navigation.Screen
import com.example.rompecabezas.ui.screens.GalleryScreen
import com.example.rompecabezas.ui.screens.GameScreen
import com.example.rompecabezas.ui.screens.HomeScreen
import com.example.rompecabezas.ui.screens.UploadScreen
import com.example.rompecabezas.ui.theme.RompecabezasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RompecabezasTheme {
                RompecabezasApp()
            }
        }
    }
}

@Composable
fun RompecabezasApp() {
    val context = LocalContext.current
    val repository = remember { PuzzleRepository(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = currentScreen, label = "screenTransition") { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        onNavigateToUpload = {
                            currentScreen = Screen.Upload
                        },
                        onNavigateToGallery = {
                            currentScreen = Screen.Gallery
                        },
                        onPlayDefaultLogo = {
                            currentScreen = Screen.Game(
                                puzzleId = PuzzleRepository.DEFAULT_LOGO_ID,
                                levelNumber = 1
                            )
                        }
                    )
                }

                is Screen.Upload -> {
                    UploadScreen(
                        repository = repository,
                        onStartGame = { puzzleId ->
                            currentScreen = Screen.Game(puzzleId = puzzleId, levelNumber = 1)
                        },
                        onNavigateToGallery = {
                            currentScreen = Screen.Gallery
                        },
                        onNavigateToHome = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Game -> {
                    GameScreen(
                        puzzleId = screen.puzzleId,
                        initialLevelNumber = screen.levelNumber,
                        repository = repository,
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        onNavigateToGallery = {
                            currentScreen = Screen.Gallery
                        },
                        onNavigateToHome = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Gallery -> {
                    GalleryScreen(
                        repository = repository,
                        onSelectPuzzle = { puzzleId, level ->
                            currentScreen = Screen.Game(puzzleId = puzzleId, levelNumber = level)
                        },
                        onNavigateToUpload = {
                            currentScreen = Screen.Upload
                        },
                        onNavigateToHome = {
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}
