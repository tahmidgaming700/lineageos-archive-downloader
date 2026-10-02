package com.tahmidgaming.lineagearchive

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class UpdateItem(
    val type: String,
    val installed: String,
    val available: String?,
    val file: AddonDownload? = null,
    val description: String = "",
    val canDownload: Boolean = false
) {
    val availableUpdate: Boolean
        get() = available != null && compareVersions(available, installed) > 0
}

object UpdateRepository {
    private const val APP_REPO = "tahmidgaming700/lineageos-archive-downloader"

    suspend fun app(context: Context): UpdateItem = withContext(Dispatchers.IO) {
        val installed = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
        val release = runCatching {
            AddonRepository.latestGithubRelease("tahmidgaming700", "lineageos-archive-downloader")
        }.getOrNull()
        val asset = release?.assets?.firstOrNull { it.name.endsWith(".apk", true) }
        UpdateItem("App", installed, release?.tag_name?.removePrefix("v"), asset?.let {
            AddonDownload(it.name, release.tag_name, it.size, it.digest?.removePrefix("sha256:"), it.browser_download_url, APP_REPO)
        }, "LineageOS Archive Downloader", asset != null)
    }

    suspend fun magisk(context: Context): UpdateItem = withContext(Dispatchers.IO) {
        val installed = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "magisk -V"))
            val value = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }.trim()
            p.waitFor()
            value.ifBlank { "not installed" }
        }.getOrDefault("not installed")
        val remote = AddonRepository.magisk().firstOrNull { it.name.endsWith(".apk", true) }
        UpdateItem("Magisk", normalizeMagisk(installed), remote?.version?.removePrefix("v"), remote, "Official Magisk release", remote != null)
    }

    suspend fun gapps(selectedAndroid: Int? = null, selectedArch: String = "arm64"): UpdateItem = withContext(Dispatchers.IO) {
        val android = selectedAndroid ?: Build.VERSION.SDK_INT
        val repo = AddonRepository.gapps().filter {
            it.source.contains("${android}.0.0-${selectedArch}", true)
        }.maxByOrNull { it.version }
        UpdateItem("GApps", "not tracked", repo?.version, repo, "Latest MindTheGapps package for Android $android / $selectedArch", repo != null)
    }

    suspend fun os(device: LineageDevice?, currentVersion: String): UpdateItem = withContext(Dispatchers.IO) {
        if (device == null) return@withContext UpdateItem("OS", currentVersion, null, description = "Select a supported LineageOS device first")
        val builds = runCatching { LineageRepository.builds(device.model) }.getOrDefault(emptyList())
        val latestBuild = builds.firstOrNull()\n        val latest = latestBuild?.files?.firstOrNull()
        UpdateItem("OS", currentVersion, latestBuild?.version, latest?.let {
            AddonDownload(it.filename, latest.os_patch_level ?: "LineageOS", it.size ?: 0L, it.sha256, it.url ?: "", "LineageOS")
        }, "Latest official LineageOS build", latest != null)
    }

    fun localRecovery(context: Context): Pair<String, String> {
        val props = runRoot("getprop ro.twrp.version; getprop ro.orangefox.version; getprop ro.of.version")
            .lines().map { it.trim() }.filter { it.isNotBlank() && it != "0" }
        return (props.firstOrNull() ?: "not detected") to (props.drop(1).firstOrNull() ?: "not detected")
    }

    private fun normalizeMagisk(value: String): String {
        if (value == "not installed") return value
        return if (value.length >= 5) value.dropLast(2) + "." + value.takeLast(2).trimStart('0').ifBlank { "0" } else value
    }

    private fun runRoot(command: String): String = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        p.waitFor()
        out
    }.getOrDefault("")
}

fun compareVersions(a: String, b: String): Int {
    val aa = Regex("\\d+").findAll(a).map { it.value.toInt() }.toList()
    val bb = Regex("\\d+").findAll(b).map { it.value.toInt() }.toList()
    val n = maxOf(aa.size, bb.size)
    for (i in 0 until n) {
        val x = aa.getOrElse(i) { 0 }
        val y = bb.getOrElse(i) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}
