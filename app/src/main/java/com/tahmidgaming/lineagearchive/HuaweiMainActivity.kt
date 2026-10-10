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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class HuaweiMainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var model by remember { mutableStateOf("") }
                var region by remember { mutableStateOf("") }
                var vendor by remember { mutableStateOf("hw-eu") }
                var version by remember { mutableStateOf("") }
                var proxy by remember { mutableStateOf(HuaweiFirmwareRepository.DEFAULT_PROXY) }
                var result by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                var busy by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Huawei OS Updater", fontWeight = FontWeight.Bold)
                                    Text("Native firmware finder", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://professorjtj.github.io/")))
                                }) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open website")
                                }
                            }
                        )
                    }
                ) { insets ->
                    Column(
                        Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Firmware search", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Enter the same device filters used by Huawei Firm Finder.")
                                OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Phone model (e.g. JKM-LX2)") }, singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(region, { region = it }, Modifier.weight(1f), label = { Text("Region (CXXX)") }, singleLine = true)
                                    OutlinedTextField(vendor, { vendor = it }, Modifier.weight(1f), label = { Text("Vendor/country") }, singleLine = true)
                                }
                                OutlinedTextField(version, { version = it }, Modifier.fillMaxWidth(), label = { Text("Target version (optional)") }, singleLine = true)
                                OutlinedTextField(proxy, { proxy = it }, Modifier.fillMaxWidth(), label = { Text("HiSuite Proxy address") }, singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = !busy && model.isNotBlank() && region.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            busy = true; error = null; result = ""
                                            scope.launch {
                                                runCatching {
                                                    HuaweiFirmwareRepository.checkStatus(
                                                        proxy,
                                                        HuaweiFirmwareRepository.Query(model, region, vendor, version)
                                                    )
                                                }.onSuccess { result = it.detail }
                                                    .onFailure { error = it.message ?: "Could not query firmware status." }
                                                busy = false
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                        Spacer(Modifier.height(0.dp))
                                        Text("Check status")
                                    }
                                    Button(
                                        enabled = !busy && model.isNotBlank() && region.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            busy = true; error = null; result = ""
                                            scope.launch {
                                                runCatching {
                                                    HuaweiFirmwareRepository.getFileList(
                                                        proxy,
                                                        HuaweiFirmwareRepository.Query(model, region, vendor, version)
                                                    )
                                                }.onSuccess { result = it.detail }
                                                    .onFailure { error = it.message ?: "Could not retrieve the file list." }
                                                busy = false
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                        Text("Get file list")
                                    }
                                }
                            }
                        }

                        if (busy) CircularProgressIndicator()
                        error?.let {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Request failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(6.dp))
                                    Text(it)
                                }
                            }
                        }
                        if (result.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Proxy response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(result)
                                }
                            }
                        }
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Connection note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "The public Firm Finder website is a JavaScript frontend, not a documented public JSON API. Its status and file-list actions call a HiSuite Proxy service. The default address is localhost:7777; it works only when that service is running at the configured address. This app does not pretend the website alone provides a public API."
                                )
                                TextButton(onClick = {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://professorjtj.github.io/")))
                                }) { Text("Open Huawei Firm Finder") }
                            }
                        }
                    }
                }
            }
        }
    }
}
