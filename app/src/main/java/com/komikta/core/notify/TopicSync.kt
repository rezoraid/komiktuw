package com.komikta.core.notify

import com.google.firebase.messaging.FirebaseMessaging
import com.komikta.data.local.dao.BookmarkDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

fun chapterTopic(source: String, slug: String): String =
    "c_" + MessageDigest.getInstance("SHA-1").digest("$source|$slug".toByteArray()).joinToString("") { "%02x".format(it) }.take(24)

@Singleton
class TopicSync @Inject constructor(private val bookmarks: BookmarkDao, private val prefs: NotificationPrefs) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(bookmarks.getAll().map { list -> list.take(MAX_TOPICS).map { chapterTopic(it.source, it.comicSlug) }.toSet() }, prefs.enabledFlow) { wanted, on ->
                if (on) wanted else emptySet()
            }.distinctUntilChanged().collect { wanted -> apply(wanted) }
        }
    }

    private fun apply(wanted: Set<String>) {
        val current = prefs.topics
        val fcm = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        (wanted - current).forEach { fcm.subscribeToTopic(it) }
        (current - wanted).forEach { fcm.unsubscribeFromTopic(it) }
        prefs.topics = wanted
    }

    private companion object {
        const val MAX_TOPICS = 200
    }
}
