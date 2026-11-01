package com.example.rompecabezas.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Stable
class ZoomState(
    val minScale: Float = 0.8f,
    val maxScale: Float = 4.0f,
    initialScale: Float = 1.0f
) {
    var scale by mutableFloatStateOf(initialScale)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    fun updateGesture(pan: Offset, zoomChange: Float) {
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        scale = newScale
        offset += pan
    }

    fun zoomIn() {
        scale = (scale + 0.35f).coerceAtMost(maxScale)
    }

    fun zoomOut() {
        val newScale = (scale - 0.35f).coerceAtLeast(minScale)
        scale = newScale
        if (newScale <= 1.0f) {
            offset = Offset.Zero
        }
    }

    fun reset() {
        scale = 1.0f
        offset = Offset.Zero
    }
}

@Composable
fun rememberZoomState(
    minScale: Float = 0.8f,
    maxScale: Float = 4.0f,
    initialScale: Float = 1.0f
): ZoomState {
    return remember { ZoomState(minScale, maxScale, initialScale) }
}

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    zoomState: ZoomState = rememberZoomState(),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomState.updateGesture(pan, zoom)
                }
            },
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offset.x
                    translationY = zoomState.offset.y
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

