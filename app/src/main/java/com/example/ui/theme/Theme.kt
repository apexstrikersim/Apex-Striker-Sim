package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SportsColorScheme = darkColorScheme(
    primary = PitchGreen,
    onPrimary = Color(0xFF070B14),
    primaryContainer = DarkSlate,
    onPrimaryContainer = PitchGreen,
    secondary = TrophyGold,
    onSecondary = Color(0xFF070B14),
    background = SportsDarkBg,
    onBackground = TextPrimary,
    surface = SportsCardBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlate,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = MutedRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme by default for immersive sports feel
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored branding
    content: @Composable () -> Unit,
) {
    val colorScheme = SportsColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
