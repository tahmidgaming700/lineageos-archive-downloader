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
        get() = available != null &&
            installed != "not tracked" &&
            installed != "not installed" &&
            installed != "unknown" &&
            compareVersions(available, installed) > 0
}

object UpdateRepository {
    private const val APP_OWNER = "tahmidgaming700"
    private const val APP_REPO = "lineageos-archive-downloader"

    suspend fun app(context: Context): UpdateItem = withContext(Dispatchers.IO) {
        val installed = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")

        val release = runCatching {
            AddonRepository.latestGithubRelease(APP_OWNER, APP_REPO)
        }.getOrNull()

        val asset = release?.assets?.firstOrNull { it.name.endsWith(".apk", true) }

        UpdateItem(
            type = "App",
            installed = installed,
            available = release?.tag_name?.removePrefix("v"),
            file = asset?.let {
                AddonDownload(
                    it.name,
                    release.tag_name,
                    it.size,
                    it.digest?.removePrefix("sha256:"),
                    it.browser_download_url,
                    "$APP_OWNER/$APP_REPO"
                )
            },
            description = "LineageOS Archive Downloader",
            canDownload = asset != null
        )
    }

    suspend fun magisk(context: Context): UpdateItem = withContext(Dispatchers.IO) {
        val installed = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "magisk -v"))
            val value = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }.trim()
            p.waitFor()
            parseMagiskVersion(value)
        }.getOrDefault("not installed")

        val remote = runCatching { AddonRepository.magisk() }
            .getOrDefault(emptyList())
            .firstOrNull { it.name.endsWith(".apk", true) }

        UpdateItem(
            type = "Magisk",
            installed = installed,
            available = remote?.version?.removePrefix("v"),
            file = remote,
            description = "Official Magisk release",
            canDownload = remote != null
        )
    }

    suspend fun gapps(selectedAndroidSdk: Int? = null, selectedArch: String = "arm64"): UpdateItem =
        withContext(Dispatchers.IO) {
            val sdk = selectedAndroidSdk ?: Build.VERSION.SDK_INT
            val androidMajor = androidMajorFromSdk(sdk)
            val arch = if (selectedArch.equals("arm", true)) "arm" else "arm64"

            val repo = runCatching { AddonRepository.gapps() }
                .getOrDefault(emptyList())
                .filter { download ->
                    download.source.substringAfterLast("/")
                        .matches(Regex("^" + androidMajor + "(?:\\.0\\.0|\\.1\\.0)-" + arch + "(?:-ATV)?$"))
                }
                .maxByOrNull { it.version }

            UpdateItem(
                type = "GApps",
                installed = "not tracked",
                available = repo?.version,
                file = repo,
                description = "Latest MindTheGapps package for Android " + androidMajor + " / " + arch,
                canDownload = repo != null
            )
        }

    suspend fun os(device: LineageDevice?, currentVersion: String): UpdateItem =
        withContext(Dispatchers.IO) {
            if (device == null) {
                return@withContext UpdateItem(
                    "OS",
                    currentVersion,
                    null,
                    description = "Select a supported LineageOS device first"
                )
            }

            val latestBuild = runCatching {
                LineageRepository.builds(device.model)
                    .maxByOrNull { it.datetime }
            }.getOrNull()

            val latest = latestBuild?.files
                ?.maxByOrNull { it.size ?: 0L }

            UpdateItem(
                type = "OS",
                installed = currentVersion,
                available = latestBuild?.version,
                file = latest?.let {
                    AddonDownload(
                        it.filename,
                        latestBuild.version ?: "LineageOS",
                        it.size ?: 0L,
                        it.sha256,
                        it.url ?: "",
                        "LineageOS"
                    )
                },
                description = "Latest official LineageOS build for ${device.name}",
                canDownload = latest?.url?.isNotBlank() == true
            )
        }

    fun localOsVersion(): String =
        runRoot("getprop ro.lineage.version").trim()
            .ifBlank { Build.VERSION.RELEASE }

    fun localRecovery(context: Context): Pair<String, String> {
        val props = runRoot(
            "getprop ro.twrp.version; " +
                "getprop ro.orangefox.version; " +
                "getprop ro.of.version"
        ).lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "0" }

        return (props.firstOrNull() ?: "not detected") to
            (props.drop(1).firstOrNull() ?: "not detected")
    }

    private fun parseMagiskVersion(value: String): String {
        val version = Regex("\\b\\d+(?:\\.\\d+)?\\b")
            .find(value)
            ?.value
        return version ?: "not installed"
    }

    private fun androidMajorFromSdk(sdk: Int): Int = when {
        sdk >= 37 -> 17
        sdk == 36 -> 16
        sdk == 35 -> 15
        sdk == 34 -> 14
        sdk == 33 -> 13
        sdk == 32 || sdk == 31 -> 12
        sdk == 30 -> 11
        sdk == 29 -> 10
        sdk == 28 -> 9
        sdk == 27 || sdk == 26 -> 8
        sdk == 25 || sdk == 24 -> 7
        else -> 6
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
