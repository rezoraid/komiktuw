package com.komikta.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.core.network.ApiConfig
import com.komikta.domain.model.Comic
import com.komikta.domain.model.SearchParams
import com.komikta.domain.usecase.BrowseComicsUseCase
import com.komikta.domain.usecase.SearchComicsUseCase
import com.komikta.domain.model.BrowseParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val comics: List<Comic> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val searchComics: SearchComicsUseCase,
    private val browseComics: BrowseComicsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadBrowse()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            if (query.isBlank()) {
                loadBrowse()
            } else {
                doSearch(query)
            }
        }
    }

    private fun loadBrowse() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = browseComics(BrowseParams(source = ApiConfig.DEFAULT_SOURCE, page = 1))) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, comics = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Result.Empty -> _uiState.update {
                    it.copy(isLoading = false, comics = emptyList())
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun doSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = searchComics(SearchParams(source = ApiConfig.DEFAULT_SOURCE, query = query))) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, comics = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Result.Empty -> _uiState.update {
                    it.copy(isLoading = false, comics = emptyList())
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
