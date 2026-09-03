package com.tahmidgaming.lineagearchive

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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

private enum class Screen { HOME, DEVICES, BUILDS, DOWNLOADS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var devices by remember { mutableStateOf<List<LineageDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<LineageDevice?>(null) }
    var builds by remember { mutableStateOf<List<LineageBuild>>(emptyList()) }
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
        selected = device; screen = Screen.BUILDS
        scope.launch {
            loading = true; error = null
            runCatching { LineageRepository.builds(device.model) }
                .onSuccess { builds = it.sortedByDescending { b -> b.datetime } }
                .onFailure { error = it.message ?: "Unable to load builds" }
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (screen == Screen.HOME) "LineageOS Archive Downloader" else titleFor(screen)) },
            navigationIcon = { if (screen != Screen.HOME) IconButton({ screen = Screen.HOME }) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = { if (screen == Screen.HOME) IconButton({ screen = Screen.SETTINGS }) { Icon(Icons.Default.Settings, "Settings") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (screen) {
                Screen.HOME -> HomeContent(DeviceDetector.current(), ::loadDevices, { screen = Screen.DOWNLOADS })
                Screen.DEVICES -> DeviceContent(devices, query, { query = it }, loading, error, ::selectDevice)
                Screen.BUILDS -> BuildContent(selected, builds, loading, error) { file, version ->
                    DownloadHelper.enqueue(context, file, selected?.model ?: "device", version)
                    screen = Screen.DOWNLOADS
                }
                Screen.DOWNLOADS -> DownloadsContent {
                    runCatching { context.startActivity(Intent(Settings.ACTION_DOWNLOADS_SETTINGS)) }
                }
                Screen.SETTINGS -> SettingsContent()
            }
        }
    }
}

private fun titleFor(screen: Screen) = when (screen) {
    Screen.DEVICES -> "Choose device"
    Screen.BUILDS -> "Available builds"
    Screen.DOWNLOADS -> "Downloads"
    Screen.SETTINGS -> "Settings & About"
    Screen.HOME -> "Home"
}

@Composable
private fun HomeContent(detected: DeviceDetector.Info, onChoose: () -> Unit, onDownloads: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Find official LineageOS builds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Browse official builds, download the ZIP, and verify it before using it. This app never flashes or modifies your device.")
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Detected Android device", fontWeight = FontWeight.Bold)
            Text("${detected.manufacturer} ${detected.model}")
            Text("Device: ${detected.device} • Product: ${detected.product}")
        } }
        Button(onChoose, Modifier.fillMaxWidth()) { Text("Choose device") }
        Button(onDownloads, Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Downloads") }
    }
}

@Composable
private fun DeviceContent(devices: List<LineageDevice>, query: String, onQuery: (String) -> Unit, loading: Boolean, error: String?, onSelect: (LineageDevice) -> Unit) {
    OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Search manufacturer, model or codename") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
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
private fun DownloadsContent(onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Downloaded ROM ZIPs are stored in Android's public Downloads area.")
        Button(onOpen) { Text("Open Downloads") }
        Text("SHA-256 verification will be performed against the checksum supplied by LineageOS in the next downloader milestone.")
    }
}

@Composable
private fun SettingsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settings & About", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Version 0.1.0")
        Text("Official-source downloader only. No flashing, bootloader unlocking, recovery changes, or partition modification.")
        Text("LineageOS can remove older builds from its official servers. This app does not replace them with third-party ROM archives.")
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
