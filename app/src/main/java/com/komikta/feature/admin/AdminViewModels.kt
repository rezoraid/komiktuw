package com.komikta.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.remote.dto.AdminCommentDto
import com.komikta.data.remote.dto.AdminStatsDto
import com.komikta.data.remote.dto.BroadcastDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminStatsState(val loading: Boolean = true, val stats: AdminStatsDto? = null, val error: String? = null)

@HiltViewModel
class AdminStatsViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminStatsState())
    val ui: StateFlow<AdminStatsState> = _ui.asStateFlow()
    val viewer: StateFlow<CommunityUserDto?> = repo.user

    init { load() }

    fun load() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        when (val r = repo.adminStats()) {
            is Result.Success -> _ui.update { it.copy(loading = false, stats = r.data) }
            is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
            else -> _ui.update { it.copy(loading = false) }
        }
    }
}

data class AdminUsersState(
    val query: String = "",
    val role: String = "",
    val banned: Boolean = false,
    val items: List<CommunityUserDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminUsersState())
    val ui: StateFlow<AdminUsersState> = _ui.asStateFlow()
    private var job: Job? = null

    init { load() }

    fun setQuery(q: String) { _ui.update { it.copy(query = q) }; load(350) }
    fun setRole(role: String) { _ui.update { it.copy(role = role, banned = false, items = emptyList()) }; load() }
    fun toggleBanned() { _ui.update { it.copy(banned = !it.banned, role = "", items = emptyList()) }; load() }

    fun load(wait: Long = 0, silent: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (wait > 0) delay(wait)
            if (!silent) _ui.update { it.copy(loading = true, error = null) }
            val s = _ui.value
            when (val r = repo.adminUsers(s.query.trim(), s.role, s.banned, 1)) {
                is Result.Success -> _ui.update { it.copy(loading = false, loadingMore = false, items = r.data.items, total = r.data.total, page = 1, hasMore = r.data.hasMore, error = null) }
                is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun loadMore() {
        val s = _ui.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        job = viewModelScope.launch {
            _ui.update { it.copy(loadingMore = true) }
            when (val r = repo.adminUsers(s.query.trim(), s.role, s.banned, s.page + 1)) {
                is Result.Success -> _ui.update { st ->
                    val known = st.items.map { it.id }.toSet()
                    st.copy(loadingMore = false, items = st.items + r.data.items.filter { it.id !in known }, total = r.data.total, page = s.page + 1, hasMore = r.data.hasMore)
                }
                is Result.Error -> _ui.update { it.copy(loadingMore = false, hasMore = false, error = if (it.items.isEmpty()) r.message else null) }
                else -> _ui.update { it.copy(loadingMore = false) }
            }
        }
    }
}

data class AdminCommentsState(
    val query: String = "",
    val reported: Boolean = false,
    val items: List<AdminCommentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

@HiltViewModel
class AdminCommentsViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminCommentsState())
    val ui: StateFlow<AdminCommentsState> = _ui.asStateFlow()
    private var job: Job? = null

    init { load() }

    fun setQuery(q: String) { _ui.update { it.copy(query = q) }; load(350) }
    fun setReported(on: Boolean) { _ui.update { it.copy(reported = on, items = emptyList()) }; load() }
    fun clearNotice() = _ui.update { it.copy(notice = null) }

    fun load(wait: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (wait > 0) delay(wait)
            _ui.update { it.copy(loading = true, error = null) }
            val s = _ui.value
            when (val r = repo.adminComments(s.query.trim(), s.reported, 1)) {
                is Result.Success -> _ui.update { it.copy(loading = false, loadingMore = false, items = r.data.items, total = r.data.total, page = 1, hasMore = r.data.hasMore) }
                is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun loadMore() {
        val s = _ui.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        job = viewModelScope.launch {
            _ui.update { it.copy(loadingMore = true) }
            when (val r = repo.adminComments(s.query.trim(), s.reported, s.page + 1)) {
                is Result.Success -> _ui.update { st ->
                    val known = st.items.map { it.id }.toSet()
                    st.copy(loadingMore = false, items = st.items + r.data.items.filter { it.id !in known }, total = r.data.total, page = s.page + 1, hasMore = r.data.hasMore)
                }
                is Result.Error -> _ui.update { it.copy(loadingMore = false, hasMore = false, notice = r.message) }
                else -> _ui.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun remove(c: AdminCommentDto) = viewModelScope.launch {
        when (val r = repo.delete(c.id)) {
            is Result.Success -> _ui.update { st -> st.copy(items = st.items.filter { it.id != c.id }, total = (st.total - 1).coerceAtLeast(0), notice = "Komentar dihapus") }
            is Result.Error -> _ui.update { it.copy(notice = r.message) }
            else -> Unit
        }
    }
}

data class AdminBroadcastState(
    val title: String = "",
    val body: String = "",
    val sending: Boolean = false,
    val items: List<BroadcastDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null
)

const val BROADCAST_TITLE_MAX = 60
const val BROADCAST_BODY_MAX = 200

@HiltViewModel
class AdminBroadcastViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminBroadcastState())
    val ui: StateFlow<AdminBroadcastState> = _ui.asStateFlow()

    init { load() }

    fun setTitle(v: String) = _ui.update { it.copy(title = v.take(BROADCAST_TITLE_MAX).replace("\n", " ")) }
    fun setBody(v: String) = _ui.update { it.copy(body = v.take(BROADCAST_BODY_MAX)) }
    fun clearNotice() = _ui.update { it.copy(notice = null) }

    fun load() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        when (val r = repo.adminBroadcasts(1)) {
            is Result.Success -> _ui.update { it.copy(loading = false, items = r.data.items) }
            is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
            else -> _ui.update { it.copy(loading = false) }
        }
    }

    fun send() {
        val s = _ui.value
        if (s.sending || s.title.isBlank() || s.body.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(sending = true, notice = null) }
            when (val r = repo.adminSendBroadcast(s.title.trim(), s.body.trim())) {
                is Result.Success -> _ui.update {
                    it.copy(sending = false, title = "", body = "", items = listOf(r.data) + it.items, notice = "Terkirim. HP akan menerimanya dalam ±30 menit.")
                }
                is Result.Error -> _ui.update { it.copy(sending = false, notice = r.message) }
                else -> _ui.update { it.copy(sending = false) }
            }
        }
    }

    fun remove(item: BroadcastDto) {
        viewModelScope.launch {
            when (val r = repo.adminDeleteBroadcast(item.id)) {
                is Result.Success -> _ui.update { it.copy(items = it.items.filter { b -> b.id != item.id }, notice = "Pengumuman ditarik") }
                is Result.Error -> _ui.update { it.copy(notice = r.message) }
                else -> Unit
            }
        }
    }
}
