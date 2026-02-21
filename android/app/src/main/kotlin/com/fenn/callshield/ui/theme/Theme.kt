package com.fenn.callshield.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Dark palette ────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4D9EFF),
    onPrimary = Color(0xFF00264D),
    primaryContainer = Color(0xFF1A3A6B),
    onPrimaryContainer = Color(0xFFB8D4FF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF003540),
    secondaryContainer = Color(0xFF004E5C),
    onSecondaryContainer = Color(0xFFB3F0FF),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF3D2000),
    tertiaryContainer = Color(0xFF593000),
    onTertiaryContainer = Color(0xFFFFDDB8),
    error = Color(0xFFFF3D3D),
    onError = Color(0xFF5A0000),
    errorContainer = Color(0xFF7A0000),
    onErrorContainer = Color(0xFFFFDAD4),
    surface = Color(0xFF141929),
    onSurface = Color(0xFFE3E8FF),
    surfaceVariant = Color(0xFF1C2438),
    onSurfaceVariant = Color(0xFFB0B8D0),
    background = Color(0xFF0A0F1E),
    onBackground = Color(0xFFE3E8FF),
    outline = Color(0xFF3A4565),
    outlineVariant = Color(0xFF2A3352),
    inverseSurface = Color(0xFFE3E8FF),
    inverseOnSurface = Color(0xFF0A0F1E),
    inversePrimary = Color(0xFF1A5099),
    scrim = Color(0xFF000000),
)

// ── Light palette ────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E4FF),
    onPrimaryContainer = Color(0xFF001A45),
    secondary = Color(0xFF006878),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFAEEEFF),
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = Color(0xFF7E5700),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF281900),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C2A),
    surfaceVariant = Color(0xFFF0F4FF),
    onSurfaceVariant = Color(0xFF414B6A),
    background = Color(0xFFF0F4FF),
    onBackground = Color(0xFF1A1C2A),
    outline = Color(0xFF717A9A),
    outlineVariant = Color(0xFFCAD0E8),
    inverseSurface = Color(0xFF2E3047),
    inverseOnSurface = Color(0xFFF0F4FF),
    inversePrimary = Color(0xFF9EC5FF),
    scrim = Color(0xFF000000),
)

// ── Semantic extension colors ────────────────────────────────────────────────
val LocalSuccessColor = staticCompositionLocalOf { Color(0xFF00C853) }
val LocalWarningColor = staticCompositionLocalOf { Color(0xFFFF9100) }
val LocalDangerColor  = staticCompositionLocalOf { Color(0xFFFF3D3D) }

@Composable
fun CallShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val successColor = if (darkTheme) Color(0xFF00C853) else Color(0xFF1B873A)
    val warningColor = if (darkTheme) Color(0xFFFF9100) else Color(0xFFD46000)
    val dangerColor  = if (darkTheme) Color(0xFFFF3D3D) else Color(0xFFBA1A1A)

    CompositionLocalProvider(
        LocalSuccessColor provides successColor,
        LocalWarningColor provides warningColor,
        LocalDangerColor  provides dangerColor,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CallShieldTypography,
            content = content,
        )
    }
}
