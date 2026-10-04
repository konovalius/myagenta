package com.example.myagent.ui.folders

import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.db.entity.Subfolder
import com.example.myagent.ui.common.DeleteModeSwitch
import com.example.myagent.ui.folders.components.MediaGridItem
import com.example.myagent.ui.folders.components.SubfolderGridItem
import com.example.myagent.ui.theme.GradientBackground
import com.example.myagent.ui.theme.GoshaSans

sealed interface GridItem {
    data class Single(val media: Media, val index: Int) : GridItem
    data class SubfolderItem(val subfolder: Subfolder, val lastMedia: Media?) : GridItem
}

@Composable
fun MasterFolderContentScreen(
    onBack: () -> Unit,
    onOpenMedia: (Uri, String, Int) -> Unit,
    onOpenVideo: (Uri) -> Unit,
    viewModel: MasterFolderContentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val subfolders by viewModel.subfolders.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf(false) }
    var isMergeMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var mediaToDelete by remember { mutableStateOf<Media?>(null) }
    var showMediaDeleteDialog by remember { mutableStateOf(false) }
    var expandedSubfolderUuid by remember { mutableStateOf<String?>(null) }
    var subfolderToDelete by remember { mutableStateOf<Subfolder?>(null) }
    var subfolderDeleteCount by remember { mutableStateOf(0) }

    // Группируем медиа: одиночные (subfolderUuid == null) и по подпапкам
    val singleMedia = remember(media) { media.filter { it.subfolderUuid == null } }
    val mediaBySubfolder = remember(media) {
        media
            .filter { it.subfolderUuid != null }
            .groupBy { it.subfolderUuid!! }
    }
    // Индекс каждого медиа в общем списке папки — для открытия в просмотрщике
    val indexByUuid = remember(media) {
        media.withIndex().associate { (index, item) -> item.uuid to index }
    }

    // Ряды по 3 ячейки; развёрнутая подпапка добавляет свои кадры отдельными рядами
    val gridRows = remember(
        singleMedia,
        mediaBySubfolder,
        subfolders,
        indexByUuid,
        expandedSubfolderUuid
    ) {
        val base = mutableListOf<GridItem>()
        singleMedia.forEach { base.add(GridItem.Single(it, indexByUuid[it.uuid] ?: 0)) }
        subfolders.forEach { subfolder ->
            val subfolderMedia = mediaBySubfolder[subfolder.uuid] ?: emptyList()
            base.add(GridItem.SubfolderItem(subfolder, subfolderMedia.maxByOrNull { it.createdAt }))
        }

        val result = mutableListOf<List<GridItem>>()
        var current = mutableListOf<GridItem>()
        fun flush() {
            if (current.isNotEmpty()) {
                result.add(current)
                current = mutableListOf()
            }
        }

        base.forEach { item ->
            current.add(item)
            if (current.size == 3) flush()
            val isExpanded = item is GridItem.SubfolderItem &&
                item.subfolder.uuid == expandedSubfolderUuid
            if (isExpanded) {
                flush()
                val inside = (mediaBySubfolder[item.subfolder.uuid] ?: emptyList())
                    .sortedBy { it.createdAt }
                    .map { GridItem.Single(it, indexByUuid[it.uuid] ?: 0) }
                inside.chunked(3).forEach { chunk -> result.add(chunk) }
            }
        }
        flush()
        result
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    fontSize = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeleteModeSwitch(
                    checked = isMergeMode,
                    onCheckedChange = {
                        deleteMode = false
                        isMergeMode = it
                        if (!it) selectedIds = emptySet()
                    },
                    contentDescription = "Режим объединения медиа",
                    icon = Icons.AutoMirrored.Outlined.MergeType
                )
                Spacer(Modifier.width(16.dp))
                DeleteModeSwitch(
                    checked = deleteMode,
                    onCheckedChange = {
                        isMergeMode = false
                        selectedIds = emptySet()
                        deleteMode = it
                    },
                    contentDescription = "Режим удаления медиа"
                )
            }

            if (gridRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Медиа пока нет",
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = GoshaSans,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gridRows, key = { row ->
                        row.joinToString("|") { item ->
                            when (item) {
                                is GridItem.Single -> item.media.uuid
                                is GridItem.SubfolderItem -> "subfolder_${item.subfolder.uuid}"
                            }
                        }
                    }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    when (item) {
                                        is GridItem.Single -> {
                                            MediaGridItem(
                                                media = item.media,
                                                folderUuid = folder?.uuid ?: "",
                                                index = item.index,
                                                onOpenPhoto = { uri, folderUuid, idx ->
                                                    onOpenMedia(uri, folderUuid, idx)
                                                },
                                                onOpenVideo = onOpenVideo,
                                                modifier = Modifier.fillMaxSize(),
                                                selectionMode = isMergeMode,
                                                isSelected = item.media.uuid in selectedIds,
                                                onClickOverride = when {
                                                    isMergeMode -> {
                                                        {
                                                            selectedIds = if (item.media.uuid in selectedIds) {
                                                                selectedIds - item.media.uuid
                                                            } else {
                                                                selectedIds + item.media.uuid
                                                            }
                                                        }
                                                    }
                                                    deleteMode -> {
                                                        {
                                                            viewModel.deleteMedia(item.media)
                                                            Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    else -> null
                                                }
                                            )
                                        }
is GridItem.SubfolderItem -> {
                                             val subfolderMedia = mediaBySubfolder[item.subfolder.uuid] ?: emptyList()
                                             SubfolderGridItem(
                                                 subfolder = item.subfolder,
                                                 lastMedia = item.lastMedia,
                                                 onClick = {
                                                     Log.wtf("Subfolder", "Clicked: ${item.subfolder.uuid}")
                                                     expandedSubfolderUuid =
                                                         if (expandedSubfolderUuid == item.subfolder.uuid) {
                                                             null
                                                         } else {
                                                             item.subfolder.uuid
                                                         }
                                                 },
                                                 onClickOverride = if (deleteMode) {
                                                     {
                                                         if (subfolderMedia.isEmpty()) {
                                                             viewModel.deleteSubfolder(item.subfolder)
                                                             Toast.makeText(context, "Подпапка удалена", Toast.LENGTH_SHORT).show()
                                                         } else {
                                                             subfolderToDelete = item.subfolder
                                                             subfolderDeleteCount = subfolderMedia.size
                                                         }
                                                     }
                                                 } else {
                                                     null
                                                 },
                                                 modifier = Modifier.fillMaxSize()
                                             )
                                         }
                                    }
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (isMergeMode) {
                Button(
                    onClick = {
                        Log.wtf("Merge", "Selected: ${selectedIds.size}")
                        selectedIds = emptySet()
                        isMergeMode = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Готово (${selectedIds.size})",
                        fontFamily = GoshaSans,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    subfolderToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { subfolderToDelete = null },
            title = { Text("Удалить подпапку?", fontFamily = GoshaSans) },
            text = {
                Text(
                    text = "В подпапке $subfolderDeleteCount файлов. Что с ними делать?",
                    fontFamily = GoshaSans
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.deleteSubfolderWithMedia(target)
                        subfolderToDelete = null
                        Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Удалить всё", fontFamily = GoshaSans)
                    }
                    TextButton(onClick = {
                        viewModel.deleteSubfolderKeepMedia(target)
                        subfolderToDelete = null
                        Toast.makeText(context, "Фото остались", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Оставить фото", fontFamily = GoshaSans)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { subfolderToDelete = null }) {
                    Text("Отмена", fontFamily = GoshaSans)
                }
            }
        )
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

    if (showMediaDeleteDialog && mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { showMediaDeleteDialog = false },
            title = { Text("Удалить медиа?", fontFamily = GoshaSans) },
            text = { Text("Медиа будет удалено из папки и из галереи устройства.", fontFamily = GoshaSans) },
            confirmButton = {
                TextButton(onClick = {
                    mediaToDelete?.let { viewModel.deleteMedia(it) }
                    showMediaDeleteDialog = false
                    mediaToDelete = null
                }) {
                    Text("Да", fontFamily = GoshaSans)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMediaDeleteDialog = false
                    mediaToDelete = null
                }) {
                    Text("Нет", fontFamily = GoshaSans)
                }
            }
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