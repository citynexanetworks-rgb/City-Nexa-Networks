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

private val DarkColorScheme = darkColorScheme(
    primary = NexaIceBlue,
    onPrimary = NexaNavyDark,
    primaryContainer = NexaDarkBlue,
    onPrimaryContainer = Color.White,
    secondary = NexaCobaltAccent,
    onSecondary = Color.White,
    secondaryContainer = NexaNavyMedium,
    onSecondaryContainer = Color.White,
    tertiary = NexaTeal,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5FB),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = NexaDarkBlue,
    onPrimary = Color.White,
    primaryContainer = NexaDeepNavy,
    onPrimaryContainer = Color.White,
    secondary = NexaNavyMedium,
    onSecondary = Color.White,
    secondaryContainer = NexaBlueSubtle,
    onSecondaryContainer = NexaDarkBlue,
    tertiary = NexaTeal,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF334155)
)

@Composable
fun CityNexaCRMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep corporate Dark Blue theme strictly cohesive
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    CityNexaCRMTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
