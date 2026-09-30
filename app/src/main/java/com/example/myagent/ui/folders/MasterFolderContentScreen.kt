package com.example.myagent.ui.folders

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.ui.folders.components.MediaGridItem
import com.example.myagent.ui.theme.GradientBackground
import com.example.myagent.ui.theme.GoshaSans

@Composable
fun MasterFolderContentScreen(
    onBack: () -> Unit,
    onOpenMedia: (Uri, String, Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    viewModel: MasterFolderContentViewModel = hiltViewModel()
) {
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = folder?.name ?: "Папка",
                color = Color.White,
                fontFamily = GoshaSans,
                fontSize = 30.sp,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Меню папки",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Переименовать", fontFamily = GoshaSans) },
                        onClick = {
                            menuExpanded = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить", fontFamily = GoshaSans) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Свойства", color = Color.Gray, fontFamily = GoshaSans) },
                        onClick = { menuExpanded = false }
                    )
                }
            }
        }

        if (media.isEmpty()) {
Text(
                text = "Медиа пока нет",
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = GoshaSans,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(media, key = { _, media -> media.uuid }) { index, media ->
                    MediaGridItem(
                        media = media,
                        folderUuid = folder?.uuid ?: "",
                        index = index,
                        onOpenPhoto = { uri, folderUuid, idx ->
                            onOpenMedia(uri, folderUuid, idx)
                        },
                        onOpenVideo = onOpenVideo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameFolderDialog(
            currentName = folder?.name.orEmpty(),
            onConfirm = { name ->
                folder?.let { viewModel.renameFolder(it, name) }
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        folder?.let { target ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить папку?", fontFamily = GoshaSans) },
                text = { Text("Папка «${target.name}» и все фото в ней будут удалены.", fontFamily = GoshaSans) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFolderWithMedia(target)
                        showDeleteDialog = false
                        onBack()
                    }) {
                        Text("Да", fontFamily = GoshaSans)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Нет", fontFamily = GoshaSans)
                    }
                }
            )
        }
    }
}

@Composable
fun RenameFolderDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать папку", fontFamily = GoshaSans) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text("Название папки", fontFamily = GoshaSans) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) }
            ) {
                Text("Сохранить", fontFamily = GoshaSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = GoshaSans)
            }
        }
    )
}

@Composable
fun PickFolderDialog(
    folders: List<MasterFolder>,
    onPick: (MasterFolder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите папку", fontFamily = GoshaSans) },
        text = {
            Column {
                folders.forEach { item ->
                    Text(
                        text = item.name,
                        fontFamily = GoshaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(item) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = GoshaSans)
            }
        }
    )
}