package com.tahmidgaming.lineagearchive

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

data class AddonDownload(
    val name: String,
    val version: String,
    val size: Long,
    val sha256: String?,
    val url: String,
    val source: String
)

object AddonRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()
    private lateinit var api: GithubAddonApi

    fun initialize(context: Context) {
        api = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(TlsClient.get(context))
            .addConverterFactory(json.asConverterFactory(jsonType))
            .build()
            .create(GithubAddonApi::class.java)
    }

    suspend fun latestGithubRelease(owner: String, repo: String): GithubRelease =
        api.githubReleases(owner, repo, 1).first()

    suspend fun recoveryReleaseAssets(owner: String, repo: String): List<AddonDownload> {
        require(owner.matches(Regex("[A-Za-z0-9_.-]+")) && repo.matches(Regex("[A-Za-z0-9_.-]+"))) {
            "Invalid GitHub repository"
        }
        return api.githubReleases(owner, repo, 5)
            .flatMap { release ->
                release.assets
                    .filter { asset ->
                        val name = asset.name.lowercase()
                        listOf(".zip", ".img", ".img.xz", ".img.gz", ".bin", ".apk").any(name::endsWith)
                    }
                    .map { asset ->
                        AddonDownload(
                            asset.name,
                            release.tag_name,
                            asset.size,
                            asset.digest?.removePrefix("sha256:"),
                            asset.browser_download_url,
                            "$owner/$repo"
                        )
                    }
            }
            .distinctBy { it.url }
            .take(30)
    }

    suspend fun magisk(): List<AddonDownload> {
        return api.magiskReleases()
            .flatMap { release ->
                release.assets.filter { it.name.endsWith(".apk", true) || it.name.endsWith(".zip", true) }
                    .map { asset ->
                        AddonDownload(asset.name, release.tag_name, asset.size, asset.digest?.removePrefix("sha256:"), asset.browser_download_url, "topjohnwu/Magisk")
                    }
            }
    }

    /**
     * Community GSI discovery. Results are live GitHub release assets from known
     * Treble/GSI projects; availability does not imply compatibility with a device.
     */
    suspend fun gsi(): List<AddonDownload> = coroutineScope {
        val sources = listOf(
            "ponces" to "treble_build_los",
            "AndyYan" to "lineage_build_unified"
        )
        sources.map { (owner, repo) ->
            async {
                runCatching { api.githubReleases(owner, repo, 5) }
                    .getOrDefault(emptyList())
                    .flatMap { release ->
                        release.assets
                            .filter { asset ->
                                val name = asset.name.lowercase()
                                val imageLike = name.endsWith(".img") ||
                                    name.endsWith(".img.xz") ||
                                    name.endsWith(".img.gz") ||
                                    name.endsWith(".zip")
                                imageLike && Regex(".*(gsi|treble|arm|a64|aarch64|x86|i686|vndklite|lineage).*").matches(name)
                            }
                            .map { asset ->
                                AddonDownload(
                                    asset.name,
                                    release.tag_name,
                                    asset.size,
                                    asset.digest?.removePrefix("sha256:"),
                                    asset.browser_download_url,
                                    "$owner/$repo"
                                )
                            }
                    }
            }
        }.awaitAll().flatten().distinctBy { it.url }
            .sortedByDescending { it.version }
            .take(40)
    }

    suspend fun gapps(): List<AddonDownload> = coroutineScope {
        val repos = api.gappsRepos()
            .filterNot { it.archived }
            .map { it.name }
            .filter { Regex("^([0-9]{2})\\.0\\.0-(arm64|arm)(-ATV)?$").matches(it) }
            .sortedWith(compareByDescending<String> { Regex("^([0-9]{2})").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
                .thenBy { if (it.contains("arm64")) 0 else 1 })
            .take(16)

        repos.map { repo ->
            async {
                runCatching { api.gappsReleases(repo).firstOrNull() }
                    .getOrNull()
                    ?.let { release ->
                        release.assets.filter { it.name.endsWith(".zip", true) }
                            .map { asset ->
                                AddonDownload(asset.name, release.tag_name, asset.size, asset.digest?.removePrefix("sha256:"), asset.browser_download_url, "MindTheGapps/$repo")
                            }
                    } ?: emptyList()
            }
        }.awaitAll().flatten()
    }
}
