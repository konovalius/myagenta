package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.common.pressScale
import com.example.myagent.ui.theme.SmoochSans

@Composable
fun CameraToolbar(
    isSlowMotionActive: Boolean,
    onToggleSlowMotion: () -> Unit,
    isTimelapseActive: Boolean,
    onToggleTimelapse: () -> Unit,
    isVideoMode: Boolean,
    onToggleVideoMode: () -> Unit,
    onNavigateToMasterFolders: () -> Unit,
    onNavigateToMap: () -> Unit,
    onOpenGallery: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolbarItems = listOf(
        ToolbarItem("Слоумо", isSlowMotionActive, onToggleSlowMotion),
        ToolbarItem("Таймлапс", isTimelapseActive, onToggleTimelapse),
        ToolbarItem("Видео", isVideoMode, onToggleVideoMode),
        ToolbarItem("Мастер-папки", false, onNavigateToMasterFolders),
        ToolbarItem("Карта", false, onNavigateToMap),
        ToolbarItem("Галерея", false, onOpenGallery),
        ToolbarItem("В первый раз", false, onNavigateToOnboarding)
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(toolbarItems) { item ->
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = item.label,
                fontSize = 18.sp,
                fontFamily = SmoochSans,
                fontWeight = FontWeight.Bold,
                color = if (item.isActive) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .pressScale(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = item.onClick
                    )
            )
        }
    }
}

data class ToolbarItem(
    val label: String,
    val isActive: Boolean,
    val onClick: () -> Unit
)