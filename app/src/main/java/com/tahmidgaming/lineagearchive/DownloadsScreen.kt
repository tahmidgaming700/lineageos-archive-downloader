package com.tahmidgaming.lineagearchive

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DownloadsScreen(context: Context, downloads: List<DownloadStore.Item>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("ROMs are saved to the public Downloads folder. Downloads can resume from a partial file when the server supports HTTP range requests.")
        if (downloads.isEmpty()) {
            Text("No downloads yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(downloads.reversed(), key = { it.id }) { item -> DownloadCard(context, item) }
            }
        }
    }
}

@Composable
private fun DownloadCard(context: Context, item: DownloadStore.Item) {
    val downloading = item.status.startsWith("Downloading") || item.status == "Queued"
    val paused = item.status == "Paused"
    val pass = item.verified == true
    val fail = item.verified == false
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(item.filename, fontWeight = FontWeight.Bold)
            Text("${item.device} • ${item.version ?: "LineageOS"}")
            Text(item.status)
            if (downloading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (pass) Row { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.padding(3.dp)); Text("SHA-256 verified") }
            if (fail) Row { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.padding(3.dp)); Text("SHA-256 verification failed") }
            item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (downloading) Button(onClick = { DownloadHelper.pause(context, item.id) }) { Icon(Icons.Default.Pause, null); Text("Pause") }
                if (paused) Button(onClick = { DownloadHelper.resume(context, item) }) { Icon(Icons.Default.PlayArrow, null); Text("Resume") }
            }
        }
    }
}
