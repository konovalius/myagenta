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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.common.pressScale
import com.example.myagent.ui.theme.GoshaSans
import com.example.myagent.ui.theme.SmoochSans

@Composable
fun CameraTopBar(modifier: Modifier = Modifier) {
    val isSettingsActive = remember { mutableStateOf(false) }
    val isFlashActive = remember { mutableStateOf(false) }
    val isResolutionActive = remember { mutableStateOf(false) }
    val isProModeActive = remember { mutableStateOf(false) }
    val isAiHelpActive = remember { mutableStateOf(false) }
    var isFormatMenuOpen by remember { mutableStateOf(false) }

    if (isFormatMenuOpen) {
        FormatMenuRow(modifier = modifier, onSelect = { isFormatMenuOpen = false })
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // РќР°СЃС‚СЂРѕР№РєРё
        val settingsInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "РќР°СЃС‚СЂРѕР№РєРё",
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
        )

        // Р’СЃРїС‹С€РєР°
        val flashInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.FlashOn,
            contentDescription = "Р’СЃРїС‹С€РєР°",
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
        )

        // Р Р°Р·СЂРµС€РµРЅРёРµ
        val resolutionInteraction = remember { MutableInteractionSource() }
        Text(
            text = "1080",
            fontSize = 12.sp,
            fontFamily = GoshaSans,
            color = if (isResolutionActive.value) Color(0xFFFF3B30) else Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .pressScale(resolutionInteraction)
                .clickable(
                    interactionSource = resolutionInteraction,
                    indication = null
                ) { isResolutionActive.value = !isResolutionActive.value }
        )

        // Р СѓС‡РЅРѕР№ СЂРµР¶РёРј
        val proInteraction = remember { MutableInteractionSource() }
        Text(
            text = "PRO",
            fontSize = 12.sp,
            fontFamily = GoshaSans,
            color = if (isProModeActive.value) Color(0xFFFF3B30) else Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .pressScale(proInteraction)
                .clickable(
                    interactionSource = proInteraction,
                    indication = null
                ) { isProModeActive.value = !isProModeActive.value }
        )

        // Р¤РѕСЂРјР°С‚
        val formatInteraction = remember { MutableInteractionSource() }
        Text(
            text = "4:3",
            fontSize = 12.sp,
            fontFamily = GoshaSans,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .pressScale(formatInteraction)
                .clickable(
                    interactionSource = formatInteraction,
                    indication = null
                ) { isFormatMenuOpen = true }
        )

        // РџРѕРјРѕС‰СЊ РР
        val aiHelpInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = "РџРѕРјРѕС‰СЊ РР",
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
        )
        }
    }
}

@Composable
private fun FormatMenuRow(modifier: Modifier = Modifier, onSelect: () -> Unit) {
    val formats = listOf("16:9", "4:3", "1:1", "Full")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        formats.forEach { format ->
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = format,
                fontSize = 12.sp,
                fontFamily = SmoochSans,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .pressScale(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSelect
                    )
            )
        }
    }
}
