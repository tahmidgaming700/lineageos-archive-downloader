package com.tahmidgaming.lineagearchive

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GithubRepo(
    val name: String,
    val archived: Boolean = false
)

@Serializable
data class GithubRelease(
    val tag_name: String,
    val name: String? = null,
    val published_at: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    val name: String,
    val size: Long = 0,
    val browser_download_url: String,
    val digest: String? = null
)

interface GithubAddonApi {
    @GET("orgs/MindTheGapps/repos")
    suspend fun gappsRepos(
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GithubRepo>

    @GET("repos/MindTheGapps/{repo}/releases")
    suspend fun gappsReleases(
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 3
    ): List<GithubRelease>

    @GET("repos/topjohnwu/Magisk/releases")
    suspend fun magiskReleases(
        @Query("per_page") perPage: Int = 5
    ): List<GithubRelease>
}
