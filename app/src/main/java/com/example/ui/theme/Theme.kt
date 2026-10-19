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
    primary = ElectricCyan,
    secondary = SapphireBlue,
    tertiary = IceViolet,
    background = ObsidianDark,
    surface = GlassSlateCard,
    onPrimary = Color(0xFF06080D), // Deep obsidian text on vibrant cyan buttons
    onSecondary = Color.White,
    onBackground = CinemaTextLight,
    onSurface = CinemaTextLight,
    surfaceVariant = GeometricSecondary,
    outline = GlassBorderLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0284C7), // Crisp sapphire cyan (no gold)
    secondary = Color(0xFF475569),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic colors to enforce the selected Geometric Balance palette consistently
  dynamicColor: Boolean = false,
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
