package com.tahmidgaming.lineagearchive

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class LineageDevice(
    val model: String,
    val oem: String,
    val name: String,
    val lineage_recovery: Boolean? = null
)

@Serializable
data class LineageBuild(
    val datetime: Long,
    val files: List<LineageFile> = emptyList(),
    val type: String? = null,
    val version: String? = null
)

@Serializable
data class LineageFile(
    val filename: String,
    val size: Long? = null,
    val sha256: String? = null,
    val url: String? = null,
    val os_patch_level: String? = null,
    val os_sdk_level: Int? = null
)

@Serializable
data class ArchiveBuildSummary(
    val id: Long,
    val filename: String,
    val device: String
)

@Serializable
data class ArchiveBuildDetail(
    val id: Long,
    val filename: String,
    val filesize: Long? = null,
    val md5: String? = null,
    val sha1: String? = null,
    val sha256: String? = null,
    val sha512: String? = null,
    val url: String? = null,
    val path: String? = null
)

interface LineageDeviceApi {
    @GET("updater/devices.json")
    suspend fun devices(): List<LineageDevice>
}

interface LineageBuildApi {
    @GET("api/v2/devices/{device}/builds")
    suspend fun builds(@Path("device") device: String): List<LineageBuild>
}

interface ArchiveBuildApi {
    @GET("api/builds")
    suspend fun builds(): List<ArchiveBuildSummary>

    @GET("api/builds/{id}")
    suspend fun build(@Path("id") id: Long): ArchiveBuildDetail
}
