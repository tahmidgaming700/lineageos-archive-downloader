package com.tahmidgaming.lineagearchive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class HuaweiTab { FINDER, DOWNLOADS, TOOLS, SETTINGS }

class HuaweiMainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(ThemePreferences.get(this)) }
            ArchiveTheme(themeMode) {
                HuaweiUpdaterApp(
                    themeMode = themeMode,
                    onThemeChange = {
                        themeMode = it
                        ThemePreferences.set(this, it)
                    },
                    openUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HuaweiUpdaterApp(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    openUrl: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(HuaweiTab.FINDER) }
    var model by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("C636") }
    var vendor by remember { mutableStateOf("hw-eu") }
    var version by remember { mutableStateOf("") }
    var proxy by remember { mutableStateOf(HuaweiFirmwareRepository.DEFAULT_PROXY) }
    var result by remember { mutableStateOf<HuaweiFirmwareRepository.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var downloads by remember { mutableStateOf(DownloadStore.items(context)) }
    var refreshCounter by remember { mutableIntStateOf(0) }

    fun runQuery(fileList: Boolean) {
        if (model.isBlank() || region.isBlank()) {
            error = "Enter a phone model and region before searching."
            return
        }
        busy = true
        error = null
        result = null
        scope.launch {
            runCatching {
                val query = HuaweiFirmwareRepository.Query(model, region, vendor, version)
                if (fileList) HuaweiFirmwareRepository.getFileList(proxy, query)
                else HuaweiFirmwareRepository.checkStatus(proxy, query)
            }.onSuccess { result = it }
                .onFailure { error = it.message ?: "The firmware request failed." }
            busy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Huawei OS Updater", fontWeight = FontWeight.Bold)
                        Text("Native firmware finder and downloader", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    TextButton(onClick = { openUrl("https://professorjtj.github.io/") }) {
                        androidx.compose.material3.Icon(Icons.Default.OpenInBrowser, contentDescription = "Open website")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HuaweiTab.FINDER,
                    onClick = { tab = HuaweiTab.FINDER },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Search, null) },
                    label = { Text("Finder") }
                )
                NavigationBarItem(
                    selected = tab == HuaweiTab.DOWNLOADS,
                    onClick = { downloads = DownloadStore.items(context); tab = HuaweiTab.DOWNLOADS },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Download, null) },
                    label = { Text("Downloads") }
                )
                NavigationBarItem(
                    selected = tab == HuaweiTab.TOOLS,
                    onClick = { tab = HuaweiTab.TOOLS },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Build, null) },
                    label = { Text("Tools") }
                )
                NavigationBarItem(
                    selected = tab == HuaweiTab.SETTINGS,
                    onClick = { tab = HuaweiTab.SETTINGS },
                    icon = { androidx.compose.material3.Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (tab) {
                HuaweiTab.FINDER -> {
                    FinderHeader()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Device parameters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = model, onValueChange = { model = it.uppercase() },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Phone model") },
                                placeholder = { Text("e.g. JKM-LX2 or MHA-L29") }, singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = region, onValueChange = { region = it.uppercase() },
                                    modifier = Modifier.weight(1f), label = { Text("Region (CXXX)") }, singleLine = true
                                )
                                OutlinedTextField(
                                    value = vendor, onValueChange = { vendor = it },
                                    modifier = Modifier.weight(1f), label = { Text("Vendor/country") }, singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = version, onValueChange = { version = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Target version (optional)") },
                                placeholder = { Text("e.g. 9.1.0.396") }, singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { runQuery(false) }, enabled = !busy && model.isNotBlank() && region.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Icon(Icons.Default.Search, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Check status")
                                }
                                Button(
                                    onClick = { runQuery(true) }, enabled = !busy && model.isNotBlank() && region.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Icon(Icons.Default.SystemUpdate, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Find files")
                                }
                            }
                        }
                    }
                    if (busy) CircularProgressIndicator()
                    error?.let { MessageCard("Request failed", it, true) }
                    result?.let { response ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(response.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (response.files.isNotEmpty()) {
                                    Text("${response.files.size} candidate firmware file(s) detected.")
                                    response.files.forEach { file ->
                                        HorizontalDivider()
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(file.filename, fontWeight = FontWeight.SemiBold)
                                            file.size?.let { Text(formatBytes(it), style = MaterialTheme.typography.bodySmall) }
                                            Text(file.url, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            Button(onClick = {
                                                val item = LineageFile(file.filename, file.size, file.sha256, file.url)
                                                DownloadHelper.enqueue(context, item, model, file.version ?: version.takeIf { it.isNotBlank() })
                                                downloads = DownloadStore.items(context)
                                                tab = HuaweiTab.DOWNLOADS
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                androidx.compose.material3.Icon(Icons.Default.Download, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Download firmware")
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        "The proxy returned a response, but no direct firmware package links were recognised. Review the raw response below; the proxy may return a different format.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                                Text("Raw response", fontWeight = FontWeight.SemiBold)
                                Text(response.detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("How the finder connects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("The public Firm Finder is a JavaScript frontend. Its search actions require a compatible HiSuite Proxy service. The default address is localhost:7777; that only works when the proxy is reachable from this Android device.")
                            TextButton(onClick = { openUrl("https://professorjtj.github.io/") }) { Text("Open Huawei Firm Finder website") }
                        }
                    }
                }
                HuaweiTab.DOWNLOADS -> {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { downloads = DownloadStore.items(context); refreshCounter++ }) {
                            androidx.compose.material3.Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Refresh")
                        }
                    }
                    if (downloads.isEmpty()) {
                        MessageCard("No downloads yet", "Firmware downloads you start from Finder will appear here.")
                    } else {
                        downloads.asReversed().forEach { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(item.filename, fontWeight = FontWeight.SemiBold)
                                    Text(item.device + (item.version?.let { " • $it" } ?: ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.status)
                                    item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                if (item.status == "Paused" || item.status.startsWith("Failed")) DownloadHelper.resume(context, item)
                                                else DownloadHelper.pause(context, item.id)
                                                downloads = DownloadStore.items(context)
                                            }
                                        ) { Text(if (item.status == "Paused" || item.status.startsWith("Failed")) "Resume" else "Pause") }
                                        TextButton(onClick = { item.url.let(openUrl) }) { Text("Open source") }
                                    }
                                }
                            }
                        }
                    }
                }
                HuaweiTab.TOOLS -> {
                    Text("Huawei tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Open the related firmware utilities. These open in your browser; the updater interface itself is native.")
                    ToolCard("Firm Finder", "Search Huawei firmware by model, region and target version.", "https://professorjtj.github.io/", openUrl)
                    ToolCard("Firm Finder v2", "Alternative firmware finder interface.", "https://professorjtj.github.io/v2/", openUrl)
                    ToolCard("BaseArchive", "Browse archived Huawei firmware packages.", "https://professorjtj.github.io/BaseArchive/", openUrl)
                    ToolCard("Downgrade", "Open the downgrade information/tool page.", "https://professorjtj.github.io/Downgrade/", openUrl)
                }
                HuaweiTab.SETTINGS -> {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemeButton("System", ThemeMode.SYSTEM, themeMode, onThemeChange, Modifier.weight(1f))
                                ThemeButton("Light", ThemeMode.LIGHT, themeMode, onThemeChange, Modifier.weight(1f))
                                ThemeButton("Dark", ThemeMode.DARK, themeMode, onThemeChange, Modifier.weight(1f))
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("HiSuite Proxy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Set the address of a compatible HiSuite Proxy service. Do not leave localhost unless the service is running on this same device.")
                            OutlinedTextField(
                                value = proxy, onValueChange = { proxy = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Proxy base URL") }, singleLine = true
                            )
                            Text("Current address: $proxy", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Huawei OS Updater • Native Android UI")
                            Text("Firmware downloads use the original background download worker, resumable HTTP Range support, download history and SHA-256 verification when a checksum is available.")
                            Text("Firmware availability and installation compatibility are not guaranteed. Verify the model, region and build before flashing.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinderHeader() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text("Firmware Finder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Find packages for Huawei and Honor devices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String, isError: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToolCard(title: String, detail: String, url: String, openUrl: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { openUrl(url) }) { Text("Open tool") }
            }
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    mode: ThemeMode,
    selected: ThemeMode,
    onClick: (ThemeMode) -> Unit,
    modifier: Modifier
) {
    if (mode == selected) {
        Button(onClick = { onClick(mode) }, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = { onClick(mode) }, modifier = modifier) { Text(label) }
    }
}

private fun formatBytes(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(java.util.Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}
