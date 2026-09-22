package com.example.myagent.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.R
import com.example.myagent.ui.theme.ThemeToggleSwitch
import com.example.myagent.ui.theme.TrailText

@Composable
fun HomeScreen(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenImage: (Uri) -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenMasterFolders: () -> Unit
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onOpenImage(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrailText(
                text = "Timelapse",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayMedium,
                isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Выберите способ входа",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            HomeEntryCard(
                icon = painterResource(R.drawable.ic_globe),
                title = "Войти через карту",
                subtitle = "По геолокации",
                onClick = onOpenMap
            )
            Spacer(Modifier.height(16.dp))
            HomeEntryCard(
                icon = painterResource(R.drawable.ic_old_building),
                title = "Войти через изображение",
                subtitle = "Выбрать фото-ориентир",
                onClick = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            HomeEntryCard(
                icon = painterResource(R.drawable.ic_book),
                title = "Мастер-папки",
                subtitle = "Гео и объекты",
                onClick = onOpenMasterFolders
            )
            Spacer(Modifier.height(16.dp))
            HomeEntryCard(
                icon = painterResource(R.drawable.ic_question),
                title = "Войти в первый раз",
                subtitle = "Обучение",
                onClick = onOpenOnboarding
            )
            Spacer(Modifier.height(28.dp))
            ThemeToggleSwitch(
                isDark = isDark,
                onToggle = onThemeToggle
            )
        }
    }
}

@Composable
private fun HomeEntryCard(
    icon: Painter,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            badge?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}