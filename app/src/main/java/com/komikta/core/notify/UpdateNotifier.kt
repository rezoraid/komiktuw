package com.komikta.core.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.komikta.MainActivity
import com.komikta.R
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.remote.dto.BroadcastDto

object UpdateNotifier {
    const val CHANNEL_ID = "chapter_updates"
    const val ANNOUNCE_CHANNEL_ID = "announcements"
    const val EXTRA_SOURCE = "komikta.open.source"
    const val EXTRA_SLUG = "komikta.open.slug"
    private const val GROUP = "komikta_updates"
    private const val SUMMARY_ID = 1

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Update chapter", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pemberitahuan saat komik di bookmark punya chapter baru"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(ANNOUNCE_CHANNEL_ID, "Pengumuman", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pengumuman dari tim Komikta"
            }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** [labels] = nomor chapter baru, terbaru dulu (boleh kosong kalau nomornya tidak diketahui). */
    @SuppressLint("MissingPermission") // sudah dicek di canPost()
    fun postUpdate(context: Context, bookmark: BookmarkEntity, count: Int, labels: List<String>, cover: Bitmap?) {
        if (!canPost(context)) return
        ensureChannel(context)
        val text = when {
            count <= 1 -> labels.firstOrNull()?.let { "Chapter $it sudah rilis" } ?: "Chapter baru sudah rilis"
            labels.size >= 2 -> "$count chapter baru (Chapter ${labels.last()} – ${labels.first()})"
            else -> "$count chapter baru sudah rilis"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(bookmark.comicTitle)
            .setContentText(text)
            .setLargeIcon(cover)
            .setGroup(GROUP)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent(context, bookmark))
            .build()
        NotificationManagerCompat.from(context).notify(bookmark.id.hashCode(), notification)
    }

    suspend fun loadCover(context: Context, loader: coil.ImageLoader, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return kotlinx.coroutines.withTimeoutOrNull(6_000) {
            val result = loader.execute(coil.request.ImageRequest.Builder(context).data(url).size(256, 256).allowHardware(false).build())
            (result as? coil.request.SuccessResult)?.drawable?.toBitmap(256, 256)
        }
    }

    /** Pengumuman dari OWNER. Diketuk: buka komik yang ditautkan, atau aplikasi saja kalau tidak ada tautan. */
    @SuppressLint("MissingPermission")
    fun postBroadcast(context: Context, b: BroadcastDto) {
        if (!canPost(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (b.source.isNotBlank() && b.slug.isNotBlank()) {
                putExtra(EXTRA_SOURCE, b.source)
                putExtra(EXTRA_SLUG, b.slug)
            }
        }
        val notification = NotificationCompat.Builder(context, ANNOUNCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(b.title)
            .setContentText(b.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(b.body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(PendingIntent.getActivity(context, b.id.hashCode(), intent, FLAGS))
            .build()
        NotificationManagerCompat.from(context).notify(("bc" + b.id).hashCode(), notification)
    }

    /** Ringkasan grup, supaya beberapa update sekaligus tidak memenuhi bilah notifikasi. */
    @SuppressLint("MissingPermission")
    fun postSummary(context: Context, titles: List<String>) {
        if (titles.size < 2 || !canPost(context)) return
        val style = NotificationCompat.InboxStyle().also { s -> titles.take(5).forEach(s::addLine) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${titles.size} komik punya chapter baru")
            .setContentText(titles.take(3).joinToString(", "))
            .setStyle(style)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), FLAGS))
            .build()
        NotificationManagerCompat.from(context).notify(SUMMARY_ID, notification)
    }

    private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun openIntent(context: Context, b: BookmarkEntity): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SOURCE, b.source)
            putExtra(EXTRA_SLUG, b.comicSlug)
        }
        return PendingIntent.getActivity(context, b.id.hashCode(), intent, FLAGS)
    }
}
