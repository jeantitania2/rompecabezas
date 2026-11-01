package com.example.rompecabezas.ui.navigation

sealed class Screen {
    object Home : Screen()
    object Upload : Screen()
    data class Game(val puzzleId: String, val levelNumber: Int = 1) : Screen()
    object Gallery : Screen()
}

