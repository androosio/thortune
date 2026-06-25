package com.androosio.thortune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// One owned identity, always dark. The Thor is an OLED handheld; a fixed storm-night scheme keeps
// the "thunder" accent consistent rather than handing the palette to system dynamic colour.
private val ThorColorScheme = darkColorScheme(
    primary = ThunderBlue,
    onPrimary = ThunderBlueOn,
    primaryContainer = ThunderBlueDeep,
    onPrimaryContainer = ThunderBlueOnContainer,
    secondary = StormSteel,
    onSecondary = ThunderBlueOn,
    secondaryContainer = StormSteelContainer,
    onSecondaryContainer = MistHigh,
    tertiary = VioletSpark,
    onTertiary = ThunderBlueOn,
    tertiaryContainer = VioletSparkContainer,
    onTertiaryContainer = MistHigh,
    background = StormNight,
    onBackground = MistHigh,
    surface = StormNight,
    onSurface = MistHigh,
    surfaceVariant = StormSurfaceHigh,
    onSurfaceVariant = MistLow,
    surfaceContainerLowest = StormNight,
    surfaceContainerLow = StormSurfaceLow,
    surfaceContainer = StormSurface,
    surfaceContainerHigh = StormSurfaceHigh,
    surfaceContainerHighest = StormSurfaceHighest,
    outline = StormOutline,
    outlineVariant = StormOutlineVariant,
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ThorColorScheme,
        typography = Typography,
        content = content
    )
}
