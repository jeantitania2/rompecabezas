package com.example.rompecabezas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.rompecabezas.data.model.PuzzleLevel
import com.example.rompecabezas.data.model.PuzzlePiece

@Composable
fun PuzzleBoard(
    level: PuzzleLevel,
    placedPieces: List<PuzzlePiece>,
    selectedSlot: Pair<Int, Int>?,
    flashWrongSlot: Pair<Int, Int>?,
    flashSuccessSlot: Pair<Int, Int>?,
    boardWidthPx: Float,
    boardHeightPx: Float,
    onSlotClicked: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = level.rows
    val cols = level.cols
    val slotWidth = boardWidthPx / cols
    val slotHeight = boardHeightPx / rows

    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141824))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .pointerInput(boardWidthPx, boardHeightPx, rows, cols) {
                detectTapGestures { offset ->
                    if (slotWidth > 0 && slotHeight > 0) {
                        val clickedCol = (offset.x / slotWidth).toInt().coerceIn(0, cols - 1)
                        val clickedRow = (offset.y / slotHeight).toInt().coerceIn(0, rows - 1)
                        onSlotClicked(clickedRow, clickedCol)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw grid lines for empty slots (pure puzzle board without hints)
            val gridColor = Color.White.copy(alpha = 0.22f)
            for (r in 1 until rows) {
                val y = r * slotHeight
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(boardWidthPx, y),
                    strokeWidth = 1.5f
                )
            }
            for (c in 1 until cols) {
                val x = c * slotWidth
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, boardHeightPx),
                    strokeWidth = 1.5f
                )
            }

            // Draw placed pieces
            for (piece in placedPieces) {
                val x = piece.col * slotWidth
                val y = piece.row * slotHeight
                drawImage(
                    image = piece.bitmap.asImageBitmap(),
                    dstOffset = IntOffset(x.toInt(), y.toInt()),
                    dstSize = IntSize(slotWidth.toInt(), slotHeight.toInt())
                )
                // Subtle slot outline
                drawRect(
                    color = Color.Black.copy(alpha = 0.20f),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight),
                    style = Stroke(width = 1f)
                )
            }

            // Highlight user-selected slot (if user tapped a slot on the board)
            if (selectedSlot != null) {
                val x = selectedSlot.second * slotWidth
                val y = selectedSlot.first * slotHeight
                drawRect(
                    color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight)
                )
                drawRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight),
                    style = Stroke(width = 2.5f)
                )
            }

            // Flash red on slot if user guessed incorrectly
            if (flashWrongSlot != null) {
                val x = flashWrongSlot.second * slotWidth
                val y = flashWrongSlot.first * slotHeight
                drawRect(
                    color = Color(0xFFEF4444).copy(alpha = 0.40f),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight)
                )
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight),
                    style = Stroke(width = 3f)
                )
            }

            // Flash green on slot when piece is successfully placed
            if (flashSuccessSlot != null) {
                val x = flashSuccessSlot.second * slotWidth
                val y = flashSuccessSlot.first * slotHeight
                drawRect(
                    color = Color(0xFF22C55E).copy(alpha = 0.40f),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight)
                )
                drawRect(
                    color = Color(0xFF22C55E),
                    topLeft = Offset(x, y),
                    size = Size(slotWidth, slotHeight),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
