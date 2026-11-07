package com.komikta.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.local.SourcePrefs
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.domain.model.Comic
import com.komikta.domain.model.fillMissingFrom
import com.komikta.domain.model.isIncomplete
import com.komikta.domain.model.replacing
import com.komikta.domain.model.uid
import com.komikta.domain.usecase.EnrichComicsUseCase
import com.komikta.domain.usecase.GetComicDetailUseCase
import com.komikta.domain.usecase.GetLatestComicsUseCase
import com.komikta.domain.usecase.GetPopularComicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val latestComics: List<Comic> = emptyList(),
    val popularComics: List<Comic> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLatestComics: GetLatestComicsUseCase,
    private val getPopularComics: GetPopularComicsUseCase,
    private val getComicDetail: GetComicDetailUseCase,
    private val enrichComics: EnrichComicsUseCase,
    historyDao: ReadingHistoryDao,
    private val sourcePrefs: SourcePrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val continueReading: StateFlow<List<ReadingHistoryEntity>> =
        historyDao.getRecentHistory(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var job: Job? = null
    private var latestDone = false
    private var popularDone = false
    private val prefetched = HashSet<String>() // "source:slug"
    private val enrichTried = HashSet<String>()
    private val extras = HashMap<String, Comic>() // hasil pelengkapan data, dipakai ulang saat daftar dimuat ulang

    private fun withExtras(items: List<Comic>) = items.map { c -> extras[c.uid]?.let { c.fillMissingFrom(it) } ?: c }

    /** Komik yang rating/views/genre/chapternya kosong dilengkapi dari detail, lalu kartunya diperbarui. */
    private fun enrich(items: List<Comic>) {
        val todo = items.filter { it.isIncomplete && enrichTried.add(it.uid) }.take(ENRICH_LIMIT)
        if (todo.isEmpty()) return
        viewModelScope.launch {
            enrichComics(todo, ENRICH_LIMIT) { c ->
                extras[c.uid] = c
                _uiState.update { st -> st.copy(latestComics = st.latestComics.replacing(c), popularComics = st.popularComics.replacing(c)) }
            }
        }
    }

    init {
        loadHome()
    }

    /**
     * Detail komik teratas dimuat diam-diam begitu daftar Home tampil (server sudah menyimpannya hangat),
     * jadi mengetuk komik langsung membuka detail + chapter lengkap dari cache, tanpa menunggu jaringan.
     */
    private fun prefetchDetails(items: List<Comic>) {
        val todo = items.take(PREFETCH_DETAILS).filter { prefetched.add("${it.source}:${it.slug}") }
        if (todo.isEmpty()) return
        viewModelScope.launch { todo.forEach { getComicDetail(it.source, it.slug) } }
    }

    fun loadHome(refresh: Boolean = false) {
        job?.cancel()
        latestDone = false
        popularDone = false
        _uiState.update { it.copy(isLoading = it.latestComics.isEmpty(), errorMessage = null) }
        val source = sourcePrefs.source
        job = viewModelScope.launch {
            launch {
                getLatestComics.observe(source, 1, refresh).collect { r ->
                    latestDone = true
                    applyResult(r) { st, items -> st.copy(latestComics = items) }
                }
            }
            launch {
                getPopularComics.observe(source, refresh).collect { r ->
                    popularDone = true
                    applyResult(r) { st, items -> st.copy(popularComics = items) }
                }
            }
        }
    }

    fun revalidate() {
        if (job?.isActive != true) loadHome()
    }

    private fun applyResult(result: Result<List<Comic>>, merge: (HomeUiState, List<Comic>) -> HomeUiState) {
        val items = (result as? Result.Success)?.data?.let(::withExtras)
        if (items != null) {
            prefetchDetails(items)
            enrich(items)
        }
        _uiState.update { st ->
            val merged = when (result) {
                is Result.Success -> merge(st, items.orEmpty())
                is Result.Empty -> merge(st, emptyList())
                else -> st
            }
            val error = (result as? Result.Error)?.message
            val hasData = merged.latestComics.isNotEmpty() || merged.popularComics.isNotEmpty()
            merged.copy(
                isLoading = !hasData && !(latestDone && popularDone),
                errorMessage = if (hasData) null else error ?: merged.errorMessage
            )
        }
    }

    private companion object {
        const val PREFETCH_DETAILS = 8
        const val ENRICH_LIMIT = 40
    }
}
