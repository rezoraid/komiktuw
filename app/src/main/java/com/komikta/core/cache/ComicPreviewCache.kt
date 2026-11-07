package com.komikta.core.cache

import com.komikta.domain.model.Comic
import com.komikta.domain.model.ComicDetail

/**
 * Ingatan ringan (hanya memori) untuk komik yang pernah tampil di daftar (Home, Explore, pencarian).
 * Saat Detail dibuka, sampul + judul + info dasar langsung tampil dari sini sementara detail lengkap
 * (sinopsis, daftar chapter) masih diambil, jadi tidak ada layar kosong/loading penuh.
 */
object ComicPreviewCache {
    private const val MAX = 400
    private val map = object : LinkedHashMap<String, Comic>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Comic>?) = size > MAX
    }

    fun remember(items: List<Comic>) {
        if (items.isEmpty()) return
        synchronized(map) { items.forEach { map["${it.source}|${it.slug}"] = it } }
    }

    /** Ganti sampul/judul/info komik yang sudah diingat (dipakai setelah OWNER menyimpan perubahan). */
    fun forget(source: String, slug: String) {
        synchronized(map) { map.remove("$source|$slug") }
    }

    fun detailPreview(source: String, slug: String): ComicDetail? {
        val c = synchronized(map) { map["$source|$slug"] } ?: return null
        return ComicDetail(
            title = c.title,
            slug = c.slug,
            coverUrl = c.coverUrl,
            status = c.status,
            type = c.type,
            author = c.author,
            viewCount = c.viewCount,
            rating = c.rating,
            genres = c.genres,
            totalChapters = c.chapterCount,
            chapters = c.chapters,
            source = c.source
        )
    }
}
