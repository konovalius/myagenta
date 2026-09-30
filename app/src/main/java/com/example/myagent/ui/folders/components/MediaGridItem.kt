package com.example.myagent.ui.folders.components

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
import coil.request.ImageRequest
import com.example.myagent.R
import com.example.myagent.data.db.entity.Media

@Composable
fun MediaGridItem(
    media: Media,
    folderUuid: String,
    index: Int,
    onOpenPhoto: (Uri, String, Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onClickOverride: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uri = Uri.parse(media.uri)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (onClickOverride != null) {
                    onClickOverride()
                } else if (media.type == "video") {
                    onOpenVideo(uri)
                } else {
                    onOpenPhoto(uri, folderUuid, index)
                }
            }
    ) {
        if (media.type == "video") {
            // Для видео используем превью с первого кадра
AsyncImage(
                 model = ImageRequest.Builder(context).apply {
                     data(uri)
                     // Для Coil 2.x с coil-video видео превью должно работать автоматически
                     // через VideoFrameDecoder, зарегистрированный в MyAgentApp
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
                 contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                 modifier = Modifier.fillMaxSize()
             )
        }
    }
}