package com.example.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.data.model.MangaItem
import com.example.data.model.ReaderBackground
import com.example.data.model.ReaderMode
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val manga: MangaItem,
        val currentChapter: ChapterItem,
        val chaptersList: List<ChapterItem>,
        val pages: List<ChapterPage>,
        val currentPageIndex: Int = 0,
        val readerMode: ReaderMode = ReaderMode.VERTICAL_CONTINUOUS,
        val readerBg: ReaderBackground = ReaderBackground.BLACK
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

class ReaderViewModel(
    private val repository: MangaRepository,
    private val mangaId: Int,
    private val chapterId: String,
    private val chapterTitle: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapterPages()
    }

    fun loadChapterPages() {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val manga = repository.getMangaDetails(mangaId)
                if (manga == null) {
                    _uiState.value = ReaderUiState.Error("Manga not found")
                    return@launch
                }

                val allChapters = repository.getChapters(manga.displayTitle, manga.chapters)
                val activeChapter = allChapters.find { it.id == chapterId || it.title == chapterTitle }
                    ?: ChapterItem(id = chapterId, title = chapterTitle, chapterNumber = "1")

                val pagesList = repository.getChapterPages(activeChapter, manga.displayTitle)

                _uiState.value = ReaderUiState.Success(
                    manga = manga,
                    currentChapter = activeChapter,
                    chaptersList = allChapters,
                    pages = pagesList,
                    currentPageIndex = 0
                )

                // Initial history save
                saveProgress(manga, activeChapter, 1, pagesList.size)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load chapter pages")
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (pageIndex in state.pages.indices && pageIndex != state.currentPageIndex) {
            _uiState.value = state.copy(currentPageIndex = pageIndex)
            saveProgress(state.manga, state.currentChapter, pageIndex + 1, state.pages.size)
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readerMode = mode)
    }

    fun setReaderBackground(bg: ReaderBackground) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readerBg = bg)
    }

    fun navigateChapter(next: Boolean) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        val currentIndex = state.chaptersList.indexOfFirst { it.id == state.currentChapter.id }
        if (currentIndex == -1) return

        val targetIndex = if (next) currentIndex - 1 else currentIndex + 1 // Chapters are usually desc order
        if (targetIndex in state.chaptersList.indices) {
            val newChapter = state.chaptersList[targetIndex]
            viewModelScope.launch {
                _uiState.value = ReaderUiState.Loading
                val pagesList = repository.getChapterPages(newChapter, state.manga.displayTitle)
                _uiState.value = state.copy(
                    currentChapter = newChapter,
                    pages = pagesList,
                    currentPageIndex = 0
                )
                saveProgress(state.manga, newChapter, 1, pagesList.size)
            }
        }
    }

    private fun saveProgress(manga: MangaItem, chapter: ChapterItem, pageNum: Int, totalPages: Int) {
        viewModelScope.launch {
            repository.saveReadingProgress(manga, chapter, pageNum, totalPages)
        }
    }
}
