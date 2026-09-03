package com.tahmidgaming.lineagearchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArchiveTheme { ArchiveApp() } }
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
    var downloads by remember { mutableStateOf(DownloadStore.items(context)) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { LineageRepository.devices() }
            .onSuccess { list -> devices = list; selected = DeviceDetector.match(DeviceDetector.current(), list) }
    }
    LaunchedEffect(screen) {
        while (screen == Screen.DOWNLOADS) {
            downloads = DownloadStore.items(context)
            delay(700)
        }
    }

    fun loadDevices() = scope.launch {
        loading = true
        error = null
        runCatching { LineageRepository.devices() }
            .onSuccess { devices = it; screen = Screen.DEVICES }
            .onFailure { error = it.message ?: "Unable to load devices" }
        loading = false
    }

    fun selectDevice(device: LineageDevice) {
        selected = device
        screen = Screen.BUILDS
        scope.launch {
            loading = true
            error = null
            runCatching { LineageRepository.builds(device.model) }
                .onSuccess { builds = it.sortedByDescending(LineageBuild::datetime) }
                .onFailure { error = it.message ?: "Unable to load builds" }
            loading = false
        }
    }

    fun loadArchive() {
        val device = selected ?: return
        screen = Screen.ARCHIVE
        scope.launch {
            loading = true
            error = null
            runCatching { LineageRepository.archiveBuilds(device.model) }
                .onSuccess { archiveBuilds = it.sortedByDescending { archiveDate(it.filename) ?: 0L } }
                .onFailure { error = it.message ?: "Unable to load archive" }
            loading = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (screen == Screen.HOME) "LineageOS Downloader" else titleFor(screen), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (screen != Screen.HOME) {
                        IconButton(onClick = { screen = Screen.HOME }) { Icon(Icons.Default.ArrowBack, "Back") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .82f)) {
                NavItem("Home", Icons.Default.Home, screen == Screen.HOME) { screen = Screen.HOME }
                NavItem("Archive", Icons.Default.Archive, screen == Screen.ARCHIVE) { if (selected != null) loadArchive() else loadDevices() }
                NavItem("Downloads", Icons.Default.Download, screen == Screen.DOWNLOADS) { screen = Screen.DOWNLOADS }
                NavItem("Settings", Icons.Default.Settings, screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainer)))
        ) {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                when (screen) {
                    Screen.HOME -> HomeContent(DeviceDetector.current(), selected, ::loadDevices, { screen = Screen.DOWNLOADS }, ::loadArchive)
                    Screen.DEVICES -> DeviceContent(devices, query, { query = it }, loading, error, ::selectDevice)
                    Screen.BUILDS -> BuildContent(selected, builds, loading, error) { file, version ->
                        DownloadHelper.enqueue(context, file, selected?.model ?: "device", version)
                        screen = Screen.DOWNLOADS
                    }
                    Screen.ARCHIVE -> ArchiveContent(selected, archiveBuilds, loading, error) { summary ->
                        scope.launch {
                            loading = true
                            error = null
                            runCatching { LineageRepository.archiveBuild(summary.id) }
                                .onSuccess { detail ->
                                    if (detail.url != null) {
                                        DownloadHelper.enqueue(
                                            context,
                                            LineageFile(detail.filename, detail.filesize, detail.sha256, detail.url),
                                            selected?.model ?: summary.device,
                                            archiveVersion(detail.filename)
                                        )
                                        screen = Screen.DOWNLOADS
                                    } else {
                                        error = "This archived build is not currently stored online."
                                    }
                                }
                                .onFailure { error = it.message ?: "Unable to open archived build" }
                            loading = false
                        }
                    }
                    Screen.DOWNLOADS -> DownloadsScreen(context, downloads)
                    Screen.SETTINGS -> SettingsContent()
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label) })
}

private fun titleFor(screen: Screen) = when (screen) {
    Screen.DEVICES -> "Choose device"
    Screen.BUILDS -> "Available builds"
    Screen.ARCHIVE -> "Archive"
    Screen.DOWNLOADS -> "Downloads"
    Screen.SETTINGS -> "Settings"
    Screen.HOME -> "Home"
}

@Composable
private fun HomeContent(detected: DeviceDetector.Info, selected: LineageDevice?, onChoose: () -> Unit, onDownloads: () -> Unit, onArchive: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Find your next build.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("A clean, downloader-only way to discover LineageOS builds and preserved older releases.", style = MaterialTheme.typography.bodyLarge)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Detected device", fontWeight = FontWeight.Bold)
                Text("${detected.manufacturer} ${detected.model}")
                Text("${detected.device} • ${detected.product}")
                selected?.let { Text("Matched: ${it.name} (${it.model})", color = MaterialTheme.colorScheme.primary) }
            }
        }
        Button(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Choose device")
        }
        OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth(), enabled = selected != null) {
            Icon(Icons.Default.Archive, null); Spacer(Modifier.width(8.dp)); Text("Browse older builds")
        }
        OutlinedButton(onClick = onDownloads, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("View downloads")
        }
    }
}

@Composable
private fun DeviceContent(devices: List<LineageDevice>, query: String, onQuery: (String) -> Unit, loading: Boolean, error: String?, onSelect: (LineageDevice) -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Manufacturer, model or codename") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
    Spacer(Modifier.height(10.dp))
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        items(devices.filter { query.isBlank() || it.name.contains(query, true) || it.model.contains(query, true) || it.oem.contains(query, true) }) { device ->
            Card(onClick = { onSelect(device) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f))) {
                Column(Modifier.padding(15.dp)) { Text(device.name, fontWeight = FontWeight.SemiBold); Text("${device.oem} • ${device.model}") }
            }
        }
    }
}

@Composable
private fun BuildContent(device: LineageDevice?, builds: List<LineageBuild>, loading: Boolean, error: String?, onDownload: (LineageFile, String?) -> Unit) {
    device?.let { Text("${it.name} • ${it.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        items(builds) { build ->
            build.files.forEach { file ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .76f))) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("LineageOS ${build.version ?: "unknown"}", fontWeight = FontWeight.Bold)
                        Text(file.filename)
                        Text("${formatDate(build.datetime)} • ${formatBytes(file.size)}")
                        Text("SHA-256  ${file.sha256 ?: "not supplied"}", style = MaterialTheme.typography.bodySmall)
                        Text("Android ${file.os_sdk_level ?: "?"} • Patch ${file.os_patch_level ?: "?"}")
                        Button(onClick = { onDownload(file, build.version) }, enabled = file.url != null) { Text(if (file.url != null) "Download ZIP" else "URL unavailable") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveContent(device: LineageDevice?, builds: List<ArchiveBuildSummary>, loading: Boolean, error: String?, onDownload: (ArchiveBuildSummary) -> Unit) {
    Text("Older builds • ${device?.name ?: "device"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("TimSchumi archive • unofficial, old and unsupported. Verify SHA-256 and the LineageOS signature before use.", style = MaterialTheme.typography.bodyMedium)
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        items(builds) { build ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .76f))) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(build.filename, fontWeight = FontWeight.SemiBold)
                    Text("${archiveVersion(build.filename) ?: "LineageOS"} • ${archiveDate(build.filename)?.let(::formatDate) ?: "date unknown"}")
                    Button(onClick = { onDownload(build) }) { Text("Download archived ZIP") }
                }
            }
        }
    }
}

@Composable
private fun SettingsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LineageOS Archive Downloader", fontWeight = FontWeight.Bold)
                Text("Version 0.2.0")
                Text("Official builds come from LineageOS infrastructure. Older builds come from the separate TimSchumi archive.")
                Text("SHA-256 is checked after every downloaded file when a trusted digest is supplied. Signature verification is still recommended for ROM authenticity.")
                Text("This app never unlocks the bootloader, flashes a ROM, changes recovery, or modifies partitions.")
            }
        }
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

private fun archiveVersion(filename: String): String? = Regex("lineage-([0-9]+(?:\\.[0-9]+)*)-").find(filename.lowercase())?.groupValues?.getOrNull(1)?.let { "LineageOS $it" }

private fun archiveDate(filename: String): Long? {
    val raw = Regex("-(\\d{8,14})-").find(filename)?.groupValues?.getOrNull(1) ?: return null
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
