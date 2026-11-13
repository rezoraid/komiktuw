package com.komikta.core.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    private const val NAME = "komikta-update-check"

    /**
     * Cadangan kalau FCM gagal/terlambat (OEM agresif).
     * Push FCM = jalur utama → notifikasi datang detik setelah server kirim.
     * WorkManager minimum period = 15 menit.
     */
    fun sync(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()
        // UPDATE: ganti jadwal lama 30 menit jika sudah terpasang
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
