package com.universalazan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep green and gold palette. Every surface colour is set explicitly so no default
// (purple-tinted) Material colours leak into the app.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E3F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7EFCF),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF7A5C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF7A5C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F7F2),
    surfaceContainer = Color(0xFFEFF2EC),
    surfaceContainerHigh = Color(0xFFE9ECE6),
    surfaceContainerHighest = Color(0xFFE3E6E1),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD3AE),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF0F5134),
    onPrimaryContainer = Color(0xFFB7EFCF),
    secondary = Color(0xFFE9C349),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFE9C349),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF111412),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF111412),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    surfaceContainerLowest = Color(0xFF0C0F0D),
    surfaceContainerLow = Color(0xFF191C1A),
    surfaceContainer = Color(0xFF1D201E),
    surfaceContainerHigh = Color(0xFF272B28),
    surfaceContainerHighest = Color(0xFF323533),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404943),
)

@Composable
fun UniversalAzanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
