package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ToolsScreen(
    context: android.content.Context,
    capability: FlashManager.Capability,
    downloads: List<DownloadStore.Item>,
    gapps: List<AddonDownload>,
    magisk: List<AddonDownload>,
    gsis: List<AddonDownload>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenHarmonyBuild: () -> Unit,
    onDownloadAddon: (AddonDownload) -> Unit
) {
    var pendingRecovery by remember { mutableStateOf<Pair<DownloadStore.Item, FlashManager.Recovery>?>(null) }
    var pendingDd by remember { mutableStateOf<Pair<DownloadStore.Item, String>?>(null) }
    val verified = downloads.filter { it.verified == true && it.status.startsWith("Downloaded") }
    val uriHandler = LocalUriHandler.current
    var gsiArch by remember { mutableStateOf("All") }
    var gsiSearch by remember { mutableStateOf("") }
    val filteredGsis = gsis.filter { item ->
        val name = item.name.lowercase()
        val matchesArch = when (gsiArch) {
            "arm64" -> name.contains("arm64") || name.contains("aarch64") || name.contains("a64")
            "arm" -> (name.contains("arm") || name.contains("a32")) && !name.contains("arm64")
            "x86_64" -> name.contains("x86_64") || name.contains("x64")
            "x86" -> (name.contains("x86") || name.contains("i686")) && !name.contains("x86_64")
            else -> true
        }
        matchesArch && (gsiSearch.isBlank() || name.contains(gsiSearch.trim().lowercase()))
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)) {
        item {
            ToolsCard {
                Text("Flashing & tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (capability.rooted) "Root detected. Recovery and image flashing controls are available below."
                    else "Root is not detected. Download tools are still available; flashing controls remain visible but disabled.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolStatus("Root", capability.rooted)
                    ToolStatus("TWRP", capability.twrpDetected)
                    ToolStatus("OrangeFox", capability.orangeFoxDetected)
                }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recovery flash", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("OpenRecoveryScript", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("Flash verified ZIP packages through TWRP, OrangeFox, or generic OpenRecoveryScript.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                if (verified.isEmpty()) {
                    Text("Download and SHA-256 verify a ZIP first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    verified.filter { it.filename.endsWith(".zip", true) }.take(12).forEach { item ->
                        Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            FlashButton("TWRP", capability.twrpDetected) { pendingRecovery = item to FlashManager.Recovery.TWRP }
                            FlashButton("OrangeFox", capability.orangeFoxDetected) { pendingRecovery = item to FlashManager.Recovery.ORANGEFOX }
                            FlashButton("OpenRecovery", capability.rooted) { pendingRecovery = item to FlashManager.Recovery.OPEN_RECOVERY }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            ToolsCard {
                Text("Harmony Recovery image build", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Prepare a reproducible Linux build script from a trusted manifest, branch, device codename, and the source tree's exact build target. The phone does not fabricate a recovery image.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenHarmonyBuild, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Build, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Open Harmony Recovery Build Lab")
                }
            }
        }

        item {
            ToolsCard {
                Text("dd image flasher", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Root-only raw image flashing to an explicitly selected by-name partition. This can permanently brick a device if the image or target is wrong.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                val images = verified.filter { it.filename.endsWith(".img", true) }
                if (images.isEmpty()) {
                    Text("No verified .img downloads available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    images.take(8).forEach { item ->
                        Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        listOf("boot", "boot_a", "boot_b", "system", "system_a", "system_b", "recovery", "vendor_boot", "dtbo")
                            .chunked(3).forEach { targets ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    targets.forEach { target ->
                                        FlashButton(target, capability.rooted) { pendingDd = item to target }
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Live GSI discovery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh GSI releases") }
                }
                Text("Community Generic System Image releases from public GitHub projects. Discovery is not a compatibility guarantee.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Before downloading, confirm Android version, CPU architecture, A/B layout, dynamic partitions, and your device's Treble support. A GSI can fail to boot or erase data if installed incorrectly.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = gsiSearch, onValueChange = { gsiSearch = it }, label = { Text("Filter GSI assets") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "arm64", "arm", "x86_64", "x86").forEach { arch ->
                        FilterChip(selected = gsiArch == arch, onClick = { gsiArch = arch }, label = { Text(arch) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (filteredGsis.isEmpty() && !loading) {
                    Text("No matching GSI assets were returned. Refresh later or open the source repositories below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                filteredGsis.take(20).forEach { AddonRow(it, onDownloadAddon) }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { uriHandler.openUri("https://github.com/ponces/treble_build_los/releases") }) { Text("Open Treble build releases") }
                TextButton(onClick = { uriHandler.openUri("https://github.com/AndyYan/lineage_build_unified/releases") }) { Text("Open unified LineageOS releases") }
            }
        }

        item {
            ToolsCard {
                Text("Recovery release tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Track Harmony Recovery, TWRP, OrangeFox, or another recovery project using its real GitHub owner/repository. Only assets returned by GitHub are shown; this tracker does not certify device compatibility.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                var recoveryRepo by remember { mutableStateOf(context.getSharedPreferences("tools", 0).getString("harmony_repo", "") ?: "") }
                var recoveryAssets by remember { mutableStateOf<List<AddonDownload>>(emptyList()) }
                var recoveryLoading by remember { mutableStateOf(false) }
                var recoveryError by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                OutlinedTextField(
                    value = recoveryRepo,
                    onValueChange = { recoveryRepo = it; context.getSharedPreferences("tools", 0).edit().putString("harmony_repo", it).apply() },
                    label = { Text("GitHub owner/repository") },
                    placeholder = { Text("owner/Harmony-Recovery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val parts = recoveryRepo.trim().trim('/').split('/')
                        if (parts.size != 2 || parts.any { it.isBlank() || !it.matches(Regex("[A-Za-z0-9_.-]+")) }) {
                            recoveryError = "Enter a valid owner/repository path."
                        } else {
                            scope.launch {
                                recoveryLoading = true
                                recoveryError = null
                                recoveryAssets = runCatching { AddonRepository.recoveryReleaseAssets(parts[0], parts[1]) }
                                    .onFailure { recoveryError = it.message ?: "Unable to load recovery releases." }
                                    .getOrDefault(emptyList())
                                recoveryLoading = false
                            }
                        }
                    },
                    enabled = !recoveryLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (recoveryLoading) "Checking releases…" else "Check Harmony Recovery releases") }
                recoveryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                recoveryAssets.forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GApps Downloader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh GApps") }
                }
                Text("Official MindTheGapps GitHub release assets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                gapps.take(12).forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Magisk Downloader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh Magisk") }
                }
                Text("Official topjohnwu/Magisk GitHub release assets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                magisk.take(10).forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        error?.let { item { ToolsCard { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRefresh) { Text("Try again") } } } }
    }

    pendingRecovery?.let { (item, recovery) ->
        AlertDialog(
            onDismissRequest = { pendingRecovery = null },
            title = { Text("Flash with " + recoveryLabel(recovery) + "?") },
            text = { Text("Only the already SHA-256 verified ZIP will be installed. The device will reboot to recovery. Confirm the package and make a backup before continuing.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRecovery = null
                    FlashManager.flashVerifiedZip(context, item.filename, recovery)
                }) { Text("Flash") }
            },
            dismissButton = { TextButton(onClick = { pendingRecovery = null }) { Text("Cancel") } }
        )
    }

    pendingDd?.let { (item, target) ->
        AlertDialog(
            onDismissRequest = { pendingDd = null },
            title = { Text("dd → " + target) },
            text = { Text("This writes " + item.filename + " directly to /dev/block/by-name/" + target + ". A wrong image or target can make the device unbootable. Continue only if you have confirmed the partition layout.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDd = null
                    FlashManager.flashImageWithDd(context, item.filename, target)
                }) { Text("Write image") }
            },
            dismissButton = { TextButton(onClick = { pendingDd = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ToolStatus(label: String, available: Boolean) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(if (available) label + " ✓" else label + " —") },
        leadingIcon = { Icon(if (available) Icons.Default.CheckCircle else Icons.Default.Security, null) }
    )
}

@Composable
private fun RowScope.FlashButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Icon(Icons.Default.Build, null, Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun AddonRow(item: AddonDownload, onDownload: (AddonDownload) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Storage, null, Modifier.padding(top = 2.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(item.version + " • " + formatAddonBytes(item.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(item.source, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            if (item.sha256 != null) Text("SHA-256 available", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalButton(onClick = { onDownload(item) }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Download")
        }
    }
}

private fun recoveryLabel(recovery: FlashManager.Recovery) = when (recovery) {
    FlashManager.Recovery.TWRP -> "TWRP"
    FlashManager.Recovery.ORANGEFOX -> "OrangeFox"
    FlashManager.Recovery.OPEN_RECOVERY -> "OpenRecovery"
}

private fun formatAddonBytes(size: Long): String = when {
    size < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}

@Composable
private fun ToolsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
