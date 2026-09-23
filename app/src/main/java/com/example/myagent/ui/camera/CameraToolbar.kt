package com.example.myagent.ui.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class ToolbarItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
fun CameraToolbar(
    onNavigateToMasterFolders: () -> Unit,
    onNavigateToMap: () -> Unit,
    onOpenGallery: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val alpha = remember { Animatable(0.3f) }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 300)
            )
            delay(2000)
            alpha.animateTo(
                targetValue = 0.3f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }
    
    val toolbarItems = listOf(
        ToolbarItem(
            icon = Icons.Filled.Folder,
            contentDescription = "Мастер-папки",
            onClick = onNavigateToMasterFolders
        ),
        ToolbarItem(
            icon = Icons.Filled.Map,
            contentDescription = "Карта",
            onClick = onNavigateToMap
        ),
        ToolbarItem(
            icon = Icons.Filled.PhotoLibrary,
            contentDescription = "Галерея",
            onClick = onOpenGallery
        ),
        ToolbarItem(
            icon = Icons.Filled.HelpOutline,
            contentDescription = "В первый раз",
            onClick = onNavigateToOnboarding
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = -20.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .alpha(alpha.value),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(toolbarItems) { item ->
                IconButton(
                    onClick = item.onClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}