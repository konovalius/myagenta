package com.example.myagent.ui.media

import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.db.entity.Subfolder
import com.example.myagent.ui.common.DeleteModeSwitch
import com.example.myagent.ui.folders.GridItem
import com.example.myagent.ui.folders.components.MediaGridItem
import com.example.myagent.ui.folders.components.SubfolderGridItem
import com.example.myagent.ui.theme.GoshaSans

@Composable
fun AllMediaScreen(
    onBackClick: () -> Unit,
    onOpenVideo: (Uri) -> Unit,
    onOpenPhoto: (Uri, Int) -> Unit
) {
    val viewModel: AllMediaViewModel = hiltViewModel()
    val mediaList = viewModel.unassignedMedia.collectAsState().value
    val subfolders = viewModel.subfolders.collectAsState().value
    val context = LocalContext.current

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (mediaToDelete, setMediaToDelete) = remember { mutableStateOf<Media?>(null) }
    var deleteMode by remember { mutableStateOf(false) }
    var isMergeMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeFolderName by remember { mutableStateOf("") }
    var expandedSubfolderUuid by remember { mutableStateOf<String?>(null) }
    var subfolderToDelete by remember { mutableStateOf<Subfolder?>(null) }
    var subfolderDeleteCount by remember { mutableStateOf(0) }

    // Группируем медиа: одиночные (subfolderUuid == null) и по подпапкам
    val singleMedia = remember(mediaList) { mediaList.filter { it.subfolderUuid == null } }
    val mediaBySubfolder = remember(mediaList) {
        mediaList
            .filter { it.subfolderUuid != null }
            .groupBy { it.subfolderUuid!! }
    }
    val indexByUuid = remember(mediaList) {
        mediaList.withIndex().associate { (index, item) -> item.uuid to index }
    }

    // Ряды по 3 ячейки; развёрнутая подпапка добавляет свои кадры отдельными рядами
    val gridRows = remember(
        singleMedia,
        mediaBySubfolder,
        subfolders,
        indexByUuid,
        expandedSubfolderUuid
    ) {
        // Единый список: одиночные медиа и подпапки вперемешку, по дате (новые сверху).
        // Дата подпапки — последний кадр в ней, у пустой — дата создания подпапки.
        val dated = mutableListOf<Pair<Long, GridItem>>()
        singleMedia.forEach { media ->
            dated.add(media.createdAt to GridItem.Single(media, indexByUuid[media.uuid] ?: 0))
        }
        subfolders.forEach { subfolder ->
            val subfolderMedia = mediaBySubfolder[subfolder.uuid] ?: emptyList()
            val sortDate = subfolderMedia.maxOfOrNull { it.createdAt } ?: subfolder.createdAt
            dated.add(
                sortDate to GridItem.SubfolderItem(
                    subfolder,
                    subfolderMedia.maxByOrNull { it.createdAt }
                )
            )
        }
        val base = dated.sortedByDescending { it.first }.map { it.second }

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
                        text = "Нет несортированных медиа",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
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
                                            val media = item.media
                                            MediaGridItem(
                                                media = media,
                                                folderUuid = "",
                                                index = item.index,
                                                onOpenPhoto = { uri, _, index -> onOpenPhoto(uri, index) },
                                                onOpenVideo = onOpenVideo,
                                                modifier = Modifier.fillMaxSize(),
                                                selectionMode = isMergeMode,
                                                isSelected = media.uuid in selectedIds,
                                                onLongClick = {
                                                    setMediaToDelete(media)
                                                    setShowDeleteDialog(true)
                                                },
                                                onClickOverride = when {
                                                    isMergeMode -> {
                                                        {
                                                            selectedIds = if (media.uuid in selectedIds) {
                                                                selectedIds - media.uuid
                                                            } else {
                                                                selectedIds + media.uuid
                                                            }
                                                        }
                                                    }
                                                    deleteMode -> {
                                                        {
                                                            // Удаление сразу без диалога
                                                            viewModel.deleteMedia(media)
                                                            try {
                                                                context.contentResolver.delete(Uri.parse(media.uri), null, null)
                                                            } catch (e: Exception) {
                                                                Log.e("AllMedia", "Ошибка удаления файла из галереи", e)
                                                            }
                                                            // Показываем Toast
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
                        if (selectedIds.isNotEmpty()) {
                            mergeFolderName = viewModel.defaultFolderName()
                            showMergeDialog = true
                        }
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
            title = { Text("Удалить подпапку?", color = Color.White) },
            text = {
                Text(
                    text = "В подпапке $subfolderDeleteCount файлов. Что с ними делать?",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Row {
                    Button(
                        onClick = {
                            viewModel.deleteSubfolderWithMedia(target)
                            subfolderToDelete = null
                            Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Удалить всё")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.deleteSubfolderKeepMedia(target)
                            subfolderToDelete = null
                            Toast.makeText(context, "Фото остались", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A2C4A),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Оставить фото")
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { subfolderToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A2C4A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Создать новую мастер-папку?", color = Color.White, fontFamily = GoshaSans) },
            text = {
                OutlinedTextField(
                    value = mergeFolderName,
                    onValueChange = { if (it.length <= 50) mergeFolderName = it },
                    label = { Text("Название папки", fontFamily = GoshaSans) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White.copy(alpha = 0.7f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFFFF3B30),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color(0xFFFF3B30)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.mergeSelected(
                            selectedIds = selectedIds.toList(),
                            createMasterFolder = true,
                            masterFolderName = mergeFolderName
                        )
                        showMergeDialog = false
                        selectedIds = emptySet()
                        isMergeMode = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    Text("Да", fontFamily = GoshaSans)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.mergeSelected(
                            selectedIds = selectedIds.toList(),
                            createMasterFolder = false,
                            masterFolderName = ""
                        )
                        showMergeDialog = false
                        selectedIds = emptySet()
                        isMergeMode = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A2C4A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Нет", fontFamily = GoshaSans)
                }
            }
        )
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