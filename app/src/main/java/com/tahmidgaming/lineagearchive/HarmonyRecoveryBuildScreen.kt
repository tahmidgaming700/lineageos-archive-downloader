package com.tahmidgaming.lineagearchive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HarmonyRecoveryBuildScreen(context: Context) {
    var manifestUrl by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var codename by remember { mutableStateOf("") }
    var lunchTarget by remember { mutableStateOf("") }
    var buildTarget by remember { mutableStateOf("recoveryimage") }
    var script by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val dollar = '$'

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Harmony Recovery Build Lab", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Prepare a reproducible recovery-image build script for a real Linux build host. This screen does not compile or fabricate an image on the phone. Use only a trusted source and verify the device tree, kernel, vendor blobs, and build instructions.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Recovery builds can require 100+ GB of free storage, substantial RAM, Java/toolchains, and hours of build time. The exact lunch target and build target depend on the source tree.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = manifestUrl,
                        onValueChange = { manifestUrl = it; script = ""; error = null },
                        label = { Text("Manifest repository URL (HTTPS)") },
                        placeholder = { Text("https://github.com/owner/manifest") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it; script = ""; error = null },
                        label = { Text("Manifest branch") },
                        placeholder = { Text("Enter the branch from project documentation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = codename,
                        onValueChange = { codename = it; script = ""; error = null },
                        label = { Text("Device codename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = lunchTarget,
                        onValueChange = { lunchTarget = it; script = ""; error = null },
                        label = { Text("Exact lunch target") },
                        placeholder = { Text("Use the target specified by your source tree") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = buildTarget,
                        onValueChange = { buildTarget = it; script = ""; error = null },
                        label = { Text("Build target") },
                        placeholder = { Text("recoveryimage") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val validUrl = manifestUrl.matches(
                                Regex("^https://[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9._~:/-]+$")
                            )
                            val validBranch = branch.matches(Regex("[A-Za-z0-9._/-]+")) &&
                                !branch.startsWith("-") && !branch.contains("..")
                            val validCodename = codename.matches(Regex("[A-Za-z0-9_-]+"))
                            val validLunch = lunchTarget.matches(Regex("[A-Za-z0-9_.-]+"))
                            val validTarget = buildTarget.matches(Regex("[A-Za-z0-9_.-]+"))
                            if (!validUrl || !validBranch || !validCodename || !validLunch || !validTarget) {
                                error = "Check every field. Use an HTTPS manifest URL and simple branch, codename, lunch-target, and build-target values."
                                script = ""
                            } else {
                                error = null
                                val shellHome = dollar.toString() + "{HOME}"
                                val shellWorkdir = dollar.toString() + "WORKDIR"
                                val shellNproc = dollar.toString() + "(nproc)"
                                script = """
                                    #!/usr/bin/env bash
                                    set -euo pipefail

                                    # Harmony Recovery build plan for device: $codename
                                    # Run only on a Linux build host with the required toolchain and disk space.
                                    # Confirm the manifest, branch, device tree and build target with the project maintainers.
                                    WORKDIR="$shellHome/harmony-recovery-$codename"
                                    mkdir -p "$shellWorkdir"
                                    cd "$shellWorkdir"

                                    repo init -u '$manifestUrl' -b '$branch'
                                    repo sync -c --force-sync -j"$shellNproc"
                                    source build/envsetup.sh
                                    lunch '$lunchTarget'
                                    mka '$buildTarget'

                                    echo "Build command finished. Locate the output using the source tree's documented output path."
                                """.trimIndent()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Generate Linux build script") }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (script.isNotBlank()) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Generated script", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            script,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 30,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Harmony Recovery build script", script))
                                Toast.makeText(context, "Build script copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copy build script") }
                        TextButton(
                            onClick = { script = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Clear script") }
                    }
                }
            }
        }
    }
}
