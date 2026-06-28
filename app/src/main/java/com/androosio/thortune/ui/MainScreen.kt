package com.androosio.thortune.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.androosio.thortune.utils.JdspUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Brand accent (matches the lower-screen panel's "charge" gradient), used sparingly for depth.
private val Accent = Color(0xFF8A5BFF)
private val AccentSoft = Color(0xFFB49DFF)

private enum class Destination(val label: String, val icon: ImageVector) {
    Display("Display", Icons.Filled.Contrast),
    Audio("Audio", Icons.Filled.GraphicEq),
    Settings("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(appState: AppState) {
    var selected by remember { mutableIntStateOf(0) }
    val destinations = Destination.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("ThorTune", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                destinations.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (destinations[selected]) {
                Destination.Audio -> AudioSection(appState)
                Destination.Display -> DisplaySection(appState)
                Destination.Settings -> SettingsSection(appState)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        // A hairline edge lifts the card off the near-black background instead of melting into it.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon)
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

/** A section icon set in a softly-lit rounded tile — gives each card header depth and a focal point. */
@Composable
private fun IconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.30f), Accent.copy(alpha = 0.08f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentSoft,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AudioSection(appState: AppState) {
    val context = LocalContext.current

    SectionCard("JamesDSP", Icons.Filled.GraphicEq) {
        Text(
            "System-wide audio DSP. Follow the steps to set it up — the engine re-applies " +
                "automatically on every boot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!appState.hasPServer) {
            Text(
                "No root method detected on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
        }

        // 1. Install (or reinstall) the Manager app — required before the engine is useful.
        Step(1, "Install the Manager", done = appState.managerInstalled) {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { appState.installManager() },
            ) {
                Text(if (appState.managerInstalled) "Reinstall Manager" else "Install Manager")
            }
        }

        // 2. Enable the engine: a runtime toggle over the PServer binder, set in its own tile.
        Step(2, "Turn on the engine", done = appState.jdspEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (appState.jdspEnabled) "Engine on" else "Engine off",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = appState.jdspEnabled,
                    // Enabling the engine is pointless until the Manager app is installed.
                    enabled = appState.managerInstalled,
                    onCheckedChange = { appState.toggleJdsp(it) },
                )
            }
        }

        // 3. Hand the recommended preset straight to JamesDSP's importer.
        Step(3, "Add the recommended preset") {
            Text(
                "Joey's Retro Handhelds tuning for the Thor's speakers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                // The Manager only accepts the import while its engine/service is running.
                enabled = appState.jdspEnabled,
                onClick = {
                    if (appState.importPreset()) {
                        Toast.makeText(context, "Choose \"Import and activate\" in JamesDSP", Toast.LENGTH_LONG).show()
                    } else {
                        // The file is still in Downloads for a manual import.
                        Toast.makeText(
                            context,
                            "Couldn't open JamesDSP. Saved to Downloads as ${JdspUtils.RECOMMENDED_PRESET_FILENAME} for manual import.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            ) {
                Text("Add preset to JamesDSP")
            }
            Text(
                "Opens JamesDSP — choose \"Import and activate\" to apply it. A copy is also saved " +
                    "to your Downloads. A video guide to the preset is linked in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Optional, outside the numbered flow: jump into the Manager for further tuning.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            "That's the essential setup. Want to go further? Open JamesDSP to fine-tune the EQ, " +
                "presets and effects yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = appState.managerInstalled,
            onClick = {
                if (!appState.openManager()) {
                    // The rootless Manager only exposes its UI while the engine is running.
                    Toast.makeText(context, "Turn the engine on first to open JamesDSP", Toast.LENGTH_SHORT).show()
                }
            },
        ) {
            Text("Open JamesDSP")
        }
    }
}

/** One numbered step in the JamesDSP setup flow: a badge + title above the step's control. */
@Composable
private fun Step(
    number: Int,
    title: String,
    done: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (done) Brush.linearGradient(listOf(Accent, Color(0xFF5A33C8)))
                    else Brush.linearGradient(
                        listOf(Accent.copy(alpha = 0.22f), Accent.copy(alpha = 0.10f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (done) Color.White else AccentSoft,
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun DisplaySection(appState: AppState) {
    SectionCard("Display saturation", Icons.Filled.Contrast) {
        if (!appState.saturationSupported) {
            Text(
                "Privileged access isn't available, so saturation can't be changed on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@SectionCard
        }

        Text(
            "Tune the colour intensity of both AYN Thor screens. 100% is stock; 80% is " +
                "recommended. Changes apply instantly and persist across reboots.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            "${(appState.saturation * 100).roundToInt()}%",
            style = MaterialTheme.typography.displaySmall.copy(
                brush = Brush.linearGradient(listOf(AccentSoft, Accent)),
                fontWeight = FontWeight.Bold,
            ),
        )

        Slider(
            value = appState.saturation,
            onValueChange = { appState.previewSaturation(it) },
            onValueChangeFinished = { appState.applySaturation(appState.saturation) },
            valueRange = 0f..2f,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { appState.applySaturation(0.8f) },
        ) {
            Text("Use recommended (80%)")
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { appState.resetSaturation() },
        ) {
            Text("Reset to stock (100%)")
        }
    }
}

@Composable
private fun SettingsSection(appState: AppState) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    SectionCard("Lower screen", Icons.Filled.Contrast) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Quick-controls panel", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Show the engine toggle and saturation presets on the Thor's bottom screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(12.dp))
            Switch(
                checked = appState.secondaryPanelEnabled,
                onCheckedChange = { appState.setPanelEnabled(it) },
            )
        }
    }

    SectionCard("About", Icons.Filled.Settings) {
        StatusRow("Version", versionName, true)
        LinkRow(Icons.Filled.PlayCircle, "Preset guide (video)", "Joey's Retro Handhelds on YouTube", "https://www.youtube.com/watch?v=kk5Q4DtMrME")
        LinkRow(Icons.Filled.Code, "Source code", "github.com/androosio/thortune", "https://github.com/androosio/thortune")
        LinkRow(Icons.Filled.Coffee, "Buy me a coffee", "buymeacoffee.com/androosio", "https://buymeacoffee.com/androosio")
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, subtitle: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    Toast.makeText(context, "No app available to open the link", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
        )
    }
    Spacer(Modifier.height(0.dp))
}
