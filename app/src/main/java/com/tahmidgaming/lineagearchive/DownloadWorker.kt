package com.tahmidgaming.lineagearchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val client = OkHttpClient.Builder().build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_ID) ?: return@withContext Result.failure()
        val filename = inputData.getString(KEY_FILENAME) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val expected = inputData.getString(KEY_SHA256)
        val expectedSize = inputData.getLong(KEY_SIZE, -1L).takeIf { it >= 0 }
        val item = DownloadStore.items(applicationContext).firstOrNull { it.id == id }
            ?: return@withContext Result.failure()

        setForeground(createForegroundInfo(filename, 0))
        DownloadStore.update(applicationContext, id) { it.copy(status = "Downloading", error = null) }

        val part = File(applicationContext.cacheDir, "downloads/$id.part").apply { parentFile?.mkdirs() }
        var existing = if (part.exists()) part.length() else 0L
        val requestBuilder = Request.Builder().url(url)
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")
        val response = try { client.newCall(requestBuilder.build()).execute() } catch (e: Exception) {
            DownloadStore.update(applicationContext, id) { it.copy(status = "Failed", error = e.message) }
            return@withContext Result.retry()
        }
        response.use { r ->
            if (!r.isSuccessful && r.code != 206) {
                DownloadStore.update(applicationContext, id) { it.copy(status = "Failed", error = "HTTP ${r.code}") }
                return@withContext Result.failure()
            }
            if (r.code == 200 && existing > 0) { part.delete(); existing = 0L }
            val total = if (r.code == 206) existing + (r.body?.contentLength() ?: -1L) else r.body?.contentLength() ?: -1L
            val body = r.body ?: return@withContext Result.failure()
            body.byteStream().use { input ->
                part.outputStream().use { out ->
                    if (existing > 0) {
                        // Range responses append to the existing partial file.
                        part.appendBytes(ByteArray(0))
                    }
                    copyWithProgress(input, out, existing, total, filename)
                }
            }
        }

        if (expectedSize != null && part.length() != expectedSize) {
            DownloadStore.update(applicationContext, id) { it.copy(status = "Failed", error = "Size mismatch: ${part.length()} / $expectedSize bytes") }
            return@withContext Result.failure()
        }

        val actual = sha256(part)
        val verified = expected?.equals(actual, ignoreCase = true) ?: false
        if (expected != null && !verified) {
            DownloadStore.update(applicationContext, id) { it.copy(status = "SHA-256 FAILED", verified = false, error = "Expected $expected, got $actual") }
            part.delete()
            return@withContext Result.failure()
        }

        publishToDownloads(part, filename)
        DownloadStore.update(applicationContext, id) { it.copy(status = if (expected == null) "Downloaded • SHA-256 unavailable" else "Downloaded • SHA-256 PASS", verified = expected?.let { verified }) }
        Result.success()
    }

    private fun copyWithProgress(input: InputStream, output: OutputStream, base: Long, total: Long, filename: String) {
        val buffer = ByteArray(256 * 1024)
        var done = base
        var lastUpdate = 0L
        while (true) {
            if (isStopped) return
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            done += read
            if (done - lastUpdate >= 1024 * 1024 || (total > 0 && done == total)) {
                val percent = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
                setProgressAsync(androidx.work.workDataOf("bytes" to done, "total" to total, "percent" to percent))
                updateNotification(filename, percent)
                lastUpdate = done
            }
        }
    }

    private fun publishToDownloads(part: File, filename: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = applicationContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads entry")
            try {
                resolver.openOutputStream(uri)?.use { out -> part.inputStream().use { it.copyTo(out) } }
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            part.copyTo(File(dir, filename), overwrite = true)
        }
        part.delete()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createForegroundInfo(filename: String, progress: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(filename)
            .setContentText(if (progress > 0) "Downloading • $progress%" else "Preparing download")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(filename: String, progress: Int) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(filename).setContentText("Downloading • $progress%")
            .setProgress(100, progress, false).setOngoing(true).build())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "ROM downloads", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_FILENAME = "filename"
        const val KEY_URL = "url"
        const val KEY_SHA256 = "sha256"
        const val KEY_SIZE = "size"
        private const val CHANNEL = "rom_downloads"
        private const val NOTIFICATION_ID = 7211
    }
}
