package com.tahmidgaming.lineagearchive

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { ArchiveApp() } } }
    }
}

private enum class Screen { HOME, DEVICES, BUILDS, ARCHIVE, DOWNLOADS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var devices by remember { mutableStateOf<List<LineageDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<LineageDevice?>(null) }
    var builds by remember { mutableStateOf<List<LineageBuild>>(emptyList()) }
    var archiveBuilds by remember { mutableStateOf<List<ArchiveBuildSummary>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadDevices() = scope.launch {
        loading = true; error = null
        runCatching { LineageRepository.devices() }
            .onSuccess { devices = it; screen = Screen.DEVICES }
            .onFailure { error = it.message ?: "Unable to load devices" }
        loading = false
    }

    fun selectDevice(device: LineageDevice) {
        selected = device
        screen = Screen.BUILDS
        scope.launch {
            loading = true; error = null
            runCatching { LineageRepository.builds(device.model) }
                .onSuccess { builds = it.sortedByDescending { b -> b.datetime } }
                .onFailure { error = it.message ?: "Unable to load builds" }
            loading = false
        }
    }

    fun loadArchive() {
        val device = selected ?: return
        screen = Screen.ARCHIVE
        scope.launch {
            loading = true; error = null
            runCatching { LineageRepository.archiveBuilds(device.model) }
                .onSuccess { archiveBuilds = it.sortedByDescending { archiveDate(it.filename) ?: 0L } }
                .onFailure { error = it.message ?: "Unable to load archive" }
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (screen == Screen.HOME) "LineageOS Archive Downloader" else titleFor(screen)) },
            navigationIcon = { if (screen != Screen.HOME) IconButton(onClick = { screen = Screen.HOME }) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = { if (screen == Screen.HOME) IconButton(onClick = { screen = Screen.SETTINGS }) { Icon(Icons.Default.Settings, "Settings") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (screen) {
                Screen.HOME -> HomeContent(DeviceDetector.current(), ::loadDevices, { screen = Screen.DOWNLOADS }, {
                    if (selected != null) loadArchive() else loadDevices()
                }, selected)
                Screen.DEVICES -> DeviceContent(devices, query, { query = it }, loading, error, ::selectDevice)
                Screen.BUILDS -> BuildContent(selected, builds, loading, error) { file, version ->
                    DownloadHelper.enqueue(context, file, selected?.model ?: "device", version)
                    screen = Screen.DOWNLOADS
                }
                Screen.ARCHIVE -> ArchiveContent(selected, archiveBuilds, loading, error) { summary ->
                    scope.launch {
                        loading = true; error = null
                        runCatching { LineageRepository.archiveBuild(summary.id) }
                            .onSuccess { detail ->
                                if (detail.url != null) {
                                    val file = LineageFile(
                                        filename = detail.filename,
                                        size = detail.filesize,
                                        sha256 = detail.sha256,
                                        url = detail.url
                                    )
                                    DownloadHelper.enqueue(context, file, selected?.model ?: summary.device, archiveVersion(detail.filename))
                                    screen = Screen.DOWNLOADS
                                } else {
                                    error = "This archived build is currently not stored online."
                                }
                            }
                            .onFailure { error = it.message ?: "Unable to open archived build" }
                        loading = false
                    }
                }
                Screen.DOWNLOADS -> DownloadsContent {
                    runCatching { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                }
                Screen.SETTINGS -> SettingsContent()
            }
        }
    }
}

private fun titleFor(screen: Screen) = when (screen) {
    Screen.DEVICES -> "Choose device"
    Screen.BUILDS -> "Available builds"
    Screen.ARCHIVE -> "Archive"
    Screen.DOWNLOADS -> "Downloads"
    Screen.SETTINGS -> "Settings & About"
    Screen.HOME -> "Home"
}

@Composable
private fun HomeContent(
    detected: DeviceDetector.Info,
    onChoose: () -> Unit,
    onDownloads: () -> Unit,
    onArchive: () -> Unit,
    selected: LineageDevice?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Find LineageOS builds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Browse official LineageOS builds or older builds preserved by the TimSchumi archive. This app only downloads files; it never flashes or modifies your device.")
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Detected Android device", fontWeight = FontWeight.Bold)
            Text("${detected.manufacturer} ${detected.model}")
            Text("Device: ${detected.device} • Product: ${detected.product}")
            selected?.let { Text("Selected: ${it.name} (${it.model})") }
        } }
        Button(onClick = onChoose, modifier = Modifier.fillMaxWidth()) { Text("Choose device") }
        OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth(), enabled = selected != null) {
            Icon(Icons.Default.Archive, null); Spacer(Modifier.width(8.dp)); Text("Browse archived builds")
        }
        Button(onClick = onDownloads, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Downloads") }
    }
}

@Composable
private fun DeviceContent(devices: List<LineageDevice>, query: String, onQuery: (String) -> Unit, loading: Boolean, error: String?, onSelect: (LineageDevice) -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search manufacturer, model or codename") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
    Spacer(Modifier.height(10.dp))
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) CircularProgressIndicator()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(devices.filter { query.isBlank() || it.name.contains(query, true) || it.model.contains(query, true) || it.oem.contains(query, true) }) { device ->
            Card(Modifier.fillMaxWidth(), onClick = { onSelect(device) }) { Column(Modifier.padding(14.dp)) {
                Text(device.name, fontWeight = FontWeight.Bold); Text("${device.oem} • ${device.model}")
            } }
        }
    }
}

@Composable
private fun BuildContent(device: LineageDevice?, builds: List<LineageBuild>, loading: Boolean, error: String?, onDownload: (LineageFile, String?) -> Unit) {
    device?.let { Text("${it.name} • ${it.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) CircularProgressIndicator()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(builds) { build -> build.files.forEach { file ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("LineageOS ${build.version ?: "unknown"}", fontWeight = FontWeight.Bold)
                Text(file.filename)
                Text("Build: ${formatDate(build.datetime)} • ${formatBytes(file.size)}")
                Text("SHA-256: ${file.sha256 ?: "not supplied"}")
                Text("Android SDK: ${file.os_sdk_level ?: "unknown"} • Patch: ${file.os_patch_level ?: "unknown"}")
                Button(onClick = { onDownload(file, build.version) }, enabled = file.url != null) { Text(if (file.url != null) "Download official ZIP" else "Download URL unavailable") }
            } }
        } }
    }
}

@Composable
private fun ArchiveContent(device: LineageDevice?, builds: List<ArchiveBuildSummary>, loading: Boolean, error: String?, onDownload: (ArchiveBuildSummary) -> Unit) {
    Text("Older builds • ${device?.name ?: "device"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Unofficial archive by TimSchumi. These builds are old/unsupported; verify the SHA-256 and LineageOS signature before use.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) CircularProgressIndicator()
    if (!loading && builds.isEmpty() && error == null) Text("No archived builds were found for this device.")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(builds) { build ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(build.filename, fontWeight = FontWeight.Bold)
                Text("Device: ${build.device}")
                Text("${archiveVersion(build.filename) ?: "LineageOS"} • ${archiveDate(build.filename)?.let { formatDate(it) } ?: "date unknown"}")
                Button(onClick = { onDownload(build) }) { Text("Download archived ZIP") }
            } }
        }
    }
}

@Composable
private fun DownloadsContent(onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Downloaded ROM ZIPs are stored in Android's public Downloads area.")
        Button(onClick = onOpen) { Text("Open Downloads") }
        Text("SHA-256 verification will be added to the downloader workflow. Archived builds should also be signature-verified before use.")
    }
}

@Composable
private fun SettingsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settings & About", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Version 0.1.0")
        Text("Official LineageOS builds are fetched from LineageOS infrastructure. Older builds can also be fetched from the separate TimSchumi archive.")
        Text("The TimSchumi archive is unofficial and warns that archived builds may contain security issues and are unsupported by the LineageOS team.")
        Text("No flashing, bootloader unlocking, recovery changes, or partition modification is performed by this app.")
    }
}

private fun formatDate(seconds: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(seconds * 1000))

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "size unknown"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KiB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MiB", mb)
    return String.format(Locale.US, "%.2f GiB", mb / 1024.0)
}

private fun archiveVersion(filename: String): String? {
    val match = Regex("lineage-([0-9]+(?:\\.[0-9]+)*)-").find(filename.lowercase())
    return match?.groupValues?.getOrNull(1)?.let { "LineageOS $it" }
}

private fun archiveDate(filename: String): Long? {
    val match = Regex("-(\\d{8,14})-").find(filename)
    val raw = match?.groupValues?.getOrNull(1) ?: return null
    return runCatching {
        val pattern = when (raw.length) {
            14 -> "yyyyMMddHHmmss"
            12 -> "yyyyMMddHHmm"
            10 -> "yyyyMMddHH"
            8 -> "yyyyMMdd"
            else -> return null
        }
        SimpleDateFormat(pattern, Locale.US).parse(raw)?.time?.div(1000)
    }.getOrNull()
}
