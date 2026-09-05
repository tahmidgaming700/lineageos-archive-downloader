package com.tahmidgaming.lineagearchive

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object LineageRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    private lateinit var deviceApi: LineageDeviceApi
    private lateinit var buildApi: LineageBuildApi
    private lateinit var archiveApi: ArchiveBuildApi

    fun initialize(context: Context) {
        val client = TlsClient.get(context)
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

    suspend fun devices(): List<LineageDevice> = deviceApi.devices()
    suspend fun builds(device: String): List<LineageBuild> = buildApi.builds(device)
    suspend fun archiveBuilds(device: String): List<ArchiveBuildSummary> =
        archiveApi.builds().filter { it.device.equals(device, ignoreCase = true) }
    suspend fun archiveBuild(id: Long): ArchiveBuildDetail = archiveApi.build(id)
}
