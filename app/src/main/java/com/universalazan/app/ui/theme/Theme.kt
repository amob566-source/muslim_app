package com.universalazan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Basic green/gold palette. Refined in Stage 1.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E3F),
    secondary = Color(0xFF8A6D1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD3AE),
    secondary = Color(0xFFE2C56B),
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
