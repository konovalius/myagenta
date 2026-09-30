package com.example.myagent.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import com.example.myagent.data.db.entity.Media

@Composable
fun MediaViewerScreen(
    uri: Uri,
    media: List<Media>,
    startIndex: Int,
    onBack: () -> Unit,
    onDelete: (Uri) -> Unit,
    onUseMedia: ((Uri) -> Unit)? = null,
    onSaveMedia: (() -> Unit)? = null
) {
    val uris = remember(media, uri) {
        if (media.isEmpty()) listOf(uri) else media.map { Uri.parse(it.uri) }
    }
    val total = uris.size
    val initialIndex = startIndex.coerceIn(0, total - 1)
    val looping = total > 1
    val startPage = if (looping) {
        val half = Int.MAX_VALUE / 2
        half - (half % total) + initialIndex
    } else {
        0
    }

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { if (looping) Int.MAX_VALUE else 1 }
    )
    val currentIndex = if (looping) pagerState.currentPage % total else 0
    val currentUri = uris[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (total > 1) {
                    Text(
                        text = "${currentIndex + 1} из $total",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.Black)
            ) {
                if (looping) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = uris[page % total],
                            contentDescription = "Снимок",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AsyncImage(
                        model = currentUri,
                        contentDescription = "Снимок",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                // Верхняя строка с кнопками save, back, delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка save
                    onSaveMedia?.let { saveMedia ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(onClick = saveMedia)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "save",
                                style = TextStyle(
                                    color = Color(0xFF0D3B14),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    } ?: Box(modifier = Modifier)

                    // Кнопка back
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("back", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }

                    // Кнопка delete
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onDelete(currentUri) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "delete",
                            style = TextStyle(
                                color = Color(0xFF8C1D18),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                // Кнопка use (без изменений)
                onUseMedia?.let { useMedia ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable { useMedia(currentUri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "use",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}