package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MangaItem
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<MangaItem>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val genresList = listOf("All", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Supernatural")

    private var searchJob: Job? = null

    init {
        // Load initial popular manga as search starting point
        performSearch("", "All")
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        triggerDebouncedSearch()
    }

    fun onGenreSelect(genre: String) {
        _selectedGenre.value = genre
        performSearch(_query.value, genre)
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce delay
            performSearch(_query.value, _selectedGenre.value)
        }
    }

    fun performSearch(q: String, genre: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.searchManga(
                    query = q.takeIf { it.isNotBlank() },
                    genre = genre.takeIf { it != "All" }
                )
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Failed to search")
            }
        }
    }
}
