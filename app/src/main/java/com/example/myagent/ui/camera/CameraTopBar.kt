package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraTopBar() {
    val isSettingsActive = remember { mutableStateOf(false) }
    val isFlashActive = remember { mutableStateOf(false) }
    val isResolutionActive = remember { mutableStateOf(false) }
    val isProModeActive = remember { mutableStateOf(false) }
    val isAiHelpActive = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Настройки
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Настройки",
            tint = if (isSettingsActive.value) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier
                .clickable { isSettingsActive.value = !isSettingsActive.value }
                .alpha(if (isSettingsActive.value) 1.0f else 0.75f)
        )

        // Вспышка
        Icon(
            imageVector = Icons.Outlined.FlashOn,
            contentDescription = "Вспышка",
            tint = if (isFlashActive.value) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier
                .clickable { isFlashActive.value = !isFlashActive.value }
                .alpha(if (isFlashActive.value) 1.0f else 0.75f)
        )

        // Разрешение
        Text(
            text = "1080",
            fontSize = 14.sp,
            color = if (isResolutionActive.value) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable { isResolutionActive.value = !isResolutionActive.value }
        )

        // Ручной режим
        Text(
            text = "PRO",
            fontSize = 14.sp,
            color = if (isProModeActive.value) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable { isProModeActive.value = !isProModeActive.value }
        )

            // Помощь ИИ
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "Помощь ИИ",
            tint = if (isAiHelpActive.value) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier
                .clickable { isAiHelpActive.value = !isAiHelpActive.value }
                .alpha(if (isAiHelpActive.value) 1.0f else 0.75f)
        )
    }
}