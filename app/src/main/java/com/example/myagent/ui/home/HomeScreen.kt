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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
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
            .background(Color(0xFF101418))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MyAgent",
                color = Color.White,
                fontSize = 40.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Выберите способ входа",
                color = Color(0xFF9AA3AF),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            HomeEntryCard(
                icon = Icons.Filled.Map,
                title = "Войти через карту",
                subtitle = "По геолокации",
                onClick = onOpenMap
            )
            Spacer(Modifier.height(16.dp))
            HomeEntryCard(
                icon = Icons.Filled.PhotoLibrary,
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
                icon = Icons.Filled.Folder,
                title = "Мастер-папки",
                subtitle = "Гео и объекты",
                onClick = onOpenMasterFolders
            )
            Spacer(Modifier.height(16.dp))
            HomeEntryCard(
                icon = Icons.Filled.AutoAwesome,
                title = "Войти в первый раз",
                subtitle = "Обучение и настройка",
                onClick = onOpenOnboarding
            )
        }
    }
}

@Composable
private fun HomeEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C2128))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2B313B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 17.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = Color(0xFF9AA3AF), fontSize = 13.sp)
                }
            }
            badge?.let {
                Text(
                    text = it,
                    color = Color(0xFF9AA3AF),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0xFF2B313B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}