package com.tahmidgaming.lineagearchive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Native client for the Huawei Firm Finder / HiSuite Proxy workflow.
 *
 * The public website is a static JavaScript frontend. Its firmware-status and file-list
 * operations are designed around the local HiSuite Proxy HTTP service, not a documented
 * public JSON API. Keep the endpoint configurable rather than pretending the static site
 * itself is a JSON API.
 */
object HuaweiFirmwareRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_PROXY = "http://127.0.0.1:7777"

    data class Query(
        val model: String,
        val region: String,
        val vendor: String,
        val targetVersion: String
    )

    data class Result(val title: String, val detail: String, val url: String? = null)

    suspend fun checkStatus(proxy: String, query: Query): Result = withContext(Dispatchers.IO) {
        post(proxy, "/checkRom.txt", query, "ROM status")
    }

    suspend fun getFileList(proxy: String, query: Query): Result = withContext(Dispatchers.IO) {
        post(proxy, "/getFile.txt", query, "ROM file list")
    }

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
            Result(title, text.take(12000))
        }
    }
}
