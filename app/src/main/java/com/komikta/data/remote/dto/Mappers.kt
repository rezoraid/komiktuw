package com.komikta.data.remote.dto

import com.komikta.domain.model.Chapter
import com.komikta.domain.model.ChapterContent
import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail
import com.komikta.domain.model.Source

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
    coverUrl = cover,
    type = type,
    status = status,
    rating = rating,
    chapterCount = chapterCount,
    genres = genres,
    source = source
)

fun ChapterSummaryDto.toDomain(): Chapter = Chapter(
    slug = slug,
    number = number,
    title = title,
    timeAgo = timeAgo
)

fun ComicDetailDto.toDomain(source: String): ComicDetail = ComicDetail(
    title = title,
    slug = slug,
    coverUrl = cover,
    alternativeTitles = alt,
    synopsis = synopsis,
    status = status,
    type = type,
    author = author,
    artist = artist,
    year = year,
    views = views,
    rating = rating,
    genres = genres,
    totalChapters = totalChapters,
    chapters = chapters.map { it.toDomain() },
    source = source
)

fun ChapterContentDto.toDomain(source: String, comicSlug: String, chapterSlug: String): ChapterContent =
    ChapterContent(
        title = title,
        comicSlug = comicSlug,
        chapterSlug = chapterSlug,
        imageUrls = images,
        proxyPaths = proxy,
        prevChapterSlug = prev,
        nextChapterSlug = next,
        source = source
    )
