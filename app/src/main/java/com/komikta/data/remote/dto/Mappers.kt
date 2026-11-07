package com.komikta.data.remote.dto

import com.komikta.domain.model.Chapter
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.Source
import com.komikta.domain.model.canonicalGenres
import com.komikta.domain.model.canonicalStatus
import com.komikta.domain.model.canonicalType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import com.komikta.core.network.ApiConfig

fun SourceDto.toDomain(): Source = Source(
    id = id,
    name = name,
    features = features,
    types = filters?.types.orEmpty(),
    statuses = filters?.statuses.orEmpty(),
    sorts = filters?.sorts.orEmpty()
)

fun ComicListItemDto.toDomain(source: String): Comic = Comic(
    title = title,
    slug = slug,
    coverUrl = absoluteUrl(cover),
    type = canonicalType(type, genres),
    status = canonicalStatus(status),
    rating = rating?.takeIf { it > 0.0 },
    chapterCount = chapterCount,
    genres = canonicalGenres(genres),
    source = source,
    author = author?.trim()?.takeIf { it.isNotEmpty() },
    artist = artist?.trim()?.takeIf { it.isNotEmpty() },
    year = year?.trim()?.takeIf { it.isNotEmpty() },
    viewCount = viewCount.toCount(),
    chapters = chapters.map { it.toDomain() }
)

/** Uploaded cover/gambar dari server dikirim sebagai path ("/media/..."): jadikan URL penuh. */
fun absoluteUrl(url: String?): String? {
    val u = url?.trim().orEmpty()
    if (u.isEmpty()) return null
    return if (u.startsWith("/")) ApiConfig.BASE_URL.trimEnd('/') + u else u
}

/** Jumlah views/dilihat dari server Komikta: angka bulat positif, selain itu dianggap tidak ada. */
internal fun JsonElement?.toCount(): Long? {
    val p = this as? JsonPrimitive ?: return null
    val n = p.doubleOrNull ?: p.content.trim().toDoubleOrNull() ?: return null
    return n.toLong().takeIf { it > 0L }
}

fun ChapterSummaryDto.toDomain(): Chapter = Chapter(
    slug = slug,
    number = number,
    title = title,
    timeAgo = timeAgo,
    viewCount = viewCount.toCount()
)

fun ComicDetailDto.toDomain(source: String): ComicDetail = ComicDetail(
    title = title,
    slug = slug,
    coverUrl = absoluteUrl(cover),
    alternativeTitles = alt,
    synopsis = synopsis,
    status = canonicalStatus(status),
    type = canonicalType(type, genres),
    author = author,
    artist = artist,
    year = year,
    viewCount = viewCount.toCount(),
    rating = rating?.takeIf { it > 0.0 },
    genres = canonicalGenres(genres),
    totalChapters = totalChapters,
    chapters = chapters.map { it.toDomain() },
    source = source
)

fun ChapterContentDto.toDomain(source: String, comicSlug: String, chapterSlug: String): ChapterContent {
    // Prefer proxy paths when present (often faster / fewer hotlink blocks)
    val urls = when {
        proxy.isNotEmpty() -> proxy.map { path ->
            if (path.startsWith("http")) path
            else ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
        else -> images.map { absoluteUrl(it) ?: it }
    }
    return ChapterContent(
        title = title,
        comicSlug = comicSlug,
        chapterSlug = chapterSlug,
        imageUrls = urls,
        proxyPaths = proxy,
        prevChapterSlug = prev,
        nextChapterSlug = next,
        source = source
    )
}
