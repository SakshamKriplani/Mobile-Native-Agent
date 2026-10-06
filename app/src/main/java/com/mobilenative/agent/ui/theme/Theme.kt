package com.mobilenative.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    tertiary = Pink80,
    background = SurfaceDark,
    surface = CardBackground,
    onPrimary = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MobileNativeAgentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
