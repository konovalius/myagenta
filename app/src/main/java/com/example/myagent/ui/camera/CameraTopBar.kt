package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.common.pressScale
import com.example.myagent.ui.theme.SmoochSans

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
            .padding(horizontal = 16.dp, vertical = 88.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Настройки
        val settingsInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Настройки",
            tint = if (isSettingsActive.value) Color(0xFFFF3B30) else Color.White,
            modifier = Modifier
                .size(20.dp)
                .pressScale(settingsInteraction)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clickable(
                    interactionSource = settingsInteraction,
                    indication = null
                ) { isSettingsActive.value = !isSettingsActive.value }
                .alpha(if (isSettingsActive.value) 1.0f else 0.85f)
        )

        // Вспышка
        val flashInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.FlashOn,
            contentDescription = "Вспышка",
            tint = if (isFlashActive.value) Color(0xFFFF3B30) else Color.White,
            modifier = Modifier
                .size(24.dp)
                .pressScale(flashInteraction)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clickable(
                    interactionSource = flashInteraction,
                    indication = null
                ) { isFlashActive.value = !isFlashActive.value }
                .alpha(if (isFlashActive.value) 1.0f else 0.85f)
        )

        // Разрешение
        val resolutionInteraction = remember { MutableInteractionSource() }
        Text(
            text = "1080",
            fontSize = 20.sp,
            fontFamily = SmoochSans,
            color = if (isResolutionActive.value) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .pressScale(resolutionInteraction)
                .clickable(
                    interactionSource = resolutionInteraction,
                    indication = null
                ) { isResolutionActive.value = !isResolutionActive.value }
        )

        // Ручной режим
        val proInteraction = remember { MutableInteractionSource() }
        Text(
            text = "PRO",
            fontSize = 20.sp,
            fontFamily = SmoochSans,
            color = if (isProModeActive.value) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .pressScale(proInteraction)
                .clickable(
                    interactionSource = proInteraction,
                    indication = null
                ) { isProModeActive.value = !isProModeActive.value }
        )

        // Помощь ИИ
        val aiHelpInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "Помощь ИИ",
            tint = if (isAiHelpActive.value) Color(0xFFFF3B30) else Color.White,
            modifier = Modifier
                .size(24.dp)
                .pressScale(aiHelpInteraction)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clickable(
                    interactionSource = aiHelpInteraction,
                    indication = null
                ) { isAiHelpActive.value = !isAiHelpActive.value }
                .alpha(if (isAiHelpActive.value) 1.0f else 0.85f)
        )
    }
}