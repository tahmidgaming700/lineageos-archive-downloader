package com.tahmidgaming.lineagearchive

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

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

interface ArchiveBuildApi {
    @GET("api/builds")
    suspend fun builds(): List<ArchiveBuildSummary>

    @GET("api/builds/{id}")
    suspend fun build(@Path("id") id: Long): ArchiveBuildDetail
}
