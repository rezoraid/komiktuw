package com.komikta.data.repository

import com.komikta.BuildConfig
import com.komikta.core.cache.ResponseCache
import com.komikta.core.common.Result
import com.komikta.core.network.HttpException
import com.komikta.data.local.SessionStore
import com.komikta.data.remote.api.CommunityApi
import com.komikta.data.remote.dto.ActivityPageDto
import com.komikta.data.remote.dto.AdminCommentPageDto
import com.komikta.data.remote.dto.AdminStatsDto
import com.komikta.data.remote.dto.AdminUserPageDto
import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.CommentDto
import com.komikta.data.remote.dto.CommentPageDto
import com.komikta.data.remote.dto.CommentResultDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.data.remote.dto.LikeResultDto
import com.komikta.data.remote.dto.LikeStateDto
import com.komikta.data.remote.dto.ProfileResponseDto
import com.komikta.data.remote.dto.PublicReadingHistoryDto
import com.komikta.data.remote.dto.ReportPageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val api: CommunityApi,
    private val session: SessionStore,
    private val cache: ResponseCache
) {
    val user: StateFlow<CommunityUserDto?> get() = session.user

    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val meLock = Mutex()
    private var lastMeAt = 0L
    private var progressJob: Job? = null
    private var lastProgressKey: String? = null

    private val countsSerializer = MapSerializer(String.serializer(), Int.serializer())
    private val pinnedSerializer = ListSerializer(CommentDto.serializer())
    private val pinLock = Mutex()

    private fun uid(): String = session.user.value?.id?.takeIf { it.isNotBlank() } ?: "anon"
    private fun scope(): String = "u|${uid()}|"

    private fun commentKey(source: String, slug: String, chapter: String?, sort: String) =
        "${scope()}comments|$source|$slug|${chapter.orEmpty()}|$sort"

    private fun likeKey(source: String, slug: String, chapter: String?) =
        "${scope()}like|$source|$slug|${chapter.orEmpty()}"

    private fun pinKey(source: String, slug: String, chapter: String?) =
        "${scope()}mine|$source|$slug|${chapter.orEmpty()}"

    private fun countsKey(source: String, slug: String) = "counts|$source|$slug"
    private fun profileKey(userId: String) = "${scope()}profile|$userId"
    private fun activityKey(userId: String, tab: String) = "${scope()}activity|$userId|$tab"

    fun cachedComments(source: String, slug: String, chapter: String?, sort: String): CommentPageDto? =
        cache.peekMemory<CommentPageDto>(commentKey(source, slug, chapter, sort))?.value

    fun storeComments(source: String, slug: String, chapter: String?, sort: String, page: CommentPageDto) {
        cache.put(commentKey(source, slug, chapter, sort), CommentPageDto.serializer(), page)
    }

    fun observeComments(source: String, slug: String, chapter: String?, sort: String, refresh: Boolean = false): Flow<Result<CommentPageDto>> =
        cache.stream(commentKey(source, slug, chapter, sort), CommentPageDto.serializer(), TTL_COMMENTS, refresh) {
            call { api.comments(source, slug, chapter, sort, 1) }
        }

    suspend fun pinned(source: String, slug: String, chapter: String?): List<CommentDto> {
        val limit = System.currentTimeMillis() - PIN_MS
        return cache.peek(pinKey(source, slug, chapter), pinnedSerializer)?.value.orEmpty().filter { it.createdAt > limit }
    }

    suspend fun pin(source: String, slug: String, chapter: String?, comment: CommentDto, onlyIfPresent: Boolean = false) {
        pinLock.withLock {
            val key = pinKey(source, slug, chapter)
            val current = cache.peek(key, pinnedSerializer)?.value.orEmpty()
            if (!onlyIfPresent || current.any { it.id == comment.id }) {
                cache.put(key, pinnedSerializer, (listOf(comment) + current.filter { it.id != comment.id }).take(PIN_MAX))
            }
        }
    }

    suspend fun unpin(source: String, slug: String, chapter: String?, id: String) {
        pinLock.withLock {
            val key = pinKey(source, slug, chapter)
            val current = cache.peek(key, pinnedSerializer)?.value.orEmpty()
            if (current.any { it.id == id }) cache.put(key, pinnedSerializer, current.filter { it.id != id })
        }
    }

    suspend fun findPosted(
        source: String,
        slug: String,
        chapter: String?,
        rootId: String?,
        userId: String,
        body: String,
        since: Long,
        known: Set<String>
    ): CommentDto? {
        val fetches: List<suspend () -> Result<CommentPageDto>> =
            if (rootId != null) listOf({ call { api.replies(rootId, 1) } })
            else listOf({ call { api.comments(source, slug, chapter, "new", 1) } }, { call { api.comments(source, slug, chapter, "top", 1) } })
        for (fetch in fetches) {
            val page = (fetch() as? Result.Success)?.data ?: continue
            page.items.firstOrNull { it.id !in known && it.user.id == userId && it.body.trim() == body && it.createdAt >= since }?.let { return it }
        }
        return null
    }

    fun prefetchComments(source: String, slug: String, chapter: String?, sort: String = "top") {
        background.launch {
            cache.load(commentKey(source, slug, chapter, sort), CommentPageDto.serializer(), TTL_COMMENTS) {
                call { api.comments(source, slug, chapter, sort, 1) }
            }
        }
    }

    suspend fun commentPreview(source: String, slug: String, chapter: String): CommentPageDto? =
        cache.load(commentKey(source, slug, chapter, "top"), CommentPageDto.serializer(), TTL_COMMENTS) {
            call { api.comments(source, slug, chapter, "top", 1) }
        }.let { (it as? Result.Success)?.data }

    fun peekCommentPreview(source: String, slug: String, chapter: String): CommentPageDto? =
        cache.peekMemory<CommentPageDto>(commentKey(source, slug, chapter, "top"))?.value

    private suspend fun <T> call(block: suspend () -> ApiResponse<T>): Result<T> = try {
        val r = block()
        val data = r.data
        if (r.ok && data != null) {
            Result.Success(data)
        } else {
            if (r.error?.code == "UNAUTHORIZED") signOut()
            Result.Error(r.error?.message ?: "Terjadi kesalahan", r.error?.code)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        Result.Error("Koneksi timeout, coba lagi", throwable = e)
    } catch (e: UnknownHostException) {
        Result.Error("Tidak ada koneksi internet", throwable = e)
    } catch (e: HttpException) {
        Result.Error("Server bermasalah (${e.code})", throwable = e)
    } catch (e: IOException) {
        Result.Error("Gangguan jaringan", throwable = e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Terjadi kesalahan", throwable = e)
    }

    suspend fun signInWithGoogle(idToken: String): Result<CommunityUserDto> =
        call { api.loginGoogle(idToken) }.map {
            session.save(it.token, it.user)
            lastMeAt = System.currentTimeMillis()
            it.user
        }

    suspend fun refreshMe(force: Boolean = false): Result<CommunityUserDto> {
        if (session.token == null) return Result.Error("Belum masuk", "UNAUTHORIZED")
        return meLock.withLock {
            val current = session.user.value
            if (!force && current != null && System.currentTimeMillis() - lastMeAt < ME_FRESH_MS) {
                return@withLock Result.Success(current)
            }
            call { api.me() }.map { r ->
                r.token?.let { session.updateToken(it) }
                session.update(r.user)
                lastMeAt = System.currentTimeMillis()
                r.user
            }
        }
    }

    suspend fun updateUsername(username: String): Result<CommunityUserDto> {
        val before = session.user.value
        if (before != null) session.replaceLocal(before.copy(username = username))
        return call { api.updateUsername(username) }
            .map { session.update(it.user); it.user }
            .also { if (it is Result.Error && before != null) session.replaceLocal(before) }
    }

    suspend fun updateDisplayName(name: String): Result<CommunityUserDto> {
        val before = session.user.value
        if (before != null) session.replaceLocal(before.copy(name = name))
        return call { api.updateDisplayName(name) }
            .map { session.update(it.user); it.user }
            .also { if (it is Result.Error && before != null) session.replaceLocal(before) }
    }

    suspend fun updateAvatar(
        imageData: String,
        previewUrl: String? = null,
        preload: suspend (String) -> Unit = {}
    ): Result<CommunityUserDto> {
        val before = session.user.value
        if (before != null && previewUrl != null) session.replaceLocal(before.copy(picture = previewUrl))
        return when (val r = call { api.updateAvatar(imageData) }) {
            is Result.Success -> {
                val revision = System.currentTimeMillis()
                val remote = r.data.user
                val busted = session.bustedUrl(remote.picture, revision)
                val absolute = if (busted.startsWith("/")) BuildConfig.BASE_URL.trimEnd('/') + busted else busted
                if (busted.isNotBlank() && !busted.startsWith("file:")) {
                    withTimeoutOrNull(AVATAR_PRELOAD_MS) { preload(absolute) }
                }
                session.bumpAvatar(revision)
                session.update(remote)
                Result.Success(remote)
            }
            else -> {
                if (before != null) session.replaceLocal(before)
                r.map { it.user }
            }
        }
    }

    // ---- Panel OWNER ----
    suspend fun adminStats(): Result<AdminStatsDto> = call { api.adminStats() }

    suspend fun adminUsers(q: String, role: String, banned: Boolean, page: Int): Result<AdminUserPageDto> = call { api.adminUsers(q, role, banned, page) }

    suspend fun adminComments(q: String, reported: Boolean, page: Int): Result<AdminCommentPageDto> = call { api.adminComments(q, reported, page) }

    suspend fun adminUser(id: String): Result<CommunityUserDto> = call { api.adminUser(id) }.map { it.user }

    suspend fun adminGrantXp(id: String, amount: Int): Result<CommunityUserDto> = call { api.adminGrantXp(id, amount) }.map { it.user }

    suspend fun adminResetMedia(id: String, avatar: Boolean): Result<Unit> =
        call { api.adminResetMedia(id, avatar) }.map { }

    suspend fun adminSetRole(id: String, role: String): Result<CommunityUserDto> = call { api.adminSetRole(id, role) }.map { it.user }

    suspend fun adminSetVerified(id: String, verified: Boolean): Result<CommunityUserDto> = call { api.adminSetVerified(id, verified) }.map { it.user }

    suspend fun adminSetBan(id: String, banned: Boolean): Result<Unit> = call { api.adminSetBan(id, banned) }.map { }

    fun queueReadingProgress(source: String, slug: String, chapter: String, comicTitle: String, chapterTitle: String, coverUrl: String) {
        if (session.token == null) return
        val key = "$source|$slug|$chapter|$comicTitle"
        if (key == lastProgressKey) return
        progressJob?.cancel()
        progressJob = background.launch {
            delay(PROGRESS_DEBOUNCE_MS)
            var attempt = 0
            while (attempt < PROGRESS_ATTEMPTS) {
                val r = call { api.saveReadingProgress(source, slug, chapter, comicTitle, chapterTitle, coverUrl) }
                if (r is Result.Success) {
                    lastProgressKey = key
                    return@launch
                }
                if (r is Result.Error && r.code != null) return@launch
                attempt++
                delay(PROGRESS_RETRY_MS * attempt)
            }
        }
    }

    suspend fun cachedProfile(userId: String): ProfileResponseDto? =
        cache.peek(profileKey(userId), ProfileResponseDto.serializer())?.value

    suspend fun profile(userId: String): Result<ProfileResponseDto> =
        call { api.profile(userId) }.also {
            if (it is Result.Success) cache.put(profileKey(userId), ProfileResponseDto.serializer(), it.data)
        }

    suspend fun cachedActivity(userId: String, tab: String): ActivityPageDto? =
        cache.peek(activityKey(userId, tab), ActivityPageDto.serializer())?.value

    suspend fun activity(userId: String, tab: String, page: Int): Result<ActivityPageDto> =
        call { api.activity(userId, tab, page) }.also {
            if (page == 1 && it is Result.Success) cache.put(activityKey(userId, tab), ActivityPageDto.serializer(), it.data)
        }

    fun trackReadingTime(source: String, slug: String, chapter: String, seconds: Int) {
        if (session.token == null || seconds < 1) return
        background.launch {
            var left = seconds
            while (left > 0) {
                val part = minOf(left, 300)
                if (call { api.readingTime(source, slug, chapter, part) } is Result.Error) return@launch
                left -= part
            }
        }
    }

    fun observeLikeState(source: String, slug: String, chapter: String?): Flow<Result<LikeStateDto>> =
        cache.stream(likeKey(source, slug, chapter), LikeStateDto.serializer(), TTL_LIKE) {
            call { api.likeState(source, slug, chapter) }
        }

    suspend fun setLike(on: Boolean, source: String, slug: String, chapter: String?, comicTitle: String, chapterTitle: String, coverUrl: String): Result<LikeStateDto> =
        call { if (on) api.putLike(source, slug, chapter, comicTitle, chapterTitle, coverUrl) else api.deleteLike(source, slug, chapter) }
            .also { if (it is Result.Success) cache.put(likeKey(source, slug, chapter), LikeStateDto.serializer(), it.data) }

    fun signOut() {
        val prefix = scope()
        session.clear()
        cache.clearPrefix(prefix)
        lastProgressKey = null
        lastMeAt = 0L
    }

    suspend fun deleteAccount(): Result<Unit> = call { api.deleteAccount() }.map { signOut() }

    suspend fun publicReadingHistory(userId: String): Result<PublicReadingHistoryDto> = call { api.publicReadingHistory(userId) }

    suspend fun reports(page: Int): Result<ReportPageDto> = call { api.reports(page) }

    suspend fun resolveReport(id: String, action: String): Result<Unit> = call { api.resolveReport(id, action) }.map { }

    suspend fun comments(source: String, slug: String, chapter: String?, sort: String, page: Int): Result<CommentPageDto> =
        call { api.comments(source, slug, chapter, sort, page) }
            .also { if (page == 1 && it is Result.Success) storeComments(source, slug, chapter, sort, it.data) }

    fun observeChapterCounts(source: String, slug: String): Flow<Result<Map<String, Int>>> =
        cache.stream(countsKey(source, slug), countsSerializer, TTL_COUNTS) {
            call { api.counts(source, slug) }.map { it.counts }
        }

    private fun repliesKey(rootId: String, page: Int) = "${scope()}replies|$rootId|$page"

    suspend fun replies(rootId: String, page: Int): Result<CommentPageDto> =
        cache.load(repliesKey(rootId, page), CommentPageDto.serializer(), TTL_COMMENTS) {
            call { api.replies(rootId, page) }
        }

    fun invalidateReplies(rootId: String) {
        cache.clearPrefix("${scope()}replies|$rootId|")
    }

    suspend fun post(source: String, slug: String, chapter: String?, parentId: String?, text: String, clientId: String? = null): Result<CommentResultDto> =
        call { api.post(source, slug, chapter, parentId, text, clientId) }.map { r ->
            r.user?.let { u -> session.update(u.copy(commentCount = (session.user.value?.commentCount ?: 0).coerceAtLeast(0) + 1)) }
            r
        }

    suspend fun edit(id: String, text: String): Result<CommentDto> = call { api.edit(id, text) }.map { it.comment }

    suspend fun delete(id: String): Result<Unit> = call { api.delete(id) }.map { }

    suspend fun like(id: String, on: Boolean): Result<LikeResultDto> = call { api.like(id, on) }

    suspend fun report(id: String, reason: String, note: String): Result<Unit> = call { api.report(id, reason, note) }.map { }

    private companion object {
        val TTL_COMMENTS = TimeUnit.SECONDS.toMillis(45)
        val TTL_LIKE = TimeUnit.SECONDS.toMillis(60)
        val TTL_COUNTS = TimeUnit.SECONDS.toMillis(60)
        const val PIN_MS = 6L * 60 * 60 * 1000
        const val PIN_MAX = 20
        const val ME_FRESH_MS = 30_000L
        const val AVATAR_PRELOAD_MS = 4_000L
        const val PROGRESS_DEBOUNCE_MS = 1_200L
        const val PROGRESS_RETRY_MS = 2_000L
        const val PROGRESS_ATTEMPTS = 3
    }
}
