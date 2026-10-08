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
  primary = DeepTealPrimary,
  onPrimary = Color.White,
  primaryContainer = DeepTealDark,
  onPrimaryContainer = DeepTealContainer,
  secondary = DeepTealPrimary,
  onSecondary = Color.White,
  secondaryContainer = DarkSurface,
  onSecondaryContainer = DeepTealContainer,
  tertiary = StatusAdvance,
  onTertiary = Color.White,
  tertiaryContainer = StatusAdvanceContainer,
  onTertiaryContainer = StatusOnAdvanceContainer,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkBorder,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorder.copy(alpha = 0.6f),
  error = StatusOverdue,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = DeepTealPrimary,
  onPrimary = Color.White,
  primaryContainer = DeepTealContainer,
  onPrimaryContainer = DeepTealOnContainer,
  secondary = DeepTealDark,
  onSecondary = Color.White,
  secondaryContainer = DeepTealContainer,
  onSecondaryContainer = DeepTealOnContainer,
  tertiary = StatusAdvance,
  onTertiary = Color.White,
  tertiaryContainer = StatusAdvanceContainer,
  onTertiaryContainer = StatusOnAdvanceContainer,
  background = WarmIvoryBackground,
  onBackground = TextInkPrimary,
  surface = CardSurfaceWhite,
  onSurface = TextInkPrimary,
  surfaceVariant = SurfaceMuted,
  onSurfaceVariant = TextSecondaryMuted,
  outline = BorderWarmGray,
  outlineVariant = BorderWarmGray,
  error = StatusOverdue,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Always enforce intentional financial identity
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
