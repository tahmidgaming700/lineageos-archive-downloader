package com.tahmidgaming.lineagearchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
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
                ArchiveApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        ThemePreferences.set(context, mode)
                    }
                )
            }
        }
    }
}

private enum class Screen { HOME, DEVICES, BUILDS, ARCHIVE, DOWNLOADS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
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
                .onSuccess { archiveBuilds = it.sortedByDescending { item -> archiveDate(item.filename) ?: 0L } }
                .onFailure { error = it.message ?: "Unable to load archive" }
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (screen != Screen.HOME) {
                    TopAppBar(
                        title = { Text(titleFor(screen), fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { screen = Screen.HOME }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                FloatingNavigationBar(screen) { target ->
                    when (target) {
                        Screen.HOME -> screen = Screen.HOME
                        Screen.ARCHIVE -> if (selected != null) loadArchive() else loadDevices()
                        Screen.DOWNLOADS -> screen = Screen.DOWNLOADS
                        Screen.SETTINGS -> screen = Screen.SETTINGS
                        else -> screen = target
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp)
            ) {
                when (screen) {
                    Screen.HOME -> {
                        if (initialChecking) {
                            CheckingContent()
                        } else {
                            HomeContent(
                                detected = detected,
                                selected = selected,
                                latest = builds.firstOrNull()?.files?.firstOrNull(),
                                latestBuild = builds.firstOrNull(),
                                loading = loading,
                                error = error,
                                onRefresh = {
                                    scope.launch {
                                        loading = true
                                        error = null
                                        refreshForDevice(selected)
                                        loading = false
                                    }
                                },
                                onChoose = { loadDevices() },
                                onDownloads = { screen = Screen.DOWNLOADS },
                                onArchive = ::loadArchive
                            )
                        }
                    }
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
                    Screen.SETTINGS -> SettingsContent(themeMode, onThemeModeChange)
                }
            }
        }
    }
}

@Composable
private fun FloatingNavigationBar(screen: Screen, onSelect: (Screen) -> Unit) {
    val dark = LocalArchiveDarkTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        color = if (dark) Color(0xE91B1E26) else Color(0xEAFBFCFF),
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = .10f) else Color.White.copy(alpha = .90f))
    ) {
        NavigationBar(
            modifier = Modifier.height(68.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            NavItem("Home", Icons.Default.Home, screen == Screen.HOME) { onSelect(Screen.HOME) }
            NavItem("Archive", Icons.Default.Archive, screen == Screen.ARCHIVE) { onSelect(Screen.ARCHIVE) }
            NavItem("Downloads", Icons.Default.Download, screen == Screen.DOWNLOADS) { onSelect(Screen.DOWNLOADS) }
            NavItem("Settings", Icons.Default.Settings, screen == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        label = { Text(label, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .15f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

private fun titleFor(screen: Screen) = when (screen) {
    Screen.DEVICES -> "Choose device"
    Screen.BUILDS -> "Available builds"
    Screen.ARCHIVE -> "Archive"
    Screen.DOWNLOADS -> "Downloads"
    Screen.SETTINGS -> "Settings"
    Screen.HOME -> "LineageOS Downloader"
}

@Composable
private fun CheckingContent() {
    Column(
        Modifier.fillMaxSize().padding(bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(190.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("LineageOS", fontWeight = FontWeight.Bold)
                    Text("1.0.0", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Checking for builds…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Connecting to the official LineageOS updater", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeContent(
    detected: DeviceDetector.Info,
    selected: LineageDevice?,
    latest: LineageFile?,
    latestBuild: LineageBuild?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onChoose: () -> Unit,
    onDownloads: () -> Unit,
    onArchive: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Software Update", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("LineageOS Archive Downloader", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Check again") }
            }
        }
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your device", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${detected.manufacturer} ${detected.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${detected.device} • ${detected.product}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (selected != null) {
                    StatusPill("Matched: ${selected.name} • ${selected.model}", MaterialTheme.colorScheme.primary)
                } else {
                    StatusPill("Select a supported device", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GlassCard {
                Text("Latest build", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else if (latest != null && latestBuild != null) {
                    Text("LineageOS ${latestBuild.version ?: ""}".trim(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(formatDate(latestBuild.datetime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    BuildMetaRow("Package", latest.filename)
                    BuildMetaRow("Size", formatBytes(latest.size))
                    BuildMetaRow("Security patch", latest.os_patch_level ?: "Not supplied")
                    BuildMetaRow("Android", latest.os_sdk_level?.let { "SDK $it" } ?: "Not supplied")
                    Spacer(Modifier.height(10.dp))
                    if (latest.sha256 != null) StatusPill("SHA-256 available", MaterialTheme.colorScheme.tertiary)
                } else {
                    Text("No current build was returned for this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let { message -> item { ErrorCard(message, onRefresh) } }
        item {
            Button(
                onClick = onChoose,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose device", fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onArchive, enabled = selected != null, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.Archive, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Archive")
                }
                OutlinedButton(onClick = onDownloads, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Downloads")
                }
            }
        }
    }
}

@Composable
private fun DeviceContent(devices: List<LineageDevice>, query: String, onQuery: (String) -> Unit, loading: Boolean, error: String?, onSelect: (LineageDevice) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Manufacturer, model or codename") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            items(devices.filter { query.isBlank() || it.name.contains(query, true) || it.model.contains(query, true) || it.oem.contains(query, true) }) { device ->
                GlassCard(onClick = { onSelect(device) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text("${device.oem} • ${device.model}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildContent(device: LineageDevice?, builds: List<LineageBuild>, loading: Boolean, error: String?, onDownload: (LineageFile, String?) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(10.dp))
        device?.let {
            Text(it.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${it.oem} • ${it.model}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)) {
            items(builds.flatMap { build -> build.files.map { file -> build to file } }) { (build, file) ->
                GlassCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("LineageOS ${build.version ?: "Unknown"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(formatDate(build.datetime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill("Official", MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(file.filename, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    BuildMetaRow("Size", formatBytes(file.size))
                    BuildMetaRow("Android", file.os_sdk_level?.let { "SDK $it" } ?: "Unknown")
                    BuildMetaRow("Patch", file.os_patch_level ?: "Unknown")
                    BuildMetaRow("SHA-256", file.sha256 ?: "Not supplied")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onDownload(file, build.version) }, enabled = file.url != null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.CloudDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (file.url != null) "Download ZIP" else "URL unavailable")
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveContent(device: LineageDevice?, builds: List<ArchiveBuildSummary>, loading: Boolean, error: String?, onDownload: (ArchiveBuildSummary) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(10.dp))
        Text("Older releases", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${device?.name ?: "Selected device"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("TimSchumi archive", fontWeight = FontWeight.Bold)
                    Text("Unofficial • old • unsupported. Verify the SHA-256 and LineageOS signature before use.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(builds) { build ->
                GlassCard {
                    Text(build.filename, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text("${archiveVersion(build.filename) ?: "LineageOS"} • ${archiveDate(build.filename)?.let(::formatDate) ?: "date unknown"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onDownload(build) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.CloudDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download archived ZIP")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(context: android.content.Context, downloads: List<DownloadStore.Item>) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(10.dp))
        Text("Downloads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Files are checked before they are saved.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (downloads.isEmpty()) {
            EmptyState(Icons.Default.Download, "No downloads yet", "Choose a build to start a download.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                items(downloads, key = { it.id }) { item ->
                    GlassCard {
                        Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text("${item.device} • ${item.version ?: "Version unknown"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(item.status, fontWeight = FontWeight.Medium)
                        item.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.status.startsWith("Downloading") || item.status == "Queued") {
                                OutlinedButton(onClick = { DownloadHelper.pause(context, item.id) }, shape = RoundedCornerShape(14.dp)) { Text("Pause") }
                            } else if (item.status == "Paused") {
                                Button(onClick = { DownloadHelper.resume(context, item) }, shape = RoundedCornerShape(14.dp)) { Text("Resume") }
                            }
                            if (item.verified == true) StatusPill("SHA-256 PASS", MaterialTheme.colorScheme.tertiary)
                            else if (item.verified == false) StatusPill("SHA-256 FAILED", MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)) {
        item {
            Text("Settings & About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("A downloader and verifier — never a flasher.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            GlassCard {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Choose how the app looks. Your choice is saved on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChoice("System", ThemeMode.SYSTEM, themeMode, onThemeModeChange, Modifier.weight(1f))
                    ThemeChoice("Light", ThemeMode.LIGHT, themeMode, onThemeModeChange, Modifier.weight(1f))
                    ThemeChoice("Dark", ThemeMode.DARK, themeMode, onThemeModeChange, Modifier.weight(1f))
                }
            }
        }
        item {
            GlassCard {
                Text("LineageOS Archive Downloader", fontWeight = FontWeight.Bold)
                Text("Version 1.0.0", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("The interface uses a Liquid Glass inspired visual language adapted for Android: translucent navigation, clear hierarchy, rounded controls, restrained tinting and strong light/dark contrast.")
            }
        }
        item {
            GlassCard {
                SettingRow(Icons.Default.Shield, "Integrity", "SHA-256 is checked after downloads when an expected digest is supplied.")
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingRow(Icons.Default.Info, "Authenticity", "SHA-256 proves file integrity, not who signed the ROM. Verify LineageOS signatures when required.")
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingRow(Icons.Default.Archive, "Archive", "TimSchumi is a separate unofficial archive for older builds.")
            }
        }
        item {
            GlassCard {
                Text("Safety boundary", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("This app does not unlock bootloaders, flash ZIPs, replace recovery, modify partitions, or alter your system. It only discovers, downloads and verifies files.")
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = mode == selectedMode
    if (selected) {
        Button(
            onClick = { onSelect(mode) },
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { Text(label, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = { onSelect(mode) },
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { Text(label, maxLines = 1) }
    }
}

@Composable
private fun GlassCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalArchiveDarkTheme.current
    val modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, if (dark) Color.White.copy(alpha = .09f) else Color.White.copy(alpha = .85f), RoundedCornerShape(24.dp))
    val surfaceColor = if (dark) Color(0xD91B1E25) else Color(0xEAFBFCFF)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) { Column(Modifier.padding(17.dp), content = content) }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) { Column(Modifier.padding(17.dp), content = content) }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = .13f),
        border = BorderStroke(1.dp, color.copy(alpha = .22f))
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BuildMetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.padding(start = 12.dp).weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ErrorCard(message: String, retry: () -> Unit) {
    GlassCard {
        Text("Couldn't check for builds", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(4.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = retry) { Text("Try again") }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(seconds: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(seconds * 1000))

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "Size unknown"
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
