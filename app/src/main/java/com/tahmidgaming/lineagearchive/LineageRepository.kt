package com.tahmidgaming.lineagearchive

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object LineageRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()
    private lateinit var appContext: Context

    private lateinit var deviceApi: LineageDeviceApi
    private lateinit var buildApi: LineageBuildApi
    private lateinit var archiveApi: ArchiveBuildApi

    private val cachePrefs by lazy {
        appContext.getSharedPreferences("lineage_public_metadata_cache", Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val client = TlsClient.get(appContext)
        deviceApi = Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/LineageOS/hudson/main/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(jsonType))
            .build()
            .create(LineageDeviceApi::class.java)

        buildApi = Retrofit.Builder()
            .baseUrl("https://download.lineageos.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(jsonType))
            .build()
            .create(LineageBuildApi::class.java)

        archiveApi = Retrofit.Builder()
            .baseUrl("https://lineage-archive.timschumi.net/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(jsonType))
            .build()
            .create(ArchiveBuildApi::class.java)
    }

    fun buildsCacheKey(device: String): String = "builds_" + device.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun isUsingCachedFallback(key: String): Boolean = cachePrefs.getBoolean(key + "_stale", false)

    suspend fun devices(): List<LineageDevice> = withContext(Dispatchers.IO) {
        val serializer = ListSerializer(LineageDevice.serializer())
        runCatching {
            deviceApi.devices().also { saveCache("devices", json.encodeToString(serializer, it)) }
        }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            val cached = cachedJson("devices") ?: throw failure
            markCacheFallback("devices")
            json.decodeFromString(serializer, cached)
        }
    }

    suspend fun builds(device: String): List<LineageBuild> = withContext(Dispatchers.IO) {
        val key = buildsCacheKey(device)
        val serializer = ListSerializer(LineageBuild.serializer())
        runCatching {
            buildApi.builds(device).also { saveCache(key, json.encodeToString(serializer, it)) }
        }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            val cached = cachedJson(key) ?: throw failure
            markCacheFallback(key)
            json.decodeFromString(serializer, cached)
        }
    }

    suspend fun archiveBuilds(device: String): List<ArchiveBuildSummary> = withContext(Dispatchers.IO) {
        val serializer = ListSerializer(ArchiveBuildSummary.serializer())
        val all = runCatching {
            archiveApi.builds().also { saveCache("archive_summaries", json.encodeToString(serializer, it)) }
        }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            val cached = cachedJson("archive_summaries") ?: throw failure
            markCacheFallback("archive_summaries")
            json.decodeFromString(serializer, cached)
        }
        all.filter { it.device.equals(device, ignoreCase = true) }
    }

    suspend fun archiveBuild(id: Long): ArchiveBuildDetail = withContext(Dispatchers.IO) {
        val key = "archive_detail_$id"
        runCatching {
            archiveApi.build(id).also {
                saveCache(key, json.encodeToString(ArchiveBuildDetail.serializer(), it))
            }
        }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            val cached = cachedJson(key) ?: throw failure
            markCacheFallback(key)
            json.decodeFromString(ArchiveBuildDetail.serializer(), cached)
        }
    }

    fun lastCacheUpdated(): Long? =
        cachePrefs.getLong("last_updated", 0L).takeIf { it > 0L }

    private fun saveCache(key: String, value: String) {
        val now = System.currentTimeMillis()
        cachePrefs.edit()
            .putString(key, value)
            .putLong(key + "_updated", now)
            .putBoolean(key + "_stale", false)
            .putLong("last_updated", now)
            .apply()
    }

    private fun markCacheFallback(key: String) {
        cachePrefs.edit().putBoolean(key + "_stale", true).apply()
    }

    private fun cachedJson(key: String): String? =
        cachePrefs.getString(key, null)?.takeIf { it.isNotBlank() }
}
