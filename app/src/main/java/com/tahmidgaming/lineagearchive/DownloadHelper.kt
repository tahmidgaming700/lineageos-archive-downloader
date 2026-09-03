package com.tahmidgaming.lineagearchive

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DownloadHelper {
    fun enqueue(context: Context, file: LineageFile, device: String, version: String?): String? {
        val url = file.url ?: return null
        val id = DownloadStore.newId()
        DownloadStore.add(context, DownloadStore.Item(id, file.filename, device, version, file.sha256, file.size, url))
        enqueueWork(context, DownloadStore.items(context).first { it.id == id })
        return id
    }

    fun pause(context: Context, id: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(id)
        DownloadStore.update(context, id) { it.copy(status = "Paused") }
    }

    fun resume(context: Context, item: DownloadStore.Item) {
        enqueueWork(context, item)
    }

    private fun enqueueWork(context: Context, item: DownloadStore.Item) {
        val data = Data.Builder()
            .putString(DownloadWorker.KEY_ID, item.id)
            .putString(DownloadWorker.KEY_FILENAME, item.filename)
            .putString(DownloadWorker.KEY_URL, item.url)
            .putString(DownloadWorker.KEY_SHA256, item.expectedSha256)
            .putLong(DownloadWorker.KEY_SIZE, item.expectedSize ?: -1L)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(item.id)
            .build()
        DownloadStore.update(context, item.id) { it.copy(status = "Queued", error = null) }
        WorkManager.getInstance(context).enqueue(request)
    }
}
