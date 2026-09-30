package com.example.myagent.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AllMediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _unassignedMedia = MutableStateFlow<List<Media>>(emptyList())
    val unassignedMedia: StateFlow<List<Media>> = _unassignedMedia.asStateFlow()

    init {
        viewModelScope.launch {
            val source: Flow<List<Media>> = mediaRepository.getUnassigned()
            source.collect { list -> _unassignedMedia.value = list }
        }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            mediaRepository.delete(media)
        }
    }
}