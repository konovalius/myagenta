package com.example.myagent.ui.folders.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
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
    modifier: Modifier = Modifier,
    onClickOverride: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clickable(onClick = { if (onClickOverride != null) onClickOverride() else onClick() })
    ) {
        // Тень - контур, сдвинутый влево-вниз на 6dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 6.dp, top = 6.dp)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        // Основная ячейка
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
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