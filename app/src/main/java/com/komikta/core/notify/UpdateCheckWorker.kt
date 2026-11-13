package com.komikta.core.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.remote.api.CommunityApi
import com.komikta.data.remote.api.KomiktaApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Mengecek bookmark ke backend. Chapter terbaru dibandingkan dengan yang terakhir dilihat;
 * kalau ada yang baru, kirim notifikasi. Pengecekan pertama untuk sebuah komik hanya mencatat
 * posisi awal (tidak ada notifikasi), supaya bookmark lama tidak membanjiri notifikasi.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun api(): KomiktaApi
        fun community(): CommunityApi
        fun bookmarks(): BookmarkDao
        fun history(): ReadingHistoryDao
        fun prefs(): NotificationPrefs
        fun state(): UpdateState
        fun images(): ImageLoader
    }

    override suspend fun doWork(): Result {
        val d = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        if (!d.prefs().enabled) return Result.success()

        checkBroadcasts(d)

        val bookmarks = d.bookmarks().getRecent(MAX_COMICS)
        d.state().retainOnly(bookmarks.map { it.id }.toSet())
        if (bookmarks.isEmpty()) return Result.success()

        val gate = Semaphore(PARALLEL)
        val posted = coroutineScope {
            bookmarks.map { b -> async { gate.withPermit { checkOne(d, b) } } }.awaitAll()
        }.filterNotNull()
        UpdateNotifier.postSummary(applicationContext, posted)
        return Result.success()
    }

    /**
     * Pengumuman dari OWNER. Pengecekan pertama di HP ini hanya mencatat posisi terbaru (pengumuman lama tidak
     * dikirim ulang). Selanjutnya hanya pengumuman baru, dan yang lebih tua dari 3 hari dilewati.
     */
    private suspend fun checkBroadcasts(d: Deps) {
        try {
            val prefs = d.prefs()
            val cursor = prefs.broadcastCursor
            val response = d.community().broadcastFeed(cursor)
            val data = response.data
            if (!response.ok || data == null) return
            if (cursor == null) {
                prefs.broadcastCursor = data.latestId.ifBlank { null }
                return
            }
            val now = System.currentTimeMillis()
            data.items.filter { now - it.createdAt <= BROADCAST_MAX_AGE_MS }.takeLast(MAX_BROADCASTS).filter { prefs.markShown(it.id) }.forEach {
                UpdateNotifier.postBroadcast(applicationContext, it)
            }
            data.items.lastOrNull()?.let { prefs.broadcastCursor = it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // jaringan/server bermasalah: dicoba lagi di putaran berikutnya
        }
    }

    /** Mengembalikan judul komik kalau notifikasi dikirim. Gagal satu komik tidak menggagalkan yang lain. */
    private suspend fun checkOne(d: Deps, b: BookmarkEntity): String? = try {
        val chapters = d.api().getComicDetail(b.source, b.comicSlug).data?.chapters.orEmpty() // terbaru -> terlama
        val top = chapters.firstOrNull()
        if (top == null || top.slug.isBlank()) null
        else {
            val previous = d.state().get(b.id)
            d.state().put(b.id, UpdateState.Seen(top.slug, chapters.size))
            val fresh = freshChapters(chapters, previous)
            // Sudah dibaca langsung dari aplikasi sebelum pengecekan ini: tidak perlu diberitahu.
            val alreadyRead = d.history().getById(b.id)?.lastChapterSlug == top.slug
            if (fresh.isEmpty() || alreadyRead) null
            else {
                val labels = fresh.mapNotNull { it.number?.replace(Regex("(?i)^\\s*(chapter|ch\\.?)\\s*"), "")?.trim()?.takeIf(String::isNotEmpty) }
                UpdateNotifier.postUpdate(applicationContext, b, fresh.size, labels, UpdateNotifier.loadCover(applicationContext, d.images(), b.coverUrl))
                b.comicTitle
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val MAX_COMICS = 40
        const val PARALLEL = 3
        const val MAX_BROADCASTS = 5
        const val BROADCAST_MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000
    }
}
