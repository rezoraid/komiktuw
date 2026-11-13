package com.komikta.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.cache.CatalogBus
import com.komikta.core.common.Result
import com.komikta.data.local.SourcePrefs
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.Comic
import com.komikta.domain.model.fillMissingFrom
import com.komikta.domain.model.isIncomplete
import com.komikta.domain.model.originOf
import com.komikta.domain.model.replacing
import com.komikta.domain.model.uid
import com.komikta.domain.usecase.EnrichComicsUseCase
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

internal const val ALL_TYPES = "all"
internal const val DEFAULT_TYPE = ALL_TYPES
internal val EXPLORE_TYPES = setOf(ALL_TYPES, "manhwa", "manhua", "manga")
private val REAL_TYPES = EXPLORE_TYPES - ALL_TYPES

/** Tab "All" menampilkan Manhwa, Manhua, dan Manga sekaligus; tab lain hanya jenisnya. */
internal fun matchesType(type: String?, tab: String): Boolean =
    if (tab == ALL_TYPES) type == null || originOf(type) in REAL_TYPES else type == null || originOf(type) == tab

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
    val type: String = DEFAULT_TYPE,
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
    private val getGenres: GetGenresUseCase,
    private val enrichComics: EnrichComicsUseCase,
    private val sourcePrefs: SourcePrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var listJob: Job? = null
    private var moreJob: Job? = null
    private var genresJob: Job? = null
    private val enrichTried = HashSet<String>()
    private val extras = HashMap<String, Comic>() // hasil pelengkapan data, dipakai ulang saat daftar dimuat ulang

    private fun withExtras(items: List<Comic>) = items.map { c -> extras[c.uid]?.let { c.fillMissingFrom(it) } ?: c }

    /** Komik yang rating/genre/jenis/chapternya kosong dilengkapi dari detail, lalu kartunya diperbarui. */
    private fun enrich(items: List<Comic>) {
        val todo = items.filter { it.isIncomplete && enrichTried.add(it.uid) }.take(ENRICH_LIMIT)
        if (todo.isEmpty()) return
        viewModelScope.launch {
            enrichComics(todo, ENRICH_LIMIT) { c ->
                extras[c.uid] = c
                _uiState.update { st -> st.copy(comics = st.comics.replacing(c)) }
            }
        }
    }

    init {
        loadGenres()
        loadInitial()
        viewModelScope.launch {
            CatalogBus.events.collect {
                extras.clear()
                val q = _uiState.value.query
                if (q.isBlank()) loadInitial(refresh = true) else retry()
            }
        }
    }

    private fun loadGenres() {
        genresJob?.cancel()
        val source = sourcePrefs.source
        genresJob = viewModelScope.launch {
            getGenres.observe(source).collect { result ->
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
        source = sourcePrefs.source,
        page = page,
        type = state.type.takeIf { it != ALL_TYPES },
        status = state.status,
        sort = state.sort,
        genres = state.selectedGenres.takeIf { it.isNotEmpty() }?.joinToString(",") { it.lowercase() }
    )

    private fun filtersChanged(old: ExploreUiState, new: ExploreUiState) =
        old.selectedGenres != new.selectedGenres || old.type != new.type ||
            old.sort != new.sort || old.status != new.status

    /** Saring daftar yang sedang tampil secara lokal supaya perubahan filter terlihat seketika, sebelum server menjawab. */
    private fun prefilter(list: List<Comic>, st: ExploreUiState): List<Comic> = list.filter { c ->
        matchesType(c.type, st.type) &&
            (st.selectedGenres.isEmpty() || st.selectedGenres.all { g -> c.genres.any { it.equals(g, true) } }) &&
            (st.status == null || c.status == null || c.status.equals(st.status, true))
    }

    fun applyFilters(genres: Set<String>, sort: String?, status: String?) {
        val old = _uiState.value
        var new = old.copy(selectedGenres = genres, sort = sort, status = status, query = "")
        if (filtersChanged(old, new)) new = new.copy(comics = prefilter(old.comics, new))
        _uiState.value = new
        if (filtersChanged(old, new) || old.query.isNotBlank()) loadInitial()
    }

    fun setType(type: String) {
        if (type !in EXPLORE_TYPES) return
        val old = _uiState.value
        if (old.type == type) return
        val next = old.copy(type = type, query = "")
        _uiState.value = next.copy(comics = prefilter(old.comics, next))
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
            // Pertahankan hasil sebelumnya sampai halaman baru tiba agar Explore tidak berkedip
            // menjadi kosong setiap filter berubah atau pengguna kembali ke Explore.
            it.copy(isLoading = it.comics.isEmpty(), isLoadingMore = false, errorMessage = null, page = 1, canLoadMore = true)
        }
        browseComics.observe(browseParams(snapshot, 1), refresh).collect { result -> applyFirstPage(result, true) }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isLoading = it.comics.isEmpty(), isLoadingMore = false, errorMessage = null, canLoadMore = false) }
        searchComics.observe(SearchParams(source = sourcePrefs.source, query = query, page = 1))
            .collect { result -> applyFirstPage(result, false) }
    }

    private fun applyFirstPage(result: Result<List<Comic>>, pageable: Boolean) {
        val data = (result as? Result.Success)?.data?.let(::withExtras)
        if (data != null) enrich(data)
        _uiState.update { st ->
            when (result) {
                is Result.Success -> st.copy(
                    isLoading = false,
                    comics = data.orEmpty().filter { matchesType(it.type, st.type) }.distinctBy { "${it.source}:${it.slug}" },
                    page = 1,
                    canLoadMore = pageable && data.orEmpty().isNotEmpty(),
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
            val batch = (result as? Result.Success)?.data?.let(::withExtras).orEmpty().filter { matchesType(it.type, state.type) }
            enrich(batch)
            _uiState.update {
                if (filtersChanged(state, it) || it.query.isNotBlank() || result is Result.Error) it.copy(isLoadingMore = false) else {
                    val merged = (it.comics + batch).distinctBy { c -> "${c.source}:${c.slug}" }
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
        const val ENRICH_LIMIT = 40
    }
}
