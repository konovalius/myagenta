package com.example.myagent.ui.folders

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.focus.FocusState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.ui.theme.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MasterFoldersScreen(
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    viewModel: MasterFolderViewModel = hiltViewModel()
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<MasterFolder?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = "Мастер-папки",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery.value,
                        onValueChange = { viewModel.searchQuery.value = it },
                        label = null,
                        placeholder = { Text("Поиск по названию", color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Поиск", tint = Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = {
                            if (viewModel.searchQuery.value.isNotBlank()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Очистить", tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF3B30),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            val filteredFolders by viewModel.filteredFolders.collectAsStateWithLifecycle()
            if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (viewModel.searchQuery.value.isBlank()) "Мастер-папок пока нет" else "Ничего не найдено",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 88.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFolders, key = { it.uuid }) { folder ->
                        MasterFolderCard(
                            folder = folder,
                            onClick = { onOpenFolder(folder.uuid) },
                            onLongClick = { folderToDelete = folder }
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Создать папку")
                }
            }
        }
    }

        if (showCreateDialog) {
            CreateFolderDialog(
                onConfirm = { name ->
                    viewModel.createFolder(name)
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false }
            )
        }

        folderToDelete?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = { Text("Удалить папку?") },
                text = { Text("Папка «${folder.name}» будет удалена.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFolder(folder)
                        folderToDelete = null
                    }) {
                        Text("Да")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text("Нет")
                    }
                }
            )
        }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать папку") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название папки") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MasterFolderCard(
    folder: MasterFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, typeLabel) = if (folder.type == "geo") {
        Icons.Filled.Map to "Гео"
    } else {
        Icons.Filled.Folder to "Объект"
    }
    val dateText = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        .format(Date(folder.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = typeLabel, tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(folder.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            Spacer(Modifier.size(4.dp))
            Text(
                text = "$typeLabel • $dateText",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}