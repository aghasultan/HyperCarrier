package dev.hypercarrier.patcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HyperDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricViolet,
    tertiary = NeonEmerald,
    background = DarkOledBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color(0xFF001F2B),
    onSecondary = Color(0xFF1E0B36),
    onTertiary = Color(0xFF002914),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkCardBorder,
    error = CriticalCoral,
    onError = Color.White
)

private val HyperLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    secondary = Color(0xFF7C3AED),
    tertiary = Color(0xFF059669),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = LightCardBorder,
    error = CriticalCoral,
    onError = Color.White
)

@Composable
fun HyperCarrierTheme(
    darkTheme: Boolean = true, // Default to stunning OLED dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HyperDarkColorScheme else HyperLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
