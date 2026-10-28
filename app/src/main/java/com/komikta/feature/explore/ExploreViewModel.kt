package com.komikta.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.core.network.ApiConfig
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.Comic
import com.komikta.domain.model.SearchParams
import com.komikta.domain.usecase.BrowseComicsUseCase
import com.komikta.domain.usecase.SearchComicsUseCase
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
    val isLoadingMore: Boolean = false,
    val comics: List<Comic> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = true,
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
        loadInitial()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            if (query.isBlank()) loadInitial() else doSearch(query)
        }
    }

    /** Load several pages at once so Explore feels complete */
    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, page = 1, canLoadMore = true)
            }
            val source = ApiConfig.DEFAULT_SOURCE
            val result = browseComics(BrowseParams(source = source, page = 1))
            val firstPage = (result as? Result.Success)?.data.orEmpty().distinctBy { it.slug }
            val error = (result as? Result.Error)?.message
            _uiState.update {
                it.copy(
                    isLoading = false,
                    comics = firstPage,
                    page = 1,
                    canLoadMore = firstPage.isNotEmpty(),
                    errorMessage = if (firstPage.isEmpty()) error else null
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || state.query.isNotBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val next = state.page + 1
            val source = ApiConfig.DEFAULT_SOURCE
            val result = browseComics(BrowseParams(source = source, page = next))
            val more = (result as? Result.Success)?.data.orEmpty()
            _uiState.update {
                val merged = (it.comics + more).distinctBy { c -> c.slug }
                it.copy(
                    isLoadingMore = false,
                    comics = merged,
                    page = next,
                    canLoadMore = more.isNotEmpty(),
                    errorMessage = if (merged.isEmpty()) (result as? Result.Error)?.message else null
                )
            }
        }
    }

    private fun doSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canLoadMore = false) }
            val source = ApiConfig.DEFAULT_SOURCE
            val result = searchComics(SearchParams(source = source, query = query, page = 1))
            val all = (result as? Result.Success)?.data.orEmpty().distinctBy { it.slug }
            val error = (result as? Result.Error)?.message
            _uiState.update {
                it.copy(
                    isLoading = false,
                    comics = all,
                    errorMessage = if (all.isEmpty()) error else null
                )
            }
        }
    }
}
