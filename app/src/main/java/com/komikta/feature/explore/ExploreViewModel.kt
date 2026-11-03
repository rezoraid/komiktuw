package com.komikta.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.core.network.ApiConfig
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.Comic
import com.komikta.domain.model.SearchParams
import com.komikta.domain.usecase.BrowseComicsUseCase
import com.komikta.domain.usecase.GetGenresUseCase
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

internal const val NO_COMICS_MESSAGE = "Tidak ada komik untuk filter ini"

data class ExploreUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val comics: List<Comic> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val genres: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val type: String? = null,
    val sort: String? = null,
    val status: String? = null
)

private fun String.titleCase(): String =
    trim().split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

private val CURATED_GENRES = listOf(
    "Action", "Adaptation", "Adult", "Adventure", "Comedy", "Cooking", "Crime", "Drama",
    "Ecchi", "Fantasy", "Harem", "Historical", "Horror", "Isekai", "Martial Arts", "Mature",
    "Mystery", "Psychological", "Reincarnation", "Revenge", "Romance", "School Life", "Sci-Fi",
    "Seinen", "Shoujo", "Shounen", "Slice of Life", "Sports", "Supernatural", "Thriller", "Tragedy"
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val searchComics: SearchComicsUseCase,
    private val browseComics: BrowseComicsUseCase,
    private val getGenres: GetGenresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var listJob: Job? = null
    private var moreJob: Job? = null

    init {
        loadGenres()
        loadInitial()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            getGenres.observe(ApiConfig.DEFAULT_SOURCE).collect { result ->
                val remote = (result as? Result.Success)?.data.orEmpty()
                val keys = remote.map { it.trim().lowercase() }.toSet()
                val curated = if (remote.isEmpty() || CURATED_GENRES.any { it.lowercase() in keys }) {
                    CURATED_GENRES
                } else {
                    remote.take(30).map { it.titleCase() }
                }
                _uiState.update { it.copy(genres = curated) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        moreJob?.cancel()
        listJob?.cancel()
        listJob = viewModelScope.launch {
            if (query.isBlank()) {
                runInitial(refresh = false)
            } else {
                delay(SEARCH_DEBOUNCE_MS)
                runSearch(query.trim())
            }
        }
    }

    private fun browseParams(state: ExploreUiState, page: Int) = BrowseParams(
        source = ApiConfig.DEFAULT_SOURCE,
        page = page,
        type = state.type,
        status = state.status,
        sort = state.sort,
        genres = state.selectedGenres.takeIf { it.isNotEmpty() }?.joinToString(",") { it.lowercase() }
    )

    private fun filtersChanged(old: ExploreUiState, new: ExploreUiState) =
        old.selectedGenres != new.selectedGenres || old.type != new.type ||
            old.sort != new.sort || old.status != new.status

    fun applyFilters(genres: Set<String>, sort: String?, status: String?) {
        val old = _uiState.value
        val new = old.copy(selectedGenres = genres, sort = sort, status = status, query = "")
        _uiState.value = new
        if (filtersChanged(old, new) || old.query.isNotBlank()) loadInitial()
    }

    fun setType(type: String?) {
        val old = _uiState.value
        if (old.type == type) return
        _uiState.value = old.copy(type = type, query = "")
        loadInitial()
    }

    fun resetAll() {
        _uiState.update { it.copy(query = "", selectedGenres = emptySet(), sort = null, status = null) }
        loadInitial()
    }

    fun retry() {
        val q = _uiState.value.query
        moreJob?.cancel()
        listJob?.cancel()
        listJob = viewModelScope.launch { if (q.isNotBlank()) runSearch(q.trim()) else runInitial(refresh = true) }
    }

    private fun loadInitial(refresh: Boolean = false) {
        moreJob?.cancel()
        listJob?.cancel()
        listJob = viewModelScope.launch { runInitial(refresh) }
    }

    private suspend fun runInitial(refresh: Boolean) {
        val snapshot = _uiState.value
        _uiState.update {
            it.copy(isLoading = true, isLoadingMore = false, errorMessage = null, page = 1, canLoadMore = true, comics = emptyList())
        }
        browseComics.observe(browseParams(snapshot, 1), refresh).collect { result -> applyFirstPage(result, true) }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isLoading = it.comics.isEmpty(), isLoadingMore = false, errorMessage = null, canLoadMore = false) }
        searchComics.observe(SearchParams(source = ApiConfig.DEFAULT_SOURCE, query = query, page = 1))
            .collect { result -> applyFirstPage(result, false) }
    }

    private fun applyFirstPage(result: Result<List<Comic>>, pageable: Boolean) {
        _uiState.update { st ->
            when (result) {
                is Result.Success -> st.copy(
                    isLoading = false,
                    comics = result.data.distinctBy { it.slug },
                    page = 1,
                    canLoadMore = pageable && result.data.isNotEmpty(),
                    errorMessage = null
                )
                is Result.Empty -> st.copy(isLoading = false, comics = emptyList(), canLoadMore = false, errorMessage = NO_COMICS_MESSAGE)
                is Result.Error -> if (st.comics.isEmpty()) st.copy(isLoading = false, errorMessage = result.message) else st.copy(isLoading = false)
                else -> st.copy(isLoading = false)
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || state.query.isNotBlank()) return
        val next = state.page + 1
        moreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = browseComics(browseParams(state, next))
            val batch = (result as? Result.Success)?.data.orEmpty()
            _uiState.update {
                if (filtersChanged(state, it) || it.query.isNotBlank()) it.copy(isLoadingMore = false) else {
                    val merged = (it.comics + batch).distinctBy { c -> c.slug }
                    it.copy(
                        isLoadingMore = false,
                        comics = merged,
                        page = next,
                        canLoadMore = batch.isNotEmpty() && merged.size > it.comics.size,
                        errorMessage = if (merged.isEmpty()) (result as? Result.Error)?.message else null
                    )
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 280L
    }
}
