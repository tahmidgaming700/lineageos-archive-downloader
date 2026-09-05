package com.tahmidgaming.lineagearchive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreferences.get(context)) }
            ArchiveTheme(themeMode) {
                ArchiveApp(themeMode, { mode ->
                    themeMode = mode
                    ThemePreferences.set(context, mode)
                })
            }
        }
    }
}

private enum class Screen { HOME, DEVICES, BUILDS, ARCHIVE, DOWNLOADS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveApp(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val detected = remember { DeviceDetector.current() }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var devices by remember { mutableStateOf<List<LineageDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<LineageDevice?>(null) }
    var builds by remember { mutableStateOf<List<LineageBuild>>(emptyList()) }
    var archiveBuilds by remember { mutableStateOf<List<ArchiveBuildSummary>>(emptyList()) }
    var downloads by remember { mutableStateOf(DownloadStore.items(context)) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var initialChecking by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var pendingDownload by remember { mutableStateOf<Triple<LineageFile, String, String?>?>(null) }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingDownload
        pendingDownload = null
        if (granted && pending != null) {
            val (file, device, version) = pending
            DownloadHelper.enqueue(context, file, device, version)
            screen = Screen.DOWNLOADS
        }
    }

    fun startDownload(file: LineageFile, device: String, version: String?) {
        if (android.os.Build.VERSION.SDK_INT in 23..28 && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = Triple(file, device, version)
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            DownloadHelper.enqueue(context, file, device, version)
            screen = Screen.DOWNLOADS
        }
    }

    suspend fun refreshForDevice(device: LineageDevice?) {
        if (device == null) return
        runCatching { LineageRepository.builds(device.model) }
            .onSuccess { builds = it.sortedByDescending(LineageBuild::datetime) }
            .onFailure { error = it.message ?: "Unable to check for builds" }
    }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        runCatching { LineageRepository.devices() }
            .onSuccess { list ->
                devices = list
                selected = DeviceDetector.match(detected, list)
                refreshForDevice(selected)
            }
            .onFailure { error = it.message ?: "Unable to connect to LineageOS" }
        loading = false
        initialChecking = false
    }

    LaunchedEffect(screen) {
        while (screen == Screen.DOWNLOADS) {
            downloads = DownloadStore.items(context)
            delay(350)
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
            refreshForDevice(device)
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
                .onSuccess { archiveBuilds = it.sortedByDescending { item -> item.id } }
                .onFailure { error = it.message ?: "Unable to load archive" }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        GlassBackground()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (screen != Screen.HOME && screen != Screen.DOWNLOADS) {
                    TopAppBar(
                        title = { Text(titleFor(screen), fontWeight = FontWeight.SemiBold) },
                        navigationIcon = { IconButton(onClick = { screen = Screen.HOME }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                if (screen != Screen.DOWNLOADS) FloatingNavigationBar(screen) { target ->
                    when (target) {
                        Screen.ARCHIVE -> if (selected != null) loadArchive() else loadDevices()
                        else -> screen = target
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                when (screen) {
                    Screen.HOME -> if (initialChecking) CheckingContent() else HomeContent(
                        detected, selected, builds.firstOrNull()?.files?.firstOrNull(), builds.firstOrNull(), loading, error,
                        onRefresh = { scope.launch { loading = true; error = null; refreshForDevice(selected); loading = false } },
                        onChoose = ::loadDevices,
                        onDownloads = { screen = Screen.DOWNLOADS },
                        onArchive = ::loadArchive,
                        onDownload = { file, version -> startDownload(file, selected?.model ?: "device", version) }
                    )
                    Screen.DEVICES -> DeviceContent(devices, query, { query = it }, loading, error, ::selectDevice)
                    Screen.BUILDS -> BuildContent(selected, builds, loading, error) { file, version -> startDownload(file, selected?.model ?: "device", version) }
                    Screen.ARCHIVE -> ArchiveContent(selected, archiveBuilds, loading, error) { summary ->
                        scope.launch {
                            loading = true
                            error = null
                            runCatching { LineageRepository.archiveBuild(summary.id) }
                                .onSuccess { detail ->
                                    if (detail.url != null) startDownload(LineageFile(detail.filename, detail.filesize, detail.sha256, detail.url), selected?.model ?: summary.device, archiveVersion(detail.filename))
                                    else error = "This archived build is not currently stored online."
                                }
                                .onFailure { error = it.message ?: "Unable to open archived build" }
                            loading = false
                        }
                    }
                    Screen.DOWNLOADS -> DownloadsScreen(context, downloads, { screen = Screen.HOME }, { screen = Screen.SETTINGS })
                    Screen.SETTINGS -> SettingsContent(themeMode, onThemeModeChange)
                }
            }
        }
    }
}

@Composable
private fun GlassBackground() {
    val dark = LocalArchiveDarkTheme.current
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(if (dark) listOf(Color(0xFF18304A), Color(0xFF0C1017), Color(0xFF07090D)) else listOf(Color(0xFFE9F4FF), Color(0xFFF5F7FB), Color(0xFFEDEFF4)))))
}

@Composable
private fun FloatingNavigationBar(screen: Screen, onSelect: (Screen) -> Unit) {
    val dark = LocalArchiveDarkTheme.current
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), shape = RoundedCornerShape(30.dp), color = if (dark) Color(0xCC171B22) else Color(0xD9FFFFFF), tonalElevation = 0.dp, shadowElevation = 14.dp, border = BorderStroke(1.dp, if (dark) Color.White.copy(.10f) else Color.White.copy(.85f))) {
        NavigationBar(Modifier.height(72.dp), containerColor = Color.Transparent, tonalElevation = 0.dp) {
            NavItem("Home", Icons.Default.Home, screen == Screen.HOME) { onSelect(Screen.HOME) }
            NavItem("Archive", Icons.Default.Archive, screen == Screen.ARCHIVE) { onSelect(Screen.ARCHIVE) }
            NavItem("Downloads", Icons.Default.Download, screen == Screen.DOWNLOADS) { onSelect(Screen.DOWNLOADS) }
            NavItem("Settings", Icons.Default.Settings, screen == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected, onClick, icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(.15f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
}

private fun titleFor(screen: Screen) = when (screen) {
    Screen.DEVICES -> "Choose device"
    Screen.BUILDS -> "Available builds"
    Screen.ARCHIVE -> "Archive"
    Screen.DOWNLOADS -> "Software update"
    Screen.SETTINGS -> "Settings"
    Screen.HOME -> "LineageOS Downloader"
}

@Composable
private fun CheckingContent() {
    Column(Modifier.fillMaxSize().padding(bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Software update", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp)); EmuiUpdateRing(null, "LineageOS", "Updater"); Spacer(Modifier.height(28.dp))
        Text("Checking…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp)); Text("Checking for available builds", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeContent(detected: DeviceDetector.Info, selected: LineageDevice?, latest: LineageFile?, latestBuild: LineageBuild?, loading: Boolean, error: String?, onRefresh: () -> Unit, onChoose: () -> Unit, onDownloads: () -> Unit, onArchive: () -> Unit, onDownload: (LineageFile, String?) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Software Update", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("LineageOS Archive Downloader", color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Check again") } } }
        item { GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(Icons.Default.PhoneAndroid); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("This device", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${detected.manufacturer} ${detected.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("${detected.device} • ${detected.product}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(12.dp)); StatusPill(if (selected != null) "Supported • ${selected.name}" else "Select a supported device", if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } }
        item { GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Latest build", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (latestBuild != null && latest != null) { Spacer(Modifier.height(4.dp)); Text("LineageOS ${latestBuild.version ?: ""}".trim(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(formatDate(latestBuild.datetime), color = MaterialTheme.colorScheme.onSurfaceVariant) } else if (!loading) Text("No current build found", color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (latest != null && latestBuild != null) FilledIconButton(onClick = { onDownload(latest, latestBuild.version) }) { Icon(Icons.Default.Download, "Download latest build") } }; if (loading) { Spacer(Modifier.height(16.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) } else if (latest != null) { Spacer(Modifier.height(14.dp)); BuildMetaRow("Package", latest.filename); BuildMetaRow("Size", formatBytes(latest.size)); BuildMetaRow("Security patch", latest.os_patch_level ?: "Not supplied"); latest.sha256?.let { BuildMetaRow("SHA-256", it.take(16) + "…") } } } }
        error?.let { item { ErrorCard(it, onRefresh) } }
        item { Button(onClick = onChoose, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(20.dp)) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Choose device", fontWeight = FontWeight.SemiBold) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onArchive, enabled = selected != null, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Archive, null); Spacer(Modifier.width(6.dp)); Text("Archive") }; OutlinedButton(onClick = onDownloads, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Downloads") } } }
    }
}

@Composable
private fun DeviceContent(devices: List<LineageDevice>, query: String, onQuery: (String) -> Unit, loading: Boolean, error: String?, onSelect: (LineageDevice) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(6.dp)); OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Manufacturer, model or codename") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(20.dp)); Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) { items(devices.filter { query.isBlank() || it.name.contains(query, true) || it.model.contains(query, true) || it.oem.contains(query, true) }) { device ->
            GlassCard(onClick = { onSelect(device) }) { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(Icons.Default.PhoneAndroid); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("${device.oem} • ${device.model}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) } }
        } }
    }
}

@Composable
private fun BuildContent(selected: LineageDevice?, builds: List<LineageBuild>, loading: Boolean, error: String?, onDownload: (LineageFile, String?) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        selected?.let { Text(it.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Official LineageOS builds", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()); error?.let { ErrorCard(it) }
        if (!loading && builds.isEmpty() && error == null) EmptyState(Icons.Default.SystemUpdate, "No builds found", "The official updater returned no builds for this device.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(builds) { build -> build.files.firstOrNull()?.let { file ->
            GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("LineageOS ${build.version ?: ""}".trim(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(formatDate(build.datetime), color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledIconButton(onClick = { onDownload(file, build.version) }) { Icon(Icons.Default.Download, "Download") } }; Spacer(Modifier.height(10.dp)); BuildMetaRow("Package", file.filename); BuildMetaRow("Size", formatBytes(file.size)); BuildMetaRow("Patch", file.os_patch_level ?: "Not supplied"); file.sha256?.let { BuildMetaRow("SHA-256", it.take(20) + "…") } }
        } } }
    }
}

@Composable
private fun ArchiveContent(selected: LineageDevice?, archives: List<ArchiveBuildSummary>, loading: Boolean, error: String?, onDownload: (ArchiveBuildSummary) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(Icons.Default.Archive); Spacer(Modifier.width(12.dp)); Column { Text("Unofficial archive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Historical builds • ${selected?.name ?: "device"}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        Spacer(Modifier.height(12.dp)); if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()); error?.let { ErrorCard(it) }
        if (!loading && archives.isEmpty() && error == null) EmptyState(Icons.Default.Archive, "No archived builds", "No historical packages were returned for this device.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(archives) { item -> GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.filename, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.device, color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledIconButton(onClick = { onDownload(item) }) { Icon(Icons.Default.Download, "Download archived build") } } } } }
    }
}

@Composable
private fun DownloadsScreen(context: android.content.Context, downloads: List<DownloadStore.Item>, onBack: () -> Unit, onSettings: () -> Unit) {
    val active = downloads.lastOrNull { it.status.startsWith("Downloading") || it.status == "Queued" || it.status == "Paused" }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Software update", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } }
        if (active != null) {
            val percent = downloadPercent(active.status)
            val animated = animateFloatAsState((percent ?: 0) / 100f, tween(450), label = "download-progress")
            Column(Modifier.fillMaxSize().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(24.dp)); EmuiUpdateRing(animated.value, "LineageOS", active.version ?: "Archive"); Spacer(Modifier.height(24.dp)); Text(if (active.status == "Paused") "Paused" else if (active.status == "Queued") "Preparing…" else "Downloading…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Text(percent?.let { "$it%" } ?: "Preparing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text(active.filename, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(active.device, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(22.dp)); if (active.status == "Paused") Button(onClick = { DownloadHelper.resume(context, active) }, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(7.dp)); Text("Resume") } else OutlinedButton(onClick = { DownloadHelper.pause(context, active.id) }, shape = RoundedCornerShape(18.dp)) { Text("Pause") } }
        } else {
            Text("Downloads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); if (downloads.isEmpty()) EmptyState(Icons.Default.Download, "No downloads yet", "Downloaded builds will appear here.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(downloads.asReversed()) { DownloadHistoryCard(it) } }
        }
    }
}

@Composable
private fun EmuiUpdateRing(progress: Float?, centerTitle: String, centerSubtitle: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(286.dp)) {
        Surface(Modifier.size(286.dp), CircleShape, color = MaterialTheme.colorScheme.surface.copy(.42f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(.65f)), shadowElevation = 16.dp) {}
        if (progress == null) CircularProgressIndicator(Modifier.size(238.dp), strokeWidth = 9.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(.65f))
        else CircularProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.size(238.dp), strokeWidth = 9.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(.7f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.SystemUpdate, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text(centerTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(centerSubtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DownloadHistoryCard(item: DownloadStore.Item) { GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(if (item.verified == true) Icons.Default.CheckCircle else Icons.Default.Download); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.device, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.status, color = if (item.verified == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable
private fun SettingsContent(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 30.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("One UI-inspired controls with Liquid Glass surfaces", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { GlassCard { SettingRow(Icons.Default.Settings, "Appearance", "Choose how the downloader looks"); Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { ThemeChoice("System", ThemeMode.SYSTEM, themeMode, onThemeModeChange, Modifier.weight(1f)); ThemeChoice("Light", ThemeMode.LIGHT, themeMode, onThemeModeChange, Modifier.weight(1f)); ThemeChoice("Dark", ThemeMode.DARK, themeMode, onThemeModeChange, Modifier.weight(1f)) } } }
        item { GlassCard { SettingRow(Icons.Default.Shield, "Verification", "Downloads are checked with SHA-256 when the source provides a checksum."); Spacer(Modifier.height(16.dp)); SettingRow(Icons.Default.Info, "Safety", "This app downloads and verifies ROM packages only. It never flashes partitions or modifies your device.") } }
        item { GlassCard { SettingRow(Icons.Default.Wifi, "Sources", "Official LineageOS builds and the clearly labeled TimSchumi archive."); Spacer(Modifier.height(16.dp)); SettingRow(Icons.Default.Storage, "Compatibility", "Android 5.0+ with resumable downloads where the server supports HTTP Range requests."); Spacer(Modifier.height(16.dp)); Text("Version 1.0.3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable
private fun ThemeChoice(label: String, mode: ThemeMode, selected: ThemeMode, onClick: (ThemeMode) -> Unit, modifier: Modifier) {
    val chosen = mode == selected
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.clickable { onClick(mode) }.background(if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(.55f), shape).border(1.dp, if (chosen) MaterialTheme.colorScheme.primary.copy(.25f) else MaterialTheme.colorScheme.outlineVariant.copy(.45f), shape), contentAlignment = Alignment.Center) { Text(label, modifier = Modifier.padding(vertical = 12.dp), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal) }
}

@Composable
private fun GlassCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalArchiveDarkTheme.current
    val shape = RoundedCornerShape(26.dp)
    val modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Box(modifier = modifier.background(if (dark) Color(0x991B2029) else Color(0xB8FFFFFF), shape).border(1.dp, if (dark) Color.White.copy(.10f) else Color.White.copy(.78f), shape)) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
private fun IconBubble(icon: androidx.compose.ui.graphics.vector.ImageVector) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(.75f)) { Icon(icon, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) } }

@Composable
private fun StatusPill(text: String, color: Color) { Surface(shape = RoundedCornerShape(50), color = color.copy(.11f)) { Text(text, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun BuildMetaRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text(label, Modifier.width(92.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) } }

@Composable
private fun ErrorCard(message: String, onRetry: (() -> Unit)? = null) { GlassCard { Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Something went wrong", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(3.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant); if (onRetry != null) { Spacer(Modifier.height(10.dp)); TextButton(onClick = onRetry) { Text("Try again") } } } } } }

private fun downloadPercent(status: String): Int? = Regex("(\\d+)%").find(status)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
private fun formatBytes(size: Long?): String = when { size == null || size < 0 -> "Unknown"; size < 1024 -> "$size B"; size < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", size / 1024.0); size < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0)); else -> String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0)) }
private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(timestamp * 1000))
private fun archiveVersion(filename: String): String? = Regex("lineage-([0-9.]+)-").find(filename)?.groupValues?.getOrNull(1)
