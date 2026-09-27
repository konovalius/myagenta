package com.example.myagent.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.repository.MasterFolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderViewModel @Inject constructor(
    private val repository: MasterFolderRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val _allFolders = MutableStateFlow<List<MasterFolder>>(emptyList())
    val filteredFolders: StateFlow<List<MasterFolder>> =
        combine(_allFolders, searchQuery) { folders, query ->
            if (query.isBlank()) folders else folders.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch {
            val source: Flow<List<MasterFolder>> = repository.getAll()
            source.collect { list -> _allFolders.value = list }
        }
    }

    fun createFolder(name: String) {
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            type = "object",
            createdAt = System.currentTimeMillis(),
            lat = null,
            lon = null
        )
        viewModelScope.launch { repository.insert(folder) }
    }

    fun deleteFolder(folder: MasterFolder) {
        viewModelScope.launch { repository.delete(folder) }
    }
}