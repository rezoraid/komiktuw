package com.komikta.feature.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.remote.dto.CommentDto
import com.komikta.data.remote.dto.CommentPageDto
import com.komikta.data.remote.dto.CommentResultDto
import com.komikta.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepliesState(
    val items: List<CommentDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val expanded: Boolean = false
)

data class CommentsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<CommentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val sort: String = "top",
    val replies: Map<String, RepliesState> = emptyMap(),
    val sending: Boolean = false,
    val notice: String? = null,
    val replyingTo: CommentDto? = null,
    val editing: CommentDto? = null
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val repo: CommunityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Target komentar bisa diganti lewat bind() supaya satu ViewModel di reader dipakai untuk chapter mana pun.
    var source: String = savedStateHandle["source"] ?: ""
        private set
    var slug: String = savedStateHandle["slug"] ?: ""
        private set
    var chapter: String? = savedStateHandle["chapter"]
        private set
    val chapterLabel: String?
        get() = chapter?.let { c -> Regex("""\d+(?:[.-]\d+)?""").find(c)?.value?.replace('-', '.') ?: c }

    val user = repo.user
    private val _ui = MutableStateFlow(CommentsUiState())
    val ui: StateFlow<CommentsUiState> = _ui.asStateFlow()
    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private var loadJob: Job? = null
    private var pinned: List<CommentDto> = emptyList()
    private val likeWanted = HashMap<String, Boolean>()
    private val likeConfirmed = HashMap<String, Pair<Boolean, Int>>()
    private val likeSyncing = HashSet<String>()

    init {
        if (slug.isNotBlank()) {
            primeFromCache()
            load()
        }
    }

    private fun primeFromCache() {
        val cached = repo.cachedComments(source, slug, chapter, "top") ?: return
        _ui.value = CommentsUiState(loading = false, items = cached.items.distinctBy { it.id }, total = cached.total, page = 1, hasMore = cached.hasMore)
    }

    fun bind(source: String, slug: String, chapter: String?) {
        if (this.source == source && this.slug == slug && this.chapter == chapter) return
        loadJob?.cancel()
        this.source = source
        this.slug = slug
        this.chapter = chapter
        pinned = emptyList()
        _ui.value = CommentsUiState()
        _draft.value = ""
        if (slug.isNotBlank()) primeFromCache()
    }

    fun setDraft(v: String) { if (v.length <= MAX_LEN) _draft.value = v }

    fun notice(msg: String?) = _ui.update { it.copy(notice = msg) }

    fun load(refresh: Boolean = false) {
        if (slug.isBlank()) return
        loadJob?.cancel()
        val src = source
        val sl = slug
        val ch = chapter
        val sort = _ui.value.sort
        if (_ui.value.items.isEmpty()) _ui.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            pinned = runCatching { repo.pinned(src, sl, ch) }.getOrDefault(emptyList())
            repo.observeComments(src, sl, ch, sort, refresh).collect { r ->
                when (r) {
                    is Result.Success -> {
                        _ui.update { st -> if (st.sort != sort) st else mergeServerPage(st, r.data) }
                        prefetchReplies(r.data.items)
                    }
                    is Result.Error -> _ui.update { if (it.items.isNotEmpty()) it.copy(loading = false) else it.copy(loading = false, error = r.message) }
                    else -> _ui.update { it.copy(loading = false) }
                }
            }
        }
    }

    private fun prefetchReplies(items: List<CommentDto>) {
        repo.prefetchReplies(items.filter { it.parentId == null && it.replyCount > 0 && !it.id.startsWith(TMP) }.map { it.id })
    }

    private fun mergeServerPage(st: CommentsUiState, page: CommentPageDto): CommentsUiState {
        val current = st.items.associateBy { it.id }
        val server = page.items.map { c ->
            if (c.id in likeSyncing) current[c.id]?.let { c.copy(liked = it.liked, likeCount = it.likeCount) } ?: c else c
        }
        val serverIds = server.mapTo(HashSet()) { it.id }
        val pending = st.items.filter { it.id.startsWith(TMP) }
        val local = pinned.filter { it.parentId == null && it.id !in serverIds }.map { current[it.id] ?: it }
        val localIds = local.mapTo(HashSet()) { it.id }
        val tail = if (st.page > 1) st.items.filter { !it.id.startsWith(TMP) && it.id !in serverIds && it.id !in localIds } else emptyList()
        return st.copy(
            loading = false,
            error = null,
            items = (pending + local + server + tail).distinctBy { it.id },
            total = page.total + pending.size,
            page = maxOf(st.page, 1),
            hasMore = if (st.page > 1) st.hasMore else page.hasMore
        )
    }

    private fun sortLocal(items: List<CommentDto>, sort: String): List<CommentDto> =
        if (sort == "top") items.sortedWith(compareByDescending<CommentDto> { it.likeCount }.thenByDescending { it.createdAt })
        else items.sortedByDescending { it.createdAt }

    fun setSort(sort: String) {
        if (sort == _ui.value.sort) return
        val cached = repo.cachedComments(source, slug, chapter, sort)
        _ui.update { st ->
            st.copy(
                sort = sort,
                items = cached?.items ?: sortLocal(st.items, sort),
                total = cached?.total ?: st.total,
                hasMore = cached?.hasMore ?: st.hasMore,
                page = 1,
                error = null
            )
        }
        load()
    }

    fun loadMore() {
        val s = _ui.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        viewModelScope.launch {
            _ui.update { it.copy(loadingMore = true) }
            when (val r = repo.comments(source, slug, chapter, s.sort, s.page + 1)) {
                is Result.Success -> _ui.update { st ->
                    if (st.sort != s.sort) st.copy(loadingMore = false) else {
                        val known = st.items.mapTo(HashSet()) { it.id }
                        st.copy(loadingMore = false, items = st.items + r.data.items.filter { it.id !in known }.distinctBy { it.id }, page = s.page + 1, hasMore = r.data.hasMore, total = r.data.total)
                    }
                }
                is Result.Error -> _ui.update { it.copy(loadingMore = false, notice = r.message) }
                else -> _ui.update { it.copy(loadingMore = false) }
            }
        }
    }

    private fun persist() {
        val s = _ui.value
        if (s.loading || s.page > 1) return
        repo.storeComments(source, slug, chapter, s.sort, CommentPageDto(items = s.items.filter { !it.id.startsWith(TMP) }, hasMore = s.hasMore, page = 1, total = s.total))
    }

    fun toggleReplies(root: CommentDto) {
        val cur = _ui.value.replies[root.id] ?: RepliesState()
        _ui.update { it.copy(replies = it.replies + (root.id to cur.copy(expanded = !cur.expanded))) }
        if (!cur.expanded && cur.page == 0) loadReplies(root.id)
    }

    fun loadReplies(rootId: String) {
        val cur = _ui.value.replies[rootId] ?: RepliesState()
        if (cur.loading || !cur.hasMore) return
        viewModelScope.launch {
            _ui.update { it.copy(replies = it.replies + (rootId to cur.copy(loading = true))) }
            when (val r = repo.replies(rootId, cur.page + 1)) {
                is Result.Success -> _ui.update { st ->
                    val prev = st.replies[rootId] ?: cur
                    val serverIds = r.data.items.mapTo(HashSet()) { it.id }
                    val items = if (cur.page == 0) {
                        r.data.items + prev.items.filter { it.id !in serverIds }
                    } else {
                        val known = prev.items.mapTo(HashSet()) { it.id }
                        prev.items + r.data.items.filter { it.id !in known }
                    }
                    st.copy(replies = st.replies + (rootId to prev.copy(items = items, page = cur.page + 1, hasMore = r.data.hasMore, loading = false)))
                }
                is Result.Error -> _ui.update { st -> st.copy(notice = r.message, replies = st.replies + (rootId to (st.replies[rootId] ?: cur).copy(loading = false))) }
                else -> _ui.update { st -> st.copy(replies = st.replies + (rootId to (st.replies[rootId] ?: cur).copy(loading = false))) }
            }
        }
    }

    fun startReply(c: CommentDto) {
        if (c.id.startsWith(TMP)) return
        val wasEditing = _ui.value.editing != null
        _ui.update { it.copy(replyingTo = c, editing = null) }
        if (wasEditing) _draft.value = ""
    }

    fun startEdit(c: CommentDto) {
        _ui.update { it.copy(editing = c, replyingTo = null) }
        _draft.value = c.body
    }

    fun cancelCompose() {
        val wasEditing = _ui.value.editing != null
        _ui.update { it.copy(editing = null, replyingTo = null) }
        if (wasEditing) _draft.value = ""
    }

    private fun mapAll(id: String, f: (CommentDto) -> CommentDto) = _ui.update { st ->
        st.copy(
            items = st.items.map { if (it.id == id) f(it) else it }.distinctBy { it.id },
            replies = st.replies.mapValues { (_, rs) -> rs.copy(items = rs.items.map { if (it.id == id) f(it) else it }.distinctBy { it.id }) }
        )
    }

    private fun addLocal(st: CommentsUiState, c: CommentDto, notice: String? = st.notice): CommentsUiState {
        val rootId = c.parentId
        if (rootId == null) return st.copy(replyingTo = null, items = listOf(c) + st.items, total = st.total + 1, notice = notice)
        val cur = st.replies[rootId] ?: RepliesState()
        return st.copy(
            replyingTo = null,
            total = st.total + 1,
            notice = notice,
            items = st.items.map { if (it.id == rootId) it.copy(replyCount = it.replyCount + 1) else it },
            replies = st.replies + (rootId to cur.copy(expanded = true, items = cur.items + c))
        )
    }

    private fun dropLocal(id: String, rootId: String?) = _ui.update { st ->
        val cur = rootId?.let { st.replies[it] }
        st.copy(
            total = (st.total - 1).coerceAtLeast(0),
            items = st.items.mapNotNull { if (it.id == id) null else if (it.id == rootId) it.copy(replyCount = (it.replyCount - 1).coerceAtLeast(0)) else it },
            replies = if (rootId == null || cur == null) st.replies else st.replies + (rootId to cur.copy(items = cur.items.filter { it.id != id }))
        )
    }

    private fun ambiguous(r: Result<*>) = r is Result.Error && r.code == null && r.throwable != null

    fun submit() {
        val text = _draft.value.trim()
        val s = _ui.value
        if (text.isEmpty()) return
        val src = source
        val sl = slug
        val ch = chapter
        val editing = s.editing
        if (editing != null) {
            if (editing.id.startsWith(TMP)) return
            mapAll(editing.id) { it.copy(body = text, edited = true) }
            _draft.value = ""
            _ui.update { it.copy(editing = null) }
            viewModelScope.launch {
                when (val r = repo.edit(editing.id, text)) {
                    is Result.Success -> {
                        mapAll(editing.id) { r.data }
                        pinned = pinned.map { if (it.id == r.data.id) r.data else it }
                        repo.pin(src, sl, ch, r.data, onlyIfPresent = true)
                        persist()
                        notice("Komentar diperbarui")
                    }
                    is Result.Error -> {
                        mapAll(editing.id) { editing }
                        notice(r.message)
                        if (ambiguous(r)) load(refresh = true)
                    }
                    else -> mapAll(editing.id) { editing }
                }
            }
            return
        }
        val me = user.value ?: return
        val target = s.replyingTo
        val rootId = target?.let { it.parentId ?: it.id }
        val tmp = CommentDto(
            id = TMP + System.nanoTime(),
            parentId = rootId,
            replyTo = target?.user?.username,
            body = text,
            createdAt = System.currentTimeMillis(),
            user = me
        )
        _draft.value = ""
        _ui.update { addLocal(it, tmp) }
        viewModelScope.launch {
            val sentAt = System.currentTimeMillis()
            // clientId sama dipakai untuk semua percobaan kiriman ini → server tidak pernah membuat komentar ganda
            val clientId = java.util.UUID.randomUUID().toString()
            var result: Result<CommentResultDto> = repo.post(src, sl, ch, target?.id, text, clientId)
            if (ambiguous(result)) result = repo.post(src, sl, ch, target?.id, text, clientId)
            if (ambiguous(result)) {
                val known = _ui.value.let { st -> st.items.mapTo(HashSet()) { it.id } + st.replies.values.flatMap { rs -> rs.items.map { it.id } } }
                repo.findPosted(src, sl, ch, rootId, me.id, text, sentAt - 60_000, known)?.let { result = Result.Success(CommentResultDto(comment = it)) }
            }
            when (val r = result) {
                is Result.Success -> {
                    val c = r.data.comment
                    mapAll(tmp.id) { c }
                    if (c.parentId == null) {
                        pinned = listOf(c) + pinned.filter { it.id != c.id }
                        repo.pin(src, sl, ch, c)
                    }
                    persist()
                    if (c.parentId != null) {
                        repo.invalidateReplies(c.parentId)
                        if ((_ui.value.replies[c.parentId]?.page ?: 0) == 0) loadReplies(c.parentId)
                    }
                }
                is Result.Error -> {
                    dropLocal(tmp.id, rootId)
                    if (_draft.value.isEmpty()) _draft.value = text
                    notice(r.message)
                }
                else -> {
                    dropLocal(tmp.id, rootId)
                    if (_draft.value.isEmpty()) _draft.value = text
                }
            }
        }
    }

    fun toggleLike(c: CommentDto) {
        if (c.id.startsWith(TMP)) return
        if (user.value == null) { notice("Masuk dulu untuk menyukai komentar"); return }
        val id = c.id
        val current = findComment(id) ?: c
        val on = !current.liked
        if (id !in likeSyncing) likeConfirmed[id] = current.liked to current.likeCount
        likeWanted[id] = on
        mapAll(id) { it.copy(liked = on, likeCount = (it.likeCount + if (on) 1 else -1).coerceAtLeast(0)) }
        if (likeSyncing.add(id)) syncLike(id)
    }

    private fun findComment(id: String): CommentDto? {
        val st = _ui.value
        return st.items.firstOrNull { it.id == id } ?: st.replies.values.firstNotNullOfOrNull { rs -> rs.items.firstOrNull { it.id == id } }
    }

    private fun syncLike(id: String) {
        viewModelScope.launch {
            try {
                while (true) {
                    val (confirmedLiked, confirmedCount) = likeConfirmed[id] ?: break
                    val target = likeWanted[id] ?: break
                    if (target == confirmedLiked) break
                    when (val r = repo.like(id, target)) {
                        is Result.Success -> {
                            likeConfirmed[id] = r.data.liked to r.data.likeCount
                            if (likeWanted[id] == r.data.liked) {
                                mapAll(id) { it.copy(liked = r.data.liked, likeCount = r.data.likeCount) }
                            }
                        }
                        is Result.Error -> {
                            likeWanted[id] = confirmedLiked
                            mapAll(id) { it.copy(liked = confirmedLiked, likeCount = confirmedCount) }
                            notice(r.message)
                            break
                        }
                        else -> {
                            likeWanted[id] = confirmedLiked
                            mapAll(id) { it.copy(liked = confirmedLiked, likeCount = confirmedCount) }
                            break
                        }
                    }
                }
            } finally {
                likeSyncing.remove(id)
                likeWanted.remove(id)
                likeConfirmed.remove(id)
                persist()
            }
        }
    }

    private fun removeLocal(st: CommentsUiState, c: CommentDto): CommentsUiState {
        val rootId = c.parentId
        return if (rootId == null) {
            if (c.replyCount > 0) st.copy(total = (st.total - 1).coerceAtLeast(0), items = st.items.map { if (it.id == c.id) it.copy(deleted = true, body = "", canEdit = false, canDelete = false) else it })
            else st.copy(total = (st.total - 1).coerceAtLeast(0), items = st.items.filter { it.id != c.id })
        } else {
            val root = st.items.firstOrNull { it.id == rootId }
            val left = ((root?.replyCount ?: 1) - 1).coerceAtLeast(0)
            val drop = root != null && root.deleted && left == 0
            val items = st.items.mapNotNull { if (it.id != rootId) it else if (drop) null else it.copy(replyCount = left) }
            val cur = st.replies[rootId]
            st.copy(
                total = (st.total - 1).coerceAtLeast(0),
                items = items,
                replies = if (cur == null) st.replies else st.replies + (rootId to cur.copy(items = cur.items.filter { it.id != c.id }))
            )
        }
    }

    fun delete(c: CommentDto) {
        if (c.id.startsWith(TMP)) return
        val before = _ui.value
        val src = source
        val sl = slug
        val ch = chapter
        _ui.update { removeLocal(it, c) }
        viewModelScope.launch {
            when (val r = repo.delete(c.id)) {
                is Result.Success -> {
                    pinned = pinned.filter { it.id != c.id }
                    repo.unpin(src, sl, ch, c.id)
                    c.parentId?.let(repo::invalidateReplies)
                    persist()
                    notice("Komentar dihapus")
                    repo.refreshMe(force = true)
                }
                is Result.Error -> {
                    _ui.update { it.copy(items = before.items, replies = before.replies, total = before.total) }
                    notice(r.message)
                    if (ambiguous(r)) load(refresh = true)
                }
                else -> _ui.update { it.copy(items = before.items, replies = before.replies, total = before.total) }
            }
        }
    }

    fun report(c: CommentDto, reason: String) {
        viewModelScope.launch {
            when (val r = repo.report(c.id, reason, "")) {
                is Result.Success -> notice("Laporan terkirim, terima kasih")
                is Result.Error -> notice(r.message)
                else -> Unit
            }
        }
    }

    fun signIn(idToken: String) {
        viewModelScope.launch {
            when (val r = repo.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    notice("Masuk sebagai @${r.data.username}")
                    load()
                }
                is Result.Error -> notice(r.message)
                else -> Unit
            }
        }
    }

    companion object {
        const val MAX_LEN = 1000
        const val TMP = "tmp-"
    }
}
