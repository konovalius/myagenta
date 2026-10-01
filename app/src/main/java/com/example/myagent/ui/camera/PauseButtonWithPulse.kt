package com.example.myagent.ui.camera

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myagent.ui.common.ShutterUi

/**
 * Кнопка паузы с пульсирующим кругом.
 *
 * Требование 1: максимальный размер круга пульсации = размер кнопки съёмки (ShutterUi.Size).
 * Пульсация: scale от 0.7 до 1.0, RepeatMode.Reverse.
 */
@Composable
fun PauseButtonWithPulse(
    modifier: Modifier = Modifier,
    isPaused: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pausePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pauseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pauseAlpha"
    )

    val s = pulseScale
    val a = pulseAlpha

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isPaused) {
            Box(
                modifier = Modifier
                    .size(ShutterUi.Size * s)
                    .background(
                        Color.White.copy(alpha = 0.9f * a),
                        CircleShape
                    )
            )
        }
        PauseIcon()
    }
}