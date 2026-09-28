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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myagent.data.db.entity.Photo

private val DisketteShape = GenericShape { size, _ ->
    val cut = size.width * 0.25f
    moveTo(0f, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

private val TrashShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val handleBottom = h * 0.1f
    val handleW = w * 0.4f
    val handleLeft = (w - handleW) / 2f
    val rimBottom = h * 0.22f
    val bodyBottom = h
    val bodyInset = w * 0.13f

    moveTo(handleLeft, 0f)
    lineTo(handleLeft + handleW, 0f)
    lineTo(handleLeft + handleW, handleBottom)
    lineTo(handleLeft, handleBottom)
    close()

    moveTo(0f, handleBottom)
    lineTo(w, handleBottom)
    lineTo(w, rimBottom)
    lineTo(0f, rimBottom)
    close()

    moveTo(0f, rimBottom)
    lineTo(w, rimBottom)
    lineTo(w - bodyInset, bodyBottom)
    lineTo(bodyInset, bodyBottom)
    close()
}

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    photos: List<Photo>,
    startIndex: Int,
    onBack: () -> Unit,
    onDelete: (Uri) -> Unit,
    onUsePhoto: ((Uri) -> Unit)? = null,
    onSavePhoto: (() -> Unit)? = null
) {
    val uris = remember(photos, uri) {
        if (photos.isEmpty()) listOf(uri) else photos.map { Uri.parse(it.uri) }
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

    val contentAreaCenterY = 680.dp
    val useCenterY = contentAreaCenterY + 12.dp
    val useToDelete = 94.dp
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
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.offset(y = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("back", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    onUsePhoto?.let { usePhoto ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .offset(x = (-4).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clickable { usePhoto(currentUri) },
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFB3261E), TrashShape)
                            .clickable { onDelete(currentUri) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "delete",
                            color = Color(0xFFB3261E),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                onSavePhoto?.let { savePhoto ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .offset(y = useCenterY - useToDelete - contentAreaCenterY)
                                .border(1.dp, Color(0xFF1B5E20), DisketteShape)
                                .clickable(onClick = savePhoto),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "save",
                                color = Color(0xFF1B5E20),
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
