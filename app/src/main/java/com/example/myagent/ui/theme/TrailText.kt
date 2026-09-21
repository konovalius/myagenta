package com.example.myagent.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TrailText(
    text: String,
    style: TextStyle,
    color: Color? = null,
    trailLength: Int = 150,
    trailColor: Color? = null,
    isDarkTheme: Boolean? = null,
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
    val dark = isDarkTheme
        ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val resolvedTrailColor = trailColor
        ?: if (dark) {
            Color.White.copy(alpha = 0.05f)
        } else {
            Color.Black.copy(alpha = 0.05f)
        }
    val mainColor = color ?: style.color ?: MaterialTheme.colorScheme.onBackground
    val drawSize = Size(layout.size.width.toFloat(), layout.size.height.toFloat())

    Box(
        modifier = modifier
            .requiredSize(
                width = with(density) { layout.size.width.toDp() },
                height = with(density) { layout.size.height.toDp() }
            )
            .drawBehind {
                for (i in 1..trailLength) {
                    val alpha = resolvedTrailColor.alpha * (1f - i / trailLength.toFloat())
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        topLeft = Offset(-step * i, step * i),
                        style = style.copy(color = resolvedTrailColor.copy(alpha = alpha)),
                        size = drawSize
                    )
                }
            }
    ) {
        Text(
            text = text,
            color = mainColor,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}