package com.komikta.feature.profile

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache memori halaman profil (statistik + daftar aktivitas per tab) per pengguna.
 * Membuka profil lagi langsung menampilkan data terakhir; penyegaran berjalan diam-diam.
 */
@Singleton
class ProfileCache @Inject constructor() {
    class Entry(val state: ProfilePageState, val statsAt: Long)

    private val map = object : LinkedHashMap<String, Entry>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > MAX_ENTRIES
    }

    @Synchronized fun get(id: String): Entry? = map[id]

    @Synchronized fun put(id: String, entry: Entry) { map[id] = entry }

    /** Buang entri agar buka profil berikutnya mengambil statistik & aktivitas terbaru (setelah hapus/post komentar, XP, dll). */
    @Synchronized fun invalidate(id: String) { map.remove(id) }

    private companion object { const val MAX_ENTRIES = 8 }
}
