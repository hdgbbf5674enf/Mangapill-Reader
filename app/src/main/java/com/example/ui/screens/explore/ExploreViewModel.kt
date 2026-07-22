package com.example.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MangaItem
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(
        val trendingManga: List<MangaItem>,
        val popularManga: List<MangaItem>,
        val selectedGenre: String? = null
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

class ExploreViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    val genresList = listOf("All", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Supernatural")

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading
            try {
                val trending = repository.getTrendingManga()
                val popular = repository.getPopularManga()
                if (trending.isEmpty() && popular.isEmpty()) {
                    _uiState.value = ExploreUiState.Error("Unable to load manga. Check internet connection.")
                } else {
                    _uiState.value = ExploreUiState.Success(
                        trendingManga = trending,
                        popularManga = popular
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error(e.message ?: "Failed to load manga")
            }
        }
    }
}
