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

data class TabState(
    val items: List<ActivityItemDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)

data class ProfilePageState(
    val userId: String = "",
    val stats: CommunityUserDto? = null,
    val self: Boolean = false,
    val statsError: String? = null,
    val tab: Int = 0,
    val tabs: List<TabState> = List(3) { TabState() }
)

fun ActivityItemDto.listKey(): String = when (kind) {
    "comment" -> "c:$id"
    "like" -> "l:$type:$source:$slug:$chapter"
    else -> "h:$source:$slug"
}

@HiltViewModel
class ProfilePageViewModel @Inject constructor(
    private val repo: CommunityRepository,
    private val cache: ProfileCache
) : ViewModel() {
    private val _state = MutableStateFlow(ProfilePageState())
    val state: StateFlow<ProfilePageState> = _state.asStateFlow()

    /** Akun yang sedang masuk (null = tamu). Dipakai untuk menampilkan alat OWNER di profil orang lain. */
    val viewer: StateFlow<CommunityUserDto?> = repo.user

    private var userId = ""
    private var statsAt = 0L
    private var statsJob: Job? = null
    private val tabJobs = arrayOfNulls<Job>(ProfileTabKeys.size)

    private fun commit(block: (ProfilePageState) -> ProfilePageState) {
        _state.update(block)
        if (userId.isNotBlank()) cache.put(userId, ProfileCache.Entry(_state.value, statsAt))
    }

    fun load(id: String, force: Boolean = false) {
        if (id.isBlank()) return
        if (id != userId) {
            tabJobs.forEach { it?.cancel() }
            statsJob?.cancel()
            userId = id
            val hit = cache.get(id)
            statsAt = hit?.statsAt ?: 0L
            _state.value = hit?.state?.let { st -> st.copy(tabs = st.tabs.map { it.copy(loading = false) }) } ?: seedState(id)
            if (hit == null) restoreFromDisk(id)
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
                    commit { it.copy(stats = r.data.user, self = r.data.self, statsError = null) }
                    markStale()
                    ensureTab(_state.value.tab)
                }
                is Result.Error -> {
                    statsAt = 0L
                    commit { it.copy(statsError = r.message) }
                    ensureTab(_state.value.tab)
                }
                else -> Unit
            }
        }
    }

    private fun seedState(id: String): ProfilePageState {
        val me = repo.user.value?.takeIf { it.id == id }
        return ProfilePageState(userId = id, stats = me, self = me != null)
    }

    private fun restoreFromDisk(id: String) {
        viewModelScope.launch {
            val profile = repo.cachedProfile(id)
            if (profile != null && id == userId && _state.value.noTabLoaded()) {
                _state.update { it.copy(stats = profile.user, self = profile.self) }
            }
        }
    }

    private fun ProfilePageState.noTabLoaded(): Boolean = tabs.all { it.page == 0 }

    fun refresh() = load(userId, force = true)

    fun selectTab(index: Int) {
        if (index !in ProfileTabKeys.indices) return
        commit { it.copy(tab = index) }
        ensureTab(index)
    }

    fun loadMore() = loadTab(_state.value.tab)

    fun retry() {
        val i = _state.value.tab
        commit { s -> s.copy(tabs = s.tabs.mapIndexed { n, t -> if (n == i) t.copy(error = null) else t }) }
        loadTab(i)
    }

    private fun markStale() {
        tabJobs.forEach { it?.cancel() }
        commit { s -> s.copy(tabs = s.tabs.map { it.copy(page = 0, hasMore = true, loading = false, error = null) }) }
    }

    private fun ensureTab(index: Int) {
        val t = _state.value.tabs[index]
        if (t.page == 0 && !t.loading && t.error == null) loadTab(index)
    }

    private fun loadTab(index: Int) {
        val s = _state.value
        val t = s.tabs[index]
        if (userId.isBlank() || t.loading || !t.hasMore || t.error != null) return
        val id = userId
        val replace = t.page == 0
        update(index) { it.copy(loading = true) }
        tabJobs[index] = viewModelScope.launch {
            if (replace && t.items.isEmpty()) {
                repo.cachedActivity(id, ProfileTabKeys[index])?.let { cached ->
                    if (id == userId && _state.value.tabs[index].items.isEmpty()) {
                        update(index) { it.copy(items = cached.items.take(MAX_ITEMS)) }
                    }
                }
            }
            when (val r = repo.activity(id, ProfileTabKeys[index], t.page + 1)) {
                is Result.Success -> if (id == userId) update(index) { cur ->
                    val base = if (replace) emptyList() else cur.items
                    val seen = base.mapTo(HashSet()) { it.listKey() }
                    val merged = (base + r.data.items.filter { seen.add(it.listKey()) }).take(MAX_ITEMS)
                    cur.copy(items = merged, page = r.data.page, hasMore = r.data.hasMore && merged.size < MAX_ITEMS, loading = false, error = null)
                }
                is Result.Error -> if (id == userId) update(index) { it.copy(loading = false, error = r.message) }
                else -> update(index) { it.copy(loading = false) }
            }
        }
    }

    private fun update(index: Int, block: (TabState) -> TabState) {
        commit { s -> s.copy(tabs = s.tabs.mapIndexed { n, t -> if (n == index) block(t) else t }) }
    }

    private companion object {
        const val STALE_MS = 20_000L
        const val MAX_ITEMS = 200
    }
}
