package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.layout.*
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
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDownloadAddon: (AddonDownload) -> Unit
) {
    var pendingRecovery by remember { mutableStateOf<Pair<DownloadStore.Item, FlashManager.Recovery>?>(null) }
    var pendingDd by remember { mutableStateOf<Pair<DownloadStore.Item, String>?>(null) }
    val verified = downloads.filter { it.verified == true && it.status.startsWith("Downloaded") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)) {
        item {
            GlassCard {
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
            GlassCard {
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
            GlassCard {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("boot", "recovery", "vendor_boot", "dtbo").forEach { target ->
                                FlashButton(target, capability.rooted) { pendingDd = item to target }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            GlassCard {
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
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Magisk Downloader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh Magisk") }
                }
                Text("Official topjohnwu/Magisk GitHub release assets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                magisk.take(10).forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        error?.let { item { ErrorCard(it, onRefresh) } }
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
private fun FlashButton(label: String, enabled: Boolean, onClick: () -> Unit) {
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
