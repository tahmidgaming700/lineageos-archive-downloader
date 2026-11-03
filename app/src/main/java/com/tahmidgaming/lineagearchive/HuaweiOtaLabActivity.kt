package com.tahmidgaming.lineagearchive

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.ComponentName
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private data class JkmDeviceProfile(
    val model: String, val device: String, val build: String,
    val fingerprint: String, val cust: String, val isJkm: Boolean
)
private data class OtaCandidate(
    val role: String, val queryModel: String, val filename: String,
    val size: Long?, val sha256: String?, val url: String, val version: String?
)

class HuaweiOtaLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = detectJkmProfile()
        setContent {
            ArchiveTheme(ThemePreferences.get(this)) {
                JkmOtaLabScreen(
                    profile = profile,
                    proxyUrl = getSharedPreferences("huawei_settings", Context.MODE_PRIVATE)
                        .getString("proxy_url", HuaweiFirmwareRepository.DEFAULT_PROXY)
                        ?: HuaweiFirmwareRepository.DEFAULT_PROXY,
                    onDownload = { candidate ->
                        val item = LineageFile(candidate.filename, candidate.size, candidate.sha256, candidate.url)
                        DownloadHelper.enqueue(this, item, candidate.queryModel, candidate.version)
                        Toast.makeText(this, "Queued " + candidate.filename, Toast.LENGTH_LONG).show()
                    },
                    onOpenUpdater = { openUpdaterThroughShizuku() }
                )
            }
        }
    }

    private fun detectJkmProfile(): JkmDeviceProfile {
        fun prop(key: String): String = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", key))
            p.inputStream.bufferedReader().use { it.readText().trim() }
        }.getOrDefault("")
        val model = prop("ro.product.model").ifBlank { Build.MODEL }
        val device = prop("ro.product.device").ifBlank { Build.DEVICE }
        val build = prop("ro.build.display.id").ifBlank { Build.DISPLAY }
        val fingerprint = prop("ro.build.fingerprint").ifBlank { Build.FINGERPRINT }
        val cust = listOf(prop("ro.product.CustCVersion"), prop("ro.build.version.cust"), prop("ro.hw.country"))
            .firstOrNull { it.isNotBlank() }.orEmpty()
        val identity = (model + " " + device + " " + fingerprint).uppercase()
        return JkmDeviceProfile(model, device, build, fingerprint, cust, identity.contains("JKM"))
    }

    private fun openUpdaterThroughShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku is not running. Start its service first.", Toast.LENGTH_LONG).show()
                return
            }
            if (Shizuku.isPreV11()) {
                Toast.makeText(this, "Shizuku API v11 or newer is required.", Toast.LENGTH_LONG).show()
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(7301)
                Toast.makeText(this, "Grant Shizuku permission, then tap again.", Toast.LENGTH_LONG).show()
                return
            }

            val args = Shizuku.UserServiceArgs(
                ComponentName(packageName, HuaweiUpdaterUserService::class.java.name)
            ).daemon(false).processNameSuffix("ota_handoff").debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)

            val connection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: android.os.IBinder) {
                    Thread {
                        val message = runCatching {
                            IUserService.Stub.asInterface(service).launchSystemUpdate()
                        }.getOrElse { "Shizuku handoff failed: " + (it.message ?: "unknown error") }
                        runOnUiThread {
                            Toast.makeText(this@HuaweiOtaLabActivity, message, Toast.LENGTH_LONG).show()
                            runCatching { Shizuku.unbindUserService(args, this, true) }
                        }
                    }.start()
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    // The one-shot handoff has completed or the Shizuku service stopped.
                }
            }
            Shizuku.bindUserService(args, connection)
            Toast.makeText(this, "Starting privileged updater handoff…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Shizuku is unavailable", Toast.LENGTH_LONG).show()
        }
    }}

@Composable
private fun JkmOtaLabScreen(
    profile: JkmDeviceProfile,
    proxyUrl: String,
    onDownload: (OtaCandidate) -> Unit,
    onOpenUpdater: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var model by remember { mutableStateOf(profile.model.takeIf { it.contains("JKM", true) } ?: "JKM-LX2") }
    var baseModel by remember { mutableStateOf("JKM-LGRP2-OVS") }
    var region by remember { mutableStateOf(detectRegion(profile.cust)) }
    var vendor by remember { mutableStateOf(if (region == "C00") "hw-cn" else "hw-eu") }
    var targetVersion by remember { mutableStateOf("") }
    var sourceVersion by remember { mutableStateOf("") }
    var workflow by remember { mutableStateOf("FullOTA") }
    var advanced by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready. Configure the device and proxy, then search.") }
    val candidates = remember { mutableStateListOf<OtaCandidate>() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("JKM OTA Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Firmware discovery and OTA preparation for Huawei JKM devices.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Detected device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(profile.model + " · " + profile.device)
                Text("Build: " + profile.build)
                Text("CUST: " + profile.cust.ifBlank { "Not exposed" })
                Text("Fingerprint: " + profile.fingerprint, style = MaterialTheme.typography.bodySmall)
                Text(if (profile.isJkm) "JKM family detected" else "JKM not confirmed — editable manual profile")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OTA workflow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = workflow == "FullOTA", onClick = { workflow = "FullOTA" })
                    Text("FullOTA")
                    Spacer(Modifier.weight(1f))
                    RadioButton(selected = workflow == "Incremental OTA", onClick = { workflow = "Incremental OTA" })
                    Text("Incremental")
                }
                if (workflow == "FullOTA") {
                    Text("Search the BASE identifier and handset CUST model separately. Only combine components from the same release and region.")
                    OutlinedTextField(baseModel, { baseModel = it.uppercase() }, label = { Text("BASE identifier") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(model, { model = it.uppercase() }, label = { Text("Handset / CUST model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    Text("Incremental packages are valid only for their exact source build. Enter the currently installed source version.")
                    OutlinedTextField(model, { model = it.uppercase() }, label = { Text("Handset model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(sourceVersion, { sourceVersion = it }, label = { Text("Installed source build (required)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(region, { region = it.uppercase(); vendor = if (it.equals("C00", true)) "hw-cn" else vendor }, label = { Text("CUST region (e.g. C636, C185, C00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(vendor, { vendor = it }, label = { Text("Vendor / country") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(targetVersion, { targetVersion = it }, label = { Text("Target version (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = advanced, onCheckedChange = { advanced = it })
                    Text("Advanced experimental discovery")
                }
                if (advanced) {
                    Text("Experimental model tokens: JKM-LX1, JKM-LX2, JKM-LX3, JKM-AL00, JKM-TL00 and JKM-L00. China C00 results are candidates only; never cross-flash C00 and global CUST packages.")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { model = "JKM-L00" }) { Text("JKM-L00") }
                        TextButton(onClick = { model = "JKM-AL00"; region = "C00"; vendor = "hw-cn" }) { Text("AL00 · C00") }
                        TextButton(onClick = { model = "JKM-TL00"; region = "C00"; vendor = "hw-cn" }) { Text("TL00 · C00") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { model = "JKM-LX1" }) { Text("JKM-LX1") }
                        TextButton(onClick = { model = "JKM-LX2" }) { Text("JKM-LX2") }
                        TextButton(onClick = { model = "JKM-LX3" }) { Text("JKM-LX3") }
                    }
                }
                Button(
                    onClick = {
                        if (model.isBlank() || region.isBlank()) {
                            status = "Model and CUST region are required."
                            return@Button
                        }
                        if (workflow == "Incremental OTA" && sourceVersion.isBlank()) {
                            status = "Enter the exact installed source build before searching for an incremental OTA."
                            return@Button
                        }
                        val selectedModel = model
                        val selectedBaseModel = baseModel
                        val selectedRegion = region
                        val selectedVendor = vendor
                        val selectedTarget = targetVersion
                        val fullOtaSearch = workflow == "FullOTA"
                        val incrementalSource = sourceVersion
                        busy = true
                        status = "Searching the configured HiSuite Proxy…"
                        candidates.clear()
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                val found = mutableListOf<OtaCandidate>()
                                val messages = mutableListOf<String>()
                                val searches = if (fullOtaSearch) listOf(selectedBaseModel, selectedModel).distinct() else listOf(selectedModel)
                                searches.forEach { queryModel ->
                                    runCatching {
                                        val query = HuaweiFirmwareRepository.Query(queryModel, selectedRegion, selectedVendor, selectedTarget)
                                        HuaweiFirmwareRepository.getFileList(proxyUrl, query)
                                    }.onSuccess { response ->
                                        response.files.forEach { file ->
                                            val name = file.filename.uppercase()
                                            val role = when {
                                                "PRELOAD" in name -> "PRELOAD"
                                                "CUST" in name -> "CUST CANDIDATE"
                                                "LGRP" in name || "OVS" in name -> "BASE CANDIDATE"
                                                queryModel == selectedModel -> "HANDSET / CUST CANDIDATE"
                                                else -> "OTA PACKAGE"
                                            }
                                            found += OtaCandidate(role, queryModel, file.filename, file.size, file.sha256, file.url, file.version)
                                        }
                                        if (response.files.isEmpty()) messages += queryModel + ": " + response.title + " — " + response.detail.take(240)
                                    }.onFailure { messages += queryModel + ": " + (it.message ?: "request failed") }
                                }
                                Pair(found.distinctBy { it.url }, messages)
                            }
                            candidates.addAll(result.first)
                            status = if (result.first.isEmpty()) result.second.joinToString("\n").ifBlank {
                                "No direct package links were returned by the configured proxy."
                            } else {
                                val suffix = if (fullOtaSearch) "Verify BASE/CUST/PRELOAD release grouping." else
                                    "Entered source build " + incrementalSource + " is a manual note; the proxy response is not independently source-build verified."
                                "Found " + result.first.size + " candidate package(s). " + suffix
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) { Text(if (busy) "Searching…" else "Search OTA packages") }
                if (busy) CircularProgressIndicator()
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (candidates.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Candidate packages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    candidates.forEach { item ->
                        Text(item.role, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(item.filename, fontWeight = FontWeight.SemiBold)
                        Text(item.queryModel + " · " + (item.version ?: "version not parsed") + " · " + (item.size?.let(::formatBytes) ?: "size unknown"), style = MaterialTheme.typography.bodySmall)
                        Text(item.url, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onDownload(item) }, modifier = Modifier.fillMaxWidth()) { Text("Download package") }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Shizuku updater handoff", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("With Shizuku running and permission granted, this asks Android to open System Update through the shell. It does not inject a package, alter Huawei metadata, bypass signature checks or force the updater to accept an unsupported ZIP.")
                Button(onClick = onOpenUpdater, modifier = Modifier.fillMaxWidth()) { Text("Open System Update via Shizuku") }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent("android.settings.SYSTEM_UPDATE_SETTINGS")) }
                        .onFailure { Toast.makeText(context, "System Update screen is unavailable on this build.", Toast.LENGTH_LONG).show() }
                }) { Text("Open System Update normally") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Compatibility rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Supported profile inputs: JKM-LX1, JKM-LX2, JKM-LX3.")
                Text("• Chinese candidates: JKM-AL00 / JKM-TL00 and C00 firmware where the proxy lists them.")
                Text("• JKM-L00 is experimental discovery only; it is not assumed to be a confirmed matching device.")
                Text("• FullOTA BASE, CUST and PRELOAD must belong to the same release and CUST.")
                Text("• Incremental OTA must match the installed source build exactly.")
                Text("• A successful download is not proof that Huawei System Update will accept the package.")
            }
        }
    }
}

private fun detectRegion(cust: String): String =
    Regex("C[0-9]{3}|C00").find(cust.uppercase())?.value ?: "C636"

private fun formatBytes(size: Long): String = when {
    size < 1024 -> size.toString() + " B"
    size < 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(java.util.Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}
