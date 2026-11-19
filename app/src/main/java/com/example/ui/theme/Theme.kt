package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AhxanHomeColorScheme = darkColorScheme(
    primary = ArenaOrange,
    onPrimary = Color.White,
    primaryContainer = ArenaPurple,
    onPrimaryContainer = Color.White,
    secondary = ArenaBlue,
    onSecondary = Color.White,
    secondaryContainer = HeaderNavy,
    onSecondaryContainer = TextPrimary,
    tertiary = ArenaGreen,
    onTertiary = Color.White,
    background = ArenaCharcoal,
    onBackground = TextPrimary,
    surface = HeaderNavy,
    onSurface = TextPrimary,
    surfaceVariant = BottomNavBg,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF475569),
    outlineVariant = BorderSubtle,
    error = CrimsonDanger,
    onError = Color.White
)

val ArenaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AhxanArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AhxanHomeColorScheme,
        typography = ArenaTypography,
        shapes = ArenaShapes,
        content = content
    )
}
