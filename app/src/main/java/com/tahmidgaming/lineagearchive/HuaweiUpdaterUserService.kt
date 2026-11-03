package com.tahmidgaming.lineagearchive

import android.os.RemoteException

/**
 * One-shot Shizuku UserService. Runs as shell/root and asks Android's Activity Manager
 * to open the system update settings screen. It does not install or modify firmware.
 */
class HuaweiUpdaterUserService : IUserService.Stub() {
    @Throws(RemoteException::class)
    override fun launchSystemUpdate(): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/am",
                    "start",
                    "-a",
                    "android.settings.SYSTEM_UPDATE_SETTINGS"
                )
            )
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            val details = (output + error).trim()
            if (exitCode == 0) {
                "System Update launch requested through Shizuku." +
                    if (details.isNotEmpty()) "\\n" + details.take(250) else ""
            } else {
                "System Update launch failed (exit $exitCode): " + details.take(350)
            }
        } catch (e: Exception) {
            "Shizuku command failed: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}