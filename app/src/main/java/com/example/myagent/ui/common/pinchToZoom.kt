package com.example.myagent.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Зум и панорамирование для просмотра изображения.
 * - Пинч (два пальца) — масштаб.
 * - Один палец — панорамирование, когда scale > 1.
 * - Двойной тап — быстрый зум к 2.5x или сброс к 1x.
 * - Кламп: от 1x до 5x. Ниже 1x — не уходит. При возврате к 1x — панорамирование сбрасывается.
 */
@Composable
fun Modifier.pinchToZoom(
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapScale: Float = 2.5f,
): Modifier {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    return this
        .pointerInput(minScale, maxScale) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                scale = newScale

                if (newScale > minScale) {
                    val maxX = (size.width * (newScale - 1f)) / 2f
                    val maxY = (size.height * (newScale - 1f)) / 2f
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                    )
                } else {
                    offset = Offset.Zero
                }
            }
        }
        .pointerInput(minScale, maxScale, doubleTapScale) {
            detectTapGestures(
                onDoubleTap = {
                    if (scale > minScale) {
                        scale = minScale
                        offset = Offset.Zero
                    } else {
                        scale = doubleTapScale.coerceIn(minScale, maxScale)
                    }
                }
            )
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        }
}
