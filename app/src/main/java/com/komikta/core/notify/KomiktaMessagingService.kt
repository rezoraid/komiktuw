package com.komikta.core.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.komikta.data.remote.dto.BroadcastDto
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KomiktaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var prefs: NotificationPrefs

    override fun onNewToken(token: String) {
        PushSync.schedule(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "broadcast" || !prefs.enabled) return
        val id = data["id"].orEmpty()
        val title = data["title"].orEmpty()
        val body = data["body"].orEmpty()
        if (id.isBlank() || title.isBlank() || !prefs.markShown(id)) return
        UpdateNotifier.postBroadcast(
            applicationContext,
            BroadcastDto(id, title, body, data["source"].orEmpty(), data["slug"].orEmpty(), data["createdAt"]?.toLongOrNull() ?: 0L)
        )
    }
}
