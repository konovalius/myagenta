package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraToolbar(
    onNavigateToMasterFolders: () -> Unit,
    onNavigateToMap: () -> Unit,
    onOpenGallery: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val toolbarItems = listOf(
        ToolbarItem(
            icon = Icons.Outlined.Folder,
            description = "Мастер-папки",
            onClick = onNavigateToMasterFolders
        ),
        ToolbarItem(
            icon = Icons.Outlined.Map,
            description = "Карта",
            onClick = onNavigateToMap
        ),
        ToolbarItem(
            icon = Icons.Outlined.PhotoLibrary,
            description = "Галерея",
            onClick = onOpenGallery
        ),
        ToolbarItem(
            icon = Icons.Outlined.HelpOutline,
            description = "В первый раз",
            onClick = onNavigateToOnboarding
        )
    )

    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(toolbarItems) { item ->
            Box(contentAlignment = Alignment.Center) {
                // Чёрная иконка-обводка
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
                // Основная иконка
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.description,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = item.onClick)
                        .alpha(0.75f)
                )
            }
        }
    }
}

data class ToolbarItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val onClick: () -> Unit
)