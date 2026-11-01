package com.example.rompecabezas.data.model

data class PuzzleLevel(
    val levelNumber: Int,
    val pieceCount: Int,
    val rows: Int,
    val cols: Int
) {
    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 10

        fun getLevel(levelNumber: Int, imageWidth: Int, imageHeight: Int): PuzzleLevel {
            val clampedLevel = levelNumber.coerceIn(MIN_LEVEL, MAX_LEVEL)
            val pieces = clampedLevel * 10
            val isLandscape = imageWidth >= imageHeight

            val (r, c) = when (pieces) {
                10 -> if (isLandscape) 2 to 5 else 5 to 2
                20 -> if (isLandscape) 4 to 5 else 5 to 4
                30 -> if (isLandscape) 5 to 6 else 6 to 5
                40 -> if (isLandscape) 5 to 8 else 8 to 5
                50 -> if (isLandscape) 5 to 10 else 10 to 5
                60 -> if (isLandscape) 6 to 10 else 10 to 6
                70 -> if (isLandscape) 7 to 10 else 10 to 7
                80 -> if (isLandscape) 8 to 10 else 10 to 8
                90 -> if (isLandscape) 9 to 10 else 10 to 9
                100 -> 10 to 10
                else -> 2 to 5
            }

            return PuzzleLevel(
                levelNumber = clampedLevel,
                pieceCount = pieces,
                rows = r,
                cols = c
            )
        }
    }
}

