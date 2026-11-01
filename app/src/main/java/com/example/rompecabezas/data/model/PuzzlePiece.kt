package com.example.rompecabezas.data.model

import android.graphics.Bitmap

data class PuzzlePiece(
    val id: Int,
    val row: Int,
    val col: Int,
    val bitmap: Bitmap,
    val isPlaced: Boolean = false
)

