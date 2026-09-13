package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppPrefs
import com.example.data.remote.RemoteClient
import java.util.concurrent.TimeUnit

/**
 * Controllo periodico (ogni 15 minuti, minimo consentito da Android) delle nuove notifiche
 * pubblicate sul server, anche ad app chiusa. Sostituisce le push di Firebase, rimosse per
 * la compatibilità F-Droid.
 */
class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        if (!prefs.serverConfigured) return Result.success()
        val api = RemoteClient.create(prefs.apiBaseUrl, prefs.clientId, prefs.adminToken)
        return try {
            val since = prefs.lastSeenNotificationTs
            val fresh = api.notifications(since = since)
            if (since == 0L) {
                // primo avvio: non rimostrare tutto lo storico
                prefs.lastSeenNotificationTs = fresh.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                return Result.success()
            }
            fresh.sortedBy { it.timestamp }.takeLast(MAX_PER_RUN).forEach { n ->
                NotificationHelper.sendPushNotification(applicationContext, n.id.toInt(), n.title, n.message)
            }
            fresh.maxOfOrNull { it.timestamp }?.let { prefs.lastSeenNotificationTs = it }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "neuroparty_notification_check"
        private const val MAX_PER_RUN = 3

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
