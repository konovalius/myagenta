package com.example.myagent.ui.folders.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    media: Media,
    folderUuid: String,
    index: Int,
    onOpenPhoto: (Uri, String, Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onClickOverride: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val context = LocalContext.current
    val uri = Uri.parse(media.uri)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    if (onClickOverride != null) {
                        onClickOverride()
                    } else if (media.type == "video") {
                        onOpenVideo(uri)
                    } else {
                        onOpenPhoto(uri, folderUuid, index)
                    }
                }
            )
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

        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0x33FF3B30) else Color(0x00000000)),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFF3B30) else Color(0x66000000))
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}