package com.tahmidgaming.lineagearchive

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/**
 * Installs only a downloaded APK whose source SHA-256 and signing identity
 * match the installed app. Android's package installer remains the final gate.
 */
object AppUpdateInstaller {
    fun install(context: Context, item: DownloadStore.Item): Result<Unit> = runCatching {
        require(item.filename.endsWith(".apk", true)) { "This file is not an APK." }
        val expected = item.expectedSha256?.trim()?.takeIf { it.matches(Regex("[A-Fa-f0-9]{64}")) }
            ?: error("A trusted SHA-256 digest was not supplied. Installation is blocked.")
        require(item.verified == true && item.status.startsWith("Downloaded")) {
            "Download verification has not passed."
        }

        val apk = copyDownloadedApk(context, item.filename)
        val actual = sha256(apk)
        require(actual.equals(expected, ignoreCase = true)) {
            "APK SHA-256 mismatch. The file will not be installed."
        }

        val flags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val candidate = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("Android could not read this APK's package metadata.")
        require(candidate.packageName == context.packageName) {
            "Package ID mismatch. This is not an update for this app."
        }

        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(signerFingerprints(installed).isNotEmpty() &&
            signerFingerprints(installed) == signerFingerprints(candidate)) {
            "Signing certificate mismatch. Installation is blocked to protect the installed app."
        }
        val isNewer = if (Build.VERSION.SDK_INT >= 28) {
            candidate.longVersionCode > installed.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            candidate.versionCode > installed.versionCode
        }
        require(isNewer) { "This APK is not newer than the installed version." }

        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            error("Allow this app to install unknown apps in Settings, then tap Install update again.")
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun copyDownloadedApk(context: Context, filename: String): File {
        val targetDir = File(context.cacheDir, "updates").apply {
            if (!exists() && !mkdirs()) error("Unable to create update cache.")
        }
        val target = File(targetDir, filename).canonicalFile
        require(target.parentFile == targetDir.canonicalFile) { "Unsafe APK filename." }

        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val selection = MediaStore.Downloads.DISPLAY_NAME + " = ? AND " +
                MediaStore.Downloads.RELATIVE_PATH + " = ?"
            val uri = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                selection,
                arrayOf(filename, Environment.DIRECTORY_DOWNLOADS + "/"),
                MediaStore.Downloads.DATE_ADDED + " DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    )
                } else null
            } ?: error("APK not found in the Downloads folder.")
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to read the downloaded APK.")
        } else {
            @Suppress("DEPRECATION")
            val source = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            require(source.isFile) { "APK not found in the Downloads folder." }
            source.copyTo(target, overwrite = true)
        }
        return target
    }

    private fun signerFingerprints(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList().orEmpty()
        }
        return signatures.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
