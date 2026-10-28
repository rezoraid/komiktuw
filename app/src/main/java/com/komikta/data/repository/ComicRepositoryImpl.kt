package com.komikta.data.repository

import com.komikta.core.common.Result
import com.komikta.data.remote.api.KomiktaApi
import com.komikta.data.remote.dto.toDomain
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.SearchParams
import com.komikta.domain.model.Source
import com.komikta.domain.repository.ComicRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicRepositoryImpl @Inject constructor(
    private val api: KomiktaApi
) : ComicRepository {

    override suspend fun getSources(): Result<List<Source>> = safeApiCall {
        val response = api.getSources()
        if (!response.ok || response.data == null) {
            return@safeApiCall Result.Error(
                message = response.error?.message ?: "Failed to fetch sources",
                code = response.error?.code
            )
        }
        val items = response.data.items.map { it.toDomain() }
        if (items.isEmpty()) Result.Empty else Result.Success(items)
    }

    override suspend fun getLatest(source: String, page: Int): Result<List<Comic>> = safeApiCall {
        val response = api.getLatest(source, page)
        mapListResponse(response, source)
    }

    override suspend fun getPopular(source: String): Result<List<Comic>> = safeApiCall {
        val response = api.getPopular(source)
        mapListResponse(response, source)
    }

    override suspend fun browse(params: BrowseParams): Result<List<Comic>> = safeApiCall {
        val response = api.browse(
            source = params.source,
            page = params.page,
            type = params.type,
            status = params.status,
            sort = params.sort,
            genres = params.genres
        )
        mapListResponse(response, params.source)
    }

    override suspend fun search(params: SearchParams): Result<List<Comic>> = safeApiCall {
        val response = api.search(params.source, params.query, params.page)
        mapListResponse(response, params.source)
    }

    override suspend fun getGenres(source: String): Result<List<String>> = safeApiCall {
        val response = api.getGenres(source)
        if (!response.ok || response.data == null) {
            return@safeApiCall Result.Error(
                message = response.error?.message ?: "Failed to fetch genres",
                code = response.error?.code
            )
        }
        val items = response.data.items
        if (items.isEmpty()) Result.Empty else Result.Success(items)
    }

    override suspend fun getComicDetail(source: String, slug: String): Result<ComicDetail> =
        safeApiCall {
            val response = api.getComicDetail(source, slug)
            if (!response.ok || response.data == null) {
                return@safeApiCall Result.Error(
                    message = response.error?.message ?: "Comic not found",
                    code = response.error?.code
                )
            }
            Result.Success(response.data.toDomain(source))
        }

    override suspend fun getChapterContent(
        source: String,
        comicSlug: String,
        chapterSlug: String
    ): Result<ChapterContent> = safeApiCall {
        val response = api.getChapterContent(source, comicSlug, chapterSlug)
        if (!response.ok || response.data == null) {
            return@safeApiCall Result.Error(
                message = response.error?.message ?: "Chapter not found",
                code = response.error?.code
            )
        }
        Result.Success(response.data.toDomain(source, comicSlug, chapterSlug))
    }

    private fun mapListResponse(
        response: com.komikta.data.remote.dto.ApiResponse<com.komikta.data.remote.dto.ComicListResponse>,
        source: String
    ): Result<List<Comic>> {
        if (!response.ok || response.data == null) {
            return Result.Error(
                message = response.error?.message ?: "Failed to fetch comics",
                code = response.error?.code
            )
        }
        val items = response.data.items.map { it.toDomain(source) }
        return if (items.isEmpty()) Result.Empty else Result.Success(items)
    }

    private suspend fun <T> safeApiCall(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e // Never swallow cancellation
        } catch (e: SocketTimeoutException) {
            Timber.w(e, "Request timeout")
            Result.Error(message = "Request timeout. Please try again.", throwable = e)
        } catch (e: UnknownHostException) {
            Timber.w(e, "No internet connection")
            Result.Error(message = "No internet connection.", throwable = e)
        } catch (e: IOException) {
            Timber.w(e, "Network error")
            Result.Error(message = "Network error. Please check your connection.", throwable = e)
        } catch (e: ClientRequestException) {
            Timber.w(e, "Client error ${e.response.status}")
            Result.Error(
                message = "Request failed (${e.response.status.value})",
                code = e.response.status.value.toString(),
                throwable = e
            )
        } catch (e: ServerResponseException) {
            Timber.e(e, "Server error ${e.response.status}")
            Result.Error(
                message = "Server error. Please try again later.",
                code = e.response.status.value.toString(),
                throwable = e
            )
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error")
            Result.Error(message = e.message ?: "Unexpected error occurred", throwable = e)
        }
    }
}
