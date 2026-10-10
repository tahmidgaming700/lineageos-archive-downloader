package com.tahmidgaming.lineagearchive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Native adapter for the Huawei Firm Finder's HiSuite Proxy protocol.
 *
 * Important: professorjtj.github.io is a static frontend. Its search actions post to
 * a HiSuite Proxy service at 127.0.0.1:7777; the website does not expose a documented
 * public JSON API. Users must point this app at a reachable, compatible proxy.
 */
object HuaweiFirmwareRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_PROXY = "http://127.0.0.1:7777"

    data class Query(
        val model: String,
        val region: String,
        val vendor: String,
        val targetVersion: String
    )

    data class FirmwareFile(
        val filename: String,
        val url: String,
        val size: Long? = null,
        val sha256: String? = null,
        val version: String? = null
    )

    data class Result(
        val title: String,
        val detail: String,
        val files: List<FirmwareFile> = emptyList()
    )

    suspend fun checkStatus(proxy: String, query: Query): Result =
        withContext(Dispatchers.IO) { post(proxy, "/checkRom.txt", query, "ROM status") }

    suspend fun getFileList(proxy: String, query: Query): Result =
        withContext(Dispatchers.IO) { post(proxy, "/getFile.txt", query, "ROM file list") }

    private fun post(base: String, path: String, query: Query, title: String): Result {
        val cleanBase = base.trim().trimEnd('/')
        require(cleanBase.startsWith("http://") || cleanBase.startsWith("https://")) {
            "Proxy address must start with http:// or https://"
        }
        val body = FormBody.Builder()
            .add("model", query.model.trim())
            .add("region", query.region.trim())
            .add("vendor", query.vendor.trim())
            .add("version", query.targetVersion.trim())
            .build()
        val request = Request.Builder()
            .url(cleanBase + path)
            .header("User-Agent", "Huawei-OS-Updater/1.0")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HiSuite Proxy returned HTTP ${response.code}. ${text.take(400)}")
            }
            if (text.isBlank()) throw IOException("The proxy returned an empty response.")
            return Result(title, text.take(20000), if (path == "/getFile.txt") parseFiles(text) else emptyList())
        }
    }

    /**
     * Firmware finder responses can vary between proxy versions. Parse common XML/JSON
     * fields and direct package links conservatively; never turn an arbitrary text URL
     * into a download unless it looks like a firmware package.
     */
    private fun parseFiles(raw: String): List<FirmwareFile> {
        val output = linkedMapOf<String, FirmwareFile>()
        val decoded = raw.replace("&amp;", "&")
        val urls = Regex("""https?://[^\s"'<>\\]+""", RegexOption.IGNORE_CASE)
            .findAll(decoded)
            .map { it.value.trimEnd(',', ';', ')', ']', '}').replace("&amp;", "&") }
            .toList()

        for (url in urls) {
            val cleanUrl = url.substringBefore("#")
            val pathName = cleanUrl.substringBefore("?").substringAfterLast("/")
            val name = runCatching { URLDecoder.decode(pathName, "UTF-8") }.getOrDefault(pathName)
            if (name.isBlank() || name.length > 180) continue
            val looksLikeFirmware = name.contains(".zip", true) ||
                name.contains(".app", true) ||
                name.contains(".bin", true) ||
                name.contains(".dgtks", true) ||
                name.contains("update", true) ||
                name.contains("firmware", true)
            if (!looksLikeFirmware) continue

            val sha = Regex("""(?i)(?:sha256|sha-256)[^0-9a-f]{0,20}([0-9a-f]{64})""")
                .find(decoded)?.groupValues?.getOrNull(1)
            val size = Regex("""(?i)(?:filesize|file_size|size)[^0-9]{0,10}(\d{5,})""")
                .find(decoded)?.groupValues?.getOrNull(1)?.toLongOrNull()
            output.putIfAbsent(cleanUrl, FirmwareFile(name, cleanUrl, size, sha))
        }

        // Handle JSON-style records where URL and filename are separate properties.
        runCatching {
            val json = JSONObject(decoded)
            collectJsonFiles(json, output)
        }
        runCatching {
            val array = JSONArray(decoded)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                collectJsonFiles(item, output)
            }
        }
        return output.values.toList()
    }

    private fun collectJsonFiles(json: JSONObject, output: MutableMap<String, FirmwareFile>) {
        val url = sequenceOf("url", "download_url", "downloadUrl", "link", "href")
            .mapNotNull { json.optString(it).takeIf(String::isNotBlank) }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") } ?: return
        val filename = sequenceOf("filename", "fileName", "name", "file")
            .mapNotNull { json.optString(it).takeIf(String::isNotBlank) }
            .firstOrNull() ?: url.substringBefore("?").substringAfterLast("/")
        val size = sequenceOf("size", "filesize", "file_size")
            .mapNotNull { json.optLong(it, -1L).takeIf { n -> n > 0 } }.firstOrNull()
        val sha = sequenceOf("sha256", "sha_256", "checksum")
            .mapNotNull { json.optString(it).takeIf { v -> v.matches(Regex("(?i)[0-9a-f]{64}")) } }
            .firstOrNull()
        if (filename.isNotBlank() && (filename.contains(".zip", true) ||
                filename.contains(".app", true) || filename.contains(".bin", true) ||
                filename.contains(".dgtks", true))) {
            output.putIfAbsent(url, FirmwareFile(filename, url, size, sha))
        }
    }
}
