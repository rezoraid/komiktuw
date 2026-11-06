package com.komikta.data.repository

import com.komikta.core.cache.ComicPreviewCache
import com.komikta.core.cache.ResponseCache
import com.komikta.core.common.Result
import com.komikta.core.network.HttpException
import com.komikta.data.remote.api.KomiktaApi
import com.komikta.data.remote.dto.ApiResponse
import com.komikta.data.remote.dto.ComicListResponse
import com.komikta.data.remote.dto.toDomain
import com.komikta.domain.model.BrowseParams
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.SearchParams
import com.komikta.domain.model.Source
import com.komikta.domain.repository.ComicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicRepositoryImpl @Inject constructor(
    private val api: KomiktaApi,
    private val cache: ResponseCache
) : ComicRepository {

    private val comicList = ListSerializer(Comic.serializer())
    private val sourceList = ListSerializer(Source.serializer())
    private val stringList = ListSerializer(String.serializer())

    // Setiap daftar komik yang tampil dicatat ringan di memori agar Detail bisa menampilkan sampul + judul seketika.
    private fun <T> note(items: List<T>) = ComicPreviewCache.remember(items.filterIsInstance<Comic>())

    private fun <T> listResult(items: List<T>): Result<List<T>> {
        note(items)
        return if (items.isEmpty()) Result.Empty else Result.Success(items)
    }

    override suspend fun getSources(): Result<List<Source>> =
        cache.load("sources", sourceList, TTL_STATIC) {
            guarded {
                val response = api.getSources()
                val data = response.data
                if (!response.ok || data == null) failure(response, "Failed to fetch sources")
                else Result.Success(data.items.map { it.toDomain() })
            }
        }.let { r -> if (r is Result.Success && r.data.isEmpty()) Result.Empty else r }

    override suspend fun getLatest(source: String, page: Int): Result<List<Comic>> =
        cache.load(latestKey(source, page), comicList, TTL_LIST) { fetchLatest(source, page) }.let(::emptyAware)

    override suspend fun getPopular(source: String): Result<List<Comic>> =
        cache.load(popularKey(source), comicList, TTL_LIST) { fetchPopular(source) }.let(::emptyAware)

    override suspend fun browse(params: BrowseParams): Result<List<Comic>> =
        cache.load(browseKey(params), comicList, TTL_LIST) { fetchBrowse(params) }.let(::emptyAware)

    override suspend fun search(params: SearchParams): Result<List<Comic>> =
        cache.load(searchKey(params), comicList, TTL_SEARCH) { fetchSearch(params) }.let(::emptyAware)

    override suspend fun getGenres(source: String): Result<List<String>> =
        cache.load(genresKey(source), stringList, TTL_STATIC) { fetchGenres(source) }.let(::emptyAware)

    override suspend fun getComicDetail(source: String, slug: String): Result<ComicDetail> =
        cache.load(detailKey(source, slug), ComicDetail.serializer(), TTL_DETAIL) { fetchDetail(source, slug) }

    override suspend fun getChapterContent(source: String, comicSlug: String, chapterSlug: String): Result<ChapterContent> =
        cache.load(chapterKey(source, comicSlug, chapterSlug), ChapterContent.serializer(), TTL_CHAPTER) {
            fetchChapter(source, comicSlug, chapterSlug)
        }

    override fun observeLatest(source: String, page: Int, refresh: Boolean): Flow<Result<List<Comic>>> =
        cache.stream(latestKey(source, page), comicList, TTL_LIST, refresh, ::listResult) { fetchLatest(source, page) }

    override fun observePopular(source: String, refresh: Boolean): Flow<Result<List<Comic>>> =
        cache.stream(popularKey(source), comicList, TTL_LIST, refresh, ::listResult) { fetchPopular(source) }

    override fun observeBrowse(params: BrowseParams, refresh: Boolean): Flow<Result<List<Comic>>> =
        cache.stream(browseKey(params), comicList, TTL_LIST, refresh, ::listResult) { fetchBrowse(params) }

    override fun observeSearch(params: SearchParams): Flow<Result<List<Comic>>> =
        cache.stream(searchKey(params), comicList, TTL_SEARCH, false, ::listResult) { fetchSearch(params) }

    override fun observeGenres(source: String): Flow<Result<List<String>>> =
        cache.stream(genresKey(source), stringList, TTL_STATIC, false, ::listResult) { fetchGenres(source) }

    override fun observeDetail(source: String, slug: String, refresh: Boolean): Flow<Result<ComicDetail>> =
        cache.stream(detailKey(source, slug), ComicDetail.serializer(), TTL_DETAIL, refresh) { fetchDetail(source, slug) }

    private fun <T> emptyAware(result: Result<List<T>>): Result<List<T>> {
        if (result is Result.Success) note(result.data)
        return if (result is Result.Success && result.data.isEmpty()) Result.Empty else result
    }

    private fun latestKey(source: String, page: Int) = "latest|$source|$page"
    private fun popularKey(source: String) = "popular|$source"
    private fun genresKey(source: String) = "genres|$source"
    private fun detailKey(source: String, slug: String) = "detail|$source|$slug"
    private fun chapterKey(source: String, comic: String, chapter: String) = "chapter|$source|$comic|$chapter"
    private fun searchKey(p: SearchParams) = "search|${p.source}|${p.query.trim().lowercase()}|${p.page}"
    private fun browseKey(p: BrowseParams) = "browse|${p.source}|${p.page}|${p.type}|${p.status}|${p.sort}|${p.genres}"

    private suspend fun fetchLatest(source: String, page: Int): Result<List<Comic>> =
        guarded { mapList(api.getLatest(source, page), source) }

    private suspend fun fetchPopular(source: String): Result<List<Comic>> =
        guarded { mapList(api.getPopular(source), source) }

    private suspend fun fetchBrowse(params: BrowseParams): Result<List<Comic>> = guarded {
        mapList(
            api.browse(params.source, params.page, params.type, params.status, params.sort, params.genres),
            params.source
        )
    }

    private suspend fun fetchSearch(params: SearchParams): Result<List<Comic>> =
        guarded { mapList(api.search(params.source, params.query, params.page), params.source) }

    private suspend fun fetchGenres(source: String): Result<List<String>> = guarded {
        val response = api.getGenres(source)
        val data = response.data
        if (!response.ok || data == null) failure(response, "Failed to fetch genres") else Result.Success(data.items)
    }

    private suspend fun fetchDetail(source: String, slug: String): Result<ComicDetail> = guarded {
        val response = api.getComicDetail(source, slug)
        val data = response.data
        if (!response.ok || data == null) failure(response, "Comic not found") else Result.Success(data.toDomain(source))
    }

    private suspend fun fetchChapter(source: String, comic: String, chapter: String): Result<ChapterContent> = guarded {
        val response = api.getChapterContent(source, comic, chapter)
        val data = response.data
        if (!response.ok || data == null) failure(response, "Chapter not found")
        else Result.Success(data.toDomain(source, comic, chapter))
    }

    private fun mapList(response: ApiResponse<ComicListResponse>, source: String): Result<List<Comic>> {
        val data = response.data
        if (!response.ok || data == null) return failure(response, "Failed to fetch comics")
        return Result.Success(data.items.map { it.toDomain(source) })
    }

    private fun failure(response: ApiResponse<*>, fallback: String): Result.Error =
        Result.Error(message = response.error?.message ?: fallback, code = response.error?.code)

    private suspend fun <T> guarded(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        Result.Error(message = "Request timeout. Please try again.", throwable = e)
    } catch (e: UnknownHostException) {
        Result.Error(message = "No internet connection.", throwable = e)
    } catch (e: HttpException) {
        if (e.code >= 500) Result.Error(message = "Server error. Please try again later.", code = e.code.toString(), throwable = e)
        else Result.Error(message = "Request failed (${e.code})", code = e.code.toString(), throwable = e)
    } catch (e: IOException) {
        Result.Error(message = "Network error. Please check your connection.", throwable = e)
    } catch (e: Exception) {
        Result.Error(message = e.message ?: "Unexpected error occurred", throwable = e)
    }

    private companion object {
        val TTL_LIST = TimeUnit.MINUTES.toMillis(2)
        val TTL_SEARCH = TimeUnit.MINUTES.toMillis(5)
        val TTL_DETAIL = TimeUnit.MINUTES.toMillis(10)
        val TTL_CHAPTER = TimeUnit.HOURS.toMillis(6)
        val TTL_STATIC = TimeUnit.HOURS.toMillis(12)
    }
}
