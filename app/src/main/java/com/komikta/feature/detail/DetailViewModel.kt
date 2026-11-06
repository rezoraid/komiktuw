package com.komikta.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.local.ReaderPrefs
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadChapterDao
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.repository.CommunityRepository
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.usecase.GetComicDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LikeUi(val liked: Boolean = false, val count: Int = 0, val busy: Boolean = false)

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: ComicDetail? = null,
    val errorMessage: String? = null,
    val chaptersNewestFirst: Boolean = true
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getComicDetail: GetComicDetailUseCase,
    private val bookmarkDao: BookmarkDao,
    readChapterDao: ReadChapterDao,
    private val prefs: ReaderPrefs,
    private val community: CommunityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val source: String = savedStateHandle["source"] ?: "lumos"
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val bookmarkId = "${source}_$slug"

    private val _uiState = MutableStateFlow(DetailUiState(chaptersNewestFirst = prefs.chaptersNewestFirst.value))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _chapterComments = MutableStateFlow<Map<String, Int>>(emptyMap())
    val chapterComments: StateFlow<Map<String, Int>> = _chapterComments.asStateFlow()

    val isBookmarked: StateFlow<Boolean> = bookmarkDao.isBookmarked(bookmarkId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val readChapters: StateFlow<Set<String>> = readChapterDao.observeRead(bookmarkId)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _like = MutableStateFlow(LikeUi())
    val like: StateFlow<LikeUi> = _like.asStateFlow()

    private val _likeMessage = MutableStateFlow<String?>(null)
    val likeMessage: StateFlow<String?> = _likeMessage.asStateFlow()

    private var detailJob: Job? = null
    private var chapterCommentsJob: Job? = null
    private var likeStateJob: Job? = null
    private var confirmed = LikeUi()
    private var wanted = false
    private var syncing = false

    init {
        loadDetail()
        loadLike()
    }

    fun loadChapterComments() {
        if (slug.isBlank()) return
        chapterCommentsJob?.cancel()
        chapterCommentsJob = viewModelScope.launch {
            community.observeChapterCounts(source, slug).collect { r ->
                if (r is Result.Success) _chapterComments.value = r.data
            }
        }
    }

    fun loadLike() {
        if (slug.isBlank()) return
        likeStateJob?.cancel()
        likeStateJob = viewModelScope.launch {
            community.observeLikeState(source, slug, null).collect { r ->
                if (r is Result.Success && !syncing) {
                    confirmed = LikeUi(r.data.liked, r.data.count)
                    _like.value = confirmed
                }
            }
        }
    }

    fun toggleLike() {
        val detail = _uiState.value.detail ?: return
        if (community.user.value == null) {
            _likeMessage.value = "Masuk dulu di tab Profil untuk menyukai komik"
            return
        }
        val before = _like.value
        if (!syncing) confirmed = before
        val want = !before.liked
        wanted = want
        _like.value = LikeUi(want, (before.count + if (want) 1 else -1).coerceAtLeast(0))
        if (syncing) return
        syncing = true
        viewModelScope.launch {
            try {
                while (wanted != confirmed.liked) {
                    val target = wanted
                    when (val r = community.setLike(target, source, slug, null, detail.title, "", detail.coverUrl.orEmpty())) {
                        is Result.Success -> {
                            confirmed = LikeUi(r.data.liked, r.data.count)
                            if (wanted == confirmed.liked) _like.value = confirmed
                        }
                        is Result.Error -> {
                            wanted = confirmed.liked
                            _like.value = confirmed
                            _likeMessage.value = r.message
                        }
                        else -> {
                            wanted = confirmed.liked
                            _like.value = confirmed
                        }
                    }
                }
            } finally {
                syncing = false
            }
        }
    }

    fun clearLikeMessage() { _likeMessage.value = null }

    fun loadDetail(refresh: Boolean = false) {
        if (slug.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid comic") }
            return
        }
        detailJob?.cancel()
        _uiState.update { it.copy(isLoading = it.detail == null, errorMessage = null) }
        detailJob = viewModelScope.launch {
            getComicDetail.observe(source, slug, refresh).collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(isLoading = false, detail = result.data, errorMessage = null) }
                    is Result.Error -> _uiState.update {
                        if (it.detail != null) it.copy(isLoading = false) else it.copy(isLoading = false, errorMessage = result.message)
                    }
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun toggleBookmark() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            if (bookmarkDao.getById(bookmarkId) != null) {
                bookmarkDao.delete(bookmarkId)
            } else {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        id = bookmarkId,
                        source = source,
                        comicSlug = slug,
                        comicTitle = detail.title,
                        coverUrl = detail.coverUrl,
                        type = detail.type,
                        status = detail.status,
                        bookmarkedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun toggleChapterOrder() {
        val newValue = !_uiState.value.chaptersNewestFirst
        prefs.setChaptersNewestFirst(newValue)
        _uiState.update { it.copy(chaptersNewestFirst = newValue) }
    }
}
