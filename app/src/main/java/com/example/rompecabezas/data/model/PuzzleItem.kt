package com.example.rompecabezas.data.model

data class PuzzleItem(
    val id: String,
    val filePath: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val maxUnlockedLevel: Int = 1,
    val isCompleted: Boolean = false
)

