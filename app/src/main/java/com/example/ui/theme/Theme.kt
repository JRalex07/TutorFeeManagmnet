package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dedicated Claymorphic Theme Token System for soft tactile shadows,
 * inflated 3D matte surfaces, and harmonized financial status palettes.
 */
@Immutable
data class ClayThemeTokens(
  val isDark: Boolean,
  val canvasBackground: Color,
  val cardBackground: Color,
  val cardMutedBackground: Color,
  val surfaceMuted: Color,
  val borderColor: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val highlightRim: Color,
  val ambientShadow: Color,
  val spotShadow: Color,
  val defaultElevation: Dp = 6.dp,
  val defaultCornerRadius: Dp = 22.dp,

  // Financial Status Clay Colors
  val paidSurface: Color = StatusPaidContainer,
  val paidBorder: Color = StatusPaidBorder,
  val paidAccent: Color = StatusPaid,

  val dueSurface: Color = StatusDueContainer,
  val dueBorder: Color = StatusDueBorder,
  val dueAccent: Color = StatusDue,

  val overdueSurface: Color = StatusOverdueContainer,
  val overdueBorder: Color = StatusOverdueBorder,
  val overdueAccent: Color = StatusOverdue,

  val advanceSurface: Color = StatusAdvanceContainer,
  val advanceBorder: Color = StatusAdvanceBorder,
  val advanceAccent: Color = StatusAdvance,

  val partialSurface: Color = StatusPartialContainer,
  val partialBorder: Color = StatusPartialBorder,
  val partialAccent: Color = StatusPartial,

  val skyBlueSurface: Color = ClaySkyBlueContainer,
  val skyBlueBorder: Color = ClaySkyBlueBorder,
  val skyBlueAccent: Color = ClaySkyBlue
)

val LightClayTokens = ClayThemeTokens(
  isDark = false,
  canvasBackground = ClayCanvasBackground,
  cardBackground = ClayCardWhite,
  cardMutedBackground = ClayCardMuted,
  surfaceMuted = ClaySurfaceMuted,
  borderColor = ClayBorderWarmGray,
  textPrimary = TextInkPrimary,
  textSecondary = TextSecondaryMuted,
  highlightRim = ClayHighlightWhite,
  ambientShadow = ClayShadowAmbientLight,
  spotShadow = ClayShadowSpotLight
)

val DarkClayTokens = ClayThemeTokens(
  isDark = true,
  canvasBackground = DarkClayBackground,
  cardBackground = DarkClaySurface,
  cardMutedBackground = DarkClayCardMuted,
  surfaceMuted = DarkClaySurface,
  borderColor = DarkClayBorder,
  textPrimary = DarkTextPrimary,
  textSecondary = DarkTextSecondary,
  highlightRim = ClayHighlightSubtle,
  ambientShadow = ClayShadowAmbientDark,
  spotShadow = ClayShadowSpotDark
)

val LocalClayTheme = staticCompositionLocalOf { LightClayTokens }

val MaterialTheme.clay: ClayThemeTokens
  @Composable
  @ReadOnlyComposable
  get() = LocalClayTheme.current

// Material 3 Dark Color Scheme tuned for soft clay contrast
private val DarkColorScheme = darkColorScheme(
  primary = DeepTealLight,
  onPrimary = Color.White,
  primaryContainer = DeepTealDark,
  onPrimaryContainer = DeepTealContainer,
  secondary = SageSecondary,
  onSecondary = Color.White,
  secondaryContainer = DarkClaySurface,
  onSecondaryContainer = SageContainer,
  tertiary = StatusAdvance,
  onTertiary = Color.White,
  tertiaryContainer = DarkClayCardMuted,
  onTertiaryContainer = StatusAdvanceContainer,
  background = DarkClayBackground,
  onBackground = DarkTextPrimary,
  surface = DarkClaySurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkClayCardMuted,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkClayBorder,
  outlineVariant = DarkClayBorder.copy(alpha = 0.6f),
  error = StatusOverdue,
  onError = Color.White
)

// Material 3 Light Color Scheme implementing warm tactile clay aesthetic
private val LightColorScheme = lightColorScheme(
  primary = DeepTealPrimary,
  onPrimary = Color.White,
  primaryContainer = DeepTealContainer,
  onPrimaryContainer = DeepTealOnContainer,
  secondary = SageSecondary,
  onSecondary = Color.White,
  secondaryContainer = SageContainer,
  onSecondaryContainer = SageOnContainer,
  tertiary = StatusAdvance,
  onTertiary = Color.White,
  tertiaryContainer = StatusAdvanceContainer,
  onTertiaryContainer = StatusOnAdvanceContainer,
  background = ClayCanvasBackground,
  onBackground = TextInkPrimary,
  surface = ClayCardWhite,
  onSurface = TextInkPrimary,
  surfaceVariant = ClaySurfaceMuted,
  onSurfaceVariant = TextSecondaryMuted,
  outline = ClayBorderWarmGray,
  outlineVariant = ClayBorderWarmGray.copy(alpha = 0.7f),
  error = StatusOverdue,
  onError = Color.White
)

/**
 * Global Application Theme wrapping all screens and components.
 * Configures Material 3 with Claymorphic Color Scheme, Clay Typography,
 * Clay Shapes, and exposes LocalClayTheme for custom tactile modifiers.
 */
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Always enforce consistent clay financial branding
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

  val clayTokens = if (darkTheme) DarkClayTokens else LightClayTokens

  CompositionLocalProvider(LocalClayTheme provides clayTokens) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = ClayShapes,
      content = content
    )
  }
}

/**
 * Reusable Claymorphic Surface Modifier:
 * Applies inflated soft 3D elevation, rounded corners, matte clay background,
 * and a subtle top-left specular highlight rim gradient.
 */
fun Modifier.claymorphic(
  shape: Shape = RoundedCornerShape(22.dp),
  backgroundColor: Color = ClayCardWhite,
  elevation: Dp = 6.dp,
  highlightColor: Color = Color.White.copy(alpha = 0.85f),
  shadowColor: Color = Color(0x1A0F2B28)
): Modifier = this
  .shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = shadowColor,
    spotColor = shadowColor
  )
  .clip(shape)
  .background(backgroundColor)
