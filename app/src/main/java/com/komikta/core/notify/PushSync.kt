package com.komikta.core.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.komikta.data.local.SessionStore
import com.komikta.data.remote.api.CommunityApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object PushSync {
    private const val NAME = "komikta-push-sync"

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<PushSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun fetchToken(): String? = suspendCancellableCoroutine { c ->
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { t -> if (c.isActive) c.resume(if (t.isSuccessful) t.result else null) }
        } catch (e: Exception) {
            if (c.isActive) c.resume(null)
        }
    }
}

class PushSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun community(): CommunityApi
        fun prefs(): NotificationPrefs
        fun session(): SessionStore
    }

    override suspend fun doWork(): Result {
        val d = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        val prefs = d.prefs()
        val token = PushSync.fetchToken() ?: return retryOrFail()
        val enabled = prefs.enabled
        val key = "${if (enabled) "on" else "off"}|$token|${d.session().user.value?.id.orEmpty()}"
        if (key == prefs.pushKey && System.currentTimeMillis() - prefs.pushSyncedAt < RESYNC_MS) return Result.success()
        return try {
            val previous = prefs.pushToken
            val r = if (enabled) d.community().registerPushToken(token, previous) else d.community().unregisterPushToken(token)
            if (!r.ok) return retryOrFail()
            prefs.pushKey = key
            prefs.pushToken = token
            prefs.pushSyncedAt = System.currentTimeMillis()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result = if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

    private companion object {
        const val MAX_ATTEMPTS = 6
        const val RESYNC_MS = 7L * 24 * 60 * 60 * 1000
    }
}
