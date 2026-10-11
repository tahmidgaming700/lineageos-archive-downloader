package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GsiDiscoveryScreen(
    gsis: List<AddonDownload>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDownload: (AddonDownload) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var architecture by remember { mutableStateOf(GsiCompatibilityDetector.defaultArchitecture()) }
    var search by remember { mutableStateOf("") }
    var compatibility by remember { mutableStateOf<GsiCompatibilityInfo?>(null) }
    LaunchedEffect(Unit) { compatibility = GsiCompatibilityDetector.detect() }

    val filtered = gsis.filter { item ->
        val name = item.name.lowercase()
        val matchesArchitecture = when (architecture) {
            "arm64" -> name.contains("arm64") || name.contains("aarch64") || name.contains("a64")
            "arm" -> (name.contains("arm") || name.contains("a32")) && !name.contains("arm64")
            "x86_64" -> name.contains("x86_64") || name.contains("x64")
            "x86" -> (name.contains("x86") || name.contains("i686")) && !name.contains("x86_64")
            else -> true
        }
        matchesArchitecture && (search.isBlank() || name.contains(search.trim().lowercase()))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GSI Discovery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh GSI releases") }
                    }
                    Text("Live community Generic System Image release assets from public Treble/LineageOS projects. Results are discovery links, not a compatibility guarantee.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    compatibility?.let { info ->
                        Text("Detected ABI: " + info.primaryAbi + " • Android API " + info.sdk, fontWeight = FontWeight.SemiBold)
                        Text("Treble: " + info.trebleStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("A/B: " + info.partitionStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Dynamic partitions: " + info.dynamicPartitions.ifBlank { "unknown — confirm manually" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("These properties are hints only. Confirm the exact Android version, vendor interface, partition layout, and project instructions. Compressed .img.xz/.zip files must be extracted and verified before raw flashing. Incorrect GSIs can prevent boot or erase data.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Filter by CPU architecture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "arm64", "arm", "x86_64", "x86").forEach { arch ->
                            FilterChip(selected = architecture == arch, onClick = { architecture = arch }, label = { Text(arch) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Filter by build name/version") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (filtered.isEmpty() && !loading) {
                        Text("No matching GSI assets were returned. Refresh later or open the source repositories below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    filtered.take(40).forEach { item ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text(item.version + " • " + item.source, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { onDownload(item) }) { Text("Download and verify") }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Source repositories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Check the source project's notes for supported architecture, Android release, and installation method.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { uriHandler.openUri("https://github.com/ponces/treble_build_los/releases") }, modifier = Modifier.fillMaxWidth()) { Text("Treble build releases") }
                    TextButton(onClick = { uriHandler.openUri("https://github.com/AndyYan/lineage_build_unified/releases") }, modifier = Modifier.fillMaxWidth()) { Text("Unified LineageOS releases") }
                }
            }
        }
    }
}
