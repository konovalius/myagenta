package com.example.myagent.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val SWITCH_WIDTH = 56.dp
private val SWITCH_HEIGHT = 28.dp
private val SWITCH_THUMB = 24.dp
private val SWITCH_TRAVEL = 28.dp

@Composable
fun DeleteModeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Режим удаления"
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) SWITCH_TRAVEL else 0.dp,
        animationSpec = tween(durationMillis = 140),
        label = "deleteThumb"
    )

    Box(
        modifier = modifier
            .width(SWITCH_WIDTH)
            .height(SWITCH_HEIGHT)
            .clip(RoundedCornerShape(SWITCH_HEIGHT / 2))
            .background(
                if (checked) Color(0xFFFF3B30) else Color(0xFF8E8E93)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Ждем отпускания пальца
                    val up = awaitPointerEvent() 
                    // Меняем состояние при любом касании и отпускании
                    onCheckedChange(!checked)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .offset(x = thumbOffset)
                .size(SWITCH_THUMB)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f))
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Switch
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
