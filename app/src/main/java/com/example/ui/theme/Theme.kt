package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldPrimary,
    onPrimary = CharcoalBg,
    secondary = GoldLight,
    onSecondary = CharcoalBg,
    tertiary = GoldAccent,
    background = CharcoalBg,
    onBackground = OnCharcoal,
    surface = CharcoalSurface,
    onSurface = OnCharcoal,
    surfaceVariant = CharcoalSurfaceLight,
    onSurfaceVariant = OnCharcoal
  )

private val LightColorScheme = DarkColorScheme // Unified luxurious charcoal-gold brand identity!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled to preserve premium charcoal-gold brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
