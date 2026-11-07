package com.komikta.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Source(
    val id: String,
    val name: String,
    val features: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val sorts: List<String> = emptyList()
)

@Serializable
data class Comic(
    val title: String,
    val slug: String,
    val coverUrl: String? = null,
    val type: String? = null,
    val status: String? = null,
    val rating: Double? = null,
    val chapterCount: Int? = null,
    val genres: List<String> = emptyList(),
    val source: String,
    val author: String? = null,
    /** Views dari database Komikta (aktivitas pengguna), bukan statistik sumber. Null = belum ada, tidak ditampilkan. */
    val viewCount: Long? = null,
    val chapters: List<Chapter> = emptyList()
)

@Serializable
data class ComicDetail(
    val title: String,
    val slug: String,
    val coverUrl: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val synopsis: String? = null,
    val status: String? = null,
    val type: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val year: String? = null,
    val viewCount: Long? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val totalChapters: Int? = null,
    val chapters: List<Chapter> = emptyList(),
    val source: String
)

@Serializable
data class Chapter(
    val slug: String,
    val number: String? = null,
    val title: String? = null,
    val timeAgo: String? = null,
    /** Views chapter dari database Komikta; null/0 = tidak ditampilkan. */
    val viewCount: Long? = null
)

@Serializable
data class ChapterContent(
    val title: String? = null,
    val comicSlug: String,
    val chapterSlug: String,
    val imageUrls: List<String> = emptyList(),
    val proxyPaths: List<String> = emptyList(),
    val prevChapterSlug: String? = null,
    val nextChapterSlug: String? = null,
    val source: String
)

data class BrowseParams(
    val source: String,
    val page: Int = 1,
    val type: String? = null,
    val status: String? = null,
    val sort: String? = null,
    val genres: String? = null
)

data class SearchParams(
    val source: String,
    val query: String,
    val page: Int = 1
)

private fun Comic.key() = "$source:$slug"

/** Kunci unik komik lintas sumber. */
val Comic.uid: String get() = key()

/**
 * Data yang tampil di kartu belum lengkap (rating, genre, jenis, atau chapter terbaru).
 * Views tidak ikut dihitung: views berasal dari database Komikta, bukan dari detail sumber.
 */
val Comic.isIncomplete: Boolean
    get() = rating == null || genres.isEmpty() || type == null || chapters.isEmpty()

/**
 * Lengkapi field yang kosong dari detail komik. Data yang sudah ada dari daftar tidak ditimpa,
 * jadi hasilnya hanya menambah kelengkapan, tidak mengubah apa yang sudah benar.
 */
fun Comic.completedWith(d: ComicDetail): Comic = copy(
    coverUrl = coverUrl ?: d.coverUrl,
    type = type ?: canonicalType(d.type, d.genres),
    status = status ?: canonicalStatus(d.status),
    rating = rating ?: d.rating?.takeIf { it > 0.0 },
    viewCount = viewCount ?: d.viewCount?.takeIf { it > 0L },
    genres = genres.ifEmpty { canonicalGenres(d.genres) },
    author = author ?: d.author?.trim()?.takeIf { it.isNotEmpty() },
    chapterCount = chapterCount ?: d.totalChapters,
    chapters = chapters.ifEmpty { d.chapters.take(3) }
)

/** Pakai ulang hasil pengayaan lama pada daftar yang baru dimuat ulang dari server/cache. */
fun Comic.fillMissingFrom(other: Comic): Comic = copy(
    coverUrl = coverUrl ?: other.coverUrl,
    type = type ?: other.type,
    status = status ?: other.status,
    rating = rating ?: other.rating,
    viewCount = viewCount ?: other.viewCount,
    genres = genres.ifEmpty { other.genres },
    author = author ?: other.author,
    chapterCount = chapterCount ?: other.chapterCount,
    chapters = chapters.ifEmpty { other.chapters }
)

/** Ganti komik yang uid-nya sama dengan [c] (dipakai saat data komik dilengkapi menyusul). */
fun List<Comic>.replacing(c: Comic): List<Comic> = map { if (it.uid == c.uid) c else it }
