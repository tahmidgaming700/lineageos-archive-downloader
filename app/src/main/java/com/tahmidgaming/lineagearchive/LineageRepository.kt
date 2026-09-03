package com.tahmidgaming.lineagearchive

import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

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

    suspend fun devices(): List<LineageDevice> = deviceApi.devices()

    suspend fun builds(device: String): List<LineageBuild> = buildApi.builds(device)
}
