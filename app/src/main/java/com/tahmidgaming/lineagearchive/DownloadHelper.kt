package com.tahmidgaming.lineagearchive

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object DownloadHelper {
    fun enqueue(context: Context, file: LineageFile, device: String, version: String?) {
        val url = file.url ?: "https://download.lineageos.org/stable/${device}/${file.filename}"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(file.filename)
            .setDescription("LineageOS ${version ?: "build"} • $device")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.filename)
        context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
}
