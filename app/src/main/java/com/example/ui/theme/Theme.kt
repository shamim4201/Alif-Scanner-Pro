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

private val DarkColorScheme =
  darkColorScheme(
    primary = ScannerCyanAccent,
    onPrimary = Color(0xFF00354E),
    primaryContainer = ScannerBluePrimary,
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = ScannerTealHighlight,
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFBEEAF7),
    tertiary = GoldStar,
    onTertiary = Color(0xFF452B00),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF283655),
    error = ScannerCoral
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ScannerBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = ScannerBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F0FD),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = ScannerAmber,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = ScannerCoral
  )

@Composable
fun ScannerProTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our branded scanner theme by default for consistency
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
