package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraToolbar(
    onNavigateToMasterFolders: () -> Unit,
    onNavigateToMap: () -> Unit,
    onOpenGallery: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(toolbarItems) { item ->
            Icon(
                imageVector = item.icon,
                contentDescription = item.description,
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
                    .clickable(onClick = item.onClick)
                    .alpha(0.85f)
            )
        }
    }
}

data class ToolbarItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val onClick: () -> Unit
)