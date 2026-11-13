package com.komikta.data.local

import javax.inject.Inject
import javax.inject.Singleton

data class SourceOption(val id: String, val label: String)

/**
 * Semua sumber (Lumos, Manta, Shinigami) digabung jadi satu daftar: [source] selalu [ALL].
 * Kemampuan tiap sumber di sini mengikuti `/api/sources` pada backend, supaya app tidak memanggil
 * endpoint yang dijawab `501` (popular/genres) atau mengirim filter yang diabaikan sumber.
 *
 *  - lumos     : browse lengkap (type, status, sort, genre), popular, genres
 *  - manta     : browse hanya `type` = manga | comic | novel, tanpa popular/genres
 *  - shinigami : browse hanya `q` + `page`; jenis disaring di sisi app dari field `type`
 */
@Singleton
class SourcePrefs @Inject constructor() {
    val source: String get() = ALL

    companion object {
        /** Id gabungan: Home / Explore / pencarian mengambil dari semua sumber sekaligus. */
        const val ALL = "all"
        const val LUMOS = "lumos"
        const val MANTA = "manta"
        const val SHINIGAMI = "shinigami"

        val SOURCES = listOf(
            SourceOption(LUMOS, "Lumos"),
            SourceOption(MANTA, "Manta"),
            SourceOption(SHINIGAMI, "Shinigami")
        )

        /** Sumber yang punya endpoint `popular`. */
        val POPULAR_SOURCES = listOf(LUMOS)

        /** Sumber yang punya endpoint `genres` dan filter genre/status/urutan di browse. */
        const val GENRE_SOURCE = LUMOS
        val FULL_FILTER_SOURCES = setOf(LUMOS)

        /** Jenis yang disaring langsung oleh server per sumber (sumber lain: disaring di app atau dilewati). */
        val SERVER_TYPES: Map<String, Set<String>> = mapOf(
            LUMOS to setOf("manga", "manhwa", "manhua"),
            MANTA to setOf("manga")
        )

        /**
         * Host gambar sampul yang perlu lewat image proxy backend (referer wajib).
         * Lumos: CDN gambarnya (lms.imgsvr.my.id) dan semua mirror situsnya menolak permintaan tanpa referer,
         * dan proxy backend punya cadangan otomatis (CDN <-> proxy situs <-> mirror lain).
         */
        val PROXIED_IMAGE_HOSTS = listOf(
            "shngm.id", "shngm.io", "shinigami.id", "shinigami.asia",
            "lms.imgsvr.my.id", "lumosgg.com", "lumoskomik.com"
        )

        /** True kalau [host] (atau subdomain-nya) harus diambil lewat image proxy backend. */
        fun isProxiedImageHost(host: String): Boolean =
            PROXIED_IMAGE_HOSTS.any { host == it || host.endsWith(".$it") }
    }
}
