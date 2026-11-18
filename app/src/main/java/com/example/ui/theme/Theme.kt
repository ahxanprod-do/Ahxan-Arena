package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AhxanDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricViolet,
    onPrimaryContainer = TextPrimary,
    secondary = NeonViolet,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldProfit,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlow,
    outlineVariant = BorderSubtle,
    error = CrimsonDanger,
    onError = TextPrimary
)

val ArenaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun AhxanArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AhxanDarkColorScheme,
        typography = ArenaTypography,
        shapes = ArenaShapes,
        content = content
    )
}
