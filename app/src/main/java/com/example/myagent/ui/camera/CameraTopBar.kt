package com.example.myagent.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.theme.SmoochSans

@Composable
fun CameraTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        // Слева - текст "Timelapse"
        Text(
            text = "Timelapse",
            fontFamily = SmoochSans,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )
        
        // Справа - иконки
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка Settings
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Настройки",
                modifier = Modifier
                    .alpha(0.7f)
            )
            
            // Иконка FlashOn
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = "Вспышка",
                modifier = Modifier
                    .alpha(0.7f)
            )
            
            // Текст разрешения
            Text(
                text = "1080p",
                fontSize = 12.sp,
                modifier = Modifier
                    .alpha(0.7f)
            )
        }
    }
}