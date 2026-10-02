package com.tahmidgaming.lineagearchive

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object FlashManager {
    data class Capability(
        val rooted: Boolean,
        val twrpDetected: Boolean
    ) {
        val ready: Boolean get() = rooted && twrpDetected
    }

    fun detect(context: Context): Capability {
        val rooted = runRoot("id").contains("uid=0")
        if (!rooted) return Capability(false, false)

        val twrpProperty = runRoot("getprop ro.twrp.version").trim()
        val recoverySignature = runRoot(
            "for p in /dev/block/by-name/recovery /dev/block/bootdevice/by-name/recovery; do " +
                "if [ -r \"\$p\" ]; then grep -a -m 1 -i -E 'TWRP|TeamWin' \"\$p\" >/dev/null 2>&1 && echo yes && exit 0; fi; " +
                "done; exit 1"
        ).contains("yes", ignoreCase = true)

        return Capability(true, twrpProperty.isNotBlank() || recoverySignature)
    }

    fun flashVerifiedZip(context: Context, filename: String): Result<Unit> {
        val safeName = filename.takeIf { it.matches(Regex("[A-Za-z0-9._+()\- ]+")) }
            ?: return Result.failure(IllegalArgumentException("Unsupported ROM filename"))

        val capability = detect(context)
        if (!capability.rooted) return Result.failure(IllegalStateException("Root access was not granted"))
        if (!capability.twrpDetected) return Result.failure(IllegalStateException("TWRP was not detected"))

        val script = "install /sdcard/Download/$safeName\n"
        val escaped = shellQuote(script)
        val command = """
            mkdir -p /cache/recovery /data/cache/recovery /persist/cache/recovery 2>/dev/null
            wrote=0
            for f in /cache/recovery/openrecoveryscript /data/cache/recovery/openrecoveryscript /persist/cache/recovery/openrecoveryscript; do
              d=$(dirname "\$f")
              if [ -d "\$d" ] && printf '%s' $escaped > "\$f"; then chmod 0644 "\$f"; wrote=1; break; fi
            done
            [ "\$wrote" = "1" ] || exit 2
            echo FLASH_READY
            reboot recovery
        """.trimIndent().replace("\n", " ")

        val output = runRoot(command)
        return if (output.contains("FLASH_READY")) Result.success(Unit)
        else Result.failure(IllegalStateException("Unable to prepare TWRP flash"))
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun runRoot(command: String): String = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        process.waitFor()
        output + BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
    }.getOrDefault("")
}
