package com.komikta.data.repository

import com.komikta.core.common.Result
import com.komikta.core.network.HttpException
import com.komikta.data.local.SessionStore
import com.komikta.data.remote.api.CommunityApi
import com.komikta.data.remote.dto.ActivityPageDto
import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.LikeStateDto
import com.komikta.data.remote.dto.ProfileResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.komikta.data.remote.dto.CommentDto
import com.komikta.data.remote.dto.CommentPageDto
import com.komikta.data.remote.dto.CommentResultDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.data.remote.dto.LikeResultDto
import com.komikta.data.remote.dto.ReportPageDto
import com.komikta.data.remote.dto.PublicReadingHistoryDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val api: CommunityApi,
    private val session: SessionStore
) {
    val user: StateFlow<CommunityUserDto?> get() = session.user

    // Scope milik singleton: pengiriman waktu baca tetap selesai walau layar reader sudah ditutup.
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun <T> call(block: suspend () -> ApiResponse<T>): Result<T> = try {
        val r = block()
        val data = r.data
        if (r.ok && data != null) {
            Result.Success(data)
        } else {
            if (r.error?.code == "UNAUTHORIZED") session.clear()
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
        call { api.loginGoogle(idToken) }.map { session.save(it.token, it.user); it.user }

    suspend fun refreshMe(): Result<CommunityUserDto> {
        if (session.token == null) return Result.Error("Belum masuk", "UNAUTHORIZED")
        return call { api.me() }.map { r ->
            r.token?.let { session.updateToken(it) }
            session.update(r.user)
            r.user
        }
    }

    suspend fun updateUsername(username: String): Result<CommunityUserDto> =
        call { api.updateUsername(username) }.map { session.update(it.user); it.user }

    suspend fun updateAvatar(imageData: String): Result<CommunityUserDto> =
        call { api.updateAvatar(imageData) }.map { session.bumpAvatar(); session.update(it.user); it.user }

    suspend fun setReadingHistoryPublic(isPublic: Boolean): Result<Unit> =
        call { api.setReadingHistoryPublic(isPublic) }.map {
            session.user.value?.let { u -> session.update(u.copy(publicHistory = isPublic)) }
        }

    suspend fun saveReadingProgress(source: String, slug: String, chapter: String, comicTitle: String, chapterTitle: String, coverUrl: String): Result<Unit> =
        call { api.saveReadingProgress(source, slug, chapter, comicTitle, chapterTitle, coverUrl) }.map { }

    suspend fun profile(userId: String): Result<ProfileResponseDto> = call { api.profile(userId) }

    suspend fun activity(userId: String, tab: String, page: Int): Result<ActivityPageDto> = call { api.activity(userId, tab, page) }

    /** Kirim durasi baca aktif (detik). Fire-and-forget; server membatasi kredit berdasarkan waktu nyata. */
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

    suspend fun likeState(source: String, slug: String, chapter: String?): Result<LikeStateDto> = call { api.likeState(source, slug, chapter) }

    suspend fun setLike(on: Boolean, source: String, slug: String, chapter: String?, comicTitle: String, chapterTitle: String, coverUrl: String): Result<LikeStateDto> =
        call { if (on) api.putLike(source, slug, chapter, comicTitle, chapterTitle, coverUrl) else api.deleteLike(source, slug, chapter) }

    fun signOut() = session.clear()

    suspend fun deleteAccount(): Result<Unit> = call { api.deleteAccount() }.map { session.clear() }

    suspend fun publicReadingHistory(userId: String): Result<PublicReadingHistoryDto> = call { api.publicReadingHistory(userId) }

    suspend fun reports(page: Int): Result<ReportPageDto> = call { api.reports(page) }

    suspend fun resolveReport(id: String, action: String): Result<Unit> = call { api.resolveReport(id, action) }.map { }

    suspend fun comments(source: String, slug: String, chapter: String?, sort: String, page: Int): Result<CommentPageDto> =
        call { api.comments(source, slug, chapter, sort, page) }

    suspend fun chapterCounts(source: String, slug: String): Result<Map<String, Int>> =
        call { api.counts(source, slug) }.map { it.counts }

    suspend fun replies(rootId: String, page: Int): Result<CommentPageDto> = call { api.replies(rootId, page) }

    suspend fun post(source: String, slug: String, chapter: String?, parentId: String?, text: String): Result<CommentResultDto> =
        call { api.post(source, slug, chapter, parentId, text) }.map { r ->
            r.user?.let { u -> session.update(u.copy(commentCount = (session.user.value?.commentCount ?: 0).coerceAtLeast(0) + 1)) }
            r
        }

    suspend fun edit(id: String, text: String): Result<CommentDto> = call { api.edit(id, text) }.map { it.comment }

    suspend fun delete(id: String): Result<Unit> = call { api.delete(id) }.map { }

    suspend fun like(id: String, on: Boolean): Result<LikeResultDto> = call { api.like(id, on) }

    suspend fun report(id: String, reason: String, note: String): Result<Unit> = call { api.report(id, reason, note) }.map { }
}
