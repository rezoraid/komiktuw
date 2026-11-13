package com.komikta.core.notify

import coil.ImageLoader
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.remote.dto.BroadcastDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class KomiktaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var prefs: NotificationPrefs
    @Inject lateinit var bookmarks: BookmarkDao
    @Inject lateinit var history: ReadingHistoryDao
    @Inject lateinit var state: UpdateState
    @Inject lateinit var images: ImageLoader

    override fun onNewToken(token: String) {
        PushSync.schedule(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (!prefs.enabled) return
        when (data["type"]) {
            "broadcast" -> broadcast(data)
            "chapter" -> chapter(data)
        }
    }

    private fun broadcast(data: Map<String, String>) {
        val id = data["id"].orEmpty()
        val title = data["title"].orEmpty()
        if (id.isBlank() || title.isBlank() || !prefs.markShown(id)) return
        UpdateNotifier.postBroadcast(
            applicationContext,
            BroadcastDto(id, title, data["body"].orEmpty(), data["source"].orEmpty(), data["slug"].orEmpty(), data["createdAt"]?.toLongOrNull() ?: 0L)
        )
    }

    private fun chapter(data: Map<String, String>) {
        val source = data["source"].orEmpty()
        val slug = data["slug"].orEmpty()
        val chapterSlug = data["chapterSlug"].orEmpty()
        val id = data["id"].orEmpty()
        if (source.isBlank() || slug.isBlank() || chapterSlug.isBlank() || id.isBlank()) return
        runBlocking {
            val bookmark = bookmarks.getById("${source}_$slug") ?: return@runBlocking
            if (history.getById(bookmark.id)?.lastChapterSlug == chapterSlug || !prefs.markShown(id)) return@runBlocking
            state.get(bookmark.id)?.let { state.put(bookmark.id, UpdateState.Seen(chapterSlug, it.count + 1)) }
            val label = data["chapter"].orEmpty().replace(Regex("(?i)^\\s*(chapter|ch\\.?)\\s*"), "").trim()
            UpdateNotifier.postUpdate(applicationContext, bookmark, 1, listOfNotNull(label.takeIf { it.isNotEmpty() }), UpdateNotifier.loadCover(applicationContext, images, bookmark.coverUrl))
        }
    }
}
