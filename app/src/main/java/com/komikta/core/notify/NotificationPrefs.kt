package com.komikta.core.notify

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Pengaturan notifikasi update chapter (disimpan di perangkat). Bawaan: aktif. */
@Singleton
class NotificationPrefs @Inject constructor(@ApplicationContext private val context: Context) {
    private val sp = context.getSharedPreferences("notify_prefs", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(sp.getBoolean(KEY_ENABLED, true))
    val enabledFlow: StateFlow<Boolean> = _enabled.asStateFlow()
    val enabled: Boolean get() = _enabled.value

    /** Izin notifikasi (Android 13+) cukup diminta otomatis sekali; selanjutnya lewat tombol lonceng. */
    var permissionAsked: Boolean
        get() = sp.getBoolean(KEY_ASKED, false)
        set(v) = sp.edit().putBoolean(KEY_ASKED, v).apply()

    /** ID pengumuman terakhir yang sudah diterima HP ini. Null = belum pernah dicek (posisi awal dicatat tanpa notifikasi). */
    var broadcastCursor: String?
        get() = sp.getString(KEY_CURSOR, null)
        set(v) = sp.edit().putString(KEY_CURSOR, v).apply()

    private val shownLock = Any()

    fun markShown(id: String): Boolean {
        synchronized(shownLock) {
            val ids = sp.getString(KEY_SHOWN, "").orEmpty().split(',').filter { it.isNotEmpty() }
            if (id in ids) return false
            sp.edit().putString(KEY_SHOWN, (ids + id).takeLast(MAX_SHOWN).joinToString(",")).commit()
            return true
        }
    }

    var pushKey: String?
        get() = sp.getString(KEY_PUSH_KEY, null)
        set(v) = sp.edit().putString(KEY_PUSH_KEY, v).apply()

    var pushToken: String?
        get() = sp.getString(KEY_PUSH_TOKEN, null)
        set(v) = sp.edit().putString(KEY_PUSH_TOKEN, v).apply()

    var pushSyncedAt: Long
        get() = sp.getLong(KEY_PUSH_AT, 0L)
        set(v) = sp.edit().putLong(KEY_PUSH_AT, v).apply()

    fun setEnabled(value: Boolean) {
        sp.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
        UpdateScheduler.sync(context, value)
        PushSync.schedule(context)
    }

    private companion object {
        const val KEY_ENABLED = "updates_enabled"
        const val KEY_ASKED = "permission_asked"
        const val KEY_CURSOR = "broadcast_cursor"
        const val KEY_SHOWN = "broadcast_shown"
        const val KEY_PUSH_KEY = "push_key"
        const val KEY_PUSH_TOKEN = "push_token"
        const val KEY_PUSH_AT = "push_at"
        const val MAX_SHOWN = 50
    }
}

/** Chapter terbaru yang terakhir dilihat per bookmark, supaya hanya chapter yang benar-benar baru yang diberitahukan. */
@Singleton
class UpdateState @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("update_state", Context.MODE_PRIVATE)

    data class Seen(val topSlug: String, val count: Int)

    fun get(id: String): Seen? = sp.getString(id, null)?.split('\t')?.takeIf { it.size == 2 }?.let { p ->
        p[1].toIntOrNull()?.let { Seen(p[0], it) }
    }

    fun put(id: String, seen: Seen) = sp.edit().putString(id, "${seen.topSlug}\t${seen.count}").apply()

    /** Buang catatan komik yang bookmark-nya sudah dihapus. */
    fun retainOnly(ids: Set<String>) {
        val stale = sp.all.keys - ids
        if (stale.isNotEmpty()) sp.edit().apply { stale.forEach(::remove) }.apply()
    }
}

/**
 * Chapter yang lebih baru dari [previous]. [chapters] diurutkan terbaru -> terlama.
 * Tanpa catatan sebelumnya (pengecekan pertama) tidak ada yang dianggap baru.
 */
fun freshChapters(chapters: List<com.komikta.data.remote.dto.ChapterSummaryDto>, previous: UpdateState.Seen?): List<com.komikta.data.remote.dto.ChapterSummaryDto> {
    val top = chapters.firstOrNull() ?: return emptyList()
    if (previous == null || previous.topSlug == top.slug) return emptyList()
    val idx = chapters.indexOfFirst { it.slug == previous.topSlug }
    return when {
        idx > 0 -> chapters.take(idx)
        // chapter terakhir yang dicatat sudah tidak ada di daftar: pakai selisih jumlah
        idx == -1 && chapters.size > previous.count -> chapters.take(chapters.size - previous.count)
        else -> emptyList()
    }
}
