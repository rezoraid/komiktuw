package com.komikta.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.remote.dto.ActivityItemDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val ProfileTabKeys = listOf("all", "comments", "likes")

/** Satu tab aktivitas: daftar dimuat bertahap (paginasi) dan disimpan selama ViewModel hidup. */
data class TabState(
    val items: List<ActivityItemDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)

data class ProfilePageState(
    val stats: CommunityUserDto? = null,
    val self: Boolean = false,
    val activityVisible: Boolean = true,
    val statsError: String? = null,
    val tab: Int = 0,
    val tabs: List<TabState> = List(3) { TabState() }
) {
    /** Tab All dan Likes privat bagi orang lain kecuali pemilik membukanya; Comments selalu publik. */
    fun locked(index: Int): Boolean = !activityVisible && index != 1
}

fun ActivityItemDto.listKey(): String = when (kind) {
    "comment" -> "c:$id"
    "like" -> "l:$type:$source:$slug:$chapter"
    else -> "h:$source:$slug"
}

@HiltViewModel
class ProfilePageViewModel @Inject constructor(
    private val repo: CommunityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfilePageState())
    val state: StateFlow<ProfilePageState> = _state.asStateFlow()

    private var userId = ""
    private var statsAt = 0L
    private var statsJob: Job? = null
    private val tabJobs = arrayOfNulls<Job>(ProfileTabKeys.size)

    /** Muat profil. Jika masih segar (<[STALE_MS]) tidak memanggil jaringan lagi. */
    fun load(id: String, force: Boolean = false) {
        if (id.isBlank()) return
        if (id != userId) {
            tabJobs.forEach { it?.cancel() }
            statsJob?.cancel()
            userId = id
            statsAt = 0L
            _state.value = ProfilePageState()
        }
        val fresh = System.currentTimeMillis() - statsAt < STALE_MS
        if (!force && fresh) {
            ensureTab(_state.value.tab)
            return
        }
        statsAt = System.currentTimeMillis()
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            when (val r = repo.profile(id)) {
                is Result.Success -> {
                    val wasVisible = _state.value.activityVisible
                    _state.update { it.copy(stats = r.data.user, self = r.data.self, activityVisible = r.data.activityVisible, statsError = null) }
                    // Data lama (mis. setelah baca chapter) diganti, bukan ditumpuk.
                    if (force || !wasVisible) resetTabs()
                    ensureTab(_state.value.tab)
                }
                is Result.Error -> {
                    statsAt = 0L
                    _state.update { it.copy(statsError = r.message) }
                    ensureTab(_state.value.tab)
                }
                else -> Unit
            }
        }
    }

    fun refresh() = load(userId, force = true)

    fun selectTab(index: Int) {
        if (index !in ProfileTabKeys.indices) return
        _state.update { it.copy(tab = index) }
        ensureTab(index)
    }

    fun loadMore() = loadTab(_state.value.tab)

    fun retry() {
        val i = _state.value.tab
        _state.update { s -> s.copy(tabs = s.tabs.mapIndexed { n, t -> if (n == i) t.copy(error = null) else t }) }
        loadTab(i)
    }

    private fun resetTabs() {
        tabJobs.forEach { it?.cancel() }
        _state.update { it.copy(tabs = List(ProfileTabKeys.size) { TabState() }) }
    }

    private fun ensureTab(index: Int) {
        val t = _state.value.tabs[index]
        if (t.page == 0 && !t.loading && t.error == null) loadTab(index)
    }

    private fun loadTab(index: Int) {
        val s = _state.value
        val t = s.tabs[index]
        if (userId.isBlank() || t.loading || !t.hasMore || t.error != null) return
        if (s.locked(index)) {
            update(index) { it.copy(hasMore = false) }
            return
        }
        val id = userId
        update(index) { it.copy(loading = true) }
        tabJobs[index] = viewModelScope.launch {
            when (val r = repo.activity(id, ProfileTabKeys[index], t.page + 1)) {
                is Result.Success -> if (id == userId) update(index) { cur ->
                    val seen = cur.items.mapTo(HashSet()) { it.listKey() }
                    val merged = (cur.items + r.data.items.filter { seen.add(it.listKey()) }).take(MAX_ITEMS)
                    cur.copy(items = merged, page = r.data.page, hasMore = r.data.hasMore && merged.size < MAX_ITEMS, loading = false, error = null)
                }
                is Result.Error -> if (id == userId) update(index) { it.copy(loading = false, error = r.message) }
                else -> update(index) { it.copy(loading = false) }
            }
        }
    }

    private fun update(index: Int, block: (TabState) -> TabState) {
        _state.update { s -> s.copy(tabs = s.tabs.mapIndexed { n, t -> if (n == index) block(t) else t }) }
    }

    private companion object {
        const val STALE_MS = 20_000L
        const val MAX_ITEMS = 200 // batas memori: daftar lebih dari ini tidak dimuat ke layar
    }
}
