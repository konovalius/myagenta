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
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderViewModel @Inject constructor(
    private val repository: MasterFolderRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<MasterFolder>>(emptyList())
    val folders: StateFlow<List<MasterFolder>> = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            val source: Flow<List<MasterFolder>> = repository.getAll()
            source.collect { list -> _folders.value = list }
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