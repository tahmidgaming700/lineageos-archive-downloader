package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UpdateCenterScreen(
    context: android.content.Context,
    selected: LineageDevice?,
    currentOsVersion: String,
    onDownload: (LineageFile, String?, String?) -> Unit,
    refreshKey: Int,
    onRefresh: () -> Unit
) {
    var loading by remember(refreshKey) { mutableStateOf(true) }
    var items by remember(refreshKey) { mutableStateOf<List<UpdateItem>>(emptyList()) }
    var recovery by remember(refreshKey) { mutableStateOf(UpdateRepository.localRecovery(context)) }

    LaunchedEffect(refreshKey, selected?.model, currentOsVersion) {
        loading = true
        recovery = UpdateRepository.localRecovery(context)
        items = listOf(
            runCatching { UpdateRepository.app(context) }.getOrNull(),
            runCatching { UpdateRepository.os(selected, currentOsVersion) }.getOrNull(),
            runCatching { UpdateRepository.gapps(android.os.Build.VERSION.SDK_INT) }.getOrNull(),
            runCatching { UpdateRepository.magisk(context) }.getOrNull()
        ).filterNotNull()
        loading = false
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)) {
        item { UpdateCardHeader(loading, onRefresh) }
        items.forEach { item ->
            item { UpdateCard(item) {
                val file = item.file ?: return@UpdateCard
                if (file.url.isNotBlank()) onDownload(LineageFile(file.name, file.size, file.sha256, file.url), selected?.model ?: "updates", file.version)
            } }
        }
        item { RecoveryUpdateCard("TWRP", recovery.first, "Official Team Win releases are device-specific. Match the detected codename before flashing.") }
        item { RecoveryUpdateCard("OrangeFox", recovery.second, "OrangeFox releases are device-specific. Match the detected codename and variant before flashing.") }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Update policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("OS, GApps and Magisk are checked from their release sources. TWRP and OrangeFox are device-specific, so their status is shown separately.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UpdateCardHeader(loading: Boolean, onRefresh: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("App, OS, GApps, Magisk & recovery status", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Check again") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun UpdateCard(item: UpdateItem, onDownload: () -> Unit) {
    val update = item.availableUpdate
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(when (item.type) {
                        "Magisk" -> Icons.Default.Security
                        "OS" -> Icons.Default.SystemUpdate
                        "GApps" -> Icons.Default.Android
                        else -> Icons.Default.Build
                    }, null)
                    Column {
                        Text(item.type, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (update) AssistChip(onClick = {}, label = { Text("Update") }, leadingIcon = { Icon(Icons.Default.Download, null) })
                else Icon(Icons.Default.CheckCircle, "Up to date", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text("Installed: " + item.installed)
            Text("Available: " + (item.available ?: "—"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.canDownload && item.file?.url?.isNotBlank() == true && (update || item.type == "GApps")) {
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (update) "Download update" else "Download latest")
                }
            }
        }
    }
}

@Composable
private fun RecoveryUpdateCard(name: String, installed: String, description: String) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Build, null)
                Column { Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(10.dp))
            Text("Installed: " + installed)
            Text("Latest: device-specific", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}