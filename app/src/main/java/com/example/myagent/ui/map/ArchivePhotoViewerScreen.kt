package com.example.myagent.ui.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myagent.data.util.formatYearLabel
import com.example.myagent.ui.common.ShutterUi
import com.example.myagent.ui.theme.GoshaSans

private const val ARCHIVE_FULL_IMAGE_BASE_URL = "https://img.pastvu.com/d/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivePhotoViewerScreen(
    cid: Long,
    file: String,
    title: String,
    year: Int,
    year2: Int,
    onBack: () -> Unit,
    onUseArchivePhoto: (String) -> Unit,
    viewModel: ArchivePhotoViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val displayTitle = title.ifBlank { "Архивное фото" }
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        when (saveState) {
            ArchiveSaveState.Saved -> Toast.makeText(context, "Сохранено в галерею", Toast.LENGTH_SHORT).show()
            ArchiveSaveState.Error -> Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            else -> return@LaunchedEffect
        }
        viewModel.consumeSaveState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (saveState == ArchiveSaveState.Saving) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.saveToGallery(cid, file) }) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Сохранить"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF101418)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ARCHIVE_FULL_IMAGE_BASE_URL + file)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = ShutterUi.BottomPadding + ShutterUi.Size + 16.dp)
                ) {
                    Text(
                        text = displayTitle,
                        fontFamily = GoshaSans,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    )
                    formatYearLabel(year, year2)?.let { label ->
                        Text(
                            text = label,
                            fontFamily = GoshaSans,
                            fontSize = 16.sp,
                            color = Color(0xFFB0BEC5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ShutterUi.BottomPadding)
                        .size(ShutterUi.Size),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(ShutterUi.Size)
                            .clip(CircleShape)
                            .border(ShutterUi.BorderWidth, Color.White, CircleShape)
                            .clickable { onUseArchivePhoto(ARCHIVE_FULL_IMAGE_BASE_URL + file) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ShutterUi.InnerSize)
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