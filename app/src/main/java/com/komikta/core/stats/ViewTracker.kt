package com.komikta.core.stats

import com.komikta.data.remote.api.CommunityApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengirim aktivitas baca ke database Komikta: satu "view" komik saat Detail dibuka dan satu "view"
 * chapter saat chapter dibaca. Angka yang tampil di aplikasi berasal dari sini, bukan dari statistik sumber.
 *
 * Ringan: tanpa menunggu (fire-and-forget di latar belakang), request yang sama tidak dikirim ulang dalam
 * [WINDOW_MS], dan kegagalan jaringan diabaikan (dicoba lagi pada pembukaan berikutnya). Penghitungan akhir
 * dan anti-spam per perangkat/akun dilakukan server.
 */
@Singleton
class ViewTracker @Inject constructor(private val api: CommunityApi) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recent = HashMap<String, Long>()

    private fun shouldSend(key: String): Boolean = synchronized(recent) {
        val now = System.currentTimeMillis()
        if (recent.size > 200) recent.entries.removeAll { now - it.value > WINDOW_MS }
        val last = recent[key]
        if (last != null && now - last < WINDOW_MS) false else { recent[key] = now; true }
    }

    private fun forget(key: String) = synchronized(recent) { recent.remove(key) }

    /** [onCount] dipanggil dengan total views terbaru dari server (jika server mengirimnya). */
    fun comic(source: String, slug: String, onCount: (Long) -> Unit = {}) {
        if (source.isBlank() || slug.isBlank()) return
        val key = "c|$source|$slug"
        if (!shouldSend(key)) return
        scope.launch {
            try {
                val r = api.trackComicView(source, slug)
                val count = r.data?.viewCount
                if (!r.ok) forget(key) else if (count != null && count > 0) onCount(count)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                forget(key)
            }
        }
    }

    fun chapter(source: String, comicSlug: String, chapterSlug: String) {
        if (source.isBlank() || comicSlug.isBlank() || chapterSlug.isBlank()) return
        val key = "ch|$source|$comicSlug|$chapterSlug"
        if (!shouldSend(key)) return
        scope.launch {
            try {
                if (!api.trackChapterView(source, comicSlug, chapterSlug).ok) forget(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                forget(key)
            }
        }
    }

    private companion object {
        const val WINDOW_MS = 30 * 60 * 1000L
    }
}
