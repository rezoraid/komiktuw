package com.komikta.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.local.SourcePrefs
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.domain.model.Comic
import com.komikta.domain.usecase.GetComicDetailUseCase
import com.komikta.domain.usecase.GetLatestComicsUseCase
import com.komikta.domain.usecase.GetPopularComicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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

    init {
        loadHome()
        viewModelScope.launch {
            sourcePrefs.choice.drop(1).collect {
                prefetched.clear()
                _uiState.value = HomeUiState()
                loadHome(refresh = true)
            }
        }
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
        _uiState.update { st ->
            val merged = when (result) {
                is Result.Success -> {
                    prefetchDetails(result.data)
                    merge(st, result.data)
                }
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
    }
}
