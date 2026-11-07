package com.komikta.data.remote.api

import com.komikta.BuildConfig
import com.komikta.core.network.ApiConfig
import com.komikta.core.network.HttpException
import com.komikta.data.local.SessionStore
import com.komikta.data.remote.dto.ActivityPageDto
import com.komikta.data.remote.dto.AdminCommentPageDto
import com.komikta.data.remote.dto.AdminComicDto
import com.komikta.data.remote.dto.AdminPagesDto
import com.komikta.data.remote.dto.MediaUploadDto
import com.komikta.data.remote.dto.ViewResultDto
import com.komikta.data.remote.dto.AdminStatsDto
import com.komikta.data.remote.dto.AdminUserPageDto
import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.BroadcastFeedDto
import com.komikta.data.remote.dto.BroadcastPageDto
import com.komikta.data.remote.dto.BroadcastResponse
import com.komikta.data.remote.dto.LikeStateDto
import com.komikta.data.remote.dto.ProfileResponseDto
import com.komikta.data.remote.dto.AuthResponse
import com.komikta.data.remote.dto.ChapterCountsDto
import com.komikta.data.remote.dto.CommentEditDto
import com.komikta.data.remote.dto.CommentPageDto
import com.komikta.data.remote.dto.CommentResultDto
import com.komikta.data.remote.dto.LikeResultDto
import com.komikta.data.remote.dto.MeResponse
import com.komikta.data.remote.dto.OkDto
import com.komikta.data.remote.dto.ReportPageDto
import com.komikta.data.remote.dto.PublicReadingHistoryDto
import com.komikta.data.remote.dto.ResolveDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CommunityApi @Inject constructor(
    client: OkHttpClient,
    private val json: Json,
    private val session: SessionStore
) {
    private val http = client.newBuilder().callTimeout(ApiConfig.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
    private val uploadHttp = client.newBuilder().callTimeout(UPLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
    private val base = ApiConfig.BASE_URL.toHttpUrl()
    private val userAgent = "Komikta-Android/${BuildConfig.VERSION_NAME}"
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun loginGoogle(idToken: String): ApiResponse<AuthResponse> =
        call("POST", listOf("auth", "google"), body = buildJsonObject { put("idToken", idToken) })

    suspend fun me(): ApiResponse<MeResponse> = call("GET", listOf("me"))

    // ---- Views (database Komikta) ----
    suspend fun trackComicView(source: String, slug: String): ApiResponse<ViewResultDto> =
        call("POST", listOf("views", "comic"), body = buildJsonObject { put("source", source); put("slug", slug) })

    suspend fun trackChapterView(source: String, comic: String, chapter: String): ApiResponse<ViewResultDto> =
        call("POST", listOf("views", "chapter"), body = buildJsonObject { put("source", source); put("comic", comic); put("chapter", chapter) })

    suspend fun updateUsername(username: String): ApiResponse<MeResponse> =
        call("PATCH", listOf("me"), body = buildJsonObject { put("username", username) })

    suspend fun updateDisplayName(name: String): ApiResponse<MeResponse> =
        call("PATCH", listOf("me"), body = buildJsonObject { put("name", name) })

    suspend fun updateAvatar(imageData: String): ApiResponse<MeResponse> =
        call("PUT", listOf("me", "avatar"), body = buildJsonObject { put("image", imageData) })

    suspend fun deleteAccount(): ApiResponse<OkDto> = call("DELETE", listOf("me"))

    /** Publik (tanpa login). Tanpa [after]: hanya posisi terbaru (`latestId`); dengan [after]: pengumuman yang lebih baru. */
    suspend fun broadcastFeed(after: String?): ApiResponse<BroadcastFeedDto> =
        call("GET", listOf("broadcasts"), query = if (after.isNullOrBlank()) emptyMap() else mapOf("after" to after))

    suspend fun registerPushToken(token: String, previous: String?): ApiResponse<OkDto> =
        call("PUT", listOf("push-token"), body = buildJsonObject {
            put("token", token)
            put("appVersion", BuildConfig.VERSION_NAME)
            if (!previous.isNullOrBlank() && previous != token) put("previous", previous)
        })

    suspend fun unregisterPushToken(token: String): ApiResponse<OkDto> =
        call("POST", listOf("push-token", "unregister"), body = buildJsonObject { put("token", token) })

    // ---- Panel OWNER ----
    suspend fun adminSendBroadcast(title: String, body: String): ApiResponse<BroadcastResponse> =
        call("POST", listOf("admin", "broadcasts"), body = buildJsonObject { put("title", title); put("body", body) })

    suspend fun adminBroadcasts(page: Int): ApiResponse<BroadcastPageDto> =
        call("GET", listOf("admin", "broadcasts"), query = mapOf("page" to page.toString()))

    suspend fun adminDeleteBroadcast(id: String): ApiResponse<OkDto> = call("DELETE", listOf("admin", "broadcasts", id))

    suspend fun adminStats(): ApiResponse<AdminStatsDto> = call("GET", listOf("admin", "stats"))

    suspend fun adminUsers(q: String, role: String, banned: Boolean, page: Int): ApiResponse<AdminUserPageDto> =
        call("GET", listOf("admin", "users"), query = buildMap {
            put("page", page.toString())
            if (q.isNotEmpty()) put("q", q)
            if (role.isNotEmpty()) put("role", role)
            if (banned) put("banned", "1")
        })

    suspend fun adminComments(q: String, reported: Boolean, page: Int): ApiResponse<AdminCommentPageDto> =
        call("GET", listOf("admin", "comments"), query = buildMap {
            put("page", page.toString())
            if (q.isNotEmpty()) put("q", q)
            if (reported) put("reported", "1")
        })

    suspend fun adminUser(id: String): ApiResponse<MeResponse> = call("GET", listOf("admin", "users", id))

    // ---- Panel OWNER: komik & gambar chapter ----
    suspend fun adminComic(source: String, slug: String): ApiResponse<AdminComicDto> =
        call("GET", listOf("admin", "comics", source, slug))

    suspend fun adminSaveComic(
        source: String, slug: String, title: String, cover: String, synopsis: String,
        genres: List<String>, type: String, status: String, author: String, artist: String
    ): ApiResponse<AdminComicDto> = call(
        "PUT", listOf("admin", "comics", source, slug),
        body = buildJsonObject {
            put("title", title); put("cover", cover); put("synopsis", synopsis)
            put("genres", JsonArray(genres.map { JsonPrimitive(it) }))
            put("type", type); put("status", status); put("author", author); put("artist", artist)
        }
    )

    suspend fun adminResetComic(source: String, slug: String): ApiResponse<AdminComicDto> =
        call("DELETE", listOf("admin", "comics", source, slug, "override"))

    suspend fun adminPages(source: String, slug: String, chapter: String): ApiResponse<AdminPagesDto> =
        call("GET", listOf("admin", "comics", source, slug, "chapters", chapter, "pages"))

    suspend fun adminSavePages(source: String, slug: String, chapter: String, pages: List<String>): ApiResponse<AdminPagesDto> =
        call(
            "PUT", listOf("admin", "comics", source, slug, "chapters", chapter, "pages"),
            body = buildJsonObject { put("pages", JsonArray(pages.map { JsonPrimitive(it) })) }
        )

    suspend fun adminResetPages(source: String, slug: String, chapter: String): ApiResponse<AdminPagesDto> =
        call("DELETE", listOf("admin", "comics", source, slug, "chapters", chapter, "pages"))

    /** Unggah satu gambar (sampul atau panel). Server membalas URL/path gambar yang tersimpan. */
    suspend fun adminUpload(bytes: ByteArray, mime: String, fileName: String): ApiResponse<MediaUploadDto> {
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mime.toMediaType()))
            .build()
        val (code, text) = executeBody("POST", url(listOf("admin", "media"), emptyMap()), form, uploadHttp)
        return withContext(Dispatchers.Default) {
            try {
                json.decodeFromString(ApiResponse.serializer(MediaUploadDto.serializer()), text)
            } catch (e: SerializationException) {
                if (code in 200..299) throw e else throw HttpException(code)
            }
        }
    }

    suspend fun adminGrantXp(id: String, amount: Int): ApiResponse<MeResponse> =
        call("POST", listOf("admin", "users", id, "xp"), body = buildJsonObject { put("amount", amount) })

    suspend fun adminResetMedia(id: String, avatar: Boolean): ApiResponse<OkDto> =
        call("POST", listOf("admin", "users", id, "reset-media"), body = buildJsonObject { put("avatar", avatar) })

    suspend fun adminSetRole(id: String, role: String): ApiResponse<MeResponse> =
        call("PUT", listOf("admin", "users", id, "role"), body = buildJsonObject { put("role", role) })

    suspend fun adminSetVerified(id: String, verified: Boolean): ApiResponse<MeResponse> =
        call("PUT", listOf("admin", "users", id, "verified"), body = buildJsonObject { put("verified", verified) })

    suspend fun adminSetBan(id: String, banned: Boolean): ApiResponse<OkDto> =
        call("PUT", listOf("admin", "users", id, "ban"), body = buildJsonObject { put("banned", banned) })

    suspend fun saveReadingProgress(source: String, slug: String, chapter: String, comicTitle: String, chapterTitle: String, coverUrl: String): ApiResponse<OkDto> =
        call("PUT", listOf("me", "reading-progress"), body = buildJsonObject {
            put("source", source); put("slug", slug); put("chapter", chapter)
            put("comicTitle", comicTitle); put("chapterTitle", chapterTitle); put("coverUrl", coverUrl)
        })

 suspend fun profile(userId: String): ApiResponse<ProfileResponseDto> =
        call("GET", listOf("users", userId, "profile"))

    suspend fun activity(userId: String, tab: String, page: Int): ApiResponse<ActivityPageDto> =
        call("GET", listOf("users", userId, "activity"), query = mapOf("tab" to tab, "page" to page.toString()))

    suspend fun readingTime(source: String, slug: String, chapter: String, seconds: Int): ApiResponse<OkDto> =
        call("PUT", listOf("me", "reading-time"), body = buildJsonObject {
            put("source", source); put("slug", slug); put("chapter", chapter); put("seconds", seconds)
        })

    suspend fun likeState(source: String, slug: String, chapter: String?): ApiResponse<LikeStateDto> =
        call("GET", listOf("likes", "status"), query = likeQuery(source, slug, chapter))

    suspend fun putLike(source: String, slug: String, chapter: String?, comicTitle: String, chapterTitle: String, coverUrl: String): ApiResponse<LikeStateDto> =
        call("PUT", listOf("likes"), body = buildJsonObject {
            put("type", if (chapter == null) "comic" else "chapter")
            put("source", source); put("slug", slug)
            if (chapter != null) put("chapter", chapter)
            put("comicTitle", comicTitle); put("chapterTitle", chapterTitle); put("coverUrl", coverUrl)
        })

    suspend fun deleteLike(source: String, slug: String, chapter: String?): ApiResponse<LikeStateDto> =
        call("DELETE", listOf("likes"), query = likeQuery(source, slug, chapter))

    private fun likeQuery(source: String, slug: String, chapter: String?): Map<String, String> = buildMap {
        put("type", if (chapter == null) "comic" else "chapter")
        put("source", source)
        put("slug", slug)
        if (chapter != null) put("chapter", chapter)
    }

    suspend fun publicReadingHistory(userId: String): ApiResponse<PublicReadingHistoryDto> =
        call("GET", listOf("users", userId, "history"))

    suspend fun reports(page: Int): ApiResponse<ReportPageDto> =
        call("GET", listOf("admin", "reports"), query = mapOf("page" to page.toString()))

    suspend fun resolveReport(id: String, action: String): ApiResponse<ResolveDto> =
        call("POST", listOf("admin", "reports", id, "resolve"), body = buildJsonObject { put("action", action) })

    suspend fun comments(source: String, slug: String, chapter: String?, sort: String, page: Int): ApiResponse<CommentPageDto> =
        call(
            "GET", listOf("comments"),
            query = buildMap<String, String> {
                put("type", if (chapter == null) "comic" else "chapter")
                put("source", source)
                put("slug", slug)
                if (chapter != null) put("chapter", chapter)
                put("sort", sort)
                put("page", page.toString())
            }
        )

    suspend fun counts(source: String, slug: String): ApiResponse<ChapterCountsDto> =
        call("GET", listOf("comments", "counts"), query = mapOf("source" to source, "slug" to slug))

    suspend fun replies(rootId: String, page: Int): ApiResponse<CommentPageDto> =
        call("GET", listOf("comments", rootId, "replies"), query = mapOf("page" to page.toString()))

    suspend fun post(source: String, slug: String, chapter: String?, parentId: String?, text: String, clientId: String? = null): ApiResponse<CommentResultDto> =
        call(
            "POST", listOf("comments"),
            body = buildJsonObject {
                if (parentId != null) put("parentId", parentId)
                else {
                    put("type", if (chapter == null) "comic" else "chapter")
                    put("source", source)
                    put("slug", slug)
                    if (chapter != null) put("chapter", chapter)
                }
                put("body", text)
                // id unik per kiriman: server mengembalikan komentar yang sama jika kiriman diulang (tanpa ganda)
                if (clientId != null) put("clientId", clientId)
            }
        )

    suspend fun edit(id: String, text: String): ApiResponse<CommentEditDto> =
        call("PATCH", listOf("comments", id), body = buildJsonObject { put("body", text) })

    suspend fun delete(id: String): ApiResponse<OkDto> = call("DELETE", listOf("comments", id))

    suspend fun like(id: String, on: Boolean): ApiResponse<LikeResultDto> =
        call(if (on) "PUT" else "DELETE", listOf("comments", id, "like"))

    suspend fun report(id: String, reason: String, note: String): ApiResponse<OkDto> =
        call("POST", listOf("comments", id, "report"), body = buildJsonObject { put("reason", reason); put("note", note) })

    private fun url(segments: List<String>, query: Map<String, String>): HttpUrl =
        base.newBuilder().apply {
            addPathSegment("api")
            addPathSegment("community")
            segments.forEach { addPathSegment(it) }
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

    private suspend inline fun <reified T> call(
        method: String,
        segments: List<String>,
        query: Map<String, String> = emptyMap(),
        body: JsonObject? = null
    ): ApiResponse<T> {
        val (code, text) = execute(method, url(segments, query), body)
        return withContext(Dispatchers.Default) {
            try {
                json.decodeFromString(ApiResponse.serializer(serializer<T>()), text)
            } catch (e: SerializationException) {
                if (code in 200..299) throw e else throw HttpException(code)
            }
        }
    }

    private suspend fun execute(method: String, url: HttpUrl, body: JsonObject?): Pair<Int, String> {
        val payload = when {
            body != null -> body.toString().toRequestBody(jsonType)
            method == "POST" || method == "PUT" || method == "PATCH" -> "{}".toRequestBody(jsonType)
            else -> null
        }
        return executeBody(method, url, payload, http)
    }

    private suspend fun executeBody(method: String, url: HttpUrl, payload: RequestBody?, client: OkHttpClient): Pair<Int, String> = suspendCancellableCoroutine { cont ->
        val builder = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", userAgent)
            .header("X-Device-Id", session.deviceId)
        session.token?.let { builder.header("Authorization", "Bearer $it") }
        val call = client.newCall(builder.method(method, payload).build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                try {
                    cont.resume(code to response.use { it.body?.string().orEmpty() })
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }
        })
    }

    private companion object {
        const val UPLOAD_TIMEOUT_MS = 90_000L
    }
}
