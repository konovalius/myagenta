package com.example.myagent.ui.folders.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myagent.R
import com.example.myagent.data.db.entity.Media

@Composable
fun MediaGridItem(
    media: Media,
    folderUuid: String,
    index: Int,
    onOpenPhoto: (Uri, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = Uri.parse(media.uri)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (media.type == "video") {
                    // Открываем видео в системном плеере
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/mp4")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Если нет плеера, пытаемся открыть как фото
                        onOpenPhoto(uri, folderUuid, index)
                    }
                } else {
                    // Открываем фото
                    onOpenPhoto(uri, folderUuid, index)
                }
            }
    ) {
        if (media.type == "video") {
            // Для видео используем предзагрузчик thumbnail
            AsyncImage(
                model = uri,
                contentDescription = "Видео",
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_video),
                    contentDescription = "Play video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Для фото обычный AsyncImage
            AsyncImage(
                model = uri,
                contentDescription = "Фото",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}