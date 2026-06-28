package com.androosio.thortune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// "Storm" backdrop and the electric-purple "charge" gradient — the brand's lightning identity.
private val StormTop = Color(0xFF14121C)
private val StormBottom = Color(0xFF09080E)
private val ChargeStart = Color(0xFF8A5BFF)
private val ChargeEnd = Color(0xFF3A1FA6)
private val InertTile = Color(0xFF1B1924)

/**
 * Quick-controls surface for the Thor's lower screen (≈537×468 dp landscape). A two-tile
 * dashboard: tap the whole **Audio** tile to toggle the engine — it charges up with the brand
 * gradient when on — and use the **Display** tile's presets for saturation. Reads the same
 * [AppState] as the main UI, so a change on either screen recomposes the other.
 */
@Composable
fun CompanionPanel(appState: AppState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(StormTop, StormBottom)))
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AudioTile(appState, Modifier.weight(1f).fillMaxHeight())
                DisplayTile(appState, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Bolt,
                contentDescription = null,
                tint = ChargeStart,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "ThorTune",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "QUICK CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 2.sp,
            )
        }
        // Thin gradient hairline echoing the bolt.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ChargeStart, ChargeStart.copy(alpha = 0f)),
                    ),
                ),
        )
    }
}

@Composable
private fun AudioTile(appState: AppState, modifier: Modifier) {
    val installed = appState.managerInstalled
    val on = appState.jdspEnabled && installed

    val background =
        if (on) Brush.linearGradient(listOf(ChargeStart, ChargeEnd))
        else Brush.linearGradient(listOf(InertTile, InertTile))
    val foreground = if (on) Color.White else Color.White.copy(alpha = 0.92f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .clickable(enabled = installed) { appState.toggleJdsp(!appState.jdspEnabled) }
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(30.dp),
            )
            StatusDot(active = on)
        }
        Column {
            Text(
                "JAMESDSP",
                style = MaterialTheme.typography.labelMedium,
                color = foreground.copy(alpha = 0.7f),
                letterSpacing = 2.sp,
            )
            Text(
                when {
                    !installed -> "Set up"
                    on -> "On"
                    else -> "Off"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = foreground,
            )
            Text(
                if (!installed) "Install the Manager first" else "Tap to toggle the engine",
                style = MaterialTheme.typography.bodySmall,
                color = foreground.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun DisplayTile(appState: AppState, modifier: Modifier) {
    val supported = appState.saturationSupported
    val pct = (appState.saturation * 100).roundToInt()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(InertTile)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            Icons.Filled.Contrast,
            contentDescription = null,
            tint = ChargeStart,
            modifier = Modifier.size(30.dp),
        )
        Column {
            Text(
                "SATURATION",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 2.sp,
            )
            Text(
                if (supported) "$pct%" else "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        if (supported) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PresetChip("80%", selected = pct == 80, modifier = Modifier.weight(1f)) {
                    appState.applySaturation(0.8f)
                }
                PresetChip("100%", selected = pct == 100, modifier = Modifier.weight(1f)) {
                    appState.resetSaturation()
                }
            }
        } else {
            Text(
                "Unavailable on this device",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container = if (selected) ChargeStart else Color.White.copy(alpha = 0.08f)
    val content = if (selected) Color.White else Color.White.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = content)
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color.White else Color.White.copy(alpha = 0.25f)),
    )
}
