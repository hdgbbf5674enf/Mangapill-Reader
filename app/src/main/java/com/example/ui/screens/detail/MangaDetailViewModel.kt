package com.example.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.model.ChapterItem
import com.example.data.model.MangaItem
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MangaDetailUiState {
    object Loading : MangaDetailUiState()
    data class Success(
        val manga: MangaItem,
        val chapters: List<ChapterItem>,
        val filteredChapters: List<ChapterItem>,
        val isBookmarked: Boolean,
        val readingHistory: HistoryEntity?,
        val isAscending: Boolean = false,
        val chapterSearchQuery: String = ""
    ) : MangaDetailUiState()
    data class Error(val message: String) : MangaDetailUiState()
}

class MangaDetailViewModel(
    private val repository: MangaRepository,
    private val mangaId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaDetailUiState>(MangaDetailUiState.Loading)
    val uiState: StateFlow<MangaDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetailData()
    }

    fun loadDetailData() {
        viewModelScope.launch {
            _uiState.value = MangaDetailUiState.Loading
            try {
                val manga = repository.getMangaDetails(mangaId)
                if (manga == null) {
                    _uiState.value = MangaDetailUiState.Error("Manga details not found")
                    return@launch
                }

                val chaptersList = repository.getChapters(manga.displayTitle, manga.chapters)

                // Observe bookmark state and history
                combine(
                    repository.isBookmarked(mangaId),
                    repository.getHistoryForManga(mangaId)
                ) { isBookmarked, history ->
                    Pair(isBookmarked, history)
                }.collect { (isBookmarked, history) ->
                    val currentState = _uiState.value
                    if (currentState is MangaDetailUiState.Success) {
                        _uiState.value = currentState.copy(
                            isBookmarked = isBookmarked,
                            readingHistory = history
                        )
                    } else {
                        _uiState.value = MangaDetailUiState.Success(
                            manga = manga,
                            chapters = chaptersList,
                            filteredChapters = chaptersList,
                            isBookmarked = isBookmarked,
                            readingHistory = history,
                            isAscending = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MangaDetailUiState.Error(e.message ?: "Error loading manga details")
            }
        }
    }

    fun toggleBookmark() {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.toggleBookmark(currentState.manga, currentState.isBookmarked)
        }
    }

    fun filterChapters(query: String) {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        val filtered = filterAndSort(currentState.chapters, query, currentState.isAscending)
        _uiState.value = currentState.copy(
            chapterSearchQuery = query,
            filteredChapters = filtered
        )
    }

    fun toggleSortOrder() {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        val newAsc = !currentState.isAscending
        val filtered = filterAndSort(currentState.chapters, currentState.chapterSearchQuery, newAsc)
        _uiState.value = currentState.copy(
            isAscending = newAsc,
            filteredChapters = filtered
        )
    }

    private fun filterAndSort(chapters: List<ChapterItem>, query: String, ascending: Boolean): List<ChapterItem> {
        var list = if (query.isBlank()) {
            chapters
        } else {
            chapters.filter { it.title.contains(query, ignoreCase = true) || it.chapterNumber.contains(query) }
        }

        return if (ascending) {
            list.reversed()
        } else {
            list
        }
    }
}
