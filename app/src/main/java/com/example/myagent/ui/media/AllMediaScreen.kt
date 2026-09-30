package com.example.myagent.ui.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.myagent.R
import com.example.myagent.data.db.entity.Media
import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.myagent.ui.common.DeleteModeSwitch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllMediaScreen(
    onBackClick: () -> Unit,
    onOpenVideo: (Uri) -> Unit,
    onOpenPhoto: (Uri) -> Unit
) {
    val viewModel: AllMediaViewModel = hiltViewModel()
    val mediaList = viewModel.unassignedMedia.collectAsState().value
    val context = LocalContext.current

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (mediaToDelete, setMediaToDelete) = remember { mutableStateOf<Media?>(null) }
    var deleteMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1428),
                        Color(0xFF1A2C4A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Не сортированное",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                DeleteModeSwitch(
                    checked = deleteMode,
                    onCheckedChange = { deleteMode = it },
                    contentDescription = "Режим удаления медиа"
                )
            }

            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет несортированных медиа",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaList, key = { it.uuid }) { media ->
                        Log.wtf("AllMedia", "Item: uri=${media.uri}, type='${media.type}', isVideo=${media.type == "video"}")
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (deleteMode) {
                                            setMediaToDelete(media)
                                            setShowDeleteDialog(true)
                                        } else {
                                            val uri = Uri.parse(media.uri)
                                            if (media.type == "video") {
                                                onOpenVideo(uri)
                                            } else {
                                                onOpenPhoto(uri)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        setMediaToDelete(media)
                                        setShowDeleteDialog(true)
                                    }
                                )
                        ) {
if (media.type == "video") {
                                 AsyncImage(
                                     model = Uri.parse(media.uri),
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
                                 AsyncImage(
                                     model = Uri.parse(media.uri),
                                     contentDescription = "Фото",
                                     contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                     modifier = Modifier.fillMaxSize()
                                 )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { setShowDeleteDialog(false) },
            title = { Text("Удалить медиа?", color = Color.White) },
            text = { Text("Медиа будет удалено из приложения и из галереи устройства.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        mediaToDelete?.let { media ->
                            viewModel.deleteMedia(media)
                            try {
                                val uri = Uri.parse(media.uri)
                                context.contentResolver.delete(uri, null, null)
                            } catch (e: Exception) {
                                Log.e("AllMedia", "Ошибка удаления файла из галереи", e)
                            }
                        }
                        setShowDeleteDialog(false)
                        setMediaToDelete(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        setShowDeleteDialog(false)
                        setMediaToDelete(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A2C4A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Нет")
                }
            }
        )
    }
}