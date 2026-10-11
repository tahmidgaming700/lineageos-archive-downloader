package com.tahmidgaming.lineagearchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AppUpdateCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val update = UpdateRepository.app(applicationContext)
        if (update.availableUpdate && update.file != null) {
            val version = update.available ?: "new version"
            val prefs = applicationContext.getSharedPreferences("update_notifications", Context.MODE_PRIVATE)
            if (prefs.getString("last_notified_version", null) != version && notifyUpdate(version)) {
                prefs.edit().putString("last_notified_version", version).apply()
            }
        }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() }
    )

    private fun notifyUpdate(version: String): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "App updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notifies when a newer app release is available." }
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Lineage Updater & Downloader update available")
            .setContentText("Version $version is available. Open Updates to review the source and verify the APK.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "app_updates"
        const val NOTIFICATION_ID = 2040
    }
}
