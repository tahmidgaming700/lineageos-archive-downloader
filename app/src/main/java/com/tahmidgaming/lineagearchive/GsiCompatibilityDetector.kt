package com.tahmidgaming.lineagearchive

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class GsiCompatibilityInfo(
    val primaryAbi: String,
    val trebleEnabled: String,
    val abUpdate: String,
    val dynamicPartitions: String,
    val sdk: Int
) {
    val trebleStatus: String
        get() = when (trebleEnabled.lowercase()) {
            "true", "1" -> "reported enabled"
            "false", "0" -> "reported disabled"
            else -> "unknown — confirm manually"
        }

    val partitionStatus: String
        get() = when (abUpdate.lowercase()) {
            "true", "1" -> "A/B update property enabled"
            "false", "0" -> "A-only property reported"
            else -> "unknown — confirm manually"
        }
}

object GsiCompatibilityDetector {
    fun defaultArchitecture(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase().orEmpty()
        return when {
            abi.contains("arm64") || abi.contains("aarch64") -> "arm64"
            abi.contains("armeabi") || abi == "arm" -> "arm"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") || abi.contains("i686") -> "x86"
            else -> "All"
        }
    }

    suspend fun detect(): GsiCompatibilityInfo = withContext(Dispatchers.IO) {
        GsiCompatibilityInfo(
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            trebleEnabled = getprop("ro.treble.enabled"),
            abUpdate = getprop("ro.build.ab_update"),
            dynamicPartitions = getprop("ro.boot.dynamic_partitions")
                .ifBlank { getprop("ro.build.dynamic_partitions") },
            sdk = Build.VERSION.SDK_INT
        )
    }

    private fun getprop(key: String): String = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        val value = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }.trim()
        process.waitFor()
        value
    }.getOrDefault("")
}
