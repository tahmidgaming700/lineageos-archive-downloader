package com.tahmidgaming.lineagearchive

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object FlashManager {
    enum class Recovery { TWRP, ORANGEFOX, OPEN_RECOVERY }
    data class Capability(val rooted: Boolean, val twrpDetected: Boolean, val orangeFoxDetected: Boolean) {
        val ready: Boolean get() = rooted && (twrpDetected || orangeFoxDetected)
    }

    fun detect(context: Context): Capability {
        val rooted = runRoot("id").contains("uid=0")
        if (!rooted) return Capability(false, false, false)
        val twrp = runRoot("getprop ro.twrp.version").trim().isNotBlank() || runRoot("test -d /twres && echo yes").contains("yes")
        val orangeFox = runRoot("getprop ro.orangefox.version; getprop ro.of.version; test -d /fox && echo orangefox")
            .lineSequence().any { it.trim().isNotEmpty() && !it.trim().equals("0", true) }
        return Capability(true, twrp, orangeFox)
    }

    fun flashVerifiedZip(context: Context, filename: String, recovery: Recovery): Result<Unit> {
        val safeName = safeFilename(filename) ?: return Result.failure(IllegalArgumentException("Unsupported package filename"))
        if (!detect(context).rooted) return Result.failure(IllegalStateException("Root access was not granted"))
        val escaped = shellQuote("install /sdcard/Download/$safeName\n")
        val command = """
            set -eu
            mkdir -p /cache/recovery /data/cache/recovery /persist/cache/recovery 2>/dev/null || true
            wrote=0
            for f in /cache/recovery/openrecoveryscript /data/cache/recovery/openrecoveryscript /persist/cache/recovery/openrecoveryscript; do
              d=${'$'}(dirname "${'$'}f")
              if [ -d "${'$'}d" ] && printf '%s' $escaped > "${'$'}f"; then
                chmod 0644 "${'$'}f"
                wrote=1
                break
              fi
            done
            [ "${'$'}wrote" = "1" ]
            echo FLASH_READY_${recovery.name}
        """.trimIndent()
        val output = runRoot(command)
        return if (output.contains("FLASH_READY_${recovery.name}")) {
            runRoot("reboot recovery")
            Result.success(Unit)
        } else Result.failure(IllegalStateException("Unable to prepare OpenRecoveryScript"))
    }

    /** Raw DD is device-specific; dynamic-partition devices may require fastbootd instead. */
    fun flashImageWithDd(context: Context, filename: String, target: String): Result<Unit> {
        val safeName = safeFilename(filename) ?: return Result.failure(IllegalArgumentException("Unsupported image filename"))
        if (!safeName.endsWith(".img", true)) return Result.failure(IllegalArgumentException("DD flashing requires an uncompressed .img file"))
        val safeTarget = target.takeIf { it.matches(Regex("[A-Za-z0-9_+.-]+")) }
            ?: return Result.failure(IllegalArgumentException("Unsupported partition name"))
        if (!detect(context).rooted) return Result.failure(IllegalStateException("Root access was not granted"))
        val command = """
            set -eu
            src=/sdcard/Download/$safeName
            dst=/dev/block/by-name/$safeTarget
            [ -f "${'$'}src" ] || { echo SOURCE_IMAGE_MISSING; exit 2; }
            [ -b "${'$'}dst" ] || { echo TARGET_NOT_BLOCK_DEVICE; exit 3; }
            command -v blockdev >/dev/null 2>&1 || { echo BLOCKDEV_UNAVAILABLE; exit 4; }
            src_size=${'$'}(wc -c < "${'$'}src" | tr -d '[:space:]')
            dst_size=${'$'}(blockdev --getsize64 "${'$'}dst" | tr -d '[:space:]')
            [ "${'$'}src_size" -le "${'$'}dst_size" ] || { echo IMAGE_LARGER_THAN_PARTITION; exit 5; }
            dd if="${'$'}src" of="${'$'}dst" bs=4M conv=fsync
            sync
            echo DD_FLASH_OK
        """.trimIndent()
        return if (runRoot(command).contains("DD_FLASH_OK")) Result.success(Unit)
        else Result.failure(IllegalStateException("DD flash failed. Check root access, source file, block target, and partition size."))
    }

    private fun safeFilename(filename: String): String? = filename.takeIf { it.matches(Regex("[A-Za-z0-9._+()\\- ]+")) }
    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
    private fun runRoot(command: String): String = runCatching {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        process.waitFor()
        output
    }.getOrDefault("")
}
