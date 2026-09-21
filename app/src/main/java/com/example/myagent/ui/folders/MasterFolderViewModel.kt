package com.example.myagent.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.repository.MasterFolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderViewModel @Inject constructor(
    private val repository: MasterFolderRepository
) : ViewModel() {

    val folders: StateFlow<List<MasterFolder>> = repository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

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