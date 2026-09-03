package com.tahmidgaming.lineagearchive

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object LineageRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    private val deviceApi = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/LineageOS/hudson/main/")
        .addConverterFactory(json.asConverterFactory(jsonType))
        .build()
        .create(LineageDeviceApi::class.java)

    private val buildApi = Retrofit.Builder()
        .baseUrl("https://download.lineageos.org/")
        .addConverterFactory(json.asConverterFactory(jsonType))
        .build()
        .create(LineageBuildApi::class.java)

    private val archiveApi = Retrofit.Builder()
        .baseUrl("https://lineage-archive.timschumi.net/")
        .addConverterFactory(json.asConverterFactory(jsonType))
        .build()
        .create(ArchiveBuildApi::class.java)

    suspend fun devices(): List<LineageDevice> = deviceApi.devices()
    suspend fun builds(device: String): List<LineageBuild> = buildApi.builds(device)

    suspend fun archiveBuilds(device: String): List<ArchiveBuildSummary> =
        archiveApi.builds().filter { it.device.equals(device, ignoreCase = true) }

    suspend fun archiveBuild(id: Long): ArchiveBuildDetail = archiveApi.build(id)
}
