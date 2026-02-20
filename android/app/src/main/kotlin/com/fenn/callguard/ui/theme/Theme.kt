package com.fenn.callguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Seed: deep indigo-blue — trustworthy, privacy-focused
private val Primary = Color(0xFF4A6CF7)
private val PrimaryDark = Color(0xFF7B9EFF)
private val Surface = Color(0xFF121212)
private val SurfaceLight = Color(0xFFF5F5F5)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF2A3D8F),
    onPrimaryContainer = Color(0xFFD0DCFF),
    secondary = Color(0xFF9EABC8),
    onSecondary = Color(0xFF232D42),
    surface = Surface,
    onSurface = Color(0xFFE2E2E2),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE2E2E2),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2D0000),
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF001258),
    secondary = Color(0xFF4A5568),
    onSecondary = Color.White,
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

@Composable
fun CallGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CallGuardTypography,
        content = content,
    )
}
