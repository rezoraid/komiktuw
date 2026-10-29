package com.komikta.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.local.ReaderPrefs
import com.komikta.data.local.dao.ReadChapterDao
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.ReadChapterEntity
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.domain.model.Chapter
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.usecase.GetChapterContentUseCase
import com.komikta.domain.usecase.GetComicDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = true,
    val content: ChapterContent? = null,
    val errorMessage: String? = null,
    val source: String = "",
    val comicSlug: String = "",
    val currentChapter: Chapter? = null,
    val nextChapter: Chapter? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getChapterContent: GetChapterContentUseCase,
    private val getComicDetail: GetComicDetailUseCase,
    private val prefs: ReaderPrefs,
    private val historyDao: ReadingHistoryDao,
    private val readChapterDao: ReadChapterDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var source: String = savedStateHandle["source"] ?: "lumos"
    private var comicSlug: String = savedStateHandle["comicSlug"] ?: ""
    private var chapterSlug: String = savedStateHandle["chapterSlug"] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState(source = source, comicSlug = comicSlug))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val autoNext: StateFlow<Boolean> = prefs.autoNext

    fun setAutoNext(value: Boolean) = prefs.setAutoNext(value)

    init { loadChapter() }

    fun loadChapter(newChapterSlug: String? = null) {
        if (newChapterSlug != null) chapterSlug = newChapterSlug
        if (comicSlug.isBlank() || chapterSlug.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid chapter") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, currentChapter = null, nextChapter = null) }
            when (val result = getChapterContent(source, comicSlug, chapterSlug)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, content = result.data, source = source, comicSlug = comicSlug)
                    }
                    saveHistory(result.data)
                    loadChapterInfo(result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadChapterInfo(content: ChapterContent) {
        val detail = getComicDetail(source, comicSlug).getOrNull() ?: return
        if (content.chapterSlug != chapterSlug) return
        val next = content.nextChapterSlug
        _uiState.update { st ->
            st.copy(
                currentChapter = detail.chapters.firstOrNull { it.slug == chapterSlug },
                nextChapter = next?.let { n -> detail.chapters.firstOrNull { it.slug == n } }
            )
        }
    }

    private suspend fun saveHistory(content: ChapterContent) {
        readChapterDao.markRead(
            ReadChapterEntity(
                comicId = "${source}_$comicSlug",
                chapterSlug = chapterSlug,
                readAt = System.currentTimeMillis()
            )
        )
        val id = "${source}_$comicSlug"
        historyDao.upsert(
            ReadingHistoryEntity(
                id = id,
                source = source,
                comicSlug = comicSlug,
                comicTitle = content.title ?: comicSlug,
                coverUrl = null,
                lastChapterSlug = chapterSlug,
                lastChapterNumber = content.title,
                lastReadAt = System.currentTimeMillis()
            )
        )
    }

    fun goPrev() {
        _uiState.value.content?.prevChapterSlug?.let { loadChapter(it) }
    }

    fun goNext() {
        _uiState.value.content?.nextChapterSlug?.let { loadChapter(it) }
    }
}
