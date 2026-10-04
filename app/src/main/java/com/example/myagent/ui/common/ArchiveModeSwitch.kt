package com.example.myagent.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.theme.GoshaSans

private val ARCHIVE_HEIGHT = 32.dp
private val ARCHIVE_THUMB = 26.dp
private val ARCHIVE_TRAVEL = 24.dp
private val ARCHIVE_PAD_END = 3.dp

@Composable
fun ArchiveModeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Режим архива",
    label: String = "Архив",
    icon: ImageVector = Icons.Outlined.PhotoLibrary
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) ARCHIVE_TRAVEL else 0.dp,
        animationSpec = tween(durationMillis = 140),
        label = "archiveThumb"
    )

    Row(
        modifier = modifier
            .height(ARCHIVE_HEIGHT)
            .clip(RoundedCornerShape(ARCHIVE_HEIGHT / 2))
            .background(if (checked) Color(0xFFFF3B30) else Color(0xFF8E8E93))
            .pointerInput(checked) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onCheckedChange(!checked)
                }
            }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Switch
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontFamily = GoshaSans,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 12.dp, end = 6.dp)
        )
        Box(
            modifier = Modifier
                .width(ARCHIVE_TRAVEL + ARCHIVE_THUMB + ARCHIVE_PAD_END)
                .height(ARCHIVE_THUMB)
                .padding(end = ARCHIVE_PAD_END),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(ARCHIVE_THUMB)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
