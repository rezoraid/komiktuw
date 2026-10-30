package com.komikta.data.remote.api

import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.ChapterContentDto
import com.komikta.data.remote.dto.ComicDetailDto
import com.komikta.data.remote.dto.ComicListResponse
import com.komikta.data.remote.dto.GenresResponse
import com.komikta.data.remote.dto.SourcesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level API client. All paths are relative to baseUrl.
 * Never invent endpoints — only those verified against the live API.
 */
@Singleton
class KomiktaApi @Inject constructor(
    private val client: HttpClient
) {

    suspend fun getSources(): ApiResponse<SourcesResponse> {
        return client.get("/api/sources").body()
    }

    suspend fun getLatest(source: String, page: Int = 1): ApiResponse<ComicListResponse> {
        return client.get {
            url { appendPathSegments("api", source, "latest") }
            parameter("page", page)
        }.body()
    }

    suspend fun getPopular(source: String): ApiResponse<ComicListResponse> {
        return client.get {
            url { appendPathSegments("api", source, "popular") }
        }.body()
    }

    suspend fun browse(
        source: String,
        page: Int = 1,
        type: String? = null,
        status: String? = null,
        sort: String? = null,
        genres: String? = null
    ): ApiResponse<ComicListResponse> {
        return client.get {
            url { appendPathSegments("api", source, "browse") }
            parameter("page", page)
            type?.let { parameter("type", it) }
            status?.let { parameter("status", it) }
            sort?.let { parameter("sort", it) }
            genres?.let { parameter("genre", it) }
        }.body()
    }

    suspend fun search(source: String, query: String, page: Int = 1): ApiResponse<ComicListResponse> {
        return client.get {
            url { appendPathSegments("api", source, "search") }
            parameter("q", query)
            parameter("page", page)
        }.body()
    }

    suspend fun getGenres(source: String): ApiResponse<GenresResponse> {
        return client.get {
            url { appendPathSegments("api", source, "genres") }
        }.body()
    }

    suspend fun getComicDetail(source: String, slug: String): ApiResponse<ComicDetailDto> {
        return client.get {
            url { appendPathSegments("api", source, "comic", slug) }
        }.body()
    }

    suspend fun getChapterContent(
        source: String,
        comicSlug: String,
        chapterSlug: String
    ): ApiResponse<ChapterContentDto> {
        return client.get {
            url { appendPathSegments("api", source, "chapter", comicSlug, chapterSlug) }
        }.body()
    }
}
