package com.example.myagent.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TrailText(
    text: String,
    style: TextStyle,
    color: Color? = null,
    trailLength: Int = 150,
    trailColor: Color? = null,
    animate: Boolean = true,
    animateDurationMs: Int = 2000,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val step = with(density) { 1.5.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val layout = remember(text, style, textMeasurer) {
        textMeasurer.measure(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }

    val resolvedTrailColor = trailColor
        ?: if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color.White.copy(alpha = 0.15f)
        } else {
            Color.Black.copy(alpha = 0.15f)
        }
    val mainColor = color ?: style.color ?: MaterialTheme.colorScheme.onBackground

    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(animate) {
        if (animate) {
            val start = with(density) { Offset(-300.dp.toPx(), 300.dp.toPx()) }
            animOffset.snapTo(start)
            animOffset.animateTo(
                Offset.Zero,
                animationSpec = tween(durationMillis = animateDurationMs, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    animOffset.value.x.roundToInt(),
                    animOffset.value.y.roundToInt()
                )
            }
            .requiredSize(
                width = with(density) { layout.size.width.toDp() },
                height = with(density) { layout.size.height.toDp() }
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 1..trailLength) {
                val alpha = resolvedTrailColor.alpha * (1f - i / trailLength.toFloat())
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(-step * i, step * i),
                    style = style.copy(color = resolvedTrailColor.copy(alpha = alpha))
                )
            }
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset.Zero,
                style = style.copy(color = mainColor)
            )
        }
    }
}