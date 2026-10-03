package com.example.myagent.ui.folders.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.db.entity.Subfolder

@Composable
fun SubfolderGridItem(
    subfolder: Subfolder,
    lastMedia: Media?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Тень - сдвинутая копия ячейки
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(8.dp))
                .offset(x = 6.dp, y = 6.dp)
        )

        // Основная ячейка
        Box(modifier = Modifier.fillMaxSize()) {
            lastMedia?.let { media ->
                val uri = Uri.parse(media.uri)
                if (media.type == "video") {
                    // Для видео используем превью с первого кадра
                    AsyncImage(
                        model = ImageRequest.Builder(context).apply {
                            data(uri)
                        }.build(),
                        contentDescription = "Видео",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
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
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}